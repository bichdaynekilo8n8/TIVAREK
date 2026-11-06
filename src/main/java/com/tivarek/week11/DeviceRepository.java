package com.tivarek.week11;
import java.util.*;
import java.math.BigDecimal;
import java.time.*;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Repository;
import com.tivarek.week10.CatalogProblem;
import static com.tivarek.week11.DeviceViews.*;
@Repository
public class DeviceRepository {
    private final JdbcTemplate jdbc;
    public DeviceRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    private static final String SELECT="""
        SELECT d.DeviceID,d.AssetTag,d.ProductID,p.Name AS ProductName,p.SKU,c.Name AS CategoryName,b.Name AS BrandName,
        d.ManufacturerSerial,d.DeviceDescription,d.ConditionGrade,d.AskingPrice,d.FaultDescription,d.Version,d.CreatedAt,d.UpdatedAt,
        (SELECT TOP (1) i.ImageID FROM dbo.DeviceImages i WHERE i.DeviceID=d.DeviceID ORDER BY i.SortOrder,i.ImageID) AS CoverImageID,
        (SELECT TOP (1) k.Result FROM dbo.DeviceChecks k WHERE k.DeviceID=d.DeviceID ORDER BY k.CheckedAt DESC,k.CheckID DESC) AS InspectionResult
        FROM dbo.Devices d LEFT JOIN dbo.Products p ON p.ProductID=d.ProductID
        LEFT JOIN dbo.Categories c ON c.CategoryID=p.CategoryID LEFT JOIN dbo.Brands b ON b.BrandID=p.BrandID
        WHERE d.OwnerCustomerID IS NULL
        """;
    private final RowMapper<Device> mapper=(r,n)->new Device(r.getLong("DeviceID"),r.getString("AssetTag"),r.getLong("ProductID"),
        r.getString("ProductName"),r.getString("SKU"),r.getString("CategoryName"),r.getString("BrandName"),r.getString("ManufacturerSerial"),
        r.getString("DeviceDescription"),r.getString("ConditionGrade"),r.getBigDecimal("AskingPrice"),r.getString("FaultDescription"),
        HexFormat.of().formatHex(r.getBytes("Version")),r.getObject("CreatedAt",LocalDateTime.class),r.getObject("UpdatedAt",LocalDateTime.class),
        r.getObject("CoverImageID",Long.class),r.getString("InspectionResult"));
    public Device get(long id) { return jdbc.query(SELECT+" AND d.DeviceID=?",mapper,id).stream().findFirst().orElseThrow(CatalogProblem::missing); }
    public void lock(long id) {
        if(jdbc.queryForList("SELECT DeviceID FROM dbo.Devices WITH (UPDLOCK,HOLDLOCK) WHERE DeviceID=? AND OwnerCustomerID IS NULL",Long.class,id).isEmpty()) throw CatalogProblem.missing();
    }
    public List<Device> search(String q,Long productId,Long categoryId,Long brandId,String grade,BigDecimal minPrice,BigDecimal maxPrice,int page) {
        var sql=new StringBuilder(SELECT);List<Object> args=new ArrayList<>();
        if(!q.isBlank()) {
            String like="%"+q.replace("\\","\\\\").replace("%","\\%").replace("_","\\_").replace("[","\\[")+"%";
            sql.append(" AND (UPPER(d.AssetTag) LIKE UPPER(?) ESCAPE '\\' OR UPPER(d.ManufacturerSerial) LIKE UPPER(?) ESCAPE '\\' OR UPPER(p.Name) LIKE UPPER(?) ESCAPE '\\')");
            args.add(like);args.add(like);args.add(like);
        }
        if(productId!=null) { sql.append(" AND d.ProductID=?");args.add(productId); }
        if(categoryId!=null) { sql.append(" AND p.CategoryID=?");args.add(categoryId); }
        if(brandId!=null) { sql.append(" AND p.BrandID=?");args.add(brandId); }
        if(grade!=null&&!grade.isBlank()) { sql.append(" AND d.ConditionGrade=?");args.add(grade); }
        if(minPrice!=null) { sql.append(" AND d.AskingPrice>=?");args.add(minPrice); }
        if(maxPrice!=null) { sql.append(" AND d.AskingPrice<=?");args.add(maxPrice); }
        sql.append(" ORDER BY d.DeviceID DESC OFFSET ? ROWS FETCH NEXT 26 ROWS ONLY");args.add(page*25);
        return jdbc.query(sql.toString(),mapper,args.toArray());
    }
    public List<ProductChoice> choices(Long includeId) {
        return jdbc.query("SELECT ProductID,SKU,Name,IsActive FROM dbo.Products WHERE (StockMode='SERIALIZED' AND (IsActive=1 OR ProductID IN (SELECT ProductID FROM dbo.Devices WHERE OwnerCustomerID IS NULL))) OR ProductID=? ORDER BY Name,ProductID",
            (r,n)->new ProductChoice(r.getLong(1),r.getString(2),r.getString(3),r.getBoolean(4)),includeId);
    }
    public List<Option> categories() { return jdbc.query("SELECT CategoryID,Name FROM dbo.Categories ORDER BY Name",(r,n)->new Option(r.getLong(1),r.getString(2))); }
    public List<Option> brands() { return jdbc.query("SELECT BrandID,Name FROM dbo.Brands ORDER BY Name",(r,n)->new Option(r.getLong(1),r.getString(2))); }
    public void requireProduct(long productId) {
        var modes=jdbc.queryForList("SELECT StockMode FROM dbo.Products WITH (UPDLOCK,HOLDLOCK) WHERE ProductID=? AND IsActive=1",String.class,productId);
        if(modes.isEmpty()||!modes.getFirst().equals("SERIALIZED")) throw CatalogProblem.invalid("Chọn mẫu sản phẩm đang kinh doanh thuộc loại SERIALIZED.");
    }
    public long insert(DeviceInput d) {
        var key=new org.springframework.jdbc.support.GeneratedKeyHolder();
        jdbc.update(c->{ var s=c.prepareStatement("INSERT INTO dbo.Devices(AssetTag,ProductID,ManufacturerSerial,DeviceDescription,ConditionGrade,AskingPrice,FaultDescription) VALUES (?,?,?,?,?,?,?)",new String[]{"DeviceID"});
            s.setString(1,d.assetTag());s.setLong(2,d.productId());s.setString(3,d.manufacturerSerial());s.setString(4,d.description());s.setString(5,d.conditionGrade());s.setBigDecimal(6,d.askingPrice());s.setString(7,d.faultDescription());return s;},key);
        return Objects.requireNonNull(key.getKey()).longValue();
    }
    public int update(long id,DeviceInput d,byte[] version) {
        return jdbc.update("UPDATE dbo.Devices SET AssetTag=?,ProductID=?,ManufacturerSerial=?,DeviceDescription=?,ConditionGrade=?,AskingPrice=?,FaultDescription=?,UpdatedAt=? WHERE DeviceID=? AND OwnerCustomerID IS NULL AND Version=?",
            d.assetTag(),d.productId(),d.manufacturerSerial(),d.description(),d.conditionGrade(),d.askingPrice(),d.faultDescription(),LocalDateTime.now(ZoneOffset.UTC),id,version);
    }
    public int touch(long id,byte[] version) { return jdbc.update("UPDATE dbo.Devices SET UpdatedAt=? WHERE DeviceID=? AND OwnerCustomerID IS NULL AND Version=?",LocalDateTime.now(ZoneOffset.UTC),id,version); }
    public boolean duplicateSerial(long id,String serial) {
        if(serial==null||serial.isBlank()) return false;
        return jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Devices WHERE OwnerCustomerID IS NULL AND DeviceID<>? AND UPPER(LTRIM(RTRIM(ManufacturerSerial)))=UPPER(?)",Integer.class,id,serial)>0;
    }
    public List<Definition> definitions() {
        return jdbc.query("SELECT AttributeID,AttributeCode,Name,UnitLabel,ValueType FROM dbo.AttributeDefinitions ORDER BY AttributeID",
            (r,n)->new Definition(r.getLong(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5)));
    }
    public List<Specification> specifications(long id) {
        return jdbc.query("SELECT a.AttributeCode,a.Name,a.UnitLabel,a.ValueType,v.ValueText FROM dbo.DeviceAttributeValues v JOIN dbo.AttributeDefinitions a ON a.AttributeID=v.AttributeID WHERE v.DeviceID=? ORDER BY a.AttributeID",
            (r,n)->new Specification(r.getString(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5)),id);
    }
    public void specifications(long id,Map<Long,String> values) {
        jdbc.update("DELETE FROM dbo.DeviceAttributeValues WHERE DeviceID=?",id);
        values.forEach((key,value)->jdbc.update("INSERT INTO dbo.DeviceAttributeValues(DeviceID,AttributeID,ValueText) VALUES (?,?,?)",id,key,value));
    }
    public List<Photo> images(long id) { return jdbc.query("SELECT ImageID,SortOrder FROM dbo.DeviceImages WHERE DeviceID=? ORDER BY SortOrder,ImageID",(r,n)->new Photo(r.getLong(1),r.getInt(2),"/api/admin/devices/"+id+"/images/"+r.getLong(1)+"/content"),id); }
    public String imageKey(long deviceId,long imageId) {
        return jdbc.queryForList("SELECT ImageURL FROM dbo.DeviceImages WHERE DeviceID=? AND ImageID=?",String.class,deviceId,imageId).stream().findFirst().orElseThrow(CatalogProblem::missing);
    }
    public long addImage(long id,String key) {
        Integer order=jdbc.queryForObject("SELECT COALESCE(MAX(SortOrder),-1)+1 FROM dbo.DeviceImages WHERE DeviceID=?",Integer.class,id);
        var holder=new org.springframework.jdbc.support.GeneratedKeyHolder();
        jdbc.update(c->{var s=c.prepareStatement("INSERT INTO dbo.DeviceImages(DeviceID,ImageURL,SortOrder) VALUES (?,?,?)",new String[]{"ImageID"});s.setLong(1,id);s.setString(2,key);s.setInt(3,order);return s;},holder);
        return Objects.requireNonNull(holder.getKey()).longValue();
    }
    public void removeImage(long id,long imageId) { jdbc.update("DELETE FROM dbo.DeviceImages WHERE DeviceID=? AND ImageID=?",id,imageId); }
    public void reorder(long id,List<Long> ids) {
        // Tạm chuyển về một khoảng không trùng; tránh va chạm UNIQUE(DeviceID,SortOrder).
        int max=jdbc.queryForObject("SELECT COALESCE(MAX(SortOrder),-1) FROM dbo.DeviceImages WHERE DeviceID=?",Integer.class,id);
        if(max>Integer.MAX_VALUE-100) throw CatalogProblem.invalid("Thứ tự ảnh vượt giới hạn.");
        for(int i=0;i<ids.size();i++) jdbc.update("UPDATE dbo.DeviceImages SET SortOrder=? WHERE DeviceID=? AND ImageID=?",max+1+i,id,ids.get(i));
        for(int i=0;i<ids.size();i++) jdbc.update("UPDATE dbo.DeviceImages SET SortOrder=? WHERE DeviceID=? AND ImageID=?",i,id,ids.get(i));
    }
    public List<Inspection> checks(long id) {
        return jdbc.query("SELECT k.CheckID,u.LoginName,k.CheckedAt,k.Findings,k.Result FROM dbo.DeviceChecks k JOIN dbo.Users u ON u.UserID=k.CheckedBy WHERE k.DeviceID=? ORDER BY k.CheckedAt DESC,k.CheckID DESC",
            (r,n)->new Inspection(r.getLong(1),r.getString(2),r.getObject(3,LocalDateTime.class),r.getString(4),r.getString(5)),id);
    }
    public void check(long id,String username,CheckInput c) {
        long actor=jdbc.queryForObject("SELECT UserID FROM dbo.Users WHERE LoginName=?",Long.class,username);
        jdbc.update("INSERT INTO dbo.DeviceChecks(DeviceID,CheckedBy,Findings,Result) VALUES (?,?,?,?)",id,actor,c.findings(),c.result());
    }
}
