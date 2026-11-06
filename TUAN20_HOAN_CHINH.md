# TIVAREK — TUẦN 20: ỔN ĐỊNH NỀN TẢNG VÀ KIỂM THỬ LIÊN THÔNG

> Phiên bản này kế thừa toàn bộ Tuần 6–19. Giữ nguyên migration Flyway V1–V11, URL website, các vai trò và tài sản thương hiệu. Chỉ bổ sung bản vá nghiệp vụ có căn cứ, kiểm thử, chẩn đoán và quy trình nghiệm thu.

## 1. Sửa lỗi nghiệp vụ đã xác định

### W20-FIN-01: Chiết khấu cho dòng bán và hoàn tiền từng phần

Trong `dbo.SalesLines`, `DiscountAmount` là **giảm giá cho toàn bộ dòng hàng**. Trước Tuần 20, hai chỗ trong Java đã sử dụng `(UnitPrice - DiscountAmount) × Quantity`, tức trừ giảm giá nhiều lần khi số lượng trên 1:

- `SalesService.detail()`: tổng tiền đơn hàng có thể không khớp `dbo.vSaleLineAmounts`.
- `SaleWorkflowService.refund()`: hạn mức hoàn tiền khi nhận trả một phần hàng có thể bị tính sai.
- `ReportService.completedOrderValue()` và `ReportService.topProducts()`: giá trị sản phẩm trong báo cáo từng tính giảm giá theo từng đơn vị, có nguy cơ lệch khỏi giá trị dòng bán SQL.

Đã tạo `SaleRefundMath` dùng chung hai nơi. Công thức toàn dòng sau giảm giá và thuế:

`Net = UnitPrice × Quantity - DiscountAmount`

`Gross = Net + ROUND(Net × TaxRate / 100, 2)`

`MaxRefund(cumulativeReturned) = ROUND_DOWN(Gross × cumulativeReturned / Quantity, 2)`

Khi toàn bộ số lượng đã trả, hạn mức là đúng `Gross`. Phần trả một phần dùng làm tròn xuống 2 chữ số để không vượt tổng tiền. Nghiệp vụ hạch toán tiền đã có từ Tuần 15 được giữ nguyên, chỉ sửa hạn mức và cách tính tổng.

Báo cáo doanh thu hoàn tất và top sản phẩm nay lấy số tiền dòng bán từ **`dbo.vSaleLineAmounts`**, cùng nguồn với đối soát SQL Server, thay vì duy trì công thức giá riêng dễ sai. Số tiền báo cáo **không đồng nghĩa dòng tiền thực thu** — mục thu/hoàn vẫn căn cứ bảng Payments.

Ví dụ: 3 sản phẩm × 100.000đ, giảm 30.000đ cho **cả dòng**, thuế 10% → thành tiền đúng là **297.000đ**. Hoàn 1/3 số lượng được tối đa **99.000đ** (trừ những khoản đã hoàn trước).

### W20-FIN-02: Phát hiện yêu cầu thu/hoàn tiền gửi lại nhưng thay nội dung

`SaleWorkflowService` nay so sánh cả `ProviderReference` và `Status` khi gặp `IdempotencyKey` đã tồn tại. Yêu cầu cũ chỉ được trả về cùng PaymentID khi thực sự trùng nội dung, đúng hướng tiền, tài khoản đơn và trạng thái đã thành công. Nếu mã tham chiếu ngân hàng/biên nhận khác, hệ thống từ chối tái sử dụng khóa.

## 2. Bấm nút chạy/test trên Windows

| Tệp | Tác vụ | Có chỉnh sửa dữ liệu? |
| --- | --- | --- |
| `CHANDOAN_TUAN20.cmd` | Kiểm tra Maven Wrapper, JDK 21 và TCP SQL Server | Không |
| `KIEM_THU_TUAN20.cmd` | Build và toàn bộ JUnit, test H2 | Không đụng SQL Server thật |
| `CHAY_TUAN20.cmd` | Chạy Spring Boot chế độ DEV localhost | Có thể ghi DB DEV khi thao tác nghiệp vụ |
| `KIEM_THU_WEB_TUAN20.cmd` | 21 kiểm tra GET và POST không CSRF | Không ghi dữ liệu |
| `scripts/check-week20.sql` | SQL read-only: chênh lệch tiền/tồn kho, giảm giá, hoàn tiền | Chỉ SELECT |

### Chạy theo thứ tự

