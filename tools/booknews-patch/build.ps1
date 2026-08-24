$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$Here = $PSScriptRoot
$Src = Join-Path $Here "src"
$Out = Join-Path $Here "build/classes"
$OrigJar = Join-Path $Root "plugins/BookNews.jar"
$PatchedJar = Join-Path $Root "plugins/BookNews.jar.new"

$JavaHome = [System.Environment]::GetEnvironmentVariable("JAVA_HOME")
if ([string]::IsNullOrWhiteSpace($JavaHome)) {
    $JavaHome = "C:\Program Files\Java\jdk-25.0.2"
}
$JavacExe = Join-Path $JavaHome "bin/javac.exe"
$JarExe = Join-Path $JavaHome "bin/jar.exe"
if (-not (Test-Path $JavacExe)) { throw "Missing javac.exe: $JavacExe" }
if (-not (Test-Path $JarExe)) { throw "Missing jar.exe: $JarExe" }
if (-not (Test-Path $OrigJar)) { throw "Missing $OrigJar" }

if (Test-Path $Out) { Remove-Item $Out -Recurse -Force }
New-Item -ItemType Directory -Path $Out | Out-Null

$JavaFiles = Get-ChildItem $Src -Recurse -Filter *.java | ForEach-Object { $_.FullName }
& $JavacExe -encoding UTF-8 -source 8 -target 8 -d $Out $JavaFiles
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Copy-Item $OrigJar $PatchedJar -Force
Push-Location $Out
try {
    & $JarExe uf $PatchedJar com/gmail/legamemc/booknews/VersionUtils.class
    if ($LASTEXITCODE -ne 0) { throw "jar uf failed" }
} finally {
    Pop-Location
}

$PluginYml = Join-Path $Here "plugin.yml"
if (Test-Path $PluginYml) {
    Push-Location $Here
    try {
        & $JarExe uf $PatchedJar plugin.yml
        if ($LASTEXITCODE -ne 0) { throw "jar uf plugin.yml failed" }
    } finally {
        Pop-Location
    }
}

$Live = Join-Path $Root "plugins/BookNews.jar"
try {
    Copy-Item $PatchedJar $Live -Force
    Write-Host "Patched $Live"
} catch {
    Write-Host "Built $PatchedJar (live jar locked; replace after stop)"
}
