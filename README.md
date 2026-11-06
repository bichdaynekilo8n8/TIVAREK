> **Tuần 24:** đọc [TUAN24_HOAN_CHINH.md](TUAN24_HOAN_CHINH.md), chạy `KIEM_THU_TUAN24.cmd` rồi `CHAY_TUAN24.cmd`. Build JDK 21 đã đạt; Lighthouse, SQL Server DEV và UAT còn chờ theo báo cáo nghiệm thu.

# TIVAREK — Tuần 23: Nâng cấp giao diện và trải nghiệm responsive

**Bắt đầu tại:** [TUAN23_HOAN_CHINH.md](TUAN23_HOAN_CHINH.md) · [PHIEU_UAT_TUAN23.md](PHIEU_UAT_TUAN23.md) · [KET_QUA_KIEM_THU_TUAN23.md](KET_QUA_KIEM_THU_TUAN23.md).

- `CHAY_TUAN23.cmd`: chạy DEV SQL Server theo cấu hình Tuần 22, không áp dụng migration mới.
- `KIEM_THU_TUAN23.cmd`: toàn bộ Maven/JUnit/H2 regression bằng JDK 21, bao gồm test mới của trang chủ.
- `scripts/smoke-week23-ui.ps1`: sau khi app chạy, kiểm tra GET trang khách hàng, checkout và tài nguyên CSS/JS.
- Giao diện khách hàng mới: điều hướng mobile, trang chủ, danh sách, giỏ hàng, thanh toán, đăng ký và tra cứu sửa chữa.
- Giao diện quản trị/kho/báo cáo cải thiện responsive thông qua CSS dùng chung; trang giới thiệu GitHub Pages đồng bộ thương hiệu.
- **Giới hạn:** GitHub Pages chỉ phục vụ trang tĩnh `/docs`. Chức năng SQL Server và quản trị cần Spring Boot deploy riêng.

**Chưa xác nhận chạy trên SQL Server thực tế:** xem biên bản kiểm thử Tuần 23 trước khi dùng production.

---

# TIVAREK — Tuần 22: An toàn giao dịch và thao tác đồng thời

**Hướng dẫn chính:** [TUAN22_HOAN_CHINH.md](TUAN22_HOAN_CHINH.md),
[PHIEU_UAT_TUAN22.md](PHIEU_UAT_TUAN22.md) và
[KET_QUA_KIEM_THU_TUAN22.md](KET_QUA_KIEM_THU_TUAN22.md).

- `KIEM_THU_TUAN22.cmd`: kiểm tra hồi quy Java 21/H2 và đóng gói Maven.
- `CHAY_TUAN22.cmd`: khởi động DEV, chạy Flyway V12 nếu chưa chạy.
- `KIEM_THU_TUAN22_SQLSERVER.cmd`: test concurrency/rollback **chỉ trên** SQL Server `TIVAREK_WEEK8_DEV` đã sao lưu.
- `scripts/check-week22.sql`: chỉ đọc, cảnh báo dữ liệu kho/thanh toán/sửa chữa/bảo hành bất nhất.
- Thêm khóa SQL Server theo ProductID/DeviceID và khóa phiếu sửa để chống hai thao tác cùng lúc gây sai dữ liệu.
- Không tự động retry POST khi gặp deadlock; hệ thống trả HTTP 503 để tránh lặp thanh toán/ghi sổ.

**Chưa chứng nhận production:** máy đóng gói không có JDK 21/SQL Server thực; xem biên bản kiểm thử và thực hiện UAT trước khi đưa vào sử dụng thật.

---

# TIVAREK — Tuần 21: Bảo mật, phân quyền và bảo vệ tài khoản

**Bắt đầu tại [TUAN21_HOAN_CHINH.md](TUAN21_HOAN_CHINH.md)** và [PHIEU_UAT_TUAN21.md](PHIEU_UAT_TUAN21.md). Tuần 21 kế thừa trọn vẹn Tuần 6–20, không chỉnh sửa migration cũ.

