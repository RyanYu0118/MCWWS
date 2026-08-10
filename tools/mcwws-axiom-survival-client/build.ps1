$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$Src = Join-Path $PSScriptRoot "src/main/java"
$Res = Join-Path $PSScriptRoot "src/main/resources"
$Out = Join-Path $PSScriptRoot "build/classes"
$LibDir = Join-Path $PSScriptRoot "lib"
$JarOut = Join-Path $Root "tools/mcwws-axiom-survival-client/build/MCWWS_AxiomSurvivalClient.jar"

function Find-FirstJar($patterns) {
    foreach ($pattern in $patterns) {
        $hit = Get-ChildItem -Path $pattern -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -First 1
        if ($hit) { return $hit.FullName }
    }
    return $null
}

$Axiom = Find-FirstJar @(
    (Join-Path $LibDir "Axiom*.jar"),
    "$env:APPDATA\.minecraft\mods\Axiom*.jar",
    "$env:USERPROFILE\.minecraft\mods\Axiom*.jar",
    "D:\Minecraft\游戏数据\.minecraft\mods\Axiom*.jar"
)
$FabricApi = Find-FirstJar @(
    (Join-Path $LibDir "fabric-api*.jar"),
    "$env:APPDATA\.minecraft\mods\fabric-api*.jar",
    "$env:USERPROFILE\.minecraft\mods\fabric-api*.jar",
    "D:\Minecraft\游戏数据\.minecraft\mods\fabric-api*.jar"
)
$MinecraftClient = Find-FirstJar @(
    (Join-Path $LibDir "minecraft-client*.jar"),
    (Join-Path $Root "bluemap/minecraft-client*.jar")
)
$FabricLoader = Find-FirstJar @(
    (Join-Path $LibDir "fabric-loader*.jar"),
    "$env:USERPROFILE\.minecraft\libraries\net\fabricmc\fabric-loader\*\fabric-loader-*.jar"
)
$Mixin = Find-FirstJar @(
    (Join-Path $LibDir "mixin-*.jar"),
    "$env:USERPROFILE\.minecraft\libraries\net\fabricmc\sponge-mixin\*\mixin-*.jar"
)

if (-not $Axiom) { throw "Missing Axiom jar. Copy to tools/mcwws-axiom-survival-client/lib/" }
if (-not $FabricApi) { throw "Missing fabric-api jar." }
if (-not $MinecraftClient) { throw "Missing minecraft-client jar." }
if (-not $FabricLoader) { throw "Missing fabric-loader jar." }
if (-not $Mixin) { throw "Missing mixin jar." }

$Cp = ($Axiom, $FabricApi, $MinecraftClient, $FabricLoader, $Mixin | Select-Object -Unique) -join ';'

$JavaHome = [System.Environment]::GetEnvironmentVariable("JAVA_HOME")
if ([string]::IsNullOrWhiteSpace($JavaHome)) {
    $JavaHome = "C:\Program Files\Java\jdk-25.0.2"
}
$JarExe = Join-Path $JavaHome "bin/jar.exe"
if (-not (Test-Path $JarExe)) { throw "Missing jar.exe: $JarExe" }

if (Test-Path $Out) { Remove-Item $Out -Recurse -Force }
New-Item -ItemType Directory -Path $Out | Out-Null
New-Item -ItemType Directory -Path (Split-Path $JarOut) -Force | Out-Null

$JavaFiles = Get-ChildItem $Src -Recurse -Filter *.java | ForEach-Object { $_.FullName }
javac -encoding UTF-8 -cp $Cp -d $Out $JavaFiles
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Copy-Item -Recurse -Force (Join-Path $Res "*") $Out
if (Test-Path $JarOut) { Remove-Item $JarOut -Force }
& $JarExe cf $JarOut -C $Out .
Write-Host "Built $JarOut"
Write-Host "Deploy: copy to .minecraft/mods/MCWWS_AxiomSurvivalClient.jar"
