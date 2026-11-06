package com.tivarek.week8;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogService {
    private final JdbcTemplate jdbc;
    public CatalogService(JdbcTemplate jdbc) { this.jdbc=jdbc; }

    @Transactional(readOnly=true)
    public List<ProductView> products() {
        // DTO công khai chỉ lấy giá bán, tuyệt đối không SELECT *.
        return jdbc.query("""
            SELECT TOP (100) ProductID,SKU,Name,ListPrice FROM dbo.Products
            WHERE IsActive=1 AND IsPublished=1 AND StockMode='BULK' ORDER BY ProductID
            """,(rs,n) -> new ProductView(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getBigDecimal(4)));
    }

    @PreAuthorize("hasAuthority('COST_READ')")
    @Transactional(readOnly=true)
    public List<CostView> costs() {
        // Giá nhập của từng dòng phiếu nhập đã ghi sổ; KHÔNG phải COGS hay giá vốn bình quân.
        return jdbc.query("""
            SELECT TOP (100) StockType,EntryID,ItemCode,ItemName,UnitCost,DocumentCode
            FROM (
                SELECT 'BULK' AS StockType,e.BulkEntryID AS EntryID,p.SKU AS ItemCode,
                       p.Name AS ItemName,e.UnitCost,d.DocumentCode,d.OccurredAt
                FROM dbo.BulkStockEntries e
                JOIN dbo.Products p ON p.ProductID=e.ProductID
                JOIN dbo.InventoryDocuments d ON d.InventoryDocID=e.InventoryDocID
                WHERE d.DocumentType='RECEIPT' AND d.Status='POSTED' AND e.Delta>0
                UNION ALL
                SELECT 'DEVICE',e.DeviceEntryID,v.AssetTag,v.DeviceDescription,e.UnitCost,d.DocumentCode,d.OccurredAt
                FROM dbo.DeviceStockEntries e
                JOIN dbo.Devices v ON v.DeviceID=e.DeviceID
                JOIN dbo.InventoryDocuments d ON d.InventoryDocID=e.InventoryDocID
                WHERE d.DocumentType='RECEIPT' AND d.Status='POSTED'
                  AND e.FromLocationID IS NULL AND e.ToLocationID IS NOT NULL
            ) x ORDER BY OccurredAt DESC,StockType,EntryID DESC
            """,(rs,n) -> new CostView(rs.getString(1),rs.getLong(2),rs.getString(3),rs.getString(4),
                                      rs.getBigDecimal(5),rs.getString(6)));
    }
    public record ProductView(long productId,String sku,String name,BigDecimal listPrice) {}
    public record CostView(String stockType,long entryId,String itemCode,String itemName,
                           BigDecimal unitCost,String documentCode) {}
}
