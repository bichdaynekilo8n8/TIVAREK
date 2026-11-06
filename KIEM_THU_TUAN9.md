# Kiểm chứng bản bàn giao Tuần 9

Môi trường thực hiện: OpenJDK Temurin 21.0.13, Maven 3.9.11, Spring Boot 4.1.2. Dùng đúng Java 21, không hạ java.version trong POM.

`mvn verify` đã kết thúc thành công và tạo executable JAR.

| Nhóm | Đã chạy | Thất bại | Nội dung |
|---|---:|---:|---|
| RouteAuthorizationTest | 12 | 0 | 401/403, API cũ, giá nhập, khách, CSRF và render giao diện |
| MethodAuthorizationTest | 2 | 0 | Chặn service sai quyền và gọi hồ sơ dưới username khác |
| SecurityDatabaseTest | 17 | 0 | Login BCrypt, quyền từ DB, 5 vai trò, CRUD API/form, xem chéo hồ sơ, audit, rollback khi audit thất bại |
| HttpLoginTest | 1 | 0 | Tomcat cổng ngẫu nhiên, HTTP cookie thật, login, đổi JSESSIONID, render admin, logout và API 401 |
| SqlServerSecurityTest | Chưa chạy | — | Nhóm có điều kiện; cần SQL Server trên máy bạn |

Tổng cộng **32 kiểm thử đã chạy, không lỗi/thất bại**. Nhóm SQL Server hiện báo skipped; số test skipped ở đây không phải bằng chứng đã kiểm chứng SQL Server.

Các test có Database đã chạy dùng H2 với fixture riêng ở test scope, không sử dụng/mô phỏng đầy đủ toàn bộ ràng buộc và DDL của SQL Server. Runtime dev/prod dùng Microsoft JDBC SQL Server và Flyway, không fallback sang H2. SQL Server DDL/migration và kết nối đến máy Windows của bạn chưa được thực thi trong môi trường này.

Kiểm tra bổ sung:

- Các migration V1/V1_1/V2 từ thư mục week8 trong archive người dùng và V3 trong ứng dụng gốc được giữ nguyên byte.
- JAR có main duy nhất `com.tivarek.TivarekApplication` cùng module Flyway SQL Server; không chứa dependency H2 hoặc các lớp test.
- Maven Wrapper của project được giữ nguyên; URL bản Maven 3.10.0 trả HTTP 200 khi kiểm tra. Bài build trong môi trường này chạy bằng Maven 3.9.11.
- Chưa chạy launcher CMD/PowerShell trên Windows trong môi trường Linux này. Script dùng lệnh tương thích Windows PowerShell và Maven Wrapper đi kèm; cần Java 21 và quyền kết nối SQL Server trên máy bạn.

Chạy lại trên Windows:

```powershell
.\scripts\test-week9.ps1
.\scripts\test-week9.ps1 -SqlServer
```

Script SQL Server sẽ hỏi thông tin đăng nhập nếu chưa có biến môi trường. Dùng đúng DB thực hành `TIVAREK_WEEK8_DEV`, không dùng DB đang phục vụ cửa hàng thật cho nhóm test.
