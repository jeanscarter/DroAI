@echo off
setlocal enabledelayedexpansion
title Reparar / Actualizar DroAI en este Equipo
color 0b

echo ======================================================================
echo             ACTUALIZACION MANUAL / REPARACION DE DROAI
echo ======================================================================
echo.

:: 1. Cerrar cualquier instancia previa de DroAI
echo [1/4] Cerrando instancias activas de DroAI...
taskkill /f /im DroAI.exe >nul 2>&1
timeout /t 1 /nobreak >nul

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

echo   - Copiando DroAI.exe...
copy /Y "%ORIGEN%target\DroAI.exe" "%DESTINO%\target\DroAI.exe" >nul
if errorlevel 1 (
    color 0c
    echo [ERROR] No se pudo copiar DroAI.exe. Verifique permisos o cierre DroAI si esta abierto.
    pause
    exit /b 1
)

if exist "%ORIGEN%target\dro-ai-1.0-SNAPSHOT.jar" (
    echo   - Copiando dro-ai-1.0-SNAPSHOT.jar...
    copy /Y "%ORIGEN%target\dro-ai-1.0-SNAPSHOT.jar" "%DESTINO%\target\dro-ai-1.0-SNAPSHOT.jar" >nul
)

if exist "%DESTINO%\DroAI.exe" (
    copy /Y "%ORIGEN%target\DroAI.exe" "%DESTINO%\DroAI.exe" >nul
)

if exist "%ORIGEN%version.txt" (
    echo   - Actualizando version.txt...
    copy /Y "%ORIGEN%version.txt" "%DESTINO%\version.txt" >nul
    copy /Y "%ORIGEN%version.txt" "%DESTINO%\target\version.txt" >nul
)

if exist "%ORIGEN%Logo.ico" (
    copy /Y "%ORIGEN%Logo.ico" "%DESTINO%\Logo.ico" >nul
)

:: 4. Recrear acceso directo en el Escritorio si existe el script
if exist "%ORIGEN%Crear_Acceso_Directo.ps1" (
    echo   - Verificando acceso directo en el Escritorio...
    powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%ORIGEN%Crear_Acceso_Directo.ps1" >nul 2>&1
)

echo.
echo ======================================================================
echo  [EXITO] DroAI ha sido actualizado y reparado exitosamente.
echo ======================================================================
echo.
echo Iniciando DroAI en 2 segundos...
timeout /t 2 /nobreak >nul

cd /d "%DESTINO%\target"
start "" "%DESTINO%\target\DroAI.exe"

exit /b 0
