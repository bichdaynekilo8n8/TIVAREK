# KẾT QUẢ HIỆU NĂNG TUẦN 24

Ngày kiểm tra: 06/11/2026 (UTC+7). Nguồn đầu vào: TIVAREK(7).zip.

## Phạm vi và cách đo

Đã chạy ứng dụng Spring Boot bằng **JDK 21.0.13**, profile **test / H2**, trên loopback. Mỗi URL được đọc 3 lần bằng HTTP client với `Accept-Encoding: gzip`, cùng dữ liệu test mặc định. Đây là phép đo tải HTTP, **không phải Lighthouse, Core Web Vitals hoặc SQL Server DEV**. TTFB là khoảng từ lúc gửi request đến khi HTTP client nhận header, gồm kết nối loopback. Không đo thời gian render, paint, tương tác hay tổng request thực tế của trình duyệt.

Đã thử khởi động Chrome 142 và chạy Playwright/Lighthouse; Chrome bị môi trường chặn tạo socket (`socket() failed: Operation not permitted`). Vì vậy các chỉ số chưa đo được được để CHƯA ĐO, không thay bằng ước lượng hay điểm giả.

## Dung lượng ảnh thật trước/sau

| Tài nguyên (mỗi bản static/images hoặc docs/assets) | Trước (byte) | Sau (byte) | Giảm |
|---|---:|---:|---:|
| tivarek-wordmark.png | 611,529 | 59,184 | 90.3% |
| tivarek-icon.png | 553,542 | 4,857 | 99.1% |
| tivarek-brand-card.png | 3,285,726 | 1,140,143 | 65.3% |

Logo tối đa 570×203, favicon 64×64, danh thiếp tối đa 1200×600. Giữ nguyên định dạng PNG và URL cũ để tránh gãy trang/ảnh. Ảnh người dùng tải lên từ bản này giới hạn cạnh dài 1600px, không phóng ảnh nhỏ, giữ tỷ lệ và alpha PNG; ảnh đã lưu không bị sửa hay mất. Ảnh sản phẩm bên thứ ba qua HTTPS chưa được chuyển đổi tự động.

## HTTP thực đo — trung vị 3 lượt

| Trang | TTFB trước (ms) | TTFB sau (ms) | HTML truyền trước (byte) | HTML truyền sau (byte) | Sau |
|---|---:|---:|---:|---:|---|
| `/` | 19.74 | 76.20 | 12,442 | 4,010 | gzip |
| `/san-pham` | 14.20 | 61.14 | 6,561 | 2,677 | gzip |
| `/san-pham/phu-kien/1` | 38.71 | 20.53 | 4,513 | 2,190 | gzip |
| `/sua-chua` | 8.85 | 11.08 | 5,219 | 2,492 | gzip |

HTML sau gồm metadata và CSS bổ sung. Dung lượng truyền giảm nhờ gzip; PNG không nén HTTP lại. Một số TTFB sau cao hơn trước: **chưa thể kết luận tốc độ render hoặc TTFB đã cải thiện**. Mẫu 3 lượt trên JVM DEV, H2 và máy chia sẻ có độ nhiễu lớn; cần profile độc lập trên SQL Server và đo lại bằng trình duyệt trước nghiệm thu. Không dùng bảng này để cam kết LCP ≤2,5s.

`html_linked_resources` trong JSON chỉ đếm URL tài nguyên xuất hiện trực tiếp trong HTML, không phải tổng request của trình duyệt (không gồm import CSS, ảnh trong CSS, lazy load theo viewport). Không dùng số đó để so sánh tổng số request.

## Những thay đổi có thể kiểm chứng

- Bỏ `@import week23.css` ở 7 CSS, thêm link trực tiếp đúng thứ tự cascade; trình duyệt có thể phát hiện và tải các CSS song song.
- CSS/JS/ảnh classpath được gắn fingerprint nội dung. URL fingerprint đã được phục vụ 200 và kiểm tra lại với `If-Modified-Since` trả 304; cache tĩnh `max-age=3600, must-revalidate`.
- Ảnh danh mục và gallery có width/height; ảnh ngoài màn hình dùng lazy + async; ảnh bìa chi tiết dùng eager + fetchpriority high. Trang chủ dùng hero CSS, không tải ảnh hero khổng lồ.
- Chi tiết phụ kiện: từ 3 lần kiểm tra `repository.detail` xuống 1 lần; tổng lời gọi truy vấn từ 5 xuống 3 (detail + specs + photos). Có test kiểm tra số lần gọi và vẫn từ chối sản phẩm ẩn. Đây là xác minh luồng gọi, chưa phải benchmark SQL Server.
- Phân trang sắp xếp giá/tên bổ sung Kind làm khóa phân xử khi BULK và DEVICE trùng ID.
- Không cache tồn kho, giỏ hàng, kết quả tra cứu, token hoặc thông tin khách hàng.

