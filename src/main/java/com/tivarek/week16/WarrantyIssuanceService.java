package com.tivarek.week16;

import com.tivarek.week15.SalesService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/** Called inside the existing sale/repair transaction; never issues before handover. */
@Service
public class WarrantyIssuanceService {
    public record Policy(long id, int days, String terms) { }
    private final JdbcTemplate db;
    private final SecureRandom random = new SecureRandom();

    public WarrantyIssuanceService(JdbcTemplate db) { this.db = db; }

    public int saleDays(String kind, long id) {
        Policy p = policyForSale(kind, id);
        return p == null ? 0 : p.days();
    }

    public int repairPartDays(long productId) {
        Policy p = productPolicy(productId);
        return p == null ? 0 : p.days();
    }

    private Policy productPolicy(long id) {
        var rows = db.queryForList("""
            SELECT s.PolicyID,s.WarrantyDays,p.Terms FROM dbo.ProductWarrantySettings s
            JOIN dbo.WarrantyPolicies p ON p.PolicyID=s.PolicyID AND p.IsActive=1
            WHERE s.ProductID=?
            """, id);
        return policy(rows);
    }

    private Policy policyForSale(String kind, long id) {
        if ("BULK".equals(kind)) return productPolicy(id);
        if (!"DEVICE".equals(kind)) return null;
        var overrides = db.queryForList("""
            SELECT s.PolicyID,s.WarrantyDays,p.Terms FROM dbo.DeviceWarrantyOverrides s
            JOIN dbo.WarrantyPolicies p ON p.PolicyID=s.PolicyID AND p.IsActive=1
            WHERE s.DeviceID=?
            """, id);
        if (!overrides.isEmpty()) return policy(overrides);
        var products = db.queryForList("SELECT ProductID FROM dbo.Devices WHERE DeviceID=?", id);
        if (products.isEmpty() || products.getFirst().get("ProductID") == null) return null;
        return productPolicy(((Number) products.getFirst().get("ProductID")).longValue());
    }

    private Policy policyForQuote(Object serviceId, Object productId) {
        if (serviceId != null) {
            var rows = db.queryForList("""
                SELECT p.PolicyID,s.DefaultWarrantyDays AS WarrantyDays,p.Terms
                FROM dbo.ServiceWarrantyPolicies x JOIN dbo.Services s ON s.ServiceID=x.ServiceID
                JOIN dbo.WarrantyPolicies p ON p.PolicyID=x.PolicyID AND p.IsActive=1
                WHERE s.ServiceID=?
                """, serviceId);
            return policy(rows);
        }
        return productId == null ? null : productPolicy(((Number) productId).longValue());
    }

    private static Policy policy(List<Map<String,Object>> rows) {
        if (rows.isEmpty()) return null;
        var r = rows.getFirst();
        return new Policy(((Number) r.get("PolicyID")).longValue(),
            ((Number) r.get("WarrantyDays")).intValue(), (String) r.get("Terms"));
    }

    /** SaleWorkflowService already holds the order lock at SERIALIZABLE isolation. */
    public void issueCompletedSale(long orderId) {
        String status = db.queryForObject("SELECT Status FROM dbo.SalesOrders WHERE OrderID=?", String.class, orderId);
        if (!"COMPLETED".equals(status)) return;
        var lines = db.queryForList("""
            SELECT l.SaleLineID,l.WarrantyDaysSnapshot,b.ProductID,d.DeviceID
            FROM dbo.SalesLines l
            LEFT JOIN dbo.BulkSaleLines b ON b.SaleLineID=l.SaleLineID
            LEFT JOIN dbo.DeviceSaleLines d ON d.SaleLineID=l.SaleLineID
            WHERE l.OrderID=? AND l.WarrantyDaysSnapshot>0
            ORDER BY l.SaleLineID
            """, orderId);
        for (var row : lines) {
            long line = ((Number) row.get("SaleLineID")).longValue();
            if (exists("SELECT COUNT(*) FROM dbo.SaleWarranties WHERE SaleLineID=?", line)) continue;
            Object device = row.get("DeviceID"), product = row.get("ProductID");
            Policy p = policyForSale(device != null ? "DEVICE" : "BULK",
                ((Number) (device != null ? device : product)).longValue());
            int days = ((Number) row.get("WarrantyDaysSnapshot")).intValue();
            if (p == null || days <= 0) continue;
            long warranty = issue(p.id(), days, p.terms());
            db.update("INSERT INTO dbo.SaleWarranties(WarrantyID,SaleLineID) VALUES (?,?)", warranty, line);
        }
    }

