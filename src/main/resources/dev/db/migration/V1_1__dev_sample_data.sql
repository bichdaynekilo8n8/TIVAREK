-- CHỈ DEVELOPMENT. Các tên/mã DEMO và số điện thoại TEST đều là dữ liệu giả.
-- Chạy sau V1, trước V2. Không tự chạy file này trong SSMS.
-- Không ghi IDENTITY cứng: lấy ID qua mã nghiệp vụ.
INSERT dbo.Users(LoginName,PasswordHash,DisplayName,IsActive)
VALUES ('demo.archived','$2b$12$cCWtwx8NrhNaUGeOcL670uLpnroHAKwJDDiry2zjhgRPGr47L3.l6',N'Nhân viên mẫu đã khóa',0);
DECLARE @actor bigint=(SELECT UserID FROM dbo.Users WHERE LoginName='demo.archived');

INSERT dbo.Roles(RoleCode,Name) VALUES
('OWNER',N'Chủ cửa hàng'),('SALES',N'Bán hàng'),('TECHNICIAN',N'Kỹ thuật'),
('INVENTORY',N'Quản lý kho'),('CUSTOMER',N'Khách hàng');
-- Chưa cấp quyền/role cho tài khoản demo đã khóa; không có tài khoản admin mặc định.

INSERT dbo.Categories(CategoryCode,Name) VALUES
('LAPTOP',N'Laptop'),('TV',N'Tivi'),('ACCESSORY',N'Phụ kiện');
INSERT dbo.Brands(BrandCode,Name) VALUES ('DELL',N'Dell'),('SAMSUNG',N'Samsung'),('GENERIC',N'Phổ thông');
INSERT dbo.Units(UnitCode,Name) VALUES ('PIECE',N'Chiếc'),('SET',N'Bộ');
DECLARE @laptop bigint=(SELECT CategoryID FROM dbo.Categories WHERE CategoryCode='LAPTOP');
DECLARE @tv bigint=(SELECT CategoryID FROM dbo.Categories WHERE CategoryCode='TV');
DECLARE @acc bigint=(SELECT CategoryID FROM dbo.Categories WHERE CategoryCode='ACCESSORY');
DECLARE @dell bigint=(SELECT BrandID FROM dbo.Brands WHERE BrandCode='DELL');
DECLARE @samsung bigint=(SELECT BrandID FROM dbo.Brands WHERE BrandCode='SAMSUNG');
DECLARE @generic bigint=(SELECT BrandID FROM dbo.Brands WHERE BrandCode='GENERIC');
DECLARE @piece bigint=(SELECT UnitID FROM dbo.Units WHERE UnitCode='PIECE');

INSERT dbo.Products(SKU,Name,CategoryID,BrandID,UnitID,StockMode,ListPrice) VALUES
('DEMO-DELL-5420',N'Laptop Dell Latitude 5420 i5 16GB 512GB',@laptop,@dell,@piece,'SERIALIZED',6500000),
('DEMO-TV-43',N'Tivi Samsung 43 inch',@tv,@samsung,@piece,'SERIALIZED',3500000),
('DEMO-MOUSE',N'Chuột USB',@acc,@generic,@piece,'BULK',90000),
('DEMO-HDMI',N'Cáp HDMI 1.5m',@acc,@generic,@piece,'BULK',70000);
DECLARE @lapSku bigint=(SELECT ProductID FROM dbo.Products WHERE SKU='DEMO-DELL-5420');
DECLARE @tvSku bigint=(SELECT ProductID FROM dbo.Products WHERE SKU='DEMO-TV-43');
DECLARE @mouse bigint=(SELECT ProductID FROM dbo.Products WHERE SKU='DEMO-MOUSE');
DECLARE @hdmi bigint=(SELECT ProductID FROM dbo.Products WHERE SKU='DEMO-HDMI');