1. Cài và chọn **JDK 21**. Bấm `CHANDOAN_TUAN20.cmd`. Nếu SQL Server dùng cổng khác 1433, chạy `powershell -File .\scripts\diagnose-week20.ps1 -DbPort <port>` và dùng đúng JDBC URL DEV.
2. Bấm `KIEM_THU_TUAN20.cmd`; yêu cầu `BUILD SUCCESS`, đọc `target\surefire-reports` nếu FAIL.
3. Bấm `CHAY_TUAN20.cmd`, nhập thông tin **SQL DEV** khi được hỏi, chờ ứng dụng khởi động.
4. Giữ cửa sổ server mở, bấm `KIEM_THU_WEB_TUAN20.cmd`; yêu cầu 21/21 HTTP probes PASS.
5. Trong SSMS, chọn database DEV/copy, chạy `scripts/check-week20.sql`. Các cảnh báo quan trọng phải được điều tra trước khi tiếp tục.

`CHAY_TUAN20.cmd` kế thừa cách chạy DEV an toàn từ Tuần 19; không tự gọi launcher production, không cần thiết lập `TIVAREK_DB_URL` để chạy DB huấn luyện mặc định.

## 3. Lịch kiểm thử 7 ngày

### Ngày 1 — Môi trường và build

- [ ] JDK 21, Maven Wrapper, SQL Server DEV khả dụng.
- [ ] `clean verify` thành công với tất cả test W8–W20.
- [ ] Dev startup thành công, `/actuator/health` báo `UP`.
- [ ] 21 HTTP probes thành công; không bị chuyển nhầm sang production.

### Ngày 2 — Bán hàng và tồn kho

- [ ] Sản phẩm BULK và máy cũ SERIALIZED có tồn thực tế.
- [ ] Đặt hàng và xác nhận có giữ tồn; không thể bán âm kho.
- [ ] Hai người thử giữ cùng một máy: chỉ một thành công.
- [ ] Bấm xác nhận hai lần: lần thứ hai bị từ chối an toàn.
- [ ] Bàn giao, xuất kho, hủy đơn theo đúng trạng thái; không có chứng từ trùng.

### Ngày 3 — Thu tiền và hoàn tiền

- [ ] Thu tiền mặt/chuyển khoản xác minh thủ công; không vượt số tiền đơn.
- [ ] Gửi lại **cùng IdempotencyKey, cùng nội dung**: không sinh payment mới.
- [ ] Gửi lại **cùng IdempotencyKey, khác mã tham chiếu**: phải bị từ chối.
- [ ] Thử hoàn một phần sau khi khách trả hàng, kiểm tra hạn mức theo số lượng đã nhận trả.
- [ ] Nếu có chiết khấu cho cả dòng, tổng Java và `dbo.vSaleLineAmounts` phải khớp.
- [ ] Hoàn nhiều lần không vượt số tiền đã thu và giá trị hàng được trả.

### Ngày 4 — Sửa chữa và bảo hành

- [ ] Khách tạo yêu cầu sửa, nhân viên tiếp nhận.
- [ ] Lập báo giá, khách xác nhận, kỹ thuật sử dụng linh kiện và bàn giao.
- [ ] Bảo hành phát hành đúng điều kiện; tra cứu token đúng/sai.
- [ ] Không cho phép người dùng khác xem phiếu/đơn riêng tư.

### Ngày 5 — Báo cáo, dữ liệu và bảo mật

- [ ] Đối chiếu báo cáo bán hàng với số tiền thu/hoàn của đơn test.
- [ ] Chạy `scripts/check-week20.sql`; phân loại cảnh báo quan trọng và cảnh báo đối chiếu thủ công.
- [ ] Kiểm tra quyền nhân viên kho, bán hàng, kỹ thuật, khách hàng.
- [ ] Kiểm tra không thể POST thay đổi dữ liệu khi không có CSRF.

### Ngày 6 — Sao lưu và thử phục hồi

- [ ] Sao lưu DB và ảnh bằng các script Tuần 18.
- [ ] `RESTORE VERIFYONLY` thành công.
- [ ] Thử khôi phục vào database mới, thư mục ảnh mới; **không ghi đè dữ liệu gốc**.
- [ ] Ghi thời gian khôi phục và người xác nhận vào phiếu UAT.

### Ngày 7 — Nghiệm thu

- [ ] Toàn bộ lỗi P0 đã đóng.
- [ ] Chạy lại Maven + HTTP smoke và lưu log/ảnh đã che dữ liệu nhạy cảm.
- [ ] Xác nhận GO/NO-GO bằng `PHIEU_UAT_TUAN20.md`.
- [ ] Gắn nhãn phiên bản `TIVAREK_W20_STABLE` chỉ sau khi mọi bước bắt buộc PASS.

## 4. Giới hạn xác minh

Những bài test mới về toán tiền có thể kiểm tra riêng, nhưng **chưa được xem là đã nghiệm thu toàn hệ thống** nếu chưa chạy JDK 21, SQL Server, Flyway và kiểm tra trên Windows thực tế. Bản ZIP không tự thay đổi cấu hình database đang có và không tự chạy migration mới.
