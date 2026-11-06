package com.tivarek.week15;

import com.tivarek.week13.StorefrontRepository;
import com.tivarek.week16.WarrantyIssuanceService;
import com.tivarek.week8.AuditService;
import com.tivarek.week22.StockLocks;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Service
public class SalesService {
    public record CartLine(String kind,long id,String name,String code,BigDecimal unitPrice,int quantity,BigDecimal total,int available) { }
    public record CartView(List<CartLine> lines,BigDecimal total) { }
    public record Receipt(long id,String code,String token) { }
    public record Detail(Map<String,Object> header,List<Map<String,Object>> lines,List<Map<String,Object>> payments,
                         List<Map<String,Object>> deliveries,List<Map<String,Object>> returns,BigDecimal total,BigDecimal collected) { }

    private final JdbcTemplate db;
    private final StorefrontRepository storefront;
    private final AuditService audit;
    private final WarrantyIssuanceService warranties;

    public SalesService(JdbcTemplate db,StorefrontRepository storefront,AuditService audit,WarrantyIssuanceService warranties) {
        this.db=db;this.storefront=storefront;this.audit=audit;this.warranties=warranties;
    }

    public CartView preview(Map<String,Integer> cart) {
        if(cart==null || cart.size()>30) throw new SaleProblem("Giỏ hàng không hợp lệ.");
        List<CartLine> lines=new ArrayList<>();BigDecimal total=BigDecimal.ZERO;
        for(var entry:cart.entrySet()) {
            String[] parts=entry.getKey().split(":",-1);
            if(parts.length!=2) throw new SaleProblem("Dữ liệu giỏ hàng không hợp lệ.");
            String kind=parts[0]; long id;
            try{id=Long.parseLong(parts[1]);}catch(NumberFormatException ex){throw new SaleProblem("Mã sản phẩm không hợp lệ.");}
            SaleCart.key(kind,id);
            int quantity=entry.getValue()==null?0:entry.getValue();
            if(quantity<1 || quantity>10000 || (kind.equals("DEVICE") && quantity!=1)) throw new SaleProblem("Số lượng vượt giới hạn.");
            var product=storefront.detail(kind,id);
            if(product.price()==null || product.price().signum()<0) throw new SaleProblem("Mặt hàng chưa có giá bán.");
            BigDecimal lineTotal=product.price().multiply(BigDecimal.valueOf(quantity));
            lines.add(new CartLine(kind,id,product.name(),product.code(),product.price(),quantity,lineTotal,product.available()));
            total=total.add(lineTotal);
        }
        return new CartView(List.copyOf(lines),total);
    }

