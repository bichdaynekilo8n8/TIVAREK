-- TIVAREK | SQL Server | 2026-11-04 | fresh schema only

-- Chỉ chọn database trống chuyên dùng. Không chạy đè products JPA cũ.

-- DDL gồm PK/FK/UQ/CHECK/index; các quy tắc liên bảng BR01–BR24 nằm trong báo cáo, CHƯA được triển khai bằng trigger/procedure.

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
GO

-- Tài khoản nhân viên hoặc khách đăng nhập; không bắt buộc khách vãng lai có tài khoản.

CREATE TABLE dbo.[Users] (
    [UserID] bigint IDENTITY(1,1) NOT NULL,
    [LoginName] varchar(100) NOT NULL,
    [PasswordHash] varchar(255) NOT NULL,
    [DisplayName] nvarchar(180) NOT NULL,
    [IsActive] bit NOT NULL DEFAULT (1),
    [CreatedAt] datetime2(3) NOT NULL DEFAULT (SYSUTCDATETIME()),
    [Version] rowversion NOT NULL,
    CONSTRAINT [PK_Users] PRIMARY KEY ([UserID]),
    CONSTRAINT [UQ_Users_1] UNIQUE ([LoginName])
);
GO

-- Vai trò OWNER, SALES, TECHNICIAN, INVENTORY, CUSTOMER.

CREATE TABLE dbo.[Roles] (
    [RoleID] bigint IDENTITY(1,1) NOT NULL,
    [RoleCode] varchar(40) NOT NULL,
    [Name] nvarchar(180) NOT NULL,
    CONSTRAINT [PK_Roles] PRIMARY KEY ([RoleID]),
    CONSTRAINT [UQ_Roles_1] UNIQUE ([RoleCode])
);
GO

-- Quyền theo hành động, ví dụ REPAIR_QUOTE_APPROVE.

CREATE TABLE dbo.[Permissions] (
    [PermissionID] bigint IDENTITY(1,1) NOT NULL,
    [PermissionCode] varchar(40) NOT NULL,
    [Name] nvarchar(180) NOT NULL,
    CONSTRAINT [PK_Permissions] PRIMARY KEY ([PermissionID]),
    CONSTRAINT [UQ_Permissions_1] UNIQUE ([PermissionCode])
);
GO

-- Quan hệ nhiều–nhiều tài khoản–vai trò.

CREATE TABLE dbo.[UserRoles] (
    [UserID] bigint NOT NULL,
    [RoleID] bigint NOT NULL,
    CONSTRAINT [PK_UserRoles] PRIMARY KEY ([UserID], [RoleID])
);
GO

-- Quan hệ nhiều–nhiều vai trò–quyền.

CREATE TABLE dbo.[RolePermissions] (
    [RoleID] bigint NOT NULL,
    [PermissionID] bigint NOT NULL,
    CONSTRAINT [PK_RolePermissions] PRIMARY KEY ([RoleID], [PermissionID])
);
GO

-- Hồ sơ khách mua hoặc gửi sửa. Số điện thoại có thể dùng chung, không ép UNIQUE.

CREATE TABLE dbo.[Customers] (
    [CustomerID] bigint IDENTITY(1,1) NOT NULL,
    [CustomerCode] varchar(40) NOT NULL,
    [FullName] nvarchar(180) NOT NULL,
    [Phone] varchar(25) NOT NULL,
    [Email] varchar(254) NULL,
    [UserID] bigint NULL,
    [CreatedAt] datetime2(3) NOT NULL DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT [PK_Customers] PRIMARY KEY ([CustomerID]),
    CONSTRAINT [UQ_Customers_1] UNIQUE ([CustomerCode])
);
GO

-- Địa chỉ có cấu trúc lưu theo khách; không áp đặt mã địa giới cũ.

CREATE TABLE dbo.[CustomerAddresses] (
    [AddressID] bigint IDENTITY(1,1) NOT NULL,
    [CustomerID] bigint NOT NULL,
    [RecipientName] nvarchar(180) NOT NULL,
    [RecipientPhone] varchar(25) NOT NULL,
    [AddressLine] nvarchar(250) NOT NULL,
    [WardText] nvarchar(120) NOT NULL,
    [ProvinceText] nvarchar(120) NOT NULL,
    [IsDefault] bit NOT NULL DEFAULT (0),
    CONSTRAINT [PK_CustomerAddresses] PRIMARY KEY ([AddressID])
);
GO

-- Danh mục phân cấp: tivi, laptop, phụ kiện, linh kiện…

CREATE TABLE dbo.[Categories] (
    [CategoryID] bigint IDENTITY(1,1) NOT NULL,
    [CategoryCode] varchar(40) NOT NULL,
    [Name] nvarchar(180) NOT NULL,
    [ParentCategoryID] bigint NULL,
    [IsActive] bit NOT NULL DEFAULT (1),
    CONSTRAINT [PK_Categories] PRIMARY KEY ([CategoryID]),
    CONSTRAINT [UQ_Categories_1] UNIQUE ([CategoryCode]),
    CONSTRAINT [CK_Categories_1] CHECK ([ParentCategoryID] IS NULL OR [ParentCategoryID] <> [CategoryID])
);
GO

-- Thương hiệu sản phẩm.

CREATE TABLE dbo.[Brands] (
    [BrandID] bigint IDENTITY(1,1) NOT NULL,
    [BrandCode] varchar(40) NOT NULL,
    [Name] nvarchar(180) NOT NULL,
    CONSTRAINT [PK_Brands] PRIMARY KEY ([BrandID]),
    CONSTRAINT [UQ_Brands_1] UNIQUE ([BrandCode])
);
GO

-- Đơn vị nguyên: chiếc, cái, bộ. Mỗi SKU dùng một đơn vị; không hỗ trợ quy đổi đóng gói ở bản này.

CREATE TABLE dbo.[Units] (
    [UnitID] bigint IDENTITY(1,1) NOT NULL,
    [UnitCode] varchar(40) NOT NULL,
    [Name] nvarchar(180) NOT NULL,
    CONSTRAINT [PK_Units] PRIMARY KEY ([UnitID]),
    CONSTRAINT [UQ_Units_1] UNIQUE ([UnitCode])
);
GO

-- Một SKU/model/cấu hình chuẩn. Khác RAM/SSD/cấu hình bán thì tạo SKU khác; không có số tồn hoặc serial tại đây.

CREATE TABLE dbo.[Products] (
    [ProductID] bigint IDENTITY(1,1) NOT NULL,
    [SKU] varchar(40) NOT NULL,
    [Name] nvarchar(180) NOT NULL,
    [CategoryID] bigint NOT NULL,
    [BrandID] bigint NULL,
    [UnitID] bigint NOT NULL,
    [StockMode] varchar(24) NOT NULL,
    [ListPrice] decimal(19,2) NOT NULL,
    [IsActive] bit NOT NULL DEFAULT (1),
    CONSTRAINT [PK_Products] PRIMARY KEY ([ProductID]),
    CONSTRAINT [UQ_Products_1] UNIQUE ([SKU]),
    CONSTRAINT [CK_Products_1] CHECK ([StockMode] IN ('SERIALIZED', 'BULK')),
    CONSTRAINT [CK_Products_2] CHECK ([ListPrice] >= 0)
);
GO

-- Mỗi hàng một ảnh; thứ tự duy nhất trong sản phẩm.

CREATE TABLE dbo.[ProductImages] (
    [ImageID] bigint IDENTITY(1,1) NOT NULL,
    [ProductID] bigint NOT NULL,
    [ImageURL] nvarchar(600) NOT NULL,
    [SortOrder] int NOT NULL,
    CONSTRAINT [PK_ProductImages] PRIMARY KEY ([ImageID]),
    CONSTRAINT [UQ_ProductImages_1] UNIQUE ([ProductID], [SortOrder]),
    CONSTRAINT [CK_ProductImages_1] CHECK ([SortOrder] >= 0)
);
GO

-- Định nghĩa một thông số; đơn vị là quy ước của thông số.

CREATE TABLE dbo.[AttributeDefinitions] (
    [AttributeID] bigint IDENTITY(1,1) NOT NULL,
    [AttributeCode] varchar(40) NOT NULL,
    [Name] nvarchar(180) NOT NULL,
    [UnitLabel] nvarchar(30) NULL,
    [ValueType] varchar(24) NOT NULL,
    CONSTRAINT [PK_AttributeDefinitions] PRIMARY KEY ([AttributeID]),
    CONSTRAINT [UQ_AttributeDefinitions_1] UNIQUE ([AttributeCode]),
    CONSTRAINT [CK_AttributeDefinitions_1] CHECK ([ValueType] IN ('TEXT', 'INTEGER', 'DECIMAL', 'BOOLEAN'))
);
GO

-- Một giá trị nguyên tử cho mỗi cặp SKU–thông số. ValueText phải parse được theo ValueType ở service.

CREATE TABLE dbo.[ProductAttributeValues] (
    [ProductID] bigint NOT NULL,
    [AttributeID] bigint NOT NULL,
    [ValueText] nvarchar(250) NOT NULL,
    CONSTRAINT [PK_ProductAttributeValues] PRIMARY KEY ([ProductID], [AttributeID])
);
GO

-- Một máy vật lý. Gồm hàng của cửa hàng và máy khách gửi; ProductID được NULL để tiếp nhận model lạ.

CREATE TABLE dbo.[Devices] (
    [DeviceID] bigint IDENTITY(1,1) NOT NULL,
    [AssetTag] varchar(40) NOT NULL,
    [ProductID] bigint NULL,
    [OwnerCustomerID] bigint NULL,
    [ManufacturerSerial] nvarchar(100) NULL,
    [DeviceDescription] nvarchar(2000) NOT NULL,
    [ConditionGrade] varchar(24) NOT NULL,
    [AskingPrice] decimal(19,2) NULL,
    [Version] rowversion NOT NULL,
    CONSTRAINT [PK_Devices] PRIMARY KEY ([DeviceID]),
    CONSTRAINT [UQ_Devices_1] UNIQUE ([AssetTag]),
    CONSTRAINT [CK_Devices_1] CHECK ([ConditionGrade] IN ('NEW', 'LIKE_NEW', 'GOOD', 'FAIR', 'FAULTY')),
    CONSTRAINT [CK_Devices_2] CHECK ([AskingPrice] >= 0)
);
GO

