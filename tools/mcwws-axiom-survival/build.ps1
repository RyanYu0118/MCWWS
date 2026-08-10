$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$Src = Join-Path $PSScriptRoot "src/main/java"
$Res = Join-Path $PSScriptRoot "src/main/resources"
$Out = Join-Path $PSScriptRoot "build/classes"
$JarOut = Join-Path $Root "plugins/MCWWS_AxiomSurvival.jar"
$JarOutNew = Join-Path $Root "plugins/MCWWS_AxiomSurvival.jar.new"

$PaperApi = Join-Path $Root "libraries/io/papermc/paper/paper-api/26.2.build.103-stable/paper-api-26.2.build.103-stable.jar"
if (-not (Test-Path $PaperApi)) {
    $PaperApiCandidates = Get-ChildItem (Join-Path $Root "libraries/io/papermc/paper/paper-api") -Recurse -Filter "paper-api-*.jar" | Sort-Object Name -Descending
    if ($PaperApiCandidates) { $PaperApi = $PaperApiCandidates[0].FullName }
}

$AxiomCandidates = Get-ChildItem (Join-Path $Root "plugins") -Filter "AxiomPaper*.jar" | Sort-Object Name -Descending
$Axiom = if ($AxiomCandidates) { $AxiomCandidates[0].FullName } else { Join-Path $PSScriptRoot "lib/AxiomPaper.jar" }
$Vault = Join-Path $Root "plugins/Vault.jar"
$Slimefun = Get-ChildItem (Join-Path $Root "plugins") -Filter "Slimefun-*.jar" | Select-Object -First 1 -ExpandProperty FullName
$PaperServer = Join-Path $Root "paper-26.2-98.jar"
if (-not (Test-Path $PaperServer)) {
    $PaperServerCandidates = Get-ChildItem $Root -Filter "paper-26.2-*.jar" | Sort-Object Name -Descending
    if ($PaperServerCandidates) { $PaperServer = $PaperServerCandidates[0].FullName }
}

$Libs = @(
    (Join-Path $Root "libraries/com/google/guava/guava/33.6.0-jre/guava-33.6.0-jre.jar"),
    (Join-Path $Root "libraries/com/google/guava/failureaccess/1.0.3/failureaccess-1.0.3.jar"),
    (Join-Path $Root "libraries/net/kyori/adventure-api/5.2.0/adventure-api-5.2.0.jar"),
    (Join-Path $Root "libraries/net/kyori/adventure-key/5.2.0/adventure-key-5.2.0.jar"),
    (Join-Path $Root "libraries/net/kyori/adventure-text-serializer-legacy/5.2.0/adventure-text-serializer-legacy-5.2.0.jar"),
    (Join-Path $Root "libraries/net/kyori/examination-api/1.3.0/examination-api-1.3.0.jar"),
    (Join-Path $Root "libraries/net/kyori/examination-string/1.3.0/examination-string-1.3.0.jar"),
    (Join-Path $Root "libraries/net/kyori/option/1.1.0/option-1.1.0.jar"),
    (Join-Path $Root "libraries/net/md-5/bungeecord-chat/1.21-R0.2-deprecated+build.21/bungeecord-chat-1.21-R0.2-deprecated+build.21.jar"),
    (Join-Path $Root "libraries/org/jetbrains/annotations/26.1.0/annotations-26.1.0.jar"),
    (Join-Path $Root "libraries/io/netty/netty-buffer/4.1.118.Final/netty-buffer-4.1.118.Final.jar"),
    (Join-Path $Root "libraries/io/netty/netty-common/4.1.118.Final/netty-common-4.1.118.Final.jar")
)

$FlyWithFood = Join-Path $Root "plugins/FlyWithFood-2.0.7-all.jar"
if (-not (Test-Path $FlyWithFood)) {
    $FlyWithFoodCandidates = Get-ChildItem (Join-Path $Root "plugins") -Filter "FlyWithFood*.jar" | Sort-Object Name -Descending
    if ($FlyWithFoodCandidates) { $FlyWithFood = $FlyWithFoodCandidates[0].FullName }
}

$CpParts = @($PaperApi, $Axiom, $Vault, $Slimefun, $FlyWithFood) + $Libs | Where-Object { $_ -and (Test-Path $_) }
$Cp = ($CpParts | Select-Object -Unique) -join ';'
if (-not (Test-Path $PaperApi)) { throw "Missing paper-api: $PaperApi" }
if (-not (Test-Path $Axiom)) { throw "Missing AxiomPaper jar: $Axiom" }

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
if (Test-Path $JarOutNew) { Remove-Item $JarOutNew -Force }
& $JarExe cf $JarOutNew -C $Out .
if (Test-Path $JarOut) {
    try {
        Remove-Item $JarOut -Force
        Move-Item $JarOutNew $JarOut
        Write-Host "Built $JarOut"
    } catch {
        Write-Host "Built $JarOutNew (原 jar 被占用，停服后替换 plugins/MCWWS_AxiomSurvival.jar)"
    }
} else {
    Move-Item $JarOutNew $JarOut
    Write-Host "Built $JarOut"
}
