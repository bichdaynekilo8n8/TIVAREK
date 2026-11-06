# KẾT QUẢ KIỂM THỬ TUẦN 20

## Đã thực hiện trên mã nguồn

- Sửa phép tính chiết khấu dòng bán ở `SalesService.detail()` để khớp với `dbo.vSaleLineAmounts`.
- Sửa giới hạn hoàn tiền cộng dồn theo tỷ lệ hàng đã nhận trả ở `SaleWorkflowService.refund()`.
- Đồng bộ hai báo cáo `ReportService.completedOrderValue()`/`topProducts()` với view SQL `dbo.vSaleLineAmounts`.
- Thêm kiểm tra idempotency gồm trạng thái và mã tham chiếu giao dịch ở thu/hoàn tiền.
- Thêm kiểm thử JUnit của `SaleRefundMath`, `SalesService.detail()`, báo cáo và yêu cầu chống lặp thanh toán.
- Thêm chẩn đoán Windows, script SQL SELECT-only, launcher DEV và biểu mẫu nghiệm thu W20.

## Trạng thái nghiệm thu

| Kiểm tra | Trạng thái | Lý do / cách xác nhận |
| --- | --- | --- |
| Kiểm tra kết cấu ZIP | Chưa ghi | Xác minh bằng `unzip -t` trước khi bàn giao |
| Maven JDK 21 `clean verify` | CHƯA XÁC NHẬN | Chạy `KIEM_THU_TUAN20.cmd` trên máy Windows JDK21 |
| App chạy với SQL Server DEV | CHƯA XÁC NHẬN | Chạy `CHAY_TUAN20.cmd`, kiểm tra console |
| HTTP smoke | CHƯA XÁC NHẬN | Chạy `KIEM_THU_WEB_TUAN20.cmd` khi ứng dụng đang hoạt động |
| SQL tồn kho/tiền | CHƯA XÁC NHẬN | Chạy `scripts/check-week20.sql` trong SSMS trên DB DEV/copy |
| Backup/restore | CHƯA XÁC NHẬN | Thực hiện riêng theo `CHECKLIST_VAN_HANH.md` |
| UAT gia đình | CHƯA XÁC NHẬN | Điền `PHIEU_UAT_TUAN20.md` |

**Không đánh dấu GO trước khi các bài kiểm thử P0 PASS và đã thử khôi phục dữ liệu.**
