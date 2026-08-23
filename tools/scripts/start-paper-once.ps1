$ErrorActionPreference = 'Stop'
# Resolve server root from this script: tools/scripts -> repo root
$root = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
Set-Location -LiteralPath $root
Write-Output "ROOT=$root"

$existing = Get-Process javaw -ErrorAction SilentlyContinue | Where-Object {
    $_.MainWindowTitle -eq 'Minecraft server'
}
if ($existing) {
    Write-Output "ALREADY_RUNNING PID=$($existing.Id)"
    exit 0
}

Start-Process -FilePath 'javaw' -ArgumentList @(
    '-Xms1G',
    '-Xmx16G',
    '-Dorg.bukkit.plugin.java.LibraryLoader.centralURL=https://maven.aliyun.com/repository/central',
    '-jar',
    'paper-26.2-112.jar'
) -WorkingDirectory $root

Start-Sleep -Seconds 5
$proc = Get-Process javaw -ErrorAction SilentlyContinue | Where-Object {
    $_.MainWindowTitle -eq 'Minecraft server' -or $_.WorkingSet64 -gt 200MB
} | Select-Object -First 1

if ($proc) {
    Write-Output "STARTED PID=$($proc.Id) TITLE=$($proc.MainWindowTitle) MEMMB=$([int]($proc.WorkingSet64/1MB))"
    exit 0
}

Write-Output 'START_UNCONFIRMED'
Get-Process javaw -ErrorAction SilentlyContinue | ForEach-Object {
    Write-Output "javaw PID=$($_.Id) TITLE='$($_.MainWindowTitle)' MEMMB=$([int]($_.WorkingSet64/1MB))"
}
exit 1
