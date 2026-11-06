# TIVAREK — Tuần 11: Quản lý từng thiết bị

Đã tích hợp vào ứng dụng chính ở thư mục gốc chứa `pom.xml` và `src/`, tiếp nối đầy đủ Tuần 9–10. Thư mục `week8/example-app` giữ làm tài liệu tham khảo.

## 1. Khởi động

1. Dừng ứng dụng cũ đang dùng cổng 8080.
2. Giải nén vào đường dẫn không dấu, ví dụ `D:\Projects\TIVAREK`; mở thư mục có `pom.xml` bằng IntelliJ, dùng JDK 21 và Reload Maven.
3. Giữ nguyên database và thông tin kết nối đã dùng ở Tuần 10. Mặc định là `TIVAREK_WEEK8_DEV`; không xóa database và không chạy lại các script tạo bảng cũ.
4. Chạy `CHAY_TUAN11.cmd`, hoặc PowerShell từ thư mục dự án:

```powershell
.\scripts\start-week9.ps1
```

Script hỏi SQL login/mật khẩu nếu chưa được cấu hình. Nếu server hoặc cổng khác mặc định, dùng lại `TIVAREK_DB_URL`, `TIVAREK_DB_USER`, `TIVAREK_DB_PASSWORD` của Tuần 10.

5. Mở **http://127.0.0.1:8080/** → **Đăng nhập** → dùng tài khoản quản lý → **Thiết bị**.

Flyway tự chạy `V5__week11_device_management.sql`. Profile dev còn chạy `V5_1__week11_demo_devices.sql`; không sửa checksum các migration cũ. Hai máy minh họa W11-DEMO-LAP-001 và W11-DEMO-LAP-002 được thêm nếu mẫu DEMO-DELL-5420 còn tồn tại. Không tạo bút toán kho.

Khi chạy IntelliJ: main `com.tivarek.TivarekApplication`, profile `dev`, cấu hình biến kết nối tại Run Configuration như các tuần trước.

## 2. Tài khoản

Dùng lại tài khoản website Tuần 9 và mật khẩu đã đặt, không phải SQL login `tivarek_dev`.

| Tài khoản mẫu | Vai trò |
|---|---|
| w9.owner | Chủ cửa hàng |
| w9.inventory | Quản lý kho |
| w9.sales | Bán hàng |
| w9.tech | Kỹ thuật |
| w9.customer, w9.other | Khách hàng |

Không có mật khẩu website mặc định trong code. Nếu chưa từng tạo tài khoản, chạy `scripts/start-week9.ps1 -CreateDemoAccounts` và tự đặt mật khẩu khi được hỏi. Tài khoản đã tồn tại giữ mật khẩu cũ; chạy lại lệnh này không reset mật khẩu.

Sau khi nâng cấp quyền, đăng xuất rồi đăng nhập lại để session nhận quyền mới.

## 3. Sử dụng

### Tạo máy

1. Vào Sản phẩm, bảo đảm có mẫu đang kinh doanh thuộc `SERIALIZED`.
2. Vào Thiết bị → Thêm thiết bị.
3. Nhập mã riêng, ví dụ `TVK-LAP-000001`; chọn mẫu sản phẩm.
4. Nhập serial nếu đọc được, hoặc bỏ trống.
5. Chọn tình trạng, mô tả thực tế và lỗi/điểm cần lưu ý. Nếu chọn Có lỗi thì bắt buộc mô tả lỗi.
6. Nhập giá riêng; bỏ trống nếu chưa định giá. Giá 0 và chưa định giá là hai giá trị khác nhau.
7. Điền cấu hình phù hợp: CPU, RAM, SSD/HDD, màn hình, hệ điều hành. Không áp dụng thì để trống.
8. Lưu; trang chi tiết cho phép bổ sung ảnh và lần kiểm tra.

