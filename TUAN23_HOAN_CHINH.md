# TIVAREK — Tuần 23: UI/UX Responsive & Trải nghiệm khách hàng

Phiên bản này **kế thừa nguyên vẹn nghiệp vụ Tuần 22**, không đổi bảng, dữ liệu, migration Flyway, endpoint, phân quyền hay quy trình giao dịch.

## 1. Hạng mục đã thực hiện

| Khu vực | Thay đổi |
| --- | --- |
| Giao diện dùng chung | `css/week23.css`: thiết kế navy–đồng, typography rõ, focus, responsive, trạng thái lỗi/thành công, bảng cuộn ngang trên màn nhỏ, reduced motion. |
| Trang chủ Spring Boot (`/`) | Hero, danh mục, sản phẩm vừa cập nhật, sửa chữa, thông tin liên hệ, điều hướng mobile. Sản phẩm đề xuất lấy từ danh sách **đã công bố thực tế** và số lượng có thể bán của Tuần 13, tối đa 6 mẫu. |
| Trang danh sách/chi tiết | Giữ API lọc, tìm kiếm, ảnh, số lượng tồn và chính sách hiển thị; cải thiện style và khả năng dùng ở điện thoại. |
| Đăng ký sửa (`/sua-chua`) | Form có nhãn, gợi ý điền, thông báo, quy trình 4 bước. Dùng lại đúng các field backend `name`, `phone`, `problem`, `preferredAt`. |
| Tra cứu sửa (`/tra-cuu-sua-chua`) | Form bảo mật cho mã phiếu và mã tra cứu; timeline thể hiện trạng thái bằng tiếng Việt; không công khai dữ liệu ngoài API gốc. |
| Giỏ hàng (`/gio-hang`) | Bảng số lượng và tổng tiền, hỗ trợ trạng thái trống, nút đặt hàng; giữ nguyên các POST của Tuần 15. |
| Đặt hàng (`/thanh-toan`) | Form nhận tại quầy/giao tận nơi; địa chỉ bắt buộc khi chọn giao hàng; thông báo chưa thanh toán trực tuyến. |
| Thu ngân | Giao diện `admin/orders/new` dùng cùng template checkout, giữ danh sách khách cũ và trường `counterCustomerId`. |
| Admin/reports/repair/kho | Các CSS Tuần 8–22 cùng import lớp cải thiện hiển thị, khoảng cách, thông báo, table và responsive. |
| GitHub Pages | Cập nhật `/docs/index.html` (landing tĩnh), không giả lập các chức năng cần Java/SQL Server. |

### Điều chỉnh kỹ thuật

- `HomeController` lấy tối đa 6 sản phẩm thật qua `StorefrontService.search(...)` với filter mặc định. Khi chưa công bố sản phẩm, hiển thị thông điệp trung thực. Data `products` cũ vẫn được giữ để không phá hồi quy.
- `RepairController` cấp bản đồ nhãn trạng thái cho view Thymeleaf, không sửa trạng thái lưu DB.
- `week23-forms.js` nâng cấp phía trình duyệt: chống nhấn submit liên tục và đồng bộ `required` cho địa chỉ giao hàng. Server vẫn chịu trách nhiệm validation, chống thao tác trùng và phân quyền.
- `@MockitoBean StorefrontService` được bổ sung trong bài test MVC cũ; thêm 2 bài test controller tại `src/test/java/com/tivarek/week23`.
- CSS được dùng bằng `@import url("week23.css")` ở đầu các stylesheet cũ để giảm rủi ro sửa các template backend khác. Không thêm dependency frontend, CDN hay Javascript ngoài.

## 2. Cách chạy trên Windows

Yêu cầu JDK 21, Maven Wrapper có sẵn, SQL Server DEV đã thiết lập theo Tuần 22.

1. Sao lưu mã nguồn và **SQL Server DEV** trước khi nghiệm thu giao dịch.
2. Chạy `KIEM_THU_TUAN23.cmd`. Script kế thừa `KIEM_THU_TUAN22.cmd`, thực hiện `mvnw.cmd ... clean verify` với profile test.
3. Chạy `CHAY_TUAN23.cmd` để mở website localhost từ cấu hình DEV của Tuần 21/22.
4. Trong PowerShell chạy:

   ```powershell
   powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\smoke-week23-ui.ps1
   ```

5. Kiểm thử thủ công tại các kích thước 320px, 390px, 768px, 1024px, 1440px theo `PHIEU_UAT_TUAN23.md`.

## 3. Các đường dẫn sử dụng

- Khách hàng: `/`, `/san-pham`, `/gio-hang`, `/thanh-toan`, `/sua-chua`, `/tra-cuu-sua-chua`, `/tra-cuu-bao-hanh`.
- Nhân viên: `/admin`, `/admin/orders`, `/admin/orders/new`, `/admin/stock`, `/admin/repairs`, `/admin/reports`. Vẫn yêu cầu quyền đã có.
- Landing GitHub Pages: bật Pages từ nhánh chứa thư mục `/docs`. **Chỉ tĩnh; không chạy Spring Boot hay SQL Server trên GitHub Pages**.

## 4. Lưu ý an toàn và hạn chế

- Không công bố số lượng tồn chưa được kiểm chứng; giá hiển thị từ DB, chưa gồm phí vận chuyển tự động.
- Không tạo cổng thanh toán online hoặc hứa hẹn xử lý đơn tức thời.
- Mã tra cứu sửa chữa vẫn gồm mã phiếu **và** mã bảo mật; không bỏ cơ chế này vì mục đích đẹp giao diện.
- Các tác vụ ghi vẫn do backend Spring Security, CSRF và validation xử lý.
- Kiểm tra với dữ liệu giả trong DEV trước. Chỉ sau khi UAT đủ mới triển khai dùng thật cho gia đình.

Chi tiết giới hạn kiểm thử: [KET_QUA_KIEM_THU_TUAN23.md](KET_QUA_KIEM_THU_TUAN23.md).
