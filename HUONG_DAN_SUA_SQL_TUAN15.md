# TIVAREK — Khắc phục lỗi SQL Server 18456 khi khởi động Tuần 15

## Bản sửa tiếp theo: SQL Server 156 — `Incorrect syntax near the keyword 'LineNo'`

Log ngày 06/11/2026 sau khi đã nhập đúng mật khẩu xác nhận SQL Server kết nối thành công,
Flyway đã validate **13 migration**, ở schema version **8.1**, nhưng `V9__week15_sales_permissions.sql`
thất bại và **được rollback**. Nguyên nhân là tên cột `LineNo` trùng với từ khóa
T-SQL `LINENO` khi không đặt trong dấu `[]`.

Đã sửa `V9` thành `ON dbo.SalesLines(OrderID,[LineNo])`, đồng thời sửa mọi câu SQL
đang dùng `LineNo` chưa có dấu `[]` trong `SalesService.java` và `RepairService.java`.

**Cách áp dụng:** thay ba file trên hoặc dùng ZIP TIVAREK mới nhất, sau đó chạy
`CHAY_TUAN15.cmd`. Script có `-CleanBuild` để không dùng nhầm bản SQL cũ trong
`target/classes`. Vì V9 đã rollback và chưa được đánh dấu áp dụng thành công,
không chạy `flyway repair`, `flyway clean` và không xóa dữ liệu.

---

## Kết luận từ log ngày 06/11/2026

- `mvnw.cmd spring-boot:run` đã **compile thành công 74 Java sources** và 19 test sources.
- Ứng dụng ngừng trước khi chạy Flyway do `Login failed for user 'tivarek_dev'` (SQL Server **18456**).
- Đây là lỗi xác thực/cấu hình SQL Server, không phải lỗi biên dịch Java hay `RegistrationController`.
- `application-dev.properties` mặc định dùng SQL login `tivarek_dev` nhưng **mật khẩu để trống** nếu không có `TIVAREK_DB_PASSWORD` hoặc `DB_PASSWORD`. Vì vậy không nên chạy trực tiếp `mvnw.cmd` ở cửa sổ chưa đặt mật khẩu.

## Cách chạy đúng (ưu tiên)

1. Dừng mọi tiến trình TIVAREK khác.
2. Mở thư mục dự án có `pom.xml` và nhấp đúp **`CHAY_TUAN15.cmd`**.
3. Khi hỏi `SQL Server login`, nhập login đã sử dụng ở Tuần 8–14 (thông thường là `tivarek_dev`).
4. Nhập **mật khẩu SQL Server** tại lời nhắc; đây không phải mật khẩu tài khoản website `w9.owner`.
5. Chờ `Started TivarekApplication`, mở <http://127.0.0.1:8080/>.

Tương đương trong PowerShell:

```powershell
cd D:\Projects\TIVAREK
.\scripts\start-week9.ps1 -PromptDbCredentials -CleanBuild
```

Script hỏi thông tin đăng nhập và chỉ truyền trong tiến trình hiện tại. Không thêm mật khẩu vào `application-dev.properties`, Git, hoặc thông báo lỗi.

## Nếu vẫn bị 18456

1. Trong SSMS, chọn đúng SQL Server instance, ví dụ instance lắng nghe ở `127.0.0.1,1433`. Thử đăng nhập lại bằng **SQL Server Authentication** với đúng login và mật khẩu đã nhập cho script. Nếu SSMS cũng không đăng nhập được, ứng dụng không thể tự sửa mật khẩu.
2. Đăng nhập SSMS bằng **Windows Authentication** với quyền quản trị, mở `sql/11_week15_check_sql_login.sql` và chạy. File này chỉ `SELECT`, không ghi/xóa dữ liệu.
3. Kiểm tra `WindowsAuthenticationOnly`: nếu bằng `1`, instance đang chỉ cho Windows Authentication. Trong SSMS vào **Server Properties → Security → SQL Server and Windows Authentication mode**, xác nhận và khởi động lại dịch vụ SQL Server theo hướng dẫn Microsoft. Chỉ thực hiện khi có quyền quản trị instance và muốn dùng SQL login.
4. Nếu login `tivarek_dev` không tồn tại, tạo SQL login bằng quyền quản trị và đặt mật khẩu mạnh riêng; nếu bị disabled, kiểm tra và kích hoạt login. Không tự reset mật khẩu của tài khoản hiện có khi chưa biết nó được dùng ở đâu.
5. Xác nhận database `TIVAREK_WEEK8_DEV` tồn tại và `ONLINE`. Nếu login có thật mà chưa được map vào database, xem `sql/01_map_dev_login.sql` (chỉ dành cho DB thực hành này; quyền `db_owner` không thích hợp khi triển khai production).
6. Nếu SSMS đăng nhập thành công nhưng ứng dụng vẫn lỗi, đối chiếu **instance/cổng/database** trong URL JDBC và cấu hình SSMS. Trong PowerShell có thể đặt URL riêng cho phiên chạy:

```powershell
$env:TIVAREK_DB_URL = 'jdbc:sqlserver://127.0.0.1:1433;databaseName=TIVAREK_WEEK8_DEV;encrypt=true;trustServerCertificate=true'
.\scripts\start-week9.ps1 -PromptDbCredentials -CleanBuild
```

7. Nếu cần phân biệt mật khẩu sai, login disabled, sai database hay authentication mode, đọc **SQL Server ERRORLOG** bằng SSMS: thông báo `18456, State: N` trên máy chủ chi tiết hơn lỗi phía client.

**Không chạy `flyway clean`, không xóa hoặc tạo lại database đã có dữ liệu, không sửa các file V1–V9 đã migrate để xử lý lỗi xác thực.**

## Java 21

Log hiện chạy bằng `Java 26.0.2.1`, dù dự án biên dịch với `--release 21`. Nên đặt `JAVA_HOME`/Maven Runner về JDK 21 để dùng nhất quán theo yêu cầu dự án. Đây **không phải nguyên nhân** của SQL Server 18456.

## Tài liệu chính thức

- [Microsoft — SQL Server error 18456](https://learn.microsoft.com/en-us/sql/relational-databases/errors-events/mssqlserver-18456-database-engine-error)
- [Microsoft — Change server authentication mode](https://learn.microsoft.com/en-us/sql/database-engine/configure-windows/change-server-authentication-mode)
