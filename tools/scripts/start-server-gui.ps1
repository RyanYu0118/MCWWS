$Root = 'D:\Minecraft\MCWWS'
Set-Location $Root
# Kill empty launcher shell if leftover from failed start
Get-CimInstance Win32_Process -Filter "Name = 'javaw.exe'" | ForEach-Object {
  $cmd = $_.CommandLine
  if ($cmd -and ($cmd -like '*paper-26.2*' -or $cmd -like '*MCWWS*')) {
    Write-Host ("existing javaw PID={0} cmd={1}" -f $_.ProcessId, $cmd)
  }
}
Start-Process -FilePath 'javaw' -ArgumentList @(
  '-Xms1G',
  '-Xmx16G',
  '-Dorg.bukkit.plugin.java.LibraryLoader.centralURL=https://maven.aliyun.com/repository/central',
  '-jar',
  'paper-26.2-112.jar'
) -WorkingDirectory $Root
Start-Sleep -Seconds 8
Get-CimInstance Win32_Process -Filter "Name = 'javaw.exe'" | ForEach-Object {
  Write-Host ("PID={0} WS={1}MB CMD={2}" -f $_.ProcessId, [math]::Round((Get-Process -Id $_.ProcessId).WorkingSet64/1MB,1), $_.CommandLine)
}
Get-Process javaw -ErrorAction SilentlyContinue | ForEach-Object {
  Write-Host ("window PID={0} title=[{1}]" -f $_.Id, $_.MainWindowTitle)
}
