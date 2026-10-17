$ErrorActionPreference = 'Stop'
$RepoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$JdkDir = [Environment]::GetEnvironmentVariable('JAVA_HOME')
if ([string]::IsNullOrWhiteSpace($JdkDir)) { $JdkDir = 'C:\Program Files\Java\jdk-25.0.2' }
Push-Location $RepoRoot
try {
    & (Join-Path $PSScriptRoot 'build.ps1') -StageOnly
    if ($LASTEXITCODE -ne 0) { throw 'Plugin compilation failed' }
    $FaweJar = Get-ChildItem plugins -Filter 'FastAsyncWorldEdit-Paper-*.jar' | Sort-Object Name -Descending | Select-Object -First 1
    $TestOut = 'tools/mcwws-worldedit-survival/build/test-classes'
    New-Item -ItemType Directory -Force $TestOut | Out-Null
    $CompileCp = $FaweJar.FullName + ';tools/mcwws-worldedit-survival/build/classes'
    & (Join-Path $JdkDir 'bin/javac.exe') -encoding UTF-8 -cp $CompileCp -d $TestOut 'tools/mcwws-worldedit-survival/src/test/java/work/mcwws/worldedit/BufferedBrushExtentRegression.java'
    if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed' }
    # Relative ASCII paths also work with Windows Java argument-file decoding.
    $RuntimeCp = (@(('plugins/' + $FaweJar.Name), 'tools/mcwws-worldedit-survival/build/classes', $TestOut) + @(Get-ChildItem libraries -Recurse -Filter '*.jar' | ForEach-Object {
        [IO.Path]::GetRelativePath($RepoRoot, $_.FullName).Replace('\', '/')
    })) -join ';'
    $ArgFile = Join-Path $PSScriptRoot 'build/test.args'
    [IO.File]::WriteAllLines($ArgFile, @('-cp', ('"' + $RuntimeCp + '"'), 'work.mcwws.worldedit.BufferedBrushExtentRegression'), [Text.UTF8Encoding]::new($false))
    & (Join-Path $JdkDir 'bin/java.exe') '@tools/mcwws-worldedit-survival/build/test.args'
    if ($LASTEXITCODE -ne 0) { throw 'Regression checks failed' }
} finally {
    Pop-Location
}
