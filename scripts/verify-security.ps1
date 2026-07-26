param(
    [string]$BaseUrl = "http://127.0.0.1:8849"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot

function Read-Source {
    param([string]$Path)
    return Get-Content -LiteralPath (Join-Path $root $Path) -Raw -Encoding UTF8
}

function Assert-Contains {
    param(
        [string]$Content,
        [string]$Expected,
        [string]$Label
    )
    if (-not $Content.Contains($Expected)) {
        throw "$Label is missing: $Expected"
    }
}

function Assert-NotContains {
    param(
        [string]$Content,
        [string]$Forbidden,
        [string]$Label
    )
    if ($Content.Contains($Forbidden)) {
        throw "$Label still contains forbidden text: $Forbidden"
    }
}

$saTokenConfig = Read-Source `
    "pxczxn-backend\pxczxn-core\pxczxn-system\src\main\java\top\pxczxn\platform\system\config\SaTokenConfig.java"
$webSocketConfig = Read-Source `
    "pxczxn-backend\pxczxn-api\pxczxn-admin-api\src\main\java\top\pxczxn\platform\admin\websocket\WebSocketConfig.java"
$handshake = Read-Source `
    "pxczxn-backend\pxczxn-infra\pxczxn-websocket\src\main\java\top\pxczxn\platform\websocket\WebSocketHandshakeInterceptor.java"
$messageWebSocket = Read-Source "pxczxn-admin\src\utils\websocket.ts"
$sshWebSocket = Read-Source `
    "pxczxn-admin\src\views\monitor\server-manager\index.vue"
$userService = Read-Source `
    "pxczxn-backend\pxczxn-core\pxczxn-system\src\main\java\top\pxczxn\platform\system\service\impl\SysUserServiceImpl.java"
$userImport = Read-Source `
    "pxczxn-backend\pxczxn-core\pxczxn-system\src\main\java\top\pxczxn\platform\system\excel\SysUserImportListener.java"
$productionConfig = Read-Source `
    "pxczxn-backend\pxczxn-starter\src\main\resources\application-prod.yml"

Assert-NotContains $saTokenConfig 'allowedOriginPatterns("*")' "HTTP CORS"
Assert-NotContains $webSocketConfig 'setAllowedOrigins("*")' "WebSocket CORS"
Assert-NotContains $handshake 'getParameter("token")' "WebSocket handshake"
Assert-NotContains $messageWebSocket '?token=' "message WebSocket client"
Assert-NotContains $sshWebSocket '?token=' "SSH WebSocket client"
Assert-NotContains $userService '"123456"' "administrator user service"
Assert-NotContains $userImport '"123456"' "administrator import"

Assert-Contains $productionConfig "name: pxczxn-community" `
    "production application name"
Assert-Contains $productionConfig "demo-mode: false" `
    "production demo mode"
Assert-Contains $productionConfig "require-redis: true" `
    "production WebSocket ticket store"
Assert-Contains $productionConfig 'allowed-origins: ${PXCZXN_CORS_ALLOWED_ORIGINS}' `
    "production CORS whitelist"
Assert-NotContains $productionConfig "top.pxczxn.platform: debug" `
    "production logging"
Assert-NotContains $productionConfig "500MB" `
    "production upload limits"

$allowedOrigin = "http://localhost:8847"
$preflightHeaders = @{
    Origin = $allowedOrigin
    "Access-Control-Request-Method" = "GET"
}
$allowedResponse = Invoke-WebRequest `
    -Uri "$BaseUrl/api/v1/health" `
    -Method Options `
    -Headers $preflightHeaders `
    -UseBasicParsing

if ($allowedResponse.StatusCode -ne 200) {
    throw "Allowed CORS preflight returned HTTP $($allowedResponse.StatusCode)"
}
if ($allowedResponse.Headers["Access-Control-Allow-Origin"] -ne $allowedOrigin) {
    throw "Allowed CORS origin was not echoed explicitly"
}
if ($allowedResponse.Headers["Access-Control-Allow-Credentials"] -ne "true") {
    throw "Credentialed CORS response is missing its credentials flag"
}

$blockedStatus = $null
$blockedOriginHeader = $null
try {
    $blockedResponse = Invoke-WebRequest `
        -Uri "$BaseUrl/api/v1/health" `
        -Method Options `
        -Headers @{
            Origin = "https://attacker.example"
            "Access-Control-Request-Method" = "GET"
        } `
        -UseBasicParsing
    $blockedStatus = $blockedResponse.StatusCode
    $blockedOriginHeader = $blockedResponse.Headers["Access-Control-Allow-Origin"]
}
catch {
    $blockedStatus = [int]$_.Exception.Response.StatusCode
    $blockedOriginHeader = $_.Exception.Response.Headers["Access-Control-Allow-Origin"]
}
if ($blockedStatus -lt 400 -or $blockedOriginHeader) {
    throw "Untrusted CORS origin was not rejected"
}

$privateResponse = Invoke-RestMethod `
    -Uri "$BaseUrl/api/v1/account/me" `
    -Method Get
if ([int]$privateResponse.code -notin @(401, 403)) {
    throw "Unauthenticated community route returned code $($privateResponse.code)"
}

Write-Host "[PASS] explicit CORS origins and credential policy"
Write-Host "[PASS] one-time WebSocket ticket source contract"
Write-Host "[PASS] random administrator password source contract"
Write-Host "[PASS] production configuration hardening"
Write-Host "[PASS] unified community authentication boundary"
