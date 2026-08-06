@echo off
cd /d "%~dp0"
title MC Server 26.2
echo Starting Paper 26.2...

REM Use Aliyun Maven mirror to speed up SpigotLibraryLoader downloads in China
set PAPER_DEFAULT_CENTRAL_REPOSITORY=https://maven.aliyun.com/repository/central

REM Auto-pick the newest paper-26.2-*.jar in this folder
set "PAPER_JAR="
for /f "delims=" %%F in ('dir /b /a:-d /o:-d "paper-26.2-*.jar" 2^>nul') do (
  set "PAPER_JAR=%%F"
  goto :found
)

:found
if not defined PAPER_JAR (
  echo ERROR: No paper-26.2-*.jar found in:
  echo   %CD%
  echo Put a Paper build like paper-26.2-98.jar here and try again.
  pause
  exit /b 1
)

echo Using jar: %PAPER_JAR%
java -Xms1G -Xmx16G -Dorg.bukkit.plugin.java.LibraryLoader.centralURL=https://maven.aliyun.com/repository/central -jar "%PAPER_JAR%" nogui
pause
