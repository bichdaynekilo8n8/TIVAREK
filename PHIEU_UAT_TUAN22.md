# PHIEU UAT TUAN 22 - SO SANH DONG THOI (SQL SERVER DEV)

**NGUYEN TAC:** Chi test voi database DEV da sao luu. Dung 2 tai khoan nhan vien khac nhau; KHONG test tren database cua khach that.

| ID | Tinh huong | Buoc thu | Dieu kien PASS | Ket qua |
| --- | --- | --- | --- | --- |
| W22-01 | 2 nguoi dat may serial cuoi | Cung chon 1 DeviceID, xac nhan gan dong thoi | Chi 1 don giu duoc may | [ ] |
| W22-02 | 2 nguoi dat SKU BULK con 1 | Hai tab tao/xac nhan 1 SKU | Tong giu khong vuot 1 | [ ] |
| W22-03 | Het han giu may | Het han token giu, xac nhan don moi | Hold cu duoc giai phong, khong bao giu trung | [ ] |
| W22-04 | Xuat ban dong thoi | Hai nhan vien cung bam giao 1 dong hang | Khong vuot so luong dat, khong chung tu trung | [ ] |
| W22-05 | Thanh toan lap POST | Gui 2 lan cung IdempotencyKey + payload | Mot PaymentID duy nhat | [ ] |
| W22-06 | Tai su dung khoa khac payload | Giu key, doi so tien/ma ngan hang | Tu choi, khong thu them | [ ] |
| W22-07 | Hoan tien lap | Gui 2 lan cung IdempotencyKey | Mot refund, tong <= thu goc va hang duoc tra | [ ] |
| W22-08 | Sua phieu dong thoi | 2 ky thuat vien chuyen trang thai cung phieu | Khong bi ghi de hoac tao event sai | [ ] |
| W22-09 | Tao bao gia trung luc | 2 nhan vien tao QuoteNo moi tren 1 ticket | So QuoteNo khong trung | [ ] |
| W22-10 | Dong va phan hoi bao gia | 1 nhan vien ban giao, nguoi khac phan hoi | Khong phan hoi sau khi da dong phieu | [ ] |
| W22-11 | Linh kien sua/ban tranh ton | Sua va ban SKU con 1 cung luc | Khong xuat am kho | [ ] |
| W22-12 | 2 claim tren 1 chung nhan | Gui claim dong thoi vuot so luong con lai | Tong claim pending khong vuot eligible | [ ] |
| W22-13 | SQL rollback | Chay Week22SqlServerConcurrencyTest | Ghi tham do sau rollback = 0 | [ ] |
| W22-14 | Khoa SQL Server | Chay 2 thread theo test opt-in | Thread 2 cho transaction 1 nhả khoa | [ ] |
| W22-15 | Regression | Chay KIEM_THU_TUAN22.cmd | Maven verify PASS | [ ] |
| W22-16 | Database | Chay scripts/check-week22.sql tren SSMS | Khong co hang sai lech o cac muc canh bao | [ ] |

## Stress test thu cong co gioi han

- 2, 5, 10 luong, moi luong dung mau don hang DU LIEU THU (co key rieng).
- Ghi thoi gian p50/p95, deadlock SQL error 1205, HTTP 503, so giao dich thanh cong/thất bai.
- Khong tang load neu SQL Server dev dang cham; can thiet thi dung ngay.

## Ky nghiem thu

- Phien ban: __________________
- Database DEV: ______________
- JDK: _______________________
- Maven verify: ______________
- SqlServer integration test: ________
- So kich ban PASS / 16: ______
- Nguoi kiem thu: ____________
- Ngay: _____________________
- GO / NO-GO: _______________

Chi chuyen sang Tuần 23 hoac production sau khi xu ly cac FAIL P0/P1.
