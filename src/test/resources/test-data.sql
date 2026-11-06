-- Tuần 9: thêm dữ liệu phân quyền; không thay V1/V1_1/V2, không tạo lại bảng.

INSERT INTO dbo.Roles(RoleCode,Name)
SELECT v.Code,v.Name FROM (VALUES
 ('OWNER',N'Chủ cửa hàng'),('SALES',N'Bán hàng'),('TECHNICIAN',N'Kỹ thuật'),
 ('INVENTORY',N'Quản lý kho'),('CUSTOMER',N'Khách hàng')
) v(Code,Name) WHERE NOT EXISTS (SELECT 1 FROM dbo.Roles r WHERE r.RoleCode=v.Code);

INSERT INTO dbo.Permissions(PermissionCode,Name)
SELECT v.Code,v.Name FROM (VALUES
 ('ADMIN_ACCESS',N'Vào trang quản trị'),('CUSTOMER_READ',N'Xem danh sách khách hàng'),
 ('CUSTOMER_WRITE',N'Tạo và cập nhật khách hàng'),('CUSTOMER_SELF_READ',N'Xem hồ sơ cá nhân'),
 ('COST_READ',N'Xem giá nhập nội bộ'),('AUDIT_READ',N'Xem nhật ký thao tác')
) v(Code,Name) WHERE NOT EXISTS (SELECT 1 FROM dbo.Permissions p WHERE p.PermissionCode=v.Code);

INSERT INTO dbo.RolePermissions(RoleID,PermissionID)
SELECT r.RoleID,p.PermissionID FROM (VALUES
 ('OWNER','ADMIN_ACCESS'),('OWNER','CUSTOMER_READ'),('OWNER','CUSTOMER_WRITE'),
 ('OWNER','COST_READ'),('OWNER','AUDIT_READ'),
 ('SALES','ADMIN_ACCESS'),('SALES','CUSTOMER_READ'),('SALES','CUSTOMER_WRITE'),
 ('TECHNICIAN','ADMIN_ACCESS'),
 ('INVENTORY','ADMIN_ACCESS'),('INVENTORY','COST_READ'),
 ('CUSTOMER','CUSTOMER_SELF_READ')
) v(RoleCode,PermissionCode)
JOIN dbo.Roles r ON r.RoleCode=v.RoleCode
JOIN dbo.Permissions p ON p.PermissionCode=v.PermissionCode
WHERE NOT EXISTS (SELECT 1 FROM dbo.RolePermissions rp
                  WHERE rp.RoleID=r.RoleID AND rp.PermissionID=p.PermissionID);

INSERT INTO dbo.Categories(CategoryCode,Name) VALUES ('TEST-CATEGORY',N'Phụ kiện');
INSERT INTO dbo.Brands(BrandCode,Name) VALUES ('TEST-BRAND',N'Phổ thông');
INSERT INTO dbo.Units(UnitCode,Name) VALUES ('TEST-UNIT',N'Cái');
INSERT INTO dbo.Products(SKU,Name,ListPrice,CategoryID,BrandID,UnitID,StockMode,IsPublished) VALUES ('TEST-MOUSE','Mouse',90000,1,1,1,'BULK',1);
INSERT INTO dbo.StockLocations(LocationCode,Name,LocationType,IsActive) VALUES ('TEST-SALE',N'Kho bán','SALEABLE',1);
INSERT INTO dbo.Devices(AssetTag,DeviceDescription) VALUES ('TEST-LAPTOP','Laptop');
INSERT INTO dbo.InventoryDocuments(DocumentCode,DocumentType,Status) VALUES ('TEST-PN','RECEIPT','POSTED');
INSERT INTO dbo.BulkStockEntries(InventoryDocID,ProductID,Delta,UnitCost) VALUES (1,1,20,50000);
INSERT INTO dbo.DeviceStockEntries(InventoryDocID,DeviceID,FromLocationID,ToLocationID,UnitCost) VALUES (1,1,NULL,1,5000000);

