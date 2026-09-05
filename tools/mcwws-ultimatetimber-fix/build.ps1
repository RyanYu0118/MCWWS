$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$Src = Join-Path $PSScriptRoot "src/main/java"
$Res = Join-Path $PSScriptRoot "src/main/resources"
$Out = Join-Path $PSScriptRoot "build/classes"
. (Join-Path $Root "tools/scripts/mcwws-jar-name.ps1")
$McwwsJar = Get-McwwsPluginJarPaths -RepoRoot $Root -PluginName "MCWWS_UltimateTimberFix" -ResourcesDir $Res
$JarOut = $McwwsJar.JarOut
$JarOutNew = $McwwsJar.JarOutNew

function Find-Newest {
    param([string]$RelativeDir, [string]$Filter)
    $dir = Join-Path $Root $RelativeDir
    if (-not (Test-Path $dir)) { return $null }
    $hit = Get-ChildItem $dir -Recurse -Filter $Filter | Sort-Object Name -Descending | Select-Object -First 1
    if ($hit) { return $hit.FullName }
    return $null
}

$PaperApi = Find-Newest "libraries/io/papermc/paper/paper-api" "paper-api-*.jar"
if (-not $PaperApi) {
    $PaperApi = Join-Path $Root "libraries/io/papermc/paper/paper-api/26.2.build.103-stable/paper-api-26.2.build.103-stable.jar"
}
$UltimateTimber = Get-ChildItem (Join-Path $Root "plugins") -Filter "UltimateTimber*.jar" |
    Where-Object { $_.Name -notlike "*.new" } |
    Sort-Object Name -Descending |
    Select-Object -First 1 -ExpandProperty FullName
$Slimefun = Get-ChildItem (Join-Path $Root "plugins") -Filter "Slimefun-*.jar" |
    Where-Object { $_.Name -notlike "*.new" } |
    Select-Object -First 1 -ExpandProperty FullName
$ExoticGarden = Get-ChildItem (Join-Path $Root "plugins") -Filter "ExoticGarden*.jar" |
    Where-Object { $_.Name -notlike "*.new" } |
    Sort-Object Name -Descending |
    Select-Object -First 1 -ExpandProperty FullName
if (-not $UltimateTimber) { throw "Missing UltimateTimber jar in plugins/" }
if (-not $Slimefun) { throw "Missing Slimefun jar in plugins/" }
if (-not $ExoticGarden) { throw "Missing ExoticGarden jar in plugins/" }

$Libs = @(
    (Find-Newest "libraries/com/google/guava/guava" "guava-*.jar"),
    (Find-Newest "libraries/net/kyori/adventure-api" "adventure-api-*.jar"),
    (Find-Newest "libraries/net/kyori/adventure-key" "adventure-key-*.jar"),
    (Find-Newest "libraries/net/kyori/examination-api" "examination-api-*.jar"),
    (Find-Newest "libraries/org/jetbrains/annotations" "annotations-*.jar")
)

$CpParts = @($PaperApi, $UltimateTimber, $Slimefun, $ExoticGarden) + $Libs | Where-Object { $_ -and (Test-Path $_) }
$Cp = ($CpParts | Select-Object -Unique) -join ';'
if (-not (Test-Path $PaperApi)) { throw "Missing paper-api: $PaperApi" }

$JavaHome = [System.Environment]::GetEnvironmentVariable("JAVA_HOME")
if ([string]::IsNullOrWhiteSpace($JavaHome)) {
    $JavaHome = "C:\Program Files\Java\jdk-25.0.2"
}
$JarExe = Join-Path $JavaHome "bin/jar.exe"
$JavacExe = Join-Path $JavaHome "bin/javac.exe"
if (-not (Test-Path $JarExe)) { throw "Missing jar.exe: $JarExe" }
if (-not (Test-Path $JavacExe)) { throw "Missing javac.exe: $JavacExe" }

if (Test-Path $Out) { Remove-Item $Out -Recurse -Force }
New-Item -ItemType Directory -Path $Out | Out-Null

$JavaFiles = Get-ChildItem $Src -Recurse -Filter *.java | ForEach-Object { $_.FullName }
& $JavacExe -encoding UTF-8 -cp $Cp -d $Out $JavaFiles
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Copy-Item -Recurse -Force (Join-Path $Res "*") $Out
Publish-McwwsPluginJar -JarExe $JarExe -ClassesDir $Out -JarPaths $McwwsJar -PluginName "MCWWS_UltimateTimberFix"