INSERT dbo.Customers(CustomerCode,FullName,Phone,Email) VALUES
('DEMO-KH001',N'Khách mẫu An','TEST-0001','an@example.invalid'),
('DEMO-KH002',N'Khách mẫu Bình','TEST-0002',NULL),
('DEMO-KH003',N'Khách mẫu Chi','TEST-0002',NULL);
-- Bình/Chi có cùng số liên hệ giả: số điện thoại KH không UNIQUE theo thiết kế.
DECLARE @an bigint=(SELECT CustomerID FROM dbo.Customers WHERE CustomerCode='DEMO-KH001');
DECLARE @binh bigint=(SELECT CustomerID FROM dbo.Customers WHERE CustomerCode='DEMO-KH002');
DECLARE @chi bigint=(SELECT CustomerID FROM dbo.Customers WHERE CustomerCode='DEMO-KH003');
INSERT dbo.CustomerAddresses(CustomerID,RecipientName,RecipientPhone,AddressLine,WardText,ProvinceText,IsDefault)
VALUES (@an,N'Khách mẫu An','TEST-0001',N'Địa chỉ mẫu 01',N'Xã mẫu',N'Quảng Ngãi',1);

INSERT dbo.Devices(AssetTag,ProductID,OwnerCustomerID,ManufacturerSerial,DeviceDescription,ConditionGrade,AskingPrice)
VALUES ('DEMO-D001',@lapSku,NULL,'DEMO-SERIAL-A',N'Máy cửa hàng, có trầy nhẹ', 'GOOD',6200000),
('DEMO-D002',@lapSku,NULL,'DEMO-SERIAL-B',N'Máy cửa hàng, ngoại hình đẹp','LIKE_NEW',6800000),
('DEMO-D003',@tvSku,NULL,NULL,N'Tivi cửa hàng đã kiểm tra','GOOD',3300000),
('DEMO-C001',NULL,@an,NULL,N'Đầu đĩa khách gửi sửa, chưa có model trong Products','FAULTY',NULL);
DECLARE @lap1 bigint=(SELECT DeviceID FROM dbo.Devices WHERE AssetTag='DEMO-D001');
DECLARE @lap2 bigint=(SELECT DeviceID FROM dbo.Devices WHERE AssetTag='DEMO-D002');
DECLARE @tv1 bigint=(SELECT DeviceID FROM dbo.Devices WHERE AssetTag='DEMO-D003');
DECLARE @customerDevice bigint=(SELECT DeviceID FROM dbo.Devices WHERE AssetTag='DEMO-C001');

INSERT dbo.StockLocations(LocationCode,Name,LocationType) VALUES
('DEMO-SALE',N'Kệ bán được','SALEABLE'),('DEMO-CHECK',N'Khu cách ly kiểm tra','QUARANTINE');
DECLARE @loc bigint=(SELECT LocationID FROM dbo.StockLocations WHERE LocationCode='DEMO-SALE');
-- Nhà cung cấp mẫu dành riêng cho DEVELOPMENT.
-- Số điện thoại TEST, không phải số liên hệ thực tế.

INSERT dbo.Suppliers
(
    SupplierCode,
    Name,
    Phone,
    Address,
    TaxCode
)
VALUES
(
    'DEMO-SUP',
    N'Nhà cung cấp mẫu',
    'TEST-0003',
    N'Địa chỉ nhà cung cấp mẫu - Quảng Ngãi',
    NULL
);
DECLARE @supplier bigint=(SELECT SupplierID FROM dbo.Suppliers WHERE SupplierCode='DEMO-SUP');
INSERT dbo.InventoryDocuments(DocumentCode,DocumentType,Status,CreatedBy,Reason)
VALUES ('DEMO-PN001','RECEIPT','DRAFT',@actor,N'Nhập kho mẫu cho bài thực hành');
DECLARE @receipt bigint=(SELECT InventoryDocID FROM dbo.InventoryDocuments WHERE DocumentCode='DEMO-PN001');
INSERT dbo.PurchaseReceipts(InventoryDocID,SupplierID,SupplierReference)
VALUES (@receipt,@supplier,'DEMO-REF-001');
INSERT dbo.BulkStockEntries(InventoryDocID,ProductID,LocationID,Delta,UnitCost)
VALUES (@receipt,@mouse,@loc,20,50000),(@receipt,@hdmi,@loc,15,35000);
INSERT dbo.DeviceStockEntries(InventoryDocID,DeviceID,FromLocationID,ToLocationID,UnitCost)
VALUES (@receipt,@lap1,NULL,@loc,5000000),(@receipt,@lap2,NULL,@loc,5500000),(@receipt,@tv1,NULL,@loc,2500000);
-- Chứng từ mẫu có dòng nhập hợp lệ trước khi POSTED; máy khách KHÔNG nhập vào kho.
UPDATE dbo.InventoryDocuments SET Status='POSTED' WHERE InventoryDocID=@receipt;

