# CHECKLIST VẬN HÀNH TIVAREK — TUẦN 19

> In bảng này và điền kết quả từng ca. Chỉ thử giao dịch giả trên DB DEV / bản sao DB. Không để tài khoản, mật khẩu, mã tra cứu hay thông tin khách thật trong ảnh chụp và Git.

## A. Trước khi mở cửa (mỗi ngày)

| Mục | Kiểm tra | Kết quả / Giờ / Người |
| --- | --- | --- |
| A01 | SQL Server và database đúng môi trường (DEV/PROD) đang online | [ ] |
| A02 | `java -version` hiển thị JDK 21; ứng dụng khởi động không có stack trace | [ ] |
| A03 | `/actuator/health` báo `UP`; `/`, `/san-pham`, `/sua-chua` truy cập được | [ ] |
| A04 | Không thấy cảnh báo storage đầy; thư mục ảnh `TIVAREK_UPLOAD_DIR` ghi/đọc được | [ ] |
| A05 | Đăng nhập theo từng vai trò, chỉ thấy đúng menu và quyền | [ ] |
| A06 | Hôm qua đã có bản SQL `.bak` và ZIP hình ảnh tách khỏi server app | [ ] |
| A07 | Kiểm tra đơn, phiếu sửa, yêu cầu bảo hành đang chờ; bàn giao người xử lý | [ ] |

## B. Trong ngày

| Mục | Kiểm tra | Kết quả |
| --- | --- | --- |
| B01 | Hàng máy cũ có `AssetTag` riêng; không nhận hai lần giữ bán cùng 1 máy | [ ] |
| B02 | Không xác nhận đơn khi thiếu tồn; xuất kho đúng trạng thái giao | [ ] |
| B03 | Đối soát thanh toán bằng mã giao dịch, không ghi nhận trùng | [ ] |
| B04 | Phiếu sửa có xác nhận báo giá và biên bản bàn giao | [ ] |
| B05 | Bảo hành có thời hạn, liên kết đơn/phiếu sửa đúng | [ ] |
| B06 | Không gửi mã tra cứu kèm token vào kênh công khai | [ ] |

## C. Cuối ngày

- [ ] Đối chiếu tiền và số đơn đã thanh toán; kiểm tra đơn bị hủy / trả hàng / hoàn tiền.
- [ ] Xem tồn kho, các máy đang giữ, phiếu sửa và claim bảo hành mở.
- [ ] Chạy báo cáo chủ cửa hàng, kiểm tra lỗi trong log (ẩn dữ liệu nhạy cảm khi chia sẻ).
- [ ] Sao lưu SQL Server và thư mục ảnh ra **hai bản riêng** ở vị trí an toàn có phân quyền.
- [ ] Ghi người trực, giờ kiểm tra, sự cố, biện pháp và yêu cầu bàn giao hôm sau.

## D. Kiểm tra sao lưu và phương án phục hồi

### Tạo bản sao lưu (trên máy có `sqlcmd` và quyền BACKUP)

```powershell
# Chạy lệnh với tài khoản có quyền đúng và đường dẫn mà dịch vụ SQL Server truy cập được.
.\scripts\backup-week18.ps1 -Server 'localhost' -Database 'TIVAREK_PROD' -BackupDirectory 'D:\TIVAREK_BACKUPS'

# Chỉ chạy sau khi $env:TIVAREK_UPLOAD_DIR đã được cấu hình đúng.
.\scripts\backup-images-week18.ps1 -BackupDirectory 'D:\TIVAREK_BACKUPS'
```

`backup-week18.ps1` đã chạy `RESTORE VERIFYONLY WITH CHECKSUM`. **VERIFYONLY không tương đương khôi phục thành công**. Cần thử khôi phục độc lập.

### Diễn tập khôi phục — chỉ DB mới, không ghi đè

1. Chọn máy/SQL Server dùng để thử. Xác nhận tên database đích **`TIVAREK_W19_RESTORE_TEST` chưa tồn tại**. Nếu đã tồn tại, dừng và chọn tên khác; tuyệt đối không xóa/ghi đè DB đang có.
2. Trong SSMS, xem tên logical file bằng `RESTORE FILELISTONLY FROM DISK = N'<duong-dan-file.bak>';`.
3. Thực hiện `RESTORE DATABASE [TIVAREK_W19_RESTORE_TEST] FROM DISK = N'<duong-dan-file.bak>' WITH MOVE N'<logical-data>' TO N'<new-data-path>', MOVE N'<logical-log>' TO N'<new-log-path>', CHECKSUM, RECOVERY;` **chỉ sau khi** xác nhận các đường dẫn đích hoàn toàn mới. Không dùng `WITH REPLACE`.
4. Đăng nhập test vào DB thử bằng tài khoản chỉ đọc, kiểm tra bảng `SalesOrders`, `RepairTickets`, `Warranties`, `flyway_schema_history` và chạy `scripts/check-week19.sql`.
5. Giải nén ZIP hình ảnh vào một thư mục **mới**, mở ngẫu nhiên ảnh để xác nhận không hỏng. Không giải nén chồng lên thư mục ảnh thật.
6. Ghi lại RTO (thời gian khôi phục), kích thước DB/ảnh và ảnh chụp bằng chứng không chứa PII/mật khẩu. Việc xóa các tài nguyên diễn tập chỉ thực hiện sau khi có xác nhận người quản trị.

### Khi gặp lỗi vận hành

1. Dừng thao tác mới trong khu vực bị ảnh hưởng; giữ nguyên log, thời gian và mã đơn/phiếu thử (che token khi báo lỗi).
2. Kiểm tra `java -version`, profile `dev`/`prod`, `/actuator/health`, kết nối SQL và `TIVAREK_UPLOAD_DIR`.
3. Chạy lại `KIEM_THU_TUAN19.cmd` trên mã nguồn và `KIEM_THU_WEB_TUAN19.cmd` khi server sẵn sàng.
4. Nếu sai số liệu tồn/tiền, **không trực tiếp UPDATE/DELETE SQL để chữa cháy**; dừng giao dịch, chụp bằng chứng, đối soát nghiệp vụ trước.
5. Khôi phục chỉ khi chủ cửa hàng phê duyệt, có bản sao hợp lệ và đã xác định chính xác database đích.

## E. Điều kiện GO / NO-GO để bán/sửa thật

**GO**: test JUnit PASS, smoke PASS, 0 cảnh báo dữ liệu nghiêm trọng, vai trò/CSRF PASS, hai luồng bán/sửa/bảo hành PASS, hình ảnh có bản sao, diễn tập restore trên DB mới PASS, giao diện điện thoại khả dụng, người nhà ký xác nhận.

**NO-GO** nếu một trong các điều kiện trên chưa đạt, hoặc có khả năng lộ dữ liệu khách, ghi trùng thanh toán, bán âm kho, mất ảnh hoặc không phục hồi được database.
