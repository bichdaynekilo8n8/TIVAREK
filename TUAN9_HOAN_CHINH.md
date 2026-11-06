# TIVAREK — Bản hoàn thiện Tuần 9

Bản này được tích hợp trực tiếp từ TIVAREK.zip bạn gửi. Mở **pom.xml ở thư mục TIVAREK gốc**. Lớp chạy duy nhất của ứng dụng chính là `com.tivarek.TivarekApplication`.

## Đã làm

- Giữ Spring Boot 4.1.2 và Java 21 của project.
- Gộp hai cấu hình Security; bỏ main Tuần 8 bị lồng vào ứng dụng chính và bỏ mapping `/` trùng.
- Giữ giao diện trang chủ hiện tại; đọc sản phẩm từ schema SQL Server Tuần 8 qua JDBC.
- Bổ sung đúng starter JDBC/Flyway, dọn dependency trùng và loại mapping JPA `products(id,price,active)` không khớp `Products(ProductID,ListPrice,IsActive)`.
- Có giao diện đăng nhập, đăng xuất POST có CSRF, session 30 phút, BCrypt cost 12.
- Tài khoản, vai trò và quyền được đọc từ SQL Server; có kiểm tra URL và service.
- Quản trị có tìm kiếm, thêm/sửa khách hàng, xem giá nhập và nhật ký theo quyền.
- Khách chỉ xem hồ sơ liên kết với chính tài khoản mình; đổi ID khách khác trả 404.
- Audit đăng nhập thành công/thất bại, đăng xuất, tạo/cập nhật khách; thay đổi khách và audit cùng transaction.
- API Tuần 8 cũ vẫn được kiểm tra quyền.
- Có kiểm thử filter, service và toàn bộ luồng login/CRUD/audit với Database kiểm thử; có thêm nhóm kiểm thử SQL Server chạy trên máy bạn.

## Chạy trên máy Windows

### 1. Giải nén bản hoàn chỉnh

Dừng ứng dụng cũ. Giải nén ZIP vào một thư mục mới, ví dụ:

```text
D:\Projects\TIVAREK_Week9\TIVAREK
```

Mở `pom.xml` trong thư mục này bằng IntelliJ. Chọn Project SDK và Maven Runner JRE là Java 21. Bạn cũng có thể thay code trong repo đang làm sau khi đối chiếu thay đổi, nhưng không cần tự chép từng lớp nữa.

Bản đóng gói không mang theo thiết lập máy/phiên đăng nhập của IDE. Không mở `week8/example-app` để chạy Tuần 9; thư mục đó chỉ giữ lại tài liệu/code Tuần 8 bạn đã gửi.

### 2. Chuẩn bị SQL Server

Dùng Database thực hành Tuần 8: **TIVAREK_WEEK8_DEV**.

Trong SSMS:

```sql
SELECT name FROM sys.databases WHERE name LIKE 'TIVAREK%';
```

- Nếu `TIVAREK_WEEK8_DEV` đã có schema và lịch sử Flyway: giữ nguyên.
- Nếu chưa có DB: chạy `sql/00_create_database.sql`, tạo SQL Server login `tivarek_dev` với mật khẩu riêng trong SSMS nếu chưa có, rồi chạy `sql/01_map_dev_login.sql` bằng tài khoản quản trị.
- Không đổi/xóa `TIVAREK_DEV` cũ. DB đó từng dùng schema JPA ban đầu và không phải DB thực hành này.
- Không chạy các file `V*.sql` bằng tay. Ứng dụng chạy Flyway tự động. Các V1/V1_1/V2 được chép nguyên byte từ chính bản Tuần 8 trong archive bạn gửi; V3 cũng được giữ từ archive.
- Không xóa Flyway history hoặc tự repair khi gặp checksum mismatch; cần đối chiếu đúng migration đã áp dụng.

### 3. Khởi động và tạo tài khoản thực hành lần đầu

Có thể nhấp đúp `KHOI_TAO_TUAN9.cmd`. File này mở PowerShell cho đúng script và chỉ đặt execution policy cho process đó. Các lần sau dùng `CHAY_TIVAREK.cmd`. Muốn chạy test tự động dùng `KIEM_THU_TUAN9.cmd`.


