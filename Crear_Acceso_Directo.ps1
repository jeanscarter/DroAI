$WshShell = New-Object -ComObject WScript.Shell
$Desktop = [System.Environment]::GetFolderPath('Desktop')
$ShortcutPath = Join-Path $Desktop "DroAI.lnk"
$Shortcut = $WshShell.CreateShortcut($ShortcutPath)

$ProjectDir = (Get-Location).Path
$ExeTarget = Join-Path $ProjectDir "target\DroAI.exe"
$ExeRoot = Join-Path $ProjectDir "DroAI.exe"
$JarTarget = Join-Path $ProjectDir "target\dro-ai-1.0-SNAPSHOT.jar"
$IconFile = Join-Path $ProjectDir "Logo.ico"

if (Test-Path $ExeTarget) {
    $Shortcut.TargetPath = $ExeTarget
    $Shortcut.WorkingDirectory = (Join-Path $ProjectDir "target")
    $Shortcut.IconLocation = $IconFile
} elseif (Test-Path $ExeRoot) {
    $Shortcut.TargetPath = $ExeRoot
    $Shortcut.WorkingDirectory = $ProjectDir
    $Shortcut.IconLocation = $IconFile
} elseif (Test-Path $JarTarget) {
    $Shortcut.TargetPath = "javaw.exe"
    $Shortcut.Arguments = "-jar `"$JarTarget`""
    $Shortcut.WorkingDirectory = $ProjectDir
    $Shortcut.IconLocation = $IconFile
} else {
    Write-Warning "No se encontro DroAI.exe ni dro-ai-1.0-SNAPSHOT.jar. Compile primero el proyecto con 'mvn package'."
    exit 1
}

$Shortcut.Description = "DroAI - Sistema de Gestion Integral"
$Shortcut.Save()

Write-Host "=======================================================" -ForegroundColor Cyan
Write-Host " Acceso directo creado exitosamente en el Escritorio: " -ForegroundColor Green
Write-Host " $ShortcutPath" -ForegroundColor Yellow
Write-Host "=======================================================" -ForegroundColor Cyan
