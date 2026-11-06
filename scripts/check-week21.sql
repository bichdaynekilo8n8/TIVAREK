/* TIVAREK W21 - SQL SERVER READ-ONLY CHECKS (run on DEV or restored COPY).
   DO NOT execute edits or run this against production without authorization. */

SELECT DB_NAME() AS DatabaseName, SYSUTCDATETIME() AS CheckedAtUtc;

/* Actual permissions in the current database, not assumed from role labels. */
SELECT r.RoleCode, p.PermissionCode
FROM dbo.Roles AS r
JOIN dbo.RolePermissions AS rp ON rp.RoleID=r.RoleID
JOIN dbo.Permissions AS p ON p.PermissionID=rp.PermissionID
ORDER BY r.RoleCode, p.PermissionCode;

/* Count authentication and authorization security events by action. */
SELECT TOP (20) a.Action, COUNT_BIG(*) AS EventCount
FROM dbo.AuditLogs AS a
WHERE a.Action IN ('LOGIN_FAILURE','LOGIN_SUCCESS','ACCESS_DENIED','PASSWORD_CHANGE','LOGOUT')
GROUP BY a.Action ORDER BY EventCount DESC, a.Action;

/* Ensure no account was inadvertently registered without an assigned role. */
SELECT TOP (30) u.UserID, u.LoginName
FROM dbo.Users AS u
WHERE NOT EXISTS (SELECT 1 FROM dbo.UserRoles AS ur WHERE ur.UserID=u.UserID)
ORDER BY u.UserID DESC;

/* No password hashes are selected or exported by this diagnostic. */
