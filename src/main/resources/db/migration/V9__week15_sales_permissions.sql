-- Tuan 15: bổ sung quyền, token tra cứu đơn vãng lai và chỉ mục; không sửa migration cũ.
SET NOCOUNT ON;

INSERT INTO dbo.Permissions(PermissionCode,Name)
SELECT x.Code,x.Name FROM (VALUES
 ('SALE_READ',N'Xem đơn bán hàng'),
 ('SALE_CREATE',N'Lập đơn bán tại quầy'),
 ('SALE_CONFIRM',N'Xác nhận và giữ tồn bán'),
 ('SALE_FULFILL',N'Giao hàng theo sổ kho'),
 ('SALE_PAYMENT',N'Ghi nhận thanh toán'),
 ('SALE_RETURN',N'Nhận trả hàng'),
 ('SALE_REFUND',N'Hoàn tiền bán hàng'),
 ('SALE_SELF_READ',N'Xem đơn bán thuộc tài khoản')
) x(Code,Name) WHERE NOT EXISTS(SELECT 1 FROM dbo.Permissions p WHERE p.PermissionCode=x.Code);

INSERT INTO dbo.RolePermissions(RoleID,PermissionID)
SELECT r.RoleID,p.PermissionID FROM (VALUES
 ('OWNER','SALE_READ'),('OWNER','SALE_CREATE'),('OWNER','SALE_CONFIRM'),('OWNER','SALE_FULFILL'),
 ('OWNER','SALE_PAYMENT'),('OWNER','SALE_RETURN'),('OWNER','SALE_REFUND'),('OWNER','SALE_SELF_READ'),
 ('SALES','SALE_READ'),('SALES','SALE_CREATE'),('SALES','SALE_CONFIRM'),('SALES','SALE_PAYMENT'),
 ('INVENTORY','SALE_READ'),('INVENTORY','SALE_FULFILL'),('INVENTORY','SALE_RETURN'),
 ('CUSTOMER','SALE_SELF_READ')
) x(RoleCode,PermissionCode) JOIN dbo.Roles r ON r.RoleCode=x.RoleCode
JOIN dbo.Permissions p ON p.PermissionCode=x.PermissionCode
WHERE NOT EXISTS(SELECT 1 FROM dbo.RolePermissions rp WHERE rp.RoleID=r.RoleID AND rp.PermissionID=p.PermissionID);

-- Không lưu token truy cập dạng rõ; chỉ SHA-256 của token ngẫu nhiên.
CREATE TABLE dbo.SalesTrackingTokens (
    OrderID bigint NOT NULL PRIMARY KEY,
    TokenHash binary(32) NOT NULL UNIQUE,
    CONSTRAINT FK_W15_SalesTrackingTokens_Order FOREIGN KEY(OrderID) REFERENCES dbo.SalesOrders(OrderID)
);

-- Chống yêu cầu POST lặp khi nhân viên bấm 2 lần / trình duyệt gửi lại.
CREATE TABLE dbo.SalesInventoryRequests (
    RequestKey varchar(100) NOT NULL PRIMARY KEY,
    OrderID bigint NOT NULL REFERENCES dbo.SalesOrders(OrderID),
    SaleLineID bigint NOT NULL REFERENCES dbo.SalesLines(SaleLineID),
    OperationType varchar(24) NOT NULL CHECK(OperationType IN ('SALE_ISSUE','SALE_RETURN')),
    Quantity int NOT NULL CHECK(Quantity>0),
    LocationID bigint NOT NULL REFERENCES dbo.StockLocations(LocationID),
    InventoryDocID bigint NOT NULL UNIQUE REFERENCES dbo.InventoryDocuments(InventoryDocID)
);

CREATE INDEX IX_W15_SalesOrders_CustomerDate ON dbo.SalesOrders(CustomerID,OrderedAt DESC);
CREATE INDEX IX_W15_SalesLines_Order ON dbo.SalesLines(OrderID,[LineNo]);
CREATE INDEX IX_W15_Payments_SalesOrder ON dbo.Payments(OrderID,Status,Direction);
