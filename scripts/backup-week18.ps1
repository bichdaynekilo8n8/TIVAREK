<#
.SYNOPSIS
  Back up SQL Server using Windows Authentication without overwriting older backups.
#>
param(
    [string]$Server = 'localhost',
    [string]$Database = 'TIVAREK_PROD',
    [string]$BackupDirectory = 'D:\TIVAREK_BACKUPS'
)
$ErrorActionPreference = 'Stop'
if ($Database -notmatch '^[A-Za-z_][A-Za-z0-9_]*$') { throw 'Invalid database name.' }
if (-not [IO.Path]::IsPathRooted($BackupDirectory)) { throw 'BackupDirectory must be an absolute path.' }
if (-not (Get-Command sqlcmd -ErrorAction SilentlyContinue)) { throw 'sqlcmd is required, with BACKUP DATABASE permission.' }
New-Item -ItemType Directory -Path $BackupDirectory -Force | Out-Null
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss-fff'
$file = Join-Path $BackupDirectory "${Database}_${stamp}.bak"
if (Test-Path -LiteralPath $file) { throw 'Backup file already exists.' }
$sqlFile = $file.Replace("'", "''")
$backupSql = "BACKUP DATABASE [$Database] TO DISK = N'$sqlFile' WITH COPY_ONLY, CHECKSUM;"
& sqlcmd '-S' $Server '-E' '-b' '-Q' $backupSql
if ($LASTEXITCODE -ne 0) { throw 'BACKUP DATABASE failed.' }
$verifySql = "RESTORE VERIFYONLY FROM DISK = N'$sqlFile' WITH CHECKSUM;"
& sqlcmd '-S' $Server '-E' '-b' '-Q' $verifySql
if ($LASTEXITCODE -ne 0) { throw 'RESTORE VERIFYONLY failed.' }
Write-Host "SQL Server backup verified: $file" -ForegroundColor Green
Write-Host 'Also back up TIVAREK_UPLOAD_DIR and test restoring to a separate database.'
