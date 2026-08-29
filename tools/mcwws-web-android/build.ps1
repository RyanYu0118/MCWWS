$ErrorActionPreference = "Stop"
$ProjectRoot = $PSScriptRoot
$RepoRoot = Split-Path -Parent (Split-Path -Parent $ProjectRoot)
$Toolchain = Join-Path $ProjectRoot ".toolchain"
$JdkDir = Join-Path $Toolchain "jdk-17"
$SdkDir = Join-Path $Toolchain "android-sdk"
$GradleDir = Join-Path $Toolchain "gradle-8.7"
$KeystoreDir = Join-Path $ProjectRoot "keystore"
$KeystoreFile = Join-Path $KeystoreDir "mcwws-release.jks"
$ApkDestDir = Join-Path $RepoRoot "plugins\Skript\scripts\web\public\app"
$AssetLinks = Join-Path $RepoRoot "plugins\Skript\scripts\web\public\.well-known\assetlinks.json"

# Read versionName from app/build.gradle
$AppGradle = Join-Path $ProjectRoot "app\build.gradle"
$VersionName = "0.0.0"
if (Test-Path $AppGradle) {
    $m = Select-String -Path $AppGradle -Pattern "versionName\s+'([^']+)'" | Select-Object -First 1
    if ($m) { $VersionName = $m.Matches[0].Groups[1].Value }
}
$ApkFileName = "MCWWS-$VersionName.apk"
$ApkDest = Join-Path $ApkDestDir $ApkFileName
$ApkLatestAlias = Join-Path $ApkDestDir "MCWWS.apk"

New-Item -ItemType Directory -Force -Path $Toolchain, $KeystoreDir, $ApkDestDir | Out-Null
New-Item -ItemType Directory -Force -Path (Split-Path $AssetLinks) | Out-Null

function Get-File($Url, $OutFile, $Mirrors) {
    $candidates = @($Url) + @($Mirrors)
    foreach ($u in $candidates) {
        if ([string]::IsNullOrWhiteSpace($u)) { continue }
        try {
            Write-Host "GET $u"
            Invoke-WebRequest -Uri $u -OutFile $OutFile -UseBasicParsing
            if ((Test-Path $OutFile) -and ((Get-Item $OutFile).Length -gt 10000)) {
                return
            }
        } catch {
            Write-Host "FAIL $($_.Exception.Message)"
        }
    }
    throw "Download failed: $Url"
}

function Expand-Zip($Zip, $Dest) {
    if (Test-Path $Dest) { Remove-Item $Dest -Recurse -Force }
    New-Item -ItemType Directory -Force -Path $Dest | Out-Null
    Expand-Archive -LiteralPath $Zip -DestinationPath $Dest -Force
}

if (-not (Test-Path (Join-Path $JdkDir "bin\java.exe"))) {
    Write-Host "Preparing JDK 17..."
    $jdkZip = Join-Path $Toolchain "jdk17.zip"
    Get-File `
        "https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse?project=jdk" `
        $jdkZip `
        @("https://mirrors.cloud.tencent.com/AdoptOpenJDK/17/jdk/x64/windows/OpenJDK17U-jdk_x64_windows_hotspot_17.0.13_11.zip")
    $jdkExtract = Join-Path $Toolchain "jdk-extract"
    Expand-Zip $jdkZip $jdkExtract
    $found = Get-ChildItem $jdkExtract -Directory | Where-Object { Test-Path (Join-Path $_.FullName "bin\java.exe") } | Select-Object -First 1
    if (-not $found) { throw "java.exe missing after JDK extract" }
    if (Test-Path $JdkDir) { Remove-Item $JdkDir -Recurse -Force }
    Move-Item $found.FullName $JdkDir
    Remove-Item $jdkExtract -Recurse -Force -ErrorAction SilentlyContinue
    Remove-Item $jdkZip -Force -ErrorAction SilentlyContinue
}

if (-not (Test-Path (Join-Path $GradleDir "bin\gradle.bat"))) {
    Write-Host "Preparing Gradle 8.7..."
    $gradleZip = Join-Path $Toolchain "gradle.zip"
    Get-File `
        "https://mirrors.cloud.tencent.com/gradle/gradle-8.7-bin.zip" `
        $gradleZip `
        @("https://services.gradle.org/distributions/gradle-8.7-bin.zip")
    $gradleExtract = Join-Path $Toolchain "gradle-extract"
    Expand-Zip $gradleZip $gradleExtract
    $found = Get-ChildItem $gradleExtract -Directory | Where-Object { Test-Path (Join-Path $_.FullName "bin\gradle.bat") } | Select-Object -First 1
    if (-not $found) { throw "gradle.bat missing after extract" }
    if (Test-Path $GradleDir) { Remove-Item $GradleDir -Recurse -Force }
    Move-Item $found.FullName $GradleDir
    Remove-Item $gradleExtract -Recurse -Force -ErrorAction SilentlyContinue
    Remove-Item $gradleZip -Force -ErrorAction SilentlyContinue
}

