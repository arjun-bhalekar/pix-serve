@echo off
setlocal

echo ================================
echo PixServe - Full Build Process
echo ================================

REM ================================
REM Set backend port here
REM ================================
set BASE_URL=

REM ================================
REM Step 0: Prepare React env file
REM ================================
echo.
echo [0/4] Setting backend port for React UI...
cd pix-ui

REM Copy template env file to .env
copy /Y .env.template .env >nul

REM Replace PORT_PLACEHOLDER with actual port
powershell -Command "(Get-Content .env) -replace 'PLACEHOLDER','%BASE_URL%' | Set-Content .env"

cd ..

REM ================================
REM Step 1: Build React UI
REM ================================
echo.
echo [1/4] Building React UI...
cd pix-ui
call npm install
call npm run build
if errorlevel 1 (
    echo React build failed!
    exit /b 1
)
cd ..

REM ================================
REM Step 2: Copy dist -> Spring Boot static
REM ================================
echo.
echo [2/4] Copying UI build to Spring Boot static...
rmdir /s /q pix-service\src\main\resources\static
xcopy pix-ui\dist pix-service\src\main\resources\static /E /I /Y /H

REM ================================
REM Step 3: Package Spring Boot JAR
REM ================================
echo.
echo [3/4] Building Spring Boot JAR...
cd pix-service
call mvn clean package -DskipTests
if errorlevel 1 (
    echo Maven build failed!
    exit /b 1
)
cd ..

REM ================================
REM Step 4: Move JAR to build folder
REM ================================
if not exist build (
    mkdir build
)

echo.
echo Moving JAR to build folder...
for %%f in (pix-service\target\*.jar) do (
    move /Y "%%f" build\
)

echo ================================
echo Build finished. JAR available in /build folder.
echo ================================
pause