    /** Called only after RepairTickets is DELIVERED, in the handover transaction. */
    public void issueDeliveredRepair(long ticketId) {
        String status = db.queryForObject("SELECT Status FROM dbo.RepairTickets WHERE RepairTicketID=?", String.class, ticketId);
        if (!"DELIVERED".equals(status)) return;
        var rows = db.queryForList("""
            SELECT i.QuoteItemID,i.WarrantyDaysSnapshot,s.ServiceID,part.ProductID
            FROM dbo.QuoteItems i JOIN dbo.RepairQuotes q ON q.QuoteID=i.QuoteID AND q.Status='ACCEPTED'
            LEFT JOIN dbo.QuoteServiceItems s ON s.QuoteItemID=i.QuoteItemID
            LEFT JOIN dbo.QuotePartItems part ON part.QuoteItemID=i.QuoteItemID
            WHERE q.RepairTicketID=? AND i.WarrantyDaysSnapshot>0 ORDER BY i.QuoteItemID
            """, ticketId);
        for (var row : rows) {
            long item = ((Number) row.get("QuoteItemID")).longValue();
            if (exists("SELECT COUNT(*) FROM dbo.RepairWarranties WHERE QuoteItemID=?", item)) continue;
            Policy p = policyForQuote(row.get("ServiceID"), row.get("ProductID"));
            int days = ((Number) row.get("WarrantyDaysSnapshot")).intValue();
            if (p == null || days <= 0) continue;
            long warranty = issue(p.id(), days, p.terms());
            db.update("INSERT INTO dbo.RepairWarranties(WarrantyID,QuoteItemID) VALUES (?,?)", warranty, item);
        }
    }

    /** A partial BULK return does not cancel the whole line's warranty. */
    public void voidIfFullyReturned(long saleLineId) {
        var source = db.queryForList("""
            SELECT COALESCE(b.Quantity,1) AS Quantity FROM dbo.SalesLines l
            LEFT JOIN dbo.BulkSaleLines b ON b.SaleLineID=l.SaleLineID WHERE l.SaleLineID=?
            """, saleLineId);
        if(source.isEmpty()) return;
        int original=((Number)source.getFirst().get("Quantity")).intValue();
        int returned=db.queryForObject("""
            SELECT COALESCE((SELECT SUM(e.Delta) FROM dbo.ReturnBulkEntries x
              JOIN dbo.BulkStockEntries e ON e.BulkEntryID=x.BulkEntryID
              JOIN dbo.InventoryDocuments d ON d.InventoryDocID=e.InventoryDocID AND d.Status='POSTED'
              WHERE x.SaleLineID=?),
              (SELECT COUNT(*) FROM dbo.ReturnDeviceEntries x
              JOIN dbo.DeviceStockEntries e ON e.DeviceEntryID=x.DeviceEntryID
              JOIN dbo.InventoryDocuments d ON d.InventoryDocID=e.InventoryDocID AND d.Status='POSTED'
              WHERE x.SaleLineID=?),0)
            """,Integer.class,saleLineId,saleLineId);
        if(returned>=original)db.update("""
            UPDATE w SET Status='VOID' FROM dbo.Warranties w
            JOIN dbo.SaleWarranties sw ON sw.WarrantyID=w.WarrantyID
            WHERE sw.SaleLineID=? AND w.Status='ACTIVE'
            """,saleLineId);
    }

    private long issue(long policyId, int days, String terms) {
        LocalDateTime start = LocalDateTime.ofInstant(java.time.Instant.now(), ZoneOffset.UTC);
        String code = SalesService.code("BH-");
        // The first token is intentionally not displayed by an asynchronous operation.
        // Authorized staff can issue a fresh one-time lookup token at handover.
        byte[] secret = new byte[24]; random.nextBytes(secret);
        byte[] digest = sha256(HexFormat.of().formatHex(secret));
        var holder = new GeneratedKeyHolder();
        db.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO dbo.Warranties(WarrantyCode,PolicyID,StartAt,EndAt,TermsSnapshot,Status,TrackingTokenHash)
                VALUES (?,?,?,?,?,'ACTIVE',?)
                """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, code);
            ps.setLong(2, policyId);
            ps.setObject(3, start);
            ps.setObject(4, start.plusDays(days));
            ps.setString(5, terms);
            ps.setBytes(6, digest);
            return ps;
        }, holder);
        if (holder.getKey() == null) throw new WarrantyProblem("Không tạo được phiếu bảo hành.");
        return holder.getKey().longValue();
    }

    public static byte[] sha256(String token) {
        try { return MessageDigest.getInstance("SHA-256").digest(token.toUpperCase(java.util.Locale.ROOT).getBytes(StandardCharsets.US_ASCII)); }
        catch (Exception ex) { throw new IllegalStateException("SHA-256 không khả dụng", ex); }
    }

    private boolean exists(String query, Object... args) {
        return db.queryForObject(query, Integer.class, args) > 0;
    }
}
