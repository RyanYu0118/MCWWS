# Transfers Residence ownership away from one player and strips that player's
# personal (PlayerFlags) overrides, while leaving AreaFlags and other players'
# flags untouched. Line based on purpose: Bukkit's YAML writer would reorder and
# drop the file layout if we round-tripped through it.
#
# Residence represents server owned land as OwnerUUID 00000000-...-000000000000
# (Residence.ServerLandUUID); OwnerLastKnownName should match the locale key
# Messages.server.land so the plugin stays self consistent.
#
# The server must be stopped: Residence rewrites these files on its save timer.

[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string[]]$Path,
    [Parameter(Mandatory = $true)][string]$FromUuid,
    [string]$ToUuid = '00000000-0000-0000-0000-000000000000',
    [string]$ToName = 'MCWWS',
    [switch]$DryRun
)

$ErrorActionPreference = 'Stop'
$escaped = [regex]::Escape($FromUuid)
$encoding = New-Object System.Text.UTF8Encoding($false)

foreach ($file in $Path) {
    $full = (Resolve-Path -LiteralPath $file).Path
    $lines = [System.IO.File]::ReadAllLines($full, [System.Text.Encoding]::UTF8)

    $stage1 = New-Object System.Collections.Generic.List[string]
    $droppedFlags = 0
    $ownerChanged = 0
    $nameChanged = 0
    $afterOwnerUuid = $false

    foreach ($line in $lines) {
        if ($line -match "^(\s+)$escaped\s*:\s*\d+\s*$") {
            $droppedFlags++
            $afterOwnerUuid = $false
            continue
        }
        if ($line -match "^(\s+)OwnerUUID:\s*$escaped\s*$") {
            $stage1.Add("$($Matches[1])OwnerUUID: $ToUuid")
            $ownerChanged++
            $afterOwnerUuid = $true
            continue
        }
        if ($afterOwnerUuid -and $line -match '^(\s+)OwnerLastKnownName:\s*\S') {
            $stage1.Add("$($Matches[1])OwnerLastKnownName: $ToName")
            $nameChanged++
            $afterOwnerUuid = $false
            continue
        }
        $afterOwnerUuid = $false
        $stage1.Add($line)
    }

    $final = New-Object System.Collections.Generic.List[string]
    $emptySections = 0
    for ($i = 0; $i -lt $stage1.Count; $i++) {
        $line = $stage1[$i]
        if ($line -match '^(\s+)PlayerFlags:\s*$') {
            $indent = $Matches[1].Length
            $nextIndent = -1
            if ($i + 1 -lt $stage1.Count -and $stage1[$i + 1] -match '^(\s*)\S') {
                $nextIndent = $Matches[1].Length
            }
            if ($nextIndent -le $indent) {
                $emptySections++
                continue
            }
        }
        $final.Add($line)
    }

    Write-Host "$full"
    Write-Host "  owner rewritten     : $ownerChanged"
    Write-Host "  owner name rewritten: $nameChanged"
    Write-Host "  personal flags gone : $droppedFlags"
    Write-Host "  empty PlayerFlags   : $emptySections"

    if ($DryRun) {
        Write-Host '  (dry run, file untouched)'
        continue
    }
    [System.IO.File]::WriteAllLines($full, $final, $encoding)
    Write-Host '  written'
}
