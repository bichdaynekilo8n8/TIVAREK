package com.tivarek.week12;

import com.tivarek.week8.AuditService;
import com.tivarek.week22.StockLocks;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.*;

@Service
public class InventoryService {
    private final JdbcTemplate db;
    private final AuditService audit;
    public InventoryService(JdbcTemplate db, AuditService audit) { this.db=db; this.audit=audit; }

    public List<Map<String,Object>> locations() { return db.queryForList("SELECT LocationID,LocationCode,Name,LocationType,IsActive FROM dbo.StockLocations ORDER BY Name"); }
    public List<Map<String,Object>> suppliers() { return db.queryForList("SELECT SupplierID,SupplierCode,Name FROM dbo.Suppliers ORDER BY Name"); }
    public List<Map<String,Object>> items() { return db.queryForList("SELECT ProductID,SKU,Name,StockMode FROM dbo.Products WHERE IsActive=1 ORDER BY Name"); }
    public List<Map<String,Object>> devices() { return db.queryForList("SELECT d.DeviceID,d.AssetTag,d.DeviceDescription FROM dbo.Devices d JOIN dbo.Products p ON p.ProductID=d.ProductID WHERE d.OwnerCustomerID IS NULL AND p.StockMode='SERIALIZED' ORDER BY d.DeviceID DESC"); }
    public List<Map<String,Object>> documents() { return db.queryForList("SELECT TOP 100 InventoryDocID,DocumentCode,DocumentType,Status,OccurredAt,Reason FROM dbo.InventoryDocuments ORDER BY InventoryDocID DESC"); }
    public Map<String,Object> document(long id) {return one("SELECT InventoryDocID,DocumentCode,DocumentType,Status,OccurredAt,Reason FROM dbo.InventoryDocuments WHERE InventoryDocID=?",id);}
    public List<Map<String,Object>> bulkLines(long id) {return db.queryForList("""
        SELECT e.BulkEntryID,p.SKU,p.Name AS ProductName,l.LocationCode,e.Delta,e.UnitCost FROM dbo.BulkStockEntries e
        JOIN dbo.Products p ON p.ProductID=e.ProductID JOIN dbo.StockLocations l ON l.LocationID=e.LocationID
        WHERE e.InventoryDocID=? ORDER BY e.BulkEntryID
        """,id);}
    public List<Map<String,Object>> deviceLines(long id) {return db.queryForList("""
        SELECT e.DeviceEntryID,d.AssetTag,fl.LocationCode AS FromCode,tl.LocationCode AS ToCode,e.UnitCost
        FROM dbo.DeviceStockEntries e JOIN dbo.Devices d ON d.DeviceID=e.DeviceID
        LEFT JOIN dbo.StockLocations fl ON fl.LocationID=e.FromLocationID
        LEFT JOIN dbo.StockLocations tl ON tl.LocationID=e.ToLocationID
        WHERE e.InventoryDocID=? ORDER BY e.DeviceEntryID
        """,id);}
    public List<Map<String,Object>> reservations() { return db.queryForList("""
        SELECT TOP 100 r.ReservationID,'BULK' AS Kind,r.SaleLineID,r.LocationID,r.Quantity,r.ExpiresAt,r.Status
        FROM dbo.BulkReservations r ORDER BY r.ReservationID DESC
        """); }
    public List<Map<String,Object>> serialReservations() {return db.queryForList("""
        SELECT TOP 100 r.ReservationID,r.SaleLineID,a.DeviceID,r.ExpiresAt,r.Status
        FROM dbo.DeviceReservations r LEFT JOIN dbo.ActiveDeviceReservations a ON a.ReservationID=r.ReservationID
        ORDER BY r.ReservationID DESC
        """);}

