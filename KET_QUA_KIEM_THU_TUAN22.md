# TIVAREK - KET QUA KIEM THU TUAN 22

## Da kiem tra tren workspace lap trinh

- [x] ZIP dau vao `TIVAREK(2).zip` khong co loi cau truc (`unzip -t` PASS).
- [x] Lay dung code du an co migration Flyway V1-V11 va source Java tuan 21.
- [x] Doc luong transaction kho, ban hang, sua chua, bao hanh; xac dinh cac khoa SQL da ton tai.
- [x] Giu lai cac ranh buoc UNIQUE co san cho Payment, DeviceReservation va SalesInventoryRequest.
- [x] Bo sung source va unit/integration test tuan 22 de kiem tra tren JDK 21.

## Chua the xac nhan trong moi truong nay

- [ ] `mvnw.cmd clean verify`: chua chay - moi truong khong co JDK 21, javac, Maven.
- [ ] Spring Boot server start + Flyway V12: chua chay - khong co SQL Server.
- [ ] SQL Server multi-thread 2/5/10 tai khoan: chua chay - can Windows + SQL Server DEV.
- [ ] UAT trinh duyet voi tat ca role: chua chay.

## Cach kiem tra de ket luan PASS

1. Chay `KIEM_THU_TUAN22.cmd` voi JDK 21, luu full Maven log.
2. Sao luu SQL Server DEV, khoi dong `CHAY_TUAN22.cmd` de Flyway V12 chay.
3. Chay `KIEM_THU_TUAN22_SQLSERVER.cmd`; duoc PASS voi database ten `TIVAREK_WEEK8_DEV`.
4. Chay `scripts/check-week22.sql` tren SSMS, doi chieu ket qua canh bao.
5. Hoan thanh `PHIEU_UAT_TUAN22.md`.

**Trang thai:** DA SUA SOURCE / CHUA NGHIEM THU RUNTIME SQL SERVER. Khong ket luan "khong loi" truoc khi test thuc te.
