@echo off
setlocal EnableExtensions EnableDelayedExpansion
set "JAVA_HOME=C:\Program Files\Java\jdk-25.0.2"
set "ROOT=%~dp0"
set "LIB=%ROOT%lib"
set "EX=%LIB%\extracted"
set "SRC=%ROOT%src\main\java"
set "OUT=%ROOT%build\classes"
set "RES=%ROOT%src\main\resources"
set "JAROUT=%ROOT%build\MCWWS_AxiomSurvivalClient.jar"
set "MCLIB=D:\Minecraft\游戏主体\.minecraft\libraries"
set "DEPLOY=D:\Minecraft\游戏主体\.minecraft\mods\MCWWS_AxiomSurvivalClient.jar"

if exist "%OUT%" rmdir /s /q "%OUT%"
mkdir "%OUT%" 2>nul
mkdir "%ROOT%build" 2>nul

del "%ROOT%build\cp.txt" 2>nul
for %%f in ("%LIB%\*.jar") do >>"%ROOT%build\cp.txt" echo %%~ff
for /r "%EX%" %%f in (*.jar) do >>"%ROOT%build\cp.txt" echo %%~ff
for /r "%MCLIB%" %%f in (*.jar) do >>"%ROOT%build\cp.txt" echo %%~ff

set "CP="
for /f "usebackq delims=" %%i in ("%ROOT%build\cp.txt") do (
  if defined CP (set "CP=!CP!;%%i") else set "CP=%%i"
)

"%JAVA_HOME%\bin\javac.exe" -encoding UTF-8 -cp "!CP!" -d "%OUT%" ^
  "%SRC%\work\mcwws\axiomsurvival\client\McwwsAxiomSurvivalClientMod.java" ^
  "%SRC%\work\mcwws\axiomsurvival\client\SurvivalEditorController.java" ^
  "%SRC%\work\mcwws\axiomsurvival\client\SurvivalEditorNetworking.java" ^
  "%SRC%\work\mcwws\axiomsurvival\client\mixin\AxiomClientSpectatorPermissionMixin.java" ^
  "%SRC%\work\mcwws\axiomsurvival\client\mixin\ServerIntegrationChangeGameModeMixin.java" ^
  "%SRC%\work\mcwws\axiomsurvival\client\mixin\ServerIntegrationSendGamemodeMixin.java" ^
  "%SRC%\work\mcwws\axiomsurvival\client\mixin\EditorUIDisableMixin.java"
if errorlevel 1 exit /b 1

xcopy /E /I /Y "%RES%\*" "%OUT%\" >nul
if exist "%JAROUT%" del /f /q "%JAROUT%"
"%JAVA_HOME%\bin\jar.exe" cf "%JAROUT%" -C "%OUT%" .
copy /Y "%JAROUT%" "%DEPLOY%" >nul
echo Built %JAROUT%
echo Deployed %DEPLOY%
