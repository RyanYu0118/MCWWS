# 自研 MCWWS jar 文件名：名称-版本.jar；有配套时再加 -need配套名+配套版本
function Get-McwwsYamlVersion {
    param([Parameter(Mandatory = $true)][string]$PluginYml)
    $line = Select-String -LiteralPath $PluginYml -Pattern '^\s*version:\s*' | Select-Object -First 1
    if (-not $line) { throw "No version: in $PluginYml" }
    return (($line.Line -replace '^\s*version:\s*', '') -replace "^['`"]|['`"]$", "").Trim()
}

function Get-McwwsFabricVersion {
    param([Parameter(Mandatory = $true)][string]$FabricJson)
    $json = Get-Content -LiteralPath $FabricJson -Raw -Encoding UTF8 | ConvertFrom-Json
    if (-not $json.version) { throw "No version in $FabricJson" }
    return [string]$json.version
}

function Get-McwwsJarFileName {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][string]$Version,
        [string]$NeedName,
        [string]$NeedVersion
    )
    $file = "$Name-$Version"
    if (-not [string]::IsNullOrWhiteSpace($NeedName) -and -not [string]::IsNullOrWhiteSpace($NeedVersion)) {
        $file += "-need$NeedName+$NeedVersion"
    }
    return "$file.jar"
}

function Get-McwwsPluginJarPaths {
    param(
        [Parameter(Mandatory = $true)][string]$RepoRoot,
        [Parameter(Mandatory = $true)][string]$PluginName,
        [Parameter(Mandatory = $true)][string]$ResourcesDir,
        [string]$NeedName,
        [string]$NeedVersion,
        [string]$NeedVersionFromPluginYml,
        [string]$NeedVersionFromFabric
    )
    $version = Get-McwwsYamlVersion (Join-Path $ResourcesDir "plugin.yml")
    if ($NeedVersionFromPluginYml) {
        $NeedVersion = Get-McwwsYamlVersion $NeedVersionFromPluginYml
    }
    if ($NeedVersionFromFabric) {
        $NeedVersion = Get-McwwsFabricVersion $NeedVersionFromFabric
    }
    $jarName = Get-McwwsJarFileName -Name $PluginName -Version $version -NeedName $NeedName -NeedVersion $NeedVersion
    $plugins = Join-Path $RepoRoot "plugins"
    return [pscustomobject]@{
        Version   = $version
        JarName   = $jarName
        JarOut    = Join-Path $plugins $jarName
        JarOutNew = Join-Path $plugins ($jarName + ".new")
    }
}

function Test-McwwsJarBelongsToPlugin {
    param([string]$FileName, [string]$PluginName)
    $n = $FileName
    if ($n.EndsWith(".new")) { $n = $n.Substring(0, $n.Length - 4) }
    if (-not $n.EndsWith(".jar")) { return $false }
    $n = $n.Substring(0, $n.Length - 4)
    return ($n -eq $PluginName) -or $n.StartsWith("$PluginName-")
}

function Remove-McwwsStaleJars {
    param(
        [Parameter(Mandatory = $true)][string]$Directory,
        [Parameter(Mandatory = $true)][string]$PluginName,
        [Parameter(Mandatory = $true)][string]$KeepFileName
    )
    if (-not (Test-Path -LiteralPath $Directory)) { return }
    Get-ChildItem -LiteralPath $Directory -File | Where-Object {
        (Test-McwwsJarBelongsToPlugin $_.Name $PluginName) -and ($_.Name -ne $KeepFileName)
    } | ForEach-Object {
        try {
            Remove-Item -LiteralPath $_.FullName -Force
            Write-Host "Removed stale $($_.Name)"
        } catch {
            Write-Host "Stale jar still in use: $($_.Name)"
        }
    }
}

