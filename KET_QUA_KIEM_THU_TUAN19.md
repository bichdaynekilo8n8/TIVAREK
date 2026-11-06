# TIVAREK — KẾT QUẢ KIỂM THỬ TUẦN 19

## Kiểm tra có thể tự động hóa

| Kiểm tra | Cách thực hiện | Trạng thái trong gói bàn giao |
| --- | --- | --- |
| Java/JUnit với H2 | `KIEM_THU_TUAN19.cmd` → Maven `clean verify` | CHƯA CHẠY trong môi trường Windows/JDK21 của chủ dự án |
| HTTP công khai / phân quyền / CSRF | `KIEM_THU_WEB_TUAN19.cmd` → `smoke-week19.ps1` | CHƯA CHẠY với SQL Server thật |
| SQL invariants read-only | `scripts/check-week19.sql` trong SSMS | CHƯA CHẠY trên database của chủ dự án |
| Restore DB và hình ảnh vào môi trường tách biệt | Theo `CHECKLIST_VAN_HANH.md` | CHƯA DIỄN TẬP |
| UAT nghiệp vụ và màn hình trên thiết bị thật | `PHIEU_UAT_TUAN19.md` | CHƯA NGHIỆM THU |

## Những thay đổi Tuần 19 trong mã nguồn

- Thêm `Week19SecurityAcceptanceTest`: trang công khai, chặn truy cập ẩn danh đến đơn hàng/phiếu sửa/bảo hành/report, phân quyền nhân viên kho, bắt buộc CSRF.
- Thêm `Week19CartIsolationTest`: giỏ hàng giữa hai phiên độc lập, máy SERIALIZED chỉ số lượng 1.
- Thêm `scripts/smoke-week19.ps1`: 21 kiểm tra HTTP, không có POST hợp lệ và không thay đổi database.
- Thêm `scripts/check-week19.sql`: các cảnh báo kho âm, subtype dòng bán, reservation hết hạn/không hợp lệ, trùng dòng, warranty/payments; chỉ SELECT.
- Thêm 3 file `.cmd` chạy, test Maven và smoke, tách riêng DEV và PROD để tránh lỗi biến môi trường production khi chạy local.
- Thêm checklist vận hành, UAT và tài liệu hướng dẫn 7 ngày.

## Quy ước báo cáo

Không đánh dấu PASS chỉ vì đã viết test hoặc tạo tệp. Chỉ cập nhật **PASS** sau khi chạy thật, lưu `BUILD SUCCESS`, `PASS: 21 non-destructive HTTP checks.`, kết quả SSMS và bằng chứng thử restore. Nếu có lỗi, giữ lại log và gửi cho người phát triển (xóa password/token trước khi gửi).
