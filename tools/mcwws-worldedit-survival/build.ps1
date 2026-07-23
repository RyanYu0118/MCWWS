$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$Src = Join-Path $PSScriptRoot "src/main/java"
$Res = Join-Path $PSScriptRoot "src/main/resources"
$Out = Join-Path $PSScriptRoot "build/classes"
$JarOut = Join-Path $Root "plugins/MCWWS_WorldEditSurvival.jar"

$PaperApi = Join-Path $Root "libraries/io/papermc/paper/paper-api/1.21.11-R0.1-SNAPSHOT/paper-api-1.21.11-R0.1-SNAPSHOT.jar"
$Fawe = Join-Path $Root "plugins/FastAsyncWorldEdit-Paper-2.15.2.jar"
$Vault = Join-Path $Root "plugins/Vault.jar"
$Slimefun = Get-ChildItem (Join-Path $Root "plugins") -Filter "Slimefun-*.jar" | Select-Object -First 1 -ExpandProperty FullName
$Libs = @(
    (Join-Path $Root "libraries/com/google/guava/guava/33.3.1-jre/guava-33.3.1-jre.jar"),
    (Join-Path $Root "libraries/com/google/guava/failureaccess/1.0.2/failureaccess-1.0.2.jar"),
    (Join-Path $Root "libraries/net/kyori/adventure-api/4.26.1/adventure-api-4.26.1.jar"),
    (Join-Path $Root "libraries/net/kyori/adventure-key/4.26.1/adventure-key-4.26.1.jar"),
    (Join-Path $Root "libraries/net/kyori/adventure-text-serializer-legacy/4.26.1/adventure-text-serializer-legacy-4.26.1.jar"),
    (Join-Path $Root "libraries/net/kyori/examination-api/1.3.0/examination-api-1.3.0.jar"),
    (Join-Path $Root "libraries/net/kyori/examination-string/1.3.0/examination-string-1.3.0.jar"),
    (Join-Path $Root "libraries/net/md-5/bungeecord-chat/1.21-R0.2-deprecated+build.21/bungeecord-chat-1.21-R0.2-deprecated+build.21.jar"),
    (Join-Path $Root "libraries/org/jetbrains/annotations/24.1.0/annotations-24.1.0.jar")
)

$Cp = (@($PaperApi, $Fawe, $Vault, $Slimefun) + $Libs) -join ';'
if (-not (Test-Path $PaperApi)) { throw "Missing paper-api: $PaperApi" }
if (-not (Test-Path $Fawe)) { throw "Missing FAWE jar: $Fawe" }

$JavaHome = [System.Environment]::GetEnvironmentVariable("JAVA_HOME")
if ([string]::IsNullOrWhiteSpace($JavaHome)) {
    $JavaHome = "C:\Program Files\Java\jdk-25.0.2"
}
$JarExe = Join-Path $JavaHome "bin/jar.exe"
if (-not (Test-Path $JarExe)) { throw "Missing jar.exe: $JarExe" }

if (Test-Path $Out) { Remove-Item $Out -Recurse -Force }
New-Item -ItemType Directory -Path $Out | Out-Null

$JavaFiles = Get-ChildItem $Src -Recurse -Filter *.java | ForEach-Object { $_.FullName }
javac -encoding UTF-8 -cp $Cp -d $Out $JavaFiles
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Copy-Item -Recurse -Force (Join-Path $Res "*") $Out
if (Test-Path $JarOut) { Remove-Item $JarOut -Force }
& $JarExe cf $JarOut -C $Out .
Write-Host "Built $JarOut"
