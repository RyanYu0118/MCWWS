$ErrorActionPreference = "Stop"
$Root = $PSScriptRoot
$Lib = Join-Path $Root "lib"
$Extracted = Join-Path $Lib "extracted"
$Src = Join-Path $Root "src/main/java"
$Out = Join-Path $Root "build/classes"
$Res = Join-Path $Root "src/main/resources"
$JarOut = Join-Path $Root "build/MCWWS_AxiomSurvivalClient.jar"
$McRoot = "D:\Minecraft\游戏主体\.minecraft"
$Deploy = Join-Path $McRoot "mods/MCWWS_AxiomSurvivalClient.jar"
$FastUtil = Join-Path $McRoot "libraries/it/unimi/dsi/fastutil/8.5.18/fastutil-8.5.18.jar"

$JavaHome = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { "C:\Program Files\Java\jdk-25.0.2" }
$Javac = Join-Path $JavaHome "bin/javac.exe"
$Jar = Join-Path $JavaHome "bin/jar.exe"

$Jspecify = Join-Path $Extracted "jspecify-1.0.0.jar"

$CpParts = @()
if (Test-Path $FastUtil) { $CpParts += $FastUtil }
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
if (Test-Path $JarOut) { Remove-Item $JarOut -Force }
& $Jar cf $JarOut -C $Out .
Write-Host "Built $JarOut"
cmd /c "copy /Y `"$JarOut`" `"$Deploy`" >nul"
Write-Host "Deployed $Deploy"