-- Ảnh thật từng máy, gồm ảnh lỗi/tình trạng.

CREATE TABLE dbo.[DeviceImages] (
    [ImageID] bigint IDENTITY(1,1) NOT NULL,
    [DeviceID] bigint NOT NULL,
    [ImageURL] nvarchar(600) NOT NULL,
    [SortOrder] int NOT NULL,
    CONSTRAINT [PK_DeviceImages] PRIMARY KEY ([ImageID]),
    CONSTRAINT [UQ_DeviceImages_1] UNIQUE ([DeviceID], [SortOrder]),
    CONSTRAINT [CK_DeviceImages_1] CHECK ([SortOrder] >= 0)
);
GO

-- Lần kiểm định tình trạng một máy trước nhập/bán/sau sửa.

CREATE TABLE dbo.[DeviceChecks] (
    [CheckID] bigint IDENTITY(1,1) NOT NULL,
    [DeviceID] bigint NOT NULL,
    [CheckedBy] bigint NOT NULL,
    [CheckedAt] datetime2(3) NOT NULL DEFAULT (SYSUTCDATETIME()),
    [Findings] nvarchar(2000) NOT NULL,
    [Result] varchar(24) NOT NULL,
    CONSTRAINT [PK_DeviceChecks] PRIMARY KEY ([CheckID]),
    CONSTRAINT [CK_DeviceChecks_1] CHECK ([Result] IN ('PASS', 'FAIL', 'NEEDS_REPAIR'))
);
GO

-- Nhà cung cấp, gồm cá nhân bán lại máy cũ.

CREATE TABLE dbo.[Suppliers] (
    [SupplierID] bigint IDENTITY(1,1) NOT NULL,
    [SupplierCode] varchar(40) NOT NULL,
    [Name] nvarchar(180) NOT NULL,
    [Phone] varchar(25) NOT NULL,
    [Address] nvarchar(2000) NULL,
    [TaxCode] varchar(30) NULL,
    CONSTRAINT [PK_Suppliers] PRIMARY KEY ([SupplierID]),
    CONSTRAINT [UQ_Suppliers_1] UNIQUE ([SupplierCode])
);
GO

-- Vị trí vật lý của tài sản cửa hàng: kho bán, khu cách ly, hàng lỗi. Máy khách không ghi vào sổ kho tài sản.

CREATE TABLE dbo.[StockLocations] (
    [LocationID] bigint IDENTITY(1,1) NOT NULL,
    [LocationCode] varchar(40) NOT NULL,
    [Name] nvarchar(180) NOT NULL,
    [LocationType] varchar(24) NOT NULL,
    [IsActive] bit NOT NULL DEFAULT (1),
    CONSTRAINT [PK_StockLocations] PRIMARY KEY ([LocationID]),
    CONSTRAINT [UQ_StockLocations_1] UNIQUE ([LocationCode]),
    CONSTRAINT [CK_StockLocations_1] CHECK ([LocationType] IN ('SALEABLE', 'QUARANTINE', 'DAMAGED'))
);
GO

-- Chứng từ kho chung; chỉ POSTED mới có tác dụng tồn. Đảo bằng chứng từ bù, không sửa sổ đã ghi.

CREATE TABLE dbo.[InventoryDocuments] (
    [InventoryDocID] bigint IDENTITY(1,1) NOT NULL,
    [DocumentCode] varchar(40) NOT NULL,
    [DocumentType] varchar(24) NOT NULL,
    [Status] varchar(24) NOT NULL DEFAULT ('DRAFT'),
    [OccurredAt] datetime2(3) NOT NULL DEFAULT (SYSUTCDATETIME()),
    [CreatedBy] bigint NOT NULL,
    [Reason] nvarchar(2000) NOT NULL,
    CONSTRAINT [PK_InventoryDocuments] PRIMARY KEY ([InventoryDocID]),
    CONSTRAINT [UQ_InventoryDocuments_1] UNIQUE ([DocumentCode]),
    CONSTRAINT [CK_InventoryDocuments_1] CHECK ([DocumentType] IN ('RECEIPT', 'SALE_ISSUE', 'SALE_RETURN', 'REPAIR_USE', 'TRANSFER', 'ADJUSTMENT', 'REVERSAL')),
    CONSTRAINT [CK_InventoryDocuments_2] CHECK ([Status] IN ('DRAFT', 'POSTED'))
);
GO

-- Phần mở rộng của chứng từ RECEIPT; giá nhập nằm trong từng bút toán.

CREATE TABLE dbo.[PurchaseReceipts] (
    [InventoryDocID] bigint NOT NULL,
    [SupplierID] bigint NOT NULL,
    [SupplierReference] nvarchar(100) NULL,
    CONSTRAINT [PK_PurchaseReceipts] PRIMARY KEY ([InventoryDocID])
);
GO

-- Sổ kho số lượng có dấu. Một hàng = một SKU tại một vị trí, thuộc một chứng từ.

CREATE TABLE dbo.[BulkStockEntries] (
    [BulkEntryID] bigint IDENTITY(1,1) NOT NULL,
    [InventoryDocID] bigint NOT NULL,
    [ProductID] bigint NOT NULL,
    [LocationID] bigint NOT NULL,
    [Delta] int NOT NULL,
    [UnitCost] decimal(19,2) NOT NULL,
    CONSTRAINT [PK_BulkStockEntries] PRIMARY KEY ([BulkEntryID]),
    CONSTRAINT [CK_BulkStockEntries_1] CHECK ([Delta] <> 0),
    CONSTRAINT [CK_BulkStockEntries_2] CHECK ([UnitCost] >= 0)
);
GO

-- Một lần di chuyển đúng một máy cửa hàng; From NULL: nhập ngoài; To NULL: xuất ngoài.

CREATE TABLE dbo.[DeviceStockEntries] (
    [DeviceEntryID] bigint IDENTITY(1,1) NOT NULL,
    [InventoryDocID] bigint NOT NULL,
    [DeviceID] bigint NOT NULL,
    [FromLocationID] bigint NULL,
    [ToLocationID] bigint NULL,
    [UnitCost] decimal(19,2) NOT NULL,
    CONSTRAINT [PK_DeviceStockEntries] PRIMARY KEY ([DeviceEntryID]),
    CONSTRAINT [UQ_DeviceStockEntries_1] UNIQUE ([InventoryDocID], [DeviceID]),
    CONSTRAINT [CK_DeviceStockEntries_1] CHECK (([FromLocationID] IS NOT NULL OR [ToLocationID] IS NOT NULL) AND ([FromLocationID] IS NULL OR [ToLocationID] IS NULL OR [FromLocationID] <> [ToLocationID])),
    CONSTRAINT [CK_DeviceStockEntries_2] CHECK ([UnitCost] >= 0)
);
GO

-- Liên kết chứng từ gốc và chứng từ đảo; một gốc chỉ được đảo một lần.

CREATE TABLE dbo.[InventoryReversals] (
    [OriginalDocID] bigint NOT NULL,
    [ReversalDocID] bigint NOT NULL,
    CONSTRAINT [PK_InventoryReversals] PRIMARY KEY ([OriginalDocID]),
    CONSTRAINT [UQ_InventoryReversals_1] UNIQUE ([ReversalDocID]),
    CONSTRAINT [CK_InventoryReversals_1] CHECK ([OriginalDocID] <> [ReversalDocID])
);
GO

-- Đơn khách đăng nhập hoặc khách vãng lai. Không lưu tổng tiền/tổng đã thu.

CREATE TABLE dbo.[SalesOrders] (
    [OrderID] bigint IDENTITY(1,1) NOT NULL,
    [OrderCode] varchar(40) NOT NULL,
    [CustomerID] bigint NULL,
    [Channel] varchar(24) NOT NULL,
    [Status] varchar(24) NOT NULL DEFAULT ('DRAFT'),
    [OrderedAt] datetime2(3) NOT NULL DEFAULT (SYSUTCDATETIME()),
    [CreatedBy] bigint NULL,
    [Version] rowversion NOT NULL,
    CONSTRAINT [PK_SalesOrders] PRIMARY KEY ([OrderID]),
    CONSTRAINT [UQ_SalesOrders_1] UNIQUE ([OrderCode]),
    CONSTRAINT [CK_SalesOrders_1] CHECK ([Channel] IN ('WEB', 'COUNTER')),
    CONSTRAINT [CK_SalesOrders_2] CHECK ([Status] IN ('DRAFT', 'CONFIRMED', 'FULFILLING', 'COMPLETED', 'CANCELLED'))
);
GO

-- Thông tin người nhận và địa chỉ đã chốt riêng cho đơn, không phụ thuộc thay đổi hồ sơ khách.

CREATE TABLE dbo.[OrderShipping] (
    [OrderID] bigint NOT NULL,
    [RecipientName] nvarchar(180) NOT NULL,
    [RecipientPhone] varchar(25) NOT NULL,
    [AddressSnapshot] nvarchar(2000) NULL,
    [ShippingFee] decimal(19,2) NOT NULL,
    [FulfillmentMethod] varchar(24) NOT NULL,
    CONSTRAINT [PK_OrderShipping] PRIMARY KEY ([OrderID]),
    CONSTRAINT [CK_OrderShipping_1] CHECK ([FulfillmentMethod] = 'PICKUP' OR [AddressSnapshot] IS NOT NULL),
    CONSTRAINT [CK_OrderShipping_2] CHECK ([ShippingFee] >= 0),
    CONSTRAINT [CK_OrderShipping_3] CHECK ([FulfillmentMethod] IN ('PICKUP', 'DELIVERY'))
);
GO

-- Phần chung của dòng bán. Phải có đúng một loại con BulkSaleLines hoặc DeviceSaleLines.

