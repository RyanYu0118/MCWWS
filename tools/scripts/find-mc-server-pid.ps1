$ErrorActionPreference = 'Stop'
$p = Get-Process javaw -ErrorAction SilentlyContinue | Where-Object {
    $_.MainWindowTitle -eq 'Minecraft server'
} | Select-Object -First 1
if (-not $p) {
    Write-Output 'PID=0'
    exit 0
}
Write-Output "PID=$($p.Id)"
