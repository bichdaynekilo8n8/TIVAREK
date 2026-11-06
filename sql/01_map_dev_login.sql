-- Tạo login tivarek_dev trong Security > Logins trước nếu chưa có.
-- Dùng mật khẩu riêng của bạn, không lưu mật khẩu vào script/Git.
-- Script này không tạo/reset mật khẩu login.
USE TIVAREK_WEEK8_DEV;
GO
IF SUSER_ID(N'tivarek_dev') IS NULL
    THROW 51000, N'Chưa có login tivarek_dev. Tạo login trong SSMS trước.', 1;
GO
IF DATABASE_PRINCIPAL_ID(N'tivarek_dev') IS NULL
    CREATE USER tivarek_dev FOR LOGIN tivarek_dev WITH DEFAULT_SCHEMA=dbo;
GO
ALTER USER tivarek_dev WITH DEFAULT_SCHEMA=dbo;
ALTER ROLE db_owner ADD MEMBER tivarek_dev;
GO
-- Chỉ dùng db_owner cho DB thực hành cục bộ này.
-- Vận hành thật: tài khoản migrate riêng, tài khoản ứng dụng có quyền tối thiểu.
SELECT DB_NAME() AS DatabaseName, USER_NAME() AS DbUser;
GO
