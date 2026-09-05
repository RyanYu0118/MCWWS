$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$Src = Join-Path $PSScriptRoot "src/main/java"
$Res = Join-Path $PSScriptRoot "src/main/resources"
$Out = Join-Path $PSScriptRoot "build/classes"
. (Join-Path $Root "tools/scripts/mcwws-jar-name.ps1")
$McwwsJar = Get-McwwsPluginJarPaths -RepoRoot $Root -PluginName "MCWWS_UltimateAdvancements" -ResourcesDir $Res
$JarOut = $McwwsJar.JarOut
$JarOutNew = $McwwsJar.JarOutNew

function Find-NewestJar {
    param([string]$Dir, [string]$Filter)
    $hit = Get-ChildItem $Dir -Recurse -Filter $Filter | Sort-Object Name -Descending | Select-Object -First 1
    if ($hit) { return $hit.FullName }
    return $null
}

$PaperApi = Find-NewestJar (Join-Path $Root "libraries/io/papermc/paper/paper-api") "paper-api-*.jar"
if (-not $PaperApi) {
    # Fallback: 如果你机器上目录结构和脚本默认路径不一致，手动把 paper-api jar 放到 tools 的 libraries 里即可。
    $PaperApi = Join-Path $Root "libraries/io/papermc/paper/paper-api/26.2.build.103-stable/paper-api-26.2.build.103-stable.jar"
}

$UAAPI = Get-ChildItem (Join-Path $Root "plugins") -Filter "UltimateAdvancementAPI-*.jar" |
    Where-Object { $_.Name -notlike "*.new" } |
    Sort-Object Name -Descending |
    Select-Object -First 1 -ExpandProperty FullName
if (-not $UAAPI) { throw "Missing UltimateAdvancementAPI jar in plugins/" }

$Libs = @(
    (Find-NewestJar (Join-Path $Root "libraries/com/google/guava/guava") "guava-*.jar"),
    (Find-NewestJar (Join-Path $Root "libraries/net/kyori/adventure-api") "adventure-api-*.jar"),
    (Find-NewestJar (Join-Path $Root "libraries/net/kyori/adventure-key") "adventure-key-*.jar"),
    (Find-NewestJar (Join-Path $Root "libraries/net/kyori/adventure-text-serializer-legacy") "adventure-text-serializer-legacy-*.jar"),
    (Find-NewestJar (Join-Path $Root "libraries/net/kyori/examination-api") "examination-api-*.jar"),
    (Find-NewestJar (Join-Path $Root "libraries/net/kyori/examination-string") "examination-string-*.jar"),
    (Find-NewestJar (Join-Path $Root "libraries/net/kyori/option") "option-*.jar"),
    (Find-NewestJar (Join-Path $Root "libraries/net/md-5/bungeecord-chat") "bungeecord-chat-*.jar"),
    (Find-NewestJar (Join-Path $Root "libraries/org/jetbrains/annotations") "annotations-*.jar")
)

$CpParts = @($PaperApi, $UAAPI) + $Libs | Where-Object { $_ -and (Test-Path $_) } | Select-Object -Unique
$Cp = ($CpParts -join ';')
if (-not (Test-Path $PaperApi)) { throw "Missing paper-api jar: $PaperApi" }
if (-not (Test-Path $UAAPI)) { throw "Missing UltimateAdvancementAPI jar: $UAAPI" }

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
Publish-McwwsPluginJar -JarExe $JarExe -ClassesDir $Out -JarPaths $McwwsJar -PluginName "MCWWS_UltimateAdvancements"

