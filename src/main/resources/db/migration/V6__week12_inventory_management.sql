-- TUAN 12 - PHAN QUYEN KHO
SET NOCOUNT ON;

INSERT INTO dbo.Permissions
(PermissionCode, Name)
SELECT v.Code, v.Name
FROM (VALUES
          ('STOCK_READ', N'Xem tồn kho'),
          ('STOCK_RECEIVE', N'Nhập kho'),
          ('STOCK_ISSUE', N'Xuất kho'),
          ('STOCK_TRANSFER', N'Chuyển kho'),
          ('STOCK_ADJUST', N'Điều chỉnh tồn kho')
     ) v(Code, Name)
WHERE NOT EXISTS (
    SELECT 1
    FROM dbo.Permissions p
    WHERE p.PermissionCode = v.Code
);

INSERT INTO dbo.RolePermissions
(RoleID, PermissionID)
SELECT
    r.RoleID,
    p.PermissionID
FROM (VALUES
          ('OWNER', 'STOCK_READ'),
          ('OWNER', 'STOCK_RECEIVE'),
          ('OWNER', 'STOCK_ISSUE'),
          ('OWNER', 'STOCK_TRANSFER'),
          ('OWNER', 'STOCK_ADJUST'),

          ('INVENTORY', 'STOCK_READ'),
          ('INVENTORY', 'STOCK_RECEIVE'),
          ('INVENTORY', 'STOCK_ISSUE'),
          ('INVENTORY', 'STOCK_TRANSFER'),
          ('INVENTORY', 'STOCK_ADJUST'),

          ('SALES', 'STOCK_READ'),
          ('TECHNICIAN', 'STOCK_READ')
     ) v(RoleCode, PermissionCode)

         JOIN dbo.Roles r
              ON r.RoleCode = v.RoleCode

         JOIN dbo.Permissions p
              ON p.PermissionCode = v.PermissionCode

WHERE NOT EXISTS (
    SELECT 1
    FROM dbo.RolePermissions rp
    WHERE rp.RoleID = r.RoleID
      AND rp.PermissionID = p.PermissionID
);
-- Ho tro tinh ton theo san pham va vi tri
CREATE INDEX IX_W12_BulkStock_ProductLocation
    ON dbo.BulkStockEntries
        (
         ProductID,
         LocationID,
         InventoryDocID
            )
    INCLUDE (Delta);

-- Ho tro tra cuu bien dong cua tung thiet bi
CREATE INDEX IX_W12_DeviceStock_DeviceDocument
    ON dbo.DeviceStockEntries
        (
         DeviceID,
         InventoryDocID
            )
    INCLUDE (
    FromLocationID,
    ToLocationID
);