Mã máy tự bỏ khoảng trắng đầu/cuối, chuyển hoa, chỉ nhận chữ ASCII/số/chấm/gạch dưới/gạch ngang; tối đa 40 ký tự. Database có unique index trên mã chuẩn hóa để chặn trùng khi ghi đồng thời.

Mẫu sản phẩm được cố định sau khi tạo. Với hồ sơ cũ chưa có liên kết mẫu, có thể gán một mẫu SERIALIZED hợp lệ lần đầu. Mẫu ngừng kinh doanh không được dùng để tạo máy mới, nhưng vẫn sửa được hồ sơ máy đã liên kết với mẫu đó.

### Sửa máy

Mở Chi tiết → Chỉnh sửa. Giá, cấu hình, tình trạng và mô tả của máy được lưu riêng, không sửa máy khác hoặc mẫu sản phẩm.

Cột `Version` rowversion có sẵn trong SQL Server được dùng để chặn ghi đè. Khi gặp thông báo người khác đã thay đổi thiết bị, tải lại dữ liệu mới nhất trước khi lưu. Thêm/gỡ/sắp xếp ảnh và ghi kiểm tra cũng thay đổi phiên bản máy.

Serial không bắt buộc duy nhất toàn hệ thống. Khi trùng với máy khác của cửa hàng, trang chi tiết cảnh báo để bạn kiểm tra; mỗi máy vẫn có mã quản lý duy nhất.

### Ảnh thật

- Tải một hoặc nhiều JPEG/PNG, tối đa 10 ảnh/máy, 5 MB/ảnh.
- Mỗi chiều không quá 8.000 px và tổng không quá 16 triệu điểm ảnh.
- Server đọc nội dung thật, kiểm tra định dạng và mã hóa lại ảnh; không tin phần mở rộng/tên tệp tải lên.
- Ảnh đầu là đại diện. Dùng Làm đại diện, Trước/Sau để sắp xếp, Gỡ ảnh để bỏ ảnh sai.
- URL ảnh yêu cầu đăng nhập và quyền xem thiết bị. Ảnh không được mở công khai.
- Một tệp trong lô lỗi làm rollback cả lô. Lỗi ghi DB/audit dọn các tệp vừa tạo; chỉ xóa tệp cũ sau khi transaction gỡ ảnh commit thành công.

### Kiểm tra kỹ thuật

Trong chi tiết máy → Lịch sử kiểm tra, nhập nội dung và chọn Đạt/Không đạt/Cần sửa chữa. Người thực hiện lấy từ tài khoản đăng nhập; mỗi lần tạo bản ghi mới, không ghi đè kết quả cũ.

Máy chưa có bản ghi hiển thị Chưa kiểm tra. Máy đang đánh dấu Có lỗi không được ghi Đạt cho đến khi quản lý cập nhật tình trạng phù hợp. Kết quả kiểm tra không tự thay giá, tạo phiếu sửa hoặc xác nhận tồn kho.

## 4. Phân quyền

| Vai trò | Xem máy cửa hàng | Thêm/sửa hồ sơ và giá | Quản lý ảnh | Ghi kiểm tra |
|---|---|---|---|---|
| OWNER | Có | Có | Có | Có |
| INVENTORY | Có | Có | Có | Có |
| SALES | Có | Không | Không | Không |
| TECHNICIAN | Có | Không | Có | Có |
| CUSTOMER | Không | Không | Không | Không |

Quyền mới: `DEVICE_READ`, `DEVICE_WRITE`, `DEVICE_IMAGE_WRITE`, `DEVICE_CHECK_WRITE`. Backend kiểm tra quyền tại route và service, giữ bảo vệ CSRF.

Các route Tuần 11 chỉ phục vụ thiết bị `OwnerCustomerID IS NULL` của cửa hàng. Hồ sơ máy khách gửi sửa hiện có được giữ nguyên và bị loại khỏi danh sách, chi tiết, sửa, kiểm tra và ảnh của module này. Tiếp nhận máy khách sẽ triển khai cùng phiếu sửa ở giai đoạn sau.