$licDir = Join-Path $SdkDir "licenses"
New-Item -ItemType Directory -Force -Path $licDir | Out-Null
@(
    "2438fde996e7506c32f75f9c3c55ce6c",
    "d56f5187479451eabf01fb78af6dfcb131a6481e",
    "2438fde996e7506c32f75f9c3c55ce6c"
) | Set-Content -Path (Join-Path $licDir "android-sdk-license") -Encoding ASCII
Set-Content -Path (Join-Path $licDir "android-sdk-preview-license") -Value "84831b9409646161da1e65340980e60f9601ecf8" -Encoding ASCII

$platformDir = Join-Path $SdkDir "platforms\android-34"
$buildToolsDir = Join-Path $SdkDir "build-tools\34.0.0"
New-Item -ItemType Directory -Force -Path (Join-Path $SdkDir "platforms"), (Join-Path $SdkDir "build-tools") | Out-Null

if (-not (Test-Path (Join-Path $platformDir "android.jar"))) {
    Write-Host "Preparing Android SDK platform 34..."
    $platZip = Join-Path $Toolchain "platform-34.zip"
    Get-File `
        "https://mirrors.cloud.tencent.com/AndroidSDK/platform-34-ext7_r03.zip" `
        $platZip `
        @(
            "https://mirrors.cloud.tencent.com/AndroidSDK/repository/platform-34-ext7_r03.zip",
            "https://mirrors.tuna.tsinghua.edu.cn/android/repository/platform-34-ext7_r03.zip",
            "https://dl.google.com/android/repository/platform-34-ext7_r03.zip",
            "https://dl.google.com/android/repository/platform-34_r03.zip"
        )
    $extract = Join-Path $Toolchain "platform-extract"
    Expand-Zip $platZip $extract
    $found = Get-ChildItem $extract -Directory | Where-Object { Test-Path (Join-Path $_.FullName "android.jar") } | Select-Object -First 1
    if (-not $found) { $found = Get-ChildItem $extract -Recurse -Directory | Where-Object { Test-Path (Join-Path $_.FullName "android.jar") } | Select-Object -First 1 }
    if (-not $found) { throw "platform-34 extract failed" }
    if (Test-Path $platformDir) { Remove-Item $platformDir -Recurse -Force }
    Move-Item $found.FullName $platformDir
    Remove-Item $extract -Recurse -Force -ErrorAction SilentlyContinue
    Remove-Item $platZip -Force -ErrorAction SilentlyContinue
}

if (-not (Test-Path (Join-Path $buildToolsDir "aapt.exe")) -and -not (Test-Path (Join-Path $buildToolsDir "aapt2.exe"))) {
    Write-Host "Preparing Android build-tools 34..."
    $btZip = Join-Path $Toolchain "build-tools.zip"
    Get-File `
        "https://mirrors.cloud.tencent.com/AndroidSDK/build-tools_r34-windows.zip" `
        $btZip `
        @(
            "https://mirrors.cloud.tencent.com/AndroidSDK/repository/build-tools_r34-windows.zip",
            "https://mirrors.tuna.tsinghua.edu.cn/android/repository/build-tools_r34-windows.zip",
            "https://dl.google.com/android/repository/build-tools_r34-windows.zip"
        )
    $extract = Join-Path $Toolchain "build-tools-extract"
    Expand-Zip $btZip $extract
    $found = Get-ChildItem $extract -Directory | Select-Object -First 1
    if (-not $found) { throw "build-tools extract failed" }
    if (Test-Path $buildToolsDir) { Remove-Item $buildToolsDir -Recurse -Force }
    Move-Item $found.FullName $buildToolsDir
    $sp = Join-Path $buildToolsDir "source.properties"
    if (-not (Test-Path $sp)) {
        @"
Pkg.UserSrc=false
Pkg.Revision=34.0.0
"@ | Set-Content -Path $sp -Encoding ASCII
    }
    Remove-Item $extract -Recurse -Force -ErrorAction SilentlyContinue
    Remove-Item $btZip -Force -ErrorAction SilentlyContinue
}

