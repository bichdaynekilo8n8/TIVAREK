package com.tivarek.week13;

import com.tivarek.week10.CatalogProblem;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** Sổ kho POSTED là nguồn tồn duy nhất; không bao giờ lấy số tồn từ Products. */
@Repository
public class StorefrontRepository {
    private final JdbcTemplate jdbc;
    public StorefrontRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private static final String CATALOG = """
        WITH BulkStock AS (
          SELECT e.ProductID, SUM(CAST(e.Delta AS bigint)) AS Qty
          FROM dbo.BulkStockEntries e
          JOIN dbo.InventoryDocuments doc ON doc.InventoryDocID=e.InventoryDocID AND doc.Status='POSTED'
          JOIN dbo.StockLocations l ON l.LocationID=e.LocationID AND l.LocationType='SALEABLE' AND l.IsActive=1
          GROUP BY e.ProductID
        ), BulkHeld AS (
          SELECT lines.ProductID, SUM(CAST(r.Quantity AS bigint)) AS Qty
          FROM dbo.BulkReservations r
          JOIN dbo.BulkSaleLines lines ON lines.SaleLineID=r.SaleLineID
          JOIN dbo.StockLocations l ON l.LocationID=r.LocationID AND l.LocationType='SALEABLE' AND l.IsActive=1
          WHERE r.Status='ACTIVE' AND r.ExpiresAt>?
          GROUP BY lines.ProductID
        ), LatestDeviceMove AS (
          SELECT m.DeviceID,m.ToLocationID,
                 ROW_NUMBER() OVER(PARTITION BY m.DeviceID ORDER BY doc.OccurredAt DESC,doc.InventoryDocID DESC,m.DeviceEntryID DESC) AS seq
          FROM dbo.DeviceStockEntries m
          JOIN dbo.InventoryDocuments doc ON doc.InventoryDocID=m.InventoryDocID AND doc.Status='POSTED'
        ), PublicCatalog AS (
          SELECT 'BULK' AS Kind,p.ProductID AS ItemId,p.SKU AS Code,p.Name AS ItemName,
                 c.Name AS CategoryName,b.Name AS BrandName,CAST(NULL AS varchar(24)) AS Grade,
                 p.ListPrice AS Price,
                 (SELECT TOP 1 i.ImageURL FROM dbo.ProductImages i WHERE i.ProductID=p.ProductID ORDER BY i.SortOrder,i.ImageID) AS ImageURL,
                 CAST(NULL AS bigint) AS CoverImageId,
                 COALESCE(stock.Qty,0)-COALESCE(held.Qty,0) AS Available,
                 p.CategoryID AS CategoryId,p.BrandID AS BrandId
          FROM dbo.Products p
          JOIN dbo.Categories c ON c.CategoryID=p.CategoryID AND c.IsActive=1
          LEFT JOIN dbo.Brands b ON b.BrandID=p.BrandID
          LEFT JOIN BulkStock stock ON stock.ProductID=p.ProductID
          LEFT JOIN BulkHeld held ON held.ProductID=p.ProductID
          WHERE p.StockMode='BULK' AND p.IsActive=1 AND p.IsPublished=1
          UNION ALL
          SELECT 'DEVICE',d.DeviceID,d.AssetTag,p.Name,c.Name,b.Name,d.ConditionGrade,d.AskingPrice,
                 CAST(NULL AS nvarchar(600)),
                 (SELECT TOP 1 i.ImageID FROM dbo.DeviceImages i WHERE i.DeviceID=d.DeviceID ORDER BY i.SortOrder,i.ImageID),
                 CASE WHEN l.LocationType='SALEABLE' AND l.IsActive=1 AND held.DeviceID IS NULL THEN CAST(1 AS bigint) ELSE CAST(0 AS bigint) END,
                 p.CategoryID,p.BrandID
          FROM dbo.Devices d
          JOIN dbo.Products p ON p.ProductID=d.ProductID AND p.StockMode='SERIALIZED' AND p.IsActive=1
          JOIN dbo.Categories c ON c.CategoryID=p.CategoryID AND c.IsActive=1
          LEFT JOIN dbo.Brands b ON b.BrandID=p.BrandID
          LEFT JOIN LatestDeviceMove movement ON movement.DeviceID=d.DeviceID AND movement.seq=1
          LEFT JOIN dbo.StockLocations l ON l.LocationID=movement.ToLocationID
          LEFT JOIN dbo.ActiveDeviceReservations held ON held.DeviceID=d.DeviceID
          WHERE d.OwnerCustomerID IS NULL AND d.IsPublished=1 AND d.AskingPrice IS NOT NULL
        )
        """;

