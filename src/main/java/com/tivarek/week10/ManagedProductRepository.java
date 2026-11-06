package com.tivarek.week10;
import java.util.*;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Repository;
@Repository
public class ManagedProductRepository {
    private final JdbcTemplate jdbc;
    public ManagedProductRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    private static final String SELECT="""
        SELECT p.ProductID,p.SKU,p.Name,p.CategoryID,c.Name AS CategoryName,
          p.BrandID,b.Name AS BrandName,p.UnitID,u.Name AS UnitName,p.StockMode,p.ListPrice,
          p.Description,p.IsActive,p.Revision
        FROM dbo.Products p JOIN dbo.Categories c ON c.CategoryID=p.CategoryID
        LEFT JOIN dbo.Brands b ON b.BrandID=p.BrandID JOIN dbo.Units u ON u.UnitID=p.UnitID
        """;
    private final RowMapper<ProductView> mapper=(r,n)->new ProductView(r.getLong("ProductID"),r.getString("SKU"),r.getString("Name"),
        r.getLong("CategoryID"),r.getString("CategoryName"),r.getObject("BrandID",Long.class),r.getString("BrandName"),
        r.getLong("UnitID"),r.getString("UnitName"),r.getString("StockMode"),r.getBigDecimal("ListPrice"),
        r.getString("Description"),r.getBoolean("IsActive"),r.getInt("Revision"));
    public List<ProductView> search(String q,Long categoryId,Long brandId,String mode,Boolean active,int page) {
        StringBuilder sql=new StringBuilder(SELECT+" WHERE 1=1");
        List<Object> args=new ArrayList<>();
        if (!q.isBlank()) {
            String like="%"+q.replace("\\","\\\\").replace("%","\\%").replace("_","\\_").replace("[","\\[")+"%";
            sql.append(" AND (UPPER(p.SKU) LIKE UPPER(?) ESCAPE '\\' OR UPPER(p.Name) LIKE UPPER(?) ESCAPE '\\')");
            args.add(like);args.add(like);
        }
        if(categoryId!=null) { sql.append(" AND p.CategoryID=?");args.add(categoryId); }
        if(brandId!=null) { sql.append(" AND p.BrandID=?");args.add(brandId); }
        if(mode!=null&&!mode.isBlank()) { sql.append(" AND p.StockMode=?");args.add(mode); }
        if(active!=null) { sql.append(" AND p.IsActive=?");args.add(active); }
        // Lấy thêm 1 dòng để xác định còn trang sau, không tải toàn bảng vào RAM.
        sql.append(" ORDER BY p.ProductID DESC OFFSET ? ROWS FETCH NEXT 26 ROWS ONLY");args.add(page*25);
        return jdbc.query(sql.toString(),mapper,args.toArray());
    }
    public ProductView get(long id) {
        return jdbc.query(SELECT+" WHERE p.ProductID=?",mapper,id).stream().findFirst().orElseThrow(CatalogProblem::missing);
    }
    public long insert(ProductInput p) {
        var key=new org.springframework.jdbc.support.GeneratedKeyHolder();
        jdbc.update(c->{ var s=c.prepareStatement("INSERT INTO dbo.Products(SKU,Name,CategoryID,BrandID,UnitID,StockMode,ListPrice,Description) VALUES (?,?,?,?,?,?,?,?)",new String[]{"ProductID"});
            s.setString(1,p.sku());s.setString(2,p.name());s.setLong(3,p.categoryId());s.setObject(4,p.brandId());s.setLong(5,p.unitId());s.setString(6,p.stockMode());s.setBigDecimal(7,p.listPrice());s.setString(8,p.description());return s; },key);
        return Objects.requireNonNull(key.getKey()).longValue();
    }
    public int update(long id,ProductInput p) {
        return jdbc.update("""
            UPDATE dbo.Products SET SKU=?,Name=?,CategoryID=?,BrandID=?,UnitID=?,ListPrice=?,Description=?,
            Revision=Revision+1,UpdatedAt=? WHERE ProductID=? AND Revision=?
            """,p.sku(),p.name(),p.categoryId(),p.brandId(),p.unitId(),p.listPrice(),p.description(),java.time.LocalDateTime.now(java.time.ZoneOffset.UTC),id,p.revision());
    }
    public int status(long id,StatusInput s) {
        return jdbc.update("UPDATE dbo.Products SET IsActive=?,Revision=Revision+1,UpdatedAt=? WHERE ProductID=? AND Revision=?",s.active(),java.time.LocalDateTime.now(java.time.ZoneOffset.UTC),id,s.revision());
    }
}
