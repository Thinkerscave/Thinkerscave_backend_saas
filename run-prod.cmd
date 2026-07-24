@echo off
REM ThinkersCave backend — production profile (Windows)
REM Stops anything on 8080, then starts with UTC timezone.

cd /d "%~dp0"

for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":8080" ^| findstr "LISTENING"') do (
  echo Stopping PID %%p on port 8080...
  taskkill /F /PID %%p >nul 2>&1
)

set JAVA_TOOL_OPTIONS=-Duser.timezone=UTC
set UPLOAD_BASE_DIR=uploads
REM Clear legacy MySQL env leftovers if present in this shell
set DB_USERNAME=
set DB_PASSWORD=
set SPRING_PROFILES_ACTIVE=prod

echo Starting with profile=prod , timezone=UTC ...
call mvnw.cmd -DskipTests spring-boot:run -Dspring-boot.run.profiles=prod -Dspring-boot.run.jvmArguments="-Duser.timezone=UTC"
