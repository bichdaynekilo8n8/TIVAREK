package com.tivarek.week17;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Báo cáo READ-ONLY, lấy số liệu thực từ chứng từ đã ghi sổ/thanh toán thành công. */
@Service
@Transactional(readOnly=true)
public class ReportService {
    private final JdbcTemplate db;
    public ReportService(JdbcTemplate db) {this.db=db;}

    public record Cash(BigDecimal salesIn,BigDecimal repairsIn,BigDecimal refunds,BigDecimal expenses) {
        public BigDecimal netCash() {return salesIn.add(repairsIn).subtract(refunds).subtract(expenses);}
    }

    public record Overview(Cash cash,long ordersCreated,long ordersCompleted,BigDecimal completedOrderValue,
                           long repairsReceived,long repairsDelivered,long repairsOpen,long claimsOpen,
                           long lowStockProducts,long devicesAvailable) {}

    public Cash cash(ReportRange r) {
        var p=db.queryForMap("""
            SELECT
              COALESCE(SUM(CASE WHEN Direction='IN' AND OrderID IS NOT NULL THEN Amount ELSE 0 END),0) AS SalesIn,
              COALESCE(SUM(CASE WHEN Direction='IN' AND RepairTicketID IS NOT NULL THEN Amount ELSE 0 END),0) AS RepairsIn,
              COALESCE(SUM(CASE WHEN Direction='OUT' THEN Amount ELSE 0 END),0) AS Refunds
            FROM dbo.Payments WHERE Status='SUCCEEDED' AND OccurredAt>=? AND OccurredAt<?
            """,r.startUtc(),r.endExclusiveUtc());
        return new Cash(decimal(p.get("SalesIn")),decimal(p.get("RepairsIn")),decimal(p.get("Refunds")),
            amount("SELECT COALESCE(SUM(Amount),0) FROM dbo.Expenses WHERE OccurredAt>=? AND OccurredAt<?",
                r.startUtc(),r.endExclusiveUtc()));
    }

    public Overview overview(ReportRange r) {
        return new Overview(cash(r),
            count("SELECT COUNT_BIG(*) FROM dbo.SalesOrders WHERE OrderedAt>=? AND OrderedAt<?",r.startUtc(),r.endExclusiveUtc()),
            count("SELECT COUNT_BIG(*) FROM dbo.SalesOrders WHERE Status='COMPLETED' AND CompletedAt>=? AND CompletedAt<?",r.startUtc(),r.endExclusiveUtc()),
            completedOrderValue(r),
            count("SELECT COUNT_BIG(*) FROM dbo.RepairTickets WHERE ReceivedAt>=? AND ReceivedAt<?",r.startUtc(),r.endExclusiveUtc()),
            count("SELECT COUNT_BIG(*) FROM dbo.RepairTickets WHERE Status='DELIVERED' AND ClosedAt>=? AND ClosedAt<?",r.startUtc(),r.endExclusiveUtc()),
            count("SELECT COUNT_BIG(*) FROM dbo.RepairTickets WHERE Status NOT IN ('DELIVERED','CANCELLED')"),
            count("SELECT COUNT_BIG(*) FROM dbo.WarrantyClaims WHERE Status IN ('OPEN','INSPECTING','APPROVED')"),
            lowStockCount(),availableDevices());
    }

    /** Giá trị hàng đã hoàn tất (chưa trừ hoàn trả), khác với số tiền thực thu. */
    public BigDecimal completedOrderValue(ReportRange r) {
        return amount("""
            SELECT COALESCE(SUM(v.NetAmount+v.TaxAmount),0)
            FROM dbo.SalesOrders o JOIN dbo.vSaleLineAmounts v ON v.OrderID=o.OrderID
            WHERE o.Status='COMPLETED' AND o.CompletedAt>=? AND o.CompletedAt<?
            """,r.startUtc(),r.endExclusiveUtc());
    }

