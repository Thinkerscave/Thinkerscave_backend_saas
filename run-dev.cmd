@echo off
REM ThinkersCave backend - dev profile (Windows). Launched by scripts\dev-start.ps1.
REM Stops anything on 8080, then starts with UTC timezone.

cd /d "%~dp0"

for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":8080" ^| findstr "LISTENING"') do (
  echo Stopping PID %%p on port 8080...
  taskkill /F /PID %%p >nul 2>&1
)

set JAVA_TOOL_OPTIONS=-Duser.timezone=UTC

echo Starting with profile=dev, timezone=UTC ...
call mvnw.cmd -DskipTests spring-boot:run -Dspring-boot.run.profiles=dev -Dspring-boot.run.jvmArguments="-Duser.timezone=UTC"