INSERT INTO dbo.Permissions
(PermissionCode,Name)
SELECT v.Code,v.Name FROM (VALUES
 ('PRODUCT_READ',N'Xem sản phẩm nội bộ'),('PRODUCT_WRITE',N'Thêm, sửa, ngừng kinh doanh sản phẩm'),
 ('CATALOG_WRITE',N'Quản lý danh mục, thương hiệu, đơn vị tính')
) v(Code,Name) WHERE NOT EXISTS (SELECT 1 FROM dbo.Permissions p WHERE p.PermissionCode=v.Code);
INSERT INTO dbo.RolePermissions(RoleID,PermissionID)
SELECT r.RoleID,p.PermissionID FROM (VALUES
 ('OWNER','PRODUCT_READ'),('OWNER','PRODUCT_WRITE'),('OWNER','CATALOG_WRITE'),
 ('SALES','PRODUCT_READ'),('INVENTORY','PRODUCT_READ'),
 ('INVENTORY','PRODUCT_WRITE'),('INVENTORY','CATALOG_WRITE')
) v(RoleCode,PermissionCode)
JOIN dbo.Roles r ON r.RoleCode=v.RoleCode
JOIN dbo.Permissions p ON p.PermissionCode=v.PermissionCode
WHERE NOT EXISTS (SELECT 1 FROM dbo.RolePermissions rp WHERE rp.RoleID=r.RoleID AND rp.PermissionID=p.PermissionID);

INSERT INTO dbo.AttributeDefinitions(AttributeCode,Name,UnitLabel,ValueType)
SELECT v.Code,v.Name,v.UnitLabel,v.ValueType FROM (VALUES
 ('CPU',N'Bộ xử lý',NULL,'TEXT'),('RAM_GB',N'RAM',N'GB','INTEGER'),
 ('SSD_GB',N'SSD',N'GB','INTEGER'),('HDD_GB',N'HDD',N'GB','INTEGER'),
 ('SCREEN_INCH',N'Kích thước màn hình',N'inch','DECIMAL'),
 ('RESOLUTION',N'Độ phân giải',NULL,'TEXT'),('OS',N'Hệ điều hành',NULL,'TEXT')
) v(Code,Name,UnitLabel,ValueType)
WHERE NOT EXISTS(SELECT 1 FROM dbo.AttributeDefinitions a WHERE a.AttributeCode=v.Code);
INSERT INTO dbo.Permissions(PermissionCode,Name)
SELECT v.Code,v.Name FROM (VALUES
 ('DEVICE_READ',N'Xem thiết bị của cửa hàng'),('DEVICE_WRITE',N'Thêm sửa hồ sơ và giá thiết bị'),
 ('DEVICE_IMAGE_WRITE',N'Quản lý ảnh thiết bị'),('DEVICE_CHECK_WRITE',N'Ghi kết quả kiểm tra thiết bị')
) v(Code,Name) WHERE NOT EXISTS(SELECT 1 FROM dbo.Permissions p WHERE p.PermissionCode=v.Code);
INSERT INTO dbo.RolePermissions(RoleID,PermissionID)
SELECT r.RoleID,p.PermissionID FROM (VALUES
 ('OWNER','DEVICE_READ'),('OWNER','DEVICE_WRITE'),('OWNER','DEVICE_IMAGE_WRITE'),('OWNER','DEVICE_CHECK_WRITE'),
 ('INVENTORY','DEVICE_READ'),('INVENTORY','DEVICE_WRITE'),('INVENTORY','DEVICE_IMAGE_WRITE'),('INVENTORY','DEVICE_CHECK_WRITE'),
 ('SALES','DEVICE_READ'),('TECHNICIAN','DEVICE_READ'),('TECHNICIAN','DEVICE_IMAGE_WRITE'),('TECHNICIAN','DEVICE_CHECK_WRITE')
) v(RoleCode,PermissionCode)
JOIN dbo.Roles r ON r.RoleCode=v.RoleCode JOIN dbo.Permissions p ON p.PermissionCode=v.PermissionCode
WHERE NOT EXISTS(SELECT 1 FROM dbo.RolePermissions rp WHERE rp.RoleID=r.RoleID AND rp.PermissionID=p.PermissionID);