## 5. Lưu và sao lưu ảnh

Mặc định ảnh nằm trong `var/device-images` tính từ thư mục khởi chạy, ngoài `src/` và `target/`. Script khởi chạy luôn chuyển về thư mục dự án.

Để ảnh ổn định khi đổi nơi đặt mã nguồn, nên cấu hình một đường dẫn tuyệt đối và giữ nguyên qua các lần nâng cấp:

```powershell
$env:TIVAREK_UPLOAD_DIR="D:\TIVAREK_DATA\device-images"
.\scripts\start-week9.ps1
```

Trong IntelliJ, đặt `TIVAREK_UPLOAD_DIR` tại Run Configuration. Cần quyền đọc/ghi tại thư mục này.

Database chỉ lưu khóa tệp. Phải sao lưu cả **SQL Server và thư mục ảnh** cùng thời điểm; chỉ chép mã nguồn hoặc chỉ backup DB sẽ không đủ. Khi đổi thư mục ảnh, chép toàn bộ tệp sang nơi mới và cập nhật biến môi trường. Không đưa ảnh thật vào Git.

Lỗi ghi thư mục ảnh trả thông báo không lưu được ảnh; kiểm tra đường dẫn, quyền ghi và dung lượng ổ đĩa. Nếu máy mất điện đúng lúc ghi, có thể còn tệp không được DB tham chiếu; sao lưu và kiểm tra trước khi dọn thủ công.

## 6. Kiểm thử

Không cần SQL Server, dùng H2 chỉ trong test:

```powershell
.\scripts\test-week11.ps1
```

Kiểm thử SQL Server thật trên máy bạn:

```powershell
.\scripts\test-week11.ps1 -SqlServer
```

Lệnh thứ hai bao gồm test Tuần 9–11, chỉ cho phép database thực hành `TIVAREK_WEEK8_DEV`; không chạy trên DB cửa hàng đang vận hành. Test Tuần 11 tạo fixture riêng, commit các luồng thật và dọn fixture sau test. Flyway cập nhật schema trước khi test.

H2 dùng trigger mô phỏng thay đổi token phiên bản để kiểm thử logic; SQL Server thực tế sử dụng rowversion gốc. Kết quả H2 không chứng minh migration, chỉ mục computed column hoặc khóa SQL Server đã hoạt động trên máy bạn.

Chạy thêm `sql/08_week11_verify.sql` bằng SSMS. Chi tiết kết quả build/test trong gói nằm ở `KET_QUA_KIEM_THU_TUAN11.md`.

## 7. Nghiệm thu bảy buổi

| Buổi | Nội dung tự thực hành | Điều kiện đạt |
|---|---|---|
| 1 | Đọc luồng Products → Devices, mở ba màn hình | Phân biệt mẫu sản phẩm và từng máy |
| 2 | Xem migration V5 và dữ liệu trong SSMS | FK, mã riêng, cấu hình và Version đúng |
| 3 | Thêm hai máy cùng mẫu qua API | Mã, cấu hình và giá độc lập |
| 4 | Thêm/sửa/tìm/lọc bằng giao diện | Biểu mẫu lỗi giữ dữ liệu; phân trang hoạt động |
| 5 | Tải nhiều ảnh, đổi đại diện, sắp xếp, gỡ | Ảnh đúng máy và còn sau khởi động lại |
| 6 | Đăng nhập các vai trò, ghi kiểm tra | Chặn vượt quyền; audit đúng người |
| 7 | Chạy test và đối chiếu SQL Server | Hoàn thành checklist bên dưới |

Checklist cuối tuần:

