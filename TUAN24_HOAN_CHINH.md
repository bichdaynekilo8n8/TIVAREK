# TIVAREK — TUẦN 24: HIỆU NĂNG, SEO VÀ TRẢI NGHIỆM

Bản nâng cấp từ TIVAREK(7).zip, ngày 06/11/2026. **Đã triển khai mã nguồn; chưa nghiệm thu đủ 12/12 để triển khai thực tế.** Các gate còn chờ được ghi trong KET_QUA_KIEM_THU_TUAN24.md và PHIEU_UAT_TUAN24.md.

## Chạy trên Windows

Giải nén vào thư mục riêng, chọn JDK 21 cho Project SDK và Maven JVM. Có SQL Server DEV cùng cấu hình đang dùng ở Tuần 23. Không chép thư mục target từ bản cũ. Không ghi mật khẩu database vào source.

```bat
KIEM_THU_TUAN24.cmd
CHAY_TUAN24.cmd
```

Khi ứng dụng đang chạy, mở cửa sổ PowerShell khác:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts\smoke-week24-ui.ps1
```

`KIEM_THU_TUAN24.cmd` gọi chuỗi kiểm thử cũ, cuối cùng chạy Maven clean verify / H2. `CHAY_TUAN24.cmd` giữ quy trình hỏi mật khẩu SQL Server DEV của các tuần trước. Đối với SQL Server, tiếp tục chạy `KIEM_THU_TUAN22_SQLSERVER.cmd` trên database DEV riêng. H2 không thay thế kiểm thử migration, locking và concurrency SQL Server.

## Đã triển khai

- Giảm dung lượng logo/favicon/danh thiếp ở backend và GitHub Pages; giữ URL PNG tương thích.
- Bỏ CSS import nối tiếp, giữ thứ tự CSS Tuần 23 → CSS theo trang → override Tuần 24.
- Nén HTTP cho HTML/CSS/JS/JSON/XML trên server thật; gắn phiên bản nội dung cho CSS/JS/ảnh classpath; cache tĩnh một giờ và revalidation.
- HTML/API/media động được ép `private, no-store, max-age=0`. Không đưa tồn kho hay dữ liệu khách hàng vào shared cache; không thêm service worker.
- Chuẩn hóa ảnh danh mục/gallery/chi tiết, ưu tiên ảnh bìa, giới hạn cạnh dài ảnh upload mới 1600px. Ảnh PNG giữ alpha, ảnh nhỏ không phóng lớn. File ảnh cũ không bị sửa.
- Chi tiết phụ kiện đọc publication một lần, sau đó đọc specs/photos. Các API service cũ vẫn còn để những luồng khác tương thích. Không thêm cache kho hay thay schema.
- Robots/sitemap backend, canonical từ origin đáng tin cậy, metadata riêng cho trang công khai, LocalBusiness với tên/địa chỉ/liên hệ đang hiển thị. Không tạo review, giờ mở cửa, tọa độ hoặc URL giả.
- Sitemap chỉ gồm home/danh mục/đăng ký sửa và ID sản phẩm/thiết bị hợp lệ theo publication. Không đưa đơn hàng, tra cứu hoặc thiết bị khách vào. Giới hạn 45.000 mặt hàng để giữ dưới 50.000 URL; nếu quy mô vượt ngưỡng cần sitemap index.
- Skip link tới main, focus bàn phím, min-height input/button, font nhập liệu mobile, xử lý chữ dài, giảm chuyển động, sửa bố cục filter ở tablet; trang lỗi 403/404/500 chung không lộ chi tiết nội bộ. Các response 403 từ Security và CatalogProblem hiện vẫn giữ thông báo an toàn theo cơ chế cũ.
- Sửa các lỗi nền được phát hiện bằng baseline: bộ test MVC thiếu LoginAttemptGuard; kiểm tra redirect theo `/login`; audit phân loại route từ URI đã bỏ context path; precheck role CUSTOMER trước khi insert User; test logo hỗ trợ URL fingerprint.

## SEO backend

DEV để `TIVAREK_PUBLIC_BASE_URL` trống. robots sẽ Disallow /, sitemap trả 404, trang trả noindex, không phát canonical localhost.

Chỉ khi đã có hostname HTTPS thật cho ứng dụng Spring Boot, khai báo biến môi trường `TIVAREK_PUBLIC_BASE_URL` là origin, ví dụ cú pháp `https://ten-mien-cua-ban` (không chép URL ví dụ làm cấu hình thật). Không có đường dẫn, query, fragment hoặc credentials. Ứng dụng kiểm tra cấu hình và fail fast nếu sai. Biến `.env.example` chỉ để tham khảo; không mặc định nạp file .env vào tiến trình.

Host/X-Forwarded-Host không được dùng để xây canonical/sitemap. Query tìm kiếm/lọc, trang riêng tư, POST, redirect và lỗi luôn có noindex. Các trang HTML dù công khai vẫn no-store để tránh chia sẻ session/CSRF. SEO index không đồng nghĩa với public caching.

## GitHub Pages

`docs/` tiếp tục chỉ chứa trang giới thiệu tĩnh. Không kết nối SQL Server, không lưu giỏ hàng hay dữ liệu đăng nhập. Chưa có URL Pages chính xác trong ZIP nên sitemap ban đầu là urlset rỗng hợp lệ, không chứa domain giả, và chưa có canonical cứng.

Trước khi đưa Pages lên, chạy bằng Python 3:

```text
python scripts/build-pages-seo.py --base-url URL_HTTPS_PAGES_THAT
```

URL phải gồm `/TIVAREK/` nếu GitHub cung cấp đường dẫn project như vậy. Script cập nhật canonical, robots, sitemap chỉ trong docs, không trỏ về app localhost. Với GitHub Pages project site, robots.txt nằm dưới đường dẫn repo không điều khiển robots ở gốc domain; có thể gửi sitemap đúng URL qua Search Console. Không chạy công cụ này bằng URL ví dụ rồi commit nhầm. Không thực hiện publish trong đợt bàn giao này.

## Dữ liệu và vận hành

Không đổi pom, phiên bản framework, migration SQL hay script tạo database. Kiểm tra checksum migrations trong reports/week24. Trước khi nâng cấp thật vẫn sao lưu database và thư mục ảnh; rollback bằng source tuần trước, không sửa hay chạy lại migration cũ. Các tác vụ tuần 6–23 và phân quyền được giữ qua bộ hồi quy; không diễn giải test H2 là bằng chứng đã kiểm thử toàn bộ thiết bị và mọi dữ liệu thật.

Đọc KET_QUA_HIEU_NANG_TUAN24.md để xem số đo thật, chạy Lighthouse/axe và các giới hạn hiện tại. PHIEU_UAT_TUAN24.md dành cho chủ tiệm/người kiểm thử xác nhận trước khi triển khai thực tế.
