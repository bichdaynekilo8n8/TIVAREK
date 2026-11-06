<#
.SYNOPSIS
  Archive TIVAREK_UPLOAD_DIR images without changing the source directory.
#>
param([string]$BackupDirectory = 'D:\TIVAREK_BACKUPS')
$ErrorActionPreference = 'Stop'
$source = [Environment]::GetEnvironmentVariable('TIVAREK_UPLOAD_DIR')
if ([string]::IsNullOrWhiteSpace($source) -or -not [IO.Path]::IsPathRooted($source)) {
    throw 'TIVAREK_UPLOAD_DIR is missing or is not an absolute path.'
}
if (-not (Test-Path -LiteralPath $source -PathType Container)) { throw 'Image source directory not found.' }
if (-not [IO.Path]::IsPathRooted($BackupDirectory)) { throw 'BackupDirectory must be an absolute path.' }
$fullSource = [IO.Path]::GetFullPath($source).TrimEnd([char]'\', [char]'/')
$fullBackup = [IO.Path]::GetFullPath($BackupDirectory).TrimEnd([char]'\', [char]'/')
if ($fullBackup.StartsWith($fullSource + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
    throw 'Backup directory cannot be inside image directory.'
}
New-Item -ItemType Directory -Path $fullBackup -Force | Out-Null
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss-fff'
$dest = Join-Path $fullBackup "TIVAREK_IMAGES_$stamp.zip"
Compress-Archive -LiteralPath $fullSource -DestinationPath $dest -CompressionLevel Optimal
if (-not (Test-Path -LiteralPath $dest)) { throw 'Failed to create image backup.' }
Write-Host "Image backup created: $dest" -ForegroundColor Green