    /** Cả web và quầy đều tạo đơn DRAFT; nhân viên xác nhận mới khóa tồn. */
    @Transactional(isolation=Isolation.SERIALIZABLE)
    public Receipt create(Map<String,Integer> cart,String name,String phone,String address,String method,
                          boolean counter,Long counterCustomerId,Authentication authentication) {
        if(counter && !has(authentication,"SALE_CREATE")) throw new SaleProblem("Không có quyền lập đơn tại quầy.");
        name=required(name,180,"Tên người nhận");phone=required(phone,25,"Số điện thoại");
        if(!phone.matches("\\+?[0-9]{8,15}")) throw new SaleProblem("Số điện thoại phải có 8–15 chữ số.");
        if(!Set.of("PICKUP","DELIVERY").contains(method)) throw new SaleProblem("Hình thức nhận hàng không hợp lệ.");
        if("DELIVERY".equals(method)) address=required(address,2000,"Địa chỉ giao hàng");
        else address=null;
        CartView view=preview(cart);
        if(view.lines().isEmpty()) throw new SaleProblem("Giỏ hàng trống.");
        for(CartLine line:view.lines()) if(line.available()<line.quantity())
            throw new SaleProblem("Sản phẩm "+line.name()+" không đủ tồn khả dụng.");
        Long actor=userId(authentication),customerId=customerId(authentication);
        if(counter) {
            customerId=null;
            if(counterCustomerId!=null) {
                if(count("SELECT COUNT(*) FROM dbo.Customers WHERE CustomerID=?",counterCustomerId)!=1)
                    throw new SaleProblem("Không tìm thấy hồ sơ khách đã chọn.");
                customerId=counterCustomerId;
            }
        }
        String code=code("SO-");
        long order=insert("INSERT INTO dbo.SalesOrders(OrderCode,CustomerID,Channel,Status,CreatedBy) VALUES (?,?,?,'DRAFT',?)",
            code,customerId,counter?"COUNTER":"WEB",actor);
        db.update("INSERT INTO dbo.OrderShipping(OrderID,RecipientName,RecipientPhone,AddressSnapshot,ShippingFee,FulfillmentMethod) VALUES (?,?,?,?,0,?)",
            order,name,phone,address,method);
        int number=0;
        for(var line:view.lines()) {
            long saleLine=insert("INSERT INTO dbo.SalesLines(OrderID,[LineNo],DescriptionSnapshot,UnitPrice,DiscountAmount,TaxRate,WarrantyDaysSnapshot) VALUES (?,?,?,?,0,0,?)",
                order,++number,trim(line.name(),250),line.unitPrice(),warranties.saleDays(line.kind(),line.id()));
            if(line.kind().equals("BULK")) db.update("INSERT INTO dbo.BulkSaleLines(SaleLineID,ProductID,Quantity) VALUES (?,?,?)",saleLine,line.id(),line.quantity());
            else db.update("INSERT INTO dbo.DeviceSaleLines(SaleLineID,DeviceID) VALUES (?,?)",saleLine,line.id());
        }
        String token=UUID.randomUUID().toString().replace("-","")+UUID.randomUUID().toString().replace("-","");
        db.update("INSERT INTO dbo.SalesTrackingTokens(OrderID,TokenHash) VALUES (?,?)",order,digest(token));
        if(actor!=null) audit.writeCurrent("SALE_CREATE","SALES_ORDER",code,counter?"Lập đơn tại quầy":"Đặt hàng web");
        return new Receipt(order,code,token);
    }

    public List<Map<String,Object>> adminOrders(String status) {
        if(status==null || !Set.of("ALL","DRAFT","CONFIRMED","FULFILLING","COMPLETED","CANCELLED").contains(status)) status="ALL";
        return db.queryForList("""
            SELECT TOP 150 o.OrderID,o.OrderCode,o.Channel,o.Status,o.OrderedAt,
              CONVERT(varchar(16),DATEADD(HOUR,7,o.OrderedAt),120) AS OrderedLocal,
              s.RecipientName,s.RecipientPhone,
              (SELECT COUNT(*) FROM dbo.SalesLines l WHERE l.OrderID=o.OrderID) AS LineCount
            FROM dbo.SalesOrders o JOIN dbo.OrderShipping s ON s.OrderID=o.OrderID
            WHERE (?='ALL' OR o.Status=?) ORDER BY o.OrderID DESC
            """,status,status);
    }

    public List<Map<String,Object>> customerOptions(String query) {
        String q=query==null?"":trim(query.trim(),80);
        String pattern="%"+q.replace("[","[[]").replace("%","[%]").replace("_","[_]")+"%";
        return db.queryForList("""
            SELECT TOP 150 CustomerID,CustomerCode,FullName,Phone FROM dbo.Customers
            WHERE FullName LIKE ? OR Phone LIKE ? OR CustomerCode LIKE ?
            ORDER BY CustomerID DESC
            """,pattern,pattern,pattern);
    }

