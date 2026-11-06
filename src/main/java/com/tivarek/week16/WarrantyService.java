package com.tivarek.week16;

import com.tivarek.week8.AuditService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.UUID;

@Service
public class WarrantyService {
    private static final String DETAIL = """
        SELECT w.WarrantyID,w.WarrantyCode,w.Status,w.StartAt,w.EndAt,w.TermsSnapshot,
               CONVERT(varchar(16),DATEADD(HOUR,7,w.StartAt),120) AS StartLocal,
               CONVERT(varchar(16),DATEADD(HOUR,7,w.EndAt),120) AS EndLocal,
               w.PolicyID,p.Name AS PolicyName,
               CASE WHEN sw.WarrantyID IS NULL THEN 'REPAIR' ELSE 'SALE' END AS SourceType,
               COALESCE(sl.DescriptionSnapshot,qi.DescriptionSnapshot) AS Description,
               so.OrderCode,rt.TicketCode,
               COALESCE(bs.Quantity,qi.Quantity,1) AS Quantity,
               COALESCE((SELECT SUM(e.Delta) FROM dbo.ReturnBulkEntries x
                 JOIN dbo.BulkStockEntries e ON e.BulkEntryID=x.BulkEntryID
                 JOIN dbo.InventoryDocuments doc ON doc.InventoryDocID=e.InventoryDocID AND doc.Status='POSTED'
                 WHERE x.SaleLineID=sl.SaleLineID),
                 (SELECT COUNT(*) FROM dbo.ReturnDeviceEntries x
                 JOIN dbo.DeviceStockEntries e ON e.DeviceEntryID=x.DeviceEntryID
                 JOIN dbo.InventoryDocuments doc ON doc.InventoryDocID=e.InventoryDocID AND doc.Status='POSTED'
                 WHERE x.SaleLineID=sl.SaleLineID),0) AS ReturnedQuantity,
               CASE WHEN w.Status='VOID' THEN 'VOID'
                    WHEN w.EndAt<=SYSUTCDATETIME() THEN 'EXPIRED' ELSE 'ACTIVE' END AS EffectiveStatus
        FROM dbo.Warranties w
        JOIN dbo.WarrantyPolicies p ON p.PolicyID=w.PolicyID
        LEFT JOIN dbo.SaleWarranties sw ON sw.WarrantyID=w.WarrantyID
        LEFT JOIN dbo.SalesLines sl ON sl.SaleLineID=sw.SaleLineID
        LEFT JOIN dbo.SalesOrders so ON so.OrderID=sl.OrderID
        LEFT JOIN dbo.BulkSaleLines bs ON bs.SaleLineID=sl.SaleLineID
        LEFT JOIN dbo.RepairWarranties rw ON rw.WarrantyID=w.WarrantyID
        LEFT JOIN dbo.QuoteItems qi ON qi.QuoteItemID=rw.QuoteItemID
        LEFT JOIN dbo.RepairQuotes rq ON rq.QuoteID=qi.QuoteID
        LEFT JOIN dbo.RepairTickets rt ON rt.RepairTicketID=rq.RepairTicketID
        """;
    private final JdbcTemplate db;
    private final AuditService audit;
    private final SecureRandom random = new SecureRandom();

    public WarrantyService(JdbcTemplate db, AuditService audit) { this.db=db; this.audit=audit; }

    @PreAuthorize("hasAuthority('WARRANTY_READ')")
    public List<Map<String,Object>> adminWarranties() {
        return db.queryForList("SELECT TOP 150 * FROM ("+DETAIL+") x ORDER BY WarrantyID DESC");
    }

    @PreAuthorize("hasAuthority('WARRANTY_READ')")
    public List<Map<String,Object>> adminClaims() {
        return db.queryForList("""
            SELECT TOP 150 c.ClaimID,c.ClaimCode,c.ClaimQuantity,c.Status,c.ClaimedAt,c.ReportedProblem,
                   w.WarrantyCode,CASE WHEN s.WarrantyID IS NULL THEN 'REPAIR' ELSE 'SALE' END AS SourceType
            FROM dbo.WarrantyClaims c JOIN dbo.Warranties w ON w.WarrantyID=c.WarrantyID
            LEFT JOIN dbo.SaleWarranties s ON s.WarrantyID=w.WarrantyID
            ORDER BY c.ClaimID DESC
            """);
    }

