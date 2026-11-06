-- Tuần 9: thêm dữ liệu phân quyền; không thay V1/V1_1/V2, không tạo lại bảng.
SET NOCOUNT ON;
INSERT dbo.Roles(RoleCode,Name)
SELECT v.Code,v.Name FROM (VALUES
 ('OWNER',N'Chủ cửa hàng'),('SALES',N'Bán hàng'),('TECHNICIAN',N'Kỹ thuật'),
 ('INVENTORY',N'Quản lý kho'),('CUSTOMER',N'Khách hàng')
) v(Code,Name) WHERE NOT EXISTS (SELECT 1 FROM dbo.Roles r WHERE r.RoleCode=v.Code);

INSERT dbo.Permissions(PermissionCode,Name)
SELECT v.Code,v.Name FROM (VALUES
 ('ADMIN_ACCESS',N'Vào trang quản trị'),('CUSTOMER_READ',N'Xem danh sách khách hàng'),
 ('CUSTOMER_WRITE',N'Tạo và cập nhật khách hàng'),('CUSTOMER_SELF_READ',N'Xem hồ sơ cá nhân'),
 ('COST_READ',N'Xem giá nhập nội bộ'),('AUDIT_READ',N'Xem nhật ký thao tác')
) v(Code,Name) WHERE NOT EXISTS (SELECT 1 FROM dbo.Permissions p WHERE p.PermissionCode=v.Code);

INSERT dbo.RolePermissions(RoleID,PermissionID)
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