    public List<Map<String,Object>> cashDays(ReportRange r) {
        return db.queryForList("""
            SELECT CONVERT(varchar(10), CAST(DATEADD(hour,7,x.OccurredAt) AS date),23) AS DayLocal,
                SUM(x.SalesIn) AS SalesIn,SUM(x.RepairsIn) AS RepairsIn,
                SUM(x.Refunds) AS Refunds,SUM(x.Expenses) AS Expenses,
                SUM(x.SalesIn+x.RepairsIn-x.Refunds-x.Expenses) AS NetCash
            FROM (
              SELECT OccurredAt,
                CASE WHEN Direction='IN' AND OrderID IS NOT NULL THEN Amount ELSE 0 END SalesIn,
                CASE WHEN Direction='IN' AND RepairTicketID IS NOT NULL THEN Amount ELSE 0 END RepairsIn,
                CASE WHEN Direction='OUT' THEN Amount ELSE 0 END Refunds,
                CAST(0 AS decimal(19,2)) Expenses
              FROM dbo.Payments WHERE Status='SUCCEEDED' AND OccurredAt>=? AND OccurredAt<?
              UNION ALL
              SELECT OccurredAt,CAST(0 AS decimal(19,2)),CAST(0 AS decimal(19,2)),
                     CAST(0 AS decimal(19,2)),Amount
              FROM dbo.Expenses WHERE OccurredAt>=? AND OccurredAt<?
            ) x
            GROUP BY CAST(DATEADD(hour,7,x.OccurredAt) AS date)
            ORDER BY DayLocal
            """,r.startUtc(),r.endExclusiveUtc(),r.startUtc(),r.endExclusiveUtc());
    }

    public List<Map<String,Object>> orderStatuses(ReportRange r) {
        return db.queryForList("""
            SELECT Status,Channel,COUNT_BIG(*) AS OrderCount
            FROM dbo.SalesOrders WHERE OrderedAt>=? AND OrderedAt<?
            GROUP BY Status,Channel ORDER BY Channel,Status
            """,r.startUtc(),r.endExclusiveUtc());
    }

    public List<Map<String,Object>> topProducts(ReportRange r) {
        return db.queryForList("""
            SELECT TOP(20) x.SKU,x.ProductName,SUM(x.Quantity) AS Quantity,
                SUM(x.GrossValue) AS GrossValue
            FROM (
              SELECT p.SKU,p.Name AS ProductName,CONVERT(bigint,b.Quantity) AS Quantity,
                v.NetAmount+v.TaxAmount AS GrossValue
              FROM dbo.SalesOrders o JOIN dbo.SalesLines l ON l.OrderID=o.OrderID
              JOIN dbo.vSaleLineAmounts v ON v.SaleLineID=l.SaleLineID
              JOIN dbo.BulkSaleLines b ON b.SaleLineID=l.SaleLineID
              JOIN dbo.Products p ON p.ProductID=b.ProductID
              WHERE o.Status='COMPLETED' AND o.CompletedAt>=? AND o.CompletedAt<?
              UNION ALL
              SELECT COALESCE(p.SKU,d.AssetTag),
                     COALESCE(p.Name,d.DeviceDescription),CONVERT(bigint,1),
                v.NetAmount+v.TaxAmount
              FROM dbo.SalesOrders o JOIN dbo.SalesLines l ON l.OrderID=o.OrderID
              JOIN dbo.vSaleLineAmounts v ON v.SaleLineID=l.SaleLineID
              JOIN dbo.DeviceSaleLines sl ON sl.SaleLineID=l.SaleLineID
              JOIN dbo.Devices d ON d.DeviceID=sl.DeviceID
              LEFT JOIN dbo.Products p ON p.ProductID=d.ProductID
              WHERE o.Status='COMPLETED' AND o.CompletedAt>=? AND o.CompletedAt<?
            ) x GROUP BY x.SKU,x.ProductName
            ORDER BY Quantity DESC,GrossValue DESC,x.SKU
            """,r.startUtc(),r.endExclusiveUtc(),r.startUtc(),r.endExclusiveUtc());
    }

