-- Repair an existing development database where the CUSTOMER role was removed
-- after the original Week 9 seed ran. Never modify already applied migrations.
-- This is idempotent and grants the CUSTOMER role only self-service access.
SET NOCOUNT ON;

INSERT INTO dbo.Roles (RoleCode, Name)
SELECT 'CUSTOMER', N'Khách hàng'
WHERE NOT EXISTS (SELECT 1 FROM dbo.Roles WHERE RoleCode='CUSTOMER');

INSERT INTO dbo.Permissions (PermissionCode, Name)
SELECT 'CUSTOMER_SELF_READ', N'Xem hồ sơ cá nhân'
WHERE NOT EXISTS (SELECT 1 FROM dbo.Permissions WHERE PermissionCode='CUSTOMER_SELF_READ');

INSERT INTO dbo.RolePermissions (RoleID, PermissionID)
SELECT r.RoleID, p.PermissionID
FROM dbo.Roles r
JOIN dbo.Permissions p ON p.PermissionCode='CUSTOMER_SELF_READ'
WHERE r.RoleCode='CUSTOMER'
  AND NOT EXISTS (
      SELECT 1 FROM dbo.RolePermissions rp
      WHERE rp.RoleID=r.RoleID AND rp.PermissionID=p.PermissionID
  );
