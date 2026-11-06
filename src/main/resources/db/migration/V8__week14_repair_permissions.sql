-- Tuần 14: chỉ thêm quyền và danh mục kho mặc định; giữ nguyên toàn bộ lịch sử V1-V7.
SET NOCOUNT ON;
INSERT INTO dbo.Permissions(PermissionCode,Name)
SELECT x.Code,x.Name FROM (VALUES
 ('REPAIR_READ',N'Xem danh sách phiếu sửa'),
 ('REPAIR_INTAKE',N'Tiếp nhận máy sửa'),
 ('REPAIR_ASSIGN',N'Phân công kỹ thuật viên'),
 ('REPAIR_WORK',N'Chẩn đoán và ghi nhật ký sửa chữa'),
 ('REPAIR_QUOTE',N'Lập và gửi báo giá'),
 ('REPAIR_HANDOVER',N'Bàn giao máy sửa')
) x(Code,Name) WHERE NOT EXISTS(SELECT 1 FROM dbo.Permissions p WHERE p.PermissionCode=x.Code);

INSERT INTO dbo.RolePermissions(RoleID,PermissionID)
SELECT r.RoleID,p.PermissionID FROM (VALUES
 ('OWNER','REPAIR_READ'),('OWNER','REPAIR_INTAKE'),('OWNER','REPAIR_ASSIGN'),
 ('OWNER','REPAIR_WORK'),('OWNER','REPAIR_QUOTE'),('OWNER','REPAIR_HANDOVER'),
 ('SALES','REPAIR_READ'),('SALES','REPAIR_INTAKE'),('SALES','REPAIR_QUOTE'),('SALES','REPAIR_HANDOVER'),
 ('TECHNICIAN','REPAIR_READ'),('TECHNICIAN','REPAIR_WORK'),('TECHNICIAN','REPAIR_QUOTE')
) x(RoleCode,PermissionCode) JOIN dbo.Roles r ON r.RoleCode=x.RoleCode
JOIN dbo.Permissions p ON p.PermissionCode=x.PermissionCode
WHERE NOT EXISTS(SELECT 1 FROM dbo.RolePermissions rp WHERE rp.RoleID=r.RoleID AND rp.PermissionID=p.PermissionID);

INSERT INTO dbo.StockLocations(LocationCode,Name,LocationType)
SELECT x.Code,x.Name,x.Type FROM (VALUES
 ('BAN_HANG',N'Kho bán hàng','SALEABLE'),
 ('CACH_LY',N'Khu kiểm tra và cách ly','QUARANTINE'),
 ('HANG_LOI',N'Hàng lỗi','DAMAGED')
) x(Code,Name,Type) WHERE NOT EXISTS(SELECT 1 FROM dbo.StockLocations l WHERE l.LocationCode=x.Code);