    @PreAuthorize("hasAuthority('WARRANTY_READ')")
    public Map<String,Object> adminClaim(long id) {
        var claim = one("""
            SELECT c.*,w.WarrantyCode,w.StartAt,w.EndAt,w.Status AS WarrantyStatus
            FROM dbo.WarrantyClaims c JOIN dbo.Warranties w ON w.WarrantyID=c.WarrantyID
            WHERE c.ClaimID=?
            """, id);
        var result = new LinkedHashMap<>(claim);
        result.put("events", db.queryForList("SELECT ToStatus,Note,OccurredAt,IsPublic FROM dbo.WarrantyClaimEvents WHERE ClaimID=? ORDER BY EventID",id));
        result.put("repair", db.queryForList("SELECT t.RepairTicketID,t.TicketCode,t.Status FROM dbo.WarrantyClaimRepairs x JOIN dbo.RepairTickets t ON t.RepairTicketID=x.RepairTicketID WHERE x.ClaimID=?",id));
        return result;
    }

    @PreAuthorize("hasAuthority('WARRANTY_READ')")
    public List<Map<String,Object>> policies() {return db.queryForList("SELECT PolicyID,PolicyCode,Name,Terms,IsActive FROM dbo.WarrantyPolicies ORDER BY PolicyID DESC");}

    @PreAuthorize("hasAuthority('WARRANTY_READ')")
    public String codeFor(long id) {return (String)one("SELECT WarrantyCode FROM dbo.Warranties WHERE WarrantyID=?",id).get("WarrantyCode");}

    @PreAuthorize("hasAuthority('WARRANTY_READ')")
    public List<Map<String,Object>> products() {return db.queryForList("""
        SELECT TOP 150 p.ProductID,p.SKU,p.Name,x.PolicyID,x.WarrantyDays FROM dbo.Products p
        LEFT JOIN dbo.ProductWarrantySettings x ON x.ProductID=p.ProductID ORDER BY p.ProductID DESC
        """);}

    @PreAuthorize("hasAuthority('WARRANTY_READ')")
    public List<Map<String,Object>> devices() {return db.queryForList("""
        SELECT TOP 150 d.DeviceID,d.AssetTag,d.DeviceDescription,x.PolicyID,x.WarrantyDays FROM dbo.Devices d
        LEFT JOIN dbo.DeviceWarrantyOverrides x ON x.DeviceID=d.DeviceID ORDER BY d.DeviceID DESC
        """);}

    @PreAuthorize("hasAuthority('WARRANTY_READ')")
    public List<Map<String,Object>> services() {return db.queryForList("""
        SELECT s.ServiceID,s.ServiceCode,s.Name,s.DefaultWarrantyDays,x.PolicyID FROM dbo.Services s
        LEFT JOIN dbo.ServiceWarrantyPolicies x ON x.ServiceID=s.ServiceID ORDER BY s.ServiceID DESC
        """);}

    @PreAuthorize("hasAuthority('WARRANTY_POLICY_MANAGE')")
    @Transactional(isolation=Isolation.SERIALIZABLE)
    public void savePolicy(String code, String name, String terms) {
        code=required(code,40,"Mã chính sách").toUpperCase(Locale.ROOT);
        if(!code.matches("[A-Z0-9_-]{3,40}"))throw new WarrantyProblem("Mã chính sách chỉ gồm chữ, số, gạch nối và gạch dưới.");
        name=required(name,180,"Tên chính sách");terms=required(terms,2000,"Điều khoản");
        // Lock the unique-key range before the update/insert to prevent concurrent duplicates.
        db.queryForList("SELECT PolicyID FROM dbo.WarrantyPolicies WITH (UPDLOCK,HOLDLOCK) WHERE PolicyCode=?",code);
        int changed=db.update("UPDATE dbo.WarrantyPolicies SET Name=?,Terms=? WHERE PolicyCode=?",name,terms,code);
        if(changed==0)db.update("INSERT INTO dbo.WarrantyPolicies(PolicyCode,Name,Terms,IsActive) VALUES (?,?,?,1)",code,name,terms);
        audit.writeCurrent("WARRANTY_POLICY","WARRANTY_POLICY",code,"Lưu chính sách bảo hành");
    }