Mở PowerShell tại thư mục có `pom.xml` và `mvnw.cmd`:

```powershell
cd D:\Projects\TIVAREK_Week9\TIVAREK
java -version
.\scripts\start-week9.ps1 -CreateDemoAccounts
```

Script hỏi SQL login, mật khẩu SQL Server và mật khẩu website thực hành. Mật khẩu website cần ít nhất 12 ký tự, không quá 72 byte UTF-8. Bạn tự đặt và ghi nhớ; không có mật khẩu mặc định trong code.

Mặc định kết nối:

```text
Server: 127.0.0.1:1433
Database: TIVAREK_WEEK8_DEV
SQL login: tivarek_dev
```

Nếu SQL Server dùng địa chỉ/cổng khác, đặt trước khi chạy script:

```powershell
$env:TIVAREK_DB_URL="jdbc:sqlserver://127.0.0.1:1433;databaseName=TIVAREK_WEEK8_DEV;encrypt=true;trustServerCertificate=true"
```

Thay địa chỉ/cổng theo kết nối đã chạy được ở Tuần 8. `trustServerCertificate=true` dành cho local. Ứng dụng cũng nhận các biến cũ `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`; ưu tiên các biến `TIVAREK_DB_*` nếu cả hai cùng được đặt.

Chờ log `Started TivarekApplication`. Mở:

```text
http://127.0.0.1:8080/login
```

### 4. Đăng nhập lần lượt

| Tài khoản | Vai trò | Thấy gì? |
|---|---|---|
| w9.owner | OWNER | Khách hàng, thêm/sửa khách, giá nhập, nhật ký |
| w9.sales | SALES | Khách hàng, thêm/sửa khách |
| w9.inventory | INVENTORY | Giá nhập |
| w9.tech | TECHNICIAN | Trang quản trị tài khoản; phân công sửa chữa sẽ làm ở module tiếp theo |
| w9.customer | CUSTOMER | Hồ sơ khách của chính tài khoản |
| w9.other | CUSTOMER | Hồ sơ riêng thứ hai, dùng kiểm tra truy cập chéo |

Cả sáu tài khoản thực hành mới dùng mật khẩu website bạn nhập khi chạy script. Bootstrap không đổi mật khẩu/role/hồ sơ của tài khoản đã tồn tại. Mật khẩu SQL Server và mật khẩu website là hai việc khác nhau.

Đăng xuất trước khi chuyển tài khoản. Dùng nhất quán host `127.0.0.1`; các tab cùng browser profile dùng chung session.

### 5. Các lần chạy sau

Dừng bằng Ctrl+C. Các lần tiếp theo chỉ chạy:

```powershell
.\scripts\start-week9.ps1
```

Không cần tạo lại tài khoản. Script không lưu mật khẩu ra file. Khi chạy bằng nút Run của IntelliJ, đặt các biến kết nối ở đúng Run Configuration; biến trong PowerShell không tự được IDE kế thừa.

### 6. Kiểm tra thao tác thật

- OWNER: mở `/admin`, thêm khách bằng form. Nhập mã như `KH009`, họ tên, số điện thoại 8–15 chữ số, email nếu có. Sau khi thêm, tìm mã vừa tạo rồi Chỉnh sửa.
- Xem `CUSTOMER_CREATE` / `CUSTOMER_UPDATE` ở bảng nhật ký. Người thao tác phải là tài khoản đang đăng nhập.
- SALES: mở trực tiếp `/api/admin/costs` phải nhận 403.
- INVENTORY/TECHNICIAN: mở `/api/admin/customers` và `/api/week8/customers` phải nhận 403.
- CUSTOMER: mở `/api/account/customer` trả hồ sơ của mình. Dùng SSMS tìm CustomerID của w9.other, rồi mở `/api/account/customers/ID_KHAC` phải nhận 404.
- Logout xong mở `/api/me` phải nhận 401.
- Chạy `sql/06_week9_verify.sql` trong SSMS để đối chiếu quyền, dữ liệu và audit.