    @PreAuthorize("hasAuthority('STOCK_ISSUE')")
    @Transactional(isolation=Isolation.SERIALIZABLE)
    public long reserveBulk(long saleLineId,long locationId,int quantity,int minutes) {
        validateExpiry(quantity,minutes);activeLocation(locationId);
        var sale=one("""
          SELECT b.ProductID,b.Quantity,o.Status FROM dbo.BulkSaleLines b
          JOIN dbo.SalesLines sl ON sl.SaleLineID=b.SaleLineID JOIN dbo.SalesOrders o WITH (UPDLOCK,HOLDLOCK) ON o.OrderID=sl.OrderID WHERE b.SaleLineID=?
          """,saleLineId);
        if(!Set.of("DRAFT","CONFIRMED").contains(sale.get("Status"))) throw new StockProblem("Đơn không ở trạng thái được phép giữ hàng.");
        long product=((Number)sale.get("ProductID")).longValue();
        StockLocks.product(db,product);
        int held=db.queryForObject("SELECT COALESCE(SUM(Quantity),0) FROM dbo.BulkReservations WHERE SaleLineID=? AND Status='ACTIVE' AND ExpiresAt>SYSUTCDATETIME()",Integer.class,saleLineId);
        int shipped=db.queryForObject("""
          SELECT COALESCE(SUM(-e.Delta),0) FROM dbo.SaleBulkIssues s
          JOIN dbo.BulkStockEntries e ON e.BulkEntryID=s.BulkEntryID
          JOIN dbo.InventoryDocuments d ON d.InventoryDocID=e.InventoryDocID AND d.Status='POSTED'
          WHERE s.SaleLineID=?
          """,Integer.class,saleLineId);
        if(held+shipped+quantity>((Number)sale.get("Quantity")).intValue()) throw new StockProblem("Giữ vượt số lượng trong đơn bán.");
        if(availableBulk(product,locationId)<quantity) throw new StockProblem("Không đủ tồn khả dụng để giữ.");
        long id=insertId("INSERT INTO dbo.BulkReservations(SaleLineID,LocationID,Quantity,ExpiresAt) VALUES (?,?,?,?)",saleLineId,locationId,quantity,java.time.LocalDateTime.now(java.time.ZoneOffset.UTC).plusMinutes(minutes));
        audit.writeCurrent("STOCK_RESERVE_BULK","RESERVATION",String.valueOf(id),"Giữ "+quantity+" sản phẩm cho dòng bán "+saleLineId);
        return id;
    }
    @PreAuthorize("hasAuthority('STOCK_ISSUE')")
    @Transactional(isolation=Isolation.SERIALIZABLE)
    public long reserveDevice(long saleLineId,int minutes) {
        validateExpiry(1,minutes);
        var sale=one("""
          SELECT d.DeviceID,o.Status FROM dbo.DeviceSaleLines d JOIN dbo.SalesLines l ON l.SaleLineID=d.SaleLineID
          JOIN dbo.SalesOrders o WITH (UPDLOCK,HOLDLOCK) ON o.OrderID=l.OrderID WHERE d.SaleLineID=?
          """,saleLineId);
        if(!Set.of("DRAFT","CONFIRMED").contains(sale.get("Status"))) throw new StockProblem("Đơn không ở trạng thái được phép giữ máy.");
        long device=((Number)sale.get("DeviceID")).longValue();
        StockLocks.device(db,device);
        StockLocks.expireDeviceHold(db,device);
        Long location=deviceLocation(device);
        if(location==null || !exists("SELECT COUNT(*) FROM dbo.StockLocations WHERE LocationID=? AND IsActive=1 AND LocationType='SALEABLE'",location)) throw new StockProblem("Máy không còn tại kho bán được.");
        if(exists("SELECT COUNT(*) FROM dbo.ActiveDeviceReservations WHERE DeviceID=?",device)) throw new StockProblem("Máy đang có người giữ.");
        long id=insertId("INSERT INTO dbo.DeviceReservations(SaleLineID,ExpiresAt) VALUES (?,?)",saleLineId,java.time.LocalDateTime.now(java.time.ZoneOffset.UTC).plusMinutes(minutes));
        db.update("INSERT INTO dbo.ActiveDeviceReservations(DeviceID,ReservationID) VALUES (?,?)",device,id);
        audit.writeCurrent("STOCK_RESERVE_DEVICE","RESERVATION",String.valueOf(id),"Giữ máy "+device);
        return id;
    }
    @PreAuthorize("hasAuthority('STOCK_ISSUE')")
    @Transactional
    public void release(String kind,long id) {
        if("BULK".equals(kind)) {
            if(db.update("UPDATE dbo.BulkReservations SET Status='RELEASED' WHERE ReservationID=? AND Status='ACTIVE'",id)!=1) throw new StockProblem("Không tìm thấy giữ hàng đang hoạt động.");
        }else if("DEVICE".equals(kind)) {
            if(db.update("UPDATE dbo.DeviceReservations SET Status='RELEASED' WHERE ReservationID=? AND Status='ACTIVE'",id)!=1) throw new StockProblem("Không tìm thấy giữ máy đang hoạt động.");
            db.update("DELETE FROM dbo.ActiveDeviceReservations WHERE ReservationID=?",id);
        }else throw new StockProblem("Loại giữ hàng không hợp lệ.");
        audit.writeCurrent("STOCK_RELEASE","RESERVATION",String.valueOf(id),"Nhả giữ hàng "+kind);
    }

    @Scheduled(fixedDelay=60000)
    @Transactional
    public void expireReservations() {
        db.update("UPDATE dbo.BulkReservations SET Status='EXPIRED' WHERE Status='ACTIVE' AND ExpiresAt<=SYSUTCDATETIME()");
        db.update("UPDATE dbo.DeviceReservations SET Status='EXPIRED' WHERE Status='ACTIVE' AND ExpiresAt<=SYSUTCDATETIME()");
        db.update("DELETE FROM dbo.ActiveDeviceReservations WHERE ReservationID IN (SELECT ReservationID FROM dbo.DeviceReservations WHERE Status<>'ACTIVE' OR ExpiresAt<=SYSUTCDATETIME())");
    }

    /** Physical balance is derived exclusively from POSTED journal entries. */
    public List<Map<String,Object>> balances() { return db.queryForList("""
        SELECT p.SKU,p.Name AS ProductName,l.LocationCode,l.LocationType,
               SUM(e.Delta) AS Quantity,
               SUM(e.Delta)-COALESCE((SELECT SUM(r.Quantity)
                   FROM dbo.BulkReservations r JOIN dbo.BulkSaleLines bl ON bl.SaleLineID=r.SaleLineID
                   WHERE bl.ProductID=e.ProductID AND r.LocationID=e.LocationID
                     AND r.Status='ACTIVE' AND r.ExpiresAt>SYSUTCDATETIME()),0) AS Available
        FROM dbo.BulkStockEntries e
        JOIN dbo.InventoryDocuments d ON d.InventoryDocID=e.InventoryDocID AND d.Status='POSTED'
        JOIN dbo.Products p ON p.ProductID=e.ProductID
        JOIN dbo.StockLocations l ON l.LocationID=e.LocationID
        GROUP BY e.ProductID,e.LocationID,p.SKU,p.Name,l.LocationCode,l.LocationType
        ORDER BY p.SKU,l.LocationCode
        """); }

    public List<Map<String,Object>> devicePositions() { return db.queryForList("""
        SELECT x.DeviceID,d.AssetTag,d.DeviceDescription,l.LocationCode,l.LocationType
        FROM dbo.DeviceStockEntries x JOIN dbo.InventoryDocuments doc ON doc.InventoryDocID=x.InventoryDocID
        JOIN dbo.Devices d ON d.DeviceID=x.DeviceID
        LEFT JOIN dbo.StockLocations l ON l.LocationID=x.ToLocationID
        WHERE doc.Status='POSTED' AND NOT EXISTS (
          SELECT 1 FROM dbo.DeviceStockEntries y JOIN dbo.InventoryDocuments dy ON dy.InventoryDocID=y.InventoryDocID
          WHERE y.DeviceID=x.DeviceID AND dy.Status='POSTED'
          AND (dy.OccurredAt>doc.OccurredAt OR (dy.OccurredAt=doc.OccurredAt AND y.DeviceEntryID>x.DeviceEntryID)))
        ORDER BY d.AssetTag
        """); }

