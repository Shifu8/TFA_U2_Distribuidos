param(
    [switch]$AjustarRelojSistema
)

$ErrorActionPreference = "Stop"
$RootDir = Resolve-Path (Join-Path $PSScriptRoot "..")

Write-Host "Iniciando red distribuida local completa en UNA sola computadora..."
Write-Host "Este modo es para probar todo localmente con nodes.json en localhost."
Write-Host ""

if ($AjustarRelojSistema) {
    Write-Host "Modo Cristian con ajuste REAL del reloj habilitado."
    Write-Host "PowerShell debe estar abierto como Administrador."
    Write-Host ""
}

$extraArgs = @()
if ($AjustarRelojSistema) {
    $extraArgs += "-AjustarRelojSistema"
}

& (Join-Path $PSScriptRoot "iniciar-nodo.ps1") 1 @extraArgs
Start-Sleep -Seconds 2
& (Join-Path $PSScriptRoot "iniciar-nodo.ps1") 2 @extraArgs
Start-Sleep -Seconds 1
& (Join-Path $PSScriptRoot "iniciar-nodo.ps1") 3 @extraArgs
Start-Sleep -Seconds 1
& (Join-Path $PSScriptRoot "iniciar-nodo.ps1") 4 @extraArgs

Write-Host ""
Write-Host "============================================================"
Write-Host "Red local iniciada."
Write-Host "Dashboard: http://localhost:5173"
Write-Host "Gateway:   http://localhost:8080"
Write-Host "Consul:    http://localhost:8500/ui"
Write-Host "Para detener: powershell -ExecutionPolicy Bypass -File scripts/detener-red.ps1"
Write-Host "============================================================"