    /** Draft-only editing: never modify stock-bound or already-posted sale lines. */
    @PreAuthorize("hasAuthority('SALE_CREATE')")
    @Transactional(isolation=Isolation.SERIALIZABLE)
    public void addDraftLine(long orderId,String kind,long itemId,int quantity) {
        draft(orderId);
        SaleCart.key(kind,itemId);
        if(quantity<1 || quantity>10000 || ("DEVICE".equals(kind) && quantity!=1))throw new SaleProblem("Số lượng dòng thêm không hợp lệ.");
        if(count("SELECT COUNT(*) FROM dbo.SalesLines WHERE OrderID=?",orderId)>=30)throw new SaleProblem("Tối đa 30 dòng hàng.");
        var product=publishedProduct(kind,itemId);
        if(product.available()<quantity)throw new SaleProblem("Không đủ tồn khả dụng cho sản phẩm mới.");
        if("DEVICE".equals(kind) && count("""
            SELECT COUNT(*) FROM dbo.DeviceSaleLines d JOIN dbo.SalesLines l ON l.SaleLineID=d.SaleLineID
            WHERE l.OrderID=? AND d.DeviceID=?
            """,orderId,itemId)>0)throw new SaleProblem("Máy đã có trong đơn hàng.");
        Integer lineNo=db.queryForObject("SELECT COALESCE(MAX([LineNo]),0)+1 FROM dbo.SalesLines WHERE OrderID=?",Integer.class,orderId);
        long saleLine=insert("INSERT INTO dbo.SalesLines(OrderID,[LineNo],DescriptionSnapshot,UnitPrice,DiscountAmount,TaxRate,WarrantyDaysSnapshot) VALUES (?,?,?,?,0,0,?)",
            orderId,lineNo,trim(product.name(),250),product.price(),warranties.saleDays(kind,itemId));
        if("BULK".equals(kind))db.update("INSERT INTO dbo.BulkSaleLines(SaleLineID,ProductID,Quantity) VALUES (?,?,?)",saleLine,itemId,quantity);
        else db.update("INSERT INTO dbo.DeviceSaleLines(SaleLineID,DeviceID) VALUES (?,?)",saleLine,itemId);
        audit.writeCurrent("SALE_DRAFT_LINE_ADD","SALES_ORDER",String.valueOf(orderId),"Thêm dòng hàng "+saleLine);
    }

    @PreAuthorize("hasAuthority('SALE_CREATE')")
    @Transactional(isolation=Isolation.SERIALIZABLE)
    public void changeDraftLine(long orderId,long saleLineId,int quantity) {
        draft(orderId);
        var lines=db.queryForList("""
            SELECT b.ProductID,d.DeviceID FROM dbo.SalesLines l
            LEFT JOIN dbo.BulkSaleLines b ON b.SaleLineID=l.SaleLineID
            LEFT JOIN dbo.DeviceSaleLines d ON d.SaleLineID=l.SaleLineID
            WHERE l.OrderID=? AND l.SaleLineID=?
            """,orderId,saleLineId);
        if(lines.isEmpty())throw new SaleProblem("Không tìm thấy dòng hàng trong đơn.");
        var row=lines.getFirst();boolean device=row.get("DeviceID")!=null;
        if(quantity<0 || quantity>10000 || (device && quantity>1))throw new SaleProblem("Số lượng không hợp lệ.");
        if(count("SELECT COUNT(*) FROM dbo.BulkReservations WHERE SaleLineID=?",saleLineId)>0 ||
           count("SELECT COUNT(*) FROM dbo.DeviceReservations WHERE SaleLineID=?",saleLineId)>0)
            throw new SaleProblem("Dòng đã có lịch sử giữ hàng, không thể chỉnh sửa.");
        if(quantity==0){
            if(device)db.update("DELETE FROM dbo.DeviceSaleLines WHERE SaleLineID=?",saleLineId);
            else db.update("DELETE FROM dbo.BulkSaleLines WHERE SaleLineID=?",saleLineId);
            db.update("DELETE FROM dbo.SalesLines WHERE SaleLineID=? AND OrderID=?",saleLineId,orderId);
        }else {
            var product=publishedProduct(device?"DEVICE":"BULK",SalesService.number(device?row.get("DeviceID"):row.get("ProductID")));
            if(product.available()<quantity)throw new SaleProblem("Không đủ tồn khả dụng để tăng số lượng.");
            if(!device)db.update("UPDATE dbo.BulkSaleLines SET Quantity=? WHERE SaleLineID=?",quantity,saleLineId);
        }
        audit.writeCurrent("SALE_DRAFT_LINE_EDIT","SALES_ORDER",String.valueOf(orderId),"Sửa dòng "+saleLineId+" thành số lượng "+quantity);
    }

