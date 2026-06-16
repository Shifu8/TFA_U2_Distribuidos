param()

$ErrorActionPreference = "Stop"
$RootDir = Resolve-Path (Join-Path $PSScriptRoot "..")
$LogDir = Join-Path $RootDir "logs"

if (-not (Test-Path $LogDir)) {
    Write-Host "No existe directorio logs/. No hay procesos registrados por detener."
    exit 0
}

Get-ChildItem -Path $LogDir -Filter "*.pid" -ErrorAction SilentlyContinue | ForEach-Object {
    $name = [System.IO.Path]::GetFileNameWithoutExtension($_.Name)
    $pidValue = (Get-Content $_.FullName -ErrorAction SilentlyContinue | Select-Object -First 1)

    if ($pidValue -and ($pidValue -as [int])) {
        $process = Get-Process -Id ([int]$pidValue) -ErrorAction SilentlyContinue
        if ($process) {
            Write-Host "Deteniendo $name PID=$pidValue"
            Stop-Process -Id ([int]$pidValue) -Force -ErrorAction SilentlyContinue
        } else {
            Write-Host "$name ya no esta activo"
        }
    }

    Remove-Item $_.FullName -Force -ErrorAction SilentlyContinue
}

Write-Host "Procesos detenidos."
