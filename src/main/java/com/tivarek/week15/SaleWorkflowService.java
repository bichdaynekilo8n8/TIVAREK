package com.tivarek.week15;

import com.tivarek.week12.InventoryService;
import com.tivarek.week12.StockCommand;
import com.tivarek.week8.AuditService;
import com.tivarek.week16.WarrantyIssuanceService;
import com.tivarek.week22.StockLocks;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** All financial and physical mutations are atomic and tied to real posted stock journals. */
@Service
public class SaleWorkflowService {
    private final JdbcTemplate db;
    private final SalesService sales;
    private final InventoryService inventory;
    private final AuditService audit;
    private final WarrantyIssuanceService warranties;
    public SaleWorkflowService(JdbcTemplate db,SalesService sales,InventoryService inventory,AuditService audit,WarrantyIssuanceService warranties) {
        this.db=db;this.sales=sales;this.inventory=inventory;this.audit=audit;this.warranties=warranties;
    }

    @PreAuthorize("hasAuthority('SALE_FULFILL') and hasAuthority('STOCK_ISSUE')")
    @Transactional(isolation=Isolation.SERIALIZABLE)
    public long ship(long orderId,long saleLineId,int quantity,long fromLocation,String carrier,String tracking,String idempotencyKey) {
        var order=lock(orderId,Set.of("CONFIRMED","FULFILLING","COMPLETED"));
        String requestKey=key(idempotencyKey);
        Long duplicate=existingInventoryRequest(requestKey,orderId,saleLineId,"SALE_ISSUE",quantity,fromLocation);
        if(duplicate!=null)return duplicate;
        if("COMPLETED".equals(order.get("Status")))throw new SaleProblem("Đơn đã hoàn tất, không thể xuất thêm hàng.");
        if(quantity<=0 || quantity>10000) throw new SaleProblem("Số lượng giao không hợp lệ.");
        if(fromLocation<=0) throw new SaleProblem("Chọn vị trí kho xuất hàng.");
        var line=line(orderId,saleLineId);
        if(!"BULK".equals(line.get("Kind")) && quantity!=1) throw new SaleProblem("Mỗi máy chỉ được bàn giao một lần.");
        int ordered=Math.toIntExact(SalesService.number(line.get("Quantity")));
        int shipped=Math.toIntExact(SalesService.number(line.get("Shipped")));
        if(shipped+quantity>ordered) throw new SaleProblem("Không được giao vượt số lượng đặt.");
        if("BULK".equals(line.get("Kind"))) {
            int totalHeld=count("SELECT COALESCE(SUM(Quantity),0) FROM dbo.BulkReservations WHERE SaleLineID=? AND Status='ACTIVE' AND ExpiresAt>SYSUTCDATETIME()",saleLineId);
            int heldHere=count("SELECT COALESCE(SUM(Quantity),0) FROM dbo.BulkReservations WHERE SaleLineID=? AND LocationID=? AND Status='ACTIVE' AND ExpiresAt>SYSUTCDATETIME()",saleLineId,fromLocation);
            if(totalHeld>0 && heldHere<quantity)throw new SaleProblem("Hãy xuất từ đúng kho và số lượng đã giữ cho dòng đơn; tránh để tồn giữ không được tiêu thụ.");
        }
        BigDecimal cost=unitCost((String)line.get("Kind"),line);
        long inventoryDoc=inventory.createDraft(new StockCommand("SALE_ISSUE",(String)line.get("Kind"),
            line.get("ProductID")==null?null:SalesService.number(line.get("ProductID")),
            line.get("DeviceID")==null?null:SalesService.number(line.get("DeviceID")),
            fromLocation,null,quantity,cost,null,saleLineId,"Xuất bán đơn "+orderId));
        inventory.post(inventoryDoc); // tuần 12 bảo vệ âm kho, số lượng đã xuất, serial, giữ hàng và audit
        db.update("INSERT INTO dbo.SalesInventoryRequests(RequestKey,OrderID,SaleLineID,OperationType,Quantity,LocationID,InventoryDocID) VALUES (?,?,?,'SALE_ISSUE',?,?,?)",
            requestKey,orderId,saleLineId,quantity,fromLocation,inventoryDoc);
        db.update("UPDATE dbo.SalesDeliveries SET Carrier=?,TrackingCode=? WHERE InventoryDocID=?",
            optional(carrier,100),optional(tracking,100),inventoryDoc);
        syncStatus(orderId);
        audit.writeCurrent("SALE_SHIP","SALES_ORDER",String.valueOf(orderId),"Bàn giao dòng "+saleLineId+", số lượng "+quantity);
        return inventoryDoc;
    }

