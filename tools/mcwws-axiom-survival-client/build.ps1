$ErrorActionPreference = "Stop"
$Root = $PSScriptRoot
$Lib = Join-Path $Root "lib"
$Extracted = Join-Path $Lib "extracted"
$Src = Join-Path $Root "src/main/java"
$Out = Join-Path $Root "build/classes"
$Res = Join-Path $Root "src/main/resources"
$RepoRoot = Split-Path -Parent (Split-Path $Root)
. (Join-Path $RepoRoot "tools/scripts/mcwws-jar-name.ps1")
$McwwsJar = Get-McwwsClientJarPaths -ProjectRoot $Root -ModName "MCWWS_AxiomSurvivalClient" -ResourcesDir $Res `
    -NeedName "MCWWS_AxiomSurvival" `
    -NeedVersionFromPluginYml (Join-Path $RepoRoot "tools/mcwws-axiom-survival/src/main/resources/plugin.yml")
$JarOut = $McwwsJar.JarOut

# 游戏目录含中文，脚本内不写字面量：PowerShell 5.1 按 ANSI 读取会破坏路径
$McRoot = $env:MCWWS_MC_ROOT
if ([string]::IsNullOrWhiteSpace($McRoot)) {
    $McRoot = Get-ChildItem "D:\Minecraft" -Directory |
        ForEach-Object { Join-Path $_.FullName ".minecraft" } |
        Where-Object { Test-Path (Join-Path $_ "mods") } |
        Sort-Object { $log = Join-Path $_ "logs\latest.log"; if (Test-Path $log) { (Get-Item $log).LastWriteTime } else { [DateTime]::MinValue } } -Descending |
        Select-Object -First 1
}
if ([string]::IsNullOrWhiteSpace($McRoot)) { throw "Cannot locate .minecraft game directory" }
$DeployDir = Join-Path $McRoot "mods"
$FastUtil = Join-Path $McRoot "libraries/it/unimi/dsi/fastutil/8.5.18/fastutil-8.5.18.jar"
$AxiomJar = Get-ChildItem -LiteralPath (Join-Path $McRoot "mods") -Filter "Axiom-*.jar" -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -notmatch '\.old$' } |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

$JavaHome = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { "C:\Program Files\Java\jdk-25.0.2" }
$Javac = Join-Path $JavaHome "bin/javac.exe"
$Jar = Join-Path $JavaHome "bin/jar.exe"

$Jspecify = Join-Path $Extracted "jspecify-1.0.0.jar"

$CpParts = @()
if (Test-Path $FastUtil) { $CpParts += $FastUtil }
if ($AxiomJar -ne $null) { $CpParts += $AxiomJar.FullName }
if (Test-Path $Jspecify) { $CpParts += $Jspecify }
$CpParts += (Join-Path $Lib "*")
if (Test-Path $Extracted) { $CpParts += (Join-Path $Extracted "*") }
$Cp = ($CpParts -join ';')

if (Test-Path $Out) { Remove-Item $Out -Recurse -Force }
New-Item -ItemType Directory -Path $Out -Force | Out-Null
New-Item -ItemType Directory -Path (Split-Path $JarOut) -Force | Out-Null

$JavaFiles = Get-ChildItem $Src -Recurse -Filter *.java | ForEach-Object { $_.FullName }
& $Javac -encoding UTF-8 -proc:none -cp $Cp -d $Out @JavaFiles
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Copy-Item -Recurse -Force (Join-Path $Res "*") $Out
Publish-McwwsClientJar -JarExe $Jar -ClassesDir $Out -JarPaths $McwwsJar -ModName "MCWWS_AxiomSurvivalClient" -ModsDir $DeployDir
