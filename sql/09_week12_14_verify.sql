-- Chạy trên SQL Server DEV bằng SSMS. CHỈ ĐỌC; không sửa dữ liệu.
SELECT installed_rank,version,description,success FROM dbo.flyway_schema_history ORDER BY installed_rank;

-- Chứng từ kho: chỉ POSTED mới được tính vào tồn.
SELECT d.InventoryDocID,d.DocumentCode,d.DocumentType,d.Status,d.OccurredAt,
       COUNT(DISTINCT b.BulkEntryID) AS BulkLines,COUNT(DISTINCT s.DeviceEntryID) AS DeviceLines
FROM dbo.InventoryDocuments d
LEFT JOIN dbo.BulkStockEntries b ON b.InventoryDocID=d.InventoryDocID
LEFT JOIN dbo.DeviceStockEntries s ON s.InventoryDocID=d.InventoryDocID
GROUP BY d.InventoryDocID,d.DocumentCode,d.DocumentType,d.Status,d.OccurredAt
ORDER BY d.InventoryDocID DESC;

-- Tồn BULK và số lượng đang được giữ theo vị trí.
SELECT p.SKU,l.LocationCode,
       COALESCE(SUM(CASE WHEN d.Status='POSTED' THEN e.Delta ELSE 0 END),0) AS PhysicalQty
FROM dbo.BulkStockEntries e JOIN dbo.InventoryDocuments d ON d.InventoryDocID=e.InventoryDocID
JOIN dbo.Products p ON p.ProductID=e.ProductID JOIN dbo.StockLocations l ON l.LocationID=e.LocationID
GROUP BY p.SKU,l.LocationCode ORDER BY p.SKU,l.LocationCode;

-- Mọi phiếu sửa nhận máy phải liên kết với một máy của khách.
SELECT t.RepairTicketID,t.TicketCode,t.Status,c.FullName,d.AssetTag,d.OwnerCustomerID,t.CustomerID
FROM dbo.RepairTickets t JOIN dbo.Customers c ON c.CustomerID=t.CustomerID
JOIN dbo.Devices d ON d.DeviceID=t.DeviceID ORDER BY t.RepairTicketID DESC;

-- Theo dõi công việc, báo giá, linh kiện sửa.
SELECT q.QuoteID,t.TicketCode,q.QuoteNo,q.Status,q.RespondedAt,q.ApprovalEvidence
FROM dbo.RepairQuotes q JOIN dbo.RepairTickets t ON t.RepairTicketID=q.RepairTicketID ORDER BY q.QuoteID DESC;

SELECT u.PartUsageID,t.TicketCode,u.Quantity,u.UsedAt,b.BulkEntryID,s.DeviceEntryID
FROM dbo.RepairPartUsages u JOIN dbo.QuoteItems qi ON qi.QuoteItemID=u.QuoteItemID
JOIN dbo.RepairQuotes q ON q.QuoteID=qi.QuoteID JOIN dbo.RepairTickets t ON t.RepairTicketID=q.RepairTicketID
LEFT JOIN dbo.RepairBulkIssues b ON b.PartUsageID=u.PartUsageID
LEFT JOIN dbo.RepairDeviceIssues s ON s.PartUsageID=u.PartUsageID ORDER BY u.PartUsageID DESC;

-- Tài khoản đăng ký thật liên kết đúng CUSTOMER; chỉ dùng để xác thực, không hiện mật khẩu.
SELECT u.UserID,u.LoginName,u.IsActive,r.RoleCode,c.CustomerCode,c.FullName
FROM dbo.Users u JOIN dbo.UserRoles ur ON ur.UserID=u.UserID
JOIN dbo.Roles r ON r.RoleID=ur.RoleID
LEFT JOIN dbo.Customers c ON c.UserID=u.UserID
ORDER BY u.UserID DESC;
