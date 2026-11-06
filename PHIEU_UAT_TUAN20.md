# PHIẾU UAT TUẦN 20 — TIVAREK

Ngày: ________  |  Người thực hiện: ________  |  Database: DEV / RESTORE COPY

| ID | Kịch bản | Kết quả yêu cầu | Trạng thái |
| --- | --- | --- | --- |
| W20-01 | JDK 21 và `clean verify` | BUILD SUCCESS, không có test FAIL | CHƯA CHẠY |
| W20-02 | Dev startup và smoke | Health UP, 21 HTTP probe PASS | CHƯA CHẠY |
| W20-03 | Hai người giữ một máy | Không có hai reservation hợp lệ đồng thời | CHƯA CHẠY |
| W20-04 | Mua vượt tồn kho | Bị từ chối; không âm Available | CHƯA CHẠY |
| W20-05 | Gửi lại yêu cầu thu tiền giống hệt | Cùng PaymentID, không phát sinh thu trùng | CHƯA CHẠY |
| W20-06 | Cùng idempotency key, khác bank reference | Bị từ chối | CHƯA CHẠY |
| W20-07 | Giảm giá cả dòng: 3×100k -30k, tax 10% | Tổng đơn 297k | CHƯA CHẠY |
| W20-08 | Trả 1/3 số lượng trên dòng ở W20-07 | Hạn mức hoàn cộng dồn 99k | CHƯA CHẠY |
| W20-09 | Trả đủ 3/3 | Hạn mức cộng dồn 297k | CHƯA CHẠY |
| W20-10 | Đặt sửa → báo giá → bàn giao | Chuyển trạng thái chính xác | CHƯA CHẠY |
| W20-11 | Tra cứu bảo hành sai token | Không lộ dữ liệu | CHƯA CHẠY |
| W20-12 | Nhân viên kho xem tài chính | 403, không lộ báo cáo | CHƯA CHẠY |
| W20-13 | SQL read-only diagnostics | Không có cảnh báo nghiêm trọng | CHƯA CHẠY |
| W20-14 | Backup & restore vào DB riêng | Khôi phục thành công, ảnh đọc được | CHƯA CHẠY |

Lỗi tồn tại và mức ưu tiên: ____________________________________________________

Quyết định: **GO / NO-GO**

Chữ ký kỹ thuật: ___________________  Chủ cửa hàng: ___________________