    @PreAuthorize("hasAuthority('STOCK_ADJUST')")
    @Transactional
    public void createLocation(String code,String name,String type) {
        code=normalizeCode(code); name=required(name,180,"Tên vị trí");
        if (!Set.of("SALEABLE","QUARANTINE","DAMAGED").contains(type)) throw new StockProblem("Loại vị trí không hợp lệ.");
        db.update("INSERT INTO dbo.StockLocations(LocationCode,Name,LocationType) VALUES (?,?,?)",code,name,type);
        audit.writeCurrent("STOCK_LOCATION_CREATE","STOCK_LOCATION",code,name);
    }
    @PreAuthorize("hasAuthority('STOCK_ADJUST')")
    @Transactional(isolation=Isolation.SERIALIZABLE)
    public void locationStatus(long locationId,boolean active) {
        activeLocationIfExists(locationId);
        if(!active) {
            if(exists("""
                SELECT COUNT(*) FROM (
                    SELECT e.ProductID FROM dbo.BulkStockEntries e
                    JOIN dbo.InventoryDocuments d ON d.InventoryDocID=e.InventoryDocID AND d.Status='POSTED'
                    WHERE e.LocationID=? GROUP BY e.ProductID HAVING SUM(e.Delta)<>0
                ) balances
                """,locationId)) throw new StockProblem("Vị trí còn hàng BULK, phải chuyển hết trước khi đóng.");
            if(exists("""
                SELECT COUNT(*) FROM dbo.DeviceStockEntries e JOIN dbo.InventoryDocuments d ON d.InventoryDocID=e.InventoryDocID
                WHERE e.ToLocationID=? AND d.Status='POSTED' AND NOT EXISTS (
                    SELECT 1 FROM dbo.DeviceStockEntries n JOIN dbo.InventoryDocuments nd ON nd.InventoryDocID=n.InventoryDocID
                    WHERE n.DeviceID=e.DeviceID AND nd.Status='POSTED' AND n.DeviceEntryID>e.DeviceEntryID)
                """,locationId)) throw new StockProblem("Vị trí còn thiết bị serial, phải chuyển hết trước khi đóng.");
            if(exists("SELECT COUNT(*) FROM dbo.BulkReservations WHERE LocationID=? AND Status='ACTIVE' AND ExpiresAt>SYSUTCDATETIME()",locationId)) throw new StockProblem("Vị trí còn hàng đang được giữ.");
        }
        db.update("UPDATE dbo.StockLocations SET IsActive=? WHERE LocationID=?",active,locationId);
        audit.writeCurrent("STOCK_LOCATION_STATUS","STOCK_LOCATION",String.valueOf(locationId),active?"Mở vị trí kho":"Đóng vị trí kho");
    }

    @PreAuthorize("hasAuthority('STOCK_RECEIVE')")
    @Transactional
    public void createSupplier(String code,String name,String phone) {
        code=normalizeCode(code); name=required(name,180,"Nhà cung cấp");phone=required(phone,25,"Điện thoại");
        if (!phone.matches("\\+?[0-9]{8,15}")) throw new StockProblem("Số điện thoại không hợp lệ.");
        db.update("INSERT INTO dbo.Suppliers(SupplierCode,Name,Phone) VALUES (?,?,?)",code,name,phone);
        audit.writeCurrent("STOCK_SUPPLIER_CREATE","SUPPLIER",code,name);
    }

