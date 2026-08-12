<#
.SYNOPSIS
关闭指定进程前，先弹出 10 秒倒计时确认框。

.DESCRIPTION
Agent 需要关闭任何进程时统一走这里。倒计时结束或用户点「立即关闭进程」即执行，
点「稍后执行」则放弃（退出码 1）。

关闭方式按优雅程度递减：
  1. 有主窗口 -> CloseMainWindow()，Minecraft 图形窗口据此走正常关服保存
  2. 无主窗口但有控制台 -> AttachConsole + Ctrl+C，触发 JVM shutdown hook
  3. 以上都失败且显式指定 -Force -> Stop-Process -Force

注意：本文件必须以 UTF-8 with BOM 保存，否则 Windows PowerShell 5.1 会按 ANSI 解析中文导致语法错误。

.EXAMPLE
powershell -STA -NoProfile -ExecutionPolicy Bypass -File tools\scripts\stop-process.ps1 -ProcessId 45432 -Label "Minecraft 服务器"
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][int]$ProcessId,
    [string]$Label = "",
    [int]$TimeoutSeconds = 10,
    [int]$WaitSeconds = 180,
    [switch]$Force
)

$ErrorActionPreference = "Stop"

$proc = Get-Process -Id $ProcessId -ErrorAction SilentlyContinue
if (-not $proc) {
    Write-Host "进程 $ProcessId 不存在，无需关闭"
    exit 0
}
if (-not $Label) { $Label = $proc.ProcessName }

Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing

$form = New-Object System.Windows.Forms.Form
$form.Text = "关闭进程确认"
$form.Size = New-Object System.Drawing.Size(430, 190)
$form.StartPosition = "CenterScreen"
$form.TopMost = $true
$form.FormBorderStyle = "FixedDialog"
$form.MaximizeBox = $false
$form.MinimizeBox = $false

# 变量名不能叫 $label：PowerShell 变量不区分大小写，会和 [string]$Label 参数撞车
$countdownText = New-Object System.Windows.Forms.Label
$countdownText.Location = New-Object System.Drawing.Point(20, 25)
$countdownText.Size = New-Object System.Drawing.Size(380, 60)
$countdownText.Font = New-Object System.Drawing.Font("Microsoft YaHei UI", 10)
$form.Controls.Add($countdownText)

$script:remaining = $TimeoutSeconds
$countdownText.Text = "$script:remaining 秒后自动关闭进程：$Label（PID $ProcessId）"

$okButton = New-Object System.Windows.Forms.Button
$okButton.Location = New-Object System.Drawing.Point(90, 100)
$okButton.Size = New-Object System.Drawing.Size(120, 34)
$okButton.Text = "立即关闭进程"
$okButton.Font = New-Object System.Drawing.Font("Microsoft YaHei UI", 9)
$okButton.DialogResult = [System.Windows.Forms.DialogResult]::OK
$form.Controls.Add($okButton)
$form.AcceptButton = $okButton

$cancelButton = New-Object System.Windows.Forms.Button
$cancelButton.Location = New-Object System.Drawing.Point(225, 100)
$cancelButton.Size = New-Object System.Drawing.Size(120, 34)
$cancelButton.Text = "稍后执行"
$cancelButton.Font = New-Object System.Drawing.Font("Microsoft YaHei UI", 9)
$cancelButton.DialogResult = [System.Windows.Forms.DialogResult]::Cancel
$form.Controls.Add($cancelButton)
$form.CancelButton = $cancelButton

$timer = New-Object System.Windows.Forms.Timer
$timer.Interval = 1000
$timer.Add_Tick({
    $script:remaining--
    if ($script:remaining -le 0) {
        $timer.Stop()
        $form.DialogResult = [System.Windows.Forms.DialogResult]::OK
        $form.Close()
    } else {
        $countdownText.Text = "$script:remaining 秒后自动关闭进程：$Label（PID $ProcessId）"
    }
})
$timer.Start()

$result = $form.ShowDialog()
$timer.Stop()
$form.Dispose()

if ($result -ne [System.Windows.Forms.DialogResult]::OK) {
    Write-Host "用户选择稍后执行，未关闭 $Label"
    exit 1
}

$proc = Get-Process -Id $ProcessId -ErrorAction SilentlyContinue
if (-not $proc) {
    Write-Host "进程已自行退出"
    exit 0
}

$closed = $false
if ($proc.MainWindowHandle -ne [IntPtr]::Zero) {
    Write-Host "向主窗口发送关闭消息…"
    $closed = $proc.CloseMainWindow()
}

if (-not $closed) {
    Write-Host "无主窗口，改用控制台 Ctrl+C…"
    Add-Type -Namespace Win32 -Name Con -MemberDefinition @'
[DllImport("kernel32.dll", SetLastError=true)] public static extern bool FreeConsole();
[DllImport("kernel32.dll", SetLastError=true)] public static extern bool AttachConsole(uint dwProcessId);
[DllImport("kernel32.dll", SetLastError=true)] public static extern bool SetConsoleCtrlHandler(IntPtr handler, bool add);
[DllImport("kernel32.dll", SetLastError=true)] public static extern bool GenerateConsoleCtrlEvent(uint dwCtrlEvent, uint dwProcessGroupId);
'@
    [Win32.Con]::FreeConsole() | Out-Null
    if ([Win32.Con]::AttachConsole([uint32]$ProcessId)) {
        [Win32.Con]::SetConsoleCtrlHandler([IntPtr]::Zero, $true) | Out-Null
        Start-Sleep -Milliseconds 300
        [Win32.Con]::GenerateConsoleCtrlEvent(0, 0) | Out-Null
    }
}

$deadline = (Get-Date).AddSeconds($WaitSeconds)
while ((Get-Date) -lt $deadline) {
    if (-not (Get-Process -Id $ProcessId -ErrorAction SilentlyContinue)) {
        Write-Host "$Label 已优雅退出"
        exit 0
    }
    Start-Sleep -Seconds 2
}

if ($Force) {
    Stop-Process -Id $ProcessId -Force
    Write-Host "等待超时，已强制结束 $Label"
    exit 0
}

Write-Host "等待 $WaitSeconds 秒仍未退出，未强杀，请人工处理"
exit 2
