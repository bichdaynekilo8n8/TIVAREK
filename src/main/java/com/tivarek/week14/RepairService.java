package com.tivarek.week14;

import com.tivarek.week8.AuditService;
import com.tivarek.week16.WarrantyIssuanceService;
import com.tivarek.week12.InventoryService;
import com.tivarek.week12.StockProblem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Service
public class RepairService {
    private final JdbcTemplate db;
    private final AuditService audit;
    private final InventoryService stock;
    private final SecureRandom random=new SecureRandom();
    private final WarrantyIssuanceService warranties;
    public RepairService(JdbcTemplate db,AuditService audit,InventoryService stock,WarrantyIssuanceService warranties) {this.db=db;this.audit=audit;this.stock=stock;this.warranties=warranties;}

    public record IntakeReceipt(long id,String ticketCode,String trackingCode) { }

    @Transactional(isolation=Isolation.SERIALIZABLE)
    public String request(String name,String phone,String deviceAndProblem,String preferredAt) {
        name=required(name,180,"Họ tên");phone=phone(phone);deviceAndProblem=required(deviceAndProblem,2000,"Thiết bị và lỗi");
        LocalDateTime appointment=null;
        if(preferredAt!=null && !preferredAt.isBlank()) {
            try {appointment=LocalDateTime.parse(preferredAt);}
            catch(java.time.format.DateTimeParseException e){throw new RepairProblem("Thời gian hẹn không hợp lệ.");}
            if(appointment.isBefore(LocalDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"))) || appointment.isAfter(LocalDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).plusMonths(6))) throw new RepairProblem("Chọn lịch hẹn trong vòng 6 tháng tới.");
        }
        int recent=db.queryForObject("SELECT COUNT(*) FROM dbo.RepairRequests WITH (UPDLOCK,HOLDLOCK) WHERE ContactPhone=? AND CreatedAt>DATEADD(MINUTE,-10,SYSUTCDATETIME())",Integer.class,phone);
        if(recent>=3) throw new RepairProblem("Số điện thoại đã gửi nhiều yêu cầu gần đây. Vui lòng thử lại sau 10 phút.");
        String code=randomCode("REQ");
        Long owner=ownCustomerId();
        db.update("INSERT INTO dbo.RepairRequests(RequestCode,CustomerID,ContactName,ContactPhone,DeviceAndProblem,PreferredAt,Status) VALUES (?,?,?,?,?,?,'NEW')",code,owner,name,phone,deviceAndProblem,appointment);
        db.update("INSERT INTO dbo.AuditLogs(ActorID,Action,EntityType,EntityKey,Summary) VALUES (NULL,'REPAIR_REQUEST_CREATE','REPAIR_REQUEST',?,N'Có yêu cầu sửa chữa trực tuyến')",code);
        return code;
    }

    @PreAuthorize("hasAuthority('REPAIR_READ')")
    public List<Map<String,Object>> requests() {return db.queryForList("SELECT TOP 100 RequestID,RequestCode,ContactName,ContactPhone,DeviceAndProblem,Status,CreatedAt FROM dbo.RepairRequests ORDER BY RequestID DESC");}
    @PreAuthorize("hasAuthority('REPAIR_READ')")
    public List<Map<String,Object>> tickets() {return db.queryForList("""
        SELECT TOP 100 t.RepairTicketID,t.TicketCode,t.Status,t.ReceivedAt,c.FullName,d.AssetTag,d.DeviceDescription,u.LoginName AS Technician
        FROM dbo.RepairTickets t JOIN dbo.Customers c ON c.CustomerID=t.CustomerID
        JOIN dbo.Devices d ON d.DeviceID=t.DeviceID LEFT JOIN dbo.Users u ON u.UserID=t.AssignedTechnicianID
        ORDER BY t.RepairTicketID DESC
        """);}
    @PreAuthorize("hasAuthority('REPAIR_READ')")
    public List<Map<String,Object>> customers() {return db.queryForList("SELECT TOP 100 CustomerID,CustomerCode,FullName,Phone FROM dbo.Customers ORDER BY CustomerID DESC");}
    @PreAuthorize("hasAuthority('REPAIR_READ')")
    public List<Map<String,Object>> technicians() {return db.queryForList("""
        SELECT u.UserID,u.LoginName,u.DisplayName FROM dbo.Users u JOIN dbo.UserRoles ur ON ur.UserID=u.UserID
        JOIN dbo.Roles r ON r.RoleID=ur.RoleID WHERE r.RoleCode='TECHNICIAN' AND u.IsActive=1 ORDER BY u.DisplayName
        """);}
    @PreAuthorize("hasAuthority('CUSTOMER_SELF_READ')")
    public List<Map<String,Object>> ownTickets() {
        Long id=ownCustomerId();if(id==null)return List.of();
        return db.queryForList("""
          SELECT t.RepairTicketID,t.TicketCode,t.Status,t.ReceivedAt,d.DeviceDescription
          FROM dbo.RepairTickets t JOIN dbo.Devices d ON d.DeviceID=t.DeviceID
          WHERE t.CustomerID=? ORDER BY t.RepairTicketID DESC
          """,id);
    }
    @PreAuthorize("hasAuthority('CUSTOMER_SELF_READ')")
    public List<Map<String,Object>> ownQuotes() {
        Long id=ownCustomerId();if(id==null)return List.of();
        return db.queryForList("""
          SELECT q.QuoteID,t.TicketCode,q.QuoteNo,q.Status,q.ValidUntil,
          (SELECT COALESCE(SUM(ROUND((i.Quantity*i.UnitPrice-i.DiscountAmount)*(1+i.TaxRate/100),2)),0) FROM dbo.QuoteItems i WHERE i.QuoteID=q.QuoteID) AS Total
          FROM dbo.RepairQuotes q JOIN dbo.RepairTickets t ON t.RepairTicketID=q.RepairTicketID
          WHERE t.CustomerID=? ORDER BY q.QuoteID DESC
          """,id);
    }
    @PreAuthorize("hasAuthority('CUSTOMER_SELF_READ')")
    public List<Map<String,Object>> ownQuoteLines() {
        Long id=ownCustomerId();if(id==null)return List.of();
        return db.queryForList("""
            SELECT q.QuoteID,t.TicketCode,i.DescriptionSnapshot,i.Quantity,i.UnitPrice,i.DiscountAmount,
                   i.TaxRate,i.WarrantyDaysSnapshot
            FROM dbo.QuoteItems i JOIN dbo.RepairQuotes q ON q.QuoteID=i.QuoteID
            JOIN dbo.RepairTickets t ON t.RepairTicketID=q.RepairTicketID
            WHERE t.CustomerID=? AND q.Status IN ('SENT','ACCEPTED','REJECTED') ORDER BY q.QuoteID DESC,i.[LineNo]
            """,id);
    }
    @PreAuthorize("hasAuthority('REPAIR_READ')")
    public List<Map<String,Object>> catalogServices() {return db.queryForList("SELECT ServiceID,Name,SuggestedPrice,DefaultWarrantyDays FROM dbo.Services ORDER BY Name");}
    @PreAuthorize("hasAuthority('REPAIR_READ')")
    public List<Map<String,Object>> parts() {return db.queryForList("SELECT ProductID,Name,SKU,StockMode FROM dbo.Products WHERE IsActive=1 ORDER BY Name");}
    @PreAuthorize("hasAuthority('REPAIR_QUOTE')")
    @Transactional
    public void addService(String code,String name,BigDecimal price,int days) {
        code=required(code,40,"Mã dịch vụ").toUpperCase(Locale.ROOT);name=required(name,180,"Tên dịch vụ");
        if(!code.matches("[A-Z0-9_-]{3,40}") || price==null || price.signum()<0 || price.scale()>2 || days<0 || days>3650)throw new RepairProblem("Mã, giá hoặc số ngày bảo hành không hợp lệ.");
        db.update("INSERT INTO dbo.Services(ServiceCode,Name,SuggestedPrice,DefaultWarrantyDays) VALUES (?,?,?,?)",code,name,price,days);
        audit.writeCurrent("REPAIR_SERVICE_CREATE","SERVICE",code,name);
    }
    @PreAuthorize("hasAuthority('REPAIR_INTAKE')")
    @Transactional
    public void updateRequest(long id,String status) {
        if(status==null || !Set.of("CONTACTED","CANCELLED").contains(status))throw new RepairProblem("Chỉ cho phép đánh dấu đã liên hệ hoặc hủy.");
        if(db.update("UPDATE dbo.RepairRequests SET Status=? WHERE RequestID=? AND Status IN ('NEW','CONTACTED')",status,id)!=1)throw new RepairProblem("Yêu cầu đã xử lý hoặc không tồn tại.");
        audit.writeCurrent("REPAIR_REQUEST_STATUS","REPAIR_REQUEST",String.valueOf(id),status);
    }
    @PreAuthorize("hasAuthority('REPAIR_READ')")
    public Map<String,Object> ticket(long id) {return one("""
        SELECT t.RepairTicketID,t.TicketCode,t.Status,t.ReceivedAt,t.PromisedAt,t.ClosedAt,t.ReportedProblem,t.IntakeCondition,
               c.FullName,c.Phone,d.AssetTag,d.DeviceDescription,d.ManufacturerSerial
        FROM dbo.RepairTickets t JOIN dbo.Customers c ON c.CustomerID=t.CustomerID
        JOIN dbo.Devices d ON d.DeviceID=t.DeviceID WHERE t.RepairTicketID=?
        """,id);}
    @PreAuthorize("hasAuthority('REPAIR_READ')")
    public List<Map<String,Object>> history(long id) {return db.queryForList("SELECT FromStatus,ToStatus,Note,IsPublic,OccurredAt FROM dbo.RepairEvents WHERE RepairTicketID=? ORDER BY RepairEventID",id);}
    @PreAuthorize("hasAuthority('REPAIR_READ')")
    public List<Map<String,Object>> quotes(long id) {return db.queryForList("""
        SELECT q.QuoteID,q.QuoteNo,q.Status,q.CreatedAt,q.ValidUntil,q.RespondedAt,q.ApprovalEvidence,
               (SELECT COALESCE(SUM(ROUND((i.Quantity*i.UnitPrice-i.DiscountAmount)*(1+i.TaxRate/100),2)),0) FROM dbo.QuoteItems i WHERE i.QuoteID=q.QuoteID) AS Total
        FROM dbo.RepairQuotes q WHERE q.RepairTicketID=? ORDER BY q.QuoteNo DESC
        """,id);}
    @PreAuthorize("hasAuthority('REPAIR_READ')")
    public List<Map<String,Object>> quoteLines(long id) {return db.queryForList("""
        SELECT i.QuoteItemID,i.[LineNo],i.DescriptionSnapshot,i.Quantity,i.UnitPrice,i.DiscountAmount,i.TaxRate,
               p.ProductID,s.ServiceID FROM dbo.QuoteItems i
        LEFT JOIN dbo.QuotePartItems p ON p.QuoteItemID=i.QuoteItemID
        LEFT JOIN dbo.QuoteServiceItems s ON s.QuoteItemID=i.QuoteItemID
        JOIN dbo.RepairQuotes q ON q.QuoteID=i.QuoteID WHERE q.RepairTicketID=? ORDER BY q.QuoteNo,i.[LineNo]
        """,id);}
    @PreAuthorize("hasAuthority('REPAIR_READ')")
    public List<Map<String,Object>> workLogs(long id) {return db.queryForList("SELECT w.WorkDescription,w.StartedAt,w.FinishedAt,u.DisplayName FROM dbo.RepairWorkLogs w JOIN dbo.Users u ON u.UserID=w.TechnicianID WHERE w.RepairTicketID=? ORDER BY w.WorkLogID DESC",id);}
    @PreAuthorize("hasAuthority('REPAIR_READ')")
    public BigDecimal due(long id) {
        var quoteTotal=db.queryForObject("""
            SELECT COALESCE(SUM(ROUND((i.Quantity*i.UnitPrice-i.DiscountAmount)*(1+i.TaxRate/100),2)),0)
            FROM dbo.QuoteItems i JOIN dbo.RepairQuotes q ON q.QuoteID=i.QuoteID
            WHERE q.RepairTicketID=? AND q.Status='ACCEPTED'
            """,BigDecimal.class,id);
        var paid=db.queryForObject("""
            SELECT COALESCE(SUM(CASE WHEN Direction='IN' THEN Amount ELSE -Amount END),0)
            FROM dbo.Payments WHERE RepairTicketID=? AND Status='SUCCEEDED'
            """,BigDecimal.class,id);
        return quoteTotal.subtract(paid).max(BigDecimal.ZERO);
    }

    @PreAuthorize("hasAuthority('REPAIR_INTAKE')")
    @Transactional(isolation=Isolation.SERIALIZABLE)
    public IntakeReceipt intake(Long requestId,Long customerId,String newName,String newPhone,String model,
             String serial,String problem,String condition,String accessories,Long technicianId) {
        model=required(model,2000,"Mô tả máy");problem=required(problem,2000,"Lỗi báo");condition=required(condition,2000,"Tình trạng lúc nhận");
        if(requestId!=null) {
            var request=one("SELECT Status,CustomerID FROM dbo.RepairRequests WITH (UPDLOCK,HOLDLOCK) WHERE RequestID=?",requestId);
            if(!Set.of("NEW","CONTACTED","ACCEPTED").contains(request.get("Status"))) throw new RepairProblem("Yêu cầu đã hủy.");
            if(exists("SELECT COUNT(*) FROM dbo.RepairTickets WHERE RequestID=?",requestId)) throw new RepairProblem("Yêu cầu đã tạo phiếu tiếp nhận.");
            if(customerId==null && request.get("CustomerID")!=null) customerId=((Number)request.get("CustomerID")).longValue();
        }
        if(customerId==null) {
            newName=required(newName,180,"Tên khách hàng");newPhone=phone(newPhone);
            customerId=insertId("INSERT INTO dbo.Customers(CustomerCode,FullName,Phone) VALUES (?,?,?)",randomCode("KH"),newName,newPhone);
        } else if(!exists("SELECT COUNT(*) FROM dbo.Customers WHERE CustomerID=?",customerId)) throw new RepairProblem("Không tìm thấy khách hàng.");
        if(technicianId!=null) checkTechnician(technicianId);
        if(serial!=null && serial.strip().length()>100) throw new RepairProblem("Serial tối đa 100 ký tự.");
        long deviceId=insertId("INSERT INTO dbo.Devices(AssetTag,ProductID,OwnerCustomerID,ManufacturerSerial,DeviceDescription,ConditionGrade,FaultDescription) VALUES (?,NULL,?,?,?,'FAULTY',?)",
            randomCode("FIX"),customerId,serial==null||serial.isBlank()?null:serial.strip(),model,problem);
        String code=randomCode("REP");String token=newTrackingToken();
        long ticketId=insertId("""
            INSERT INTO dbo.RepairTickets(TicketCode,DeviceID,CustomerID,RequestID,AssignedTechnicianID,ReceivedBy,ReportedProblem,IntakeCondition,TrackingTokenHash)
            VALUES (?,?,?,?,?,?,?,?,?)
            """,code,deviceId,customerId,requestId,technicianId,actor(),problem,condition,hash(token));
        if(accessories!=null && !accessories.isBlank()) {
            String[] parts=accessories.split("\\r?\\n");if(parts.length>30) throw new RepairProblem("Tối đa 30 dòng phụ kiện.");
            int i=0;for(String p:parts) if(!p.isBlank()) db.update("INSERT INTO dbo.RepairAccessories(RepairTicketID,ItemNo,Description,Quantity) VALUES (?,?,?,1)",ticketId,++i,required(p,250,"Phụ kiện"));
        }
        event(ticketId,null,"RECEIVED","Đã tiếp nhận thiết bị và phụ kiện",true);
        if(requestId!=null) db.update("UPDATE dbo.RepairRequests SET Status='ACCEPTED' WHERE RequestID=?",requestId);
        audit.writeCurrent("REPAIR_INTAKE","REPAIR_TICKET",code,"Nhận máy của khách hàng #"+customerId);
        return new IntakeReceipt(ticketId,code,token);
    }

    @PreAuthorize("hasAuthority('REPAIR_ASSIGN')")
    @Transactional
    public void assign(long ticketId,long technicianId) {
        checkTechnician(technicianId);var t=lockedTicket(ticketId);
        if(Set.of("DELIVERED","CANCELLED").contains(t.get("Status"))) throw new RepairProblem("Phiếu đã đóng.");
        db.update("UPDATE dbo.RepairTickets SET AssignedTechnicianID=? WHERE RepairTicketID=?",technicianId,ticketId);
        event(ticketId,(String)t.get("Status"),(String)t.get("Status"),"Đã phân công kỹ thuật viên",false);
        audit.writeCurrent("REPAIR_ASSIGN","REPAIR_TICKET",String.valueOf(ticketId),"Phân công #"+technicianId);
    }

    @PreAuthorize("hasAuthority('REPAIR_WORK')")
    @Transactional(isolation=Isolation.SERIALIZABLE)
    public void status(long ticketId,String next,String note,boolean publicNote) {
        var t=lockedTicket(ticketId);String current=(String)t.get("Status");next=required(next,24,"Trạng thái");
        var transitions=Map.of(
            "RECEIVED",Set.of("DIAGNOSING","CANCELLED"),
            "DIAGNOSING",Set.of("WAITING_APPROVAL","REPAIRING","CANCELLED"),
            "WAITING_APPROVAL",Set.of("REPAIRING","CANCELLED"),
            "REPAIRING",Set.of("WAITING_PARTS","READY","CANCELLED"),
            "WAITING_PARTS",Set.of("REPAIRING","CANCELLED"),
            "READY",Set.of("REPAIRING","CANCELLED"),
            "DELIVERED",Set.<String>of(),"CANCELLED",Set.<String>of());
        if(!transitions.getOrDefault(current,Set.of()).contains(next))throw new RepairProblem("Không thể chuyển trạng thái "+current+" → "+next+".");
        if(Set.of("REPAIRING","READY").contains(next) && exists("SELECT COUNT(*) FROM dbo.RepairQuotes WHERE RepairTicketID=? AND Status='SENT'",ticketId)) throw new RepairProblem("Vẫn còn báo giá đang chờ khách phản hồi.");
        if("REPAIRING".equals(next) && "WAITING_APPROVAL".equals(current) && !exists("SELECT COUNT(*) FROM dbo.RepairQuotes WHERE RepairTicketID=? AND Status='ACCEPTED'",ticketId)) throw new RepairProblem("Chưa có báo giá được khách duyệt.");
        if("WAITING_APPROVAL".equals(next) && !exists("SELECT COUNT(*) FROM dbo.RepairQuotes WHERE RepairTicketID=? AND Status='SENT'",ticketId)) throw new RepairProblem("Chưa có báo giá đã gửi khách.");
        db.update("UPDATE dbo.RepairTickets SET Status=? WHERE RepairTicketID=?",next,ticketId);
        event(ticketId,current,next,required(note,2000,"Ghi chú"),publicNote);
        audit.writeCurrent("REPAIR_STATUS","REPAIR_TICKET",String.valueOf(ticketId),current+" → "+next);
    }

    @PreAuthorize("hasAuthority('REPAIR_QUOTE')")
    @Transactional(isolation=Isolation.SERIALIZABLE)
    public long quote(long ticketId,String kind,long referenceId,int quantity,BigDecimal price) {
        var t=lockedTicket(ticketId);
        if(!Set.of("DIAGNOSING","WAITING_APPROVAL","REPAIRING","WAITING_PARTS").contains(t.get("Status"))) throw new RepairProblem("Phiếu chưa ở giai đoạn báo giá.");
        if(quantity<=0 || quantity>10000 || price==null || price.signum()<0 || price.scale()>2 || price.precision()>19) throw new RepairProblem("Số lượng/đơn giá không hợp lệ.");
        if(kind==null || !Set.of("SERVICE","PART").contains(kind)) throw new RepairProblem("Chọn công sửa hoặc linh kiện.");
        var reference=kind.equals("SERVICE")?one("SELECT Name,DefaultWarrantyDays FROM dbo.Services WHERE ServiceID=?",referenceId):one("SELECT Name,StockMode FROM dbo.Products WHERE ProductID=? AND IsActive=1",referenceId);
        int warranty=kind.equals("SERVICE")?((Number)reference.get("DefaultWarrantyDays")).intValue():warranties.repairPartDays(referenceId);
        int number=db.queryForObject("SELECT COALESCE(MAX(QuoteNo),0)+1 FROM dbo.RepairQuotes WHERE RepairTicketID=?",Integer.class,ticketId);
        long quoteId=insertId("INSERT INTO dbo.RepairQuotes(RepairTicketID,QuoteNo,CreatedBy,ValidUntil) VALUES (?,?,?,?)",ticketId,number,actor(),LocalDateTime.now(ZoneOffset.UTC).plusDays(7));
        long lineId=insertId("""
            INSERT INTO dbo.QuoteItems(QuoteID,[LineNo],DescriptionSnapshot,Quantity,UnitPrice,DiscountAmount,TaxRate,WarrantyDaysSnapshot) VALUES (?,1,?,?,?,0,0,?)
            """,quoteId,reference.get("Name"),quantity,price,warranty);
        if(kind.equals("PART")) db.update("INSERT INTO dbo.QuotePartItems(QuoteItemID,ProductID) VALUES (?,?)",lineId,referenceId);
        else db.update("INSERT INTO dbo.QuoteServiceItems(QuoteItemID,ServiceID) VALUES (?,?)",lineId,referenceId);
        audit.writeCurrent("REPAIR_QUOTE_CREATE","REPAIR_QUOTE",String.valueOf(quoteId),"Lập báo giá số "+number);
        return quoteId;
    }

    @PreAuthorize("hasAuthority('REPAIR_QUOTE')")
    @Transactional(isolation=Isolation.SERIALIZABLE)
    public void addQuoteLine(long ticketId,long quoteId,String kind,long referenceId,int quantity,BigDecimal price) {
        lockedTicket(ticketId);
        var quote=one("SELECT Status FROM dbo.RepairQuotes WITH (UPDLOCK,HOLDLOCK) WHERE QuoteID=? AND RepairTicketID=?",quoteId,ticketId);
        if(!"DRAFT".equals(quote.get("Status")))throw new RepairProblem("Chỉ bổ sung dòng cho báo giá nháp.");
        if(quantity<=0||quantity>10000||price==null||price.signum()<0||price.scale()>2||price.precision()>19)throw new RepairProblem("Số lượng/đơn giá không hợp lệ.");
        if(kind==null || !Set.of("SERVICE","PART").contains(kind))throw new RepairProblem("Loại dòng không hợp lệ.");
        var ref=kind.equals("SERVICE")?one("SELECT Name,DefaultWarrantyDays FROM dbo.Services WHERE ServiceID=?",referenceId):one("SELECT Name FROM dbo.Products WHERE ProductID=? AND IsActive=1",referenceId);
        int warranty=kind.equals("SERVICE")?((Number)ref.get("DefaultWarrantyDays")).intValue():warranties.repairPartDays(referenceId);
        int line=db.queryForObject("SELECT COALESCE(MAX([LineNo]),0)+1 FROM dbo.QuoteItems WHERE QuoteID=?",Integer.class,quoteId);
        long item=insertId("INSERT INTO dbo.QuoteItems(QuoteID,[LineNo],DescriptionSnapshot,Quantity,UnitPrice,DiscountAmount,TaxRate,WarrantyDaysSnapshot) VALUES (?,?,?,?,?,0,0,?)",quoteId,line,ref.get("Name"),quantity,price,warranty);
        db.update(kind.equals("SERVICE")?"INSERT INTO dbo.QuoteServiceItems(QuoteItemID,ServiceID) VALUES (?,?)":"INSERT INTO dbo.QuotePartItems(QuoteItemID,ProductID) VALUES (?,?)",item,referenceId);
        audit.writeCurrent("REPAIR_QUOTE_LINE","REPAIR_QUOTE",String.valueOf(quoteId),"Thêm dòng báo giá "+line);
    }

    @PreAuthorize("hasAuthority('REPAIR_QUOTE')")
    @Transactional
    public void sendQuote(long ticketId,long quoteId) {
        var ticket=lockedTicket(ticketId);
        if(Set.of("DELIVERED","CANCELLED").contains(ticket.get("Status")))
            throw new RepairProblem("Không thể gửi báo giá cho phiếu đã đóng.");
        if(db.update("UPDATE dbo.RepairQuotes SET Status='SENT' WHERE QuoteID=? AND RepairTicketID=? AND Status='DRAFT' AND ValidUntil>SYSUTCDATETIME()",quoteId,ticketId)!=1) throw new RepairProblem("Báo giá không thuộc phiếu hoặc không còn là bản nháp hợp lệ.");
        audit.writeCurrent("REPAIR_QUOTE_SEND","REPAIR_QUOTE",String.valueOf(quoteId),"Gửi báo giá cho khách");
    }

    @PreAuthorize("hasAuthority('REPAIR_QUOTE')")
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public void quoteResponse(long ticketId,long quoteId,boolean accepted,String evidence) {
        evidence=required(evidence,1000,"Bằng chứng khách xác nhận");
        if(!exists("SELECT COUNT(*) FROM dbo.RepairQuotes WHERE QuoteID=? AND RepairTicketID=?",quoteId,ticketId)) throw new RepairProblem("Báo giá không thuộc phiếu sửa.");
        quoteResponseInternal(quoteId,accepted,evidence);
    }

    @PreAuthorize("hasAuthority('CUSTOMER_SELF_READ')")
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public void customerResponse(long quoteId,boolean accepted) {
        Long own=ownCustomerId();
        if(own==null||!exists("SELECT COUNT(*) FROM dbo.RepairQuotes q JOIN dbo.RepairTickets t ON t.RepairTicketID=q.RepairTicketID WHERE q.QuoteID=? AND t.CustomerID=?",quoteId,own)) throw new RepairProblem("Báo giá không thuộc tài khoản của bạn.");
        quoteResponseInternal(quoteId,accepted,"Xác nhận trực tuyến từ tài khoản khách #"+own);
    }
    private void quoteResponseInternal(long quoteId,boolean accepted,String evidence) {
        long ticketId=((Number)one("SELECT RepairTicketID FROM dbo.RepairQuotes WHERE QuoteID=?",quoteId).get("RepairTicketID")).longValue();
        var ticket=lockedTicket(ticketId);
        if(Set.of("DELIVERED","CANCELLED").contains(ticket.get("Status")))
            throw new RepairProblem("Phiếu sửa đã đóng, không thể phản hồi báo giá.");
        var q=one("SELECT RepairTicketID,Status,ValidUntil FROM dbo.RepairQuotes WITH (UPDLOCK,HOLDLOCK) WHERE QuoteID=?",quoteId);
        if(!"SENT".equals(q.get("Status")))throw new RepairProblem("Báo giá đã được xử lý.");
        Object until=q.get("ValidUntil");
        LocalDateTime deadline=until instanceof java.sql.Timestamp t?t.toLocalDateTime():(LocalDateTime)until;
        if(deadline.isBefore(LocalDateTime.now(ZoneOffset.UTC)))throw new RepairProblem("Báo giá hết hạn.");
        db.update("UPDATE dbo.RepairQuotes SET Status=?,RespondedAt=SYSUTCDATETIME(),ApprovalEvidence=? WHERE QuoteID=?",accepted?"ACCEPTED":"REJECTED",evidence,quoteId);
        String current=(String)ticket.get("Status");
        event(ticketId,current,current,accepted?"Khách đã chấp thuận báo giá":"Khách đã từ chối báo giá",true);
        audit.writeCurrent("REPAIR_QUOTE_RESPONSE","REPAIR_QUOTE",String.valueOf(quoteId),accepted?"Đồng ý":"Từ chối");
    }

    @PreAuthorize("hasAuthority('REPAIR_WORK')")
    @Transactional
    public void work(long ticketId,String description) {
        var t=lockedTicket(ticketId);
        if(!Set.of("DIAGNOSING","REPAIRING","WAITING_PARTS").contains(t.get("Status"))) throw new RepairProblem("Không thể ghi công việc lúc này.");
        long actor=actor();
        db.update("INSERT INTO dbo.RepairWorkLogs(RepairTicketID,TechnicianID,StartedAt,FinishedAt,WorkDescription) VALUES (?,?,SYSUTCDATETIME(),SYSUTCDATETIME(),?)",ticketId,actor,required(description,2000,"Nội dung công việc"));
        audit.writeCurrent("REPAIR_WORK","REPAIR_TICKET",String.valueOf(ticketId),"Ghi nhận công việc kỹ thuật");
    }

    @PreAuthorize("hasAuthority('REPAIR_WORK')")
    @Transactional(isolation=Isolation.SERIALIZABLE)
    public void consume(long ticketId,long quoteItemId,long locationId,int amount) {
        lockedTicket(ticketId);
        if(!exists("SELECT COUNT(*) FROM dbo.QuoteItems i JOIN dbo.RepairQuotes q ON q.QuoteID=i.QuoteID WHERE i.QuoteItemID=? AND q.RepairTicketID=?",quoteItemId,ticketId)) throw new RepairProblem("Linh kiện không thuộc phiếu sửa.");
        stock.useApprovedPart(quoteItemId,locationId,amount);
        event(ticketId,(String)ticket(ticketId).get("Status"),(String)ticket(ticketId).get("Status"),"Đã sử dụng "+amount+" linh kiện theo báo giá",false);
    }

    @PreAuthorize("hasAuthority('REPAIR_WORK')")
    @Transactional(isolation=Isolation.SERIALIZABLE)
    public void consumeDevice(long ticketId,long quoteItemId,long deviceId,long locationId) {
        lockedTicket(ticketId);
        if(!exists("SELECT COUNT(*) FROM dbo.QuoteItems i JOIN dbo.RepairQuotes q ON q.QuoteID=i.QuoteID WHERE i.QuoteItemID=? AND q.RepairTicketID=?",quoteItemId,ticketId)) throw new RepairProblem("Linh kiện không thuộc phiếu sửa.");
        stock.useApprovedDevicePart(quoteItemId,deviceId,locationId);
        String status=(String)ticket(ticketId).get("Status");
        event(ticketId,status,status,"Đã sử dụng linh kiện serial #"+deviceId,false);
    }

    @PreAuthorize("hasAuthority('REPAIR_HANDOVER')")
    @Transactional(isolation=Isolation.SERIALIZABLE)
    public void handover(long ticketId,BigDecimal payment,String method,String evidence) {
        var t=lockedTicket(ticketId);
        if(!"READY".equals(t.get("Status"))) throw new RepairProblem("Máy phải ở trạng thái sửa xong trước khi bàn giao.");
        if(exists("SELECT COUNT(*) FROM dbo.RepairQuotes WHERE RepairTicketID=? AND Status='SENT'",ticketId)) throw new RepairProblem("Chưa hoàn tất phản hồi tất cả báo giá.");
        BigDecimal due=due(ticketId);
        if(payment==null) payment=BigDecimal.ZERO;
        if(payment.signum()<0 || payment.scale()>2 || payment.precision()>19 || payment.compareTo(due)>0) throw new RepairProblem("Số tiền thu không hợp lệ hoặc vượt số phải thu.");
        if(payment.compareTo(due)<0) throw new RepairProblem("Chưa thu đủ tiền đã duyệt để bàn giao.");
        if(payment.signum()>0) {
            if(method==null || !Set.of("CASH","BANK_TRANSFER").contains(method)) throw new RepairProblem("Chọn tiền mặt hoặc chuyển khoản.");
            db.update("""
                INSERT INTO dbo.Payments(PaymentCode,RepairTicketID,Direction,Amount,Method,Status,IdempotencyKey,RecordedBy)
                VALUES (?,?,'IN',?,?,'SUCCEEDED',?,?)
                """,randomCode("PAY"),ticketId,payment,method,"REPAIR_HANDOVER_"+ticketId,actor());
        }
        db.update("UPDATE dbo.RepairTickets SET Status='DELIVERED',ClosedAt=SYSUTCDATETIME() WHERE RepairTicketID=?",ticketId);
        warranties.issueDeliveredRepair(ticketId);
        event(ticketId,"READY","DELIVERED","Bàn giao: "+required(evidence,500,"Biên nhận bàn giao"),true);
        audit.writeCurrent("REPAIR_HANDOVER","REPAIR_TICKET",String.valueOf(ticketId),"Bàn giao và quyết toán");
    }

    /** Public tracking verifies BOTH an unguessable code and its SHA-256 digest; only public notes leave the admin area. */
    public Map<String,Object> track(String code,String token) {
        if(code==null || !code.matches("REP-[A-F0-9]{24}") || token==null || !token.matches("[A-Fa-f0-9]{48}")) throw new RepairProblem("Mã phiếu hoặc mã tra cứu không hợp lệ.");
        var rows=db.queryForList("SELECT RepairTicketID,TicketCode,Status,TrackingTokenHash FROM dbo.RepairTickets WHERE TicketCode=?",code);
        if(rows.isEmpty() || !MessageDigest.isEqual((byte[])rows.getFirst().get("TrackingTokenHash"),hash(token))) throw new RepairProblem("Không tìm thấy phiếu với thông tin tra cứu này.");
        var result=new LinkedHashMap<String,Object>(rows.getFirst());result.remove("TrackingTokenHash");
        long id=((Number)result.get("RepairTicketID")).longValue();
        result.put("events",db.queryForList("SELECT ToStatus,Note,OccurredAt FROM dbo.RepairEvents WHERE RepairTicketID=? AND IsPublic=1 ORDER BY RepairEventID",id));
        return result;
    }

    private void event(long id,String from,String to,String note,boolean publicNote) {
        db.update("INSERT INTO dbo.RepairEvents(RepairTicketID,FromStatus,ToStatus,Note,IsPublic,ActorID) VALUES (?,?,?,?,?,?)",id,from,to,note,publicNote,actor());
    }
    private Map<String,Object> lockedTicket(long ticketId) {
        return one("SELECT RepairTicketID,Status FROM dbo.RepairTickets WITH (UPDLOCK,HOLDLOCK) WHERE RepairTicketID=?",ticketId);
    }
    private void checkTechnician(long id) {if(!exists("SELECT COUNT(*) FROM dbo.Users u JOIN dbo.UserRoles ur ON ur.UserID=u.UserID JOIN dbo.Roles r ON r.RoleID=ur.RoleID WHERE u.UserID=? AND r.RoleCode='TECHNICIAN' AND u.IsActive=1",id)) throw new RepairProblem("Người được phân công phải có vai trò kỹ thuật.");}
    private Long ownCustomerId() {
        var a=SecurityContextHolder.getContext().getAuthentication();if(a==null || !a.isAuthenticated() || "anonymousUser".equals(a.getName()))return null;
        var rows=db.queryForList("SELECT c.CustomerID FROM dbo.Customers c JOIN dbo.Users u ON u.UserID=c.UserID WHERE u.LoginName=?",a.getName());
        return rows.isEmpty()?null:((Number)rows.getFirst().get("CustomerID")).longValue();
    }
    private long actor() {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth==null || !auth.isAuthenticated()) throw new RepairProblem("Yêu cầu đăng nhập.");
        return db.queryForObject("SELECT UserID FROM dbo.Users WHERE LoginName=?",Long.class,auth.getName());
    }
    private String randomCode(String prefix) {return prefix+"-"+UUID.randomUUID().toString().replace("-","").substring(0,24).toUpperCase(Locale.ROOT);}
    private String newTrackingToken() {byte[] b=new byte[24];random.nextBytes(b);return HexFormat.of().withUpperCase().formatHex(b);}
    private static byte[] hash(String token) {try{return MessageDigest.getInstance("SHA-256").digest(token.toUpperCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII));}catch(Exception e){throw new IllegalStateException(e);}}
    private static String phone(String p){p=required(p,25,"Số điện thoại");if(!p.matches("\\+?[0-9]{8,15}"))throw new RepairProblem("Số điện thoại không hợp lệ.");return p;}
    private static String required(String x,int max,String field){if(x==null || x.isBlank() || x.strip().length()>max)throw new RepairProblem(field+" bắt buộc và tối đa "+max+" ký tự.");return x.strip();}
    private boolean exists(String sql,Object... args){return db.queryForObject(sql,Integer.class,args)>0;}
    private Map<String,Object> one(String sql,Object... args){var r=db.queryForList(sql,args);if(r.isEmpty())throw new RepairProblem("Dữ liệu không tồn tại.");return r.getFirst();}
    private long insertId(String sql,Object... args) {
        var key=new GeneratedKeyHolder();db.update(con->{PreparedStatement p=con.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS);for(int i=0;i<args.length;i++)p.setObject(i+1,args[i]);return p;},key);
        if(key.getKey()==null)throw new RepairProblem("Không lưu được dữ liệu.");return key.getKey().longValue();
    }
}