    @PreAuthorize("hasAnyAuthority('STOCK_RECEIVE','STOCK_ISSUE','STOCK_TRANSFER','STOCK_ADJUST')")
    @Transactional(isolation=Isolation.SERIALIZABLE)
    public long createDraft(StockCommand c) {
        String type=c.type()==null?"":c.type().trim().toUpperCase(Locale.ROOT);
        if (!Set.of("RECEIPT","SALE_ISSUE","TRANSFER","ADJUSTMENT").contains(type)) throw new StockProblem("Loại chứng từ không hợp lệ.");
        String itemType=c.itemType()==null?"":c.itemType().trim().toUpperCase(Locale.ROOT);
        if (!Set.of("BULK","DEVICE").contains(itemType)) throw new StockProblem("Chọn hàng theo số lượng hoặc thiết bị.");
        if (c.quantity()<=0 || c.quantity()>1_000_000 || (itemType.equals("DEVICE") && c.quantity()!=1)) throw new StockProblem("Số lượng không hợp lệ.");
        BigDecimal cost=c.unitCost()==null?BigDecimal.ZERO:c.unitCost();
        if (cost.signum()<0 || cost.scale()>2 || cost.precision()>19) throw new StockProblem("Đơn giá nhập không hợp lệ.");
        String reason=required(c.reason(),2000,"Lý do");
        requireAuthority(type);
        if (type.equals("RECEIPT") && !exists("SELECT COUNT(*) FROM dbo.Suppliers WHERE SupplierID=?",c.supplierId())) throw new StockProblem("Chọn nhà cung cấp hợp lệ.");
        if (type.equals("SALE_ISSUE") && c.saleLineId()==null) throw new StockProblem("Xuất bán phải gắn với dòng đơn bán.");
        if (type.equals("TRANSFER") && (c.fromLocationId()==null || c.toLocationId()==null || c.fromLocationId().equals(c.toLocationId()))) throw new StockProblem("Hai vị trí chuyển kho phải khác nhau.");
        Long from=c.fromLocationId(),to=c.toLocationId();
        if (type.equals("RECEIPT")) { from=null; if(to==null) throw new StockProblem("Nhập kho cần vị trí nhận."); }
        if (type.equals("SALE_ISSUE")) { to=null; if(from==null) throw new StockProblem("Xuất kho cần vị trí xuất."); }
        if (type.equals("ADJUSTMENT") && (from==null)==(to==null)) throw new StockProblem("Điều chỉnh tăng: chỉ chọn vị trí đến; giảm: chỉ chọn vị trí đi.");
        if (from!=null) activeLocation(from);
        if (to!=null) activeLocation(to);
        long actor=actorId();
        String code="INV-"+UUID.randomUUID().toString().replace("-","").substring(0,24).toUpperCase(Locale.ROOT);
        long id=insertId("INSERT INTO dbo.InventoryDocuments(DocumentCode,DocumentType,Status,CreatedBy,Reason) VALUES (?,?,'DRAFT',?,?)",code,type,actor,reason);
        if (type.equals("RECEIPT")) db.update("INSERT INTO dbo.PurchaseReceipts(InventoryDocID,SupplierID) VALUES (?,?)",id,c.supplierId());
        if (type.equals("SALE_ISSUE")) {
            long orderId=saleOrder(c.saleLineId(),itemType,c.productId(),c.deviceId());
            db.update("INSERT INTO dbo.SalesDeliveries(InventoryDocID,OrderID) VALUES (?,?)",id,orderId);
        }
        if (itemType.equals("BULK")) {
            if(c.productId()==null || !exists("SELECT COUNT(*) FROM dbo.Products WHERE ProductID=? AND StockMode='BULK'",c.productId())) throw new StockProblem("Sản phẩm phải có kiểu tồn BULK.");
            if(from!=null) {
                long entry=insertId("INSERT INTO dbo.BulkStockEntries(InventoryDocID,ProductID,LocationID,Delta,UnitCost) VALUES (?,?,?,?,?)",id,c.productId(),from,-c.quantity(),cost);
                if(type.equals("SALE_ISSUE")) db.update("INSERT INTO dbo.SaleBulkIssues(BulkEntryID,SaleLineID) VALUES (?,?)",entry,c.saleLineId());
            }
            if(to!=null) insertId("INSERT INTO dbo.BulkStockEntries(InventoryDocID,ProductID,LocationID,Delta,UnitCost) VALUES (?,?,?,?,?)",id,c.productId(),to,c.quantity(),cost);
        } else {
            if(c.deviceId()==null || !exists("SELECT COUNT(*) FROM dbo.Devices d JOIN dbo.Products p ON p.ProductID=d.ProductID WHERE d.DeviceID=? AND d.OwnerCustomerID IS NULL AND p.StockMode='SERIALIZED'",c.deviceId())) throw new StockProblem("Chỉ quản lý kho máy thuộc cửa hàng và kiểu SERIALIZED.");
            long entry=insertId("INSERT INTO dbo.DeviceStockEntries(InventoryDocID,DeviceID,FromLocationID,ToLocationID,UnitCost) VALUES (?,?,?,?,?)",id,c.deviceId(),from,to,cost);
            if(type.equals("SALE_ISSUE")) db.update("INSERT INTO dbo.SaleDeviceIssues(DeviceEntryID,SaleLineID) VALUES (?,?)",entry,c.saleLineId());
        }
        audit.writeCurrent("STOCK_DRAFT_CREATE","INVENTORY_DOC",code,"Tạo nháp "+type);
        return id;
    }

    @PreAuthorize("hasAnyAuthority('STOCK_RECEIVE','STOCK_ISSUE','STOCK_TRANSFER','STOCK_ADJUST')")
    @Transactional(isolation=Isolation.SERIALIZABLE)
    public void post(long docId) {
        var doc=one("SELECT DocumentCode,DocumentType,Status FROM dbo.InventoryDocuments WHERE InventoryDocID=?",docId);
        if(!"DRAFT".equals(doc.get("Status"))) throw new StockProblem("Chứng từ đã ghi sổ, không thể ghi lần nữa.");
        String type=(String)doc.get("DocumentType"); requireAuthority(type);
        var lines=db.queryForList("SELECT ProductID,LocationID,Delta FROM dbo.BulkStockEntries WHERE InventoryDocID=?",docId);
        var devices=db.queryForList("SELECT DeviceID,FromLocationID,ToLocationID FROM dbo.DeviceStockEntries WHERE InventoryDocID=?",docId);
        if(lines.isEmpty() && devices.isEmpty()) throw new StockProblem("Chứng từ chưa có dòng hàng.");
        StockLocks.all(db,
            lines.stream().map(l->((Number)l.get("ProductID")).longValue()).toList(),
            devices.stream().map(d->((Number)d.get("DeviceID")).longValue()).toList());
        for(long deviceId:devices.stream().map(d->((Number)d.get("DeviceID")).longValue()).distinct().toList())
            StockLocks.expireDeviceHold(db,deviceId);
        for(var line:lines) if(((Number)line.get("Delta")).intValue()<0) {
            long product=((Number)line.get("ProductID")).longValue();long location=((Number)line.get("LocationID")).longValue();
            int amount=-((Number)line.get("Delta")).intValue();
            int allowance=availableBulk(product,location);
            if(type.equals("SALE_ISSUE")) allowance+=db.queryForObject("""
                SELECT COALESCE(SUM(r.Quantity),0) FROM dbo.BulkReservations r
                JOIN dbo.SaleBulkIssues s ON s.SaleLineID=r.SaleLineID JOIN dbo.BulkStockEntries e ON e.BulkEntryID=s.BulkEntryID
                WHERE e.InventoryDocID=? AND e.ProductID=? AND e.LocationID=? AND r.LocationID=e.LocationID
                  AND r.Status='ACTIVE' AND r.ExpiresAt>SYSUTCDATETIME()
                """,Integer.class,docId,product,location);
            if(allowance<amount) throw new StockProblem("Kho không đủ số lượng khả dụng, hoặc hàng đang được giữ.");
        }
        for(var entry:devices) {
            long device=((Number)entry.get("DeviceID")).longValue();
            Long current=deviceLocation(device),from=nullableId(entry.get("FromLocationID"));
            if(!Objects.equals(current,from)) throw new StockProblem("Máy đã thay đổi vị trí. Tải lại trước khi ghi sổ.");
            if(from!=null && exists("SELECT COUNT(*) FROM dbo.ActiveDeviceReservations WHERE DeviceID=?",device)) {
                if(!type.equals("SALE_ISSUE") || !exists("""
                    SELECT COUNT(*) FROM dbo.ActiveDeviceReservations a JOIN dbo.DeviceReservations r ON r.ReservationID=a.ReservationID
                    JOIN dbo.SaleDeviceIssues s ON s.SaleLineID=r.SaleLineID JOIN dbo.DeviceStockEntries e ON e.DeviceEntryID=s.DeviceEntryID
                    WHERE a.DeviceID=? AND e.InventoryDocID=? AND r.Status='ACTIVE' AND r.ExpiresAt>SYSUTCDATETIME()
                    """,device,docId)) throw new StockProblem("Máy đang được giữ cho đơn bán khác.");
            }
        }
        if (type.equals("SALE_ISSUE")) validateSaleQuantities(docId);
        if(db.update("UPDATE dbo.InventoryDocuments SET Status='POSTED',OccurredAt=SYSUTCDATETIME() WHERE InventoryDocID=? AND Status='DRAFT'",docId)!=1) throw new StockProblem("Phiếu đã được xử lý ở phiên khác.");
        if(type.equals("SALE_ISSUE")) consumeSaleReservations(docId);
        audit.writeCurrent("STOCK_POST","INVENTORY_DOC",(String)doc.get("DocumentCode"),"Ghi sổ "+type);
    }