    private void draft(long orderId) {
        var r=one("SELECT Status FROM dbo.SalesOrders WITH (UPDLOCK,HOLDLOCK) WHERE OrderID=?",orderId);
        if(!"DRAFT".equals(r.get("Status")))throw new SaleProblem("Chỉ có thể chỉnh sửa đơn nháp chưa xác nhận.");
    }
    private com.tivarek.week13.StorefrontProductView publishedProduct(String kind,long itemId) {
        try {return storefront.detail(kind,itemId);}
        catch(com.tivarek.week10.CatalogProblem e){throw new SaleProblem("Sản phẩm không còn được đăng bán.");}
    }

    public List<Map<String,Object>> myOrders(Authentication auth) {
        Long customer=customerId(auth);
        if(customer==null) return List.of();
        return db.queryForList("""
            SELECT o.OrderID,o.OrderCode,o.Status,o.OrderedAt,
              CONVERT(varchar(16),DATEADD(HOUR,7,o.OrderedAt),120) AS OrderedLocal,s.RecipientName
            FROM dbo.SalesOrders o JOIN dbo.OrderShipping s ON s.OrderID=o.OrderID
            WHERE o.CustomerID=? ORDER BY o.OrderID DESC
            """,customer);
    }

    public List<Map<String,Object>> activeReservations(long orderId) {
        return db.queryForList("""
            SELECT r.SaleLineID,l.Name AS LocationName,r.Quantity,r.ExpiresAt
            FROM dbo.BulkReservations r JOIN dbo.SalesLines sl ON sl.SaleLineID=r.SaleLineID
            JOIN dbo.StockLocations l ON l.LocationID=r.LocationID
            WHERE sl.OrderID=? AND r.Status='ACTIVE' AND r.ExpiresAt>SYSUTCDATETIME()
            ORDER BY r.SaleLineID,r.LocationID
            """,orderId);
    }

