-- WEEK 19: READ-ONLY SQL Server acceptance checks (run in SSMS on a COPY / TEST database).
-- IMPORTANT: All timestamps in TIVAREK are UTC. No writes, repairs or migrations here.
SET NOCOUNT ON;

SELECT [version], [description], [success], [installed_on]
FROM dbo.flyway_schema_history
ORDER BY [installed_rank] DESC;

-- Expected: zero findings in healthy operations. Investigate, don't auto-delete.
SELECT N'Negative available bulk stock' AS [CheckName], COUNT_BIG(*) AS [Findings]
FROM dbo.vBulkAvailable WHERE Available < 0
UNION ALL
SELECT N'Negative on-hand bulk stock', COUNT_BIG(*)
FROM dbo.vBulkOnHand WHERE OnHand < 0
UNION ALL
SELECT N'Sales lines without exactly one subtype', COUNT_BIG(*)
FROM dbo.SalesLines s
LEFT JOIN dbo.BulkSaleLines b ON b.SaleLineID = s.SaleLineID
LEFT JOIN dbo.DeviceSaleLines d ON d.SaleLineID = s.SaleLineID
WHERE (CASE WHEN b.SaleLineID IS NULL THEN 0 ELSE 1 END
      + CASE WHEN d.SaleLineID IS NULL THEN 0 ELSE 1 END) <> 1
UNION ALL
SELECT N'Active device reservation linked to non-active or expired record', COUNT_BIG(*)
FROM dbo.ActiveDeviceReservations a
LEFT JOIN dbo.DeviceReservations d ON d.ReservationID = a.ReservationID
WHERE d.ReservationID IS NULL OR d.Status <> 'ACTIVE' OR d.ExpiresAt <= SYSUTCDATETIME()
UNION ALL
SELECT N'Bulk reservations with invalid quantities', COUNT_BIG(*)
FROM dbo.BulkReservations WHERE Quantity <= 0
UNION ALL
SELECT N'Orders with duplicate line numbers', COUNT_BIG(*)
FROM (SELECT s.OrderID, s.LineNo FROM dbo.SalesLines s
      GROUP BY s.OrderID, s.LineNo HAVING COUNT_BIG(*) > 1) x
UNION ALL
SELECT N'Warranty claims with missing warranties', COUNT_BIG(*)
FROM dbo.WarrantyClaims c LEFT JOIN dbo.Warranties w ON w.WarrantyID = c.WarrantyID
WHERE w.WarrantyID IS NULL
UNION ALL
SELECT N'Succeeded payments with non-positive amounts', COUNT_BIG(*)
FROM dbo.Payments WHERE Status = 'SUCCEEDED' AND Amount <= 0;

-- Summary only: never print customer names, phone numbers, tokens or sensitive details.
SELECT [Status], COUNT_BIG(*) AS [OrderCount]
FROM dbo.SalesOrders GROUP BY [Status] ORDER BY [Status];

SELECT [Status], COUNT_BIG(*) AS [RepairCount]
FROM dbo.RepairTickets GROUP BY [Status] ORDER BY [Status];

SELECT [Status], COUNT_BIG(*) AS [WarrantyClaimCount]
FROM dbo.WarrantyClaims GROUP BY [Status] ORDER BY [Status];

SELECT [Direction], [Status], COUNT_BIG(*) AS [Payments], SUM(Amount) AS [Total]
FROM dbo.Payments GROUP BY [Direction], [Status] ORDER BY [Direction], [Status];
