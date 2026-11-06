# Thay đổi từ TIVAREK.zip đã nhận

## Sửa nguyên nhân xung đột tích hợp

1. Giữ `TivarekApplication` làm main duy nhất trong src gốc; bỏ `week8/Week8Application` khỏi src gốc.
2. Gộp Security vào `config/SecurityConfig`; bỏ bản `week8/SecurityConfig` trùng bean.
3. Giữ HomeController nhận `/` và giao diện index của cửa hàng; PageController chỉ nhận quản trị/hồ sơ/form khách.
4. ProductRepository/ProductService chuyển sang JdbcTemplate đọc schema Tuần 8. Bỏ entity Product JPA cũ; bỏ JPA starter và Hibernate ddl-auto=update để tránh sửa schema ngoài Flyway.
5. Dọn dependency trùng, thêm đúng starter JDBC/Flyway cho Spring Boot 4.1.2 và module SQL Server. Main Maven plugin chỉ rõ TivarekApplication.
6. Đưa các migration Tuần 8 từ project con vào ứng dụng chính, giữ nguyên byte bản người dùng gửi; cấu hình dev dùng DB thực hành Tuần 8 và hỗ trợ tên biến môi trường cũ/mới.

## Hoàn thiện chức năng

- Trang login riêng, bảo vệ session/CSRF, BCrypt cost 12; kiểm tra giới hạn 72 byte khi xác minh mật khẩu.
- Form thêm/sửa/tìm kiếm khách trên admin, ràng buộc dữ liệu và thông báo kết quả.
- OWNER/SALES/TECHNICIAN/INVENTORY/CUSTOMER và permission đọc từ SQL Server, bảo vệ cả URL lẫn service.
- Giá nhập từ phiếu nhập đã POSTED, tách khỏi DTO sản phẩm công khai.
- Khách chỉ xem hồ sơ liên kết UserID của mình; ID người khác trả 404.
- Audit đăng nhập thành công/thất bại, logout, tạo/cập nhật khách; thao tác dữ liệu và audit cùng transaction. Hiển thị thời gian audit theo giờ Việt Nam.
- Giữ API khách hàng Tuần 8 nhưng áp dụng quyền giống API admin.
- Chuẩn hóa việc lấy CustomerID bằng JDBC generated keys; câu truy vấn hồ sơ tách trường hợp có/không có ID.
- Có script start/test/API thực hành, SQL đối chiếu, launcher CMD và kiểm thử tự động/SQL Server có điều kiện.

## Khi sử dụng cùng repo hiện tại

Khuyến nghị giải nén vào thư mục mới để thử trước. Nếu chép đè vào project cũ, ZIP không tự xóa các file đã bị loại khỏi bản mới. Cần bỏ chính xác các file cũ sau rồi chạy `mvnw.cmd clean verify`:

- `src/main/java/com/tivarek/week8/SecurityConfig.java`
- `src/main/java/com/tivarek/week8/Week8Application.java`
- `src/main/java/com/tivarek/entity/Product.java`
- `src/test/java/com/tivarek/TivarekApplicationTests.java` (đã thay bằng test có cấu hình Database rõ ràng)

Không xóa bản Week8Application trong `week8/example-app` nếu bạn muốn giữ project Tuần 8 tham khảo; nó nằm ngoài source root của ứng dụng chính.

Giữ lịch sử Git/config cá nhân đang có ở máy bạn. Bản đóng gói chỉ bàn giao source, tài nguyên, wrapper, script và tài liệu; không mang thiết lập IDE hoặc output build cũ.