    @PreAuthorize("hasAuthority('STOCK_ADJUST')")
    @Transactional(isolation=Isolation.SERIALIZABLE)
    public long reverse(long originalId,String reason) {
        var doc=one("SELECT DocumentCode,DocumentType,Status FROM dbo.InventoryDocuments WHERE InventoryDocID=?",originalId);
        if(!"POSTED".equals(doc.get("Status"))) throw new StockProblem("Chỉ có thể đảo phiếu đã ghi sổ.");
        if(Set.of("SALE_ISSUE","SALE_RETURN","REPAIR_USE").contains(doc.get("DocumentType"))) throw new StockProblem("Chứng từ bán hàng/sửa chữa có liên kết nghiệp vụ, phải xử lý trả hàng hoặc hoàn tác sử dụng qua quy trình chuyên biệt; không được đảo trực tiếp sổ kho.");
        if("REVERSAL".equals(doc.get("DocumentType")) || exists("SELECT COUNT(*) FROM dbo.InventoryReversals WHERE OriginalDocID=?",originalId)) throw new StockProblem("Phiếu đã đảo hoặc là phiếu đảo.");
        StockLocks.all(db,
            db.queryForList("SELECT ProductID FROM dbo.BulkStockEntries WHERE InventoryDocID=?",originalId).stream()
                .map(r->((Number)r.get("ProductID")).longValue()).toList(),
            db.queryForList("SELECT DeviceID FROM dbo.DeviceStockEntries WHERE InventoryDocID=?",originalId).stream()
                .map(r->((Number)r.get("DeviceID")).longValue()).toList());
        long reversal=insertId("INSERT INTO dbo.InventoryDocuments(DocumentCode,DocumentType,Status,CreatedBy,Reason) VALUES (?,'REVERSAL','DRAFT',?,?)",
            "REV-"+UUID.randomUUID().toString().replace("-","").substring(0,24).toUpperCase(Locale.ROOT),actorId(),required(reason,2000,"Lý do đảo"));
        db.update("INSERT INTO dbo.BulkStockEntries(InventoryDocID,ProductID,LocationID,Delta,UnitCost) SELECT ?,ProductID,LocationID,-Delta,UnitCost FROM dbo.BulkStockEntries WHERE InventoryDocID=?",reversal,originalId);
        db.update("INSERT INTO dbo.DeviceStockEntries(InventoryDocID,DeviceID,FromLocationID,ToLocationID,UnitCost) SELECT ?,DeviceID,ToLocationID,FromLocationID,UnitCost FROM dbo.DeviceStockEntries WHERE InventoryDocID=?",reversal,originalId);
        // A reversal may be posted only while the original movements are physically reversible.
        var bulk=db.queryForList("SELECT ProductID,LocationID,Delta FROM dbo.BulkStockEntries WHERE InventoryDocID=?",reversal);
        for(var row:bulk) if(((Number)row.get("Delta")).intValue()<0 && availableBulk(((Number)row.get("ProductID")).longValue(),((Number)row.get("LocationID")).longValue()) < -((Number)row.get("Delta")).intValue()) throw new StockProblem("Không thể đảo: hàng đã xuất hoặc đã giữ.");
        for(var row:db.queryForList("SELECT DeviceID,FromLocationID FROM dbo.DeviceStockEntries WHERE InventoryDocID=?",reversal)) {
            long device=((Number)row.get("DeviceID")).longValue();
            if(!Objects.equals(deviceLocation(device),nullableId(row.get("FromLocationID")))) throw new StockProblem("Không thể đảo: máy đã di chuyển từ đó.");
            if(exists("SELECT COUNT(*) FROM dbo.ActiveDeviceReservations WHERE DeviceID=?",device)) throw new StockProblem("Không thể đảo: máy đang được giữ cho đơn hàng.");
        }
        db.update("INSERT INTO dbo.InventoryReversals(OriginalDocID,ReversalDocID) VALUES (?,?)",originalId,reversal);
        db.update("UPDATE dbo.InventoryDocuments SET Status='POSTED',OccurredAt=SYSUTCDATETIME() WHERE InventoryDocID=?",reversal);
        audit.writeCurrent("STOCK_REVERSE","INVENTORY_DOC",String.valueOf(originalId),"Đảo phiếu "+doc.get("DocumentCode"));
        return reversal;
    }

