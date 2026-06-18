param(
    [Parameter(Mandatory = $true, Position = 0)]
    [int]$NodeId,
    [switch]$AjustarRelojSistema
)

$ErrorActionPreference = "Stop"
$RootDir = Resolve-Path (Join-Path $PSScriptRoot "..")
$LogDir = Join-Path $RootDir "logs"
$NodesFile = Join-Path $RootDir "nodes.json"
$AjustarRelojSistemaValor = if ($AjustarRelojSistema) { "true" } elseif ($env:AJUSTAR_RELOJ_SISTEMA) { $env:AJUSTAR_RELOJ_SISTEMA } else { "false" }
$TimeoutHeartbeatMs = if ($env:TIMEOUT_HEARTBEAT_MS) { $env:TIMEOUT_HEARTBEAT_MS } else { "15000" }

New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

function Linea {
    Write-Host "============================================================"
}

function Get-RequiredCommand {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Names,
        [Parameter(Mandatory = $true)]
        [string]$InstallHint
    )

    foreach ($name in $Names) {
        $command = Get-Command $name -ErrorAction SilentlyContinue
        if ($command) {
            return $command.Source
        }
    }

    throw "No se encontro $($Names -join ', '). $InstallHint"
}

function Quote-Arg {
    param([string]$Value)
    if ($Value -match '\s') {
        return '"' + ($Value -replace '"', '\"') + '"'
    }
    return $Value
}

function Test-Port {
    param(
        [string]$HostName,
        [int]$Port
    )

    $client = [System.Net.Sockets.TcpClient]::new()
    try {
        $task = $client.ConnectAsync($HostName, $Port)
        if (-not $task.Wait(700)) {
            return $false
        }
        return $client.Connected
    } catch {
        return $false
    } finally {
        $client.Dispose()
    }
}

function Show-StartupLog {
    param([string]$Name)

    $errLog = Join-Path $LogDir "$Name.err.log"
    $outLog = Join-Path $LogDir "$Name.out.log"
    Write-Host "Ultimas lineas de ${Name}:"

    if ((Test-Path $errLog) -and (Get-Item $errLog).Length -gt 0) {
        Get-Content $errLog -Tail 30
    } elseif ((Test-Path $outLog) -and (Get-Item $outLog).Length -gt 0) {
        Get-Content $outLog -Tail 30
    } else {
        Write-Host "No se genero informacion en los logs."
    }
}

function Wait-ForPort {
    param(
        [string]$Name,
        [string]$HostName,
        [int]$Port,
        [int]$TimeoutSeconds = 30
    )

    Write-Host "Esperando $Name en ${HostName}:$Port..."
    for ($i = 0; $i -lt $TimeoutSeconds; $i++) {
        if (Test-Port -HostName $HostName -Port $Port) {
            Write-Host "$Name listo en ${HostName}:$Port"
            return
        }
        Start-Sleep -Seconds 1
    }

    Write-Host "ERROR: $Name no abrio el puerto $Port despues de ${TimeoutSeconds}s."
    Show-StartupLog -Name $Name
    exit 1
}

