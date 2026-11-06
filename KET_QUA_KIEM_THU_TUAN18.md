# Kết quả kiểm tra mã nguồn TIVAREK — Tuần 18

**Ngày kiểm tra:** 06/11/2026.

## Đã kiểm tra thành công trong môi trường xây dựng bản ZIP

- Cấu trúc XML `pom.xml` hợp lệ.
- Workflow GitHub Actions đọc được bằng YAML parser.
- 7 file HTML trang chủ, danh mục, chi tiết, fragment và Pages đọc được bằng HTML parser.
- Đã kiểm tra 3 ảnh PNG (icon, logo ngang, danh thiếp) trong mỗi vị trí: `src/main/resources/static/images/` và `docs/assets/`. Các cặp ảnh trùng khớp byte.
- Link ảnh tương đối trên GitHub Pages đều trỏ đến file có thực.
- Dấu ngoặc `{}` trong 2 CSS chính cân bằng.
- Trang chủ và Pages đều có SĐT, email liên hệ; SecurityConfig công khai health endpoint theo GET, chỉ endpoint health được bật tại Actuator.

## Chưa thể xác nhận ở môi trường hiện tại

- Môi trường xây dựng ZIP chỉ có Java 17, **không có JDK 21, Maven CLI/Windows PowerShell hay SQL Server**. Do đó chưa thể chạy Maven `clean verify`, chạy JAR hay nghiệm thu transaction thực tế.
- Workflow GitHub Actions chỉ chạy sau khi người sở hữu repo push code lên GitHub. Không có quyền/ngữ cảnh để tự bật GitHub Pages hay xuất bản Tunnel.
- Script PowerShell backup, restore verify và smoke test cần chạy trên **Windows máy bạn** với Java 21, SQL Server, `sqlcmd` và database thật.

## Bắt buộc làm tiếp trước khi dùng thật

1. Chạy `.\mvnw.cmd -B -ntp -Dspring.profiles.active=test clean verify` trên Windows với JDK 21.
2. Kiểm tra DB production bằng tài khoản hạn chế quyền; sao lưu trước khi Flyway chạy lần đầu.
3. Chạy `.\scripts\start-week18.ps1 -Build`, rồi `.\scripts\smoke-week18.ps1`.
4. Test riêng đăng ký, đăng nhập, đặt hàng, phiếu sửa, tra cứu và bảo hành; kiểm tra role chủ cửa hàng và nhân viên.
5. Kiểm tra hiển thị trên di động, xác nhận thông tin liên hệ đúng; sau đó mới bật Pages hoặc Tunnel.

**Không kết luận “100% không lỗi” trước khi chạy đủ các bước trên.**
