# TUAN 22 - GIAO DICH DONG THOI VA TOAN VEN DU LIEU

## 1. Pham vi va ket qua ban va

TIVAREK: Java 21, Spring Boot 4.1.2, Thymeleaf, SQL Server + Flyway.

Tiep noi bao mat tuan 21, ban va nay tap trung vao tinh toan ven khi hai hoac nhieu nhan vien thao tac cung mot luc:

- `StockLocks`: cung mot ProductID / DeviceID dung chung update lock `UPDLOCK,HOLDLOCK` trong giao dich. Khoa theo thu tu tang dan, products truoc devices.
- `InventoryService`: khoa nguon ton truoc khi giu hang, ghi so, dao chung tu va xuat linh kien. Don expired hold theo DeviceID de han che quet bang toan cuc.
- `SalesService`: xac nhan, lam moi giu hang va huy don deu khoa nguon hang truoc khi tinh ton/doi trang thai giu.
- `SaleWorkflowService`: nhan tra hang khoa nguon ton truoc khi ghi nhan chung tu hang ve.
- `RepairService`: khoan khoa phieu truoc khi phan cong, chuyen trang thai, tao/bo sung bao gia, lam viec, su dung linh kien va ban giao. Khong cho gui/phan hoi bao gia sau khi dong phieu.
- `WarrantyService`: khoa pham vi ma chinh sach khi upsert va khoa ban ghi can cau hinh / cap lai token.
- `TransactionContentionAdvice`: khong bao thanh cong gia khi SQL bi tran chanh/deadlock; tra HTTP 503, `Retry-After: 2`. KHONG tu dong gui lai POST thanh toan co tac dong ben ngoai.
- Flyway `V12__week22_transaction_integrity.sql`: ranh buoc khoa idempotency khong rong, chi muc ho tro giao dich.
- Test: unit test khoa va tranh chap; opt-in integration test SQL Server DEV cho khoa dong thoi, rollback, ranh buoc.

### Khong thay doi

- Giu nguyen tat ca migration V1-V11, quyen cua cac role, endpoint, giao dien, luong don hang cu.
- Cac khoa duy nhat `Payments.IdempotencyKey`, `SalesInventoryRequests.RequestKey`, `ActiveDeviceReservations.DeviceID` da co tu truoc duoc giu nguyen.
- Khong tat isolation hoac bat RCSI SQL Server mot cach dai tra.

## 2. Chay tren Windows

1. Giai nen ZIP vao thu muc du an rieng hoac sao luu project cu truoc khi cap nhat.
2. Kiem tra JDK 21: `java -version` va `where java`.
3. Sao luu database `TIVAREK_WEEK8_DEV` truoc khi Flyway V12 chay.
4. Kiem tra profile `dev` va bien `TIVAREK_DB_URL`, `TIVAREK_DB_USER`, `TIVAREK_DB_PASSWORD`. Khong dua mat khau vao Git.
5. Chay `KIEM_THU_TUAN22.cmd` (test H2 va dong goi).
6. Chay `CHAY_TUAN22.cmd` de Flyway migrate tren SQL Server DEV, doc log `Successfully applied 1 migration` neu V12 chua chay.
7. Chay `KIEM_THU_TUAN22_SQLSERVER.cmd` tren database DEV da sao luu.
8. Mo SSMS, chay `scripts/check-week22.sql` o che do read-only (ket qua cac muc canh bao phai rong).
9. Thuc hien UAT theo `PHIEU_UAT_TUAN22.md` truoc khi chuyen production.

## 3. Lenh PowerShell nhanh

```powershell
Set-Location D:\Projects\TIVAREK
java -version
.\mvnw.cmd -B -ntp -Dspring.profiles.active=test clean verify
.\CHAY_TUAN22.cmd
```

Chi khi database DEV duoc sao luu va ten database dung `TIVAREK_WEEK8_DEV`:

```powershell
.\KIEM_THU_TUAN22_SQLSERVER.cmd
```

## 4. Ngay 1-7, nhan ban giao

1. Kiem toan: `TUAN22_DATABASE_AUDIT.md`.
2. Kho: `StockLocks`, `InventoryService`, `Week22StockLocksTest`.
3. Don va thanh toan: lock co dinh tren mot SKU, giu nguyen unique idempotency; regression tests tuan 20.
4. Sua va bao hanh: guard ticket, quote, warranty; `Week22RepairConcurrencyTest`.
5. Migration SQL: V12, check-week22.sql.
6. Deadlock/rollback: HTTP 503, test rollback that tren DEV; khong tu dong retry cac thao tac khong idempotent.
7. Nghiem thu: `KET_QUA_KIEM_THU_TUAN22.md`, `PHIEU_UAT_TUAN22.md`.

## 5. Cac gioi han va quyet dinh an toan

- `@Transactional` noi bo khong tu nang isolation khi tham gia transaction ngoai da mo. Vi vay luong tieu hao linh kien ngoai `RepairService` duoc nang len SERIALIZABLE, phu hop voi `InventoryService`.
- Cac thao tac POST co the co tac dong ngoai DB khong duoc auto-retry. Khi gap HTTP 503, kiem tra trang thai don/phiếu truoc khi thu lai va dung lai khoa idempotency cu.
- Khong cam ket runtime SQL Server PASS tren he thong khong co SQL Server. Muc dich ban va la cung cap ma nguon va bo kiem thu de ban xac minh bang may Windows.
- Neu co data cu vi pham CHECK V12, migration se dung thay vi bo qua loi. Can sua du lieu sai bang quy trinh nghiep vu va kiem tra lai.

Xem `KET_QUA_KIEM_THU_TUAN22.md` de phan biet viec da kiem tra va chua kiem tra.
