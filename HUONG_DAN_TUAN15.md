# TIVAREK — Tuần 15: Bán hàng, giỏ hàng, thanh toán và trả hàng

## 1. Cập nhật dự án

- Giải nén toàn bộ ZIP này, mở **thư mục TIVAREK** bằng IntelliJ IDEA.
- Chọn **JDK 21** (Project SDK và Maven Runner). Giữ SQL Server và cấu hình tài khoản DB hiện có.
- Tạo bản sao lưu database trước khi áp dụng cập nhật.
- **Khởi động bằng `CHAY_TUAN15.cmd`** (hoặc `.\scripts\start-week9.ps1 -PromptDbCredentials -CleanBuild` trong PowerShell). Script yêu cầu nhập mật khẩu SQL Server cho mỗi lần chạy, không ghi mật khẩu vào file. Nếu chạy trực tiếp `mvnw.cmd spring-boot:run` mà chưa đặt biến `TIVAREK_DB_PASSWORD` thì SQL Server có thể báo `18456`. Xem `HUONG_DAN_SUA_SQL_TUAN15.md`.
- **SQL Server 2025 (17.x):** bản cập nhật ngày 06/11 đã sửa lỗi `156` khi Flyway áp dụng V9 vì tên cột `LineNo` cần viết `[LineNo]`. Cùng lỗi này đã được sửa ở các câu SQL Tuần 14–15. Nếu đã gặp lỗi này, chỉ cập nhật mã nguồn và chạy lại `CHAY_TUAN15.cmd`; không xóa hoặc repair database.
- Flyway áp dụng migration **V9__week15_sales_permissions.sql**: thêm quyền, bảng hash token tra cứu đơn web và khóa chống thao tác kho lặp, không sửa hoặc xóa dữ liệu cũ.
- **Đăng xuất, đăng nhập lại** với tài khoản đã có để tải quyền Tuần 15 vào session Spring Security.

> Đã giữ nguyên mọi migration cũ. Không chạy lại các file V1 bằng tay trên CSDL đang có dữ liệu.

## 2. Điều hướng

| Địa chỉ | Chức năng | Quyền |
|---|---|---|
| `/san-pham` | Chọn sản phẩm có đăng bán, còn tồn | Công khai |
| `/gio-hang` | Giỏ hàng, sửa số lượng, xóa dòng | Công khai |
| `/thanh-toan` | Khách tự tạo đơn web | Công khai, CSRF |
| `/don-hang/{code}?token={token}` | Tra cứu đơn bằng mã bí mật | Người có token hoặc tài khoản liên kết |
| `/account/orders` | Xem các đơn gắn tài khoản | CUSTOMER, OWNER nếu có hồ sơ khách |
| `/admin/orders` | Danh sách/lọc đơn | OWNER, SALES, INVENTORY |
| `/admin/orders/new` | Bán tại quầy từ giỏ hàng của nhân viên | OWNER, SALES |
| `/admin/orders/{id}` | Xác nhận, giao hàng, thu/hoàn tiền và trả hàng | Theo quyền thao tác |

Trên trang quản trị `/admin` có thêm liên kết **Bán hàng & đơn hàng**.

## 3. Quy trình web

1. Truy cập `/san-pham`, mở hàng theo số lượng hoặc thiết bị riêng lẻ, nhấn **Thêm vào giỏ hàng**.
2. Vào `/gio-hang`, thay đổi số lượng (đặt 0 để xóa). Giá và tồn được xác minh lại từ SQL Server.
3. Vào `/thanh-toan`, nhập tên, điện thoại, lựa chọn nhận tại cửa hàng hoặc giao tận nơi; cần địa chỉ nếu giao.
4. Nhấn **Gửi đơn đặt hàng**. Hệ thống lưu `SalesOrders`, `OrderShipping`, `SalesLines`, `BulkSaleLines` hoặc `DeviceSaleLines`; trạng thái `DRAFT`.
5. Khách nhận trang biên nhận có mã bí mật trong URL. **Giữ kín URL**; đây là quyền xem đơn vãng lai. Hash SHA-256 được lưu trong `SalesTrackingTokens`, không lưu token thô.
6. Nhân viên `OWNER/SALES` mở `/admin/orders`, xác nhận đơn. Khi đó mới tạo `BulkReservations` hoặc `DeviceReservations`. Hàng giữ **7 ngày**; sau đó nhân viên có thể làm mới với đơn chưa giao.
7. `OWNER/INVENTORY` chọn dòng, số lượng và kho để **xuất kho giao hàng** (có thể giao từng phần). Sổ kho Tuần 12 kiểm tra âm kho, trùng serial và ghi bút toán `SALE_ISSUE` `POSTED`.
8. `OWNER/SALES` nhập giao dịch `CASH` hoặc `BANK_TRANSFER` **đã xác minh tiền**. Đơn `COMPLETED` khi giao đủ và đã thu đủ tiền.

**Không có cổng thanh toán thực sự**. Việc ghi nhận chuyển khoản là thao tác thủ công của nhân viên sau đối soát ngân hàng, không phải tự động xác minh tiền.

## 4. Quy trình tại quầy

1. Đăng nhập tài khoản `SALES` hoặc `OWNER`, tìm hàng ở `/san-pham` và thêm vào giỏ hàng (giỏ lưu trong phiên làm việc của nhân viên).
2. Truy cập `/admin/orders/new`, nhập khách và phương thức nhận; có thể liên kết hồ sơ khách đã tồn tại hoặc để khách vãng lai. Tạo đơn `COUNTER`.
3. Hệ thống mở trang nhân viên của đơn mới; xác nhận và giữ hàng như đơn web.
4. Thu tiền đã xác minh; nhân viên kho/OWNER xuất hàng. Hỗ trợ giao nhiều lần.

