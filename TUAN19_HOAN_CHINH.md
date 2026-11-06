# TIVAREK — TUẦN 19: NGHIỆM THU TOÀN HỆ THỐNG VÀ VẬN HÀNH THỬ

> Phiên bản này kế thừa **toàn bộ Tuần 6–18**. Tuần 19 bổ sung kiểm thử tự động xuyên mô-đun, smoke test HTTP chỉ đọc, SQL chẩn đoán chỉ đọc, biểu mẫu nghiệm thu và quy trình vận hành. Không sửa dữ liệu thật hoặc tự động đưa hệ thống lên Internet.

## 0. Điều kiện tiên quyết

1. Windows 10/11; **JDK 21** (`java -version`); SQL Server đang hoạt động (trên máy hoặc máy chủ của bạn).
2. Tạo database và tài khoản DEV theo hướng dẫn Tuần 8/9 nếu chưa có. **Không dùng database thật cho dữ liệu nghiệm thu giả**.
3. Mở PowerShell tại `D:\Projects\TIVAREK`. Nếu mới thay mã nguồn, giải nén bản ZIP đầy đủ vào đúng thư mục dự án; không chép lồng thêm một thư mục `TIVAREK`.
4. Sao lưu DB và thư mục hình ảnh trước khi thử thao tác bán, sửa, bảo hành, trả hàng. Thao tác thử nghiệm phải dùng database DEV hoặc bản sao được khôi phục độc lập.

## 1. Lối tắt Windows — không nhầm DEV và PROD

| File | Tác dụng | Cần SQL Server? |
| --- | --- | --- |
| `KIEM_THU_TUAN19.cmd` | `mvnw.cmd -B -ntp clean verify` với profile test/H2 | Không |
| `CHAY_TUAN19.cmd` | Khởi động **DEV** bằng launcher hiện có, hỏi mật khẩu SQL DEV | Có |
| `KIEM_THU_WEB_TUAN19.cmd` | Smoke HTTP sau khi DEV chạy ở `http://127.0.0.1:8080` | Có (ứng dụng đang chạy) |
| `CHAY_TUAN18_PROD.cmd` | Khởi động **PROD**; yêu cầu biến môi trường thật | Có |

Không khởi động `CHAY_TUAN18_PROD.cmd` trong lúc đang học/chạy DEV. Lỗi `Missing environment variable: TIVAREK_DB_URL` ở launcher PROD **là kiểm tra bảo vệ hợp lệ**: `.env.example` chỉ là tài liệu, không được tự động nạp. Dùng `CHAY_TUAN19.cmd` để chạy DEV hoặc khai báo đầy đủ biến môi trường được liệt kê bên dưới để chạy PROD.

### Kiểm thử dự án

```powershell
cd D:\Projects\TIVAREK
java -version
.\mvnw.cmd -B -ntp clean verify
```

Mong đợi: `BUILD SUCCESS`; `target\surefire-reports` chứa kết quả JUnit. Tuần 19 có hai lớp test mới: `Week19SecurityAcceptanceTest`, `Week19CartIsolationTest`. Các bài trước vẫn giữ nguyên.

### Chạy DEV và thử HTTP

1. Nhấp đôi `CHAY_TUAN19.cmd` hoặc chạy `.\CHAY_TUAN19.cmd`; nhập thông tin SQL DEV khi được hỏi.
2. Khi Tomcat báo đã khởi động, giữ nguyên cửa sổ ứng dụng.
3. Mở **cửa sổ PowerShell thứ hai** rồi chạy:

```powershell
.\scripts\smoke-week19.ps1 -BaseUrl 'http://127.0.0.1:8080'
```

Smoke gồm 21 kiểm tra **không làm thay đổi dữ liệu**: trang công khai, logo, số điện thoại, email, health, điều hướng khách chưa đăng nhập, CSRF cho POST không có token. Nếu kết quả khác kỳ vọng, dừng nghiệm thu và kiểm tra console ứng dụng. Khi chạy dưới domain khác, thay `-BaseUrl` cho phù hợp.

