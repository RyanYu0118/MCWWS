param(
    [Parameter(Mandatory)][string]$Stage,
    [Parameter(Mandatory)][int]$JobId
)

. (Join-Path $PSScriptRoot 'enable-window-dpi.ps1')
$form = New-Object Windows.Forms.Form
$form.SuspendLayout()
$form.AutoScaleDimensions = [Drawing.SizeF]::new(96, 96)
$form.AutoScaleMode = 'Dpi'
$form.Text = 'BlueMap 地表图 · 实时日志'
$form.Size = [Drawing.Size]::new(820, 520)
$form.StartPosition = 'CenterScreen'
$output = New-Object Windows.Forms.TextBox
$output.Multiline = $true
$output.ReadOnly = $true
$output.ScrollBars = 'Both'
$output.WordWrap = $false
$output.Dock = 'Fill'
$output.Font = [Drawing.Font]::new('Consolas', 11)
$form.Controls.Add($output)
$form.ResumeLayout($true)
$expectedStart = (Get-Process -Id $JobId -ErrorAction SilentlyContinue).StartTime
$timer = New-Object Windows.Forms.Timer
$timer.Interval = 1500
$timer.Add_Tick({
    $process = Get-Process -Id $JobId -ErrorAction SilentlyContinue
    $status = if (Test-Path -LiteralPath "$Stage/surface-manifest.json") {
        '基线生成完成'
    } elseif ($process -and $process.StartTime -eq $expectedStart) {
        '正在运行'
    } else {
        '任务已停止，未生成完成标记'
    }
    $lines = Get-Content -LiteralPath "$Stage.log" -Tail 100 -ErrorAction SilentlyContinue
    $errors = Get-Content -LiteralPath "$Stage.error.log" -Tail 15 -ErrorAction SilentlyContinue
    $text = (@($status, '') + @($lines) + @($errors)) -join [Environment]::NewLine
    if ($output.Text -ne $text) {
        $output.Text = $text
        $output.SelectionStart = $output.TextLength
        $output.ScrollToCaret()
    }
})
$form.Add_FormClosed({ $timer.Stop(); $timer.Dispose() })
$timer.Start()
[void]$form.ShowDialog()