    @Transactional(isolation=Isolation.SERIALIZABLE)
    public void useApprovedPart(long quoteItemId,long locationId,int quantity) {
        if(quantity<=0) throw new StockProblem("Số lượng linh kiện phải lớn hơn 0.");
        var item=one("""
            SELECT qi.QuoteID,qp.ProductID,rq.RepairTicketID,rq.Status AS QuoteStatus,t.Status AS TicketStatus,
                   qi.Quantity AS ApprovedQuantity
            FROM dbo.QuoteItems qi JOIN dbo.QuotePartItems qp ON qp.QuoteItemID=qi.QuoteItemID
            JOIN dbo.RepairQuotes rq ON rq.QuoteID=qi.QuoteID
            JOIN dbo.RepairTickets t ON t.RepairTicketID=rq.RepairTicketID WHERE qi.QuoteItemID=?
            """,quoteItemId);
        if(!"ACCEPTED".equals(item.get("QuoteStatus")) || !Set.of("REPAIRING","WAITING_PARTS").contains(item.get("TicketStatus"))) throw new StockProblem("Chỉ xuất linh kiện theo báo giá đã duyệt khi đang sửa.");
        long product=((Number)item.get("ProductID")).longValue();
        StockLocks.product(db,product);
        int approved=((Number)item.get("ApprovedQuantity")).intValue();
        int used=db.queryForObject("SELECT COALESCE(SUM(Quantity),0) FROM dbo.RepairPartUsages WHERE QuoteItemID=?",Integer.class,quoteItemId);
        if(used+quantity>approved) throw new StockProblem("Số lượng xuất vượt báo giá đã duyệt.");
        if(availableBulk(product,locationId)<quantity) throw new StockProblem("Kho không đủ linh kiện còn khả dụng.");
        if(!exists("SELECT COUNT(*) FROM dbo.Products WHERE ProductID=? AND StockMode='BULK'",product)) throw new StockProblem("Linh kiện theo serial cần chọn từng thiết bị, không thể xuất số lượng.");
        long docId=insertId("INSERT INTO dbo.InventoryDocuments(DocumentCode,DocumentType,Status,CreatedBy,Reason) VALUES (?,'REPAIR_USE','POSTED',?,?)",
            "USE-"+UUID.randomUUID().toString().replace("-","").substring(0,24).toUpperCase(Locale.ROOT),actorId(),"Sử dụng linh kiện sửa chữa theo báo giá "+quoteItemId);
        var costs=db.queryForList("""
            SELECT TOP 1 e.UnitCost FROM dbo.BulkStockEntries e
            JOIN dbo.InventoryDocuments d ON d.InventoryDocID=e.InventoryDocID
            WHERE e.ProductID=? AND d.DocumentType='RECEIPT' AND d.Status='POSTED' AND e.Delta>0
            ORDER BY d.OccurredAt DESC,e.BulkEntryID DESC
            """,product);
        BigDecimal cost=costs.isEmpty()?BigDecimal.ZERO:(BigDecimal)costs.getFirst().get("UnitCost");
        long bulkEntry=insertId("INSERT INTO dbo.BulkStockEntries(InventoryDocID,ProductID,LocationID,Delta,UnitCost) VALUES (?,?,?,?,?)",docId,product,locationId,-quantity,cost);
        long partUsage=insertId("INSERT INTO dbo.RepairPartUsages(QuoteItemID,Quantity,UsedBy) VALUES (?,?,?)",quoteItemId,quantity,actorId());
        db.update("INSERT INTO dbo.RepairBulkIssues(BulkEntryID,PartUsageID) VALUES (?,?)",bulkEntry,partUsage);
        audit.writeCurrent("REPAIR_PART_USE","REPAIR_TICKET",String.valueOf(item.get("RepairTicketID")),"Xuất "+quantity+" linh kiện từ kho");
    }

