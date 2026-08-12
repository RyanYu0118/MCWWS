# Fills in the Translated (and optionally Description) entries under
# CommandHelp.flags.SubCommands.<flag> in a Residence language file.
#
# Residence resolves a flag's display name through Flags.getName(), which
# returns Translated when set, and Flags.getFlag() matches both the enum name
# and the translation, so command input keeps working. Flag keys inside
# Save/Worlds/*.yml stay English because ResidencePermissions.setFlag()
# normalises through Flags.toString() before storing.
#
# Names live in a separate UTF-8 data file so this script stays ASCII-only:
# PowerShell 5.1 misreads non-ASCII .ps1 files that lack a BOM.
#
# Apply in game afterwards with: /res reload lang

[CmdletBinding()]
param(
    [string]$Locale = 'plugins\Residence\Language\Chinese.yml',
    [string]$Names = 'tools\scripts\residence-flag-names.tsv',
    [switch]$DryRun
)

$ErrorActionPreference = 'Stop'
$utf8 = New-Object System.Text.UTF8Encoding($false)

$map = @{}
foreach ($line in [System.IO.File]::ReadAllLines((Resolve-Path -LiteralPath $Names).Path, [System.Text.Encoding]::UTF8)) {
    if ($line -match '^\s*(#|$)') { continue }
    $cols = $line -split "`t"
    if ($cols.Count -lt 2 -or [string]::IsNullOrWhiteSpace($cols[1])) { continue }
    $map[$cols[0].Trim()] = [pscustomobject]@{
        Translated  = $cols[1].Trim()
        Description = if ($cols.Count -ge 3) { $cols[2].Trim() } else { '' }
    }
}
Write-Host "loaded $($map.Count) flag names"

$dupes = $map.Values | Group-Object -Property Translated | Where-Object { $_.Count -gt 1 }
if ($dupes) {
    throw "duplicate translations would make Flags.getFlag() ambiguous: $(($dupes | ForEach-Object { $_.Name }) -join ', ')"
}
foreach ($key in $map.Keys) {
    $clash = $map.Keys | Where-Object { $_ -ne $key -and $_ -eq $map[$key].Translated }
    if ($clash) { throw "translation of $key collides with flag name $clash" }
}

$localePath = (Resolve-Path -LiteralPath $Locale).Path
$lines = [System.IO.File]::ReadAllLines($localePath, [System.Text.Encoding]::UTF8)

$out = New-Object System.Collections.Generic.List[string]
$currentFlag = $null
$translatedHits = 0
$descriptionHits = 0
$seen = New-Object System.Collections.Generic.HashSet[string]
$skipWrapped = $false

for ($i = 0; $i -lt $lines.Count; $i++) {
    $line = $lines[$i]

    if ($skipWrapped) {
        # drop continuation lines of a folded scalar we just replaced
        if ($line -match '^\s{16,}[^-\s]') { continue }
        $skipWrapped = $false
    }

    if ($line -match '^            ([A-Za-z0-9_]+):\s*$') {
        $currentFlag = $Matches[1]
        $out.Add($line)
        continue
    }
    if ($line -match '^\s{0,12}\S' -and $line -notmatch '^\s{14,}') {
        if ($line -notmatch '^            ') { $currentFlag = $null }
    }

    if ($currentFlag -and $map.ContainsKey($currentFlag)) {
        $entry = $map[$currentFlag]
        if ($line -match '^(\s{14})Translated:\s*') {
            $out.Add("$($Matches[1])Translated: $($entry.Translated)")
            $translatedHits++
            [void]$seen.Add($currentFlag)
            continue
        }
        if ($entry.Description -and $line -match '^(\s{14})Description:\s*') {
            $out.Add("$($Matches[1])Description: $($entry.Description)")
            $descriptionHits++
            $skipWrapped = $true
            continue
        }
    }

    $out.Add($line)
}

Write-Host "  Translated rewritten : $translatedHits"
Write-Host "  Description rewritten: $descriptionHits"

$missing = $map.Keys | Where-Object { -not $seen.Contains($_) }
if ($missing) { Write-Host "  no Translated node found for: $($missing -join ', ')" }

if ($DryRun) {
    Write-Host '  (dry run, file untouched)'
    return
}
[System.IO.File]::WriteAllLines($localePath, $out, $utf8)
Write-Host "  written: $localePath"