    @PreAuthorize("hasAuthority('SALE_PAYMENT')")
    @Transactional(isolation=Isolation.SERIALIZABLE)
    public long payment(long orderId,BigDecimal amount,String method,String providerReference,String idempotencyKey) {
        lock(orderId,Set.of("CONFIRMED","FULFILLING","COMPLETED"));
        amount=money(amount);method=method(method);
        String key=key(idempotencyKey);
        String reference=optional(providerReference,150);
        if("BANK_TRANSFER".equals(method) && reference==null) throw new SaleProblem("Nhập mã tham chiếu ngân hàng đã kiểm tra.");
        var old=db.queryForList("SELECT PaymentID,Amount,Method,Direction,Status,ProviderReference FROM dbo.Payments WHERE IdempotencyKey=?",key);
        if(!old.isEmpty()) {
            var previous=old.getFirst();
            if(amount.compareTo((BigDecimal)previous.get("Amount"))==0 && method.equals(previous.get("Method"))
                && "IN".equals(previous.get("Direction")) && "SUCCEEDED".equals(previous.get("Status"))
                && Objects.equals(reference,previous.get("ProviderReference"))
                && count("SELECT COUNT(*) FROM dbo.Payments WHERE PaymentID=? AND OrderID=?",previous.get("PaymentID"),orderId)==1)
                return SalesService.number(previous.get("PaymentID"));
            throw new SaleProblem("Mã giao dịch đã được sử dụng cho yêu cầu khác.");
        }
        SalesService.Detail detail=sales.detail(orderId);
        if(detail.collected().add(amount).compareTo(detail.total())>0) throw new SaleProblem("Thanh toán vượt tổng tiền đơn hàng.");
        BigDecimal alreadyCollected=amountOrZero("SELECT SUM(Amount) FROM dbo.Payments WHERE OrderID=? AND Direction='IN' AND Status='SUCCEEDED'",orderId);
        if(alreadyCollected.add(amount).compareTo(detail.total())>0)
            throw new SaleProblem("Tổng thu gốc không được vượt giá trị đơn, kể cả khi đã hoàn tiền.");
        long id=insert("""
            INSERT INTO dbo.Payments(PaymentCode,OrderID,Direction,Amount,Method,Status,ProviderReference,IdempotencyKey,RecordedBy)
            VALUES (? ,?,'IN',?,?,'SUCCEEDED',?,?,?)
            """,SalesService.code("PAY-"),orderId,amount,method,reference,key,actor());
        syncStatus(orderId);
        audit.writeCurrent("SALE_PAYMENT","SALES_ORDER",String.valueOf(orderId),"Đã xác minh và ghi nhận thu "+amount+" bằng "+method);
        return id;
    }

