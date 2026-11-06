-- Tuần 17: thời điểm hoàn tất đơn thực tế, quyền báo cáo và chỉ mục phục vụ thống kê.
-- Không sửa lịch sử các migration V1-V10.
SET NOCOUNT ON;

-- Không lấy OrderedAt làm ngày hoàn tất. Đơn COMPLETED lịch sử giữ NULL
-- vì dữ liệu cũ không lưu được thời điểm chuyển trạng thái chính xác.
ALTER TABLE dbo.SalesOrders ADD CompletedAt datetime2(3) NULL;
GO

CREATE INDEX IX_W17_Orders_CompletedAt ON dbo.SalesOrders(CompletedAt) INCLUDE(Status,Channel);
CREATE INDEX IX_W17_Payments_Occurred ON dbo.Payments(OccurredAt,Status,Direction)
    INCLUDE(Amount,OrderID,RepairTicketID);
CREATE INDEX IX_W17_Repairs_Received ON dbo.RepairTickets(ReceivedAt,Status)
    INCLUDE(ClosedAt,PromisedAt);
CREATE INDEX IX_W17_Claims_Claimed ON dbo.WarrantyClaims(ClaimedAt,Status);
GO

INSERT INTO dbo.Permissions(PermissionCode,Name)
SELECT x.Code,x.Name FROM (VALUES
 ('REPORT_OVERVIEW',N'Xem dashboard tổng hợp và các chỉ số tài chính'),
 ('REPORT_SALES',N'Xem thống kê bán hàng'),
 ('REPORT_FINANCE',N'Xem dòng tiền và chi phí cửa hàng'),
 ('REPORT_REPAIRS',N'Xem thống kê hoạt động sửa chữa'),
 ('REPORT_STOCK',N'Xem báo cáo số lượng tồn kho'),
 ('REPORT_WARRANTIES',N'Xem báo cáo bảo hành')
) x(Code,Name)
WHERE NOT EXISTS (SELECT 1 FROM dbo.Permissions p WHERE p.PermissionCode=x.Code);

INSERT INTO dbo.RolePermissions(RoleID,PermissionID)
SELECT r.RoleID,p.PermissionID FROM (VALUES
 ('OWNER','REPORT_OVERVIEW'),('OWNER','REPORT_SALES'),('OWNER','REPORT_FINANCE'),
 ('OWNER','REPORT_REPAIRS'),('OWNER','REPORT_STOCK'),('OWNER','REPORT_WARRANTIES'),
 ('SALES','REPORT_SALES'),('SALES','REPORT_WARRANTIES'),
 ('INVENTORY','REPORT_STOCK'),
 ('TECHNICIAN','REPORT_REPAIRS'),('TECHNICIAN','REPORT_WARRANTIES')
) x(RoleCode,PermissionCode)
JOIN dbo.Roles r ON r.RoleCode=x.RoleCode
JOIN dbo.Permissions p ON p.PermissionCode=x.PermissionCode
WHERE NOT EXISTS (SELECT 1 FROM dbo.RolePermissions rp
                  WHERE rp.RoleID=r.RoleID AND rp.PermissionID=p.PermissionID);
