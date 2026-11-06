-- Chỉ đọc, có thể chạy trong SSMS trên database DEV sau khi áp dụng V10.
SELECT installed_rank,version,description,success FROM dbo.flyway_schema_history ORDER BY installed_rank;

SELECT TOP 50 w.WarrantyCode,w.Status,w.StartAt,w.EndAt,
       sw.SaleLineID,rw.QuoteItemID
FROM dbo.Warranties w
LEFT JOIN dbo.SaleWarranties sw ON sw.WarrantyID=w.WarrantyID
LEFT JOIN dbo.RepairWarranties rw ON rw.WarrantyID=w.WarrantyID
ORDER BY w.WarrantyID DESC;

-- Kết quả mong đợi: không có dòng nào (một phiếu không được có hai nguồn).
SELECT w.WarrantyID FROM dbo.Warranties w
LEFT JOIN dbo.SaleWarranties sw ON sw.WarrantyID=w.WarrantyID
LEFT JOIN dbo.RepairWarranties rw ON rw.WarrantyID=w.WarrantyID
WHERE (CASE WHEN sw.WarrantyID IS NULL THEN 0 ELSE 1 END +
       CASE WHEN rw.WarrantyID IS NULL THEN 0 ELSE 1 END)<>1;

SELECT TOP 50 c.ClaimCode,w.WarrantyCode,c.Status,c.ClaimQuantity,c.ClaimedAt
FROM dbo.WarrantyClaims c JOIN dbo.Warranties w ON w.WarrantyID=c.WarrantyID
ORDER BY c.ClaimID DESC;

SELECT TOP 50 l.SaleLineID,l.WarrantyDaysSnapshot,o.OrderCode,o.Status
FROM dbo.SalesLines l JOIN dbo.SalesOrders o ON o.OrderID=l.OrderID
ORDER BY l.SaleLineID DESC;
