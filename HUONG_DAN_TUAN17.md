# TIVAREK — Tuần 17: Dashboard, báo cáo, kiểm thử

## Đã tích hợp trong mã nguồn

- Dashboard tổng hợp `GET /admin/reports` (chỉ OWNER).
- Báo cáo bán hàng `/admin/reports/sales`: trạng thái theo kênh, top 20 mặt hàng từ đơn hoàn tất, 30 phiếu trả hàng gần nhất.
- Báo cáo tài chính `/admin/reports/finance`: thu bán, thu sửa, hoàn tiền, chi phí và dòng tiền ròng theo ngày.
- Báo cáo sửa chữa `/admin/reports/repairs`: phiếu theo trạng thái trong kỳ, 30 phiếu đang mở được ưu tiên theo trễ hạn.
- Báo cáo kho `/admin/reports/stock`: 150 SKU dưới ngưỡng 5 đơn vị, tồn theo vị trí và máy riêng lẻ có thể bán.
- Báo cáo bảo hành `/admin/reports/warranties`: yêu cầu theo trạng thái, bảo hành còn hiệu lực, 30 bảo hành sắp hết hạn.
- CSV UTF-8 BOM (mở bằng Excel) cho mỗi nhóm báo cáo qua `/admin/reports/export/{sales|finance|repairs|stock|warranties}`. Chỉ xuất bảng chi tiết chính của từng nhóm; không xuất token bí mật.
- Giao diện responsive hỗ trợ 1440px và 390px, lọc từ/đến ngày, tiếng Việt và trạng thái chưa có dữ liệu.
- Phân quyền bằng cả HTTP matcher và `@PreAuthorize`: OWNER xem tất cả, SALES xem bán/bảo hành, INVENTORY xem kho, TECHNICIAN xem sửa/bảo hành, CUSTOMER không có quyền.

## 1. Chuẩn bị và chạy trên Windows

1. Dùng JDK 21 và SQL Server đã tạo CSDL TIVAREK; sao lưu CSDL trước khi nâng cấp.
2. Mở thư mục `TIVAREK` chứa `pom.xml` trong IntelliJ.
3. Kiểm tra `src/main/resources/application-dev.properties` và các biến môi trường kết nối DB. Đừng đưa mật khẩu DB vào GitHub.
4. Mở PowerShell tại thư mục dự án, chạy:

   ```powershell
   java -version
   .\mvnw.cmd clean test
   .\mvnw.cmd spring-boot:run
   ```

   Hoặc chạy `CHAY_TUAN17.cmd` từ File Explorer. Nếu `clean test` thất bại, **không** bỏ qua để triển khai thật.
5. Mở `http://localhost:8080/login`, đăng nhập tài khoản `OWNER`, vào `/admin` và chọn “Mở Dashboard & báo cáo”.
6. Flyway chạy migration `V11__week17_reports.sql` *tự động* khi kết nối SQL Server; giữ nguyên V1–V10, không sửa lại các migration cũ.

## 2. Quy ước số liệu quan trọng

| Chỉ tiêu | Nguồn dữ liệu | Quy ước |
|---|---|---|
| Đơn mới | `SalesOrders.OrderedAt` | Ngày tạo đơn |
| Đơn hoàn tất | `SalesOrders.CompletedAt` | Chỉ ghi khi workflow chuyển sang `COMPLETED` từ Tuần 17 |
| Giá trị hàng đã hoàn tất | `SalesLines` + dòng BULK/DEVICE | Theo ngày hoàn tất, chưa trừ hàng trả và không gồm phí giao |
| Thu / hoàn | `Payments` | Chỉ `Status='SUCCEEDED'`; `Direction=IN/OUT`, theo `OccurredAt` |
| Dòng tiền ròng | Thu bán + thu sửa − hoàn tiền − `Expenses` | **Không phải lợi nhuận**, chưa tính đầy đủ giá vốn |
| Kho | `InventoryDocuments` `POSTED` + sổ `BulkStockEntries` | Chỉ kho `SALEABLE`, trừ giữ hàng chưa hết hạn |
| Máy có thể bán | Trạng thái máy từ chứng từ POSTED mới nhất | Có vị trí `SALEABLE` và không thuộc khách gửi sửa |
| Phiếu sửa | `RepairTickets` | Thống kê trong kỳ theo ngày tiếp nhận; bảng đang mở lấy thời điểm hiện tại |
| Bảo hành | `Warranties`, `WarrantyClaims` | Tình trạng hiện tại và ngày yêu cầu trong kỳ |

