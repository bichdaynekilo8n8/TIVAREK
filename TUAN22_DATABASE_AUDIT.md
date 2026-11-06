# TIVAREK - TUAN 22: KIEM TOAN GIAO DICH SQL SERVER

## Thuc trang ma nguon (ket qua doc source; chua xac nhan database that)

| Pham vi | Truoc tuan 22 | Thay doi tuan 22 |
| --- | --- | --- |
| Kho BULK / serialized | SERIALIZABLE cho nhieu thao tac; kiem tra so du / giu | Khoa chung theo ProductID hoac DeviceID truoc khi tinh ton |
| Xac nhan don / lam moi giu hang | Khoa SalesOrders, nhung kha nang tranh chap khac don tren cung SKU | Product / Device update locks, thu tu khoa co dinh |
| Ghi so / dao chung tu | Co check trang thai DRAFT -> POSTED, kiem tra ton | Khoa cac dinh danh hang truoc kiem tra va post |
| Tra hang | Khoa don, co idempotency request | Khoa nguon ton theo ProductID / DeviceID |
| Tieu hao linh kien sua | Co SERIALIZABLE o InventoryService | Khoa nguon ton; ke thua SERIALIZABLE tu method consume/consumeDevice ben ngoai |
| Phieu sua / bao gia | Mot so thao tac chi doc trang thai roi ghi | Khoa RepairTickets/RepairQuotes theo thu tu ticket -> quote |
| Han che request sua | COUNT trong 10 phut, co the tranh chap | SERIALIZABLE + khoa pham vi theo ContactPhone, index ho tro |
| Phieu bao hanh | Da khoa khi tao claim va doi trang thai | Khoa pham vi PolicyCode khi upsert, khoa ban ghi khi cau hinh |
| Thanh toan | Payments.IdempotencyKey UNIQUE; code so sanh gia tri khi replay | Them CHECK chong khoa rong; giu nguyen logic bat trung |
| SQL deadlock/timeout | Chua co phan hoi chuyen biet cho moi endpoint | HTTP 503 + Retry-After; khong tu dong lap POST khong an toan |

## Bat bien nghiep vu can kiem tra tren SQL Server DEV

1. Ton BULK vat ly va ton kha dung khong am.
2. Mot DeviceID chi co mot gio giu dang hoat dong.
3. Mot ma IdempotencyKey chi ghi mot thanh toan va mot RequestKey chi ghi mot chung tu giao/tra.
4. Hoan tien khong vuot so thu goc va so duoc phep hoan theo hang tra.
5. Khong tieu hao linh kien vuot so luong bao gia duyet.
6. Khong tao hai phieu tiep nhan cho mot RepairRequestID.
7. Khong thay doi/phat sinh su kien bao gia sau khi phieu sua DELIVERED/CANCELLED.
8. SQL rollback khong de lai chung tu/line/thanh toan mo coi.

`scripts/check-week22.sql` chi doc, bao cao cac truong hop nghi van. Ket qua rong o cac truy van bat thuong la mong doi.

## Quy tac thay doi database

- Chua sua migration V1-V11. Migration moi: `V12__week22_transaction_integrity.sql`.
- Trien khai tren database DEV duoc sao luu truoc; KHONG chay stress test tren production.
- Cac constraint moi se tu choi du lieu khong hop le. Neu co du lieu cu vi pham, phai sua data bang nghiep vu truoc khi migrate.
- Khong bat RCSI toan CSDL, khong tu dong retry request thanh toan va khong bo UNIQUE constraint hien co.

## Luu y ve do dam bao

Vi khong co JDK 21 / Maven / SQL Server tren moi truong tao ban va, ket qua nay la source review va test code; khong phai chung nhan runtime SQL Server PASS. Phai hoan tat `KIEM_THU_TUAN22.cmd` va `KIEM_THU_TUAN22_SQLSERVER.cmd` tren may Windows truoc khi dung that.