    public Detail detail(long orderId) {
        var header=one("""
            SELECT o.OrderID,o.OrderCode,o.CustomerID,o.Channel,o.Status,o.OrderedAt,
                   s.RecipientName,s.RecipientPhone,s.AddressSnapshot,s.FulfillmentMethod,s.ShippingFee
            FROM dbo.SalesOrders o JOIN dbo.OrderShipping s ON s.OrderID=o.OrderID WHERE o.OrderID=?
            """,orderId);
        var lines=db.queryForList("""
            SELECT l.SaleLineID,l.[LineNo],l.DescriptionSnapshot,l.UnitPrice,l.DiscountAmount,l.TaxRate,
                   COALESCE(b.Quantity,1) AS Quantity,
                   CASE WHEN b.SaleLineID IS NULL THEN 'DEVICE' ELSE 'BULK' END AS Kind,
                   b.ProductID,d.DeviceID,
                   COALESCE((SELECT SUM(-e.Delta) FROM dbo.SaleBulkIssues si
                     JOIN dbo.BulkStockEntries e ON e.BulkEntryID=si.BulkEntryID
                     JOIN dbo.InventoryDocuments doc ON doc.InventoryDocID=e.InventoryDocID AND doc.Status='POSTED'
                     WHERE si.SaleLineID=l.SaleLineID),
                   (SELECT COUNT(*) FROM dbo.SaleDeviceIssues si
                     JOIN dbo.DeviceStockEntries e ON e.DeviceEntryID=si.DeviceEntryID
                     JOIN dbo.InventoryDocuments doc ON doc.InventoryDocID=e.InventoryDocID AND doc.Status='POSTED'
                     WHERE si.SaleLineID=l.SaleLineID),0) AS Shipped,
                   COALESCE((SELECT SUM(e.Delta) FROM dbo.ReturnBulkEntries r
                     JOIN dbo.BulkStockEntries e ON e.BulkEntryID=r.BulkEntryID
                     JOIN dbo.InventoryDocuments doc ON doc.InventoryDocID=e.InventoryDocID AND doc.Status='POSTED'
                     WHERE r.SaleLineID=l.SaleLineID),
                   (SELECT COUNT(*) FROM dbo.ReturnDeviceEntries r
                     JOIN dbo.DeviceStockEntries e ON e.DeviceEntryID=r.DeviceEntryID
                     JOIN dbo.InventoryDocuments doc ON doc.InventoryDocID=e.InventoryDocID AND doc.Status='POSTED'
                     WHERE r.SaleLineID=l.SaleLineID),0) AS Returned
            FROM dbo.SalesLines l LEFT JOIN dbo.BulkSaleLines b ON b.SaleLineID=l.SaleLineID
            LEFT JOIN dbo.DeviceSaleLines d ON d.SaleLineID=l.SaleLineID WHERE l.OrderID=? ORDER BY l.[LineNo]
            """,orderId);
        var payments=db.queryForList("SELECT PaymentID,PaymentCode,Direction,Amount,Method,Status,OccurredAt,ProviderReference FROM dbo.Payments WHERE OrderID=? ORDER BY PaymentID DESC",orderId);
        var deliveries=db.queryForList("""
            SELECT doc.InventoryDocID,doc.DocumentCode,doc.Status,doc.OccurredAt,d.Carrier,d.TrackingCode
            FROM dbo.SalesDeliveries d JOIN dbo.InventoryDocuments doc ON doc.InventoryDocID=d.InventoryDocID
            WHERE d.OrderID=? ORDER BY doc.InventoryDocID DESC
            """,orderId);
        var returns=db.queryForList("""
            SELECT doc.InventoryDocID,doc.DocumentCode,r.ReturnReason,doc.OccurredAt
            FROM dbo.SalesReturns r JOIN dbo.InventoryDocuments doc ON doc.InventoryDocID=r.InventoryDocID
            WHERE r.OrderID=? ORDER BY doc.InventoryDocID DESC
            """,orderId);
        BigDecimal total=((BigDecimal)header.get("ShippingFee"));
        for(var line:lines) {
            BigDecimal unit=(BigDecimal)line.get("UnitPrice"),discount=(BigDecimal)line.get("DiscountAmount");
            int quantity=Math.toIntExact(((Number)line.get("Quantity")).longValue());
            // DiscountAmount belongs to the entire line, not to each individual unit.
            // Match dbo.vSaleLineAmounts (tax rounded once to two decimal places).
            total=total.add(SaleRefundMath.maximumRefundable(unit,quantity,discount,
                    (BigDecimal)line.get("TaxRate"),quantity));
        }
        BigDecimal paid=BigDecimal.ZERO;
        for(var payment:payments) if("SUCCEEDED".equals(payment.get("Status"))) {
            BigDecimal sum=(BigDecimal)payment.get("Amount");
            paid=paid.add("IN".equals(payment.get("Direction"))?sum:sum.negate());
        }
        return new Detail(header,lines,payments,deliveries,returns,total,paid);
    }

    public Detail guestDetail(String code,String token,Authentication auth) {
        var rows=db.queryForList("SELECT OrderID,CustomerID FROM dbo.SalesOrders WHERE OrderCode=?",code);
        if(rows.isEmpty()) throw new SaleProblem("Không tìm thấy đơn hàng.");
        long order=((Number)rows.getFirst().get("OrderID")).longValue();
        Long customer=customerId(auth);
        Long owner=rows.getFirst().get("CustomerID")==null?null:number(rows.getFirst().get("CustomerID"));
        if(has(auth,"SALE_READ") || (customer!=null && customer.equals(owner))) return detail(order);
        if(token==null || !token.matches("[0-9a-f]{64}")) throw new SaleProblem("Cần mã bảo mật của đơn hàng để tra cứu.");
        var hashes=db.queryForList("SELECT TokenHash FROM dbo.SalesTrackingTokens WHERE OrderID=?",order);
        if(hashes.isEmpty() || !MessageDigest.isEqual((byte[])hashes.getFirst().get("TokenHash"),digest(token)))
            throw new SaleProblem("Mã tra cứu đơn hàng không hợp lệ.");
        return detail(order);
    }

