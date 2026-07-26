param(
    [string]$Database = "mars-system",
    [string]$DatabaseUser = "root",
    [string]$DatabasePassword = "root",
    [string]$MySqlPath = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe",
    [string]$MavenPath = "D:\Coding\software\environment\apache-maven-3.9.9\bin\mvn.cmd",
    [string]$BaseUrl = "http://127.0.0.1:8849",
    [string]$AdminPassword = "admin123"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$backendRoot = Join-Path $root "pxczxn-backend"
$webRoot = Join-Path $root "pxczxn-web"
$adminRoot = Join-Path $root "pxczxn-admin"
$workDirectory = Join-Path $root "work\verify"
$backendProcess = $null
$captchaOriginal = $null
$captchaChanged = $false
$oldMySqlPassword = $env:MYSQL_PWD
$oldDbUrl = $env:PXCZXN_DB_URL
$oldDbUsername = $env:PXCZXN_DB_USERNAME
$oldDbPassword = $env:PXCZXN_DB_PASSWORD
$oldBaseUrl = $env:PXCZXN_BASE_URL
$oldDbName = $env:PXCZXN_DB_NAME
$oldMySqlPath = $env:PXCZXN_MYSQL_PATH
$oldAdminPassword = $env:PXCZXN_ADMIN_PASSWORD

function Invoke-Step {
    param(
        [string]$Name,
        [string]$WorkingDirectory,
        [scriptblock]$Action
    )

    Write-Host ""
    Write-Host "========== $Name ==========" -ForegroundColor Cyan
    Push-Location $WorkingDirectory
    try {
        & $Action
        if ($LASTEXITCODE -ne 0) {
            throw "$Name exited with code $LASTEXITCODE"
        }
        Write-Host "[PASS] $Name" -ForegroundColor Green
    }
    finally {
        Pop-Location
    }
}

function Invoke-MySqlScalar {
    param([string]$Sql)

    $env:MYSQL_PWD = $DatabasePassword
    $output = & $MySqlPath `
        "--user=$DatabaseUser" `
        "--database=$Database" `
        "--default-character-set=utf8mb4" `
        "--batch" `
        "--skip-column-names" `
        "--execute=$Sql" 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "MySQL command failed: $output"
    }
    return ($output | Out-String).Trim()
}

