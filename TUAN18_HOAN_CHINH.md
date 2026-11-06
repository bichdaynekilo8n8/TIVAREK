# TIVAREK — Tuần 18: Chuẩn bị triển khai và công bố website

> Bản giao: bổ sung nhận diện thương hiệu, liên hệ, trang giới thiệu GitHub Pages, production profile, CI, health check, backup và kiểm tra HTTP. **Bản này chưa được xuất bản lên Internet tự động.**

## 1. Thông tin chính thức

| Trường | Giá trị |
|---|---|
| Tên | TIVAREK — Điện tử & Sửa chữa |
| Điện thoại / Zalo | **0335135549** (hiển thị: 0335 135 549) |
| Email | **tivarek@gmail.com** |
| Khu vực | Sơn Dung (cũ), xã Sơn Tây, Quảng Ngãi |
| Slogan | Tận tâm từng thiết bị. An tâm từng khách hàng. |

Ảnh được cung cấp: logo vuông dùng làm favicon, logo ngang dùng cho thanh điều hướng, mẫu danh thiếp dùng cho phần liên hệ. Các file đã được chép vào `src/main/resources/static/images/` và `docs/assets/`.

## 2. Hai cách đưa TIVAREK lên mạng (không thuê VPS)

| | GitHub Pages | Spring Boot + SQL Server + Cloudflare Tunnel |
|---|---|---|
| Hiển thị thông tin thương hiệu, số điện thoại, email | Có | Có |
| Danh mục sản phẩm từ SQL Server | Không | Có |
| Đặt hàng, sửa chữa, bảo hành, báo cáo, đăng nhập | Không | Có |
| Yêu cầu máy tính cửa hàng luôn chạy | Không | Có |
| Địa chỉ public | GitHub Pages của repo | Tên miền HTTPS tạm thời của Tunnel |

**Lựa chọn phù hợp:** dùng GitHub Pages để có trang giới thiệu hoạt động liên tục. Chạy ứng dụng thật trên máy Windows cửa hàng khi cần thao tác, chỉ công khai qua đường hầm HTTPS sau khi đã kiểm tra an toàn, dùng tài khoản thật và có phương án sao lưu.

## 3. Trước khi chạy: JDK 21 và SQL Server

1. Cài Java **JDK 21**, kiểm tra `java -version`; đảm bảo trong PowerShell đang dùng đúng Java 21.
2. Cài SQL Server và SSMS; tạo **database production riêng**, không lấy database test làm dữ liệu thật.
3. Tạo tài khoản SQL Server riêng cho TIVAREK (không dùng `sa`), có quyền đủ để Flyway migration lần đầu và sau đó hạn chế quyền nếu mô hình vận hành cho phép.
4. Bật TCP/IP cho SQL Server nếu kết nối qua TCP, đảm bảo máy Windows cho phép kết nối cổng SQL **chỉ từ địa chỉ tin cậy**. Không mở cổng 1433 ra Internet.
5. Cấu hình TLS SQL Server với chứng chỉ được tin cậy; ví dụ URL JDBC trong `.env.example` yêu cầu `encrypt=true;trustServerCertificate=false`. Nếu dùng chứng chỉ tự ký cho local, chỉ chấp nhận tạm khi thử nghiệm; không tắt xác minh chứng chỉ trong production.
6. Lập thư mục chứa ảnh tách khỏi source, ví dụ `D:\TIVAREK_DATA\device-images`. Không lưu dữ liệu upload trong `target/`.
7. Khai báo **Environment Variables** tại PowerShell/Windows: `TIVAREK_DB_URL`, `TIVAREK_DB_USER`, `TIVAREK_DB_PASSWORD`, `TIVAREK_UPLOAD_DIR`. `.env.example` chỉ là mẫu; Spring Boot không tự nạp file `.env`.

Ví dụ cho **phiên PowerShell hiện tại** (thay giá trị theo máy, không chụp màn hình đăng nhập/mật khẩu):

```powershell
$env:TIVAREK_DB_URL = 'jdbc:sqlserver://127.0.0.1:1433;databaseName=TIVAREK_PROD;encrypt=true;trustServerCertificate=false'
$env:TIVAREK_DB_USER = 'tivarek_app'
$env:TIVAREK_DB_PASSWORD = Read-Host 'Nhập mật khẩu SQL Server' -AsSecureString | ForEach-Object { [System.Net.NetworkCredential]::new('', $_).Password }
$env:TIVAREK_UPLOAD_DIR = 'D:\TIVAREK_DATA\device-images'
```

