-- Chạy trong SSMS trên đúng database ứng dụng.
USE TIVAREK_WEEK8_DEV;
GO
SELECT installed_rank,version,description,success FROM dbo.flyway_schema_history ORDER BY installed_rank;
SELECT p.ProductID,p.SKU,p.Name,c.Name AS CategoryName,b.Name AS BrandName,
       u.Name AS UnitName,p.StockMode,p.ListPrice,p.IsActive,p.Revision,p.Description
FROM dbo.Products p JOIN dbo.Categories c ON c.CategoryID=p.CategoryID
LEFT JOIN dbo.Brands b ON b.BrandID=p.BrandID JOIN dbo.Units u ON u.UnitID=p.UnitID
ORDER BY p.ProductID DESC;
SELECT r.RoleCode,p.PermissionCode FROM dbo.RolePermissions rp
JOIN dbo.Roles r ON r.RoleID=rp.RoleID JOIN dbo.Permissions p ON p.PermissionID=rp.PermissionID
WHERE p.PermissionCode IN ('PRODUCT_READ','PRODUCT_WRITE','CATALOG_WRITE') ORDER BY r.RoleCode,p.PermissionCode;
SELECT TOP (100) a.OccurredAt,u.LoginName,a.Action,a.EntityType,a.EntityKey,a.Summary
FROM dbo.AuditLogs a LEFT JOIN dbo.Users u ON u.UserID=a.ActorID
WHERE a.EntityType IN ('PRODUCT','CATEGORIES','BRANDS','UNITS') ORDER BY a.AuditID DESC;
-- Kết quả phải rỗng: không có SKU chuẩn hóa trùng.
SELECT UPPER(LTRIM(RTRIM(SKU))) AS NormalizedSKU,COUNT(*) AS Total
FROM dbo.Products GROUP BY UPPER(LTRIM(RTRIM(SKU))) HAVING COUNT(*)>1;