    private static final RowMapper<StorefrontProductView> MAPPER = (rs, rowNum) -> {
        long available = rs.getLong("Available");
        return new StorefrontProductView(rs.getString("Kind"),rs.getLong("ItemId"),rs.getString("Code"),
            rs.getString("ItemName"),rs.getString("CategoryName"),rs.getString("BrandName"),
            rs.getString("Grade"),rs.getBigDecimal("Price"),rs.getString("ImageURL"),
            rs.getObject("CoverImageId",Long.class),
            (int)Math.max(0,Math.min(Integer.MAX_VALUE,available)),rs.getLong("CategoryId"),
            rs.getObject("BrandId",Long.class));
    };

    /** SEO needs published identifiers only, never stock totals or customer-owned devices.
     * 45,000 keeps the sitemap below Google's 50,000 URL limit for this small shop. */
    public List<String> sitemapPaths() {
        return jdbc.query("""
            SELECT TOP 45000 Kind, ItemId FROM (
              SELECT 'phu-kien' AS Kind,p.ProductID AS ItemId
              FROM dbo.Products p JOIN dbo.Categories c ON c.CategoryID=p.CategoryID AND c.IsActive=1
              WHERE p.StockMode='BULK' AND p.IsActive=1 AND p.IsPublished=1
              UNION ALL
              SELECT 'thiet-bi',d.DeviceID FROM dbo.Devices d
              JOIN dbo.Products p ON p.ProductID=d.ProductID AND p.StockMode='SERIALIZED' AND p.IsActive=1
              JOIN dbo.Categories c ON c.CategoryID=p.CategoryID AND c.IsActive=1
              WHERE d.OwnerCustomerID IS NULL AND d.IsPublished=1 AND d.AskingPrice IS NOT NULL
            ) visible ORDER BY Kind,ItemId
            """, (rs,n)->"/san-pham/"+rs.getString(1)+"/"+rs.getLong(2));
    }

    public record Results(List<StorefrontProductView> items,long total) { }
    private record Conditions(String sql,List<Object> params) { }

