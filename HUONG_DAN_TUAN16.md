# TIVAREK — Tuần 16: Bảo hành

## Chuẩn bị

1. Sao lưu database và thư mục tải ảnh trước khi cập nhật.
2. Giữ **Java 21**, Spring Boot 4.1.2, SQL Server và cấu hình tài khoản hiện tại.
3. Chạy `CHAY_TUAN16.cmd` trên Windows, nhập tài khoản/mật khẩu SQL Server khi được hỏi.
4. Flyway sẽ áp dụng **V10__week16_warranty_workflow.sql**; không chạy tay V1 hoặc `flyway clean`/`repair`.
5. Đăng xuất, đăng nhập lại để nhận các quyền Tuần 16.

## Các màn hình

| Địa chỉ | Chức năng |
| --- | --- |
| `/admin/warranties` | Nhân viên xem bảo hành/yêu cầu; OWNER cấu hình chính sách |
| `/admin/warranties/claims/{id}` | Xử lý yêu cầu và liên kết phiếu sửa |
| `/tra-cuu-bao-hanh` | Tra cứu công khai bằng mã phiếu **và** mã xác minh |
| `/account/warranties` | Khách đăng nhập xem, gửi yêu cầu trên phiếu của mình |

## Quy trình phải làm theo thứ tự

1. OWNER tạo chính sách có nội dung cam kết thực tế (chưa tự cài thời hạn mặc định cho mọi hàng).
2. OWNER gắn chính sách và số ngày cho SKU/thiết bị riêng lẻ/dịch vụ. Máy cũ có quy tắc riêng được ưu tiên hơn SKU chung. Số ngày bằng 0 = không cấp bảo hành có thời hạn.
3. Tạo **đơn mới** ở Tuần 15; dòng đơn chụp `WarrantyDaysSnapshot` khi tạo. Chỉ khi đơn được giao/thu đủ và sang `COMPLETED` mới cấp bảo hành tự động.
4. Với sửa chữa Tuần 14, dòng dịch vụ lấy số ngày từ `Services.DefaultWarrantyDays`, linh kiện lấy từ SKU. Chỉ dòng thuộc báo giá `ACCEPTED` và đã bàn giao `DELIVERED` được cấp bảo hành.
5. Nhân viên vào `/admin/warranties`, nhấn **Cấp mã** của phiếu để nhận một mã xác minh mới **chỉ hiện một lần**; bàn giao riêng cho khách. Mã cũ không còn hợp lệ.
6. Khách có tài khoản và đơn/phiếu sửa được liên kết có thể xem/gửi yêu cầu tại `/account/warranties` mà không cần token. Khách vãng lai dùng mã bảo hành + token tại `/tra-cuu-bao-hanh`.
7. Nhân viên chuyển `OPEN → INSPECTING`; OWNER được chấp thuận/từ chối. Nếu cần sửa, tiếp nhận phiếu **mới** tại `/admin/repairs`, sau đó liên kết ID phiếu sửa với yêu cầu `APPROVED`. Chỉ chuyển `RESOLVED` khi phiếu sửa liên kết đã `DELIVERED` (hoặc giải quyết theo hình thức khác được mô tả rõ).

## Giới hạn cần biết

- Đơn cũ Tuần 15 đã lưu `WarrantyDaysSnapshot=0` **không được cấp tự động hồi tố**. Muốn xử lý cam kết cũ cần xét duyệt theo chứng từ thực tế.
- Bảo hành BULK hiện một phiếu trên một dòng bán; các lần giao một dòng có chung mốc khi đơn `COMPLETED`. Trả một phần chỉ giảm số lượng đủ điều kiện; trả toàn bộ làm phiếu `VOID`.
- Không gửi SMS/Zalo/email tự động; nhân viên bàn giao mã xác minh. Mã chỉ được lưu dưới dạng SHA-256.
- Không tự động tạo phiếu sửa sau khi chấp thuận: nhân viên tiếp nhận máy thực tế qua nghiệp vụ Tuần 14 rồi liên kết. Không tạo máy giả để lấp khóa ngoại.
- Không tích hợp tự động đổi máy, trả tiền hoặc xử lý tranh chấp; các tình huống này phải được OWNER kiểm tra và ghi chú.

## Kiểm thử

```powershell
.\mvnw.cmd clean verify
```

Trong SSMS, chạy `sql/12_week16_verify.sql`. Ngoài ra phải kiểm thử trực tiếp với SQL Server: cấp phiếu từ đơn `COMPLETED`, phiếu sửa `DELIVERED`, chống trùng khi bấm hai lần, tra cứu đúng/sai token, khách khác không thể xem phiếu, CSRF bắt buộc, số lượng BULK sau khi trả hàng, và chuyển trạng thái hợp lệ.