`CHAY_TUAN19.cmd` cố ý bỏ các biến SQL đang kế thừa từ máy để **chỉ kết nối `TIVAREK_WEEK8_DEV` ở localhost**, tránh vô tình thử nghiệm trên PROD. Nếu DB DEV của bạn có tên/địa chỉ khác, gọi `scripts/start-week9.ps1` trực tiếp với `TIVAREK_DB_URL` DEV đã kiểm tra, không sửa launcher thành URL sản xuất.

### Chạy PROD có chủ đích

Chỉ thực hiện sau khi test H2 + smoke DEV + sao lưu thành công. Chuẩn bị các biến **trong phiên PowerShell hiện tại**; giữ password ngoài source/git:

```powershell
$env:TIVAREK_DB_URL = 'jdbc:sqlserver://127.0.0.1:1433;databaseName=TIVAREK_PROD;encrypt=true;trustServerCertificate=true'
$env:TIVAREK_DB_USER = 'your_prod_sql_login'
$dbSecret = Read-Host 'SQL Server PROD password' -AsSecureString
$env:TIVAREK_DB_PASSWORD = [System.Net.NetworkCredential]::new('', $dbSecret).Password
$env:TIVAREK_UPLOAD_DIR = 'D:\TIVAREK_UPLOADS'
.\CHAY_TUAN18_PROD.cmd
```

Sửa SQL Server, database và đường dẫn phù hợp thực tế; với máy chủ kết nối qua Internet, cần **chứng chỉ SQL Server đáng tin cậy và kiểm tra encryption**, không dùng `trustServerCertificate=true` như một cấu hình bảo mật sản xuất. Launcher PROD mặc định bind vào `127.0.0.1`; muốn công khai cần reverse proxy HTTPS, kiểm soát firewall và cấu hình triển khai riêng.

## 2. Quy trình 7 ngày — việc phải làm và tiêu chí hoàn thành

### Ngày 1 — Xác minh nền tảng

- [ ] JDK 21, SQL Server DEV chạy ổn định; kết nối DB và Flyway áp dụng V1–V11 không lỗi.
- [ ] `KIEM_THU_TUAN19.cmd` báo `BUILD SUCCESS`.
- [ ] `CHAY_TUAN19.cmd` chạy được; `KIEM_THU_WEB_TUAN19.cmd` báo PASS.
- [ ] Cả `target` và `.env` nằm ngoài bản bàn giao Git/ZIP công khai; không lộ mật khẩu, token.

### Ngày 2 — Quy trình bán hàng + kho

Trên **DB DEV**, đăng nhập đúng vai trò để: tạo sản phẩm BULK và thiết bị SERIALIZED → nhập kho → bật hiển thị → đặt hàng → xác nhận → giao hàng/xuất kho → ghi nhận thanh toán → kiểm tra báo cáo doanh thu. Kiểm tra riêng các tình huống từ chối: mua vượt tồn, hai khách cùng giữ máy, xác nhận/giao trùng, hủy đơn đã giao và hoàn trả. Mỗi lần đối chiếu tồn kho với `dbo.vBulkAvailable`, lịch sử nhập/xuất và trạng thái đơn. Không tạo hai đơn thanh toán thật để mô phỏng.

### Ngày 3 — Quy trình tiếp nhận sửa + bảo hành

Khách gửi `/sua-chua` → nhân viên tiếp nhận `/admin/repairs` → lập và gửi báo giá → khách chấp nhận/từ chối → sửa/chờ linh kiện → bàn giao → phát hành bảo hành → tra cứu `/tra-cuu-bao-hanh` → gửi yêu cầu bảo hành. Kiểm tra không thể dùng mã tra cứu thiếu token, không thể chuyển trạng thái trái quy trình, quyền xem phiếu của khách khác bị chặn. Lưu mã phiếu thử nghiệm tại file nghiệm thu riêng; **không chép token lên GitHub**.

### Ngày 4 — Phân quyền, CSRF, dữ liệu cá nhân

