# PHIẾU UAT TUẦN 24 — CHƯA XÁC NHẬN

Người kiểm thử: __________  Thiết bị/OS/Chrome: __________  Ngày: __________
Database DEV: __________  Phiên bản ZIP/commit: __________

Dùng dữ liệu thử. Không ghi mật khẩu, token tra cứu hoặc thông tin khách hàng thật vào phiếu/chụp ảnh. Ô trống có nghĩa là chưa kiểm thử, không có nghĩa là đạt.

| # | Tình huống và kết quả mong đợi | Đạt / Lỗi / Chưa thử | Bằng chứng |
|---|---|---|---|
| 1 | clean verify JDK 21 và kiểm thử SQL Server riêng chạy thành công | | |
| 2 | App khởi động bằng SQL Server DEV, Flyway validate sạch | | |
| 3 | Home, tìm kiếm, lọc, sort, phân trang hoạt động; URL/ảnh fingerprint trả 200 | | |
| 4 | Ảnh lớn/ảnh trong suốt/ảnh lỗi/ảnh thiếu; không vỡ layout | | |
| 5 | Chi tiết hàng còn/hết/ẩn, ảnh và specs đúng; hàng ẩn trả 404 | | |
| 6 | Giỏ hàng, checkout, tạo đơn, refresh/back không gửi lặp ngoài dự kiến | | |
| 7 | Đăng ký sửa, tra cứu đúng/sai mã, lỗi validation và thông báo hỗ trợ rõ | | |
| 8 | Bảo hành và trang tài khoản không cho truy cập dữ liệu người khác | | |
| 9 | Quyền OWNER / SALES / INVENTORY / TECHNICIAN / CUSTOMER đúng ma trận | | |
| 10 | 320/390/768/1440px, portrait/landscape, chữ dài: không cuộn ngang viewport | | |
| 11 | Chỉ bàn phím: skip link, Tab/Shift+Tab, menu details, form, focus không bị che/kẹt | | |
| 12 | Axe tags A/AA không còn violation trong phạm vi, kiểm tra các mục incomplete thủ công | | |
| 13 | Screen reader/NVDA: tên control, heading, landmark, label/lỗi, thông báo đọc được | | |
| 14 | Zoom 200% và 400%, reduced motion, tương phản và kích thước target phù hợp | | |
| 15 | Mạng chậm/mất mạng/DB tắt/timeout: lỗi rõ, không tạo đơn trùng hay lộ SQL | | |
| 16 | Lighthouse desktop/mobile mỗi trang 3 lượt, giữ report before/after | | |
| 17 | CSS/JS cache được; thay file tạo hash mới; ảnh upload ẩn không được lấy từ cache công khai | | |
| 18 | HTML, API csrf, kết quả tra cứu, lỗi, redirect có no-store/noindex đúng phạm vi | | |
| 19 | Canonical/sitemap chỉ dùng domain thật; LocalBusiness khớp nội dung đã xác nhận | | |
| 20 | Pages trên URL thật tải ảnh, liên hệ, mobile; không có nghiệp vụ SQL giả lập | | |

## Quyết định triển khai

- Lỗi còn mở / mức độ / người xử lý: ____________________________________
- Phạm vi đã kiểm thử và phạm vi chưa kiểm thử: ___________________________
- Chủ cửa hàng xác nhận tên, địa chỉ, điện thoại, email: ____________________
- Người kiểm thử xác nhận: ____________________ Ngày: ____________________
- Chủ tiệm/người phê duyệt: ___________________ Ngày: ____________________
- Quyết định: [ ] Cho phép triển khai  [ ] Chưa cho phép

Phiếu này được bàn giao chưa ký; không thay người dùng xác nhận UAT.