- `KIEM_THU_TUAN21.cmd`: build và toàn bộ test Java/H2, thêm test mới về login throttling, ranh giới quyền, CSRF, đổi mật khẩu và audit.
- `CHAY_TUAN21.cmd`: khởi động DEV an toàn bằng launcher đã có từ Tuần 20.
- `KIEM_THU_WEB_TUAN21.cmd`: kiểm tra hai trang công khai và header bảo mật (GET-only).
- `/doi-mat-khau`: người dùng đã đăng nhập có thể thay mật khẩu, sau đó bắt buộc đăng nhập lại.
- Bổ sung HTTP 429 khi đăng nhập sai liên tục; ràng buộc POST theo quyền nghiệp vụ và ghi audit khi truy cập bị từ chối.
- `scripts/check-week21.sql`: SQL Server read-only để đối chiếu role-permission và sự kiện bảo mật.

**Chưa nghiệm thu production:** xem [KET_QUA_KIEM_THU_TUAN21.md](KET_QUA_KIEM_THU_TUAN21.md) để biết giới hạn xác minh.

---

# TIVAREK — Tuần 20: Ổn định giao dịch và nghiệm thu liên thông

**Bắt đầu tại [TUAN20_HOAN_CHINH.md](TUAN20_HOAN_CHINH.md)** và [PHIEU_UAT_TUAN20.md](PHIEU_UAT_TUAN20.md). Tuần 20 kế thừa nguyên vẹn Tuần 6–19, không tạo migration mới.

- `CHANDOAN_TUAN20.cmd`: kiểm tra JDK21, Maven Wrapper, cổng SQL Server.
- `KIEM_THU_TUAN20.cmd`: chạy toàn bộ Java JUnit/H2 + build; có thêm test hồi quy tổng đơn, hoàn tiền và idempotency.
- `CHAY_TUAN20.cmd`: khởi động **DEV** dùng DB `TIVAREK_WEEK8_DEV`.
- `KIEM_THU_WEB_TUAN20.cmd`: smoke 21 đường dẫn, quyền và CSRF khi app đang chạy.
- `scripts/check-week20.sql`: đối chiếu SQL Server chỉ đọc, bao gồm bất thường giảm giá dòng bán, tiền, tồn kho.

Sửa cốt lõi: phép tính chiết khấu theo **cả dòng bán** (không lặp lại trên từng sản phẩm), thuế và giới hạn hoàn tiền theo số hàng trả; xác minh đầy đủ tham chiếu giao dịch khi nhận yêu cầu lặp. Kết quả nghiệm thu tại [KET_QUA_KIEM_THU_TUAN20.md](KET_QUA_KIEM_THU_TUAN20.md).

---

# TIVAREK — Tuần 19: Kiểm thử, nghiệm thu và vận hành thử

**Bắt đầu tại [TUAN19_HOAN_CHINH.md](TUAN19_HOAN_CHINH.md)**, xem thêm [CHECKLIST_VAN_HANH.md](CHECKLIST_VAN_HANH.md) và [PHIEU_UAT_TUAN19.md](PHIEU_UAT_TUAN19.md).

- `KIEM_THU_TUAN19.cmd`: chạy tất cả JUnit và kiểm tra build với JDK 21/H2 (không cần SQL Server).
- `CHAY_TUAN19.cmd`: chạy ứng dụng **local DEV** với SQL Server `TIVAREK_WEEK8_DEV`, hỏi login/password an toàn.
- `KIEM_THU_WEB_TUAN19.cmd`: kiểm tra 21 endpoint/đường dẫn, bảo mật cơ bản và CSRF khi ứng dụng đang chạy.
- `scripts/check-week19.sql`: đọc dữ liệu để phát hiện bất nhất kho, đơn hàng, bảo hành, thanh toán; không sửa dữ liệu.
- **Production** vẫn dùng `CHAY_TUAN18_PROD.cmd` với các biến `TIVAREK_DB_*`/`TIVAREK_UPLOAD_DIR` được thiết lập riêng; không tự động đọc `.env.example`.

Không đổi schema/Flyway hoặc nghiệp vụ đã triển khai Tuần 6–18. Kết quả chạy thật của 7 ngày phải được ghi vào [KET_QUA_KIEM_THU_TUAN19.md](KET_QUA_KIEM_THU_TUAN19.md) và phiếu UAT trước khi công khai website thương mại.

---

# TIVAREK — Bản triển khai Tuần 18

**Xem hướng dẫn:** [TUAN18_HOAN_CHINH.md](TUAN18_HOAN_CHINH.md). Nhận diện TIVAREK đã tích hợp logo, số điện thoại **0335 135 549**, email **tivarek@gmail.com** và danh thiếp. Trang tĩnh GitHub Pages nằm trong `/docs`; ứng dụng Java và SQL Server chạy riêng với profile `prod`.

