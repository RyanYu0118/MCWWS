$target = "d:\Minecraft\服务器\26.2\plugins\MCWWS_AxiomSurvival.jar"
$new = "$target.new"
$deadline = (Get-Date).AddMinutes(45)
while ((Get-Date) -lt $deadline) {
    if (-not (Test-Path -LiteralPath $new)) { break }
    try {
        Move-Item -LiteralPath $new -Destination $target -Force -ErrorAction Stop
        Write-Output ("[{0}] swapped jar" -f (Get-Date -Format HH:mm:ss))
        break
    } catch {
        Start-Sleep -Seconds 3
    }
}