    /** Khóa tồn dưới mức cô lập SERIALIZABLE. Không tin số lượng ở trình duyệt. */
    @PreAuthorize("hasAuthority('SALE_CONFIRM')")
    @Transactional(isolation=Isolation.SERIALIZABLE)
    public void confirm(long orderId) {
        var order=one("SELECT OrderCode,Status FROM dbo.SalesOrders WITH (UPDLOCK,HOLDLOCK) WHERE OrderID=?",orderId);
        if(!"DRAFT".equals(order.get("Status"))) throw new SaleProblem("Chỉ xác nhận đơn nháp.");
        holdLines(orderId);
        db.update("UPDATE dbo.SalesOrders SET Status='CONFIRMED' WHERE OrderID=? AND Status='DRAFT'",orderId);
        audit.writeCurrent("SALE_CONFIRM","SALES_ORDER",(String)order.get("OrderCode"),"Đã xác nhận và giữ hàng");
    }

    @PreAuthorize("hasAuthority('SALE_CONFIRM')")
    @Transactional(isolation=Isolation.SERIALIZABLE)
    public void renewReservations(long orderId) {
        var order=one("SELECT OrderCode,Status FROM dbo.SalesOrders WITH (UPDLOCK,HOLDLOCK) WHERE OrderID=?",orderId);
        if(!"CONFIRMED".equals(order.get("Status"))) throw new SaleProblem("Chỉ có thể giữ lại đơn đã xác nhận nhưng chưa giao.");
        lockOrderStock(orderId);
        db.update("UPDATE dbo.BulkReservations SET Status='RELEASED' WHERE Status='ACTIVE' AND SaleLineID IN (SELECT SaleLineID FROM dbo.SalesLines WHERE OrderID=?)",orderId);
        db.update("DELETE FROM dbo.ActiveDeviceReservations WHERE ReservationID IN (SELECT r.ReservationID FROM dbo.DeviceReservations r JOIN dbo.SalesLines l ON l.SaleLineID=r.SaleLineID WHERE l.OrderID=?)",orderId);
        db.update("UPDATE dbo.DeviceReservations SET Status='RELEASED' WHERE Status='ACTIVE' AND SaleLineID IN (SELECT SaleLineID FROM dbo.SalesLines WHERE OrderID=?)",orderId);
        holdLines(orderId);
        audit.writeCurrent("SALE_RESERVE_RENEW","SALES_ORDER",(String)order.get("OrderCode"),"Làm mới giữ hàng cho đơn");
    }

