$ErrorActionPreference = "Stop"
$Root = $PSScriptRoot
$Lib = Join-Path $Root "lib"
$AxiomLib = Join-Path (Split-Path $Root) "mcwws-axiom-survival-client\lib"
$Extracted = Join-Path $AxiomLib "extracted"
$Src = Join-Path $Root "src/main/java"
$Out = Join-Path $Root "build/classes"
$Res = Join-Path $Root "src/main/resources"
$JarOut = Join-Path $Root "build/MCWWS_ImmersiveCreativeClient.jar"

$McRoot = $env:MCWWS_MC_ROOT
if ([string]::IsNullOrWhiteSpace($McRoot)) {
    $McRoot = Get-ChildItem "D:\Minecraft" -Directory |
        ForEach-Object { Join-Path $_.FullName ".minecraft" } |
        Where-Object { Test-Path (Join-Path $_ "mods") } |
        Sort-Object { $log = Join-Path $_ "logs\latest.log"; if (Test-Path $log) { (Get-Item $log).LastWriteTime } else { [DateTime]::MinValue } } -Descending |
        Select-Object -First 1
}
if ([string]::IsNullOrWhiteSpace($McRoot)) { throw "Cannot locate .minecraft game directory" }
$Deploy = Join-Path $McRoot "mods/MCWWS_ImmersiveCreativeClient.jar"
$FastUtil = Join-Path $McRoot "libraries/it/unimi/dsi/fastutil/8.5.18/fastutil-8.5.18.jar"
# Codec / DynamicOps / Keyable 都在 DataFixerUpper 里，缺了它 javac 会报「无法访问 Codec」
$DfuDir = Join-Path $McRoot "libraries\com\mojang\datafixerupper"
$Dfu = $null
if ([System.IO.Directory]::Exists($DfuDir)) {
    $Dfu = [System.IO.Directory]::GetFiles($DfuDir, "datafixerupper-*.jar", "AllDirectories") |
        Sort-Object { [version]([System.IO.Path]::GetFileName([System.IO.Path]::GetDirectoryName($_))) } -Descending |
        Select-Object -First 1
}
if (-not $Dfu) { throw "Cannot locate datafixerupper jar under $DfuDir" }
$AsmTree = Join-Path $McRoot "libraries/org/ow2/asm/asm-tree/9.10.1/asm-tree-9.10.1.jar"
$Asm = Join-Path $McRoot "libraries/org/ow2/asm/asm/9.10.1/asm-9.10.1.jar"
$ServerRoot = Split-Path -Parent (Split-Path $Root)
if (-not (Test-Path $AsmTree)) {
    $AsmTree = Join-Path $ServerRoot "libraries/org/ow2/asm/asm-tree/9.9.1/asm-tree-9.9.1.jar"
    $Asm = Join-Path $ServerRoot "libraries/org/ow2/asm/asm/9.9.1/asm-9.9.1.jar"
}

$JavaHome = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { "C:\Program Files\Java\jdk-25.0.2" }
$Javac = Join-Path $JavaHome "bin/javac.exe"
$Jar = Join-Path $JavaHome "bin/jar.exe"

$Jspecify = Join-Path $Extracted "jspecify-1.0.0.jar"

$CpParts = @()
if (Test-Path $FastUtil) { $CpParts += $FastUtil }
if ($Dfu -and (Test-Path $Dfu)) { $CpParts += $Dfu }
if (Test-Path $Asm) { $CpParts += $Asm }
if (Test-Path $AsmTree) { $CpParts += $AsmTree }
if (Test-Path $Jspecify) { $CpParts += $Jspecify }
if (Test-Path $Lib) { $CpParts += (Join-Path $Lib "*") }
if (Test-Path $AxiomLib) { $CpParts += (Join-Path $AxiomLib "*") }
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
Copy-Item -LiteralPath $JarOut -Destination $Deploy -Force
Write-Host "Deployed $Deploy"
Get-ChildItem "D:\Minecraft" -Directory | ForEach-Object {
    $extra = Join-Path $_.FullName ".minecraft\mods\MCWWS_ImmersiveCreativeClient.jar"
    if ((Test-Path $extra) -and ($extra -ne $Deploy)) {
        Copy-Item -LiteralPath $JarOut -Destination $extra -Force
        Write-Host "Deployed $extra"
    }
}
