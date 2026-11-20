param([string]$Stage, [int]$JobId, [int]$Total)

. (Join-Path $PSScriptRoot 'enable-window-dpi.ps1')
$form = New-Object Windows.Forms.Form
$form.SuspendLayout()
$form.AutoScaleDimensions = [Drawing.SizeF]::new(96, 96)
$form.AutoScaleMode = 'Dpi'
$form.Text = 'BlueMap 地表图实时进度'
$form.ClientSize = [Drawing.Size]::new(460, 120)
$form.StartPosition = 'CenterScreen'
$label = New-Object Windows.Forms.Label
$label.SetBounds(16, 14, 428, 48)
$label.Anchor = 'Top, Left, Right'
$label.Font = [Drawing.Font]::new('Microsoft YaHei', 12)
$bar = New-Object Windows.Forms.ProgressBar
$bar.SetBounds(16, 74, 428, 24)
$bar.Anchor = 'Top, Left, Right'
$bar.Maximum = $Total
$form.Controls.Add($label)
$form.Controls.Add($bar)
$form.ResumeLayout($true)
$checkpoint = Join-Path $Stage 'work/surface-relight.checkpoint'
$manifest = Join-Path $Stage 'surface-manifest.json'
$refresh = {
    $count = 0
    if (Test-Path -LiteralPath $manifest) {
        $count = $Total
        $status = '基线已完成'
    } else {
        if ([int]::TryParse([string](Get-Content -LiteralPath $checkpoint -ErrorAction SilentlyContinue), [ref]$count)) {
            $count = [Math]::Max(0, [Math]::Min($count, $Total))
        } else {
            $count = $bar.Value
        }
        $status = if (!(Get-Process -Id $JobId -ErrorAction SilentlyContinue)) {
            '任务已停止 · 未完成'
        } elseif ($count -eq $Total) {
            $scan = Get-Content -LiteralPath "$Stage.log" -Tail 12 -ErrorAction SilentlyContinue |
                Select-String '\[(\d+)/(\d+)\] Scanned regions' | Select-Object -Last 1
            if ($scan) {
                $match = $scan.Matches[0]
                $count = [int]($Total * [int]$match.Groups[1].Value / [int]$match.Groups[2].Value)
                $status = "核对区域 $($match.Groups[1].Value)/$($match.Groups[2].Value)"
            } else {
                $status = '光照完成 · 正在整理基线'
            }
        } else {
            '光照重算 · 已保存'
        }
    }
    $bar.Value = $count
    $label.Text = '{0}：{1:P1}（当前阶段）' -f $status, ($count / $Total)
}
$timer = New-Object Windows.Forms.Timer
$timer.Interval = 1000
$timer.Add_Tick($refresh)
$form.Add_Shown($refresh)
$form.Add_FormClosed({ $timer.Stop(); $timer.Dispose() })
$timer.Start()
[void]$form.ShowDialog()
