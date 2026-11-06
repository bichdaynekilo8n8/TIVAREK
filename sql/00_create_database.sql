-- Chạy bằng Windows Authentication/tài khoản quản trị trong SSMS, không qua Flyway.
-- Giữ nguyên TIVAREK_DEV cũ. Database mới phải trống trước lần migrate đầu tiên.
USE master;
GO
IF DB_ID(N'TIVAREK_WEEK8_DEV') IS NULL
BEGIN
    CREATE DATABASE TIVAREK_WEEK8_DEV COLLATE Vietnamese_100_CI_AS;
END;
GO
SELECT name, collation_name FROM sys.databases WHERE name=N'TIVAREK_WEEK8_DEV';
GO
