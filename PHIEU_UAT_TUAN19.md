# PHIẾU NGHIỆM THU TIVAREK — TUẦN 19

Ngày thực hiện: ____ / ____ / 2026  |  Người nghiệm thu: __________  |  Môi trường: DEV / RESTORE TEST

**Cách ghi:** Chọn `PASS`, `FAIL`, hoặc `CHƯA THỬ`, bổ sung minh chứng/ghi chú. Dùng dữ liệu giả. Không chụp password hoặc token.

| ID | Vai trò | Kịch bản thử | Kỳ vọng | Kết quả | Ghi chú / bằng chứng |
| --- | --- | --- | --- | --- | --- |
| U01 | Người phát triển | `KIEM_THU_TUAN19.cmd` | `BUILD SUCCESS` | CHƯA THỬ | |
| U02 | Người phát triển | `KIEM_THU_WEB_TUAN19.cmd` khi server đã chạy | 21/21 probes PASS | CHƯA THỬ | |
| U03 | Kho | Nhập kho sản phẩm BULK | OnHand và Available tăng đúng | CHƯA THỬ | |
| U04 | Bán hàng | Đặt, xác nhận, giao đơn BULK | Xuất kho đúng; không ghi trùng | CHƯA THỬ | |
| U05 | Bán hàng | Đặt hai đơn giữ cùng một máy SERIALIZED | Chỉ một đơn được giữ máy | CHƯA THỬ | |
| U06 | Bán hàng | Số lượng vượt tồn / xác nhận hai lần | Báo lỗi an toàn, không âm kho | CHƯA THỬ | |
| U07 | Chủ tiệm | Hoàn tiền / trả hàng có kiểm soát | Có lịch sử và báo cáo đối soát | CHƯA THỬ | |
| U08 | Khách | Gửi yêu cầu sửa trên `/sua-chua` | Sinh yêu cầu hợp lệ | CHƯA THỬ | |
| U09 | Kỹ thuật | Tiếp nhận, báo giá, khách phản hồi, sửa, bàn giao | Trạng thái và lịch sử đúng | CHƯA THỬ | |
| U10 | Khách | Tra cứu phiếu sửa bằng mã/token giả | Đúng phiếu; token sai bị từ chối | CHƯA THỬ | |
| U11 | Chủ tiệm | Phát hành, tra cứu, tiếp nhận claim bảo hành | Trạng thái đúng; không lộ PII | CHƯA THỬ | |
| U12 | Khách chưa đăng nhập | Mở `/admin/stock`, `/admin/orders`, `/account/orders` | Chuyển tới `/login` | CHƯA THỬ | |
| U13 | Kho | Mở báo cáo tài chính và phiếu sửa | HTTP 403, không lộ dữ liệu | CHƯA THỬ | |
| U14 | Người phát triển | POST không CSRF | HTTP 403 | CHƯA THỬ | |
| U15 | Chủ tiệm | Đối chiếu số đơn, thu chi, tồn trong báo cáo | Khớp giao dịch DEV | CHƯA THỬ | |
| U16 | Người phát triển | Chạy `scripts/check-week19.sql` trên DB sao chép | Các `Findings` P0 = 0 | CHƯA THỬ | |
| U17 | Người quản trị | Backup DB và ảnh, thử restore vào DB + thư mục mới | Đọc được bảng và hình ảnh | CHƯA THỬ | |
| U18 | Gia đình | Desktop 1440px, mobile 390px | Không vỡ bố cục; form và nút dùng được | CHƯA THỬ | |
| U19 | Gia đình | Kiểm tra thương hiệu, số `0335 135 549`, email | Đúng thông tin | CHƯA THỬ | |
| U20 | Chủ tiệm | Thử quy trình nhận đơn/sửa mô phỏng trong một ca | Không phát sinh lỗi nghiêm trọng | CHƯA THỬ | |

**Lỗi tồn đọng và người chịu trách nhiệm:**

| Mã lỗi | Mức P0/P1/P2 | Mô tả | Người xử lý | Tình trạng |
| --- | --- | --- | --- | --- |
| | | | | |

**Quyết định:** GO / NO-GO / CHỜ SỬA

Người phụ trách kỹ thuật: __________________ | Chủ cửa hàng: __________________ | Ngày: __________________