CREATE TABLE dbo.[SalesLines] (
    [SaleLineID] bigint IDENTITY(1,1) NOT NULL,
    [OrderID] bigint NOT NULL,
    [LineNo] int NOT NULL,
    [DescriptionSnapshot] nvarchar(250) NOT NULL,
    [UnitPrice] decimal(19,2) NOT NULL,
    [DiscountAmount] decimal(19,2) NOT NULL DEFAULT (0),
    [TaxRate] decimal(5,2) NOT NULL DEFAULT (0),
    [WarrantyDaysSnapshot] int NOT NULL,
    CONSTRAINT [PK_SalesLines] PRIMARY KEY ([SaleLineID]),
    CONSTRAINT [UQ_SalesLines_1] UNIQUE ([OrderID], [LineNo]),
    CONSTRAINT [CK_SalesLines_1] CHECK ([LineNo] > 0),
    CONSTRAINT [CK_SalesLines_2] CHECK ([TaxRate] BETWEEN 0 AND 100),
    CONSTRAINT [CK_SalesLines_3] CHECK ([WarrantyDaysSnapshot] >= 0),
    CONSTRAINT [CK_SalesLines_4] CHECK ([UnitPrice] >= 0),
    CONSTRAINT [CK_SalesLines_5] CHECK ([DiscountAmount] >= 0)
);
GO

-- Dòng bán SKU theo số lượng, chỉ StockMode BULK.

CREATE TABLE dbo.[BulkSaleLines] (
    [SaleLineID] bigint NOT NULL,
    [ProductID] bigint NOT NULL,
    [Quantity] int NOT NULL,
    CONSTRAINT [PK_BulkSaleLines] PRIMARY KEY ([SaleLineID]),
    CONSTRAINT [CK_BulkSaleLines_1] CHECK ([Quantity] > 0)
);
GO

-- Dòng bán đúng một máy. Không chép ProductID vì DeviceID đã xác định ProductID.

CREATE TABLE dbo.[DeviceSaleLines] (
    [SaleLineID] bigint NOT NULL,
    [DeviceID] bigint NOT NULL,
    CONSTRAINT [PK_DeviceSaleLines] PRIMARY KEY ([SaleLineID])
);
GO

-- Giữ số lượng cho dòng bán tại một vị trí; SKU suy từ BulkSaleLines.

CREATE TABLE dbo.[BulkReservations] (
    [ReservationID] bigint IDENTITY(1,1) NOT NULL,
    [SaleLineID] bigint NOT NULL,
    [LocationID] bigint NOT NULL,
    [Quantity] int NOT NULL,
    [CreatedAt] datetime2(3) NOT NULL DEFAULT (SYSUTCDATETIME()),
    [ExpiresAt] datetime2(3) NOT NULL,
    [Status] varchar(24) NOT NULL DEFAULT ('ACTIVE'),
    CONSTRAINT [PK_BulkReservations] PRIMARY KEY ([ReservationID]),
    CONSTRAINT [CK_BulkReservations_1] CHECK ([ExpiresAt] > [CreatedAt]),
    CONSTRAINT [CK_BulkReservations_2] CHECK ([Quantity] > 0),
    CONSTRAINT [CK_BulkReservations_3] CHECK ([Status] IN ('ACTIVE', 'RELEASED', 'CONSUMED', 'EXPIRED'))
);
GO

-- Lịch sử các lần giữ một dòng máy; máy suy từ DeviceSaleLines.

CREATE TABLE dbo.[DeviceReservations] (
    [ReservationID] bigint IDENTITY(1,1) NOT NULL,
    [SaleLineID] bigint NOT NULL,
    [CreatedAt] datetime2(3) NOT NULL DEFAULT (SYSUTCDATETIME()),
    [ExpiresAt] datetime2(3) NOT NULL,
    [Status] varchar(24) NOT NULL DEFAULT ('ACTIVE'),
    CONSTRAINT [PK_DeviceReservations] PRIMARY KEY ([ReservationID]),
    CONSTRAINT [CK_DeviceReservations_1] CHECK ([ExpiresAt] > [CreatedAt]),
    CONSTRAINT [CK_DeviceReservations_2] CHECK ([Status] IN ('ACTIVE', 'RELEASED', 'CONSUMED', 'EXPIRED'))
);
GO

-- Khóa giữ độc quyền: DeviceID PK chặn hai người giữ cùng máy. ReservationID cũng là khóa ứng viên.

CREATE TABLE dbo.[ActiveDeviceReservations] (
    [DeviceID] bigint NOT NULL,
    [ReservationID] bigint NOT NULL,
    CONSTRAINT [PK_ActiveDeviceReservations] PRIMARY KEY ([DeviceID]),
    CONSTRAINT [UQ_ActiveDeviceReservations_1] UNIQUE ([ReservationID])
);
GO

-- Lần bàn giao/xuất cho đơn; hỗ trợ giao từng phần.

CREATE TABLE dbo.[SalesDeliveries] (
    [InventoryDocID] bigint NOT NULL,
    [OrderID] bigint NOT NULL,
    [Carrier] nvarchar(100) NULL,
    [TrackingCode] varchar(100) NULL,
    CONSTRAINT [PK_SalesDeliveries] PRIMARY KEY ([InventoryDocID])
);
GO

-- Chỉ rõ bút toán xuất số lượng phục vụ dòng bán nào; số lượng lấy -Delta.

CREATE TABLE dbo.[SaleBulkIssues] (
    [BulkEntryID] bigint NOT NULL,
    [SaleLineID] bigint NOT NULL,
    CONSTRAINT [PK_SaleBulkIssues] PRIMARY KEY ([BulkEntryID])
);
GO

-- Chỉ rõ lần xuất máy phục vụ dòng bán nào; không lặp DeviceID.

CREATE TABLE dbo.[SaleDeviceIssues] (
    [DeviceEntryID] bigint NOT NULL,
    [SaleLineID] bigint NOT NULL,
    CONSTRAINT [PK_SaleDeviceIssues] PRIMARY KEY ([DeviceEntryID])
);
GO

-- Phiếu nhận trả thuộc đơn gốc; tiền hoàn theo Payments OUT.

CREATE TABLE dbo.[SalesReturns] (
    [InventoryDocID] bigint NOT NULL,
    [OrderID] bigint NOT NULL,
    [ReturnReason] nvarchar(2000) NOT NULL,
    CONSTRAINT [PK_SalesReturns] PRIMARY KEY ([InventoryDocID])
);
GO

-- Bút toán nhập trả của dòng hàng số lượng.

CREATE TABLE dbo.[ReturnBulkEntries] (
    [BulkEntryID] bigint NOT NULL,
    [SaleLineID] bigint NOT NULL,
    CONSTRAINT [PK_ReturnBulkEntries] PRIMARY KEY ([BulkEntryID])
);
GO

-- Bút toán nhập lại đúng máy đã giao.

CREATE TABLE dbo.[ReturnDeviceEntries] (
    [DeviceEntryID] bigint NOT NULL,
    [SaleLineID] bigint NOT NULL,
    CONSTRAINT [PK_ReturnDeviceEntries] PRIMARY KEY ([DeviceEntryID])
);
GO

-- Danh mục công sửa và kiểm tra; giá mặc định chỉ là gợi ý.

CREATE TABLE dbo.[Services] (
    [ServiceID] bigint IDENTITY(1,1) NOT NULL,
    [ServiceCode] varchar(40) NOT NULL,
    [Name] nvarchar(180) NOT NULL,
    [SuggestedPrice] decimal(19,2) NOT NULL,
    [DefaultWarrantyDays] int NOT NULL,
    CONSTRAINT [PK_Services] PRIMARY KEY ([ServiceID]),
    CONSTRAINT [UQ_Services_1] UNIQUE ([ServiceCode]),
    CONSTRAINT [CK_Services_1] CHECK ([DefaultWarrantyDays] >= 0),
    CONSTRAINT [CK_Services_2] CHECK ([SuggestedPrice] >= 0)
);
GO

-- Đăng ký online trước khi nhận máy. Không tạo tồn kho, không bắt buộc đã có hồ sơ khách.

CREATE TABLE dbo.[RepairRequests] (
    [RequestID] bigint IDENTITY(1,1) NOT NULL,
    [RequestCode] varchar(40) NOT NULL,
    [CustomerID] bigint NULL,
    [ContactName] nvarchar(180) NOT NULL,
    [ContactPhone] varchar(25) NOT NULL,
    [DeviceAndProblem] nvarchar(2000) NOT NULL,
    [PreferredAt] datetime2(3) NULL,
    [Status] varchar(24) NOT NULL DEFAULT ('NEW'),
    [CreatedAt] datetime2(3) NOT NULL DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT [PK_RepairRequests] PRIMARY KEY ([RequestID]),
    CONSTRAINT [UQ_RepairRequests_1] UNIQUE ([RequestCode]),
    CONSTRAINT [CK_RepairRequests_1] CHECK ([Status] IN ('NEW', 'CONTACTED', 'ACCEPTED', 'CANCELLED'))
);
GO

-- Mỗi phiếu nhận đúng một máy, một người gửi; cùng máy có nhiều lần sửa theo thời gian.

CREATE TABLE dbo.[RepairTickets] (
    [RepairTicketID] bigint IDENTITY(1,1) NOT NULL,
    [TicketCode] varchar(40) NOT NULL,
    [DeviceID] bigint NOT NULL,
    [CustomerID] bigint NOT NULL,
    [RequestID] bigint NULL,
    [AssignedTechnicianID] bigint NULL,
    [ReceivedBy] bigint NOT NULL,
    [ReceivedAt] datetime2(3) NOT NULL DEFAULT (SYSUTCDATETIME()),
    [PromisedAt] datetime2(3) NULL,
    [ClosedAt] datetime2(3) NULL,
    [ReportedProblem] nvarchar(2000) NOT NULL,
    [IntakeCondition] nvarchar(2000) NOT NULL,
    [Status] varchar(24) NOT NULL DEFAULT ('RECEIVED'),
    [TrackingTokenHash] binary(32) NOT NULL,
    [Version] rowversion NOT NULL,
    CONSTRAINT [PK_RepairTickets] PRIMARY KEY ([RepairTicketID]),
    CONSTRAINT [UQ_RepairTickets_1] UNIQUE ([TicketCode]),
    CONSTRAINT [UQ_RepairTickets_2] UNIQUE ([TrackingTokenHash]),
    CONSTRAINT [CK_RepairTickets_1] CHECK ([PromisedAt] IS NULL OR [PromisedAt] >= [ReceivedAt]),
    CONSTRAINT [CK_RepairTickets_2] CHECK ([ClosedAt] IS NULL OR [ClosedAt] >= [ReceivedAt]),
    CONSTRAINT [CK_RepairTickets_3] CHECK ([Status] IN ('RECEIVED', 'DIAGNOSING', 'WAITING_APPROVAL', 'REPAIRING', 'WAITING_PARTS', 'READY', 'DELIVERED', 'CANCELLED'))
);
GO