    @PreAuthorize("hasAuthority('WARRANTY_POLICY_MANAGE')")
    @Transactional(isolation=Isolation.SERIALIZABLE)
    public void configure(String kind,long referenceId,long policyId,int days) {
        if(referenceId<=0 || policyId<=0 || days<0 || days>3650)throw new WarrantyProblem("ID hoặc thời hạn bảo hành không hợp lệ.");
        if(count("SELECT COUNT(*) FROM dbo.WarrantyPolicies WHERE PolicyID=? AND IsActive=1",policyId)==0)
            throw new WarrantyProblem("Chỉ được chọn chính sách đang hoạt động.");
        String table,idColumn;
        switch(kind) {
            case "PRODUCT" -> {table="ProductWarrantySettings";idColumn="ProductID";}
            case "DEVICE" -> {table="DeviceWarrantyOverrides";idColumn="DeviceID";}
            case "SERVICE" -> {table="ServiceWarrantyPolicies";idColumn="ServiceID";}
            default -> throw new WarrantyProblem("Loại cấu hình không hợp lệ.");
        }
        String sourceTable=switch(kind){case "PRODUCT"->"Products";case "DEVICE"->"Devices";default->"Services";};
        if(db.queryForList("SELECT "+idColumn+" FROM dbo."+sourceTable+" WITH (UPDLOCK,HOLDLOCK) WHERE "+idColumn+"=?",referenceId).isEmpty())
            throw new WarrantyProblem("Sản phẩm, thiết bị hoặc dịch vụ không tồn tại.");
        if("SERVICE".equals(kind)) {
            if(db.update("UPDATE dbo.ServiceWarrantyPolicies SET PolicyID=? WHERE ServiceID=?",policyId,referenceId)==0)
                db.update("INSERT INTO dbo.ServiceWarrantyPolicies(ServiceID,PolicyID) VALUES (?,?)",referenceId,policyId);
            // Services là nơi duy nhất lưu thời hạn mặc định của dịch vụ.
            db.update("UPDATE dbo.Services SET DefaultWarrantyDays=? WHERE ServiceID=?",days,referenceId);
        } else {
            if(db.update("UPDATE dbo."+table+" SET PolicyID=?,WarrantyDays=? WHERE "+idColumn+"=?",policyId,days,referenceId)==0)
                db.update("INSERT INTO dbo."+table+"("+idColumn+",PolicyID,WarrantyDays) VALUES (?,?,?)",referenceId,policyId,days);
        }
        audit.writeCurrent("WARRANTY_CONFIG","WARRANTY_RULE",kind+":"+referenceId,"Đặt chính sách "+policyId+" ("+days+" ngày)");
    }

    @PreAuthorize("hasAuthority('WARRANTY_TOKEN_ISSUE')")
    @Transactional(isolation=Isolation.SERIALIZABLE)
    public String renewToken(long warrantyId) {
        if(db.queryForList("SELECT WarrantyID FROM dbo.Warranties WITH (UPDLOCK,HOLDLOCK) WHERE WarrantyID=?",warrantyId).isEmpty())throw new WarrantyProblem("Không tìm thấy phiếu bảo hành.");
        byte[] secret=new byte[24];random.nextBytes(secret);
        String token=HexFormat.of().withUpperCase().formatHex(secret);
        db.update("UPDATE dbo.Warranties SET TrackingTokenHash=? WHERE WarrantyID=?",WarrantyIssuanceService.sha256(token),warrantyId);
        audit.writeCurrent("WARRANTY_TOKEN","WARRANTY",String.valueOf(warrantyId),"Cấp lại mã tra cứu (không lưu mã rõ)");
        return token;
    }

