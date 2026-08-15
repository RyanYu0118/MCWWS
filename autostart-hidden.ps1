$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root
$env:PAPER_DEFAULT_CENTRAL_REPOSITORY = "https://maven.aliyun.com/repository/central"

$jar = Get-ChildItem -File -Filter "paper-26.2-*.jar" |
  Sort-Object LastWriteTime -Descending |
  Select-Object -First 1
if (-not $jar) {
  exit 1
}

$javaw = Join-Path $env:ProgramFiles "Common Files\Oracle\Java\javapath\javaw.exe"
if (-not (Test-Path $javaw)) {
  $javaw = "javaw.exe"
}

$argList = @(
  "-Xms1G",
  "-Xmx16G",
  "-Dorg.bukkit.plugin.java.LibraryLoader.centralURL=https://maven.aliyun.com/repository/central",
  "-jar",
  $jar.FullName
)
Start-Process -FilePath $javaw -ArgumentList $argList -WorkingDirectory $root