    @Transactional(isolation=Isolation.SERIALIZABLE)
    public void useApprovedDevicePart(long quoteItemId,long deviceId,long locationId) {
        var item=one("""
            SELECT qp.ProductID,rq.RepairTicketID,rq.Status AS QuoteStatus,t.Status AS TicketStatus,qi.Quantity AS ApprovedQuantity
            FROM dbo.QuoteItems qi JOIN dbo.QuotePartItems qp ON qp.QuoteItemID=qi.QuoteItemID
            JOIN dbo.RepairQuotes rq ON rq.QuoteID=qi.QuoteID JOIN dbo.RepairTickets t ON t.RepairTicketID=rq.RepairTicketID
            WHERE qi.QuoteItemID=?
            """,quoteItemId);
        if(!"ACCEPTED".equals(item.get("QuoteStatus")) || !Set.of("REPAIRING","WAITING_PARTS").contains(item.get("TicketStatus"))) throw new StockProblem("Linh kiện chỉ được xuất theo báo giá đã duyệt trong lúc sửa.");
        StockLocks.device(db,deviceId);
        long product=((Number)item.get("ProductID")).longValue();
        if(!exists("""
            SELECT COUNT(*) FROM dbo.Devices d JOIN dbo.Products p ON p.ProductID=d.ProductID
            WHERE d.DeviceID=? AND d.ProductID=? AND d.OwnerCustomerID IS NULL AND p.StockMode='SERIALIZED'
            """,deviceId,product)) throw new StockProblem("Máy linh kiện không thuộc SKU SERIALIZED của báo giá.");
        if(!Objects.equals(deviceLocation(deviceId),locationId)) throw new StockProblem("Máy linh kiện không nằm ở vị trí được chọn.");
        if(exists("SELECT COUNT(*) FROM dbo.ActiveDeviceReservations WHERE DeviceID=?",deviceId)) throw new StockProblem("Máy linh kiện đang được giữ cho đơn bán.");
        int approved=((Number)item.get("ApprovedQuantity")).intValue();
        int used=db.queryForObject("SELECT COALESCE(SUM(Quantity),0) FROM dbo.RepairPartUsages WHERE QuoteItemID=?",Integer.class,quoteItemId);
        if(used>=approved) throw new StockProblem("Đã dùng hết số lượng được duyệt.");
        long docId=insertId("INSERT INTO dbo.InventoryDocuments(DocumentCode,DocumentType,Status,CreatedBy,Reason) VALUES (?,'REPAIR_USE','POSTED',?,?)",
            "USE-"+UUID.randomUUID().toString().replace("-","").substring(0,24).toUpperCase(Locale.ROOT),actorId(),"Sử dụng máy linh kiện "+deviceId+" cho phiếu sửa");
        var costs=db.queryForList("""
            SELECT TOP 1 e.UnitCost FROM dbo.DeviceStockEntries e JOIN dbo.InventoryDocuments d ON d.InventoryDocID=e.InventoryDocID
            WHERE e.DeviceID=? AND d.DocumentType='RECEIPT' AND d.Status='POSTED'
            ORDER BY d.OccurredAt DESC,e.DeviceEntryID DESC
            """,deviceId);
        BigDecimal cost=costs.isEmpty()?BigDecimal.ZERO:(BigDecimal)costs.getFirst().get("UnitCost");
        long deviceEntry=insertId("INSERT INTO dbo.DeviceStockEntries(InventoryDocID,DeviceID,FromLocationID,ToLocationID,UnitCost) VALUES (?,?,?,NULL,?)",docId,deviceId,locationId,cost);
        long partUsage=insertId("INSERT INTO dbo.RepairPartUsages(QuoteItemID,Quantity,UsedBy) VALUES (?,1,?)",quoteItemId,actorId());
        db.update("INSERT INTO dbo.RepairDeviceIssues(DeviceEntryID,PartUsageID) VALUES (?,?)",deviceEntry,partUsage);
        audit.writeCurrent("REPAIR_DEVICE_PART_USE","REPAIR_TICKET",String.valueOf(item.get("RepairTicketID")),"Xuất linh kiện serial "+deviceId);
    }

