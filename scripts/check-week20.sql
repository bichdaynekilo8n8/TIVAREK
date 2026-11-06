-- WEEK 20: READ-ONLY FINANCIAL/STOCK CONSISTENCY REPORT.
-- Run in SSMS against TIVAREK_WEEK8_DEV or a verified restored COPY.
-- No UPDATE/DELETE/INSERT, no customer PII or tokens are selected.
SET NOCOUNT ON;

SELECT [version],[description],[success],[installed_on]
FROM dbo.flyway_schema_history
ORDER BY [installed_rank] DESC;

-- Review every nonzero finding. Some warnings may be historical data; never fix by
-- manually editing ledger rows without an approved reconciliation procedure.
SELECT N'Negative available stock' AS CheckName,COUNT_BIG(*) AS Findings
FROM dbo.vBulkAvailable WHERE Available < 0
UNION ALL
SELECT N'Negative posted stock',COUNT_BIG(*)
FROM dbo.vBulkOnHand WHERE OnHand < 0
UNION ALL
SELECT N'Invalid line discount',COUNT_BIG(*)
FROM dbo.SalesLines l LEFT JOIN dbo.BulkSaleLines b ON b.SaleLineID=l.SaleLineID
WHERE l.DiscountAmount > l.UnitPrice * COALESCE(b.Quantity,1)
UNION ALL
SELECT N'Orders without lines after confirmation',COUNT_BIG(*)
FROM dbo.SalesOrders o
WHERE o.Status IN ('CONFIRMED','FULFILLING','COMPLETED')
AND NOT EXISTS(SELECT 1 FROM dbo.SalesLines l WHERE l.OrderID=o.OrderID)
UNION ALL
SELECT N'Over-collected orders against DB invoice view',COUNT_BIG(*)
FROM (
    SELECT o.OrderID, COALESCE(sh.ShippingFee,0)+COALESCE(la.TotalDue,0) AS AmountDue,
           COALESCE(pb.NetCollected,0) AS NetCollected
    FROM dbo.SalesOrders o
    LEFT JOIN dbo.OrderShipping sh ON sh.OrderID=o.OrderID
    LEFT JOIN (
        SELECT OrderID,SUM(NetAmount+TaxAmount) AS TotalDue
        FROM dbo.vSaleLineAmounts GROUP BY OrderID
    ) la ON la.OrderID=o.OrderID
    LEFT JOIN dbo.vPaymentBalances pb ON pb.OrderID=o.OrderID AND pb.RepairTicketID IS NULL
) balances WHERE balances.NetCollected > balances.AmountDue
UNION ALL
SELECT N'Over-refunded payment sources',COUNT_BIG(*)
FROM (
    SELECT r.OriginalPaymentID
    FROM dbo.PaymentRefunds r
    JOIN dbo.Payments refund ON refund.PaymentID=r.RefundPaymentID AND refund.Status='SUCCEEDED'
    JOIN dbo.Payments original ON original.PaymentID=r.OriginalPaymentID
    GROUP BY r.OriginalPaymentID,original.Amount
    HAVING SUM(refund.Amount)>original.Amount
) overrefund
UNION ALL
SELECT N'Reused bank transfer references (manual review)',COUNT_BIG(*)
FROM (
    SELECT ProviderReference FROM dbo.Payments
    WHERE Method='BANK_TRANSFER' AND Direction='IN' AND Status='SUCCEEDED'
      AND ProviderReference IS NOT NULL
    GROUP BY ProviderReference HAVING COUNT_BIG(*)>1
) reused;

-- Compare finance totals after recorded return/refund in a test order.
SELECT [Status],COUNT_BIG(*) AS OrderCount
FROM dbo.SalesOrders GROUP BY [Status] ORDER BY [Status];

SELECT [Direction],[Status],COUNT_BIG(*) AS Payments,SUM(Amount) AS TotalAmount
FROM dbo.Payments GROUP BY [Direction],[Status] ORDER BY [Direction],[Status];
