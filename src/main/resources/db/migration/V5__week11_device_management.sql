-- Tuần 11: mở rộng tại chỗ, giữ nguyên migration V1 đến V4_1.
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
GO
ALTER TABLE dbo.Devices ADD FaultDescription nvarchar(2000) NULL,
    CreatedAt datetime2(3) NOT NULL CONSTRAINT DF_Devices_CreatedAt DEFAULT SYSUTCDATETIME(),
    UpdatedAt datetime2(3) NOT NULL CONSTRAINT DF_Devices_UpdatedAt DEFAULT SYSUTCDATETIME();
ALTER TABLE dbo.Devices ADD AssetTagKey AS UPPER(LTRIM(RTRIM(AssetTag))) PERSISTED;
CREATE UNIQUE INDEX UX_Devices_AssetTagKey ON dbo.Devices(AssetTagKey);
ALTER TABLE dbo.Devices ADD CONSTRAINT CK_Devices_AssetTagNotBlank CHECK (LEN(LTRIM(RTRIM(AssetTag)))>0);
CREATE INDEX IX_Devices_StoreSearch ON dbo.Devices(OwnerCustomerID,ProductID,ConditionGrade,DeviceID);
CREATE INDEX IX_DeviceChecks_Latest ON dbo.DeviceChecks(DeviceID,CheckedAt DESC,CheckID DESC);
CREATE TABLE dbo.DeviceAttributeValues (
    DeviceID bigint NOT NULL,
    AttributeID bigint NOT NULL,
    ValueText nvarchar(250) NOT NULL,
    CONSTRAINT PK_DeviceAttributeValues PRIMARY KEY(DeviceID,AttributeID),
    CONSTRAINT FK_DeviceAttributes_Device FOREIGN KEY(DeviceID) REFERENCES dbo.Devices(DeviceID),
    CONSTRAINT FK_DeviceAttributes_Definition FOREIGN KEY(AttributeID) REFERENCES dbo.AttributeDefinitions(AttributeID),
    CONSTRAINT CK_DeviceAttributes_NotBlank CHECK(LEN(LTRIM(RTRIM(ValueText)))>0)
);
GO
-- Cùng định nghĩa thông số có thể được dùng cho mẫu và cho từng máy; giá trị lưu riêng.
INSERT dbo.AttributeDefinitions(AttributeCode,Name,UnitLabel,ValueType)
SELECT v.Code,v.Name,v.UnitLabel,v.ValueType FROM (VALUES
 ('CPU',N'Bộ xử lý',NULL,'TEXT'),('RAM_GB',N'RAM',N'GB','INTEGER'),
 ('SSD_GB',N'SSD',N'GB','INTEGER'),('HDD_GB',N'HDD',N'GB','INTEGER'),
 ('SCREEN_INCH',N'Kích thước màn hình',N'inch','DECIMAL'),
 ('RESOLUTION',N'Độ phân giải',NULL,'TEXT'),('OS',N'Hệ điều hành',NULL,'TEXT')
) v(Code,Name,UnitLabel,ValueType)
WHERE NOT EXISTS(SELECT 1 FROM dbo.AttributeDefinitions a WHERE a.AttributeCode=v.Code);
INSERT dbo.Permissions(PermissionCode,Name)
SELECT v.Code,v.Name FROM (VALUES
 ('DEVICE_READ',N'Xem thiết bị của cửa hàng'),('DEVICE_WRITE',N'Thêm sửa hồ sơ và giá thiết bị'),
 ('DEVICE_IMAGE_WRITE',N'Quản lý ảnh thiết bị'),('DEVICE_CHECK_WRITE',N'Ghi kết quả kiểm tra thiết bị')
) v(Code,Name) WHERE NOT EXISTS(SELECT 1 FROM dbo.Permissions p WHERE p.PermissionCode=v.Code);
INSERT dbo.RolePermissions(RoleID,PermissionID)
SELECT r.RoleID,p.PermissionID FROM (VALUES
 ('OWNER','DEVICE_READ'),('OWNER','DEVICE_WRITE'),('OWNER','DEVICE_IMAGE_WRITE'),('OWNER','DEVICE_CHECK_WRITE'),
 ('INVENTORY','DEVICE_READ'),('INVENTORY','DEVICE_WRITE'),('INVENTORY','DEVICE_IMAGE_WRITE'),('INVENTORY','DEVICE_CHECK_WRITE'),
 ('SALES','DEVICE_READ'),('TECHNICIAN','DEVICE_READ'),('TECHNICIAN','DEVICE_IMAGE_WRITE'),('TECHNICIAN','DEVICE_CHECK_WRITE')
) v(RoleCode,PermissionCode)
JOIN dbo.Roles r ON r.RoleCode=v.RoleCode JOIN dbo.Permissions p ON p.PermissionCode=v.PermissionCode
WHERE NOT EXISTS(SELECT 1 FROM dbo.RolePermissions rp WHERE rp.RoleID=r.RoleID AND rp.PermissionID=p.PermissionID);
