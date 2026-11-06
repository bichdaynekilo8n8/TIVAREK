# Kết quả build và kiểm thử Tuần 11

Ngày 04/11/2026. Môi trường: JDK 21.0.13, Maven 3.9.11, Spring Boot 4.1.2.

Đã chạy Maven `verify` thành công: biên dịch ứng dụng, biên dịch test, chạy test và đóng gói executable JAR. Gói bàn giao chứa mã nguồn; JAR được tạo lại trong `target/` khi build.

**71 ca thực thi đạt, 0 thất bại, 0 lỗi.** Có 41 ca SQL Server opt-in được bỏ qua vì môi trường hiện tại không có SQL Server và thông tin kết nối máy bạn. Chưa xác nhận migration V5/V5_1 và hành vi khóa trên SQL Server thật.

| Nhóm | Tổng ca | Thất bại | Lỗi | Bỏ qua |
|---|---:|---:|---:|---:|
| ProductManagementTest | 15 | 0 | 0 | 0 |
| SqlServerProductTest | 11 | 0 | 0 | 11 |
| DeviceManagementTest | 21 | 0 | 0 | 0 |
| HttpDeviceWorkflowTest | 1 | 0 | 0 | 0 |
| SqlServerDeviceTest | 17 | 0 | 0 | 17 |
| HttpLoginTest | 1 | 0 | 0 | 0 |
| MethodAuthorizationTest | 4 | 0 | 0 | 0 |
| RouteAuthorizationTest | 12 | 0 | 0 | 0 |
| SecurityDatabaseTest | 17 | 0 | 0 | 0 |
| SqlServerSecurityTest | 13 | 0 | 0 | 13 |

Các luồng đã kiểm tra:

- Tuần 9–10: đăng nhập/đăng xuất, session, quyền, khách hàng, sản phẩm, từ điển và audit vẫn chạy đạt.
- Tạo hai máy cùng mẫu, cấu hình/giá riêng; sửa một máy không đổi máy còn lại hoặc Products.
- Mã máy trùng khác hoa/thường/khoảng trắng bị chặn ở API và unique key DB test.
- Giá âm, loại tình trạng sai, cấu hình số sai kiểu, thông số không tồn tại, mẫu BULK/ngừng kinh doanh/không tồn tại bị từ chối.
- Serial trống thành NULL; chưa định giá được lưu NULL; cảnh báo serial trùng.
- Version cũ không ghi đè thông tin, giá hoặc ảnh mới; ảnh và kiểm tra cập nhật phiên bản máy.
- Hồ sơ cũ chưa liên kết mẫu có thể gán mẫu hợp lệ lần đầu; sau đó chặn đổi mẫu. Máy có lỗi phải mô tả lỗi.
- OWNER/INVENTORY được quản lý máy; SALES chỉ xem; TECHNICIAN chỉ xem, quản lý ảnh và ghi kiểm tra; CUSTOMER bị chặn. Quyền service tiếp tục chặn ghi khi gọi trực tiếp với quyền chỉ đọc.
- Máy khách bị loại khỏi danh sách và bị chặn trên chi tiết, sửa và ảnh của module cửa hàng.
- Ảnh PNG thật được lưu ngoài source, chỉ đọc qua route có quyền; tên tệp gốc chứa đường dẫn không chi phối đường dẫn lưu.
- Đổi ảnh đại diện, thứ tự và gỡ ảnh không vi phạm unique thứ tự; chặn ảnh của máy khác, ảnh giả và tệp vượt 5 MB.
- Tải một lô có tệp hỏng hoặc ghi audit lỗi rollback DB và dọn tệp đã tạo; gỡ ảnh chỉ xóa tệp sau commit.
- Kiểm tra kỹ thuật ghi nhiều lần, lấy tài khoản thực hiện từ session; không thay giá khi gửi thêm trường trái quyền.
- Render và submit form thêm/sửa, bind cấu hình động, giữ đầu vào khi lỗi; tìm kiếm, lọc, phân trang và ký tự LIKE literal.
- HTTP thật qua Tomcat: đăng nhập, tạo máy bằng form, gửi multipart với CSRF trong body, đọc ảnh, đăng xuất bị chặn đọc ảnh, đăng nhập lại vẫn xem hồ sơ và ảnh.
- Đối chiếu các migration có trong ZIP gốc (V1, V1_1, V2, V3, V4, V4_1): giữ nguyên byte.

H2 dùng trigger test để mô phỏng cập nhật token phiên bản; đây không phải xác nhận rowversion SQL Server, Flyway DDL hoặc các khóa UPDLOCK/HOLDLOCK trên SQL Server.

Trên máy có SQL Server, chạy:

```powershell
.\scripts\test-week11.ps1 -SqlServer
```

Sau đó chạy `sql/08_week11_verify.sql` trong SSMS và thực hiện checklist tại `TUAN11_HOAN_CHINH.md`, đặc biệt kiểm tra dữ liệu/ảnh sau khi khởi động lại ứng dụng.