    private void holdLines(long orderId) {
        var lines=db.queryForList("""
            SELECT l.SaleLineID,b.ProductID,b.Quantity,d.DeviceID FROM dbo.SalesLines l
            LEFT JOIN dbo.BulkSaleLines b ON b.SaleLineID=l.SaleLineID
            LEFT JOIN dbo.DeviceSaleLines d ON d.SaleLineID=l.SaleLineID
            WHERE l.OrderID=? ORDER BY l.SaleLineID
            """,orderId);
        if(lines.isEmpty()) throw new SaleProblem("Đơn hàng không có dòng sản phẩm.");
        lockStock(lines);
        LocalDateTime expiry=LocalDateTime.now(ZoneOffset.UTC).plusDays(7);
        for(var line:lines) {
            long lineId=number(line.get("SaleLineID"));
            if(line.get("ProductID")!=null) {
                long product=number(line.get("ProductID"));int remaining=((Number)line.get("Quantity")).intValue();
                var stocks=db.queryForList("""
                    SELECT l.LocationID,
                           COALESCE(SUM(e.Delta),0)-COALESCE((SELECT SUM(r.Quantity) FROM dbo.BulkReservations r
                             JOIN dbo.BulkSaleLines b ON b.SaleLineID=r.SaleLineID
                             WHERE b.ProductID=? AND r.LocationID=l.LocationID AND r.Status='ACTIVE' AND r.ExpiresAt>SYSUTCDATETIME()),0) AS Available
                    FROM dbo.StockLocations l JOIN dbo.BulkStockEntries e ON e.LocationID=l.LocationID AND e.ProductID=?
                    JOIN dbo.InventoryDocuments doc ON doc.InventoryDocID=e.InventoryDocID AND doc.Status='POSTED'
                    WHERE l.LocationType='SALEABLE' AND l.IsActive=1
                    GROUP BY l.LocationID ORDER BY l.LocationID
                    """,product,product);
                for(var stock:stocks) {
                    int qty=(int)Math.min(remaining,Math.max(0,number(stock.get("Available"))));
                    if(qty==0)continue;
                    db.update("INSERT INTO dbo.BulkReservations(SaleLineID,LocationID,Quantity,ExpiresAt) VALUES (?,?,?,?)",lineId,stock.get("LocationID"),qty,expiry);
                    remaining-=qty;if(remaining==0)break;
                }
                if(remaining>0) throw new SaleProblem("Không đủ hàng để xác nhận đơn; hãy nhập bổ sung vào kho bán.");
            } else if(line.get("DeviceID")!=null) {
                long device=number(line.get("DeviceID"));
                StockLocks.expireDeviceHold(db,device);
                var positions=db.queryForList("""
                    SELECT TOP 1 e.ToLocationID,l.LocationType,l.IsActive FROM dbo.DeviceStockEntries e
                    JOIN dbo.InventoryDocuments doc ON doc.InventoryDocID=e.InventoryDocID AND doc.Status='POSTED'
                    LEFT JOIN dbo.StockLocations l ON l.LocationID=e.ToLocationID
                    WHERE e.DeviceID=? ORDER BY doc.OccurredAt DESC,doc.InventoryDocID DESC,e.DeviceEntryID DESC
                    """,device);
                if(positions.isEmpty() || !"SALEABLE".equals(positions.getFirst().get("LocationType")) || !Boolean.TRUE.equals(positions.getFirst().get("IsActive")))
                    throw new SaleProblem("Máy không có trong kho bán được.");
                if(count("SELECT COUNT(*) FROM dbo.ActiveDeviceReservations WHERE DeviceID=?",device)>0)
                    throw new SaleProblem("Máy đã được giữ cho đơn hàng khác.");
                long hold=insert("INSERT INTO dbo.DeviceReservations(SaleLineID,ExpiresAt) VALUES (?,?)",lineId,expiry);
                db.update("INSERT INTO dbo.ActiveDeviceReservations(DeviceID,ReservationID) VALUES (?,?)",device,hold);
            } else throw new SaleProblem("Dòng đơn không có mặt hàng hợp lệ.");
        }
    }

