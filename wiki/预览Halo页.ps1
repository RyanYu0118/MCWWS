param(
    [Parameter(Mandatory = $true)]
    [string]$InputPath
)
$Repo = Split-Path -Parent $PSScriptRoot
& (Join-Path $Repo "tools\mcwws-halo-preview\compile.ps1") -InputPath $InputPath