- CI JDK 21 + Maven tại `.github/workflows/tivarek-ci.yml`.
- Khởi động: `CHAY_TUAN18.cmd` hoặc `scripts/start-week18.ps1 -Build`.
- Kiểm tra HTTP: `KIEM_THU_TUAN18.cmd`.
- Backup SQL Server/ảnh: `scripts/backup-week18.ps1` và `scripts/backup-images-week18.ps1`.
- Không có thay đổi nghiệp vụ hoặc Flyway so với Tuần 17.

---

# TIVAREK — Bản tích hợp Dashboard và báo cáo Tuần 17

**Bắt đầu tại đây:** [HUONG_DAN_TUAN17.md](HUONG_DAN_TUAN17.md), chạy **`CHAY_TUAN17.cmd`** trên Windows với **JDK 21 + SQL Server**. Flyway V11 thêm `CompletedAt`, chỉ mục và quyền xem báo cáo mà không sửa V1–V10.

- Dashboard cho chủ cửa hàng tại `/admin/reports`.
- Báo cáo bán hàng, tài chính, sửa chữa, kho và bảo hành (mỗi nhóm có phân quyền riêng).
- Lọc ngày theo UTC+7, phân biệt dòng tiền với lợi nhuận, xuất CSV cho Excel.
- Kiểm thử tại `src/test/java/com/tivarek/week17`; SQL read-only tại `scripts/check-week17.sql`.

---

# TIVAREK — Bản tích hợp bảo hành Tuần 16

**Bắt đầu tại đây:** [HUONG_DAN_TUAN16.md](HUONG_DAN_TUAN16.md). Khởi động bằng **`CHAY_TUAN16.cmd`** trên Windows với Java 21 và SQL Server. Migration V10 bổ sung cấu hình, lịch sử và phân quyền bảo hành, không sửa V1–V9.

- Chủ cửa hàng tạo chính sách và gắn thời hạn cho sản phẩm/máy/dịch vụ tại `/admin/warranties`.
- Phiếu tự phát hành sau đơn bán `COMPLETED` hoặc phiếu sửa `DELIVERED` nếu nguồn có bảo hành.
- Khách tra cứu tại `/tra-cuu-bao-hanh`, tài khoản liên kết xem tại `/account/warranties`.
- Nhân viên xử lý yêu cầu và liên kết phiếu sửa mới nếu cần.
- Kiểm thử riêng tại `src/test/java/com/tivarek/week16`, SQL chỉ đọc tại `sql/12_week16_verify.sql`.

---

# TIVAREK — Bản hoàn thiện nghiệp vụ Tuần 15

**Bắt đầu tại đây:** [HUONG_DAN_TUAN15.md](HUONG_DAN_TUAN15.md) — nhấp đúp **`CHAY_TUAN15.cmd`**, nhập SQL login/mật khẩu, dùng JDK 21. Nếu gặp SQL Server `18456`, đọc [HUONG_DAN_SUA_SQL_TUAN15.md](HUONG_DAN_SUA_SQL_TUAN15.md).

- Khách mua hàng: `/san-pham` → `/gio-hang` → `/thanh-toan` → biên nhận `/don-hang/{code}?token=...`.
- Nhân viên bán hàng: `/admin/orders`, tạo đơn tại quầy `/admin/orders/new`, xác nhận/giữ tồn.
- Kho: xuất từng phần theo sổ chứng từ `SALE_ISSUE` đã POSTED, nhận hàng trả vào khu cách ly bằng `SALE_RETURN`.
- Thu tiền và hoàn tiền thủ công theo phương thức tiền mặt/chuyển khoản đã xác minh, chống thao tác lặp.
- Khách đã đăng ký xem đơn của chính mình tại `/account/orders`.
- Migration mới `V9__week15_sales_permissions.sql`, giữ toàn bộ migration V1–V8_1 và dữ liệu trước đó.
- Test mới: `src/test/java/com/tivarek/week15`; SQL kiểm tra tại `sql/10_week15_verify.sql`.

Lưu ý: chưa tích hợp cổng thanh toán tự động, phí vận chuyển đang 0đ, và bảo hành riêng là nhiệm vụ Tuần 16.