    /** Public lookup requires both the certificate code and its unguessable secret. */
    public Map<String,Object> lookup(String code,String token) {
        long id=verifiedId(code,token,false);
        Map<String,Object> result=new LinkedHashMap<>(one(DETAIL+" WHERE w.WarrantyID=?",id));
        result.put("claims",db.queryForList("SELECT ClaimCode,ClaimQuantity,Status,ClaimedAt,Resolution FROM dbo.WarrantyClaims WHERE WarrantyID=? ORDER BY ClaimID DESC",id));
        return result;
    }

    @PreAuthorize("hasAuthority('WARRANTY_SELF_READ')")
    public List<Map<String,Object>> myWarranties(Authentication authentication) {
        if(authentication==null)return List.of();
        return db.queryForList("""
            SELECT * FROM (
            """+DETAIL+"""
            ) x WHERE EXISTS (
                SELECT 1 FROM dbo.Warranties w
                LEFT JOIN dbo.SaleWarranties sw ON sw.WarrantyID=w.WarrantyID
                LEFT JOIN dbo.SalesLines sl ON sl.SaleLineID=sw.SaleLineID
                LEFT JOIN dbo.SalesOrders so ON so.OrderID=sl.OrderID
                LEFT JOIN dbo.RepairWarranties rw ON rw.WarrantyID=w.WarrantyID
                LEFT JOIN dbo.QuoteItems qi ON qi.QuoteItemID=rw.QuoteItemID
                LEFT JOIN dbo.RepairQuotes rq ON rq.QuoteID=qi.QuoteID
                LEFT JOIN dbo.RepairTickets rt ON rt.RepairTicketID=rq.RepairTicketID
                JOIN dbo.Customers c ON c.CustomerID=COALESCE(so.CustomerID,rt.CustomerID)
                JOIN dbo.Users u ON u.UserID=c.UserID
                WHERE w.WarrantyID=x.WarrantyID AND u.LoginName=?
            ) ORDER BY x.WarrantyID DESC
            """, authentication.getName());
    }

    @Transactional(isolation=Isolation.SERIALIZABLE)
    public String claim(String code,String token,int quantity,String problem) {
        long warrantyId=verifiedId(code,token,true);
        return createClaim(warrantyId,quantity,problem);
    }

    @PreAuthorize("hasAuthority('WARRANTY_SELF_READ')")
    @Transactional(isolation=Isolation.SERIALIZABLE)
    public String claimOwned(long warrantyId,int quantity,String problem,Authentication authentication) {
        if(authentication==null || myWarranties(authentication).stream()
            .noneMatch(w->((Number)w.get("WarrantyID")).longValue()==warrantyId))
            throw new WarrantyProblem("Phiếu này không thuộc tài khoản của bạn.");
        db.queryForMap("SELECT WarrantyID FROM dbo.Warranties WITH (UPDLOCK,HOLDLOCK) WHERE WarrantyID=?",warrantyId);
        return createClaim(warrantyId,quantity,problem);
    }

    private String createClaim(long warrantyId,int quantity,String problem) {
        if(quantity<1 || quantity>10000)throw new WarrantyProblem("Số lượng yêu cầu không hợp lệ.");
        problem=required(problem,2000,"Mô tả lỗi");
        Map<String,Object> detail=one(DETAIL+" WHERE w.WarrantyID=?",warrantyId);
        if(!"ACTIVE".equals(detail.get("EffectiveStatus")))throw new WarrantyProblem("Phiếu bảo hành không còn hiệu lực.");
        int total=((Number)detail.get("Quantity")).intValue();
        int returned=((Number)detail.get("ReturnedQuantity")).intValue();
        int pending=count("SELECT COALESCE(SUM(ClaimQuantity),0) FROM dbo.WarrantyClaims WHERE WarrantyID=? AND Status IN ('OPEN','INSPECTING','APPROVED')",warrantyId);
        if(quantity>total-returned-pending)throw new WarrantyProblem("Số lượng đang được bảo hành hoặc đã trả vượt quá số đủ điều kiện.");
        String claimCode="YC-"+UUID.randomUUID().toString().replace("-","").substring(0,24).toUpperCase(Locale.ROOT);
        var key=new GeneratedKeyHolder();String validated=problem;
        db.update(connection->{
            PreparedStatement p=connection.prepareStatement("INSERT INTO dbo.WarrantyClaims(ClaimCode,WarrantyID,ClaimQuantity,ReportedProblem,Status) VALUES (?,?,?,?,'OPEN')",Statement.RETURN_GENERATED_KEYS);
            p.setString(1,claimCode);p.setLong(2,warrantyId);p.setInt(3,quantity);p.setString(4,validated);return p;
        },key);
        if(key.getKey()==null)throw new WarrantyProblem("Không lưu được yêu cầu bảo hành.");
        db.update("INSERT INTO dbo.WarrantyClaimEvents(ClaimID,FromStatus,ToStatus,Note,ActorID) VALUES (?,NULL,'OPEN',?,?)",
            key.getKey().longValue(),"Khách gửi yêu cầu bảo hành",actorOrNull());
        return claimCode;
    }