    public List<Map<String,Object>> returns(ReportRange r) {
        return db.queryForList("""
            SELECT TOP(30) o.OrderCode,CONVERT(varchar(16),DATEADD(hour,7,d.OccurredAt),120) AS ReturnLocal,
                CONVERT(bigint,COALESCE((SELECT SUM(e.Delta) FROM dbo.ReturnBulkEntries b
                    JOIN dbo.BulkStockEntries e ON e.BulkEntryID=b.BulkEntryID
                    WHERE e.InventoryDocID=d.InventoryDocID),0)
                  +COALESCE((SELECT COUNT_BIG(*) FROM dbo.ReturnDeviceEntries x
                    JOIN dbo.DeviceStockEntries e ON e.DeviceEntryID=x.DeviceEntryID
                    WHERE e.InventoryDocID=d.InventoryDocID),0)) AS ReturnedQuantity,
                s.ReturnReason
            FROM dbo.SalesReturns s JOIN dbo.InventoryDocuments d ON d.InventoryDocID=s.InventoryDocID
            JOIN dbo.SalesOrders o ON o.OrderID=s.OrderID
            WHERE d.Status='POSTED' AND d.OccurredAt>=? AND d.OccurredAt<?
              AND NOT EXISTS(SELECT 1 FROM dbo.InventoryReversals ir
                  JOIN dbo.InventoryDocuments rev ON rev.InventoryDocID=ir.ReversalDocID AND rev.Status='POSTED'
                  WHERE ir.OriginalDocID=d.InventoryDocID)
            ORDER BY d.OccurredAt DESC,d.InventoryDocID DESC
            """,r.startUtc(),r.endExclusiveUtc());
    }

    public List<Map<String,Object>> repairStatuses(ReportRange r) {
        return db.queryForList("""
            SELECT Status,COUNT_BIG(*) AS TicketCount FROM dbo.RepairTickets
            WHERE ReceivedAt>=? AND ReceivedAt<? GROUP BY Status ORDER BY TicketCount DESC
            """,r.startUtc(),r.endExclusiveUtc());
    }

    /** Hàng đợi hiện tại, không phụ thuộc bộ lọc ngày. */
    public List<Map<String,Object>> repairQueue() {
        return db.queryForList("""
            SELECT TOP(30) TicketCode,Status,CONVERT(varchar(16),DATEADD(hour,7,ReceivedAt),120) AS ReceivedLocal,
                DATEDIFF(day,ReceivedAt,SYSUTCDATETIME()) AS DaysOpen,
                CASE WHEN PromisedAt IS NOT NULL AND PromisedAt<SYSUTCDATETIME() THEN 1 ELSE 0 END AS Overdue
            FROM dbo.RepairTickets WHERE Status NOT IN ('DELIVERED','CANCELLED')
            ORDER BY CASE WHEN PromisedAt<SYSUTCDATETIME() THEN 0 ELSE 1 END,ReceivedAt,RepairTicketID
            """);
    }

    /** Tồn kho tính từ journal POSTED; hàng giữ còn hiệu lực trừ khỏi số khả dụng. */
    private static final String LOW_STOCK_CTES = """
        WITH stock AS (
          SELECT e.ProductID,SUM(CONVERT(bigint,e.Delta)) Qty
          FROM dbo.BulkStockEntries e
          JOIN dbo.InventoryDocuments d ON d.InventoryDocID=e.InventoryDocID AND d.Status='POSTED'
          JOIN dbo.StockLocations loc ON loc.LocationID=e.LocationID AND loc.LocationType='SALEABLE'
          GROUP BY e.ProductID
        ), held AS (
          SELECT b.ProductID,SUM(CONVERT(bigint,r.Quantity)) Qty
          FROM dbo.BulkReservations r JOIN dbo.BulkSaleLines b ON b.SaleLineID=r.SaleLineID
          JOIN dbo.StockLocations loc ON loc.LocationID=r.LocationID AND loc.LocationType='SALEABLE'
          WHERE r.Status='ACTIVE' AND r.ExpiresAt>SYSUTCDATETIME()
          GROUP BY b.ProductID
        )
        """;

