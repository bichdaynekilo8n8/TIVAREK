-- Chạy read-only trong SSMS sau khi Flyway đã áp dụng V11.
-- Tất cả mốc giờ SQL Server trong hệ thống lưu UTC.
SELECT [Version],[Description],[Success],[InstalledOn]
FROM dbo.flyway_schema_history
WHERE [Version]='11';

SELECT p.PermissionCode,r.RoleCode
FROM dbo.RolePermissions rp JOIN dbo.Permissions p ON p.PermissionID=rp.PermissionID
JOIN dbo.Roles r ON r.RoleID=rp.RoleID
WHERE p.PermissionCode LIKE 'REPORT[_]%'
ORDER BY p.PermissionCode,r.RoleCode;

SELECT COUNT_BIG(*) AS AllOrders,
    SUM(CASE WHEN Status='COMPLETED' THEN 1 ELSE 0 END) AS CurrentCompletedOrders,
    SUM(CASE WHEN CompletedAt IS NOT NULL THEN 1 ELSE 0 END) AS CompletedWithTrackedTimestamp
FROM dbo.SalesOrders;

SELECT Direction,Status,COUNT_BIG(*) AS PaymentCount,SUM(Amount) AS TotalAmount
FROM dbo.Payments GROUP BY Direction,Status ORDER BY Status,Direction;

SELECT Status,COUNT_BIG(*) AS RepairCount FROM dbo.RepairTickets GROUP BY Status;
SELECT Status,COUNT_BIG(*) AS ClaimCount FROM dbo.WarrantyClaims GROUP BY Status;

-- Cảnh báo: số liệu quá khứ không có CompletedAt; đừng dùng OrderedAt để "điền bù".