- Khách chưa đăng nhập truy cập trang công khai; các trang `/admin/*`, `/account/*` chuyển sang đăng nhập.
- Tài khoản kho không xem được tài chính, phiếu sửa; tài khoản kỹ thuật không tự ý ghi doanh thu; khách chỉ đọc dữ liệu của chính mình.
- POST không có CSRF bị chặn, đăng nhập sai không lộ lỗi kỹ thuật, session/logout hoạt động.
- Chạy `Week19SecurityAcceptanceTest`; xem thêm ma trận ở `PHIEU_UAT_TUAN19.md`.

### Ngày 5 — SQL chẩn đoán + sao lưu/khôi phục

1. Trong SSMS **chọn database COPY/DEV** và chạy `scripts/check-week19.sql`. Kết quả `[Findings]` nên bằng 0; nếu có cảnh báo, tra cứu bản ghi bằng tài khoản được phân quyền, không xóa dữ liệu tự động.
2. Sao lưu bằng `scripts/backup-week18.ps1` và `scripts/backup-images-week18.ps1`; kiểm tra `RESTORE VERIFYONLY` và tính đầy đủ của ảnh.
3. Khôi phục **một bản sao database vào database mới** và một bản sao ảnh sang thư mục riêng; làm theo `CHECKLIST_VAN_HANH.md`. Tuyệt đối không `RESTORE ... WITH REPLACE` lên DB gốc.
4. Chụp ảnh màn hình tên DB thử nghiệm, xác nhận đọc được dữ liệu, rồi ghi thời gian phục hồi vào phiếu UAT. Không lưu password trong ảnh.

### Ngày 6 — Giao diện, di động và đường dẫn công khai

Kiểm tra trên desktop 1440px và mobile 390px: trang chủ, danh sách sản phẩm, giỏ hàng, đặt hàng, sửa chữa, tra cứu bảo hành và trang quản trị. Kiểm tra ảnh logo/wordmark, số `0335 135 549`, `tivarek@gmail.com`, footer, favicon, nút bấm, thông báo lỗi, trạng thái rỗng. Chỉ khi có server Spring Boot thực sự mới được kết nối trang tĩnh GitHub Pages qua API CORS được phép.

**Lưu ý:** GitHub Pages chỉ lưu nội dung tĩnh, **không chạy được Spring Boot/SQL Server**. Không đăng GitHub Pages như thể quy trình đặt hàng và quản lý dữ liệu đã vận hành thật. Không công khai endpoint quản trị nếu chưa có HTTPS, firewall, tài khoản và bản sao lưu.

### Ngày 7 — Nghiệm thu gia đình + chạy thử tại cửa hàng

- Chủ tiệm làm thử 1 đơn bán, 1 phiếu sửa; nhân viên kho kiểm tra tồn, kỹ thuật xử lý báo giá; một người đóng vai khách tra cứu bằng mã/token thử nghiệm.
- Ghi mỗi trường hợp PASS/FAIL, ảnh chụp (che dữ liệu khách), người kiểm thử, giờ chạy và lỗi tồn tại vào `PHIEU_UAT_TUAN19.md`.
- Kết luận **GO** nếu tất cả kiểm tra P0/P1 PASS và đã thử khôi phục; nếu còn lỗi bảo mật, mất dữ liệu hoặc sai tồn kho thì **NO-GO**.

## 3. Những việc chưa tự động thay bạn

Không có SQL Server hoặc JDK21 thực tế trong gói ZIP nên không thể kết luận Flyway, thanh toán, nhập/xuất kho, sao lưu hoặc runtime Windows thành công chỉ từ mã nguồn. **Chạy các bước Ngày 1–7 trên máy của bạn** và ghi bằng chứng; chỉ khi đó Tuần 19 mới được nghiệm thu hoàn chỉnh để dùng thật.

Tài liệu hỗ trợ: `CHECKLIST_VAN_HANH.md`, `PHIEU_UAT_TUAN19.md`, `KET_QUA_KIEM_THU_TUAN19.md`.