-- Phụ kiện khách gửi kèm; mỗi loại/mô tả là một hàng, không nhét danh sách vào phiếu.

CREATE TABLE dbo.[RepairAccessories] (
    [RepairTicketID] bigint NOT NULL,
    [ItemNo] int NOT NULL,
    [Description] nvarchar(250) NOT NULL,
    [Quantity] int NOT NULL,
    CONSTRAINT [PK_RepairAccessories] PRIMARY KEY ([RepairTicketID], [ItemNo]),
    CONSTRAINT [CK_RepairAccessories_1] CHECK ([ItemNo] > 0),
    CONSTRAINT [CK_RepairAccessories_2] CHECK ([Quantity] > 0)
);
GO

-- Lịch sử trạng thái và ghi chú gửi khách; append-only.

CREATE TABLE dbo.[RepairEvents] (
    [RepairEventID] bigint IDENTITY(1,1) NOT NULL,
    [RepairTicketID] bigint NOT NULL,
    [FromStatus] varchar(24) NULL,
    [ToStatus] varchar(24) NOT NULL,
    [Note] nvarchar(2000) NOT NULL,
    [IsPublic] bit NOT NULL DEFAULT (0),
    [ActorID] bigint NOT NULL,
    [OccurredAt] datetime2(3) NOT NULL DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT [PK_RepairEvents] PRIMARY KEY ([RepairEventID]),
    CONSTRAINT [CK_RepairEvents_1] CHECK ([FromStatus] IS NULL OR [FromStatus] IN ('RECEIVED','DIAGNOSING','WAITING_APPROVAL','REPAIRING','WAITING_PARTS','READY','DELIVERED','CANCELLED')),
    CONSTRAINT [CK_RepairEvents_2] CHECK ([ToStatus] IN ('RECEIVED', 'DIAGNOSING', 'WAITING_APPROVAL', 'REPAIRING', 'WAITING_PARTS', 'READY', 'DELIVERED', 'CANCELLED'))
);
GO

-- Một gói công việc/bổ sung độc lập; nhiều báo giá ACCEPTED trên cùng phiếu được cộng. Bản nháp bị thay thế phải CANCELLED, không cộng lại.

CREATE TABLE dbo.[RepairQuotes] (
    [QuoteID] bigint IDENTITY(1,1) NOT NULL,
    [RepairTicketID] bigint NOT NULL,
    [QuoteNo] int NOT NULL,
    [Status] varchar(24) NOT NULL DEFAULT ('DRAFT'),
    [CreatedBy] bigint NOT NULL,
    [CreatedAt] datetime2(3) NOT NULL DEFAULT (SYSUTCDATETIME()),
    [ValidUntil] datetime2(3) NOT NULL,
    [RespondedAt] datetime2(3) NULL,
    [ApprovalEvidence] nvarchar(1000) NULL,
    CONSTRAINT [PK_RepairQuotes] PRIMARY KEY ([QuoteID]),
    CONSTRAINT [UQ_RepairQuotes_1] UNIQUE ([RepairTicketID], [QuoteNo]),
    CONSTRAINT [CK_RepairQuotes_1] CHECK ([QuoteNo] > 0),
    CONSTRAINT [CK_RepairQuotes_2] CHECK ([ValidUntil] > [CreatedAt]),
    CONSTRAINT [CK_RepairQuotes_3] CHECK ([Status] NOT IN ('ACCEPTED','REJECTED') OR ([RespondedAt] IS NOT NULL AND [ApprovalEvidence] IS NOT NULL)),
    CONSTRAINT [CK_RepairQuotes_4] CHECK ([RespondedAt] IS NULL OR [RespondedAt] >= [CreatedAt]),
    CONSTRAINT [CK_RepairQuotes_5] CHECK ([Status] IN ('DRAFT', 'SENT', 'ACCEPTED', 'REJECTED', 'CANCELLED'))
);
GO

-- Dòng công hoặc linh kiện, có đúng một loại con. Không lưu TicketID vì QuoteID đã suy ra phiếu.

CREATE TABLE dbo.[QuoteItems] (
    [QuoteItemID] bigint IDENTITY(1,1) NOT NULL,
    [QuoteID] bigint NOT NULL,
    [LineNo] int NOT NULL,
    [DescriptionSnapshot] nvarchar(250) NOT NULL,
    [Quantity] int NOT NULL,
    [UnitPrice] decimal(19,2) NOT NULL,
    [DiscountAmount] decimal(19,2) NOT NULL DEFAULT (0),
    [TaxRate] decimal(5,2) NOT NULL DEFAULT (0),
    [WarrantyDaysSnapshot] int NOT NULL,
    CONSTRAINT [PK_QuoteItems] PRIMARY KEY ([QuoteItemID]),
    CONSTRAINT [UQ_QuoteItems_1] UNIQUE ([QuoteID], [LineNo]),
    CONSTRAINT [CK_QuoteItems_1] CHECK ([LineNo] > 0),
    CONSTRAINT [CK_QuoteItems_2] CHECK ([TaxRate] BETWEEN 0 AND 100),
    CONSTRAINT [CK_QuoteItems_3] CHECK ([WarrantyDaysSnapshot] >= 0),
    CONSTRAINT [CK_QuoteItems_4] CHECK ([DiscountAmount] <= [Quantity] * [UnitPrice]),
    CONSTRAINT [CK_QuoteItems_5] CHECK ([Quantity] > 0),
    CONSTRAINT [CK_QuoteItems_6] CHECK ([UnitPrice] >= 0),
    CONSTRAINT [CK_QuoteItems_7] CHECK ([DiscountAmount] >= 0)
);
GO

-- Loại con linh kiện được khách chấp thuận; chỉ ra SKU cần lắp.

CREATE TABLE dbo.[QuotePartItems] (
    [QuoteItemID] bigint NOT NULL,
    [ProductID] bigint NOT NULL,
    CONSTRAINT [PK_QuotePartItems] PRIMARY KEY ([QuoteItemID])
);
GO

-- Loại con công sửa/kiểm tra.

CREATE TABLE dbo.[QuoteServiceItems] (
    [QuoteItemID] bigint NOT NULL,
    [ServiceID] bigint NOT NULL,
    CONSTRAINT [PK_QuoteServiceItems] PRIMARY KEY ([QuoteItemID])
);
GO

-- Lịch sử công việc kỹ thuật và thời gian; một phiếu có nhiều lần thực hiện.

CREATE TABLE dbo.[RepairWorkLogs] (
    [WorkLogID] bigint IDENTITY(1,1) NOT NULL,
    [RepairTicketID] bigint NOT NULL,
    [TechnicianID] bigint NOT NULL,
    [StartedAt] datetime2(3) NOT NULL DEFAULT (SYSUTCDATETIME()),
    [FinishedAt] datetime2(3) NULL,
    [WorkDescription] nvarchar(2000) NOT NULL,
    CONSTRAINT [PK_RepairWorkLogs] PRIMARY KEY ([WorkLogID]),
    CONSTRAINT [CK_RepairWorkLogs_1] CHECK ([FinishedAt] IS NULL OR [FinishedAt] >= [StartedAt])
);
GO

-- Lần sử dụng thực tế của dòng linh kiện đã duyệt; không lặp ProductID hoặc TicketID.

CREATE TABLE dbo.[RepairPartUsages] (
    [PartUsageID] bigint IDENTITY(1,1) NOT NULL,
    [QuoteItemID] bigint NOT NULL,
    [Quantity] int NOT NULL,
    [UsedBy] bigint NOT NULL,
    [UsedAt] datetime2(3) NOT NULL DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT [PK_RepairPartUsages] PRIMARY KEY ([PartUsageID]),
    CONSTRAINT [CK_RepairPartUsages_1] CHECK ([Quantity] > 0)
);
GO

-- Bút toán xuất linh kiện BULK cho lần sử dụng; có thể lấy nhiều vị trí kho.

CREATE TABLE dbo.[RepairBulkIssues] (
    [BulkEntryID] bigint NOT NULL,
    [PartUsageID] bigint NOT NULL,
    CONSTRAINT [PK_RepairBulkIssues] PRIMARY KEY ([BulkEntryID])
);
GO

-- Bút toán xuất linh kiện SERIALIZED cho lần sử dụng (SSD/RAM quản lý từng chiếc nếu cần).

CREATE TABLE dbo.[RepairDeviceIssues] (
    [DeviceEntryID] bigint NOT NULL,
    [PartUsageID] bigint NOT NULL,
    CONSTRAINT [PK_RepairDeviceIssues] PRIMARY KEY ([DeviceEntryID])
);
GO

-- Chính sách tái sử dụng; nội dung đã cam kết được chụp lại trên giấy bảo hành.

CREATE TABLE dbo.[WarrantyPolicies] (
    [PolicyID] bigint IDENTITY(1,1) NOT NULL,
    [PolicyCode] varchar(40) NOT NULL,
    [Name] nvarchar(180) NOT NULL,
    [Terms] nvarchar(2000) NOT NULL,
    [IsActive] bit NOT NULL DEFAULT (1),
    CONSTRAINT [PK_WarrantyPolicies] PRIMARY KEY ([PolicyID]),
    CONSTRAINT [UQ_WarrantyPolicies_1] UNIQUE ([PolicyCode])
);
GO

-- Chứng nhận bảo hành cho dòng bán hoặc dòng công/linh kiện sửa, có đúng một nguồn. Không lặp CustomerID/DeviceID suy ra từ nguồn.