---

# TIVAREK — Bản tích hợp Tuần 12 + Tuần 14

**Bắt đầu tại đây:** [TUAN12_TUAN14_HOAN_CHINH.md](TUAN12_TUAN14_HOAN_CHINH.md), chạy `CHAY_TUAN14.cmd` trên Windows với **Java 21 + SQL Server**.

- Kho và chứng từ: `/admin/stock`.
- Tiếp nhận/sửa chữa: `/admin/repairs`.
- Khách đăng ký và đăng nhập bằng database: `/register`, `/login`.
- Khách gửi yêu cầu và tra cứu: `/sua-chua`, `/tra-cuu-sua-chua`.

Giữ nguyên migration V1–V8; `V8_1__ensure_customer_registration_role.sql` khôi phục vai trò CUSTOMER tự động nếu database cũ bị thiếu (sửa lỗi 500 khi đăng ký). Các tài liệu Tuần 13 và trước đó ở dưới đây là lịch sử triển khai.

---

# TIVAREK — Tuần 13: Website sản phẩm công khai

**Bắt đầu:** đọc [TUAN13_HOAN_CHINH.md](TUAN13_HOAN_CHINH.md), chạy `CHAY_TUAN13.cmd` với **JDK 21**, mở `http://127.0.0.1:8080/san-pham`. Quản trị đăng/gỡ sản phẩm tại `/admin/storefront`.

Lưu ý: Tuần 12 hiện chưa có nghiệp vụ nhập/xuất kho hoàn chỉnh; trang Tuần 13 chỉ đọc tồn từ các chứng từ đã `POSTED`. Không sửa các migration V1–V7 đã phát hành.

---

# TIVAREK — Tuần 11: Quản lý từng thiết bị

> **Bản sửa lỗi khởi động cho Tuần 12:** đọc [HUONG_DAN_SUA_LOI_TUAN12.md](HUONG_DAN_SUA_LOI_TUAN12.md) và chạy **CHAY_TUAN12.cmd**. Migration V6 hiện bổ sung phân quyền/chỉ mục kho; các nghiệp vụ nhập, xuất, chuyển kho và dashboard vẫn cần triển khai trong các buổi tiếp theo.

Bắt đầu với [TUAN11_HOAN_CHINH.md](TUAN11_HOAN_CHINH.md). Chạy **CHAY_TUAN11.cmd**, mở http://127.0.0.1:8080/, chọn **Đăng nhập** và dùng tài khoản website đã có. Tài khoản quản trị tự vào trang làm việc; chọn **Thiết bị**.

- Mã máy/serial, cấu hình, tình trạng, lỗi và giá riêng từng chiếc.
- Ảnh JPEG/PNG thật: tải nhiều ảnh, đại diện, thứ tự, gỡ và kiểm soát truy cập.
- Lịch sử kiểm tra, phân quyền, audit và bảo vệ sửa đồng thời bằng rowversion.
- Flyway V5/V5_1 nâng cấp tại chỗ, giữ nguyên dữ liệu và migration Tuần 9–10.
- Kiểm thử: `scripts/test-week11.ps1`; xác minh SQL: `sql/08_week11_verify.sql`.
- Ảnh nằm ngoài mã nguồn; cấu hình `TIVAREK_UPLOAD_DIR` và sao lưu cùng DB.

---

## Tài liệu các giai đoạn trước

# TIVAREK — Tuần 10: Quản lý danh mục và sản phẩm

Đã tích hợp trực tiếp vào ứng dụng Spring Boot / Java 21 / SQL Server ở thư mục gốc.

**Bắt đầu:** đọc [TUAN10_HOAN_CHINH.md](TUAN10_HOAN_CHINH.md), chạy `CHAY_TUAN10.cmd`, mở http://127.0.0.1:8080/ và chọn **Đăng nhập**. Tài khoản quản trị tự vào `/admin`; mở mục **Sản phẩm** để làm việc.

- Quản lý danh mục, thương hiệu, đơn vị tính, sản phẩm, SKU và SERIALIZED/BULK.
- Thêm/sửa/tìm/lọc/ngừng/kinh doanh lại; quyền backend, audit và bảo vệ sửa đồng thời.
- Flyway nâng cấp DB Tuần 9 bằng V4 và dữ liệu dev V4_1; giữ nguyên migration cũ.
- Kiểm thử: `scripts/test-week10.ps1`; SQL xác minh: `sql/07_week10_verify.sql`.

