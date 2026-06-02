param(
  [string]$RepoRoot = (Resolve-Path ".").Path,
  [int]$IntervalSeconds = 5
)

$ErrorActionPreference = "Stop"

$fixScript = Join-Path $RepoRoot "scripts/fix-bluemap-web.ps1"
$settings = Join-Path $RepoRoot "bluemap/web/settings.json"

if (-not (Test-Path $fixScript)) {
  throw "Missing fix script: $fixScript"
}

Write-Host "[watch-bluemap-web] watching: $settings"
Write-Host "[watch-bluemap-web] interval: $IntervalSeconds s"

function SettingsNeedsFix {
  if (-not (Test-Path $settings)) { return $true }
  $raw = Get-Content -LiteralPath $settings -Raw -ErrorAction SilentlyContinue
  if (-not $raw) { return $true }
  # If BlueMap overwrote settings.json, these scripts/styles usually disappear or revert to old versions.
  return ($raw -notmatch "mcwws-gis\.js\?v") -or ($raw -notmatch "mcwws-shops\.js\?v")
}

while ($true) {
  try {
    if (SettingsNeedsFix) {
      Write-Host "[watch-bluemap-web] detected reset; applying patch..."
      powershell -NoProfile -ExecutionPolicy Bypass -File $fixScript | Out-Host
    }
  } catch {
    Write-Warning "[watch-bluemap-web] error: $($_.Exception.Message)"
  }
  Start-Sleep -Seconds $IntervalSeconds
}

