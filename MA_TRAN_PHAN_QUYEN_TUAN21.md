# TIVAREK — MA TRẬN KIỂM TRA PHÂN QUYỀN TUẦN 21

Đây là **ma trận kiểm soát quyền kỹ thuật** dựa vào permission code; tránh diễn giải sai rằng một role mặc định luôn sở hữu một permission. Role/permission thực tế được lấy từ `dbo.RolePermissions` qua `DbUserDetailsService`.

| Chức năng HTTP | Permission bắt buộc ở SecurityConfig | Service kiểm tra bổ sung |
| --- | --- | --- |
| GET `/admin/orders/**` | SALE_READ | Ràng buộc nghiệp vụ liên quan |
| POST `/admin/orders/new` | SALE_CREATE | SALE_CREATE trong `SalesService.create()` |
| POST `/admin/orders/*/confirm`, `/renew`, `/cancel` | SALE_CONFIRM | SALE_CONFIRM |
| POST `/admin/orders/*/lines/**` | SALE_CREATE | SALE_CREATE |
| POST `/admin/orders/*/ship` | SALE_FULFILL **và** STOCK_ISSUE | Giống HTTP |
| POST `/admin/orders/*/payment` | SALE_PAYMENT | SALE_PAYMENT |
| POST `/admin/orders/*/return` | SALE_RETURN | SALE_RETURN |
| POST `/admin/orders/*/refund` | SALE_REFUND | SALE_REFUND |
| GET `/admin/stock/**` | STOCK_READ | Kiểm tra kho hiện có |
| POST `/admin/stock/*/reverse`, `/locations/**` | STOCK_ADJUST | STOCK_ADJUST |
| POST `/admin/stock/suppliers` | STOCK_RECEIVE | STOCK_RECEIVE |
| POST `/admin/stock/reservations/**` | STOCK_ISSUE | STOCK_ISSUE |
| POST `/admin/stock/draft`, `/*/post` | Một trong các quyền thao tác kho | Kiểm tra loại chứng từ tại Service |
| GET `/admin/repairs/**` | REPAIR_READ | REPAIR_READ |
| POST `/admin/repairs/intake` | REPAIR_INTAKE | REPAIR_INTAKE |
| POST `/admin/repairs/*/assign` | REPAIR_ASSIGN | REPAIR_ASSIGN |
| POST `/admin/repairs/*/quotes/**` và `/services` | REPAIR_QUOTE | REPAIR_QUOTE |
| POST `/admin/repairs/*/work`, `/parts/**`, `/status` | REPAIR_WORK | REPAIR_WORK |
| POST `/admin/repairs/*/handover` | REPAIR_HANDOVER | REPAIR_HANDOVER |
| POST `/admin/repairs/*/images/**` | REPAIR_INTAKE hoặc REPAIR_WORK | Upload service xác minh quyền ảnh |
| GET `/admin/warranties/**` | WARRANTY_READ | WARRANTY_READ |
| POST `/admin/warranties/policies`, `/settings` | WARRANTY_POLICY_MANAGE | WARRANTY_POLICY_MANAGE |
| POST `/admin/warranties/*/token` | WARRANTY_TOKEN_ISSUE | WARRANTY_TOKEN_ISSUE |
| POST `/admin/warranties/claims/*/status`, `/repair` | WARRANTY_CLAIM_PROCESS | WARRANTY_CLAIM_PROCESS |
| GET/POST `/doi-mat-khau` | Đăng nhập hợp lệ | BCrypt + mật khẩu hiện tại + session revoke |

Lưu ý: Những Controller vốn yêu cầu quyền đọc ở cấp class vẫn giữ ràng buộc đó; quyền viết không mặc nhiên cho phép đọc nếu `RolePermissions` không cấp. Đừng sửa role dữ liệu thật chỉ để làm test đi qua.

## Đối chiếu role thực tế trên SQL DEV (chỉ đọc)

```sql
SELECT r.RoleCode, p.PermissionCode
FROM dbo.Roles r
JOIN dbo.RolePermissions rp ON rp.RoleID=r.RoleID
JOIN dbo.Permissions p ON p.PermissionID=rp.PermissionID
ORDER BY r.RoleCode, p.PermissionCode;
```

Khi xác nhận phân quyền, kiểm tra cả người được phép và người không được phép. HTTP 403 đúng phải đi kèm **không có cập nhật DB**.