Lưu ý: Environment Variable có thể bị đọc bởi tiến trình có quyền truy cập thích hợp trên cùng máy; đặt quyền truy cập Windows, không đưa chúng vào Git, không gửi ảnh chụp cấu hình cho người khác. Trên production thực tế, ưu tiên credential store hoặc service manager.

## 4. Kiểm thử và đóng gói JAR

Tại thư mục chứa `pom.xml`:

```powershell
.\mvnw.cmd -B -ntp -Dspring.profiles.active=test clean verify
```

Maven Wrapper sẽ tự tải các thành phần cần thiết nếu môi trường cho phép. Test profile sử dụng H2 độc lập với SQL Server; các test có nhãn `RUN_SQLSERVER_TESTS=true` chỉ chạy khi bật rõ ràng trong môi trường SQL Server chuẩn bị riêng.

Kết quả mong đợi:

- Tất cả test không phụ thuộc SQL Server hoàn tất, không có `FAILURE`/`ERROR`.
- `target/tivarek-0.0.1-SNAPSHOT.jar` được tạo.
- Không ghi đè hay xóa dữ liệu SQL Server thật.

## 5. Chạy bản production (Windows)

Sau khi cấu hình Environment Variables:

```powershell
.\scripts\start-week18.ps1 -Build
```

Hoặc nháy đúp `CHAY_TUAN18.cmd`. Nếu đã build thành công và chỉ muốn chạy lại:

```powershell
.\scripts\start-week18.ps1
```

`prod` mặc định bind `127.0.0.1:8080`, có `Secure` session cookie và graceful shutdown. Nếu port đã được dùng, đặt `$env:SERVER_PORT='8081'` và thay port trong lệnh kiểm tra/tunnel. Không tự đổi `TIVAREK_BIND_ADDRESS=0.0.0.0` trừ khi đặt phía sau reverse proxy tin cậy và đã cấu hình firewall.

Kiểm tra thủ công:

```powershell
.\scripts\smoke-week18.ps1
```

Script thử 6 endpoint: `/actuator/health`, `/`, `/san-pham`, `/login`, logo icon, logo ngang. Endpoint health chỉ trả về trạng thái, không lộ thông tin DB hay bí mật. Trình duyệt truy cập `http://127.0.0.1:8080` có thể không đăng nhập với cookie `Secure`: đó là chủ ý cho production HTTPS; kiểm tra đăng nhập thực tế trên URL HTTPS của tunnel.

## 6. Công khai trang tĩnh qua GitHub Pages

1. Đẩy phiên bản mới lên GitHub repository TIVAREK (xem kỹ diff trước `git add`).
2. Vào **Repository → Settings → Pages → Build and deployment**.
3. Chọn `Deploy from a branch`, chọn nhánh `main` (hoặc nhánh thực tế), folder **`/docs`**, bấm Save.
4. Chờ GitHub hiển thị địa chỉ Pages; kiểm tra logo, điện thoại, email, nút Gọi/Zalo trên di động.
5. Nếu repo là `https://github.com/<ten-tai-khoan>/TIVAREK`, địa chỉ thông thường có dạng `https://<ten-tai-khoan>.github.io/TIVAREK/` (chỉ ví dụ; lấy URL được GitHub xác nhận).

Trang này là **landing page HTML tĩnh**. Không kết nối đến SQL Server, không giả lập đặt hàng hoặc cho đăng nhập. Dữ liệu giao dịch vẫn nằm trong ứng dụng Java và DB nội bộ.

## 7. Cho người ngoài xem web động — Cloudflare Tunnel

> Chỉ thực hiện sau khi đã thay toàn bộ mật khẩu demo, kiểm thử phân quyền, sao lưu dữ liệu, kiểm tra khả năng phục hồi. Quick Tunnel tạo địa chỉ **công khai, không có lớp xác thực tự động**; không dùng để công khai dữ liệu thật nhạy cảm trong giai đoạn thử nghiệm.

