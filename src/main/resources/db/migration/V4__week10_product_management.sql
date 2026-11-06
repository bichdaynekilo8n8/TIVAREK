-- Nâng cấp tại chỗ từ Tuần 9; không sửa checksum các migration cũ.
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
GO
ALTER TABLE dbo.Products ADD Description nvarchar(2000) NULL,
    Revision int NOT NULL CONSTRAINT DF_Products_Revision DEFAULT (0),
    CreatedAt datetime2(3) NOT NULL CONSTRAINT DF_Products_CreatedAt DEFAULT SYSUTCDATETIME(),
    UpdatedAt datetime2(3) NOT NULL CONSTRAINT DF_Products_UpdatedAt DEFAULT SYSUTCDATETIME();
ALTER TABLE dbo.Categories ADD Revision int NOT NULL CONSTRAINT DF_Categories_Revision DEFAULT (0);
ALTER TABLE dbo.Brands ADD IsActive bit NOT NULL CONSTRAINT DF_Brands_Active DEFAULT (1),
    Revision int NOT NULL CONSTRAINT DF_Brands_Revision DEFAULT (0);
ALTER TABLE dbo.Units ADD IsActive bit NOT NULL CONSTRAINT DF_Units_Active DEFAULT (1),
    Revision int NOT NULL CONSTRAINT DF_Units_Revision DEFAULT (0);
GO
-- Khóa chuẩn hóa bảo đảm SKU/mã không trùng dù DB phân biệt hoa thường.
ALTER TABLE dbo.Products ADD SKUKey AS UPPER(LTRIM(RTRIM(SKU))) PERSISTED;
CREATE UNIQUE INDEX UX_Products_SKUKey ON dbo.Products(SKUKey);
ALTER TABLE dbo.Categories ADD CodeKey AS UPPER(LTRIM(RTRIM(CategoryCode))) PERSISTED;
CREATE UNIQUE INDEX UX_Categories_CodeKey ON dbo.Categories(CodeKey);
ALTER TABLE dbo.Brands ADD CodeKey AS UPPER(LTRIM(RTRIM(BrandCode))) PERSISTED;
CREATE UNIQUE INDEX UX_Brands_CodeKey ON dbo.Brands(CodeKey);
ALTER TABLE dbo.Units ADD CodeKey AS UPPER(LTRIM(RTRIM(UnitCode))) PERSISTED;
CREATE UNIQUE INDEX UX_Units_CodeKey ON dbo.Units(CodeKey);
ALTER TABLE dbo.Products ADD CONSTRAINT CK_Products_NameNotBlank CHECK (LEN(LTRIM(RTRIM(Name)))>0),
    CONSTRAINT CK_Products_SKUNotBlank CHECK (LEN(LTRIM(RTRIM(SKU)))>0);
CREATE INDEX IX_Products_CatalogSearch ON dbo.Products(IsActive,CategoryID,BrandID,ProductID);
GO
INSERT dbo.Permissions(PermissionCode,Name)
SELECT v.Code,v.Name FROM (VALUES
 ('PRODUCT_READ',N'Xem sản phẩm nội bộ'),('PRODUCT_WRITE',N'Thêm, sửa, ngừng kinh doanh sản phẩm'),
 ('CATALOG_WRITE',N'Quản lý danh mục, thương hiệu, đơn vị tính')
) v(Code,Name) WHERE NOT EXISTS (SELECT 1 FROM dbo.Permissions p WHERE p.PermissionCode=v.Code);
INSERT dbo.RolePermissions(RoleID,PermissionID)
SELECT r.RoleID,p.PermissionID FROM (VALUES
 ('OWNER','PRODUCT_READ'),('OWNER','PRODUCT_WRITE'),('OWNER','CATALOG_WRITE'),
 ('SALES','PRODUCT_READ'),('INVENTORY','PRODUCT_READ'),
 ('INVENTORY','PRODUCT_WRITE'),('INVENTORY','CATALOG_WRITE')
) v(RoleCode,PermissionCode)
JOIN dbo.Roles r ON r.RoleCode=v.RoleCode
JOIN dbo.Permissions p ON p.PermissionCode=v.PermissionCode
WHERE NOT EXISTS (SELECT 1 FROM dbo.RolePermissions rp WHERE rp.RoleID=r.RoleID AND rp.PermissionID=p.PermissionID);