    @PreAuthorize("hasAuthority('SALE_RETURN')")
    @Transactional(isolation=Isolation.SERIALIZABLE)
    public long receiveReturn(long orderId,long saleLineId,int quantity,long destination,String reason,String idempotencyKey) {
        lock(orderId,Set.of("FULFILLING","COMPLETED"));
        String requestKey=key(idempotencyKey);
        Long duplicate=existingInventoryRequest(requestKey,orderId,saleLineId,"SALE_RETURN",quantity,destination);
        if(duplicate!=null)return duplicate;
        if(quantity<=0 || quantity>10000) throw new SaleProblem("Số lượng trả không hợp lệ.");
        reason=SalesService.required(reason,2000,"Lý do trả hàng");
        if(count("SELECT COUNT(*) FROM dbo.StockLocations WHERE LocationID=? AND LocationType='QUARANTINE' AND IsActive=1",destination)!=1)
            throw new SaleProblem("Hàng trả phải nhập vào khu cách ly đang hoạt động.");
        var l=line(orderId,saleLineId);
        if(SalesService.number(l.get("Returned"))+quantity>SalesService.number(l.get("Shipped")))
            throw new SaleProblem("Số lượng trả vượt số đã giao, sau khi trừ các lần trả trước.");
        String kind=(String)l.get("Kind");
        if("BULK".equals(kind)) StockLocks.product(db,SalesService.number(l.get("ProductID")));
        else StockLocks.device(db,SalesService.number(l.get("DeviceID")));
        if("DEVICE".equals(kind)) {
            if(quantity!=1) throw new SaleProblem("Mỗi thiết bị chỉ nhận lại một lần.");
            long device=SalesService.number(l.get("DeviceID"));
            var last=db.queryForList("""
                SELECT TOP 1 e.ToLocationID FROM dbo.DeviceStockEntries e
                JOIN dbo.InventoryDocuments doc ON doc.InventoryDocID=e.InventoryDocID AND doc.Status='POSTED'
                WHERE e.DeviceID=? ORDER BY doc.OccurredAt DESC,doc.InventoryDocID DESC,e.DeviceEntryID DESC
                """,device);
            if(last.isEmpty() || last.getFirst().get("ToLocationID")!=null) throw new SaleProblem("Thiết bị không còn ở trạng thái đã bán ra.");
        }
        long doc=insert("INSERT INTO dbo.InventoryDocuments(DocumentCode,DocumentType,Status,CreatedBy,Reason) VALUES (?,'SALE_RETURN','POSTED',?,?)",
            SalesService.code("RET-"),actor(),reason);
        db.update("INSERT INTO dbo.SalesReturns(InventoryDocID,OrderID,ReturnReason) VALUES (?,?,?)",doc,orderId,reason);
        if("BULK".equals(kind)) {
            BigDecimal cost=amountOrZero("""
                SELECT TOP 1 e.UnitCost FROM dbo.SaleBulkIssues s
                JOIN dbo.BulkStockEntries e ON e.BulkEntryID=s.BulkEntryID
                JOIN dbo.InventoryDocuments d ON d.InventoryDocID=e.InventoryDocID AND d.Status='POSTED'
                WHERE s.SaleLineID=? ORDER BY d.OccurredAt DESC,e.BulkEntryID DESC
                """,saleLineId);
            long entry=insert("INSERT INTO dbo.BulkStockEntries(InventoryDocID,ProductID,LocationID,Delta,UnitCost) VALUES (?,?,?,?,?)",
                doc,l.get("ProductID"),destination,quantity,cost);
            db.update("INSERT INTO dbo.ReturnBulkEntries(BulkEntryID,SaleLineID) VALUES (?,?)",entry,saleLineId);
        }else {
            BigDecimal cost=amountOrZero("""
                SELECT TOP 1 e.UnitCost FROM dbo.SaleDeviceIssues s
                JOIN dbo.DeviceStockEntries e ON e.DeviceEntryID=s.DeviceEntryID
                JOIN dbo.InventoryDocuments d ON d.InventoryDocID=e.InventoryDocID AND d.Status='POSTED'
                WHERE s.SaleLineID=? ORDER BY d.OccurredAt DESC,e.DeviceEntryID DESC
                """,saleLineId);
            long entry=insert("INSERT INTO dbo.DeviceStockEntries(InventoryDocID,DeviceID,FromLocationID,ToLocationID,UnitCost) VALUES (?,?,NULL,?,?)",
                doc,l.get("DeviceID"),destination,cost);
            db.update("INSERT INTO dbo.ReturnDeviceEntries(DeviceEntryID,SaleLineID) VALUES (?,?)",entry,saleLineId);
        }
        db.update("INSERT INTO dbo.SalesInventoryRequests(RequestKey,OrderID,SaleLineID,OperationType,Quantity,LocationID,InventoryDocID) VALUES (?,?,?,'SALE_RETURN',?,?,?)",
            requestKey,orderId,saleLineId,quantity,destination,doc);
        warranties.voidIfFullyReturned(saleLineId);
        audit.writeCurrent("SALE_RETURN","SALES_ORDER",String.valueOf(orderId),"Nhận trả dòng "+saleLineId+", số lượng "+quantity+" vào khu cách ly");
        return doc;
    }

