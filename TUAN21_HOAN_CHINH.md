# TIVAREK — TUẦN 21: BẢO MẬT, PHÂN QUYỀN VÀ AN TOÀN TÀI KHOẢN

Tuần 21 kế thừa toàn bộ Tuần 6–20. Không chỉnh sửa migration Flyway cũ, không thay đổi cấu trúc database, không làm mất URL hoặc chức năng bán hàng/sửa chữa/bảo hành.

## 1. Phần đã tích hợp

1. **Kiểm soát truy cập HTTP theo nghiệp vụ** trong `SecurityConfig`: bổ sung ràng buộc riêng cho các POST nhạy cảm về đơn bán, hoàn tiền, kho, sửa chữa và bảo hành. Phân quyền `@PreAuthorize` có sẵn trong các Service vẫn được giữ lại làm lớp bảo vệ thứ hai.
2. **Giới hạn đăng nhập thất bại** qua `LoginAttemptGuard` và `LoginThrottleFilter`. Trong một tiến trình, sau 5 lần nhập sai cùng cặp tài khoản/IP trong 15 phút sẽ tạm chặn 5 phút; 30 lần sai từ cùng IP cũng tạm chặn. Trả HTTP 429 và `Retry-After: 300`. Không tin vào `X-Forwarded-For` do client tự khai.
3. **Đổi mật khẩu** tại `/doi-mat-khau`: mọi tài khoản đã đăng nhập có thể đổi sau khi nhập lại mật khẩu hiện tại; mật khẩu mới dài tối thiểu 12 ký tự, gồm chữ và số và tối đa 72 byte UTF-8. Kiểm tra mật khẩu hiện tại bằng BCrypt, ghi hash mới qua cập nhật có điều kiện, lưu audit, hủy phiên hiện tại và yêu cầu đăng nhập lại.
4. **Nhật ký truy cập bị từ chối**: `ACCESS_DENIED` lưu người thực hiện và nhóm thao tác (`SALES`, `STOCK`, `REPAIR`, ...), không lưu URL đầy đủ, mã bí mật hay query string. Hỏng Audit không biến HTTP 403 thành HTTP 500.
5. **Ngăn lộ tracking token qua Referer**: header `Referrer-Policy: no-referrer` trên response của ứng dụng (token đơn bán hiện được đặt trong URL biên nhận). Không thay đổi quy trình khách truy cập bằng mã bảo mật.
6. **Kiểm thử mới** cho throttling, HTTP 429, ma trận write-permission, CSRF, audit và đổi mật khẩu; toàn bộ test W8–W20 được giữ nguyên.

## 2. Giới hạn bảo mật cần biết

- Bộ giới hạn đăng nhập hiện lưu trong bộ nhớ của **một tiến trình Java** và sẽ reset khi khởi động lại. Nếu sau này dùng nhiều server/replica, phải chuyển sang bộ đếm chia sẻ (ví dụ Redis hoặc rate limit ở reverse proxy). Khi chạm ngưỡng 10.000 khóa trong bộ nhớ, truy cập mới có thể bị hạn chế tạm thời; đây là cơ chế tự bảo vệ nhưng cũng có rủi ro từ chối dịch vụ. Cần kiểm tra trong tuần thử tải.
- Khi chạy sau proxy/tunnel, `getRemoteAddr()` có thể là địa chỉ proxy của mọi khách. Cần cấu hình trusted proxy và rate limiting phía trước nếu triển khai Internet; **không** tự bật tin tưởng tất cả `X-Forwarded-For`.
- Đổi mật khẩu làm mất hiệu lực **phiên hiện tại**; chưa có tính năng thu hồi mọi session đã đăng nhập trên các thiết bị khác. Trước khi công khai, hãy cân nhắc session registry/session-version theo yêu cầu vận hành.
- Tuần 21 không bổ sung chức năng cấp quyền mới hay tự đổi vai trò. Tất cả quyền quản trị vẫn do SQL Server/role management hiện hữu kiểm soát.
- Các phép thử SQL Injection, XSS, an toàn upload, sao lưu/khôi phục và phân quyền cấp từng bản ghi vẫn cần được kiểm thử thủ công trên môi trường DEV. Những phần đã an toàn ở các tuần trước được giữ nguyên, không tuyên bố đã pentest toàn bộ.