    private Conditions conditions(StorefrontSearch s) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (!s.q().isBlank()) {
            String term="%" + s.q().replace("[","[[]").replace("%","[%]").replace("_","[_]") + "%";
            where.append(" AND (UPPER(v.ItemName) LIKE UPPER(?) OR UPPER(v.Code) LIKE UPPER(?) OR UPPER(v.BrandName) LIKE UPPER(?))");
            args.add(term);args.add(term);args.add(term);
        }
        if (s.categoryId()!=null) {where.append(" AND v.CategoryId=?");args.add(s.categoryId());}
        if (s.brandId()!=null) {where.append(" AND v.BrandId=?");args.add(s.brandId());}
        if (s.type().equals("bulk")) where.append(" AND v.Kind='BULK'");
        if (s.type().equals("device")) where.append(" AND v.Kind='DEVICE'");
        if (!s.grade().isBlank()) {where.append(" AND v.Grade=?");args.add(s.grade());}
        if (s.minPrice()!=null) {where.append(" AND v.Price>=?");args.add(s.minPrice());}
        if (s.maxPrice()!=null) {where.append(" AND v.Price<=?");args.add(s.maxPrice());}
        return new Conditions(where.toString(),args);
    }

    public Results search(StorefrontSearch s) {
        Conditions where=conditions(s);
        List<Object> countArgs=new ArrayList<>();countArgs.add(LocalDateTime.now(ZoneOffset.UTC));countArgs.addAll(where.params());
        Long total=jdbc.queryForObject(CATALOG+"SELECT COUNT(*) FROM PublicCatalog v"+where.sql(),Long.class,countArgs.toArray());
        String order=switch(s.sort()) {
            case "price_asc" -> "v.Price ASC,v.ItemId DESC,v.Kind DESC";
            case "price_desc" -> "v.Price DESC,v.ItemId DESC,v.Kind DESC";
            case "name" -> "v.ItemName ASC,v.ItemId DESC,v.Kind DESC";
            default -> "v.ItemId DESC,v.Kind DESC";
        };
        List<Object> args=new ArrayList<>();args.add(countArgs.getFirst());args.addAll(where.params());
        args.add(s.page()*StorefrontSearch.PAGE_SIZE);args.add(StorefrontSearch.PAGE_SIZE);
        var rows=jdbc.query(CATALOG+"SELECT v.* FROM PublicCatalog v"+where.sql()+
             " ORDER BY "+order+" OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",MAPPER,args.toArray());
        return new Results(rows,total == null ? 0 : total);
    }

    public StorefrontProductView detail(String kind,long id) {
        if(id<=0) throw CatalogProblem.missing();
        return jdbc.query(CATALOG+"SELECT v.* FROM PublicCatalog v WHERE v.Kind=? AND v.ItemId=?",MAPPER,
                LocalDateTime.now(ZoneOffset.UTC),kind,id)
            .stream().findFirst().orElseThrow(CatalogProblem::missing);
    }

    public List<StorefrontDeviceView.Spec> productSpecifications(long id) {
        return jdbc.query("""
            SELECT a.Name,v.ValueText,a.UnitLabel FROM dbo.ProductAttributeValues v
            JOIN dbo.AttributeDefinitions a ON a.AttributeID=v.AttributeID
            WHERE v.ProductID=? ORDER BY a.AttributeID
            """,(rs,n)->spec(rs),id);
    }
    public List<StorefrontDeviceView.Spec> deviceSpecifications(long id) {
        return jdbc.query("""
            SELECT a.Name,v.ValueText,a.UnitLabel FROM dbo.DeviceAttributeValues v
            JOIN dbo.AttributeDefinitions a ON a.AttributeID=v.AttributeID
            WHERE v.DeviceID=? ORDER BY a.AttributeID
            """,(rs,n)->spec(rs),id);
    }
    private static StorefrontDeviceView.Spec spec(ResultSet rs) throws SQLException {
        return new StorefrontDeviceView.Spec(rs.getString(1),rs.getString(2),rs.getString(3));
    }
    public String description(long id) {
        return jdbc.queryForObject("SELECT DeviceDescription FROM dbo.Devices WHERE DeviceID=?",String.class,id);
    }
    public String inspection(long id) {
        return jdbc.query("SELECT TOP 1 Result FROM dbo.DeviceChecks WHERE DeviceID=? ORDER BY CheckedAt DESC,CheckID DESC",
            (rs,n)->rs.getString(1),id).stream().findFirst().orElse("NOT_CHECKED");
    }
    public List<StorefrontDeviceView.Photo> deviceImages(long id) {
        return jdbc.query("SELECT ImageID FROM dbo.DeviceImages WHERE DeviceID=? ORDER BY SortOrder,ImageID",
            (rs,n)->new StorefrontDeviceView.Photo("/media/public/devices/"+id+"/images/"+rs.getLong(1),"Ảnh thiết bị "+(n+1)),id);
    }
    public List<StorefrontDeviceView.Photo> productImages(long id) {
        return jdbc.query("SELECT ImageURL FROM dbo.ProductImages WHERE ProductID=? ORDER BY SortOrder,ImageID",
            (rs,n)->new StorefrontDeviceView.Photo(safeProductUrl(rs.getString(1)),"Ảnh sản phẩm "+(n+1)),id)
            .stream().filter(p->p.url()!=null).toList();
    }
    public List<ManagedImage> managedImages(long id) {
        return jdbc.query("SELECT ImageID,ImageURL FROM dbo.ProductImages WHERE ProductID=? ORDER BY SortOrder,ImageID",
            (rs,n)->new ManagedImage(rs.getLong(1),rs.getString(2)),id);
    }
    public record ManagedImage(long id,String url) { }
    public boolean bulkProductExists(long id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Products WHERE ProductID=? AND StockMode='BULK'",
            Integer.class,id)>0;
    }
    public void addProductImage(long id,String url) {
        Integer order=jdbc.queryForObject("SELECT COALESCE(MAX(SortOrder),-1)+1 FROM dbo.ProductImages WHERE ProductID=?",
            Integer.class,id);
        jdbc.update("INSERT INTO dbo.ProductImages(ProductID,ImageURL,SortOrder) VALUES (?,?,?)",id,url,order);
    }
    public int removeProductImage(long id,long imageId) {
        return jdbc.update("DELETE FROM dbo.ProductImages WHERE ProductID=? AND ImageID=?",id,imageId);
    }
    private static String safeProductUrl(String value) {
        return new StorefrontProductView("BULK",0,"","","","",null,BigDecimal.ZERO,value,null,0,0,null).imageUrl();
    }
    public List<Option> categories() {
        return jdbc.query("SELECT CategoryID,Name FROM dbo.Categories WHERE IsActive=1 ORDER BY Name,CategoryID",
            (rs,n)->new Option(rs.getLong(1),rs.getString(2)));
    }
    public List<Option> brands() {
        return jdbc.query("SELECT BrandID,Name FROM dbo.Brands ORDER BY Name,BrandID",
            (rs,n)->new Option(rs.getLong(1),rs.getString(2)));
    }
    public record Option(long id,String name) { }

    public List<Publication> manageProducts() {
        return jdbc.query("""
            SELECT p.ProductID,p.SKU,p.Name,p.IsPublished FROM dbo.Products p
            WHERE p.StockMode='BULK' ORDER BY p.ProductID DESC
            """,(rs,n)->new Publication(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getBoolean(4)));
    }
    public List<Publication> manageDevices() {
        return jdbc.query("""
            SELECT d.DeviceID,d.AssetTag,COALESCE(p.Name,d.DeviceDescription),d.IsPublished
            FROM dbo.Devices d LEFT JOIN dbo.Products p ON p.ProductID=d.ProductID
            WHERE d.OwnerCustomerID IS NULL ORDER BY d.DeviceID DESC
            """,(rs,n)->new Publication(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getBoolean(4)));
    }
    public record Publication(long id,String code,String name,boolean published) { }

    public boolean productReady(long id) {
        return jdbc.queryForObject("""
            SELECT COUNT(*) FROM dbo.Products WHERE ProductID=? AND StockMode='BULK' AND IsActive=1
            """,Integer.class,id)>0;
    }
    public boolean deviceReady(long id) {
        return jdbc.queryForObject("""
            SELECT COUNT(*) FROM dbo.Devices d
            JOIN dbo.Products p ON p.ProductID=d.ProductID AND p.StockMode='SERIALIZED' AND p.IsActive=1
            WHERE d.DeviceID=? AND d.OwnerCustomerID IS NULL AND d.AskingPrice IS NOT NULL
            """,Integer.class,id)>0;
    }
    public int publish(String kind,long id,boolean published) {
        if ("BULK".equals(kind)) return jdbc.update("""
            UPDATE dbo.Products SET IsPublished=? WHERE ProductID=? AND StockMode='BULK'
            """,published,id);
        return jdbc.update("""
            UPDATE dbo.Devices SET IsPublished=? WHERE DeviceID=? AND OwnerCustomerID IS NULL
            """,published,id);
    }
    /** Bảo đảm media riêng của khách hàng không thể bị lộ dù đoán trúng ID. */
    public String publicImageKey(long deviceId,long imageId) {
        return jdbc.query("""
            SELECT i.ImageURL FROM dbo.DeviceImages i
            JOIN dbo.Devices d ON d.DeviceID=i.DeviceID AND d.OwnerCustomerID IS NULL AND d.IsPublished=1
            JOIN dbo.Products p ON p.ProductID=d.ProductID AND p.IsActive=1 AND p.StockMode='SERIALIZED'
            WHERE i.DeviceID=? AND i.ImageID=? AND d.AskingPrice IS NOT NULL
            """,(rs,n)->rs.getString(1),deviceId,imageId).stream().findFirst().orElseThrow(CatalogProblem::missing);
    }
}