    @PreAuthorize("hasAuthority('WARRANTY_CLAIM_PROCESS')")
    @Transactional(isolation=Isolation.SERIALIZABLE)
    public void transition(long id,String next,String note) {
        note=required(note,2000,"Nội dung xử lý");
        Map<String,Object> c=one("SELECT Status FROM dbo.WarrantyClaims WITH (UPDLOCK,HOLDLOCK) WHERE ClaimID=?",id);
        String old=(String)c.get("Status");
        boolean allowed=switch(old) {
            case "OPEN" -> "INSPECTING".equals(next);
            case "INSPECTING" -> Set.of("APPROVED","REJECTED").contains(next);
            case "APPROVED" -> "RESOLVED".equals(next);
            default -> false;
        };
        if(!allowed)throw new WarrantyProblem("Chuyển trạng thái không hợp lệ.");
        if(Set.of("APPROVED","REJECTED").contains(next) && !has("WARRANTY_CLAIM_APPROVE"))
            throw new WarrantyProblem("Chỉ chủ cửa hàng được phê duyệt hoặc từ chối.");
        if("RESOLVED".equals(next)) {
            var linked=db.queryForList("""
                SELECT t.Status FROM dbo.WarrantyClaimRepairs x JOIN dbo.RepairTickets t ON t.RepairTicketID=x.RepairTicketID WHERE x.ClaimID=?
                """,id);
            if(!linked.isEmpty() && !"DELIVERED".equals(linked.getFirst().get("Status")))
                throw new WarrantyProblem("Phiếu sửa bảo hành phải bàn giao xong trước khi kết thúc yêu cầu.");
        }
        db.update("UPDATE dbo.WarrantyClaims SET Status=?,Resolution=CASE WHEN ? IN ('REJECTED','RESOLVED') THEN ? ELSE Resolution END WHERE ClaimID=?",next,next,note,id);
        db.update("INSERT INTO dbo.WarrantyClaimEvents(ClaimID,FromStatus,ToStatus,Note,ActorID) VALUES (?,?,?,?,?)",id,old,next,note,actorOrNull());
        audit.writeCurrent("WARRANTY_CLAIM_STATUS","WARRANTY_CLAIM",String.valueOf(id),old+" → "+next);
    }