## 3. Chạy an toàn trên Windows

1. Bảo đảm đã cấu hình **JDK 21** và SQL Server DEV; trước đó nên chạy `CHANDOAN_TUAN20.cmd`.
2. Nhấp đúp `KIEM_THU_TUAN21.cmd`; đợi `BUILD SUCCESS` (sử dụng H2 test profile, không ghi SQL Server thật).
3. Nhấp đúp `CHAY_TUAN21.cmd` để chạy **DEV localhost**. Launcher kế thừa từ tuần 20; không tự gọi production.
4. Khi app đang chạy, nhấp đúp `KIEM_THU_WEB_TUAN21.cmd` và `KIEM_THU_WEB_TUAN20.cmd` để kiểm tra HTTP không ghi dữ liệu.
5. Đăng nhập bằng từng vai trò và dùng `PHIEU_UAT_TUAN21.md` kiểm thử thao tác thực tế trên **database DEV**.
6. Chỉ chạy lại launcher production khi đã nghiệm thu, sao lưu, rà soát secrets và HTTPS.

Trong SSMS, có thể chạy `scripts/check-week21.sql` để đọc ma trận role-permission, thống kê audit và danh sách tài khoản chưa có role. Tệp này chỉ chứa SELECT, không thay đổi dữ liệu.

## 4. Nhiệm vụ nghiệm thu 7 ngày

| Ngày | Kiểm thử | Tiêu chí đạt |
| --- | --- | --- |
| 1 | Ma trận OWNER, SALES, INVENTORY, TECHNICIAN, CUSTOMER | Mọi URL nhạy cảm từ chối vai trò không đúng |
| 2 | Đăng nhập sai, thành công, logout, đổi mật khẩu | HTTP 429 khi quá ngưỡng, mật khẩu cũ hết hiệu lực |
| 3 | Hai khách cố xem đơn/phiếu sửa/bảo hành của nhau | Bị từ chối/404, không lộ dữ liệu |
| 4 | Hoàn tiền, giao hàng, đảo chứng từ, duyệt báo giá | Không thao tác nếu thiếu quyền; số tiền và tồn không đổi |
| 5 | CSRF, XSS, upload giả ảnh, truy vấn bất thường | Không thực thi mã hay tiết lộ dữ liệu |
| 6 | Audit Log, toàn bộ Java/H2 tests, HTTP probes | Không có thông tin bí mật trong log; test PASS |
| 7 | SQL DEV, sao lưu, chạy lại toàn bộ UAT W20–21 | Không lỗi P0, GO/NO-GO được ký xác nhận |

## 5. Không bỏ qua bước an toàn trước khi công khai

- Đổi tất cả mật khẩu test/mẫu, không dùng tài khoản demo ở production.
- Giữ `.env` và các giá trị `TIVAREK_DB_*` ngoài Git. Cấp tài khoản DB quyền tối thiểu.
- Production chỉ mở qua HTTPS; session cookie cần `Secure` và `HttpOnly`.
- Khóa truy cập file upload; đảm bảo chỉ xem ảnh thuộc quyền hiện hành.
- Giữ bản sao lưu database và ảnh trước khi triển khai phiên bản mới.
- Trong trường hợp cần nhiều instance, triển khai lưu throttle/session tập trung hoặc giới hạn ở reverse proxy.

## 6. Kết quả xác minh trong môi trường dựng bản vá

Đã kiểm tra cấu trúc source và giữ nguyên các file cũ. Hệ thống chạy bản vá không có JDK 21, trình biên dịch Java hoặc SQL Server thực, do đó **chưa thể xác nhận `mvnw clean verify` hoặc UAT trên Windows đã PASS**. Hoàn thành các bước ở mục 3 và ghi kết quả trong phiếu UAT trước khi sử dụng thật.
