# TIVAREK — Kho Tuần 12 + Sửa chữa Tuần 14 + Đăng ký tài khoản

## Những chức năng đã thêm

### Tuần 12 — Quản lý kho

- `/admin/stock`: tồn kho theo vị trí, chứng từ, vị trí hiện tại từng máy serial, nhà cung cấp và vị trí kho.
- Phiếu kho **DRAFT** chưa làm đổi tồn; thao tác **POSTED** kiểm tra lại tồn khả dụng, kiểm tra vị trí máy và khóa ghi đồng thời bằng giao dịch SQL Server `SERIALIZABLE`.
- Phiếu nhập từ nhà cung cấp `RECEIPT`, xuất bán `SALE_ISSUE` **phải gắn với SaleLineID có đơn bán xác nhận**, chuyển kho `TRANSFER`, điều chỉnh có lý do `ADJUSTMENT`.
- Máy SERIALIZED có đúng một vị trí theo chứng từ mới nhất; không đưa thiết bị của khách (`OwnerCustomerID != NULL`) vào sổ kho cửa hàng.
- Đặt giữ hàng BULK/DEVICE cho đơn, hạn dùng, nhả giữ và tự hết hạn; giữ máy độc quyền qua `ActiveDeviceReservations`.
- Phiếu REVERSAL có bút toán bù; giữ nguyên chứng từ gốc. Từ chối đảo nếu không còn đủ tồn hoặc máy đã di chuyển/đang giữ.
- Nhật ký thao tác AuditLogs và phân quyền Tuần 12 trong migration V6.

**Giới hạn nghiệp vụ cố ý:** Dòng `SALE_ISSUE` phải có đơn bán hợp lệ từ Tuần 15, không được phát sinh xuất bán vô chủ. Bảo hành/hoàn tiền và hoàn trả của giao dịch bán sẽ bổ sung ở Tuần 15–16. Không dùng nhập hàng của khách để tăng hàng của cửa hàng.

### Tuần 14 — Tiếp nhận và sửa chữa

- `/sua-chua`: khách gửi yêu cầu online vào `RepairRequests`, mã yêu cầu riêng.
- `/admin/repairs`: nhân viên liên hệ, tạo `RepairTickets`, `Devices.OwnerCustomerID`, phụ kiện nhận máy, phân công kỹ thuật, tạo mã tra cứu mạnh chỉ hiển thị **một lần**.
- `/admin/repairs/{id}`: cập nhật trạng thái theo luồng hợp lệ, các `RepairEvents` append-only, nhật ký kỹ thuật, danh mục công sửa, báo giá gồm nhiều dòng công/lắp linh kiện, khách duyệt hoặc từ chối có chứng cứ.
- Ảnh hiện trạng trước/sau sửa: JPEG/PNG tối đa 5 MB, làm sạch metadata bằng cách mã hóa lại ảnh, lưu cùng vùng ảnh thiết bị, chỉ nhân viên có quyền sửa chữa được đọc; tối đa 20 ảnh một máy.
- `/account/repairs`: khách đã có tài khoản xem phiếu của chính mình và tự xác nhận báo giá.
- `/tra-cuu-sua-chua`: tra cứu bằng mã phiếu và mã bí mật, chỉ hiện sự kiện đánh dấu công khai; database chỉ lưu SHA-256 của mã bí mật.
- `RepairPartUsages` + `RepairBulkIssues`/`RepairDeviceIssues` + `InventoryDocuments` ghi xuất linh kiện BULK hoặc SERIALIZED theo báo giá `ACCEPTED`, chặn số lượng vượt được duyệt, chặn tồn không đủ, chặn máy serial đang được giữ.
- Bàn giao chỉ khi `READY` và thanh toán đủ; ghi `Payments` cho tiền mặt/chuyển khoản, `ClosedAt`, `DELIVERED`, sự kiện và audit cùng một giao dịch.
- `V8__week14_repair_permissions.sql`: chỉ thêm quyền và các vị trí kho mẫu, **không sửa V1–V7**.

### Tài khoản database

- `/register` tạo `Users` (mật khẩu BCrypt), `UserRoles` chỉ `CUSTOMER`, `Customers.UserID` và AuditLogs trong một giao dịch; `/login` dùng cơ chế SQL Server có sẵn.
- Không cho client tự chọn quyền OWNER/ADMIN.
- **Bản vá đăng ký sau V8:** `V8_1__ensure_customer_registration_role.sql` tự bổ sung vai trò `CUSTOMER` và đúng quyền `CUSTOMER_SELF_READ` nếu database cũ bị thiếu. Không xóa bảng, không cấp quyền quản trị, không sửa checksum migration V3/V8 đã chạy.

