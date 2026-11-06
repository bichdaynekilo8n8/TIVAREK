-- Mở SSMS trên database thực hành của ứng dụng.
USE TIVAREK_WEEK8_DEV;
GO
SELECT installed_rank,version,description,success FROM dbo.flyway_schema_history ORDER BY installed_rank;
SELECT d.DeviceID,d.AssetTag,p.SKU,p.Name,d.ManufacturerSerial,d.ConditionGrade,d.AskingPrice,
       d.OwnerCustomerID,d.DeviceDescription,d.FaultDescription,CONVERT(varchar(18),d.Version,1) AS VersionHex,
       d.CreatedAt,d.UpdatedAt
FROM dbo.Devices d LEFT JOIN dbo.Products p ON p.ProductID=d.ProductID
ORDER BY d.DeviceID DESC;
SELECT d.AssetTag,a.AttributeCode,a.Name,v.ValueText,a.UnitLabel
FROM dbo.DeviceAttributeValues v JOIN dbo.Devices d ON d.DeviceID=v.DeviceID
JOIN dbo.AttributeDefinitions a ON a.AttributeID=v.AttributeID ORDER BY d.DeviceID,a.AttributeID;
SELECT d.AssetTag,i.ImageID,i.ImageURL,i.SortOrder FROM dbo.DeviceImages i JOIN dbo.Devices d ON d.DeviceID=i.DeviceID ORDER BY d.DeviceID,i.SortOrder;
SELECT d.AssetTag,c.CheckedAt,u.LoginName,c.Result,c.Findings FROM dbo.DeviceChecks c
JOIN dbo.Devices d ON d.DeviceID=c.DeviceID JOIN dbo.Users u ON u.UserID=c.CheckedBy ORDER BY c.CheckedAt DESC,c.CheckID DESC;
SELECT TOP(100) a.OccurredAt,u.LoginName,a.Action,a.EntityKey,a.Summary FROM dbo.AuditLogs a
LEFT JOIN dbo.Users u ON u.UserID=a.ActorID WHERE a.EntityType='DEVICE' ORDER BY a.AuditID DESC;
-- Phải rỗng: mã thiết bị chuẩn hóa trùng.
SELECT UPPER(LTRIM(RTRIM(AssetTag))) AS AssetTagKey,COUNT(*) AS Total FROM dbo.Devices
GROUP BY UPPER(LTRIM(RTRIM(AssetTag))) HAVING COUNT(*)>1;
-- Serial có thể thiếu hoặc trùng; đây là danh sách để người quản lý kiểm tra.
SELECT UPPER(LTRIM(RTRIM(ManufacturerSerial))) AS SerialKey,COUNT(*) AS Total FROM dbo.Devices
WHERE OwnerCustomerID IS NULL AND NULLIF(LTRIM(RTRIM(ManufacturerSerial)),'') IS NOT NULL
GROUP BY UPPER(LTRIM(RTRIM(ManufacturerSerial))) HAVING COUNT(*)>1;
SELECT r.RoleCode,p.PermissionCode FROM dbo.RolePermissions rp JOIN dbo.Roles r ON r.RoleID=rp.RoleID
JOIN dbo.Permissions p ON p.PermissionID=rp.PermissionID WHERE p.PermissionCode LIKE 'DEVICE_%'
ORDER BY r.RoleCode,p.PermissionCode;