CREATE TABLE dbo.[Warranties] (
    [WarrantyID] bigint IDENTITY(1,1) NOT NULL,
    [WarrantyCode] varchar(40) NOT NULL,
    [PolicyID] bigint NOT NULL,
    [StartAt] datetime2(3) NOT NULL,
    [EndAt] datetime2(3) NOT NULL,
    [TermsSnapshot] nvarchar(2000) NOT NULL,
    [Status] varchar(24) NOT NULL DEFAULT ('ACTIVE'),
    [TrackingTokenHash] binary(32) NOT NULL,
    CONSTRAINT [PK_Warranties] PRIMARY KEY ([WarrantyID]),
    CONSTRAINT [UQ_Warranties_1] UNIQUE ([WarrantyCode]),
    CONSTRAINT [UQ_Warranties_2] UNIQUE ([TrackingTokenHash]),
    CONSTRAINT [CK_Warranties_1] CHECK ([EndAt] > [StartAt]),
    CONSTRAINT [CK_Warranties_2] CHECK ([Status] IN ('ACTIVE', 'VOID'))
);
GO

-- Nguồn bảo hành dòng bán; tối đa một giấy trên một dòng. Hàng BULK được xác minh bằng đơn và số lượng.

CREATE TABLE dbo.[SaleWarranties] (
    [WarrantyID] bigint NOT NULL,
    [SaleLineID] bigint NOT NULL,
    CONSTRAINT [PK_SaleWarranties] PRIMARY KEY ([WarrantyID]),
    CONSTRAINT [UQ_SaleWarranties_1] UNIQUE ([SaleLineID])
);
GO

-- Nguồn bảo hành dòng báo giá đã duyệt và hoàn thành.

CREATE TABLE dbo.[RepairWarranties] (
    [WarrantyID] bigint NOT NULL,
    [QuoteItemID] bigint NOT NULL,
    CONSTRAINT [PK_RepairWarranties] PRIMARY KEY ([WarrantyID]),
    CONSTRAINT [UQ_RepairWarranties_1] UNIQUE ([QuoteItemID])
);
GO

-- Yêu cầu bảo hành; mỗi lần yêu cầu riêng, có thể liên kết phiếu sửa mới.

CREATE TABLE dbo.[WarrantyClaims] (
    [ClaimID] bigint IDENTITY(1,1) NOT NULL,
    [ClaimCode] varchar(40) NOT NULL,
    [WarrantyID] bigint NOT NULL,
    [ClaimQuantity] int NOT NULL,
    [ClaimedAt] datetime2(3) NOT NULL DEFAULT (SYSUTCDATETIME()),
    [ReportedProblem] nvarchar(2000) NOT NULL,
    [Status] varchar(24) NOT NULL DEFAULT ('OPEN'),
    [Resolution] nvarchar(2000) NULL,
    CONSTRAINT [PK_WarrantyClaims] PRIMARY KEY ([ClaimID]),
    CONSTRAINT [UQ_WarrantyClaims_1] UNIQUE ([ClaimCode]),
    CONSTRAINT [CK_WarrantyClaims_1] CHECK ([ClaimQuantity] > 0),
    CONSTRAINT [CK_WarrantyClaims_2] CHECK ([Status] IN ('OPEN', 'INSPECTING', 'APPROVED', 'REJECTED', 'RESOLVED'))
);
GO

-- Liên kết một yêu cầu bảo hành với một phiếu sửa thực hiện bảo hành.

CREATE TABLE dbo.[WarrantyClaimRepairs] (
    [ClaimID] bigint NOT NULL,
    [RepairTicketID] bigint NOT NULL,
    CONSTRAINT [PK_WarrantyClaimRepairs] PRIMARY KEY ([ClaimID]),
    CONSTRAINT [UQ_WarrantyClaimRepairs_1] UNIQUE ([RepairTicketID])
);
GO

-- Một lần thu/hoàn cho đúng một đơn bán HOẶC một phiếu sửa. Hỗ trợ đặt cọc và thanh toán nhiều lần.

CREATE TABLE dbo.[Payments] (
    [PaymentID] bigint IDENTITY(1,1) NOT NULL,
    [PaymentCode] varchar(40) NOT NULL,
    [OrderID] bigint NULL,
    [RepairTicketID] bigint NULL,
    [Direction] varchar(24) NOT NULL,
    [Amount] decimal(19,2) NOT NULL,
    [Method] varchar(24) NOT NULL,
    [Status] varchar(24) NOT NULL DEFAULT ('PENDING'),
    [ProviderReference] varchar(150) NULL,
    [IdempotencyKey] varchar(100) NOT NULL,
    [OccurredAt] datetime2(3) NOT NULL DEFAULT (SYSUTCDATETIME()),
    [RecordedBy] bigint NULL,
    CONSTRAINT [PK_Payments] PRIMARY KEY ([PaymentID]),
    CONSTRAINT [UQ_Payments_1] UNIQUE ([PaymentCode]),
    CONSTRAINT [UQ_Payments_2] UNIQUE ([IdempotencyKey]),
    CONSTRAINT [CK_Payments_1] CHECK ([Amount] > 0),
    CONSTRAINT [CK_Payments_2] CHECK (([OrderID] IS NOT NULL AND [RepairTicketID] IS NULL) OR ([OrderID] IS NULL AND [RepairTicketID] IS NOT NULL)),
    CONSTRAINT [CK_Payments_3] CHECK ([Direction] IN ('IN', 'OUT')),
    CONSTRAINT [CK_Payments_4] CHECK ([Amount] >= 0),
    CONSTRAINT [CK_Payments_5] CHECK ([Method] IN ('CASH', 'BANK_TRANSFER', 'GATEWAY')),
    CONSTRAINT [CK_Payments_6] CHECK ([Status] IN ('PENDING', 'SUCCEEDED', 'FAILED'))
);
GO

-- Mỗi lần hoàn tham chiếu một lần thu gốc; tổng hoàn không được vượt thu.

CREATE TABLE dbo.[PaymentRefunds] (
    [RefundPaymentID] bigint NOT NULL,
    [OriginalPaymentID] bigint NOT NULL,
    CONSTRAINT [PK_PaymentRefunds] PRIMARY KEY ([RefundPaymentID]),
    CONSTRAINT [CK_PaymentRefunds_1] CHECK ([RefundPaymentID] <> [OriginalPaymentID])
);
GO

-- Phân loại chi phí vận hành; không ghi trùng giá nhập hàng vào chi phí để tính lãi.

CREATE TABLE dbo.[ExpenseCategories] (
    [ExpenseCategoryID] bigint IDENTITY(1,1) NOT NULL,
    [ExpenseCategoryCode] varchar(40) NOT NULL,
    [Name] nvarchar(180) NOT NULL,
    CONSTRAINT [PK_ExpenseCategories] PRIMARY KEY ([ExpenseCategoryID]),
    CONSTRAINT [UQ_ExpenseCategories_1] UNIQUE ([ExpenseCategoryCode])
);
GO

-- Chi phí điện nước, vận chuyển, thuê mặt bằng… ngoài sổ giá vốn nhập/xuất.

CREATE TABLE dbo.[Expenses] (
    [ExpenseID] bigint IDENTITY(1,1) NOT NULL,
    [ExpenseCode] varchar(40) NOT NULL,
    [ExpenseCategoryID] bigint NOT NULL,
    [Amount] decimal(19,2) NOT NULL,
    [OccurredAt] datetime2(3) NOT NULL DEFAULT (SYSUTCDATETIME()),
    [Description] nvarchar(2000) NOT NULL,
    [RecordedBy] bigint NOT NULL,
    CONSTRAINT [PK_Expenses] PRIMARY KEY ([ExpenseID]),
    CONSTRAINT [UQ_Expenses_1] UNIQUE ([ExpenseCode]),
    CONSTRAINT [CK_Expenses_1] CHECK ([Amount] > 0),
    CONSTRAINT [CK_Expenses_2] CHECK ([Amount] >= 0)
);
GO

-- Nhật ký thao tác append-only. EntityType/EntityKey là nhãn bằng chứng, KHÔNG phải liên kết nghiệp vụ hoặc FK đa hình.

CREATE TABLE dbo.[AuditLogs] (
    [AuditID] bigint IDENTITY(1,1) NOT NULL,
    [ActorID] bigint NULL,
    [Action] varchar(60) NOT NULL,
    [EntityType] varchar(60) NOT NULL,
    [EntityKey] varchar(100) NOT NULL,
    [Summary] nvarchar(2000) NOT NULL,
    [OccurredAt] datetime2(3) NOT NULL DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT [PK_AuditLogs] PRIMARY KEY ([AuditID])
);
GO