Trước khi xác nhận (`DRAFT`), nhân viên có thể **thêm dòng theo ID sản phẩm/máy**, chỉnh số lượng hoặc nhập `0` để xóa dòng. Sau xác nhận, snapshot và dòng đơn bị khóa; nếu cần sửa đơn chưa giao, hãy hủy (sau khi xử lý tiền) và lập đơn mới.

Khách mua tại quầy không cần tài khoản, vì `SalesOrders.CustomerID` có thể NULL. Khi chọn hồ sơ khách đã có tại quầy, đơn liên kết `CustomerID`; một đơn web từ tài khoản đăng nhập được liên kết với `Customers.UserID` nếu hồ sơ khách tồn tại.

## 5. Nhận trả và hoàn tiền

- Người quản lý kho/OWNER vào đơn đã giao, chọn dòng và số lượng **không vượt số đã giao trừ số đã trả**, chọn vị trí kiểu `QUARANTINE`, ghi lý do.
- Hệ thống ghi `SALE_RETURN` `POSTED` và liên kết `SalesReturns` + `ReturnBulkEntries` hoặc `ReturnDeviceEntries`. Máy serial chỉ được nhận trả một lần.
- `OWNER` hoàn tiền trên giao dịch thu `SUCCEEDED` gốc. Hệ thống ghi `Payments` hướng `OUT`, `PaymentRefunds` và khóa tổng hoàn không vượt giao dịch gốc.
- Nếu đã giao, tổng hoàn không vượt trị giá hàng đã nhận trả. Nếu chưa giao, có thể hoàn tiền trước rồi hủy đơn.
- Hai thao tác nhận trả và hoàn tiền cố ý tách biệt: hàng có thể được nhận về kiểm tra trước khi quyết định hoàn.

## 6. Phân quyền

| Vai trò | Xem | Tạo quầy | Xác nhận/hủy | Giao hàng | Thu tiền | Nhận trả | Hoàn tiền |
|---|---|---|---|---|---|---|---|
| OWNER | Có | Có | Có | Có | Có | Có | Có |
| SALES | Có | Có | Có | Không | Có | Không | Không |
| INVENTORY | Có | Không | Không | Có | Không | Có | Không |
| CUSTOMER | Chỉ đơn của mình | Không | Không | Không | Không | Không | Không |

Phân quyền ở service qua `@PreAuthorize` và bộ lọc HTTP; không dựa vào việc ẩn nút trong HTML. Giao hàng yêu cầu cả `SALE_FULFILL` và `STOCK_ISSUE`.

## 7. Kiểm thử đề xuất

### Tự động

Trong máy có JDK 21 và Maven wrapper:

```powershell
.\mvnw.cmd test
```

Kiểm thử mới `SaleCartTest` và `SaleRoutesTest` kiểm tra số lượng serial, lấy giá từ DB, quyền truy cập và CSRF. Chạy toàn bộ test hiện có để kiểm tra hồi quy Tuần 8–14.

### SQL Server/SSMS — kịch bản bắt buộc

1. Nhập một SKU `BULK` (10 cái) và một máy riêng lẻ vào kho `SALEABLE` qua `/admin/stock`, ghi `POSTED`, rồi đăng bán tại Tuần 13.
2. Hai cửa sổ cùng chọn máy; xác nhận một đơn thành công, đơn còn lại không được giữ trùng.
3. Tạo đơn 4 cái BULK, xác nhận, xuất 2 cái, xuất tiếp 2 cái; tổng số xuất đúng 4, kho giảm 4.
4. Thử xuất 5 cái cho đơn 4 cái; hệ thống phải từ chối.
5. Thu 50% rồi thu 50%: tổng không vượt giá trị đơn; khi đã giao đủ trạng thái `COMPLETED`.
6. Dùng cùng `idempotencyKey` gửi lại thu tiền, xuất hàng, nhận hàng trả hoặc hoàn tiền: trả về cùng ID, không ghi sổ lần hai.
7. Nhận trả 1 cái vào `QUARANTINE`, thử trả quá số đã giao sẽ bị từ chối; hàng không tự nhập lại kho bán.
8. Hoàn tiền theo giao dịch thu gốc; tổng hoàn không vượt giá trị hàng đã trả.
9. Thử khách A mở `/account/orders/{code}` của khách B: phải từ chối, trừ khi có token bí mật hợp lệ.
10. Thử POST không có CSRF: phải bị từ chối; tài khoản CUSTOMER không thể vào `/admin/orders`.

Các SQL kiểm tra an toàn chỉ đọc nằm ở `sql/10_week15_verify.sql`.

## 8. Giới hạn nghiệp vụ và triển khai thực tế

- Phí giao hàng đang là **0 đ**: cần chính sách cước và đơn vị vận chuyển nếu đưa vào kinh doanh thật.
- Không tích hợp VNPay/MoMo/chuyển khoản tự động; `BANK_TRANSFER` phải do nhân viên đối soát.
- Không có hóa đơn VAT điện tử, mã giảm giá, tính phí thuế, in POS, theo dõi vận đơn hãng vận chuyển hoặc quy trình đổi hàng lấy hàng khác; cần bổ sung trước vận hành thương mại đầy đủ.
- `WarrantyDaysSnapshot` hiện để **0** khi chốt đơn; sang Tuần 16 phải xác lập chính sách bảo hành và ghi snapshot bảo hành cho từng dòng.
- Giá vốn là giá của phiếu nhập `RECEIPT` gần nhất; đây là lựa chọn tạm thời, không phải phương pháp giá vốn bình quân/FIFO chuẩn để lập báo cáo kế toán.
- Tiếp tục triển khai Tuần 16: bảo hành theo serial và phiếu bán, tra cứu và quản lý yêu cầu bảo hành.