    public List<Map<String,Object>> lowStock() {
        return db.queryForList(LOW_STOCK_CTES+"""
            SELECT TOP(150) p.SKU,p.Name AS ProductName,COALESCE(s.Qty,0) AS OnHand,
                COALESCE(h.Qty,0) AS Reserved,COALESCE(s.Qty,0)-COALESCE(h.Qty,0) AS Available
            FROM dbo.Products p LEFT JOIN stock s ON s.ProductID=p.ProductID
            LEFT JOIN held h ON h.ProductID=p.ProductID
            WHERE p.IsActive=1 AND p.StockMode='BULK' AND COALESCE(s.Qty,0)-COALESCE(h.Qty,0)<=5
            ORDER BY Available,p.SKU
            """);
    }

    public long lowStockCount() {
        return count(LOW_STOCK_CTES+"""
            SELECT COUNT_BIG(*) FROM dbo.Products p
            LEFT JOIN stock s ON s.ProductID=p.ProductID
            LEFT JOIN held h ON h.ProductID=p.ProductID
            WHERE p.IsActive=1 AND p.StockMode='BULK' AND COALESCE(s.Qty,0)-COALESCE(h.Qty,0)<=5
            """);
    }

    public List<Map<String,Object>> stockByLocation() {
        return db.queryForList("""
            SELECT loc.LocationCode,loc.Name AS LocationName,loc.LocationType,
                COALESCE(SUM(CONVERT(bigint,e.Delta)),0) AS OnHand
            FROM dbo.StockLocations loc
            LEFT JOIN dbo.BulkStockEntries e ON e.LocationID=loc.LocationID
              AND EXISTS(SELECT 1 FROM dbo.InventoryDocuments d WHERE d.InventoryDocID=e.InventoryDocID AND d.Status='POSTED')
            GROUP BY loc.LocationCode,loc.Name,loc.LocationType
            ORDER BY loc.LocationType,loc.LocationCode
            """);
    }

    public long availableDevices() {
        return count("""
            WITH latest AS (
              SELECT e.DeviceID,e.ToLocationID,
                     ROW_NUMBER() OVER(PARTITION BY e.DeviceID
                       ORDER BY d.OccurredAt DESC,d.InventoryDocID DESC,e.DeviceEntryID DESC) rn
              FROM dbo.DeviceStockEntries e
              JOIN dbo.InventoryDocuments d ON d.InventoryDocID=e.InventoryDocID AND d.Status='POSTED'
            )
            SELECT COUNT_BIG(*) FROM latest x
            JOIN dbo.StockLocations loc ON loc.LocationID=x.ToLocationID AND loc.LocationType='SALEABLE'
            JOIN dbo.Devices device ON device.DeviceID=x.DeviceID AND device.OwnerCustomerID IS NULL
            WHERE x.rn=1
            """);
    }

    public List<Map<String,Object>> claimStatuses(ReportRange r) {
        return db.queryForList("""
            SELECT Status,COUNT_BIG(*) AS ClaimCount FROM dbo.WarrantyClaims
            WHERE ClaimedAt>=? AND ClaimedAt<? GROUP BY Status ORDER BY ClaimCount DESC
            """,r.startUtc(),r.endExclusiveUtc());
    }

    public List<Map<String,Object>> expiringWarranties() {
        return db.queryForList("""
            SELECT TOP(30) WarrantyCode,CONVERT(varchar(10),DATEADD(hour,7,EndAt),103) AS EndLocal,
                DATEDIFF(day,SYSUTCDATETIME(),EndAt) AS DaysRemaining
            FROM dbo.Warranties
            WHERE Status='ACTIVE' AND EndAt>=SYSUTCDATETIME() AND EndAt<DATEADD(day,30,SYSUTCDATETIME())
            ORDER BY EndAt,WarrantyID
            """);
    }

    public long activeWarranties() {
        return count("SELECT COUNT_BIG(*) FROM dbo.Warranties WHERE Status='ACTIVE' AND EndAt>=SYSUTCDATETIME()");
    }

    private long count(String sql,Object... args) {
        Long value=db.queryForObject(sql,Long.class,args);
        return value==null?0:value;
    }
    private BigDecimal amount(String sql,Object... args) {
        return decimal(db.queryForObject(sql,BigDecimal.class,args));
    }
    private static BigDecimal decimal(Object value) {
        return value==null?BigDecimal.ZERO:(BigDecimal)value;
    }
}