1. Đảm bảo Spring Boot đang chạy ở `127.0.0.1:8080`.
2. Cài `cloudflared` theo tài liệu Cloudflare chính thức.
3. Mở PowerShell riêng và chạy:

```powershell
cloudflared tunnel --url http://127.0.0.1:8080
```

4. Cloudflare sẽ trả URL HTTPS tạm thời dạng `https://...trycloudflare.com`. Trên trình duyệt khác/mạng khác, thử `/`, `/san-pham`, `/sua-chua`, `/tra-cuu-bao-hanh` và chức năng đăng nhập.
5. Trước khi công khai lâu dài, cấu hình tên miền, Cloudflare Access hoặc biện pháp kiểm soát truy cập phù hợp; chặn dashboard quản trị ở lớp proxy khi có thể.
6. Nhấn Ctrl+C để ngừng public web; tắt máy hoặc ứng dụng cũng sẽ khiến website động không truy cập được.

Không cho phép truy cập trực tiếp SQL Server từ Internet. Không mở forwarding port 8080/1433 trên router khi dùng tunnel.

## 8. GitHub Actions

Đã thêm `.github/workflows/tivarek-ci.yml`. Mỗi lần push hoặc tạo pull request sẽ dùng Java 21, H2 test profile, Maven verify và tải JAR làm artifact trong 7 ngày. Workflow **không triển khai production**, không cần đưa tài khoản SQL Server lên GitHub Secrets để chạy unit/integration tests H2. Nếu CI đỏ, đọc logs và sửa trước khi chạy môi trường thật.

## 9. Sao lưu dữ liệu

Ví dụ sao lưu bằng Windows Authentication qua `sqlcmd` (quyền SQL Server phải cho phép backup và service phải ghi được đường dẫn):

```powershell
.\scripts\backup-week18.ps1 -Server 'localhost' -Database 'TIVAREK_PROD' -BackupDirectory 'D:\TIVAREK_BACKUPS'
```

Script tạo tên `.bak` riêng theo timestamp, `COPY_ONLY`, `CHECKSUM`, `RESTORE VERIFYONLY`; **không xóa dữ liệu**. Sao lưu thêm ảnh bằng:

```powershell
.\scripts\backup-images-week18.ps1 -BackupDirectory 'D:\TIVAREK_BACKUPS'
```

Lưu bản sao lưu ở vùng ổ đĩa do bạn kiểm soát, hạn chế quyền đọc và sao chép thêm sang thiết bị/vị trí khác. `RESTORE VERIFYONLY` không thay thế việc phục hồi thử nghiệm; tối thiểu mỗi tháng thử restore vào **database thử nghiệm riêng** bằng quy trình quản trị SQL Server.

## 10. Kiểm tra nghiệm thu Tuần 18

- [ ] Logo ngang rõ trên desktop/điện thoại; favicon đúng.
- [ ] Điện thoại `0335135549`, email `tivarek@gmail.com` và liên kết Gọi/Email/Zalo đúng.
- [ ] Sản phẩm, sửa chữa, đơn hàng, bảo hành, báo cáo Tuần 17 không đổi nghiệp vụ.
- [ ] Tất cả kiểm thử Maven pass với Java 21.
- [ ] SQL Server prod sử dụng DB riêng và thông tin bí mật không xuất hiện trong repo/log.
- [ ] Tài khoản demo vô hiệu hóa/thay mật khẩu trước khi có truy cập công khai.
- [ ] Login/logout, CSRF và phân quyền OWNER/SALES/TECHNICIAN/INVENTORY/CUSTOMER được kiểm thử bằng tài khoản thật.
- [ ] Health trả UP, endpoint actuator khác không công khai.
- [ ] Backup `.bak` được verify, ảnh được backup, thử phục hồi trên DB riêng.
- [ ] GitHub Actions xanh, Pages hiển thị đúng, Tunnel HTTPS được test bằng điện thoại.
- [ ] Kiểm tra trên màn hình 390px và 1440px, không có hình ảnh vỡ hoặc menu tràn.

**Điểm chưa thể tự động hoàn thành từ ZIP:** tạo DB và chứng chỉ TLS trên máy bạn, đặt credential, bật Pages trên tài khoản GitHub, công khai Tunnel và chạy nghiệm thu bằng SQL Server thật. Các bước đó phải được thực hiện trong hạ tầng do bạn kiểm soát.
