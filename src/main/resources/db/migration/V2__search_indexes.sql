-- Thay đổi mới bằng migration mới; không sửa V1 sau khi đã áp dụng.
CREATE INDEX IX_Customers_Phone ON dbo.Customers(Phone) INCLUDE (FullName);
GO
CREATE INDEX IX_SalesOrders_OrderedAt_Status
ON dbo.SalesOrders(OrderedAt, Status) INCLUDE (CustomerID);
GO
CREATE INDEX IX_RepairTickets_Status_ReceivedAt
ON dbo.RepairTickets(Status, ReceivedAt) INCLUDE (DeviceID, CustomerID);
GO