INSERT dbo.SalesOrders(OrderCode,CustomerID,Channel,Status,CreatedBy) VALUES
('DEMO-DH001',@an,'COUNTER','DRAFT',@actor),
('DEMO-DH002',@an,'WEB','DRAFT',NULL),
('DEMO-DH003',@binh,'COUNTER','DRAFT',@actor);
DECLARE @o1 bigint=(SELECT OrderID FROM dbo.SalesOrders WHERE OrderCode='DEMO-DH001');
DECLARE @o2 bigint=(SELECT OrderID FROM dbo.SalesOrders WHERE OrderCode='DEMO-DH002');
DECLARE @o3 bigint=(SELECT OrderID FROM dbo.SalesOrders WHERE OrderCode='DEMO-DH003');
-- ==========================================================
-- SALES LINES - DỮ LIỆU ĐƠN HÀNG MẪU
-- ==========================================================

INSERT dbo.SalesLines
(
    OrderID,
    [LineNo],
    DescriptionSnapshot,
    UnitPrice,
    DiscountAmount,
    TaxRate,
    WarrantyDaysSnapshot
)
VALUES
(
    @o1, 1,
    N'Laptop mẫu D001',
    6200000, 0, 0, 90
),
(
    @o1, 2,
    N'Chuột USB',
    90000, 10000, 0, 30
),
(
    @o2, 1,
    N'Cáp HDMI 1.5m',
    70000, 0, 0, 30
),
(
    @o3, 1,
    N'Chuột USB',
    90000, 0, 0, 30
);

-- Lấy ID dòng bán laptop.
DECLARE @laptopLine BIGINT =
(
    SELECT SaleLineID
    FROM dbo.SalesLines
    WHERE OrderID = @o1
      AND [LineNo] = 1
);

-- Lấy ID dòng bán chuột trong đơn DH001.
DECLARE @mouseLine BIGINT =
(
    SELECT SaleLineID
    FROM dbo.SalesLines
    WHERE OrderID = @o1
      AND [LineNo] = 2
);

-- Lấy ID dòng bán HDMI.
DECLARE @hdmiLine BIGINT =
(
    SELECT SaleLineID
    FROM dbo.SalesLines
    WHERE OrderID = @o2
      AND [LineNo] = 1
);

-- Lấy ID dòng bán chuột trong đơn DH003.
DECLARE @mouseLine2 BIGINT =
(
    SELECT SaleLineID
    FROM dbo.SalesLines
    WHERE OrderID = @o3
      AND [LineNo] = 1
);

-- Liên kết dòng bán với thiết bị cụ thể.
INSERT dbo.DeviceSaleLines
(
    SaleLineID,
    DeviceID
)
VALUES
(
    @laptopLine,
    @lap1
);

-- Liên kết dòng bán với sản phẩm BULK.
INSERT dbo.BulkSaleLines
(
    SaleLineID,
    ProductID,
    Quantity
)
VALUES
(
    @mouseLine,
    @mouse,
    2
),
(
    @hdmiLine,
    @hdmi,
    3
),
(
    @mouseLine2,
    @mouse,
    1
);

-- Toàn bộ là đơn NHÁP:
-- Chưa thu tiền.
-- Chưa xuất kho.
-- Chưa tính doanh thu.
-- Toàn bộ là đơn NHÁP: chưa thu tiền, chưa xuất kho, chưa tính doanh thu.

INSERT dbo.RepairTickets(TicketCode,DeviceID,CustomerID,ReceivedBy,ReportedProblem,IntakeCondition,TrackingTokenHash)
VALUES ('DEMO-SC001',@customerDevice,@an,@actor,N'Không đọc được đĩa',N'Vỏ trầy, kèm dây nguồn',HASHBYTES('SHA2_256',CRYPT_GEN_RANDOM(32)));
DECLARE @ticket bigint=(SELECT RepairTicketID FROM dbo.RepairTickets WHERE TicketCode='DEMO-SC001');
INSERT dbo.RepairAccessories(RepairTicketID,ItemNo,Description,Quantity)
VALUES (@ticket,1,N'Dây nguồn khách gửi',1);
INSERT dbo.RepairEvents(RepairTicketID,FromStatus,ToStatus,Note,IsPublic,ActorID)
VALUES (@ticket,NULL,'RECEIVED',N'Đã tiếp nhận thiết bị mẫu',1,@actor);
