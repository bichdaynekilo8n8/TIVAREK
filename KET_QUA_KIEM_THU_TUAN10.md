# Kết quả kiểm thử Tuần 10

Ngày: 04/11/2026. Môi trường: JDK 21.0.13, Maven 3.9.11, Spring Boot 4.1.2.

Đã biên dịch toàn bộ nguồn và chạy Maven `verify` thành công, bao gồm tạo executable JAR. Gói bàn giao chứa mã nguồn; JAR được Maven tạo lại trong `target/` khi build.

**48 ca thực thi đạt, 0 lỗi, 0 thất bại.** 24 ca SQL Server opt-in được bỏ qua vì môi trường hiện tại không có SQL Server và thông tin kết nối của bạn. Không coi kết quả H2 là xác nhận SQL Server thật.

| Nhóm | Tổng ca | Thất bại | Lỗi | Bỏ qua |
|---|---:|---:|---:|---:|
| ProductManagementTest | 15 | 0 | 0 | 0 |
| SqlServerProductTest | 11 | 0 | 0 | 11 |
| HttpLoginTest | 1 | 0 | 0 | 0 |
| MethodAuthorizationTest | 3 | 0 | 0 | 0 |
| RouteAuthorizationTest | 12 | 0 | 0 | 0 |
| SecurityDatabaseTest | 17 | 0 | 0 | 0 |
| SqlServerSecurityTest | 13 | 0 | 0 | 13 |

Các luồng đã kiểm tra:

- Đăng nhập qua HTTP thật với cookie và CSRF, đổi session ID, tự vào `/admin`, lưu sản phẩm, mở các trang quản lý, đăng xuất rồi đăng nhập lại vẫn đọc được sản phẩm từ DB test.
- Phân quyền Tuần 9–10: chủ cửa hàng, quản lý kho, bán hàng, kỹ thuật và khách hàng; chặn truy cập trực tiếp API và form thiếu quyền.
- Thêm/sửa/ngừng/kinh doanh lại sản phẩm; kiểm tra tài khoản thực hiện trong audit.
- SKU trùng khác hoa/thường/khoảng trắng; unique constraint chặn cả ghi trực tiếp vào DB test.
- Tên rỗng, giá âm, số lẻ quá giới hạn, StockMode sai, FK không tồn tại.
- Danh mục/thương hiệu/đơn vị: mã trùng, sửa tên, chặn ngừng khi còn sản phẩm hoạt động, chặn tham chiếu đã ngừng, dùng lại.
- Phiên bản cũ không thể ghi đè tên hoặc trạng thái mới.
- Tìm kiếm, bộ lọc, phân trang 25 dòng, ký tự `%`/`_` dạng literal và đầu vào SQL injection.
- Render các template mới, thêm qua form, hiển thị dữ liệu cũ khi đầu vào không hợp lệ.
- Thiếu CSRF bị chặn; quyền ở service tiếp tục chặn ghi khi gọi trực tiếp.
- Ghi audit thất bại làm rollback thay đổi sản phẩm.
- Đối chiếu byte các migration V1, V1_1, V2, V3 với ZIP gốc: giữ nguyên.

Để xác nhận migration V4, unique computed index, JDBC generated keys và khóa giao dịch hoạt động trên SQL Server máy bạn, chạy:

```powershell
.\scripts\test-week10.ps1 -SqlServer
```

Sau đó chạy `sql/07_week10_verify.sql` trong SSMS và làm các bước nghiệm thu ở `TUAN10_HOAN_CHINH.md`.