$javaHome = $JdkDir
$keytool = Join-Path $javaHome "bin\keytool.exe"
if (-not (Test-Path $KeystoreFile)) {
    Write-Host "Generating signing keystore..."
    & $keytool -genkeypair -v `
        -keystore $KeystoreFile `
        -alias mcwws `
        -keyalg RSA `
        -keysize 2048 `
        -validity 10000 `
        -storepass mcwws-web-app `
        -keypass mcwws-web-app `
        -dname "CN=MCWWS, OU=RYAN STUDIO, O=RYAN STUDIO, L=Internet, ST=Internet, C=CN"
    if ($LASTEXITCODE -ne 0) { throw "keytool failed" }
}

$sdkDirForGradle = $SdkDir
try {
    $fso = New-Object -ComObject Scripting.FileSystemObject
    $short = $fso.GetFolder($SdkDir).ShortPath
    if ($short) { $sdkDirForGradle = $short }
} catch {
    # keep original path
}
$sdkDirUnix = ($sdkDirForGradle -replace '\\', '/')
$localProps = Join-Path $ProjectRoot "local.properties"
[System.IO.File]::WriteAllText($localProps, "sdk.dir=$sdkDirUnix`n", [System.Text.UTF8Encoding]::new($false))

Write-Host "Building release APK..."
$env:JAVA_HOME = $javaHome
$env:ANDROID_HOME = $SdkDir
$env:ANDROID_SDK_ROOT = $SdkDir
$env:GRADLE_USER_HOME = Join-Path $Toolchain "gradle-home"
New-Item -ItemType Directory -Force -Path $env:GRADLE_USER_HOME | Out-Null

$gradleBat = Join-Path $GradleDir "bin\gradle.bat"
Push-Location $ProjectRoot
try {
    & $gradleBat --no-daemon assembleRelease
    if ($LASTEXITCODE -ne 0) { throw "Gradle assembleRelease failed" }
} finally {
    Pop-Location
}

$built = Join-Path $ProjectRoot "app\build\outputs\apk\release\app-release.apk"
if (-not (Test-Path $built)) { throw "APK not found: $built" }
Copy-Item -Force $built $ApkDest
Copy-Item -Force $built $ApkLatestAlias
Write-Host "Copied APK to $ApkDest"
Write-Host "Also updated latest alias $ApkLatestAlias"

$certOut = & $keytool -list -v -keystore $KeystoreFile -alias mcwws -storepass mcwws-web-app
$shaLine = ($certOut | Select-String -Pattern 'SHA-256:|SHA256:').Line | Select-Object -First 1
if ($shaLine) {
    $fp = ($shaLine -replace '.*SHA-256:\s*', '').Trim() -replace ':', ''
    $colon = ($fp -replace '.{2}', '$0:').TrimEnd(':').ToUpper()
    $json = @"
[
  {
    "relation": ["delegate_permission/common.handle_all_urls"],
    "target": {
      "namespace": "android_app",
      "package_name": "work.mcwws.webapp",
      "sha256_cert_fingerprints": ["$colon"]
    }
  }
]
"@
    [System.IO.File]::WriteAllText($AssetLinks, $json.Trim() + "`n", [System.Text.UTF8Encoding]::new($false))
    Write-Host "Wrote assetlinks.json"
}

Write-Host "Done. APK: https://mcs.ryanstudio.work/app/$ApkFileName"