    @PreAuthorize("hasAuthority('WARRANTY_CLAIM_PROCESS')")
    @Transactional(isolation=Isolation.SERIALIZABLE)
    public void attachRepair(long claimId,long ticketId) {
        var claim=one("SELECT WarrantyID,ClaimedAt,Status FROM dbo.WarrantyClaims WITH (UPDLOCK,HOLDLOCK) WHERE ClaimID=?",claimId);
        if(!"APPROVED".equals(claim.get("Status")))throw new WarrantyProblem("Chỉ liên kết phiếu sửa với yêu cầu đã chấp thuận.");
        if(count("SELECT COUNT(*) FROM dbo.WarrantyClaimRepairs WHERE ClaimID=? OR RepairTicketID=?",claimId,ticketId)>0)
            throw new WarrantyProblem("Phiếu yêu cầu hoặc phiếu sửa đã được liên kết.");
        var ticket=one("SELECT CustomerID,ReceivedAt,Status FROM dbo.RepairTickets WHERE RepairTicketID=?",ticketId);
        if(((java.sql.Timestamp)ticket.get("ReceivedAt")).before((java.sql.Timestamp)claim.get("ClaimedAt")))
            throw new WarrantyProblem("Phiếu sửa phải được tiếp nhận sau yêu cầu bảo hành.");
        var source=db.queryForList("""
            SELECT COALESCE(o.CustomerID,t.CustomerID) AS CustomerID
            FROM dbo.Warranties w
            LEFT JOIN dbo.SaleWarranties sw ON sw.WarrantyID=w.WarrantyID
            LEFT JOIN dbo.SalesLines sl ON sl.SaleLineID=sw.SaleLineID
            LEFT JOIN dbo.SalesOrders o ON o.OrderID=sl.OrderID
            LEFT JOIN dbo.RepairWarranties rw ON rw.WarrantyID=w.WarrantyID
            LEFT JOIN dbo.QuoteItems qi ON qi.QuoteItemID=rw.QuoteItemID
            LEFT JOIN dbo.RepairQuotes rq ON rq.QuoteID=qi.QuoteID
            LEFT JOIN dbo.RepairTickets t ON t.RepairTicketID=rq.RepairTicketID
            WHERE w.WarrantyID=?
            """,claim.get("WarrantyID"));
        Object original=source.isEmpty()?null:source.getFirst().get("CustomerID");
        if(original!=null && ((Number)original).longValue()!=((Number)ticket.get("CustomerID")).longValue())
            throw new WarrantyProblem("Khách hàng của phiếu sửa không khớp nguồn bảo hành.");
        db.update("INSERT INTO dbo.WarrantyClaimRepairs(ClaimID,RepairTicketID) VALUES (?,?)",claimId,ticketId);
        db.update("INSERT INTO dbo.WarrantyClaimEvents(ClaimID,FromStatus,ToStatus,Note,ActorID) VALUES (?,'APPROVED','APPROVED',?,?)",claimId,"Liên kết phiếu sửa #"+ticketId,actorOrNull());
        audit.writeCurrent("WARRANTY_REPAIR_LINK","WARRANTY_CLAIM",String.valueOf(claimId),"Phiếu sửa #"+ticketId);
    }

    private long verifiedId(String code,String token,boolean locked) {
        if(code==null || !code.matches("BH-[A-F0-9]{24}") || token==null || !token.matches("[a-fA-F0-9]{48}"))
            throw new WarrantyProblem("Mã bảo hành hoặc mã xác minh không hợp lệ.");
        String sql="SELECT WarrantyID,TrackingTokenHash FROM dbo.Warranties "+(locked?"WITH (UPDLOCK,HOLDLOCK) ":"")+"WHERE WarrantyCode=?";
        var rows=db.queryForList(sql,code);
        byte[] supplied=WarrantyIssuanceService.sha256(token);
        if(rows.isEmpty() || !MessageDigest.isEqual((byte[])rows.getFirst().get("TrackingTokenHash"),supplied))
            throw new WarrantyProblem("Không tìm thấy phiếu với thông tin xác minh này.");
        return ((Number)rows.getFirst().get("WarrantyID")).longValue();
    }

    private boolean has(String permission) {
        Authentication a=SecurityContextHolder.getContext().getAuthentication();
        return a!=null && a.getAuthorities().stream().anyMatch(x->permission.equals(x.getAuthority()));
    }
    private Long actorOrNull() {
        Authentication a=SecurityContextHolder.getContext().getAuthentication();
        if(a==null || !a.isAuthenticated() || "anonymousUser".equals(a.getName()))return null;
        var rows=db.queryForList("SELECT UserID FROM dbo.Users WHERE LoginName=?",a.getName());
        return rows.isEmpty()?null:((Number)rows.getFirst().get("UserID")).longValue();
    }
    private Map<String,Object> one(String sql,Object... args) {
        var rows=db.queryForList(sql,args);if(rows.isEmpty())throw new WarrantyProblem("Không tìm thấy dữ liệu.");return rows.getFirst();
    }
    private int count(String sql,Object...args) {return db.queryForObject(sql,Integer.class,args);}
    private static String required(String value,int max,String label) {
        if(value==null || value.isBlank() || value.strip().length()>max)throw new WarrantyProblem(label+" không được trống, tối đa "+max+" ký tự.");
        return value.strip();
    }
}
