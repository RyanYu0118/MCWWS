$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
. (Join-Path $Root "tools/scripts/mcwws-jar-name.ps1")
$Res = Join-Path $PSScriptRoot "src/main/resources"
$McwwsJar = Get-McwwsPluginJarPaths -RepoRoot $Root -PluginName "MCWWS_AxiomSurvival" -ResourcesDir $Res `
    -NeedName "MCWWS_AxiomSurvivalClient" `
    -NeedVersionFromFabric (Join-Path $Root "tools/mcwws-axiom-survival-client/src/main/resources/fabric.mod.json")
$target = $McwwsJar.JarOut
$new = $McwwsJar.JarOutNew
$deadline = (Get-Date).AddMinutes(45)
while ((Get-Date) -lt $deadline) {
    if (-not (Test-Path -LiteralPath $new)) { break }
    try {
        Move-Item -LiteralPath $new -Destination $target -Force -ErrorAction Stop
        Remove-McwwsStaleJars -Directory (Split-Path $target) -PluginName "MCWWS_AxiomSurvival" -KeepFileName $McwwsJar.JarName
        Write-Output ("[{0}] swapped jar" -f (Get-Date -Format HH:mm:ss))
        break
    } catch {
        Start-Sleep -Seconds 3
    }
}
