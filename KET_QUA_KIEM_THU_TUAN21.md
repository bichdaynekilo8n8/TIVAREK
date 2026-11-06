# TIVAREK — KIỂM THỬ TUẦN 21

- Đầu vào: file ZIP TIVAREK Tuần 20 do người dùng gửi.
- Đã thêm các class/thử nghiệm bảo mật thuộc Tuần 21 và các lệnh Windows tương ứng.
- Các bài kiểm thử JUnit mới: `LoginAttemptGuardTest`, `LoginThrottleFilterTest`, `Week21RouteSecurityTest`, `Week21PasswordChangeTest`.
- Tình trạng kiểm thử `clean verify`: **CHƯA CHẠY** — môi trường đóng gói chỉ có Java Runtime 17, không có JDK 21/Maven/SQL Server.
- Tình trạng UAT trên Windows + SQL Server DEV: **CHƯA CHẠY** — phải do máy triển khai thực hiện.
- Các kết quả PASS/FAIL sẽ được cập nhật từ `target/surefire-reports` và phiếu `PHIEU_UAT_TUAN21.md`.

Không coi Week 21 là đã nghiệm thu production cho tới khi các bước bắt buộc PASS.