ALTER TABLE dbo.[UserRoles] ADD CONSTRAINT [FK_UserRoles_UserID] FOREIGN KEY ([UserID]) REFERENCES dbo.[Users] ([UserID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE dbo.[UserRoles] ADD CONSTRAINT [FK_UserRoles_RoleID] FOREIGN KEY ([RoleID]) REFERENCES dbo.[Roles] ([RoleID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX [IX_UserRoles_RoleID] ON dbo.[UserRoles] ([RoleID]);

ALTER TABLE dbo.[RolePermissions] ADD CONSTRAINT [FK_RolePermissions_RoleID] FOREIGN KEY ([RoleID]) REFERENCES dbo.[Roles] ([RoleID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE dbo.[RolePermissions] ADD CONSTRAINT [FK_RolePermissions_PermissionID] FOREIGN KEY ([PermissionID]) REFERENCES dbo.[Permissions] ([PermissionID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX [IX_RolePermissions_PermissionID] ON dbo.[RolePermissions] ([PermissionID]);

ALTER TABLE dbo.[Customers] ADD CONSTRAINT [FK_Customers_UserID] FOREIGN KEY ([UserID]) REFERENCES dbo.[Users] ([UserID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE UNIQUE INDEX [UX_Customers_1] ON dbo.[Customers] ([UserID]) WHERE [UserID] IS NOT NULL;

CREATE INDEX [IX_Customers_UserID] ON dbo.[Customers] ([UserID]);

ALTER TABLE dbo.[CustomerAddresses] ADD CONSTRAINT [FK_CustomerAddresses_CustomerID] FOREIGN KEY ([CustomerID]) REFERENCES dbo.[Customers] ([CustomerID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE UNIQUE INDEX [UX_CustomerAddresses_1] ON dbo.[CustomerAddresses] ([CustomerID]) WHERE [IsDefault] = 1;

CREATE INDEX [IX_CustomerAddresses_CustomerID] ON dbo.[CustomerAddresses] ([CustomerID]);

ALTER TABLE dbo.[Categories] ADD CONSTRAINT [FK_Categories_ParentCategoryID] FOREIGN KEY ([ParentCategoryID]) REFERENCES dbo.[Categories] ([CategoryID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX [IX_Categories_ParentCategoryID] ON dbo.[Categories] ([ParentCategoryID]);

ALTER TABLE dbo.[Products] ADD CONSTRAINT [FK_Products_CategoryID] FOREIGN KEY ([CategoryID]) REFERENCES dbo.[Categories] ([CategoryID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE dbo.[Products] ADD CONSTRAINT [FK_Products_BrandID] FOREIGN KEY ([BrandID]) REFERENCES dbo.[Brands] ([BrandID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE dbo.[Products] ADD CONSTRAINT [FK_Products_UnitID] FOREIGN KEY ([UnitID]) REFERENCES dbo.[Units] ([UnitID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX [IX_Products_CategoryID] ON dbo.[Products] ([CategoryID]);

CREATE INDEX [IX_Products_BrandID] ON dbo.[Products] ([BrandID]);

CREATE INDEX [IX_Products_UnitID] ON dbo.[Products] ([UnitID]);

ALTER TABLE dbo.[ProductImages] ADD CONSTRAINT [FK_ProductImages_ProductID] FOREIGN KEY ([ProductID]) REFERENCES dbo.[Products] ([ProductID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE dbo.[ProductAttributeValues] ADD CONSTRAINT [FK_ProductAttributeValues_ProductID] FOREIGN KEY ([ProductID]) REFERENCES dbo.[Products] ([ProductID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE dbo.[ProductAttributeValues] ADD CONSTRAINT [FK_ProductAttributeValues_AttributeID] FOREIGN KEY ([AttributeID]) REFERENCES dbo.[AttributeDefinitions] ([AttributeID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX [IX_ProductAttributeValues_AttributeID] ON dbo.[ProductAttributeValues] ([AttributeID]);

ALTER TABLE dbo.[Devices] ADD CONSTRAINT [FK_Devices_ProductID] FOREIGN KEY ([ProductID]) REFERENCES dbo.[Products] ([ProductID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE dbo.[Devices] ADD CONSTRAINT [FK_Devices_OwnerCustomerID] FOREIGN KEY ([OwnerCustomerID]) REFERENCES dbo.[Customers] ([CustomerID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX [IX_Devices_ProductID] ON dbo.[Devices] ([ProductID]);

CREATE INDEX [IX_Devices_OwnerCustomerID] ON dbo.[Devices] ([OwnerCustomerID]);

ALTER TABLE dbo.[DeviceImages] ADD CONSTRAINT [FK_DeviceImages_DeviceID] FOREIGN KEY ([DeviceID]) REFERENCES dbo.[Devices] ([DeviceID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE dbo.[DeviceChecks] ADD CONSTRAINT [FK_DeviceChecks_DeviceID] FOREIGN KEY ([DeviceID]) REFERENCES dbo.[Devices] ([DeviceID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE dbo.[DeviceChecks] ADD CONSTRAINT [FK_DeviceChecks_CheckedBy] FOREIGN KEY ([CheckedBy]) REFERENCES dbo.[Users] ([UserID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX [IX_DeviceChecks_DeviceID] ON dbo.[DeviceChecks] ([DeviceID]);

CREATE INDEX [IX_DeviceChecks_CheckedBy] ON dbo.[DeviceChecks] ([CheckedBy]);

ALTER TABLE dbo.[InventoryDocuments] ADD CONSTRAINT [FK_InventoryDocuments_CreatedBy] FOREIGN KEY ([CreatedBy]) REFERENCES dbo.[Users] ([UserID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX [IX_InventoryDocuments_CreatedBy] ON dbo.[InventoryDocuments] ([CreatedBy]);

ALTER TABLE dbo.[PurchaseReceipts] ADD CONSTRAINT [FK_PurchaseReceipts_InventoryDocID] FOREIGN KEY ([InventoryDocID]) REFERENCES dbo.[InventoryDocuments] ([InventoryDocID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE dbo.[PurchaseReceipts] ADD CONSTRAINT [FK_PurchaseReceipts_SupplierID] FOREIGN KEY ([SupplierID]) REFERENCES dbo.[Suppliers] ([SupplierID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX [IX_PurchaseReceipts_SupplierID] ON dbo.[PurchaseReceipts] ([SupplierID]);

ALTER TABLE dbo.[BulkStockEntries] ADD CONSTRAINT [FK_BulkStockEntries_InventoryDocID] FOREIGN KEY ([InventoryDocID]) REFERENCES dbo.[InventoryDocuments] ([InventoryDocID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE dbo.[BulkStockEntries] ADD CONSTRAINT [FK_BulkStockEntries_ProductID] FOREIGN KEY ([ProductID]) REFERENCES dbo.[Products] ([ProductID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE dbo.[BulkStockEntries] ADD CONSTRAINT [FK_BulkStockEntries_LocationID] FOREIGN KEY ([LocationID]) REFERENCES dbo.[StockLocations] ([LocationID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX [IX_BulkStockEntries_InventoryDocID] ON dbo.[BulkStockEntries] ([InventoryDocID]);

CREATE INDEX [IX_BulkStockEntries_ProductID] ON dbo.[BulkStockEntries] ([ProductID]);

CREATE INDEX [IX_BulkStockEntries_LocationID] ON dbo.[BulkStockEntries] ([LocationID]);

ALTER TABLE dbo.[DeviceStockEntries] ADD CONSTRAINT [FK_DeviceStockEntries_InventoryDocID] FOREIGN KEY ([InventoryDocID]) REFERENCES dbo.[InventoryDocuments] ([InventoryDocID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE dbo.[DeviceStockEntries] ADD CONSTRAINT [FK_DeviceStockEntries_DeviceID] FOREIGN KEY ([DeviceID]) REFERENCES dbo.[Devices] ([DeviceID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE dbo.[DeviceStockEntries] ADD CONSTRAINT [FK_DeviceStockEntries_FromLocationID] FOREIGN KEY ([FromLocationID]) REFERENCES dbo.[StockLocations] ([LocationID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE dbo.[DeviceStockEntries] ADD CONSTRAINT [FK_DeviceStockEntries_ToLocationID] FOREIGN KEY ([ToLocationID]) REFERENCES dbo.[StockLocations] ([LocationID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX [IX_DeviceStockEntries_DeviceID] ON dbo.[DeviceStockEntries] ([DeviceID]);

CREATE INDEX [IX_DeviceStockEntries_FromLocationID] ON dbo.[DeviceStockEntries] ([FromLocationID]);

CREATE INDEX [IX_DeviceStockEntries_ToLocationID] ON dbo.[DeviceStockEntries] ([ToLocationID]);

ALTER TABLE dbo.[InventoryReversals] ADD CONSTRAINT [FK_InventoryReversals_OriginalDocID] FOREIGN KEY ([OriginalDocID]) REFERENCES dbo.[InventoryDocuments] ([InventoryDocID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE dbo.[InventoryReversals] ADD CONSTRAINT [FK_InventoryReversals_ReversalDocID] FOREIGN KEY ([ReversalDocID]) REFERENCES dbo.[InventoryDocuments] ([InventoryDocID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE dbo.[SalesOrders] ADD CONSTRAINT [FK_SalesOrders_CustomerID] FOREIGN KEY ([CustomerID]) REFERENCES dbo.[Customers] ([CustomerID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE dbo.[SalesOrders] ADD CONSTRAINT [FK_SalesOrders_CreatedBy] FOREIGN KEY ([CreatedBy]) REFERENCES dbo.[Users] ([UserID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX [IX_SalesOrders_CustomerID] ON dbo.[SalesOrders] ([CustomerID]);

CREATE INDEX [IX_SalesOrders_CreatedBy] ON dbo.[SalesOrders] ([CreatedBy]);

ALTER TABLE dbo.[OrderShipping] ADD CONSTRAINT [FK_OrderShipping_OrderID] FOREIGN KEY ([OrderID]) REFERENCES dbo.[SalesOrders] ([OrderID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE dbo.[SalesLines] ADD CONSTRAINT [FK_SalesLines_OrderID] FOREIGN KEY ([OrderID]) REFERENCES dbo.[SalesOrders] ([OrderID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE dbo.[BulkSaleLines] ADD CONSTRAINT [FK_BulkSaleLines_SaleLineID] FOREIGN KEY ([SaleLineID]) REFERENCES dbo.[SalesLines] ([SaleLineID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE dbo.[BulkSaleLines] ADD CONSTRAINT [FK_BulkSaleLines_ProductID] FOREIGN KEY ([ProductID]) REFERENCES dbo.[Products] ([ProductID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX [IX_BulkSaleLines_ProductID] ON dbo.[BulkSaleLines] ([ProductID]);

ALTER TABLE dbo.[DeviceSaleLines] ADD CONSTRAINT [FK_DeviceSaleLines_SaleLineID] FOREIGN KEY ([SaleLineID]) REFERENCES dbo.[SalesLines] ([SaleLineID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE dbo.[DeviceSaleLines] ADD CONSTRAINT [FK_DeviceSaleLines_DeviceID] FOREIGN KEY ([DeviceID]) REFERENCES dbo.[Devices] ([DeviceID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX [IX_DeviceSaleLines_DeviceID] ON dbo.[DeviceSaleLines] ([DeviceID]);

ALTER TABLE dbo.[BulkReservations] ADD CONSTRAINT [FK_BulkReservations_SaleLineID] FOREIGN KEY ([SaleLineID]) REFERENCES dbo.[BulkSaleLines] ([SaleLineID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE dbo.[BulkReservations] ADD CONSTRAINT [FK_BulkReservations_LocationID] FOREIGN KEY ([LocationID]) REFERENCES dbo.[StockLocations] ([LocationID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX [IX_BulkReservations_SaleLineID] ON dbo.[BulkReservations] ([SaleLineID]);

CREATE INDEX [IX_BulkReservations_LocationID] ON dbo.[BulkReservations] ([LocationID]);

ALTER TABLE dbo.[DeviceReservations] ADD CONSTRAINT [FK_DeviceReservations_SaleLineID] FOREIGN KEY ([SaleLineID]) REFERENCES dbo.[DeviceSaleLines] ([SaleLineID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE UNIQUE INDEX [UX_DeviceReservations_1] ON dbo.[DeviceReservations] ([SaleLineID]) WHERE [Status] = 'ACTIVE';

CREATE INDEX [IX_DeviceReservations_SaleLineID] ON dbo.[DeviceReservations] ([SaleLineID]);

ALTER TABLE dbo.[ActiveDeviceReservations] ADD CONSTRAINT [FK_ActiveDeviceReservations_DeviceID] FOREIGN KEY ([DeviceID]) REFERENCES dbo.[Devices] ([DeviceID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE dbo.[ActiveDeviceReservations] ADD CONSTRAINT [FK_ActiveDeviceReservations_ReservationID] FOREIGN KEY ([ReservationID]) REFERENCES dbo.[DeviceReservations] ([ReservationID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE dbo.[SalesDeliveries] ADD CONSTRAINT [FK_SalesDeliveries_InventoryDocID] FOREIGN KEY ([InventoryDocID]) REFERENCES dbo.[InventoryDocuments] ([InventoryDocID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE dbo.[SalesDeliveries] ADD CONSTRAINT [FK_SalesDeliveries_OrderID] FOREIGN KEY ([OrderID]) REFERENCES dbo.[SalesOrders] ([OrderID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX [IX_SalesDeliveries_OrderID] ON dbo.[SalesDeliveries] ([OrderID]);

ALTER TABLE dbo.[SaleBulkIssues] ADD CONSTRAINT [FK_SaleBulkIssues_BulkEntryID] FOREIGN KEY ([BulkEntryID]) REFERENCES dbo.[BulkStockEntries] ([BulkEntryID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE dbo.[SaleBulkIssues] ADD CONSTRAINT [FK_SaleBulkIssues_SaleLineID] FOREIGN KEY ([SaleLineID]) REFERENCES dbo.[BulkSaleLines] ([SaleLineID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX [IX_SaleBulkIssues_SaleLineID] ON dbo.[SaleBulkIssues] ([SaleLineID]);

ALTER TABLE dbo.[SaleDeviceIssues] ADD CONSTRAINT [FK_SaleDeviceIssues_DeviceEntryID] FOREIGN KEY ([DeviceEntryID]) REFERENCES dbo.[DeviceStockEntries] ([DeviceEntryID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE dbo.[SaleDeviceIssues] ADD CONSTRAINT [FK_SaleDeviceIssues_SaleLineID] FOREIGN KEY ([SaleLineID]) REFERENCES dbo.[DeviceSaleLines] ([SaleLineID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX [IX_SaleDeviceIssues_SaleLineID] ON dbo.[SaleDeviceIssues] ([SaleLineID]);

ALTER TABLE dbo.[SalesReturns] ADD CONSTRAINT [FK_SalesReturns_InventoryDocID] FOREIGN KEY ([InventoryDocID]) REFERENCES dbo.[InventoryDocuments] ([InventoryDocID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE dbo.[SalesReturns] ADD CONSTRAINT [FK_SalesReturns_OrderID] FOREIGN KEY ([OrderID]) REFERENCES dbo.[SalesOrders] ([OrderID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX [IX_SalesReturns_OrderID] ON dbo.[SalesReturns] ([OrderID]);

ALTER TABLE dbo.[ReturnBulkEntries] ADD CONSTRAINT [FK_ReturnBulkEntries_BulkEntryID] FOREIGN KEY ([BulkEntryID]) REFERENCES dbo.[BulkStockEntries] ([BulkEntryID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE dbo.[ReturnBulkEntries] ADD CONSTRAINT [FK_ReturnBulkEntries_SaleLineID] FOREIGN KEY ([SaleLineID]) REFERENCES dbo.[BulkSaleLines] ([SaleLineID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX [IX_ReturnBulkEntries_SaleLineID] ON dbo.[ReturnBulkEntries] ([SaleLineID]);

ALTER TABLE dbo.[ReturnDeviceEntries] ADD CONSTRAINT [FK_ReturnDeviceEntries_DeviceEntryID] FOREIGN KEY ([DeviceEntryID]) REFERENCES dbo.[DeviceStockEntries] ([DeviceEntryID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE dbo.[ReturnDeviceEntries] ADD CONSTRAINT [FK_ReturnDeviceEntries_SaleLineID] FOREIGN KEY ([SaleLineID]) REFERENCES dbo.[DeviceSaleLines] ([SaleLineID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX [IX_ReturnDeviceEntries_SaleLineID] ON dbo.[ReturnDeviceEntries] ([SaleLineID]);

ALTER TABLE dbo.[RepairRequests] ADD CONSTRAINT [FK_RepairRequests_CustomerID] FOREIGN KEY ([CustomerID]) REFERENCES dbo.[Customers] ([CustomerID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX [IX_RepairRequests_CustomerID] ON dbo.[RepairRequests] ([CustomerID]);

ALTER TABLE dbo.[RepairTickets] ADD CONSTRAINT [FK_RepairTickets_DeviceID] FOREIGN KEY ([DeviceID]) REFERENCES dbo.[Devices] ([DeviceID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE dbo.[RepairTickets] ADD CONSTRAINT [FK_RepairTickets_CustomerID] FOREIGN KEY ([CustomerID]) REFERENCES dbo.[Customers] ([CustomerID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE dbo.[RepairTickets] ADD CONSTRAINT [FK_RepairTickets_RequestID] FOREIGN KEY ([RequestID]) REFERENCES dbo.[RepairRequests] ([RequestID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE dbo.[RepairTickets] ADD CONSTRAINT [FK_RepairTickets_AssignedTechnicianID] FOREIGN KEY ([AssignedTechnicianID]) REFERENCES dbo.[Users] ([UserID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE dbo.[RepairTickets] ADD CONSTRAINT [FK_RepairTickets_ReceivedBy] FOREIGN KEY ([ReceivedBy]) REFERENCES dbo.[Users] ([UserID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE UNIQUE INDEX [UX_RepairTickets_1] ON dbo.[RepairTickets] ([RequestID]) WHERE [RequestID] IS NOT NULL;

CREATE INDEX [IX_RepairTickets_DeviceID] ON dbo.[RepairTickets] ([DeviceID]);

CREATE INDEX [IX_RepairTickets_CustomerID] ON dbo.[RepairTickets] ([CustomerID]);

CREATE INDEX [IX_RepairTickets_RequestID] ON dbo.[RepairTickets] ([RequestID]);

CREATE INDEX [IX_RepairTickets_AssignedTechnicianID] ON dbo.[RepairTickets] ([AssignedTechnicianID]);

CREATE INDEX [IX_RepairTickets_ReceivedBy] ON dbo.[RepairTickets] ([ReceivedBy]);

ALTER TABLE dbo.[RepairAccessories] ADD CONSTRAINT [FK_RepairAccessories_RepairTicketID] FOREIGN KEY ([RepairTicketID]) REFERENCES dbo.[RepairTickets] ([RepairTicketID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE dbo.[RepairEvents] ADD CONSTRAINT [FK_RepairEvents_RepairTicketID] FOREIGN KEY ([RepairTicketID]) REFERENCES dbo.[RepairTickets] ([RepairTicketID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE dbo.[RepairEvents] ADD CONSTRAINT [FK_RepairEvents_ActorID] FOREIGN KEY ([ActorID]) REFERENCES dbo.[Users] ([UserID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX [IX_RepairEvents_RepairTicketID] ON dbo.[RepairEvents] ([RepairTicketID]);

CREATE INDEX [IX_RepairEvents_ActorID] ON dbo.[RepairEvents] ([ActorID]);

ALTER TABLE dbo.[RepairQuotes] ADD CONSTRAINT [FK_RepairQuotes_RepairTicketID] FOREIGN KEY ([RepairTicketID]) REFERENCES dbo.[RepairTickets] ([RepairTicketID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE dbo.[RepairQuotes] ADD CONSTRAINT [FK_RepairQuotes_CreatedBy] FOREIGN KEY ([CreatedBy]) REFERENCES dbo.[Users] ([UserID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX [IX_RepairQuotes_CreatedBy] ON dbo.[RepairQuotes] ([CreatedBy]);

ALTER TABLE dbo.[QuoteItems] ADD CONSTRAINT [FK_QuoteItems_QuoteID] FOREIGN KEY ([QuoteID]) REFERENCES dbo.[RepairQuotes] ([QuoteID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE dbo.[QuotePartItems] ADD CONSTRAINT [FK_QuotePartItems_QuoteItemID] FOREIGN KEY ([QuoteItemID]) REFERENCES dbo.[QuoteItems] ([QuoteItemID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE dbo.[QuotePartItems] ADD CONSTRAINT [FK_QuotePartItems_ProductID] FOREIGN KEY ([ProductID]) REFERENCES dbo.[Products] ([ProductID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX [IX_QuotePartItems_ProductID] ON dbo.[QuotePartItems] ([ProductID]);

ALTER TABLE dbo.[QuoteServiceItems] ADD CONSTRAINT [FK_QuoteServiceItems_QuoteItemID] FOREIGN KEY ([QuoteItemID]) REFERENCES dbo.[QuoteItems] ([QuoteItemID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE dbo.[QuoteServiceItems] ADD CONSTRAINT [FK_QuoteServiceItems_ServiceID] FOREIGN KEY ([ServiceID]) REFERENCES dbo.[Services] ([ServiceID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX [IX_QuoteServiceItems_ServiceID] ON dbo.[QuoteServiceItems] ([ServiceID]);

ALTER TABLE dbo.[RepairWorkLogs] ADD CONSTRAINT [FK_RepairWorkLogs_RepairTicketID] FOREIGN KEY ([RepairTicketID]) REFERENCES dbo.[RepairTickets] ([RepairTicketID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE dbo.[RepairWorkLogs] ADD CONSTRAINT [FK_RepairWorkLogs_TechnicianID] FOREIGN KEY ([TechnicianID]) REFERENCES dbo.[Users] ([UserID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX [IX_RepairWorkLogs_RepairTicketID] ON dbo.[RepairWorkLogs] ([RepairTicketID]);

CREATE INDEX [IX_RepairWorkLogs_TechnicianID] ON dbo.[RepairWorkLogs] ([TechnicianID]);

ALTER TABLE dbo.[RepairPartUsages] ADD CONSTRAINT [FK_RepairPartUsages_QuoteItemID] FOREIGN KEY ([QuoteItemID]) REFERENCES dbo.[QuotePartItems] ([QuoteItemID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE dbo.[RepairPartUsages] ADD CONSTRAINT [FK_RepairPartUsages_UsedBy] FOREIGN KEY ([UsedBy]) REFERENCES dbo.[Users] ([UserID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX [IX_RepairPartUsages_QuoteItemID] ON dbo.[RepairPartUsages] ([QuoteItemID]);

CREATE INDEX [IX_RepairPartUsages_UsedBy] ON dbo.[RepairPartUsages] ([UsedBy]);

ALTER TABLE dbo.[RepairBulkIssues] ADD CONSTRAINT [FK_RepairBulkIssues_BulkEntryID] FOREIGN KEY ([BulkEntryID]) REFERENCES dbo.[BulkStockEntries] ([BulkEntryID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE dbo.[RepairBulkIssues] ADD CONSTRAINT [FK_RepairBulkIssues_PartUsageID] FOREIGN KEY ([PartUsageID]) REFERENCES dbo.[RepairPartUsages] ([PartUsageID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX [IX_RepairBulkIssues_PartUsageID] ON dbo.[RepairBulkIssues] ([PartUsageID]);

ALTER TABLE dbo.[RepairDeviceIssues] ADD CONSTRAINT [FK_RepairDeviceIssues_DeviceEntryID] FOREIGN KEY ([DeviceEntryID]) REFERENCES dbo.[DeviceStockEntries] ([DeviceEntryID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE dbo.[RepairDeviceIssues] ADD CONSTRAINT [FK_RepairDeviceIssues_PartUsageID] FOREIGN KEY ([PartUsageID]) REFERENCES dbo.[RepairPartUsages] ([PartUsageID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX [IX_RepairDeviceIssues_PartUsageID] ON dbo.[RepairDeviceIssues] ([PartUsageID]);

ALTER TABLE dbo.[Warranties] ADD CONSTRAINT [FK_Warranties_PolicyID] FOREIGN KEY ([PolicyID]) REFERENCES dbo.[WarrantyPolicies] ([PolicyID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX [IX_Warranties_PolicyID] ON dbo.[Warranties] ([PolicyID]);

ALTER TABLE dbo.[SaleWarranties] ADD CONSTRAINT [FK_SaleWarranties_WarrantyID] FOREIGN KEY ([WarrantyID]) REFERENCES dbo.[Warranties] ([WarrantyID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE dbo.[SaleWarranties] ADD CONSTRAINT [FK_SaleWarranties_SaleLineID] FOREIGN KEY ([SaleLineID]) REFERENCES dbo.[SalesLines] ([SaleLineID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE dbo.[RepairWarranties] ADD CONSTRAINT [FK_RepairWarranties_WarrantyID] FOREIGN KEY ([WarrantyID]) REFERENCES dbo.[Warranties] ([WarrantyID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE dbo.[RepairWarranties] ADD CONSTRAINT [FK_RepairWarranties_QuoteItemID] FOREIGN KEY ([QuoteItemID]) REFERENCES dbo.[QuoteItems] ([QuoteItemID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE dbo.[WarrantyClaims] ADD CONSTRAINT [FK_WarrantyClaims_WarrantyID] FOREIGN KEY ([WarrantyID]) REFERENCES dbo.[Warranties] ([WarrantyID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX [IX_WarrantyClaims_WarrantyID] ON dbo.[WarrantyClaims] ([WarrantyID]);

ALTER TABLE dbo.[WarrantyClaimRepairs] ADD CONSTRAINT [FK_WarrantyClaimRepairs_ClaimID] FOREIGN KEY ([ClaimID]) REFERENCES dbo.[WarrantyClaims] ([ClaimID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE dbo.[WarrantyClaimRepairs] ADD CONSTRAINT [FK_WarrantyClaimRepairs_RepairTicketID] FOREIGN KEY ([RepairTicketID]) REFERENCES dbo.[RepairTickets] ([RepairTicketID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE dbo.[Payments] ADD CONSTRAINT [FK_Payments_OrderID] FOREIGN KEY ([OrderID]) REFERENCES dbo.[SalesOrders] ([OrderID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE dbo.[Payments] ADD CONSTRAINT [FK_Payments_RepairTicketID] FOREIGN KEY ([RepairTicketID]) REFERENCES dbo.[RepairTickets] ([RepairTicketID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE dbo.[Payments] ADD CONSTRAINT [FK_Payments_RecordedBy] FOREIGN KEY ([RecordedBy]) REFERENCES dbo.[Users] ([UserID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE UNIQUE INDEX [UX_Payments_1] ON dbo.[Payments] ([ProviderReference]) WHERE [ProviderReference] IS NOT NULL;

CREATE INDEX [IX_Payments_OrderID] ON dbo.[Payments] ([OrderID]);

CREATE INDEX [IX_Payments_RepairTicketID] ON dbo.[Payments] ([RepairTicketID]);

CREATE INDEX [IX_Payments_RecordedBy] ON dbo.[Payments] ([RecordedBy]);

ALTER TABLE dbo.[PaymentRefunds] ADD CONSTRAINT [FK_PaymentRefunds_RefundPaymentID] FOREIGN KEY ([RefundPaymentID]) REFERENCES dbo.[Payments] ([PaymentID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE dbo.[PaymentRefunds] ADD CONSTRAINT [FK_PaymentRefunds_OriginalPaymentID] FOREIGN KEY ([OriginalPaymentID]) REFERENCES dbo.[Payments] ([PaymentID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX [IX_PaymentRefunds_OriginalPaymentID] ON dbo.[PaymentRefunds] ([OriginalPaymentID]);

ALTER TABLE dbo.[Expenses] ADD CONSTRAINT [FK_Expenses_ExpenseCategoryID] FOREIGN KEY ([ExpenseCategoryID]) REFERENCES dbo.[ExpenseCategories] ([ExpenseCategoryID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE dbo.[Expenses] ADD CONSTRAINT [FK_Expenses_RecordedBy] FOREIGN KEY ([RecordedBy]) REFERENCES dbo.[Users] ([UserID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX [IX_Expenses_ExpenseCategoryID] ON dbo.[Expenses] ([ExpenseCategoryID]);

CREATE INDEX [IX_Expenses_RecordedBy] ON dbo.[Expenses] ([RecordedBy]);

ALTER TABLE dbo.[AuditLogs] ADD CONSTRAINT [FK_AuditLogs_ActorID] FOREIGN KEY ([ActorID]) REFERENCES dbo.[Users] ([UserID]) ON DELETE NO ACTION ON UPDATE NO ACTION;

CREATE INDEX [IX_AuditLogs_ActorID] ON dbo.[AuditLogs] ([ActorID]);

GO

-- Các view tính toán, không phải dữ liệu nguồn.

CREATE VIEW dbo.vBulkOnHand AS
SELECT e.ProductID,e.LocationID,SUM(CAST(e.Delta AS bigint)) AS OnHand
FROM dbo.BulkStockEntries e JOIN dbo.InventoryDocuments d ON d.InventoryDocID=e.InventoryDocID
WHERE d.Status='POSTED' GROUP BY e.ProductID,e.LocationID;
GO
CREATE VIEW dbo.vBulkAvailable AS
WITH r AS (
 SELECT l.ProductID,r.LocationID,SUM(CAST(r.Quantity AS bigint)) Reserved
 FROM dbo.BulkReservations r JOIN dbo.BulkSaleLines l ON l.SaleLineID=r.SaleLineID
 WHERE r.Status='ACTIVE' AND r.ExpiresAt>SYSUTCDATETIME()
 GROUP BY l.ProductID,r.LocationID
), k AS (
 SELECT ProductID,LocationID FROM dbo.vBulkOnHand
 UNION SELECT ProductID,LocationID FROM r
)
SELECT k.ProductID,k.LocationID,COALESCE(h.OnHand,0) OnHand,
 COALESCE(r.Reserved,0) Reserved,COALESCE(h.OnHand,0)-COALESCE(r.Reserved,0) Available
FROM k LEFT JOIN dbo.vBulkOnHand h ON h.ProductID=k.ProductID AND h.LocationID=k.LocationID
LEFT JOIN r ON r.ProductID=k.ProductID AND r.LocationID=k.LocationID;
GO
CREATE VIEW dbo.vSaleLineAmounts AS
SELECT s.SaleLineID,s.OrderID,COALESCE(b.Quantity,1) Quantity,
 CAST(COALESCE(b.Quantity,1)*s.UnitPrice-s.DiscountAmount AS decimal(19,2)) NetAmount,
 CAST(ROUND((COALESCE(b.Quantity,1)*s.UnitPrice-s.DiscountAmount)*s.TaxRate/100,2) AS decimal(19,2)) TaxAmount
FROM dbo.SalesLines s LEFT JOIN dbo.BulkSaleLines b ON b.SaleLineID=s.SaleLineID;
GO
CREATE VIEW dbo.vPaymentBalances AS
SELECT OrderID,RepairTicketID,
 SUM(CASE WHEN Direction='IN' THEN Amount ELSE -Amount END) AS NetCollected
FROM dbo.Payments WHERE Status='SUCCEEDED' GROUP BY OrderID,RepairTicketID;
GO
