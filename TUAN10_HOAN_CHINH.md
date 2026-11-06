# TIVAREK — Tuần 10 đã tích hợp

Bản này tiếp nối trực tiếp dự án trong TIVAREK(2).zip, giữ đăng nhập, khách hàng, giá nhập và nhật ký của Tuần 9. Ứng dụng chính ở thư mục gốc chứa `pom.xml` và `src/`. Thư mục `week8/example-app` chỉ để tham khảo.

## 1. Cách mở và chạy

1. Giải nén dự án vào đường dẫn không dấu, ví dụ `D:\Projects\TIVAREK`.
2. Mở thư mục có `pom.xml` trong IntelliJ. Chọn JDK 21 và Reload Maven.
3. Dừng phiên bản ứng dụng cũ nếu đang chiếm cổng 8080.
4. Dùng đúng SQL Server và database đã chạy thành công ở Tuần 9. Mặc định là `TIVAREK_WEEK8_DEV`. Không xóa hay tạo lại database này.
5. Chạy `CHAY_TUAN10.cmd` hoặc PowerShell từ thư mục dự án:

```powershell
.\scripts\start-week9.ps1
```

Script hỏi tài khoản và mật khẩu SQL Server nếu chưa có biến môi trường. Nếu server, cổng hoặc database khác mặc định, khai báo trước theo cấu hình Tuần 9:

```powershell
$env:TIVAREK_DB_URL="jdbc:sqlserver://127.0.0.1:1433;databaseName=TIVAREK_WEEK8_DEV;encrypt=true;trustServerCertificate=true"
$env:TIVAREK_DB_USER="tivarek_dev"
.\scripts\start-week9.ps1
```

Nếu chạy bằng IntelliJ, main là `com.tivarek.TivarekApplication`; profile `dev`; cấu hình `TIVAREK_DB_URL`, `TIVAREK_DB_USER`, `TIVAREK_DB_PASSWORD` như Tuần 9. Đừng ghi mật khẩu vào mã nguồn.

Flyway tự chạy `V4__week10_product_management.sql` để bổ sung cột, chỉ mục và quyền; profile dev còn chạy `V4_1__week10_reference_data.sql` để bổ sung từ điển mẫu. Các migration V1, V1_1, V2, V3 giữ nguyên. Không chạy tay V4 rồi lại để Flyway chạy.

Mở **http://127.0.0.1:8080/** → **Đăng nhập**. Dùng tài khoản website quản lý của Tuần 9 (ví dụ `w9.owner`) và mật khẩu website bạn đã đặt. Đăng nhập đúng tài khoản có `ADMIN_ACCESS` sẽ tự chuyển đến `/admin`; chọn **Sản phẩm**.

Nếu chưa từng khởi tạo tài khoản thực hành, chạy:

```powershell
.\scripts\start-week9.ps1 -CreateDemoAccounts
```

Tài khoản mẫu chỉ tạo trong database thực hành. Tài khoản đã tồn tại giữ nguyên mật khẩu và vai trò; không tự đặt lại mật khẩu. Tài khoản SQL `tivarek_dev` dùng để kết nối DB, khác tài khoản website `w9.owner`.

## 2. Chức năng đã có

- Một lối vào công khai ghi **Đăng nhập**; bỏ liên kết **Quản trị** riêng trên trang chủ. Khi đã đăng nhập, mở `/login` sẽ tự chuyển đến nơi làm việc phù hợp quyền.
- Danh sách sản phẩm, tìm theo tên/SKU, lọc danh mục, thương hiệu, loại quản lý và trạng thái; 25 dòng mỗi trang.
- Thêm/sửa SKU, tên, danh mục, thương hiệu tùy chọn, đơn vị, giá tham khảo và mô tả.
- Ngừng kinh doanh và kinh doanh lại; không xóa dữ liệu đã liên kết. Sản phẩm ngừng không xuất hiện ở danh sách công khai.
- Thêm/sửa/ngừng/dùng lại danh mục, thương hiệu, đơn vị tính.
- SKU và mã được cắt khoảng trắng đầu/cuối, chuyển hoa; chỉ nhận chữ ASCII, số, dấu chấm, gạch dưới, gạch ngang; tối đa 40 ký tự. Unique index trong DB chặn trùng cả khi ghi đồng thời.
- Chặn tên rỗng, giá âm/quá số chữ số, loại quản lý sai và liên kết không hợp lệ. Không gán sản phẩm vào mục đã ngừng.
- Không ngừng một mục từ điển khi còn sản phẩm đang kinh doanh sử dụng mục đó. Các sản phẩm đã ngừng vẫn giữ liên kết lịch sử.
- Phiên bản `Revision` chặn ghi đè khi hai người cùng chỉnh sửa; tải lại dữ liệu mới nhất trước khi lưu lại.
- Ghi nhật ký với tài khoản thực hiện, thời điểm, đối tượng và tóm tắt thay đổi trong cùng transaction. Lỗi ghi nhật ký làm rollback thay đổi.
- Biểu mẫu và API kiểm tra quyền ở backend, giữ bảo vệ CSRF. Nội dung do người dùng nhập được Thymeleaf escape khi hiển thị.