function Start-Background {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name,
        [Parameter(Mandatory = $true)]
        [string]$FilePath,
        [string[]]$ArgumentList = @(),
        [hashtable]$Environment = @{},
        [string]$WorkingDirectory = $RootDir
    )

    $pidFile = Join-Path $LogDir "$Name.pid"
    if (Test-Path $pidFile) {
        $oldPid = (Get-Content $pidFile -ErrorAction SilentlyContinue | Select-Object -First 1)
        if ($oldPid -and (Get-Process -Id ([int]$oldPid) -ErrorAction SilentlyContinue)) {
            Write-Host "$Name ya esta ejecutandose con PID $oldPid"
            return
        }
        Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
    }

    $outLog = Join-Path $LogDir "$Name.out.log"
    $errLog = Join-Path $LogDir "$Name.err.log"
    $envBackup = @{}

    foreach ($key in $Environment.Keys) {
        $envBackup[$key] = [Environment]::GetEnvironmentVariable($key, "Process")
        [Environment]::SetEnvironmentVariable($key, [string]$Environment[$key], "Process")
    }

    try {
        Write-Host "Iniciando $Name..."
        $quotedArgs = $ArgumentList | ForEach-Object { Quote-Arg $_ }
        $process = Start-Process `
            -FilePath $FilePath `
            -ArgumentList $quotedArgs `
            -WorkingDirectory $WorkingDirectory `
            -RedirectStandardOutput $outLog `
            -RedirectStandardError $errLog `
            -WindowStyle Hidden `
            -PassThru
        Set-Content -Path $pidFile -Value $process.Id
    } finally {
        foreach ($key in $Environment.Keys) {
            [Environment]::SetEnvironmentVariable($key, $envBackup[$key], "Process")
        }
    }

    Start-Sleep -Seconds 1
    $running = Get-Process -Id $process.Id -ErrorAction SilentlyContinue
    if (-not $running) {
        Write-Host "ERROR: $Name termino durante el arranque."
        Show-StartupLog -Name $Name
        Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
        exit 1
    }
}

function Test-Administrador {
    $identity = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = [Security.Principal.WindowsPrincipal]::new($identity)
    return $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

function Test-BuildRequired {
    param(
        [string]$HospitalJar,
        [string]$GatewayJar
    )

    if (-not (Test-Path $HospitalJar) -or -not (Test-Path $GatewayJar)) {
        return $true
    }

    $sourceFiles = @()
    $sourceFiles += Get-Item (Join-Path $RootDir "pom.xml")
    $sourceFiles += Get-ChildItem (Join-Path $RootDir "backend") -Recurse -File |
        Where-Object { $_.Extension -in ".java", ".yml", ".xml" }

    $latestSource = $sourceFiles | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    $oldestJar = @(
        Get-Item $HospitalJar
        Get-Item $GatewayJar
    ) |
        Sort-Object LastWriteTime |
        Select-Object -First 1

    return $latestSource.LastWriteTime -gt $oldestJar.LastWriteTime
}

if (-not (Test-Path $NodesFile)) {
    throw "No existe nodes.json en $RootDir"
}

$data = Get-Content $NodesFile -Raw | ConvertFrom-Json
$nodes = if ($data -is [array]) { $data } else { $data.nodos }
$node = $nodes | Where-Object { [int]$_.id -eq $NodeId } | Select-Object -First 1
$node1 = $nodes | Where-Object { [int]$_.id -eq 1 } | Select-Object -First 1

if (-not $node) {
    throw "No existe el nodo $NodeId en nodes.json"
}
if (-not $node1) {
    throw "No existe el nodo 1 en nodes.json"
}

$NodeHost = if ($node.host) { [string]$node.host } else { "localhost" }
$NodeTcpPort = if ($node.tcpPort) { [int]$node.tcpPort } elseif ($node.port) { [int]$node.port } else { 9000 + $NodeId }
$NodeHttpPort = if ($node.httpPort) { [int]$node.httpPort } elseif ($node.apiPort) { [int]$node.apiPort } else { 8080 + $NodeId }
$ConsulHost = if ($env:CONSUL_HOST) { $env:CONSUL_HOST } else { "localhost" }
$EsPrincipal = $NodeId -eq 1

Linea
Write-Host "Arranque de Red de Hospitales - Nodo $NodeId"
Linea
Write-Host "Se iniciara localmente: Consul + API Gateway + Frontend + Nodo $NodeId."
Write-Host "Host del nodo: $NodeHost"
Write-Host "HTTP del nodo: $NodeHttpPort"
Write-Host "TCP del nodo:  $NodeTcpPort"
Write-Host "Consul host:   $ConsulHost"
Write-Host "Ajuste reloj:  $AjustarRelojSistemaValor"
Linea
Write-Host ""

if ($AjustarRelojSistemaValor -eq "true" -and -not (Test-Administrador)) {
    throw "Para ajustar el reloj real en Windows, abre PowerShell como Administrador y vuelve a ejecutar el comando."
}

$mvn = Get-RequiredCommand -Names @("mvn.cmd", "mvn") -InstallHint "Instala Maven o agregalo al PATH."
$java = Get-RequiredCommand -Names @("java.exe", "java") -InstallHint "Instala Java 21 o agregalo al PATH."
$hospitalJar = Join-Path $RootDir "backend/hospital-service/target/hospital-service.jar"
$gatewayJar = Join-Path $RootDir "backend/api-gateway/target/api-gateway.jar"

if (Test-BuildRequired -HospitalJar $hospitalJar -GatewayJar $gatewayJar) {
    Write-Host "Compilando backend con Maven..."
    & $mvn clean package -DskipTests
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
}

$consul = Get-RequiredCommand -Names @("consul.exe", "consul") -InstallHint "Instala Consul en esta PC."
Start-Background -Name "consul" -FilePath $consul -ArgumentList @("agent", "-dev", "-client=0.0.0.0")
Wait-ForPort -Name "consul" -HostName "127.0.0.1" -Port 8500 -TimeoutSeconds 20

Start-Background `
    -Name "gateway" `
    -FilePath $java `
    -ArgumentList @("-jar", $gatewayJar, "--server.port=8080") `
    -Environment @{ CONSUL_HOST = $ConsulHost; GATEWAY_HOST = $NodeHost }
Wait-ForPort -Name "gateway" -HostName "127.0.0.1" -Port 8080 -TimeoutSeconds 60

$npm = Get-RequiredCommand -Names @("npm.cmd", "npm") -InstallHint "Instala Node.js y npm para iniciar el frontend."
$frontendDir = Join-Path $RootDir "frontend"
if (-not (Test-Path (Join-Path $frontendDir "node_modules"))) {
    Write-Host "Instalando dependencias del frontend..."
    & $npm --prefix $frontendDir install
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
}

Start-Background `
    -Name "frontend-vite" `
    -FilePath $npm `
    -ArgumentList @("--prefix", $frontendDir, "run", "dev", "--", "--host", "0.0.0.0", "--port", "5173", "--strictPort")
Wait-ForPort -Name "frontend-vite" -HostName "127.0.0.1" -Port 5173 -TimeoutSeconds 30

Start-Background `
    -Name "node$NodeId" `
    -FilePath $java `
    -ArgumentList @("-jar", $hospitalJar) `
    -Environment @{
        NODE_ID = $NodeId
        NODE_HOST = $NodeHost
        TCP_PORT = $NodeTcpPort
        SERVER_PORT = $NodeHttpPort
        CONSUL_HOST = $ConsulHost
        AJUSTAR_RELOJ_SISTEMA = $AjustarRelojSistemaValor
        TIMEOUT_HEARTBEAT_MS = $TimeoutHeartbeatMs
        NODES_CONFIG_FILE = $NodesFile
    }

Wait-ForPort -Name "node$NodeId" -HostName "127.0.0.1" -Port $NodeHttpPort -TimeoutSeconds 60
Wait-ForPort -Name "node$NodeId" -HostName "127.0.0.1" -Port $NodeTcpPort -TimeoutSeconds 30

Write-Host ""
Linea
Write-Host "Nodo $NodeId iniciado correctamente"
Linea
Write-Host "API del nodo:      http://${NodeHost}:$NodeHttpPort"
Write-Host "TCP del nodo:      $NodeTcpPort"
Write-Host "Consul local:      http://${ConsulHost}:8500/ui"

Write-Host ""
Write-Host "Abrir en esta PC:"
Write-Host "  Panel web:        http://localhost:5173"
Write-Host "  API Gateway:      http://localhost:8080"
Write-Host "  Consul UI:        http://localhost:8500/ui"
Write-Host ""
Write-Host "Abrir desde otras PCs de la misma red:"
Write-Host "  Panel web:        http://${NodeHost}:5173"
Write-Host "  API Gateway:      http://${NodeHost}:8080"
Write-Host "  Consul UI:        http://${NodeHost}:8500/ui"

Write-Host ""
Write-Host "Logs utiles:"
Write-Host "  Get-Content logs/node$NodeId.out.log -Tail 80"
Write-Host "  Get-Content logs/frontend-vite.out.log -Tail 80"
Write-Host "  Get-Content logs/gateway.out.log -Tail 80"
Linea