## Bảng cần đo bằng trình duyệt

| Trang | Thiết bị | LCP | CLS | Performance | Tổng byte / request | INP |
|---|---|---|---|---|---|---|
| Trang chủ | Desktop + mobile, mỗi loại 3 lượt | CHƯA ĐO | CHƯA ĐO | CHƯA ĐO | CHƯA ĐO | Chưa có dữ liệu thực |
| Danh mục | Desktop + mobile, mỗi loại 3 lượt | CHƯA ĐO | CHƯA ĐO | CHƯA ĐO | CHƯA ĐO | Chưa có dữ liệu thực |
| Chi tiết sản phẩm thật | Desktop + mobile, mỗi loại 3 lượt | CHƯA ĐO | CHƯA ĐO | CHƯA ĐO | CHƯA ĐO | Chưa có dữ liệu thực |
| Đăng ký sửa | Desktop + mobile, mỗi loại 3 lượt | CHƯA ĐO | CHƯA ĐO | CHƯA ĐO | CHƯA ĐO | Chưa có dữ liệu thực |

Mục tiêu thực tế: LCP ≤2,5s, INP ≤200ms, CLS ≤0,1 tại phân vị 75 của dữ liệu người dùng thực. Lighthouse là lab diagnostic; TBT không phải INP.

## Đo lại trên Windows

1. Giữ một bản Tuần 23 và một bản Tuần 24 riêng. Dùng cùng SQL Server DEV, cùng sản phẩm/ảnh, cùng máy và cùng Chrome; tắt tác vụ nền không cần thiết.
2. Chạy ứng dụng cần đo rồi vào `scripts/week24-tools`, dùng Node.js 22 LTS hoặc mới hơn tương thích dependencies:

```powershell
npm ci
npx playwright install chromium
$env:BASE_URL='http://127.0.0.1:8080'
$env:DETAIL_PATH='/san-pham/phu-kien/1' # đổi thành URL sản phẩm đã công khai thực sự
$env:STAGE='before'                  # khi chạy bản Tuần 23
npm run lighthouse
$env:STAGE='after'                   # đổi sang ứng dụng Tuần 24, cùng dữ liệu
npm run lighthouse
npm run browser
```

`measure-lighthouse.mjs` chạy 4 trang × 2 thiết bị × 3 lượt = 24 lượt mỗi stage. Kết quả JSON và report từng lượt lưu trong `results/`. Phải giữ kết quả before trước khi đổi stage. `DETAIL_PATH` phải là đường dẫn sản phẩm thật đang công khai, không dùng ID ví dụ nếu trả 404. Desktop dùng cấu hình desktop riêng, mobile dùng mô phỏng mobile và throttling mặc định Lighthouse.

`npm run browser` đọc trang, kiểm tra JS runtime/console, overflow, focus skip link, axe A/AA tại 320/390/768/1440px. Các kiểm tra axe chưa thay thế đọc màn hình, zoom 200/400%, bàn phím, tương phản bằng mắt và thiết bị thật. Không nhập tài khoản hay gửi dữ liệu khách hàng trong các script này.

## Ưu tiên tiếp theo

| Mức | Việc | Trạng thái |
|---|---|---|
| P0 | Chạy build, phân quyền, đăng ký, không lộ cache/token | Có kiểm thử; xem báo cáo kiểm thử |
| P1 | Lighthouse trước/sau trên SQL Server DEV và sản phẩm thật | Chờ môi trường trình duyệt |
| P1 | Axe A/AA, bàn phím, mobile thật và lỗi mạng | Chờ UAT |
| P1 | Xác nhận tên miền backend và URL Pages thật | Chưa được cung cấp |
| P2 | Thumbnail/srcset cho ảnh cũ, WebP/AVIF, Product/Breadcrumb JSON-LD | Chưa triển khai; cân nhắc sau benchmark |

Nguồn chính thức: https://docs.spring.io/spring-boot/reference/web/servlet.html ; https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-config/static-resources.html ; https://web.dev/articles/vitals ; https://developers.google.com/search/docs/appearance/structured-data/local-business ; https://developers.google.com/search/docs/crawling-indexing/robots-meta-tag .
