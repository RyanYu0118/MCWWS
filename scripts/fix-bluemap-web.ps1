param(
  [string]$RepoRoot = (Resolve-Path ".").Path
)

$ErrorActionPreference = "Stop"

function Invoke-GitRestore([string[]]$Paths) {
  $git = Get-Command git -ErrorAction SilentlyContinue
  if (-not $git) {
    throw "git not found in PATH"
  }

  Push-Location $RepoRoot
  try {
    # Restore from current HEAD. This is the most stable source after BlueMap overwrites web assets.
    & git restore --source=HEAD --worktree --staged -- $Paths 2>$null
    if ($LASTEXITCODE -ne 0) {
      # Older git versions may not like --staged here; retry without it.
      & git restore --source=HEAD --worktree -- $Paths
    }
  } finally {
    Pop-Location
  }
}

function Test-SettingsLooksPatched([string]$SettingsPath) {
  if (-not (Test-Path $SettingsPath)) { return $false }
  $raw = Get-Content -LiteralPath $SettingsPath -Raw -ErrorAction SilentlyContinue
  if (-not $raw) { return $false }
  return ($raw -match "mcwws-gis\.js\?v") -and ($raw -match "mcwws-shops\.js\?v")
}

$pathsToRestore = @(
  "bluemap/web/settings.json",
  "bluemap/web/js/mcwws-gis.js",
  "bluemap/web/js/mcwws-shops.js",
  "bluemap/web/css/mcwws-gis.css",
  "bluemap/web/css/mcwws-shops.css"
)

$settings = Join-Path $RepoRoot "bluemap/web/settings.json"

if (Test-SettingsLooksPatched $settings) {
  Write-Host "[fix-bluemap-web] settings.json looks OK; still restoring key assets to be safe..."
} else {
  Write-Host "[fix-bluemap-web] settings.json looks reset; restoring patched web assets..."
}

Invoke-GitRestore $pathsToRestore

Write-Host "[fix-bluemap-web] done."

