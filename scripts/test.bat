@echo off
cd /d "%~dp0.."
if not exist build\classes mkdir build\classes
powershell -NoProfile -Command "Get-ChildItem src/main/java -Recurse -Filter *.java | ForEach-Object { Resolve-Path -Relative $_.FullName } | ForEach-Object { $_.Replace('\','/') } | Set-Content -Encoding ASCII build/sources.txt"
if errorlevel 1 exit /b 1
javac --release 17 -encoding UTF-8 -d build\classes @build/sources.txt
if errorlevel 1 exit /b 1
if not exist build\test-classes mkdir build\test-classes
powershell -NoProfile -Command "Get-ChildItem src/test/java -Recurse -Filter *.java | ForEach-Object { Resolve-Path -Relative $_.FullName } | ForEach-Object { $_.Replace('\','/') } | Set-Content -Encoding ASCII build/test-sources.txt"
if errorlevel 1 exit /b 1
javac --release 17 -encoding UTF-8 -cp build\classes -d build\test-classes @build/test-sources.txt
if errorlevel 1 exit /b 1
java -cp build\classes;build\test-classes app.PlannerTests