    @PreAuthorize("hasAuthority('SALE_CONFIRM')")
    @Transactional(isolation=Isolation.SERIALIZABLE)
    public void cancel(long orderId) {
        var order=one("SELECT OrderCode,Status FROM dbo.SalesOrders WITH (UPDLOCK,HOLDLOCK) WHERE OrderID=?",orderId);
        if(!Set.of("DRAFT","CONFIRMED").contains(order.get("Status"))) throw new SaleProblem("Đơn đã giao hoặc đã kết thúc, không thể hủy trực tiếp.");
        lockOrderStock(orderId);
        if(count("SELECT COUNT(*) FROM dbo.SalesDeliveries d JOIN dbo.InventoryDocuments doc ON doc.InventoryDocID=d.InventoryDocID AND doc.Status='POSTED' WHERE d.OrderID=?",orderId)>0)
            throw new SaleProblem("Đơn đã xuất hàng; hãy dùng quy trình trả hàng.");
        if(detail(orderId).collected().signum()!=0) throw new SaleProblem("Hãy hoàn tiền trước khi hủy đơn đã thanh toán.");
        db.update("UPDATE dbo.BulkReservations SET Status='RELEASED' WHERE Status='ACTIVE' AND SaleLineID IN (SELECT SaleLineID FROM dbo.SalesLines WHERE OrderID=?)",orderId);
        db.update("DELETE FROM dbo.ActiveDeviceReservations WHERE ReservationID IN (SELECT r.ReservationID FROM dbo.DeviceReservations r JOIN dbo.SalesLines l ON l.SaleLineID=r.SaleLineID WHERE l.OrderID=?)",orderId);
        db.update("UPDATE dbo.DeviceReservations SET Status='RELEASED' WHERE Status='ACTIVE' AND SaleLineID IN (SELECT SaleLineID FROM dbo.SalesLines WHERE OrderID=?)",orderId);
        db.update("UPDATE dbo.SalesOrders SET Status='CANCELLED' WHERE OrderID=?",orderId);
        audit.writeCurrent("SALE_CANCEL","SALES_ORDER",(String)order.get("OrderCode"),"Đã hủy đơn và nhả giữ tồn");
    }

    private void lockOrderStock(long orderId) {
        var lines=db.queryForList("""
            SELECT b.ProductID,d.DeviceID FROM dbo.SalesLines l
            LEFT JOIN dbo.BulkSaleLines b ON b.SaleLineID=l.SaleLineID
            LEFT JOIN dbo.DeviceSaleLines d ON d.SaleLineID=l.SaleLineID
            WHERE l.OrderID=?
            """,orderId);
        lockStock(lines);
    }

    private void lockStock(List<Map<String,Object>> lines) {
        StockLocks.all(db,
            lines.stream().filter(l->l.get("ProductID")!=null)
                .map(l->number(l.get("ProductID"))).toList(),
            lines.stream().filter(l->l.get("DeviceID")!=null)
                .map(l->number(l.get("DeviceID"))).toList());
    }

    public static String required(String value,int max,String label) {
        if(value==null || value.isBlank() || value.trim().length()>max) throw new SaleProblem(label+" bắt buộc, tối đa "+max+" ký tự.");
        return value.trim();
    }
    public static String trim(String value,int max) {return value.length()<=max?value:value.substring(0,max);}
    public static String code(String prefix) {return prefix+UUID.randomUUID().toString().replace("-","").substring(0,24).toUpperCase(Locale.ROOT);}
    public static long number(Object value) {return ((Number)value).longValue();}
    public static Long userId(Authentication auth,JdbcTemplate db) {
        if(auth==null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName()))return null;
        var ids=db.queryForList("SELECT UserID FROM dbo.Users WHERE LoginName=? AND IsActive=1",Long.class,auth.getName());
        return ids.isEmpty()?null:ids.getFirst();
    }
    private Long userId(Authentication auth) {return userId(auth,db);}
    private Long customerId(Authentication auth) {
        Long id=userId(auth);if(id==null)return null;
        var ids=db.queryForList("SELECT TOP 1 CustomerID FROM dbo.Customers WHERE UserID=?",Long.class,id);
        return ids.isEmpty()?null:ids.getFirst();
    }
    public static boolean has(Authentication auth,String authority) {
        return auth!=null && auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals(authority));
    }
    private static byte[] digest(String token) {
        try{return MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.US_ASCII));}
        catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
    private int count(String sql,Object... params){return db.queryForObject(sql,Integer.class,params);}
    private Map<String,Object> one(String sql,Object... params){var r=db.queryForList(sql,params);if(r.isEmpty())throw new SaleProblem("Không tìm thấy đơn hàng.");return r.getFirst();}
    private long insert(String sql,Object... values){
        var keys=new GeneratedKeyHolder();
        db.update(c->{PreparedStatement p=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS);for(int i=0;i<values.length;i++)p.setObject(i+1,values[i]);return p;},keys);
        if(keys.getKey()==null)throw new SaleProblem("Không tạo được bản ghi đơn hàng.");
        return keys.getKey().longValue();
    }
}
