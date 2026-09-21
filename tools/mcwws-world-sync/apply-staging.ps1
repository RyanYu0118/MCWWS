# Apply MCWWS_WorldSync staging onto the server tree BEFORE starting Paper.
# Usage (server stopped):
#   powershell -NoProfile -File tools/mcwws-world-sync/apply-staging.ps1 -ServerRoot "D:\Minecraft\服务器\26.2"
param(
    [Parameter(Mandatory = $true)][string]$ServerRoot
)
$ErrorActionPreference = "Stop"
$staging = Join-Path $ServerRoot "plugins\MCWWS_WorldSync\staging"
$flag = Join-Path $ServerRoot "plugins\MCWWS_WorldSync\apply-on-boot"
if (-not (Test-Path -LiteralPath $staging)) {
    Write-Host "No staging directory."
    exit 0
}
Get-ChildItem -LiteralPath $staging -Recurse -File | ForEach-Object {
    $rel = $_.FullName.Substring((Resolve-Path $staging).Path.Length).TrimStart('\', '/')
    if ($rel.EndsWith(".part")) { return }
    $dest = Join-Path $ServerRoot $rel
    $dir = Split-Path $dest -Parent
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir | Out-Null }
    Copy-Item -LiteralPath $_.FullName -Destination $dest -Force
    Write-Host "applied $rel"
}
if (Test-Path -LiteralPath $flag) { Remove-Item -LiteralPath $flag -Force }
Write-Host "staging applied."
