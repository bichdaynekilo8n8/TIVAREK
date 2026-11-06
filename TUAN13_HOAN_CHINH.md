# TIVAREK — Tuần 13: Website sản phẩm công khai

## 1. Những phần đã bổ sung

- `/san-pham`: website công khai có tìm kiếm SKU/tên/thương hiệu, lọc danh mục, thương hiệu, loại hàng, tình trạng, khoảng giá, sắp xếp và phân trang 12 mục.
- `/san-pham/phu-kien/{id}`: chi tiết SKU BULK, giá, tồn khả dụng, ảnh, thông số.
- `/san-pham/thiet-bi/{id}`: chi tiết **từng chiếc** thiết bị SERIALIZED, ảnh thật của đúng DeviceID, tình trạng, kết quả kiểm tra và thông số riêng.
- `/admin/storefront`: nhân viên có quyền `PRODUCT_WRITE` / `DEVICE_WRITE` có thể đăng hoặc gỡ sản phẩm khỏi website, có kiểm tra dữ liệu và ghi AuditLogs.
- `/admin/storefront/products/{id}/images`: quản lý ảnh sản phẩm từ URL HTTPS hoặc `/images/...`; ảnh thiết bị tiếp tục dùng trình quản lý ảnh thật ở Tuần 11.
- `/media/public/devices/{deviceId}/images/{imageId}`: chỉ trả về ảnh của thiết bị **đã công khai**, thuộc cửa hàng và còn đáp ứng điều kiện hiển thị; không mở quyền đọc ảnh admin hay ảnh máy khách.
- Cập nhật API `/api/public/products` và mục sản phẩm trang chủ: không còn liệt kê SKU chưa đăng công khai.
- CSS responsive bằng Grid, không dùng CDN hay JavaScript bắt buộc.

## 2. Cách chạy (Windows + IntelliJ + SQL Server)

1. Sao lưu thư mục dự án và backup database `TIVAREK_WEEK8_DEV` trước khi cập nhật.
2. Giải nén ZIP vào thư mục mới. Mở đúng thư mục `TIVAREK` có `pom.xml` trong IntelliJ.
3. Chọn **Project SDK: Java 21** và **Maven Runner JRE: Java 21**.
4. Chạy **`CHAY_TUAN13.cmd`**. Nó hỏi SQL Server login/mật khẩu, làm sạch `target` rồi chạy ứng dụng bằng profile `dev`.
5. Nếu Flyway chưa áp dụng `V7__week13_public_catalog.sql`, ứng dụng sẽ áp dụng tự động. **Không chỉnh V1–V7** nếu từng migration đã chạy thành công.
6. Truy cập `http://127.0.0.1:8080/login`, đăng nhập tài khoản OWNER hoặc INVENTORY của bạn.
7. Trong quản trị chọn **Đăng lên website** (`http://127.0.0.1:8080/admin/storefront`). Đăng sản phẩm BULK hoặc từng máy SERIALIZED.
8. Mở `http://127.0.0.1:8080/san-pham` bằng cửa sổ ẩn danh để thử khách chưa đăng nhập.

**Lưu ý**: vì V7 đặt `IsPublished=0` mặc định, sản phẩm cũ **không tự công khai**. Đây là chủ ý để tránh lộ máy của khách, giá chưa duyệt hoặc hàng chưa chốt.

## 3. Thêm ảnh cho sản phẩm

- **Phụ kiện / BULK**: vào `/admin/storefront`, bấm **Quản lý ảnh**, nhập URL bắt đầu bằng `https://` hoặc `/images/ten-anh.jpg` (với tệp nằm trong `src/main/resources/static/images/`). Ảnh đầu tiên là ảnh đại diện. Có thể gỡ ảnh.
- **Laptop/TV cũ / SERIALIZED**: vào `/admin/devices/{id}`, tải ảnh thật qua tính năng Tuần 11. Sau đó trở lại `/admin/storefront` để công khai thiết bị. Ảnh riêng tư không được mở từ trang công khai trừ khi thiết bị đủ điều kiện.
- Không nhập ảnh `javascript:`, `data:`, `file:` hoặc đường dẫn `../`; các URL này không được dùng ở cửa hàng công khai.