    /** Refunds must point to real collections and cannot exceed either that payment or returned value. */
    @PreAuthorize("hasAuthority('SALE_REFUND')")
    @Transactional(isolation=Isolation.SERIALIZABLE)
    public long refund(long orderId,long originalPaymentId,BigDecimal amount,String method,String reference,String idempotencyKey) {
        var order=lock(orderId,Set.of("CONFIRMED","FULFILLING","COMPLETED"));
        amount=money(amount);method=method(method);String key=key(idempotencyKey);
        String normalizedReference=optional(reference,150);
        var old=db.queryForList("SELECT PaymentID,Amount,Method,Direction,Status,ProviderReference FROM dbo.Payments WHERE IdempotencyKey=?",key);
        if(!old.isEmpty()) {
            var earlier=old.getFirst();
            if("OUT".equals(earlier.get("Direction")) && "SUCCEEDED".equals(earlier.get("Status"))
                && method.equals(earlier.get("Method")) && amount.compareTo((BigDecimal)earlier.get("Amount"))==0
                && Objects.equals(normalizedReference,earlier.get("ProviderReference"))
                && count("SELECT COUNT(*) FROM dbo.PaymentRefunds r JOIN dbo.Payments p ON p.PaymentID=r.RefundPaymentID WHERE r.RefundPaymentID=? AND r.OriginalPaymentID=? AND p.OrderID=?",earlier.get("PaymentID"),originalPaymentId,orderId)==1)
                return SalesService.number(earlier.get("PaymentID"));
            throw new SaleProblem("Mã hoàn tiền đã được sử dụng.");
        }
        var originals=db.queryForList("SELECT PaymentID,Amount FROM dbo.Payments WHERE PaymentID=? AND OrderID=? AND Direction='IN' AND Status='SUCCEEDED'",originalPaymentId,orderId);
        if(originals.isEmpty()) throw new SaleProblem("Không tìm thấy khoản thu hợp lệ để hoàn.");
        BigDecimal refunded=amountOrZero("""
            SELECT SUM(p.Amount) FROM dbo.PaymentRefunds r JOIN dbo.Payments p ON p.PaymentID=r.RefundPaymentID AND p.Status='SUCCEEDED'
            WHERE r.OriginalPaymentID=?
            """,originalPaymentId);
        if(refunded.add(amount).compareTo((BigDecimal)originals.getFirst().get("Amount"))>0)
            throw new SaleProblem("Tổng tiền hoàn vượt khoản thu ban đầu.");
        // Trước khi giao hàng: nhân viên có thể hoàn toàn bộ để hủy đơn.
        // Sau khi giao hàng: tổng hoàn không được vượt giá trị hàng đã nhận trả.
        boolean delivered=count("SELECT COUNT(*) FROM dbo.SalesDeliveries d JOIN dbo.InventoryDocuments doc ON doc.InventoryDocID=d.InventoryDocID AND doc.Status='POSTED' WHERE d.OrderID=?",orderId)>0;
        if(delivered) {
            SalesService.Detail detail=sales.detail(orderId);
            BigDecimal returnedValue=BigDecimal.ZERO;
            for(var line:detail.lines()) {
                returnedValue=returnedValue.add(SaleRefundMath.maximumRefundable(
                    (BigDecimal)line.get("UnitPrice"),
                    Math.toIntExact(SalesService.number(line.get("Quantity"))),
                    (BigDecimal)line.get("DiscountAmount"),
                    (BigDecimal)line.get("TaxRate"),
                    Math.toIntExact(SalesService.number(line.get("Returned")))));
            }
            BigDecimal already=amountOrZero("SELECT SUM(Amount) FROM dbo.Payments WHERE OrderID=? AND Direction='OUT' AND Status='SUCCEEDED'",orderId);
            if(already.add(amount).compareTo(returnedValue)>0) throw new SaleProblem("Hoàn vượt giá trị hàng đã nhận trả.");
        }
        long id=insert("""
            INSERT INTO dbo.Payments(PaymentCode,OrderID,Direction,Amount,Method,Status,ProviderReference,IdempotencyKey,RecordedBy)
            VALUES (?,?,'OUT',?,?,'SUCCEEDED',?,?,?)
            """,SalesService.code("RFD-"),orderId,amount,method,normalizedReference,key,actor());
        db.update("INSERT INTO dbo.PaymentRefunds(RefundPaymentID,OriginalPaymentID) VALUES (?,?)",id,originalPaymentId);
        syncStatus(orderId);
        audit.writeCurrent("SALE_REFUND","SALES_ORDER",String.valueOf(orderId),"Đã hoàn "+amount+" từ giao dịch "+originalPaymentId);
        return id;
    }