## Cách chạy trên máy Windows

1. **Backup SQL Server** `TIVAREK_WEEK8_DEV` trước khi nâng cấp. Sao lưu cả thư mục dự án.
2. Giải nén ZIP vào thư mục **mới**, mở thư mục `TIVAREK` có `pom.xml` trong IntelliJ IDEA.
3. JDK và Maven Runner: **Java 21**. SQL Server dịch vụ đang chạy, database và login đã tạo, đủ quyền migration.
   - Nếu dùng ảnh hiện trạng, đặt `TIVAREK_UPLOAD_DIR` theo hướng dẫn Tuần 11 và sao lưu cả thư mục ảnh lẫn SQL Server.
4. Nhấp đúp `CHAY_TUAN14.cmd`, nhập SQL Server login và mật khẩu. Script chỉ chạy `mvn clean` (dọn bản biên dịch), **không chạy `flyway clean`**.
5. Đợi dòng `Started TivarekApplication`, mở:
   - `http://127.0.0.1:8080/register` — Đăng ký khách hàng.
   - `http://127.0.0.1:8080/login` — Đăng nhập.
   - `http://127.0.0.1:8080/admin/stock` — Quản lý kho (OWNER/INVENTORY).
   - `http://127.0.0.1:8080/admin/repairs` — Quản lý sửa chữa (OWNER/SALES/TECHNICIAN theo quyền).
   - `http://127.0.0.1:8080/sua-chua` — Khách gửi yêu cầu.
   - `http://127.0.0.1:8080/tra-cuu-sua-chua` — Tra cứu tiến độ.
   - `http://127.0.0.1:8080/account/repairs` — Khách đã đăng nhập xem yêu cầu của mình.
6. Nếu SQL Server đã có dữ liệu từ Tuần 13, Flyway tự thêm V8; không đổi V6/V7 cũ, không xóa database.

## Thử nghiệm thủ công (đúng thứ tự)

1. Đăng nhập OWNER. Tạo nhà cung cấp và vị trí kho nếu chưa có, tạo một sản phẩm BULK ở `/admin/products`.
2. `/admin/stock` tạo phiếu RECEIPT 10 món, chọn nhà cung cấp/vị trí; trước ghi sổ tồn chưa tăng. Bấm **Ghi sổ**, tồn tăng đúng 10.
3. Tạo TRANSFER 3 món từ kho bán sang kho cách ly; bấm ghi sổ, hai vị trí thay đổi -3/+3. Thử xuất 999 món phải bị từ chối.
4. Thử tạo ADJUSTMENT giảm 1 món có lý do và ghi sổ. Thử REVERSAL phiếu điều chỉnh nếu còn đủ tồn.
5. Mở `/sua-chua` ở cửa sổ riêng, đăng ký sửa TV. OWNER xem yêu cầu ở `/admin/repairs`, tạo phiếu tiếp nhận; **chép mã phiếu và mã bí mật cho khách**.
6. Truy cập `/tra-cuu-sua-chua` kiểm tra mã đúng/sai. Thay đổi trạng thái `DIAGNOSING`, ghi sự kiện công khai.
7. Tạo công sửa trong danh mục, lập báo giá, thêm dòng, gửi báo giá, ghi bằng chứng khách đồng ý. Chuyển sang sửa chữa, ghi nhật ký.
8. Với báo giá có linh kiện BULK, chọn `QuoteItemID`, vị trí và số lượng, ghi xuất sử dụng. Kiểm tra tồn kho giảm và `RepairPartUsages` xuất hiện.
9. Chuyển `READY`, nhập số tiền cần thu, ghi biên nhận và bàn giao. Kiểm tra `DELIVERED` và `Payments`.
10. Bấm bàn giao lần thứ hai: phải bị từ chối; thử khách khác xem phiếu tài khoản không được phép.

Kiểm thử Maven: `KIEM_THU_TUAN14.cmd`. Chỉ kiểm tra được môi trường SQL Server thực khi bạn chạy trên máy có JDK 21 + SQL Server.

## Điều kiện triển khai Internet

Không mở công khai ngay khi chưa có HTTPS/TLS, chống spam và giới hạn tốc độ cho đăng ký/tra cứu, xác minh email/số điện thoại, sao lưu dữ liệu, phương án phục hồi sự cố và kiểm thử bảo mật. GitHub Pages chỉ host frontend tĩnh, **không thể tự chạy backend Spring Boot + SQL Server**.