**Lưu ý dữ liệu lịch sử:** Không thể biết chính xác thời điểm các đơn đã `COMPLETED` trước khi có `CompletedAt`. Migration để trường này `NULL` cho đơn lịch sử. Không nên gán `OrderedAt` làm `CompletedAt` vì sẽ tạo doanh thu kỳ sai. Nếu muốn thống kê doanh thu hoàn tất lịch sử, cần nhập lại dữ liệu đối soát từ chứng từ thật.

**Khoảng ngày:** Việt Nam UTC+7, hai đầu ngày đều được tính; SQL lọc theo `>= fromUtc AND < toExclusiveUtc`. Khoảng tối đa 366 ngày để tránh quét dữ liệu quá lớn.

**Giới hạn bảng:** Top 20 sản phẩm; 30 phiếu trả; 30 phiếu sửa mở; 150 SKU tồn thấp; 30 bảo hành sắp hết hạn. CSV của mỗi nhóm xuất đúng những hàng đang có trong bảng chính; đây không phải bản xuất toàn bộ CSDL.

## 3. Kiểm tra nghiệm thu

- [ ] `java -version` là Java 21.
- [ ] `mvnw.cmd clean test` thành công, Flyway không báo lỗi V11.
- [ ] OWNER mở được cả 6 trang, lọc ngày không mất dữ liệu và tải được 5 loại CSV.
- [ ] SALES không truy cập được `/admin/reports/finance` và không tải CSV tài chính (HTTP 403).
- [ ] INVENTORY chỉ có báo cáo kho, TECHNICIAN chỉ có sửa và bảo hành; CUSTOMER bị chặn.
- [ ] Các khoản thu chưa thành công không góp vào dòng tiền; hoàn tiền ghi âm vào dòng tiền.
- [ ] Đơn hoàn tất mới có `CompletedAt`; đơn tạo/chưa hoàn tất không được tính giá trị hàng hoàn tất.
- [ ] Đơn đã hoàn tất cũ với `CompletedAt=NULL` không bị tự quy về kỳ gần nhất.
- [ ] Chứng từ nháp không góp tồn; giữ hàng ACTIVE còn hạn làm giảm số lượng khả dụng.
- [ ] Phiếu sửa trễ ngày hẹn được ưu tiên, bảo hành hết hạn không hiện trong “sắp hết hạn”.
- [ ] Trên màn hình 390px, thẻ KPI xếp dọc, tab cuộn ngang và bảng cuộn ngang; không tràn màn hình.

## 4. Tệp mới và tệp sửa

| Tệp | Tác dụng |
|---|---|
| `src/main/resources/db/migration/V11__week17_reports.sql` | Thời điểm hoàn tất, chỉ mục, quyền báo cáo |
| `src/main/java/com/tivarek/week17/ReportRange.java` | Ngày Việt Nam -> UTC |
| `src/main/java/com/tivarek/week17/ReportService.java` | Truy vấn báo cáo từ SQL Server |
| `src/main/java/com/tivarek/week17/ReportController.java` | Routes, phân quyền, xuất CSV |
| `src/main/resources/templates/week17/reports.html` | Dashboard và 5 nhóm báo cáo |
| `src/main/resources/static/css/week17.css` | Responsive UI |
| `src/test/java/com/tivarek/week17/*` | Test bộ lọc, CSV, phân quyền |
| `src/main/java/com/tivarek/week15/SaleWorkflowService.java` | Ghi `CompletedAt` khi hoàn tất |
| `src/main/java/com/tivarek/config/SecurityConfig.java` | Bảo vệ routes HTTP |
| `src/main/java/com/tivarek/week8/PageController.java`, `templates/admin.html` | Link đến báo cáo theo quyền |

## 5. Lưu ý triển khai

GitHub Pages chỉ lưu trang tĩnh, **không thể** chạy Spring Boot/SQL Server trực tiếp. Với Tuần 18, bạn có thể tiếp tục lưu source trên GitHub nhưng backend và SQL Server cần chạy trên máy riêng/VPS/nền tảng hỗ trợ Java + CSDL. Trước khi public thật cần hoàn thiện bảo vệ secrets, backup DB, HTTPS, logs và triển khai môi trường production.

**Trạng thái kiểm thử trong môi trường tạo bản này:** cấu trúc nguồn, SQL/tham chiếu, template và tệp ZIP được soát tĩnh; do máy tạo bản này chỉ có Java 17 và không cài Maven/JDK 21 hoặc SQL Server, các bài test Spring Boot/SQL Server phải chạy lại trên máy của bạn. Không thể cam kết "không lỗi runtime" khi chưa chạy được các bài test đó.