    private void requireAuthority(String type) {
        String auth=switch(type) {case "RECEIPT" -> "STOCK_RECEIVE";case "SALE_ISSUE" -> "STOCK_ISSUE";case "TRANSFER" -> "STOCK_TRANSFER";default -> "STOCK_ADJUST";};
        var login=org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if(login==null || login.getAuthorities().stream().noneMatch(a->a.getAuthority().equals(auth))) throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền thực hiện thao tác kho.");
    }
    private long saleOrder(long lineId,String type,Long product,Long device) {
        var row=one("SELECT o.OrderID,o.Status FROM dbo.SalesLines l JOIN dbo.SalesOrders o ON o.OrderID=l.OrderID WHERE l.SaleLineID=?",lineId);
        if(!Set.of("CONFIRMED","FULFILLING").contains(row.get("Status"))) throw new StockProblem("Đơn bán chưa được xác nhận.");
        if("BULK".equals(type) && !exists("SELECT COUNT(*) FROM dbo.BulkSaleLines WHERE SaleLineID=? AND ProductID=?",lineId,product)) throw new StockProblem("Dòng bán không khớp sản phẩm.");
        if("DEVICE".equals(type) && !exists("SELECT COUNT(*) FROM dbo.DeviceSaleLines WHERE SaleLineID=? AND DeviceID=?",lineId,device)) throw new StockProblem("Dòng bán không khớp thiết bị.");
        return ((Number)row.get("OrderID")).longValue();
    }
    private void validateSaleQuantities(long docId) {
        for(var row:db.queryForList("""
            SELECT sb.SaleLineID, b.Quantity AS Ordered, SUM(-e.Delta) AS Proposed
            FROM dbo.BulkStockEntries e JOIN dbo.SaleBulkIssues sb ON sb.BulkEntryID=e.BulkEntryID
            JOIN dbo.BulkSaleLines b ON b.SaleLineID=sb.SaleLineID
            WHERE e.InventoryDocID=? GROUP BY sb.SaleLineID,b.Quantity
            """,docId)) {
            long line=((Number)row.get("SaleLineID")).longValue();
            int delivered=db.queryForObject("""
                SELECT COALESCE(SUM(-e.Delta),0) FROM dbo.SaleBulkIssues s
                JOIN dbo.BulkStockEntries e ON e.BulkEntryID=s.BulkEntryID
                JOIN dbo.InventoryDocuments d ON d.InventoryDocID=e.InventoryDocID AND d.Status='POSTED'
                WHERE s.SaleLineID=?
                """,Integer.class,line);
            if(delivered+((Number)row.get("Proposed")).intValue()>((Number)row.get("Ordered")).intValue()) throw new StockProblem("Xuất vượt số lượng đơn bán.");
        }
        for(var row:db.queryForList("SELECT sd.SaleLineID FROM dbo.DeviceStockEntries e JOIN dbo.SaleDeviceIssues sd ON sd.DeviceEntryID=e.DeviceEntryID WHERE e.InventoryDocID=?",docId)) {
            if(exists("SELECT COUNT(*) FROM dbo.SaleDeviceIssues i JOIN dbo.DeviceStockEntries e ON e.DeviceEntryID=i.DeviceEntryID JOIN dbo.InventoryDocuments d ON d.InventoryDocID=e.InventoryDocID AND d.Status='POSTED' WHERE i.SaleLineID=?",row.get("SaleLineID"))) throw new StockProblem("Máy này đã được giao cho đơn bán.");
        }
    }
    private void consumeSaleReservations(long docId) {
        // Consume only shipped quantities; a partially used reservation keeps its remaining quantity.
        for(var row:db.queryForList("""
            SELECT s.SaleLineID,e.LocationID,-e.Delta AS Shipped FROM dbo.SaleBulkIssues s
            JOIN dbo.BulkStockEntries e ON e.BulkEntryID=s.BulkEntryID WHERE e.InventoryDocID=?
            """,docId)) {
            int left=((Number)row.get("Shipped")).intValue();
            var held=db.queryForList("""
                SELECT ReservationID,Quantity FROM dbo.BulkReservations
                WHERE SaleLineID=? AND LocationID=? AND Status='ACTIVE' AND ExpiresAt>SYSUTCDATETIME()
                ORDER BY ReservationID
                """,row.get("SaleLineID"),row.get("LocationID"));
            for(var r:held) {
                if(left<=0)break;
                int qty=((Number)r.get("Quantity")).intValue();
                int consume=Math.min(left,qty);left-=consume;
                if(consume==qty)db.update("UPDATE dbo.BulkReservations SET Status='CONSUMED' WHERE ReservationID=?",r.get("ReservationID"));
                else db.update("UPDATE dbo.BulkReservations SET Quantity=Quantity-? WHERE ReservationID=?",consume,r.get("ReservationID"));
            }
        }
        for(var r:db.queryForList("""
            SELECT dr.ReservationID,a.DeviceID FROM dbo.DeviceReservations dr
            JOIN dbo.ActiveDeviceReservations a ON a.ReservationID=dr.ReservationID
            JOIN dbo.SaleDeviceIssues s ON s.SaleLineID=dr.SaleLineID JOIN dbo.DeviceStockEntries e ON e.DeviceEntryID=s.DeviceEntryID
            WHERE e.InventoryDocID=? AND dr.Status='ACTIVE'
            """,docId)) {
            db.update("UPDATE dbo.DeviceReservations SET Status='CONSUMED' WHERE ReservationID=?",r.get("ReservationID"));
            db.update("DELETE FROM dbo.ActiveDeviceReservations WHERE ReservationID=?",r.get("ReservationID"));
        }
    }
    private int availableBulk(long product,long location) {
        int stock=db.queryForObject("""
           SELECT COALESCE(SUM(e.Delta),0) FROM dbo.BulkStockEntries e JOIN dbo.InventoryDocuments d ON d.InventoryDocID=e.InventoryDocID AND d.Status='POSTED' WHERE e.ProductID=? AND e.LocationID=?
           """,Integer.class,product,location);
        int reserved=db.queryForObject("""
           SELECT COALESCE(SUM(r.Quantity),0) FROM dbo.BulkReservations r JOIN dbo.BulkSaleLines b ON b.SaleLineID=r.SaleLineID
           WHERE b.ProductID=? AND r.LocationID=? AND r.Status='ACTIVE' AND r.ExpiresAt>SYSUTCDATETIME()
           """,Integer.class,product,location);
        return stock-reserved;
    }
    private Long deviceLocation(long device) {
        var rows=db.queryForList("""
           SELECT e.ToLocationID FROM dbo.DeviceStockEntries e JOIN dbo.InventoryDocuments d ON d.InventoryDocID=e.InventoryDocID
           WHERE e.DeviceID=? AND d.Status='POSTED' ORDER BY d.OccurredAt DESC,e.DeviceEntryID DESC
           """,device);
        return rows.isEmpty()?null:nullableId(rows.getFirst().get("ToLocationID"));
    }
    private void activeLocation(long id) {if(!exists("SELECT COUNT(*) FROM dbo.StockLocations WHERE LocationID=? AND IsActive=1",id)) throw new StockProblem("Vị trí kho không tồn tại hoặc đã đóng.");}
    private void activeLocationIfExists(long id) {if(!exists("SELECT COUNT(*) FROM dbo.StockLocations WHERE LocationID=?",id)) throw new StockProblem("Vị trí không tồn tại.");}
    private static void validateExpiry(int qty,int minutes) {if(qty<=0||qty>1_000_000||minutes<5||minutes>10080) throw new StockProblem("Số lượng hoặc thời hạn giữ không hợp lệ (5–10080 phút).");}
    private boolean exists(String sql,Object... args) {return db.queryForObject(sql,Integer.class,args)>0;}
    private Map<String,Object> one(String sql,Object... args) {var rows=db.queryForList(sql,args);if(rows.isEmpty()) throw new StockProblem("Không tìm thấy dữ liệu phù hợp.");return rows.getFirst();}
    private static Long nullableId(Object x) {return x==null?null:((Number)x).longValue();}
    private long actorId() {
        var a=org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if(a==null || !a.isAuthenticated()) throw new StockProblem("Bạn phải đăng nhập.");
        return db.queryForObject("SELECT UserID FROM dbo.Users WHERE LoginName=?",Long.class,a.getName());
    }
    private long insertId(String sql,Object... args) {
        var keys=new GeneratedKeyHolder();
        db.update(con->{PreparedStatement p=con.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS);for(int i=0;i<args.length;i++)p.setObject(i+1,args[i]);return p;},keys);
        if(keys.getKey()==null)throw new StockProblem("Không tạo được mã dữ liệu.");
        return keys.getKey().longValue();
    }
    private static String required(String s,int max,String field) {if(s==null || s.isBlank() || s.trim().length()>max)throw new StockProblem(field+" bắt buộc, tối đa "+max+" ký tự.");return s.trim();}
    private static String normalizeCode(String s) {String c=required(s,40,"Mã").toUpperCase(Locale.ROOT);if(!c.matches("[A-Z0-9_-]{3,40}"))throw new StockProblem("Mã chỉ gồm chữ, số, gạch nối.");return c;}
}