function Get-McwwsClientJarPaths {
    param(
        [Parameter(Mandatory = $true)][string]$ProjectRoot,
        [Parameter(Mandatory = $true)][string]$ModName,
        [Parameter(Mandatory = $true)][string]$ResourcesDir,
        [string]$NeedName,
        [string]$NeedVersion,
        [string]$NeedVersionFromPluginYml
    )
    $version = Get-McwwsFabricVersion (Join-Path $ResourcesDir "fabric.mod.json")
    if ($NeedVersionFromPluginYml) {
        $NeedVersion = Get-McwwsYamlVersion $NeedVersionFromPluginYml
    }
    $jarName = Get-McwwsJarFileName -Name $ModName -Version $version -NeedName $NeedName -NeedVersion $NeedVersion
    $buildDir = Join-Path $ProjectRoot "build"
    return [pscustomobject]@{
        Version  = $version
        JarName  = $jarName
        JarOut   = Join-Path $buildDir $jarName
        BuildDir = $buildDir
    }
}

function Publish-McwwsClientJar {
    param(
        [Parameter(Mandatory = $true)][string]$JarExe,
        [Parameter(Mandatory = $true)][string]$ClassesDir,
        [Parameter(Mandatory = $true)]$JarPaths,
        [Parameter(Mandatory = $true)][string]$ModName,
        [Parameter(Mandatory = $true)][string]$ModsDir,
        [string[]]$ExtraModsDirs = @()
    )
    $jarOut = $JarPaths.JarOut
    New-Item -ItemType Directory -Path $JarPaths.BuildDir -Force | Out-Null
    if (Test-Path -LiteralPath $jarOut) { Remove-Item -LiteralPath $jarOut -Force }
    & $JarExe cf $jarOut -C $ClassesDir .
    Write-Host "Built $jarOut"
    Remove-McwwsStaleJars -Directory $JarPaths.BuildDir -PluginName $ModName -KeepFileName $JarPaths.JarName

    $targets = @($ModsDir) + @($ExtraModsDirs | Where-Object { $_ })
    $seen = @{}
    foreach ($dir in $targets) {
        if ([string]::IsNullOrWhiteSpace($dir)) { continue }
        $key = [IO.Path]::GetFullPath($dir)
        if ($seen.ContainsKey($key)) { continue }
        $seen[$key] = $true
        if (-not (Test-Path -LiteralPath $dir)) { continue }
        $dest = Join-Path $dir $JarPaths.JarName
        Copy-Item -LiteralPath $jarOut -Destination $dest -Force
        Write-Host "Deployed $dest"
        Remove-McwwsStaleJars -Directory $dir -PluginName $ModName -KeepFileName $JarPaths.JarName
    }
}

function Publish-McwwsPluginJar {
    param(
        [Parameter(Mandatory = $true)][string]$JarExe,
        [Parameter(Mandatory = $true)][string]$ClassesDir,
        [Parameter(Mandatory = $true)]$JarPaths,
        [Parameter(Mandatory = $true)][string]$PluginName
    )
    $jarOut = $JarPaths.JarOut
    $jarOutNew = $JarPaths.JarOutNew
    if (Test-Path -LiteralPath $jarOutNew) { Remove-Item -LiteralPath $jarOutNew -Force }
    & $JarExe cf $jarOutNew -C $ClassesDir .
    $installed = $false
    if (Test-Path -LiteralPath $jarOut) {
        try {
            Remove-Item -LiteralPath $jarOut -Force
            Move-Item -LiteralPath $jarOutNew -Destination $jarOut
            $installed = $true
        } catch {
            Write-Host "Built $jarOutNew (原 jar 被占用，停服后替换 $($JarPaths.JarName))"
        }
    } else {
        Move-Item -LiteralPath $jarOutNew -Destination $jarOut
        $installed = $true
    }
    if ($installed) {
        Write-Host "Built $jarOut"
        Remove-McwwsStaleJars -Directory (Split-Path $jarOut) -PluginName $PluginName -KeepFileName $JarPaths.JarName
    }
}
