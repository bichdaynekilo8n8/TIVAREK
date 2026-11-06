-- TUAN 22: chi muc de giam quet va mo rong tranh chap khoa trong cac giao dich quan trong.
-- Flyway chay migration dung mot lan; khong thay doi cac migration V1-V11.

-- Bao ve tinh duy nhat bang ranh buoc CSDL da co: UQ_Payments_2,
-- PK_SalesInventoryRequests va PK_ActiveDeviceReservations.
ALTER TABLE dbo.Payments
    ADD CONSTRAINT CK_W22_Payments_IdempotencyKey_NotBlank
        CHECK (LEN(LTRIM(RTRIM(IdempotencyKey))) > 0);

ALTER TABLE dbo.SalesInventoryRequests
    ADD CONSTRAINT CK_W22_SalesInventoryRequests_RequestKey_NotBlank
        CHECK (LEN(LTRIM(RTRIM(RequestKey))) > 0);

-- Ho tro truy van reserve theo kho/dong ban khi giu hang dang hoat dong.
CREATE INDEX IX_W22_BulkReservations_Active
    ON dbo.BulkReservations(LocationID, SaleLineID, ExpiresAt)
    INCLUDE (Quantity)
    WHERE Status = 'ACTIVE';

-- Khoang khoa theo so dien thoai khi chong gui yeu cau sua qua nhanh.
CREATE INDEX IX_W22_RepairRequests_PhoneTime
    ON dbo.RepairRequests(ContactPhone, CreatedAt DESC);

-- Tra cuu bao gia cua mot phieu sua va cac yeu cau bao hanh dang xu ly.
CREATE INDEX IX_W22_RepairQuotes_TicketStatus
    ON dbo.RepairQuotes(RepairTicketID, Status)
    INCLUDE (ValidUntil);

CREATE INDEX IX_W22_WarrantyClaims_Pending
    ON dbo.WarrantyClaims(WarrantyID, Status)
    INCLUDE (ClaimQuantity);