## 3. Phân quyền

| Vai trò | Vào quản trị | Xem sản phẩm | Thêm/sửa/ngừng sản phẩm | Quản lý từ điển |
|---|---|---|---|---|
| OWNER | Có | Có | Có | Có |
| INVENTORY | Có | Có | Có | Có |
| SALES | Có | Có | Không | Không |
| TECHNICIAN | Có | Không | Không | Không |
| CUSTOMER | Không | Chỉ danh sách công khai | Không | Không |

Các quyền mới là `PRODUCT_READ`, `PRODUCT_WRITE`, `CATALOG_WRITE`. Đăng xuất rồi đăng nhập lại sau khi cập nhật quyền để session nạp quyền mới.

## 4. Products và Devices

| Nội dung | Products — Tuần 10 | Devices — Tuần 11 |
|---|---|---|
| Đối tượng | Mẫu sản phẩm | Từng máy vật lý |
| Mã | SKU duy nhất | Mã thiết bị riêng |
| Serial, tình trạng | Chưa quản lý | Theo từng máy |
| Giá | Giá tham khảo chung (`ListPrice`) | Giá riêng từng máy |
| RAM/SSD thực tế | Không quản lý theo từng chiếc | Thuộc thiết bị |
| Tồn kho | Chưa có nghiệp vụ mới | Triển khai kho ở Tuần 12 |

`SERIALIZED`: laptop, tivi và hàng cần theo dõi từng chiếc. `BULK`: chuột, dây HDMI, phụ kiện theo số lượng. Loại quản lý được cố định sau khi tạo để tránh đổi ý nghĩa dữ liệu kho và thiết bị đã có. Nếu chọn nhầm, ngừng SKU đó và tạo SKU mới.

Không nhập serial hoặc tình trạng máy riêng vào mẫu sản phẩm. Dữ liệu mẫu cũ của Tuần 8 có thể còn cấu hình trong tên; phần đó được giữ nguyên, không tự sửa dữ liệu lịch sử. Tuần 10 không tạo mới máy vật lý, số tồn hay nghiệp vụ kho.

## 5. Kiểm tra kết quả bằng giao diện và SQL Server

1. Đăng nhập quản lý → Danh mục/Thương hiệu/Đơn vị → thêm các mục cần dùng.
2. Mở Sản phẩm → Thêm sản phẩm, ví dụ SKU `LAP-DELL-5420`, tên `Dell Latitude 5420`, `SERIALIZED`, giá tham khảo 5.200.000.
3. Tìm SKU vừa tạo, sửa tên/mô tả, kiểm tra danh sách và các bộ lọc.
4. Mở SSMS, chạy `sql/07_week10_verify.sql`; xác nhận sản phẩm và nhật ký thực sự nằm trong DB.
5. Đăng xuất, đăng nhập lại; thông tin vẫn đúng. Tắt/mở lại ứng dụng cũng phải giữ dữ liệu vì profile dev dùng SQL Server.
6. Thử nhập lại SKU cùng chữ hoa/thường hoặc có khoảng trắng: phải báo trùng và giữ thông tin biểu mẫu.
7. Ngừng kinh doanh: sản phẩm vẫn có trong quản trị khi lọc đúng trạng thái và biến mất ở trang chủ; có thể kinh doanh lại.
8. Mở cùng sản phẩm ở hai tab, lưu tab thứ nhất rồi lưu tab thứ hai: tab thứ hai phải báo dữ liệu đã thay đổi.
9. Đăng nhập `w9.sales` để kiểm tra chỉ xem; `w9.customer` phải bị từ chối truy cập `/admin` và API quản lý.

