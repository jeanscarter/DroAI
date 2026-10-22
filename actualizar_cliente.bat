@echo off
setlocal enabledelayedexpansion
title Reparar / Actualizar DroAI en este Equipo
color 0b

echo ======================================================================
echo             ACTUALIZACION MANUAL / REPARACION DE DROAI
echo ======================================================================
echo.

:: 1. Cerrar cualquier instancia previa de DroAI y Java
echo [1/4] Cerrando instancias activas de DroAI...
taskkill /f /im DroAI.exe >nul 2>&1
taskkill /f /im javaw.exe >nul 2>&1
ping -n 3 127.0.0.1 >nul

:: 2. Determinar directorios de origen y destino
set "ORIGEN=%~dp0"
set "DESTINO=%USERPROFILE%\Documents\dro-ai"

echo [2/4] Verificando componentes desde el servidor:
echo       %ORIGEN%
echo.

if not exist "%ORIGEN%target\DroAI.exe" (
    color 0c
    echo [ERROR] No se encontro target\DroAI.exe en la carpeta del servidor.
    echo Asegurese de haber compilado y publicado el proyecto en el servidor primero.
    echo.
    pause
    exit /b 1
)

:: 3. Copiar componentes al directorio local del usuario
echo [3/4] Instalando componentes actualizados en:
echo       %DESTINO%
echo.

if not exist "%DESTINO%\target" mkdir "%DESTINO%\target"

echo   - Copiando target\DroAI.exe...
copy /Y "%ORIGEN%target\DroAI.exe" "%DESTINO%\target\DroAI.exe"
if errorlevel 1 (
    color 0c
    echo.
    echo [ERROR] No se pudo copiar DroAI.exe. El archivo puede estar bloqueado.
    echo Por favor cierre cualquier ventana de DroAI o reinicie el equipo y reintente.
    echo.
    pause
    exit /b 1
)

if exist "%ORIGEN%target\dro-ai-1.0-SNAPSHOT.jar" (
    echo   - Copiando dro-ai-1.0-SNAPSHOT.jar...
    copy /Y "%ORIGEN%target\dro-ai-1.0-SNAPSHOT.jar" "%DESTINO%\target\dro-ai-1.0-SNAPSHOT.jar"
)

if exist "%DESTINO%\DroAI.exe" (
    echo   - Actualizando DroAI.exe en la raiz...
    copy /Y "%ORIGEN%target\DroAI.exe" "%DESTINO%\DroAI.exe"
)

if exist "%ORIGEN%db.properties" (
    echo   - Copiando configuracion de base de datos (db.properties)...
    copy /Y "%ORIGEN%db.properties" "%DESTINO%\db.properties"
    copy /Y "%ORIGEN%db.properties" "%DESTINO%\target\db.properties"
)

if exist "%ORIGEN%version.txt" (
    echo   - Actualizando marcas de version...
    copy /Y "%ORIGEN%version.txt" "%DESTINO%\version.txt"
    copy /Y "%ORIGEN%version.txt" "%DESTINO%\target\version.txt"
)

if exist "%ORIGEN%Logo.ico" (
    copy /Y "%ORIGEN%Logo.ico" "%DESTINO%\Logo.ico"
)

:: 4. Recrear acceso directo en el Escritorio si existe el script
if exist "%ORIGEN%Crear_Acceso_Directo.ps1" (
    echo   - Verificando acceso directo en el Escritorio...
    powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "Set-Location '%DESTINO%'; & '%ORIGEN%Crear_Acceso_Directo.ps1'" >nul 2>&1
)

echo.
echo ======================================================================
echo  [EXITO] DroAI ha sido actualizado y reparado exitosamente.
echo ======================================================================
echo.
echo Presione cualquier tecla para iniciar DroAI...
pause >nul

cd /d "%DESTINO%\target"
start "" "%DESTINO%\target\DroAI.exe"

exit /b 0