    private void syncStatus(long orderId) {
        var order=db.queryForMap("SELECT Status FROM dbo.SalesOrders WHERE OrderID=?",orderId);
        if(!Set.of("CONFIRMED","FULFILLING","COMPLETED").contains(order.get("Status")))return;
        SalesService.Detail detail=sales.detail(orderId);
        boolean allShipped=!detail.lines().isEmpty() && detail.lines().stream().allMatch(l->SalesService.number(l.get("Shipped"))>=SalesService.number(l.get("Quantity")));
        boolean anyShipped=detail.lines().stream().anyMatch(l->SalesService.number(l.get("Shipped"))>0);
        String next=allShipped && detail.collected().compareTo(detail.total())>=0?"COMPLETED":anyShipped?"FULFILLING":"CONFIRMED";
        if(!next.equals(order.get("Status"))) db.update("""
            UPDATE dbo.SalesOrders SET Status=?,
                CompletedAt=CASE WHEN ?='COMPLETED' THEN SYSUTCDATETIME() ELSE NULL END
            WHERE OrderID=?
            """,next,next,orderId);
        if("COMPLETED".equals(next)) warranties.issueCompletedSale(orderId);
    }

    private Long existingInventoryRequest(String key,long orderId,long saleLineId,String type,int quantity,long locationId) {
        var rows=db.queryForList("SELECT OrderID,SaleLineID,OperationType,Quantity,LocationID,InventoryDocID FROM dbo.SalesInventoryRequests WHERE RequestKey=?",key);
        if(rows.isEmpty())return null;
        var r=rows.getFirst();
        if(SalesService.number(r.get("OrderID"))!=orderId || SalesService.number(r.get("SaleLineID"))!=saleLineId ||
           !type.equals(r.get("OperationType")) || SalesService.number(r.get("Quantity"))!=quantity || SalesService.number(r.get("LocationID"))!=locationId)
            throw new SaleProblem("Mã yêu cầu đã được dùng cho thao tác kho khác.");
        return SalesService.number(r.get("InventoryDocID"));
    }

