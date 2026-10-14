@echo off
setlocal
cd /d "%~dp0"
where javac >nul 2>nul || (echo JDK not found: put javac on your PATH first. & pause & exit /b 1)
if not exist lib\flatlaf-3.7.2.jar (echo FlatLaf jar missing from the lib folder. & pause & exit /b 1)
if not exist out mkdir out
javac -encoding UTF-8 -cp "lib\*" -d out src\*.java
if errorlevel 1 (echo Build failed. & pause & exit /b 1)
start "" javaw -cp "out;lib\*" ColorDropletApp
endlocal