function Test-BackendReady {
    try {
        $response = Invoke-RestMethod `
            -Uri "$BaseUrl/api/v1/health" `
            -Method Get `
            -TimeoutSec 2
        return $response.code -eq 200 -and $response.data.status -eq "UP"
    }
    catch {
        return $false
    }
}

try {
    if (-not (Test-Path -LiteralPath $MavenPath -PathType Leaf)) {
        throw "Maven executable not found: $MavenPath"
    }
    if (-not (Test-Path -LiteralPath $MySqlPath -PathType Leaf)) {
        throw "MySQL client not found: $MySqlPath"
    }

    Invoke-Step "Backend Maven tests" $backendRoot {
        & $MavenPath verify
    }

    Invoke-Step "Blog typecheck" $webRoot { & npm.cmd run typecheck }
    Invoke-Step "Blog lint" $webRoot { & npm.cmd run lint }
    Invoke-Step "Blog tests" $webRoot { & npm.cmd test }
    Invoke-Step "Blog build" $webRoot { & npm.cmd run build }

    Invoke-Step "Admin typecheck" $adminRoot { & npm.cmd run typecheck }
    Invoke-Step "Admin lint" $adminRoot { & npm.cmd run lint }
    Invoke-Step "Admin tests" $adminRoot { & npm.cmd test }
    Invoke-Step "Admin build" $adminRoot { & npm.cmd run build }

    Invoke-Step "Database migration version check" $root {
        & powershell.exe `
            -NoProfile `
            -ExecutionPolicy Bypass `
            -File (Join-Path $PSScriptRoot "check-database-migrations.ps1") `
            -Database $Database `
            -DatabaseUser $DatabaseUser `
            -DatabasePassword $DatabasePassword `
            -MySqlPath $MySqlPath
    }

    $env:PXCZXN_DB_URL = "jdbc:mysql://localhost:3306/$Database`?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=UTC"
    $env:PXCZXN_DB_USERNAME = $DatabaseUser
    $env:PXCZXN_DB_PASSWORD = $DatabasePassword
    $env:PXCZXN_BASE_URL = $BaseUrl
    $env:PXCZXN_DB_NAME = $Database
    $env:PXCZXN_MYSQL_PATH = $MySqlPath
    $env:PXCZXN_ADMIN_PASSWORD = $AdminPassword

    $captchaOriginal = Invoke-MySqlScalar @"
SELECT JSON_UNQUOTE(JSON_EXTRACT(config_value, '$.captchaEnabled'))
FROM sys_config_group
WHERE group_code = 'login'
LIMIT 1;
"@
    if ($captchaOriginal -notin @("true", "false")) {
        throw "Unable to read the original captchaEnabled value"
    }
    if ($captchaOriginal -eq "true") {
        Invoke-MySqlScalar @"
UPDATE sys_config_group
SET config_value = JSON_SET(config_value, '$.captchaEnabled', false)
WHERE group_code = 'login';
"@ | Out-Null
        $captchaChanged = $true
    }

    if (-not (Test-BackendReady)) {
        New-Item -ItemType Directory -Force -Path $workDirectory | Out-Null
        $jar = Join-Path $backendRoot "mars-starter\target\mars-starter-1.0.0.jar"
        if (-not (Test-Path -LiteralPath $jar -PathType Leaf)) {
            throw "Backend JAR not found after Maven verification: $jar"
        }
        $backendProcess = Start-Process `
            -FilePath "java.exe" `
            -ArgumentList @("-jar", $jar) `
            -PassThru `
            -WindowStyle Hidden `
            -RedirectStandardOutput (Join-Path $workDirectory "backend.out.log") `
            -RedirectStandardError (Join-Path $workDirectory "backend.err.log")

        $ready = $false
        for ($attempt = 0; $attempt -lt 60; $attempt++) {
            if ($backendProcess.HasExited) {
                throw "Backend exited before becoming ready. See work/verify/backend.err.log"
            }
            if (Test-BackendReady) {
                $ready = $true
                break
            }
            Start-Sleep -Seconds 1
        }
        if (-not $ready) {
            throw "Backend did not become ready within 60 seconds"
        }
        Write-Host "[PASS] Started verification backend on $BaseUrl"
    }
    else {
        Write-Host "[PASS] Reusing healthy backend on $BaseUrl"
    }

    Invoke-Step "Comments E2E" $root {
        & powershell.exe `
            -NoProfile `
            -ExecutionPolicy Bypass `
            -File (Join-Path $PSScriptRoot "e2e\m2-t005-comments.ps1") `
            -BaseUrl $BaseUrl `
            -Database $Database `
            -DatabaseUser $DatabaseUser `
            -DatabasePassword $DatabasePassword `
            -MySqlPath $MySqlPath
    }
    Invoke-Step "Moments E2E" $root {
        & powershell.exe `
            -NoProfile `
            -ExecutionPolicy Bypass `
            -File (Join-Path $PSScriptRoot "e2e\m2-t006-moments.ps1") `
            -BaseUrl $BaseUrl `
            -Database $Database `
            -DatabaseUser $DatabaseUser `
            -DatabasePassword $DatabasePassword `
            -MySqlPath $MySqlPath
    }
    Invoke-Step "Notifications E2E" $root {
        & powershell.exe `
            -NoProfile `
            -ExecutionPolicy Bypass `
            -File (Join-Path $PSScriptRoot "e2e\m2-t007-notifications.ps1") `
            -BaseUrl $BaseUrl `
            -Database $Database `
            -DatabaseUser $DatabaseUser `
            -DatabasePassword $DatabasePassword `
            -MySqlPath $MySqlPath
    }

    Invoke-Step "Governance E2E" $root {
        & node.exe (Join-Path $PSScriptRoot "e2e\m2-t009-admin-governance.mjs")
    }

    Write-Host ""
    Write-Host "ALL VERIFICATION GATES PASSED" -ForegroundColor Green
}
finally {
    if ($captchaChanged -and $captchaOriginal) {
        try {
            Invoke-MySqlScalar @"
UPDATE sys_config_group
SET config_value = JSON_SET(config_value, '$.captchaEnabled', $captchaOriginal)
WHERE group_code = 'login';
"@ | Out-Null
            Write-Host "[PASS] Restored captchaEnabled=$captchaOriginal"
        }
        catch {
            Write-Error "Failed to restore captchaEnabled: $($_.Exception.Message)"
        }
    }

    if ($backendProcess -and -not $backendProcess.HasExited) {
        Stop-Process -Id $backendProcess.Id -Force
        $backendProcess.WaitForExit()
        Write-Host "[PASS] Stopped verification backend"
    }

    $env:MYSQL_PWD = $oldMySqlPassword
    $env:PXCZXN_DB_URL = $oldDbUrl
    $env:PXCZXN_DB_USERNAME = $oldDbUsername
    $env:PXCZXN_DB_PASSWORD = $oldDbPassword
    $env:PXCZXN_BASE_URL = $oldBaseUrl
    $env:PXCZXN_DB_NAME = $oldDbName
    $env:PXCZXN_MYSQL_PATH = $oldMySqlPath
    $env:PXCZXN_ADMIN_PASSWORD = $oldAdminPassword
}
