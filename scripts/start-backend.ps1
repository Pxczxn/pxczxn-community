[CmdletBinding()]
param(
    [switch]$Rebuild
)

$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$backendRoot = Join-Path $projectRoot 'pxczxn-backend'
$jar = Join-Path $backendRoot 'pxczxn-starter\target\pxczxn-starter-1.0.0.jar'
$healthUrl = 'http://127.0.0.1:8849/api/v1/health'
$runtimeDirectory = Join-Path $projectRoot 'outputs\runtime'
$logFile = Join-Path $runtimeDirectory 'backend.log'

function Test-BackendHealthy {
    try {
        $response = Invoke-RestMethod -Uri $healthUrl -TimeoutSec 3
        return $response.data.status -eq 'UP'
    } catch {
        return $false
    }
}

if (Test-BackendHealthy) {
    Write-Host "Backend is already running: $healthUrl"
    exit 0
}

if ($Rebuild -or -not (Test-Path -LiteralPath $jar)) {
    Push-Location $backendRoot
    try {
        & .\mvnw.cmd -B -ntp -pl pxczxn-starter -am package -DskipTests
        if ($LASTEXITCODE -ne 0) {
            throw "Backend build failed with exit code $LASTEXITCODE."
        }
    } finally {
        Pop-Location
    }
}

if (-not $env:PXCZXN_DB_URL) {
    $env:PXCZXN_DB_URL = 'jdbc:mysql://localhost:3306/pxczxn_community?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=UTC'
}
if ($env:PXCZXN_DB_URL -notmatch '(^|[?&])allowPublicKeyRetrieval=') {
    $env:PXCZXN_DB_URL += '&allowPublicKeyRetrieval=true'
}

New-Item -ItemType Directory -Path $runtimeDirectory -Force | Out-Null
Remove-Item -LiteralPath $logFile -Force -ErrorAction SilentlyContinue
$process = Start-Process -FilePath 'java.exe' -ArgumentList '-jar', $jar -WorkingDirectory $backendRoot -RedirectStandardOutput $logFile -RedirectStandardError "$logFile.err" -PassThru -WindowStyle Hidden

$deadline = (Get-Date).AddSeconds(60)
while ((Get-Date) -lt $deadline) {
    if (Test-BackendHealthy) {
        Write-Host "Backend started (PID $($process.Id)): $healthUrl"
        exit 0
    }
    if ($process.HasExited) {
        Get-Content -LiteralPath "$logFile.err" -Tail 40 -ErrorAction SilentlyContinue
        throw "Backend exited unexpectedly. See $logFile"
    }
    Start-Sleep -Seconds 2
}

throw "Backend did not become healthy within 60 seconds. See $logFile"
