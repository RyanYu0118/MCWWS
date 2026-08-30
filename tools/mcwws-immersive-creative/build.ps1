$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$Src = Join-Path $PSScriptRoot "src/main/java"
$Res = Join-Path $PSScriptRoot "src/main/resources"
$Out = Join-Path $PSScriptRoot "build/classes"
$JarOut = Join-Path $Root "plugins/MCWWS_ImmersiveCreative.jar"
$JarOutNew = Join-Path $Root "plugins/MCWWS_ImmersiveCreative.jar.new"

function Find-Newest {
    param([string]$RelativeDir, [string]$Filter)
    $dir = Join-Path $Root $RelativeDir
    if (-not (Test-Path $dir)) { return $null }
    $hit = Get-ChildItem $dir -Recurse -Filter $Filter | Sort-Object Name -Descending | Select-Object -First 1
    if ($hit) { return $hit.FullName }
    return $null
}

$PaperApi = Find-Newest "libraries/io/papermc/paper/paper-api" "paper-api-*.jar"
$ProtocolLib = Get-ChildItem (Join-Path $Root "plugins") -Filter "ProtocolLib*.jar" |
    Where-Object { $_.Name -notlike "*.new" } |
    Sort-Object Name -Descending |
    Select-Object -First 1 -ExpandProperty FullName
$Vault = Get-ChildItem (Join-Path $Root "plugins") -Filter "Vault*.jar" |
    Where-Object { $_.Name -notlike "*.new" } |
    Sort-Object Name -Descending |
    Select-Object -First 1 -ExpandProperty FullName
$Papi = Get-ChildItem (Join-Path $Root "plugins") -Filter "PlaceholderAPI*.jar" |
    Where-Object { $_.Name -notlike "*.new" } |
    Sort-Object Name -Descending |
    Select-Object -First 1 -ExpandProperty FullName
$NbtApi = Get-ChildItem (Join-Path $Root "plugins") -Filter "item-nbt-api-plugin*.jar" |
    Where-Object { $_.Name -notlike "*.new" } |
    Sort-Object Name -Descending |
    Select-Object -First 1 -ExpandProperty FullName

$Libs = @(
    (Find-Newest "libraries/com/google/guava/guava" "guava-*.jar"),
    (Find-Newest "libraries/net/kyori/adventure-api" "adventure-api-*.jar"),
    (Find-Newest "libraries/net/kyori/adventure-key" "adventure-key-*.jar"),
    (Find-Newest "libraries/net/kyori/adventure-text-serializer-legacy" "adventure-text-serializer-legacy-*.jar"),
    (Find-Newest "libraries/net/kyori/examination-api" "examination-api-*.jar"),
    (Find-Newest "libraries/org/jetbrains/annotations" "annotations-*.jar"),
    (Find-Newest "libraries/net/md-5/bungeecord-chat" "bungeecord-chat-*.jar")
)

$CpParts = @($PaperApi, $ProtocolLib, $Vault, $Papi, $NbtApi) + $Libs | Where-Object { $_ -and (Test-Path $_) }
$Cp = ($CpParts | Select-Object -Unique) -join ';'
if (-not (Test-Path $PaperApi)) { throw "Missing paper-api: $PaperApi" }
if (-not $ProtocolLib) { throw "Missing ProtocolLib jar in plugins/" }
if (-not $Vault) { throw "Missing Vault jar in plugins/" }
if (-not $NbtApi) { throw "Missing item-nbt-api-plugin jar in plugins/" }

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
if (Test-Path $JarOutNew) { Remove-Item $JarOutNew -Force }
& $JarExe cf $JarOutNew -C $Out .
if (Test-Path $JarOut) {
    try {
        Remove-Item $JarOut -Force
        Move-Item $JarOutNew $JarOut
        Write-Host "Built $JarOut"
    } catch {
        Write-Host "Built $JarOutNew (原 jar 被占用，停服后替换 plugins/MCWWS_ImmersiveCreative.jar)"
    }
} else {
    Move-Item $JarOutNew $JarOut
    Write-Host "Built $JarOut"
}
