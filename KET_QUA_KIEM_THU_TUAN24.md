# KẾT QUẢ KIỂM THỬ TUẦN 24

Ngày: 06/11/2026 (UTC+7). Đầu vào: TIVAREK(7).zip. Trạng thái: **build đạt; nghiệm thu vận hành còn chờ**, không đánh dấu 12/12.

## Kết quả thực tế

| Kiểm tra | Kết quả |
|---|---|
| Maven Wrapper / JDK 21.0.13 / clean verify | BUILD SUCCESS, exit 0 |
| JUnit toàn bộ | 193 test được khai báo; **148 đạt, 0 fail, 0 error, 45 skipped** |
| 45 skipped | Nhóm SQL Server security (13), sản phẩm (11), thiết bị (17), concurrency (4); cần môi trường SQL Server riêng |
| Test Tuần 24 | 9 test đạt: policy, catalog, ảnh, MVC/render/cache/SEO, truy vấn sitemap |
| Khởi động ứng dụng | Đã khởi động và đọc HTTP với profile test/H2; chưa khởi động SQL Server DEV |
| HTTP trước/sau | 14 URL × 3 lượt mỗi bản; dữ liệu đo lưu JSON, không lưu HTML/token |
| Fingerprint/cache tĩnh | URL fingerprint trả 200; revalidate trả 304; max-age=3600 và must-revalidate |
| Token/giỏ hàng/lookup/login | Header no-store/noindex qua Spring Security và MVC; POST sửa chữa thiếu CSRF bị 403 |
| Metadata/sitemap | Trang công khai render đúng, base URL không lấy Host header; truy vấn sitemap loại sản phẩm ẩn và máy khách hàng |
| Script Node | Parse syntax toàn bộ JS và hai runner audit đạt |
| Generator Pages | Chạy ở bản sao tạm: prefix /TIVAREK/, XML hợp lệ, chạy lặp không trùng canonical, từ chối HTTP |
| PowerShell/.cmd trên Windows | Chưa chạy runtime; được bàn giao để chạy trên máy người dùng |
| Lighthouse/Playwright/axe | Đã thử, bị chặn khởi động Chrome do socket; chưa có kết quả layout/a11y/Console/CWV |
| Checksum SQL | Tất cả SQL trong resources khớp bản gốc; không thay migration hay schema |

File bằng chứng: reports/week24/test-summary.json, maven-clean-verify.log, migration-checksums.json, before-http.json, after-http.json, after-http-resource-check.json, asset-metrics.json, script-checks.json.

## Lỗi baseline và cách xử lý

Baseline JDK 21 có 184 test, 6 fail, 12 error, 45 skipped, build thất bại.

| Vấn đề | Xử lý và bằng chứng |
|---|---|
| MVC test phân quyền thiếu LoginAttemptGuard làm 12 ca không nạp context | Bổ sung MockitoBean vào MVC slice; 12 ca phân quyền chạy đạt |
| 4 ca test nhóm repair/sales/security/warranty kỳ vọng redirect glob không khớp `/login` của Spring hiện tại | Assert chính xác `/login`, giữ kiểm tra 3xx và từ chối truy cập |
| Audit phân loại OTHER vì servletPath rỗng trong MockMvc | Dùng URI đã bỏ context path để phân loại; audit vẫn chỉ ghi nhóm route cố định, không ghi ID/token |
| Ca thiếu role CUSTOMER còn nhìn thấy Users insert trong transaction bao ngoài của test trước khi rollback | Precheck role trước insert và vẫn giữ guard số row role cùng transaction; ca test đạt. Không suy diễn baseline đã tạo tài khoản mồ côi vĩnh viễn |
| Kiểm tra logo cố định URL cũ không tương thích fingerprint | Assert tên logo + hash nội dung 32 ký tự và vẫn kiểm tra MIME/GET ảnh |

Các ca kiểm thử lỗi nghiệp vụ, CSRF, hạn chế quyền, giỏ hàng và giao dịch cũ không bị bỏ qua để làm xanh build. H2 không chứng minh đặc tính lock/constraint/migration của SQL Server.

## 12 điều kiện nghiệm thu

| # | Điều kiện | Trạng thái hiện tại |
|---|---|---|
| 1 | Maven clean verify JDK 21 | ĐẠT |
| 2 | Khởi động SQL Server DEV | CHỜ — mới chạy test/H2 |
| 3 | Home/sản phẩm/giỏ hàng/sửa chữa hoạt động | ĐẠT tự động/H2 trong phạm vi test; CHỜ UAT SQL Server |
| 4 | Không lỗi JS nghiêm trọng trong Console | CHỜ — syntax đạt, runtime browser bị chặn |
| 5 | Không cuộn ngang trên mobile | CHỜ kiểm tra 320/390/768/1440px bằng browser và máy thật |
| 6 | Không lỗi A/AA tồn đọng trong phạm vi | CHỜ axe + bàn phím/screen reader thủ công |
| 7 | Đo trước/sau và cải thiện/giải thích chưa đạt | Có số đo HTTP và byte ảnh; CHỜ Lighthouse/CWV trên dữ liệu thật |
| 8 | Không cache công khai dữ liệu cá nhân/token | ĐẠT các kiểm tra header và luồng trong bộ test; CHỜ xác minh cấu hình proxy nếu triển khai |
| 9 | SEO chỉ trang công khai hợp lệ | ĐẠT test; CHỜ khai báo domain thật và kiểm chứng crawler |
| 10 | Không phá chức năng/phân quyền 6–23 | ĐẠT bộ hồi quy có sẵn trên H2; CHỜ SQL Server và UAT |
| 11 | Pages giữ vai trò giới thiệu tĩnh | ĐẠT kiểm tra source/generator; CHỜ URL Pages thật sau publish |
| 12 | UAT và xác nhận triển khai | CHỜ người kiểm thử/chủ tiệm ký |

Không có xác nhận thay chủ tiệm, không publish và không kết luận Tuần 24 đã đạt 12/12. Làm tiếp các mục CHỜ theo PHIEU_UAT_TUAN24.md và KET_QUA_HIEU_NANG_TUAN24.md trước khi dùng thực tế.