## 6. Kiểm thử tự động

Không cần SQL Server (database H2 chỉ phục vụ test):

```powershell
.\scripts\test-week10.ps1
```

Kiểm thử trực tiếp SQL Server của bạn, bao gồm migration thật và các luồng Tuần 9–10:

```powershell
.\scripts\test-week10.ps1 -SqlServer
```

Chỉ dùng DB thực hành `TIVAREK_WEEK8_DEV`; không dùng DB cửa hàng đang vận hành. Script hỏi kết nối như Tuần 9. Các fixture được rollback trong test. Flyway cập nhật schema trước khi chạy test.

Kết quả kiểm thử trong gói được ghi ở `KET_QUA_KIEM_THU_TUAN10.md`. H2 không thay thế xác nhận hành vi SQL Server; hãy chạy lệnh `-SqlServer` và kiểm tra SSMS trước khi nghiệm thu trên máy của bạn.

## 7. Bảy buổi thực hành với mã đã tích hợp

| Buổi | Công việc thực hành | Kết quả cần tự kiểm tra |
|---|---|---|
| 1 | Đọc V4, phân biệt Products/Devices, mở DB | Cột và quyền mới có trong SQL Server |
| 2 | Quản lý danh mục, thương hiệu, đơn vị | Tạo/sửa/ngừng/dùng lại đúng ràng buộc |
| 3 | Thêm và tìm sản phẩm | SKU, FK, SERIALIZED/BULK hợp lệ |
| 4 | Sửa/ngừng/kinh doanh lại | Dữ liệu bền vững và chặn sửa đồng thời |
| 5 | Thử giao diện trên desktop/điện thoại | Bộ lọc, biểu mẫu lỗi và điều hướng dùng được |
| 6 | Đăng nhập các vai trò, mở API trực tiếp | Backend chặn thiếu quyền và CSRF |
| 7 | Chạy test, SQL xác minh, nghiệm thu | Hoàn thành toàn bộ bước ở mục 5 |

## 8. API cho Postman

Lấy token tại `GET /api/csrf`, gửi form đăng nhập `POST /login` với `username`, `password`, `_csrf`, giữ cookie session. Sau login lấy token mới; mọi thao tác ghi gửi header `X-CSRF-TOKEN`.

| Method | Đường dẫn | Chức năng |
|---|---|---|
| GET | `/api/admin/products?q=&categoryId=&brandId=&stockMode=&active=&page=0` | Danh sách: `items`, `page`, `hasNext` |
| GET | `/api/admin/products/{id}` | Chi tiết |
| POST | `/api/admin/products` | Tạo, trả 201 |
| PUT | `/api/admin/products/{id}` | Sửa kèm `revision` mới nhất |
| POST | `/api/admin/products/{id}/status` | `{"active":false,"revision":0}` |
| GET/POST | `/api/admin/catalog/{kind}` | Xem/tạo từ điển |
| PUT | `/api/admin/catalog/{kind}/{id}` | Sửa từ điển |
| POST | `/api/admin/catalog/{kind}/{id}/status` | Ngừng/dùng lại từ điển |

`kind` chỉ nhận `categories`, `brands`, `units`. Dữ liệu tạo/sửa từ điển: `{"code":"DELL","name":"Dell","revision":0}`.

Ví dụ sản phẩm (thay ID bằng dữ liệu thực tế):

```json
{"sku":"LAP-DELL-5420","name":"Dell Latitude 5420","categoryId":1,"brandId":1,"unitId":1,"stockMode":"SERIALIZED","listPrice":5200000,"description":"Mẫu laptop Dell Latitude","revision":0}
```

Mã lỗi: 400 dữ liệu sai; 401 chưa đăng nhập; 403 thiếu quyền/CSRF; 404 không tồn tại; 409 mã trùng, phiên bản cũ hoặc mục từ điển đang được dùng.
