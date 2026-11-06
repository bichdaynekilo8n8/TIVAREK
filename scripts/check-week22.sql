-- CHI CHAY TREN SQL SERVER DEV. Script chi doc, khong thay doi du lieu.
SET NOCOUNT ON;

SELECT DB_NAME() AS DatabaseName;

-- 1. Kiem tra migration, rang buoc va chi muc tuan 22.
SELECT installed_rank, version, description, success
FROM dbo.flyway_schema_history
ORDER BY installed_rank DESC;

SELECT name, is_disabled, is_not_trusted
FROM sys.check_constraints
WHERE name LIKE 'CK_W22_%';

SELECT OBJECT_NAME(object_id) AS TableName, name, is_disabled
FROM sys.indexes WHERE name LIKE 'IX_W22_%'
ORDER BY TableName, name;

-- 2. Canh bao so luong ton vat ly BULK am (khong tinh phieu DRAFT).
SELECT e.ProductID, e.LocationID, SUM(CAST(e.Delta AS bigint)) AS Quantity
FROM dbo.BulkStockEntries e
JOIN dbo.InventoryDocuments d ON d.InventoryDocID=e.InventoryDocID AND d.Status='POSTED'
GROUP BY e.ProductID,e.LocationID
HAVING SUM(CAST(e.Delta AS bigint)) < 0;

-- 3. Canh bao so luong ton kha dung BULK am sau khi tru giu hang con han.
WITH Stock AS (
    SELECT e.ProductID,e.LocationID,SUM(CAST(e.Delta AS bigint)) AS Quantity
    FROM dbo.BulkStockEntries e JOIN dbo.InventoryDocuments d
       ON d.InventoryDocID=e.InventoryDocID AND d.Status='POSTED'
    GROUP BY e.ProductID,e.LocationID
), Reservations AS (
    SELECT b.ProductID,r.LocationID,SUM(CAST(r.Quantity AS bigint)) AS Held
    FROM dbo.BulkReservations r JOIN dbo.BulkSaleLines b ON b.SaleLineID=r.SaleLineID
    WHERE r.Status='ACTIVE' AND r.ExpiresAt>SYSUTCDATETIME()
    GROUP BY b.ProductID,r.LocationID
)
SELECT COALESCE(s.ProductID,r.ProductID) AS ProductID,
       COALESCE(s.LocationID,r.LocationID) AS LocationID,
       COALESCE(s.Quantity,0) AS Quantity,COALESCE(r.Held,0) AS Held
FROM Stock s FULL OUTER JOIN Reservations r
    ON r.ProductID=s.ProductID AND r.LocationID=s.LocationID
WHERE COALESCE(s.Quantity,0)-COALESCE(r.Held,0)<0;

-- 4. Canh bao thiet bi duoc giu nhieu lan (PK hien co phai chan truong hop nay).
SELECT DeviceID,COUNT(*) AS ActiveHolds
FROM dbo.ActiveDeviceReservations GROUP BY DeviceID HAVING COUNT(*)>1;

-- 5. Canh bao chung tu giao/nhan ve co khoa lap yeu cau.
SELECT RequestKey,COUNT(*) AS Duplicates
FROM dbo.SalesInventoryRequests GROUP BY RequestKey HAVING COUNT(*)>1;
SELECT IdempotencyKey,COUNT(*) AS Duplicates
FROM dbo.Payments GROUP BY IdempotencyKey HAVING COUNT(*)>1;

-- 6. Canh bao tong hoan tien vuot so tien thu goc.
SELECT p.PaymentID,p.Amount AS Collected,
       SUM(CASE WHEN refund.Status='SUCCEEDED' THEN refund.Amount ELSE 0 END) AS Refunded
FROM dbo.Payments p
JOIN dbo.PaymentRefunds r ON r.OriginalPaymentID=p.PaymentID
JOIN dbo.Payments refund ON refund.PaymentID=r.RefundPaymentID
WHERE p.Direction='IN' AND p.Status='SUCCEEDED'
GROUP BY p.PaymentID,p.Amount
HAVING SUM(CASE WHEN refund.Status='SUCCEEDED' THEN refund.Amount ELSE 0 END)>p.Amount;

-- 7. Canh bao linh kien da xuat qua so luong trong bao gia.
SELECT i.QuoteItemID,i.Quantity AS Approved,
       SUM(CAST(u.Quantity AS bigint)) AS Used
FROM dbo.QuoteItems i JOIN dbo.RepairPartUsages u ON u.QuoteItemID=i.QuoteItemID
GROUP BY i.QuoteItemID,i.Quantity
HAVING SUM(CAST(u.Quantity AS bigint))>i.Quantity;

-- 8. Canh bao yeu cau bao hanh dang xu ly vuot so luong nguon ban/sua.
WITH WarrantyQuantity AS (
    SELECT w.WarrantyID,CAST(COALESCE(b.Quantity,qi.Quantity,1) AS bigint) AS AllowedQuantity
    FROM dbo.Warranties w
    LEFT JOIN dbo.SaleWarranties sw ON sw.WarrantyID=w.WarrantyID
    LEFT JOIN dbo.BulkSaleLines b ON b.SaleLineID=sw.SaleLineID
    LEFT JOIN dbo.RepairWarranties rw ON rw.WarrantyID=w.WarrantyID
    LEFT JOIN dbo.QuoteItems qi ON qi.QuoteItemID=rw.QuoteItemID
)
SELECT w.WarrantyID,w.AllowedQuantity,
       SUM(CAST(c.ClaimQuantity AS bigint)) AS PendingQuantity
FROM WarrantyQuantity w JOIN dbo.WarrantyClaims c ON c.WarrantyID=w.WarrantyID
WHERE c.Status IN ('OPEN','INSPECTING','APPROVED')
GROUP BY w.WarrantyID,w.AllowedQuantity
HAVING SUM(CAST(c.ClaimQuantity AS bigint))>w.AllowedQuantity;
