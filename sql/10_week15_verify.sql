-- TIVAREK W15: chỉ đọc / kiểm tra dữ liệu. Chọn database TIVAREK trước khi chạy.
SELECT TOP 30 o.OrderID,o.OrderCode,o.Channel,o.Status,o.OrderedAt,
       s.RecipientName,s.FulfillmentMethod
FROM dbo.SalesOrders o JOIN dbo.OrderShipping s ON s.OrderID=o.OrderID
ORDER BY o.OrderID DESC;

SELECT TOP 100 l.SaleLineID,o.OrderCode,l.DescriptionSnapshot,
       COALESCE(b.Quantity,1) AS OrderedQty,
       COALESCE((SELECT SUM(-e.Delta) FROM dbo.SaleBulkIssues sb
                 JOIN dbo.BulkStockEntries e ON e.BulkEntryID=sb.BulkEntryID
                 JOIN dbo.InventoryDocuments d ON d.InventoryDocID=e.InventoryDocID AND d.Status='POSTED'
                 WHERE sb.SaleLineID=l.SaleLineID),0) AS BulkShipped
FROM dbo.SalesLines l JOIN dbo.SalesOrders o ON o.OrderID=l.OrderID
LEFT JOIN dbo.BulkSaleLines b ON b.SaleLineID=l.SaleLineID
ORDER BY l.SaleLineID DESC;

SELECT TOP 100 p.PaymentID,o.OrderCode,p.Direction,p.Amount,p.Method,p.Status,p.IdempotencyKey
FROM dbo.Payments p JOIN dbo.SalesOrders o ON o.OrderID=p.OrderID ORDER BY p.PaymentID DESC;

SELECT TOP 50 d.InventoryDocID,o.OrderCode,i.DocumentCode,i.Status,i.OccurredAt
FROM dbo.SalesDeliveries d JOIN dbo.InventoryDocuments i ON i.InventoryDocID=d.InventoryDocID
JOIN dbo.SalesOrders o ON o.OrderID=d.OrderID ORDER BY d.InventoryDocID DESC;

SELECT TOP 50 r.InventoryDocID,o.OrderCode,i.DocumentCode,r.ReturnReason,i.OccurredAt
FROM dbo.SalesReturns r JOIN dbo.InventoryDocuments i ON i.InventoryDocID=r.InventoryDocID
JOIN dbo.SalesOrders o ON o.OrderID=r.OrderID ORDER BY r.InventoryDocID DESC;

SELECT p.PermissionCode,r.RoleCode FROM dbo.RolePermissions rp
JOIN dbo.Roles r ON r.RoleID=rp.RoleID JOIN dbo.Permissions p ON p.PermissionID=rp.PermissionID
WHERE p.PermissionCode LIKE 'SALE[_]%' ORDER BY r.RoleCode,p.PermissionCode;
