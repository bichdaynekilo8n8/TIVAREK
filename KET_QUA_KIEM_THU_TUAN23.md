# TIVAREK — Kết quả kiểm thử Tuần 23

## Đã thực hiện trên workspace

- [x] Không thay đổi migrations SQL Server, DB schema, endpoint, cơ chế đặt hàng hoặc các role.
- [x] Nâng cấp trang chủ, đăng ký sửa, tra cứu, giỏ hàng, thanh toán, navbar và GitHub Pages.
- [x] Gắn CSS dùng chung cho stylesheet của các tuần cũ.
- [x] Bổ sung test `Week23HomeControllerTest` và mock cho test MVC `RouteAuthorizationTest` để tránh thiếu dependency sau thay đổi `HomeController`.
- [x] Thêm công cụ smoke GET-only và checklist UAT.

## Chưa thể chạy xác nhận trong container hiện tại

- [ ] `mvnw.cmd clean verify` bằng JDK 21: môi trường chỉ có **Java 17 runtime**, không có Java 21 compiler.
- [ ] Spring Boot kết nối SQL Server thật: môi trường không có máy chủ SQL Server DEV.
- [ ] Luồng tạo đơn, tồn kho, chi tiết sửa: không được phép tuyên bố PASS nếu chưa thực nghiệm trên DB sao lưu.
- [ ] Kiểm tra thị giác trên Chrome/Edge/Firefox và thiết bị thật.

## Tiêu chuẩn hoàn thành

1. Trên Windows JDK 21 chạy `KIEM_THU_TUAN23.cmd` và ghi nhận `BUILD SUCCESS`.
2. Khởi động DEV bằng `CHAY_TUAN23.cmd` và chạy `scripts/smoke-week23-ui.ps1` phải PASS 8/8.
3. Thực hiện `PHIEU_UAT_TUAN23.md`, đặc biệt kiểm thử giao hàng cần địa chỉ, thẻ CSRF và phiếu sửa bảo mật.
4. So sánh tổng tiền, trạng thái và số lượng với SQL Server trước/sau thao tác.
5. Chỉ đánh dấu sẵn sàng production khi đã có xác nhận của chủ cửa hàng.

**Tình trạng:** ĐÃ TÍCH HỢP MÃ NGUỒN / CHƯA ĐƯỢC CHỨNG NHẬN CHẠY TRÊN JDK21 + SQL SERVER.