1. Tạo hai máy Dell Latitude 5420, mã khác nhau: RAM 16/8 GB, SSD 256/512 GB, giá 5.200.000/4.700.000.
2. Tải ảnh riêng cho từng máy; đổi ảnh đại diện và thứ tự.
3. Ghi ít nhất một lần kiểm tra; xem lịch sử và tài khoản thực hiện.
4. Sửa máy 01; máy 02 và giá tham khảo của Products không đổi.
5. Mở hai tab cùng máy, lưu tab đầu rồi tab còn lại: phải báo phiên bản cũ.
6. Thử mã trùng, giá âm, RAM sai kiểu, tệp giả ảnh: phải bị từ chối.
7. Đăng nhập SALES/TECHNICIAN và thử đổi giá qua API: phải bị chặn.
8. Đăng xuất, đăng nhập lại và tắt/mở ứng dụng; dữ liệu và ảnh vẫn còn.
9. Kiểm tra SSMS, nhật ký và các tính năng Tuần 9–10.

## 8. API

Đăng nhập bằng session như Tuần 10. Lấy token mới ở `/api/csrf` sau login; gửi header `X-CSRF-TOKEN` cho mọi thao tác ghi. Multipart dùng trường `files` có thể lặp lại.

| Method | Đường dẫn | Chức năng |
|---|---|---|
| GET | `/api/admin/devices` | Tìm/lọc; trả items, page, hasNext |
| GET | `/api/admin/devices/metadata` | Mẫu SERIALIZED, định nghĩa thông số, bộ lọc |
| GET | `/api/admin/devices/{id}` | Thiết bị, cấu hình, ảnh, kiểm tra và cảnh báo serial |
| POST | `/api/admin/devices` | Tạo thiết bị, trả 201 |
| PUT | `/api/admin/devices/{id}` | Sửa, gửi version mới nhất |
| POST | `/api/admin/devices/{id}/checks` | Thêm lần kiểm tra, trả 201 |
| POST | `/api/admin/devices/{id}/images` | Multipart: files, version |
| GET | `/api/admin/devices/{id}/images/{imageId}/content` | Nội dung ảnh có bảo vệ |
| DELETE | `/api/admin/devices/{id}/images/{imageId}?version=...` | Gỡ ảnh |
| PUT | `/api/admin/devices/{id}/images/order` | Toàn bộ imageIds theo thứ tự và version |

Bộ lọc danh sách: `q`, `productId`, `categoryId`, `brandId`, `grade`, `minPrice`, `maxPrice`, `page` bắt đầu từ 0. Mỗi trang 25 dòng.

Ví dụ tạo máy, thay productId bằng ID SERIALIZED thực tế:

```json
{"assetTag":"TVK-LAP-000001","productId":1,"manufacturerSerial":"ABC123","description":"Vỏ trầy nhẹ, kèm sạc","conditionGrade":"GOOD","askingPrice":5200000,"faultDescription":"Trầy nhẹ ở nắp","version":"","specifications":{"CPU":"Intel Core i5","RAM_GB":"16","SSD_GB":"256","SCREEN_INCH":"14"}}
```

Khi sửa, lấy `device.version` từ GET chi tiết: chuỗi hex 16 ký tự không có tiền tố 0x. Không lấy `Version` làm ngày giờ.

Ví dụ kiểm tra:

```json
{"findings":"Đã kiểm tra màn hình, bàn phím và cổng USB","result":"PASS","version":"0000000000001234"}
```

Mã lỗi: 400 dữ liệu sai; 401 chưa đăng nhập; 403 thiếu quyền/CSRF; 404 không tồn tại hoặc nằm ngoài module máy cửa hàng; 409 mã trùng/phiên bản cũ; 413 yêu cầu tải lên quá lớn; 503 thư mục ảnh không ghi được.

## 9. Các phần để Tuần 12 trở đi

Chưa tạo tồn kho, vị trí kho, phiếu nhập/xuất, đặt giữ, đơn bán hoặc phiếu sửa mới. Các bảng/nghiệp vụ mẫu cũ giữ nguyên. Có hồ sơ máy hoặc kết quả kiểm tra Đạt không đồng nghĩa Còn hàng hay Có thể bán.
