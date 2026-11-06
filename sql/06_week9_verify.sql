-- Chỉ chạy các SELECT kiểm tra. Không tạo/xóa DB, không đặt mật khẩu bằng SQL.
USE TIVAREK_WEEK8_DEV;
GO
SELECT DB_NAME() AS DatabaseName;
SELECT installed_rank,version,description,success FROM dbo.flyway_schema_history ORDER BY installed_rank;
SELECT u.UserID,u.LoginName,u.IsActive,LEFT(u.PasswordHash,7) AS HashPrefix,
       LEN(u.PasswordHash) AS HashLength,r.RoleCode
FROM dbo.Users u LEFT JOIN dbo.UserRoles ur ON ur.UserID=u.UserID
LEFT JOIN dbo.Roles r ON r.RoleID=ur.RoleID WHERE u.LoginName LIKE 'w9.%';
SELECT r.RoleCode,p.PermissionCode FROM dbo.Roles r
JOIN dbo.RolePermissions rp ON rp.RoleID=r.RoleID
JOIN dbo.Permissions p ON p.PermissionID=rp.PermissionID ORDER BY r.RoleCode,p.PermissionCode;
SELECT c.CustomerID,c.CustomerCode,c.FullName,u.LoginName FROM dbo.Customers c
JOIN dbo.Users u ON u.UserID=c.UserID WHERE u.LoginName LIKE 'w9.%';
SELECT TOP (30) a.AuditID,u.LoginName,a.Action,a.EntityType,a.EntityKey,a.Summary,a.OccurredAt
FROM dbo.AuditLogs a LEFT JOIN dbo.Users u ON u.UserID=a.ActorID ORDER BY a.AuditID DESC;