---

## Tài liệu và ghi chú giai đoạn trước

# TIVAREK — Tuần 9 hoàn chỉnh

Đã tích hợp đăng nhập, phân quyền theo dữ liệu SQL Server, quản trị khách hàng, giá nhập và audit vào ứng dụng gốc Spring Boot 4.1.2 / Java 21.

Bắt đầu với [TUAN9_HOAN_CHINH.md](TUAN9_HOAN_CHINH.md).

```powershell
.\scripts\start-week9.ps1 -CreateDemoAccounts
```

Mở http://127.0.0.1:8080/login. Dùng Database `TIVAREK_WEEK8_DEV`; script hỏi mật khẩu SQL và website, không có mật khẩu cố định trong code.

Kiểm thử tự động:

```powershell
.\scripts\test-week9.ps1
```

Nguồn ứng dụng ở `src/`; main `com.tivarek.TivarekApplication`. `week8/example-app` giữ nguyên để tham khảo, không phải ứng dụng Tuần 9 cần mở.

---

## Ghi chú dự án trước Tuần 9


# TIVAREK — Website bán hàng và sửa chữa điện tử

## 1. Giới thiệu

TIVAREK là dự án website dành cho cửa hàng kinh doanh
và sửa chữa thiết bị điện tử tại Quảng Ngãi.

Website hướng đến:
- Chất lượng tốt
- Giá cả hợp lý
- Bảo hành uy tín
- Chăm sóc khách hàng tận tâm

Dự án được xây dựng với hai mục tiêu:
1. Phục vụ học tập và đồ án CNTT.
2. Phát triển thành hệ thống sử dụng thực tế trong tương lai.

## 2. Công nghệ

Frontend v0.1:
- HTML5
- CSS3
- JavaScript
- JSON
- Fetch API
- Git và GitHub

Dự kiến ở giai đoạn sau:
- Backend
- SQL Server
- API
- Xác thực và phân quyền
- Dashboard quản trị

## 3. Chức năng hiện tại

### Trang chủ
- Giới thiệu thương hiệu
- Danh mục sản phẩm
- Sản phẩm nổi bật
- Giới thiệu dịch vụ sửa chữa
- Giao diện responsive

### Sản phẩm
- Hiển thị dữ liệu từ JSON
- Tìm kiếm sản phẩm
- Lọc theo danh mục
- Lọc theo tình trạng
- Lọc theo khoảng giá
- Sắp xếp theo giá và tên
- Xem chi tiết sản phẩm

### Sửa chữa
- Nhập thông tin liên hệ
- Chọn loại thiết bị
- Mô tả tình trạng lỗi
- Chọn ảnh minh họa
- Kiểm tra dữ liệu đầu vào
- Hiển thị kết quả demo

## 4. Cách chạy

Cần chạy bằng một local web server để Fetch API
đọc được DATA/products.json.

Cách 1: Dùng VS Code và tiện ích Live Server.

Cách 2: Nếu đã cài Python, mở terminal tại
thư mục gốc và chạy:

    python -m http.server 5500

Sau đó truy cập:

    http://localhost:5500

## 5. Trạng thái dự án

Phiên bản: Frontend v0.1

Đây là website demo:
- Chưa kết nối SQL Server.
- Chưa có backend.
- Chưa nhận đơn hàng thật.
- Chưa lưu yêu cầu sửa chữa.
- Dữ liệu giá và tồn kho chỉ mang tính minh họa.

## 6. Lộ trình phát triển

- Hoàn thiện UI/UX
- Thiết kế Database ERD
- Xây dựng backend
- Kết nối SQL Server
- Phát triển quản lý sản phẩm và kho
- Quản lý đơn bán hàng
- Quản lý phiếu sửa chữa
- Quản lý bảo hành
- Quản lý nhân viên và phân quyền
- Kiểm thử bảo mật
- Triển khai thực tế

## 7. Thông tin dự án

Tên: TIVAREK

Khu vực: Quảng Ngãi, Việt Nam

Mục đích: Website bán hàng và sửa chữa điện tử.

Phát triển: Nguyễn Tấn Bá Bích

Dự án học tập — hướng tới ứng dụng thực tế.
