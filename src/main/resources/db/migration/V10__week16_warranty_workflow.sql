-- Tuần 16: cấu hình bảo hành, lịch sử yêu cầu và phân quyền.
-- Sáu bảng bảo hành chính đã tồn tại ở V1; không tạo lại hoặc sửa migration cũ.
SET NOCOUNT ON;

CREATE TABLE dbo.ProductWarrantySettings (
    ProductID bigint NOT NULL PRIMARY KEY REFERENCES dbo.Products(ProductID),
    PolicyID bigint NOT NULL REFERENCES dbo.WarrantyPolicies(PolicyID),
    WarrantyDays int NOT NULL CHECK (WarrantyDays BETWEEN 0 AND 3650)
);

-- Máy đã qua sử dụng có thể có cam kết khác với SKU gốc.
CREATE TABLE dbo.DeviceWarrantyOverrides (
    DeviceID bigint NOT NULL PRIMARY KEY REFERENCES dbo.Devices(DeviceID),
    PolicyID bigint NOT NULL REFERENCES dbo.WarrantyPolicies(PolicyID),
    WarrantyDays int NOT NULL CHECK (WarrantyDays BETWEEN 0 AND 3650)
);

-- Services.DefaultWarrantyDays là nguồn ngày bảo hành dịch vụ duy nhất.
CREATE TABLE dbo.ServiceWarrantyPolicies (
    ServiceID bigint NOT NULL PRIMARY KEY REFERENCES dbo.Services(ServiceID),
    PolicyID bigint NOT NULL REFERENCES dbo.WarrantyPolicies(PolicyID)
);

CREATE TABLE dbo.WarrantyClaimEvents (
    EventID bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
    ClaimID bigint NOT NULL REFERENCES dbo.WarrantyClaims(ClaimID),
    FromStatus varchar(24) NULL,
    ToStatus varchar(24) NOT NULL,
    Note nvarchar(2000) NOT NULL,
    ActorID bigint NULL REFERENCES dbo.Users(UserID),
    IsPublic bit NOT NULL DEFAULT (1),
    OccurredAt datetime2(3) NOT NULL DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT CK_W16_ClaimEvents_To CHECK (ToStatus IN ('OPEN','INSPECTING','APPROVED','REJECTED','RESOLVED'))
);

CREATE INDEX IX_W16_Warranties_EndAt ON dbo.Warranties(Status,EndAt);
CREATE INDEX IX_W16_Claims_Status ON dbo.WarrantyClaims(Status,ClaimedAt DESC);
CREATE INDEX IX_W16_ClaimEvents_Claim ON dbo.WarrantyClaimEvents(ClaimID,EventID);

INSERT INTO dbo.Permissions(PermissionCode,Name)
SELECT x.Code,x.Name FROM (VALUES
 ('WARRANTY_READ',N'Xem phiếu và yêu cầu bảo hành'),
 ('WARRANTY_POLICY_MANAGE',N'Quản lý chính sách bảo hành'),
 ('WARRANTY_TOKEN_ISSUE',N'Cấp lại mã tra cứu bí mật cho khách'),
 ('WARRANTY_CLAIM_PROCESS',N'Tiếp nhận và xử lý yêu cầu bảo hành'),
 ('WARRANTY_CLAIM_APPROVE',N'Phê duyệt hoặc từ chối yêu cầu bảo hành'),
 ('WARRANTY_SELF_READ',N'Xem bảo hành của tài khoản khách')
) x(Code,Name)
WHERE NOT EXISTS(SELECT 1 FROM dbo.Permissions p WHERE p.PermissionCode=x.Code);

INSERT INTO dbo.RolePermissions(RoleID,PermissionID)
SELECT r.RoleID,p.PermissionID FROM (VALUES
 ('OWNER','WARRANTY_READ'),('OWNER','WARRANTY_POLICY_MANAGE'),
 ('OWNER','WARRANTY_TOKEN_ISSUE'),
 ('OWNER','WARRANTY_CLAIM_PROCESS'),('OWNER','WARRANTY_CLAIM_APPROVE'),
 ('OWNER','WARRANTY_SELF_READ'),
 ('SALES','WARRANTY_READ'),('SALES','WARRANTY_CLAIM_PROCESS'),
 ('SALES','WARRANTY_TOKEN_ISSUE'),
 ('TECHNICIAN','WARRANTY_READ'),('TECHNICIAN','WARRANTY_CLAIM_PROCESS'),
 ('CUSTOMER','WARRANTY_SELF_READ')
) x(RoleCode,PermissionCode)
JOIN dbo.Roles r ON r.RoleCode=x.RoleCode
JOIN dbo.Permissions p ON p.PermissionCode=x.PermissionCode
WHERE NOT EXISTS(SELECT 1 FROM dbo.RolePermissions rp WHERE rp.RoleID=r.RoleID AND rp.PermissionID=p.PermissionID);