## 4. Quy tắc hiển thị và tồn

| Loại | Điều kiện xuất hiện | Quy tắc còn hàng |
| --- | --- | --- |
| SKU BULK | `Products.IsPublished=1`, `IsActive=1`, danh mục active, `StockMode=BULK` | Tổng `BulkStockEntries.Delta` thuộc phiếu `POSTED` tại vị trí `SALEABLE` đang hoạt động, **trừ** đặt giữ BULK chưa hết hạn. Âm được hiển thị là 0. |
| Thiết bị SERIALIZED | `Devices.IsPublished=1`, `OwnerCustomerID IS NULL`, có mẫu SERIALIZED active, danh mục active, giá riêng | Lần di chuyển kho `POSTED` mới nhất đến vị trí `SALEABLE` active, không có khóa `ActiveDeviceReservations`. Nếu đã xuất hoặc giữ thì hiển thị hết hàng. |

Website là danh mục giới thiệu, **chưa nhận thanh toán/đơn online** (làm ở Tuần 15). Tồn có thể thay đổi ngay khi có giao dịch kho khác; cần xác nhận thực tế trước khi bán.

## 5. Kiểm thử

- Nhấp `KIEM_THU_TUAN13.cmd` để chạy kiểm thử H2 độc lập, gồm: 10 nhập/3 xuất còn 7, phiếu DRAFT không ảnh hưởng tồn, giữ hàng trừ số khả dụng, một laptop có tối đa một khả dụng, ảnh/thiết bị khách bị ẩn, CSRF và phân quyền, lọc đầu vào, ẩn SKU chưa công khai.
- Để kiểm tra toàn bộ hồi quy: trong PowerShell ở thư mục dự án chạy ` .\mvnw.cmd test` (bỏ dấu cách đầu trước dấu chấm; **không** dùng `\mvnw.cmd`).
- Để kiểm thử SQL Server thật: chạy `CHAY_TUAN13.cmd` trên database DEV và thực hiện các tác vụ từ giao diện. Kiểm tra migration ở SSMS:

```sql
SELECT installed_rank, version, description, success
FROM dbo.flyway_schema_history
ORDER BY installed_rank;

SELECT ProductID, SKU, Name, IsPublished FROM dbo.Products ORDER BY ProductID DESC;
SELECT DeviceID, AssetTag, OwnerCustomerID, IsPublished FROM dbo.Devices ORDER BY DeviceID DESC;
```

**Giới hạn của dự án đầu vào:** migration V6 Tuần 12 hiện chỉ cấp quyền/chỉ mục, chưa chứa giao diện hay nghiệp vụ nhập, xuất, chuyển, điều chỉnh kho. Tuần 13 có thể đọc chính xác chứng từ đã `POSTED` nhưng không thay thế phần còn thiếu của Tuần 12. Cần bổ sung nghiệp vụ kho trước khi triển khai cửa hàng thật.

## 6. Nếu gặp BUILD FAILURE

1. Tìm dòng `Caused by:` **đầu tiên** trong log, vì `BUILD FAILURE` chỉ là thông báo tổng kết.
2. Nếu `Invalid versioned migration name format`, kiểm tra tên file có **hai dấu gạch dưới** sau version, ví dụ `V7__week13_public_catalog.sql` (đừng có bản `V7_week13...sql` nằm trong `target/classes`). Dùng bộ chạy Tuần 13 đã `clean`.
3. Nếu lỗi SQL Server `Login failed`, kiểm tra SQL login bằng SSMS; không gửi mật khẩu qua chat.
4. Nếu `Flyway checksum mismatch`, **không sửa migration đã chạy, không dùng `clean` trên database**, hãy so sánh file SQL và lịch sử DB để xác định bản nào đang áp dụng.