## Kiểm thử

### Tự động, không cần cài SQL Server

```powershell
.\scripts\test-week9.ps1
```

Nhóm này dùng H2 ở test scope với schema fixture tối thiểu và chế độ cú pháp MSSQL. Nó chạy thật controller/service/repository, kiểm tra mật khẩu và quyền từ Database kiểm thử, CRUD và rollback audit; có thêm test HTTP qua máy chủ Tomcat cục bộ để kiểm tra cookie và vòng đời session. **Không được coi H2 là kiểm chứng SQL Server hoặc Flyway DDL thật.** Runtime dev/prod vẫn dùng SQL Server, không có fallback H2.

### SQL Server thực tế

Trong PowerShell chạy test, đặt kết nối đến DB thực hành đã có ở trên:

```powershell
$env:TIVAREK_DB_URL="jdbc:sqlserver://127.0.0.1:1433;databaseName=TIVAREK_WEEK8_DEV;encrypt=true;trustServerCertificate=true"
$env:TIVAREK_DB_USER="tivarek_dev"
# Script sẽ hỏi mật khẩu SQL Server nếu chưa có biến TIVAREK_DB_PASSWORD.
.\scripts\test-week9.ps1 -SqlServer
```

Nhóm SQL Server chỉ chạy khi được bật qua script, có guard đúng `TIVAREK_WEEK8_DEV`. Hầu hết fixture được rollback tự động. Bài kiểm tra rollback audit dùng transaction thật và tự dọn người dùng/hồ sơ test theo tiền tố ngẫu nhiên. IDENTITY có thể tăng dù rollback.

Chi tiết các kiểm tra đã thực hiện ở môi trường tạo bản này nằm trong `KIEM_THU_TUAN9.md`.

## Phạm vi và lưu ý vận hành

- Giá nhập lấy từ các dòng `RECEIPT` đã `POSTED` ở `BulkStockEntries` và `DeviceStockEntries`; chưa tính FIFO/giá vốn bình quân hay giá vốn hàng bán.
- Các quyền được nạp khi login. Đổi quyền/khóa tài khoản trong DB cần logout/login lại để kiểm tra; thu hồi toàn bộ session đang mở chưa thuộc phần này.
- Chưa có màn hình đăng ký, reset mật khẩu, phân công phiếu sửa hoặc quản lý nhân viên. Vai trò đã được seed và có tài khoản thực hành để nghiệm thu Tuần 9.
- Dữ liệu mẫu có sẵn là giả; giao diện đọc từ Database chứ không chèn dữ liệu mẫu vào HTML.
- Profile prod dùng HTTPS/Secure cookie; khi vận hành cần cấu hình TLS và tách tài khoản chạy migration với tài khoản runtime tối thiểu quyền.

## Nếu không chạy được

| Lỗi | Cách kiểm tra |
|---|---|
| PowerShell chặn chạy script | Dùng các launcher `.cmd` trong thư mục gốc hoặc `powershell -ExecutionPolicy Bypass -File .\scripts\start-week9.ps1 -CreateDemoAccounts` |
| Java/Maven không nhận | Cài JDK 21 và đặt JAVA_HOME/PATH; project đã có Maven Wrapper |
| Port 8080 đang dùng | Dừng ứng dụng cũ trước |
| Connection refused / login failed SQL | Kiểm tra TCP port, SQL login, mật khẩu và mapping DB; dùng kết nối Tuần 8 đã chạy được |
| Flyway checksum mismatch | Giữ DB/history, đối chiếu migration đúng bản đã áp dụng |
| Tài khoản w9 login thất bại | Chạy lần đầu có -CreateDemoAccounts; nếu user đã có, dùng mật khẩu cũ |
| Đã login mà API trả 401 | Giữ cùng host và cookie jar/session |
| POST trả 403 | Dùng form có CSRF hoặc script; kiểm tra cả quyền tài khoản |
| SQL Server tests bị skipped | Chưa bật -SqlServer; đây không phải kết quả đã test SQL thành công |
