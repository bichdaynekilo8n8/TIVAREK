-- TIVAREK / SQL Server 18456: CHAN DOAN CHI DOC, KHONG SUA DB.
-- Chay trong SSMS bang Windows Authentication voi tai khoan co quyen xem logins.
-- Chay tren CUNG SQL Server instance ma JDBC 127.0.0.1:1433 dang ket noi.
USE master;
GO
SELECT @@SERVERNAME AS ServerName,
       SERVERPROPERTY(N'InstanceName') AS InstanceName,
       SERVERPROPERTY(N'IsIntegratedSecurityOnly') AS WindowsAuthenticationOnly;
-- WindowsAuthenticationOnly = 1: SQL login bi tu choi (phai bat Mixed Mode).
-- WindowsAuthenticationOnly = 0: SQL Authentication da duoc bat.
GO
SELECT name, is_disabled, default_database_name
FROM sys.sql_logins
WHERE name = N'tivarek_dev';
-- Khong co dong nao: SQL login tivarek_dev chua ton tai o instance nay.
-- is_disabled = 1: login dang bi vo hieu hoa.
GO
SELECT name, state_desc
FROM sys.databases
WHERE name = N'TIVAREK_WEEK8_DEV';
-- Khong co dong nao: chua co DB TIVAREK_WEEK8_DEV tren instance nay.
GO
IF DB_ID(N'TIVAREK_WEEK8_DEV') IS NOT NULL
BEGIN
    EXEC(N'USE [TIVAREK_WEEK8_DEV];
          SELECT name, type_desc, authentication_type_desc
          FROM sys.database_principals
          WHERE name = N''tivarek_dev'';');
END;
GO
-- Khong thay user trong DB? Dang nhap SSMS bang admin, xem sql/01_map_dev_login.sql.
-- Dung login/password THUC te de kiem tra trong SSMS voi SQL Server Authentication.
-- KHONG dan mat khau vao chat, log, Git hoac file SQL.