    private BigDecimal unitCost(String kind,Map<String,Object> line) {
        if("BULK".equals(kind))return amountOrZero("""
            SELECT TOP 1 e.UnitCost FROM dbo.BulkStockEntries e
            JOIN dbo.InventoryDocuments d ON d.InventoryDocID=e.InventoryDocID AND d.Status='POSTED' AND d.DocumentType='RECEIPT'
            WHERE e.ProductID=? AND e.Delta>0 ORDER BY d.OccurredAt DESC,e.BulkEntryID DESC
            """,line.get("ProductID"));
        return amountOrZero("""
            SELECT TOP 1 e.UnitCost FROM dbo.DeviceStockEntries e
            JOIN dbo.InventoryDocuments d ON d.InventoryDocID=e.InventoryDocID AND d.Status='POSTED' AND d.DocumentType='RECEIPT'
            WHERE e.DeviceID=? ORDER BY d.OccurredAt DESC,e.DeviceEntryID DESC
            """,line.get("DeviceID"));
    }

    private Map<String,Object> line(long orderId,long saleLineId) {
        return sales.detail(orderId).lines().stream().filter(l->SalesService.number(l.get("SaleLineID"))==saleLineId)
            .findFirst().orElseThrow(()->new SaleProblem("Dòng hàng không thuộc đơn này."));
    }
    private Map<String,Object> lock(long id,Set<String> states) {
        var orders=db.queryForList("SELECT OrderCode,Status FROM dbo.SalesOrders WITH (UPDLOCK,HOLDLOCK) WHERE OrderID=?",id);
        if(orders.isEmpty()) throw new SaleProblem("Không tìm thấy đơn hàng.");
        var row=orders.getFirst();
        if(!states.contains(row.get("Status"))) throw new SaleProblem("Trạng thái đơn hiện tại không cho phép thao tác này.");
        return row;
    }
    private static BigDecimal money(BigDecimal value) {
        if(value==null || value.signum()<=0 || value.scale()>2 || value.precision()>19) throw new SaleProblem("Số tiền phải dương và tối đa 2 chữ số thập phân.");
        return value;
    }
    private static String method(String value) {
        if(!Set.of("CASH","BANK_TRANSFER").contains(value)) throw new SaleProblem("Chỉ hỗ trợ thu/hoàn tiền mặt hoặc chuyển khoản đã được xác minh; chưa có cổng thanh toán trực tuyến.");
        return value;
    }
    private static String key(String value) {
        if(value==null || !value.matches("[A-Za-z0-9_-]{12,100}")) throw new SaleProblem("Mã chống lặp giao dịch không hợp lệ; tải lại trang và thử lại.");
        return value;
    }
    private static String optional(String value,int max) {
        if(value==null || value.isBlank())return null;
        if(value.trim().length()>max) throw new SaleProblem("Thông tin tham chiếu quá dài.");
        return value.trim();
    }
    private long actor() {
        Authentication a=SecurityContextHolder.getContext().getAuthentication();
        Long id=SalesService.userId(a,db);if(id==null)throw new SaleProblem("Cần đăng nhập nhân viên.");
        return id;
    }
    private int count(String sql,Object... args){return db.queryForObject(sql,Integer.class,args);}
    private BigDecimal amountOrZero(String sql,Object... args){
        var rows=db.query(sql,(rs,n)->rs.getBigDecimal(1),args);
        return rows.isEmpty() || rows.getFirst()==null ? BigDecimal.ZERO : rows.getFirst();
    }
    private long insert(String sql,Object... values){
        var keys=new GeneratedKeyHolder();
        db.update(c->{PreparedStatement p=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS);for(int i=0;i<values.length;i++)p.setObject(i+1,values[i]);return p;},keys);
        if(keys.getKey()==null)throw new SaleProblem("Không tạo được chứng từ.");return keys.getKey().longValue();
    }
}
