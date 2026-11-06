# TIVAREK — Hướng dẫn chạy bản sửa lỗi Tuần 12

## Đã sửa trong bản này

- Đổi migration thành `V6__week12_inventory_management.sql` (đúng hai dấu gạch dưới, chỉ một đuôi `.sql`).
- Không đóng gói `target/classes` cũ, nơi vẫn chứa migration `V6_week12_inventory_management.sql` sai tên.
- Thêm `CHAY_TUAN12.cmd`, chạy `mvn clean` trước khi khởi động để loại bản sao cũ trong `target`.
- Bộ khởi động hỏi lại SQL login và mật khẩu, kể cả khi biến môi trường đang chứa mật khẩu cũ.
- Giữ nguyên migration V1–V5, V1_1, V4_1 và V5_1.
- **Chưa triển khai nghiệp vụ nhập, xuất, chuyển kho hay dashboard.** V6 hiện chỉ bổ sung quyền và chỉ mục.

## Cách chạy trên Windows

1. Sao lưu mã nguồn và database DEV trước khi cập nhật.
2. Giải nén ZIP vào **thư mục mới**. Không chép đè dữ liệu cửa hàng đang vận hành.
3. Mở thư mục `TIVAREK` (chứa `pom.xml`) bằng IntelliJ, chọn **JDK 21**.
4. Chạy file **`CHAY_TUAN12.cmd`**. File này tự làm sạch các lớp biên dịch cũ và chạy lại ứng dụng.
5. Khi được hỏi `SQL Server login`, nhập SQL login đã tạo ở Tuần 8 (thường là `tivarek_dev`).
6. Khi được hỏi `Mat khau SQL Server`, nhập mật khẩu **SQL Server login**, không phải mật khẩu website `w9.owner`.
7. Nếu khởi động thành công, mở `http://127.0.0.1:8080/login`.

Bạn cũng có thể chạy trong PowerShell tại thư mục dự án:

```powershell
.\scripts\start-week9.ps1 -PromptDbCredentials -CleanBuild
```

## Nếu vẫn báo SQL Server 18456

Lỗi 18456 nghĩa là SQL Server từ chối đăng nhập. Mã nguồn không thể tự đoán hay đổi mật khẩu trên máy bạn.

- Dùng SSMS thử đăng nhập bằng **SQL Server Authentication**, đúng SQL login, mật khẩu và server đã cấu hình.
- Kiểm tra database DEV `TIVAREK_WEEK8_DEV`, quyền truy cập của login và chế độ Mixed Mode (nếu dùng SQL Authentication).
- Nếu SSMS cũng bị từ chối, cần kiểm tra tài khoản SQL Server bằng quyền quản trị của bạn; **không sửa Flyway, không xóa database**.
- Nếu SSMS đăng nhập được, kiểm tra `TIVAREK_DB_URL` và login mà bộ khởi động đang hỏi. **Không gửi mật khẩu cho người khác.**

## Kiểm tra Flyway V6

Trong SSMS, chọn database DEV rồi chạy:

```sql
SELECT installed_rank, version, description, success
FROM dbo.flyway_schema_history
ORDER BY installed_rank;
```

V6 phải có `success = 1` sau khi khởi động thành công.

## Giới hạn xác minh

Bản này xử lý các lỗi file migration và bộ khởi động tìm thấy trong ZIP. Không thể bảo đảm đăng nhập SQL Server thành công nếu mật khẩu hoặc quyền login trên máy bạn không chính xác.
