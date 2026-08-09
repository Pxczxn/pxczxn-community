param(
    [string]$SourceDatabase = "pxczxn_community",
    [string]$VerificationDatabase = "pxczxn_local_verify",
    [string]$DatabaseUser = "root",
    [string]$DatabasePassword = "root",
    [string]$MySqlPath = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe",
    [string]$MySqlDumpPath = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqldump.exe",
    [string]$MavenPath = "D:\Coding\software\environment\apache-maven-3.9.9\bin\mvn.cmd",
    [string]$BaseUrl = "http://127.0.0.1:8852",
    [int]$ServerPort = 8852,
    [string]$AdminPassword = "admin123",
    [switch]$KeepDatabase
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$root = Split-Path -Parent $PSScriptRoot
$workDirectory = Join-Path $root "work\verify-isolated"
$dumpPath = Join-Path $workDirectory "$VerificationDatabase-source.sql"
$oldMySqlPassword = $env:MYSQL_PWD
$databaseCreated = $false

function Assert-DatabaseName {
    param([string]$Value, [string]$ParameterName)

    if ($Value -notmatch '^[A-Za-z0-9_]+$') {
        throw "$ParameterName contains unsupported characters: $Value"
    }
}

function Invoke-MySql {
    param([string]$Sql, [switch]$WithoutDatabase)

    $arguments = @(
        "--user=$DatabaseUser"
        "--default-character-set=utf8mb4"
        "--execute=$Sql"
    )
    if (-not $WithoutDatabase) {
        $arguments += "--database=$VerificationDatabase"
    }
    $output = @(& $MySqlPath @arguments 2>&1)
    if ($LASTEXITCODE -ne 0) {
        throw "MySQL command failed: $($output -join [Environment]::NewLine)"
    }
}

try {
    Assert-DatabaseName -Value $SourceDatabase -ParameterName "SourceDatabase"
    Assert-DatabaseName -Value $VerificationDatabase -ParameterName "VerificationDatabase"
    if ($SourceDatabase -eq $VerificationDatabase) {
        throw "SourceDatabase and VerificationDatabase must be different."
    }
    if (-not $VerificationDatabase.StartsWith("pxczxn_local_verify", [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "VerificationDatabase must start with pxczxn_local_verify to prevent accidental deletion."
    }
    if (-not (Test-Path -LiteralPath $MySqlPath -PathType Leaf)) {
        throw "MySQL client not found: $MySqlPath"
    }
    if (-not (Test-Path -LiteralPath $MySqlDumpPath -PathType Leaf)) {
        throw "mysqldump not found: $MySqlDumpPath"
    }
    $baseUri = [uri]$BaseUrl
    if ($baseUri.Port -ne $ServerPort) {
        throw "BaseUrl port must match ServerPort."
    }

    New-Item -ItemType Directory -Force -Path $workDirectory | Out-Null
    $env:MYSQL_PWD = $DatabasePassword
    Write-Host "[INFO] Creating a consistent source snapshot from $SourceDatabase"
    & $MySqlDumpPath `
        "--user=$DatabaseUser" `
        "--single-transaction" `
        "--routines" `
        "--events" `
        "--triggers" `
        "--no-tablespaces" `
        "--default-character-set=utf8mb4" `
        "--result-file=$dumpPath" `
        $SourceDatabase
    if ($LASTEXITCODE -ne 0) {
        throw "mysqldump failed with code $LASTEXITCODE"
    }

    Invoke-MySql -WithoutDatabase -Sql "DROP DATABASE IF EXISTS ``$VerificationDatabase``; CREATE DATABASE ``$VerificationDatabase`` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    $databaseCreated = $true
    $sourcePath = $dumpPath.Replace('\', '/')
    Invoke-MySql -Sql "source $sourcePath"
    Write-Host "[PASS] Restored isolated database $VerificationDatabase"

    & powershell.exe `
        -NoProfile `
        -ExecutionPolicy Bypass `
        -File (Join-Path $PSScriptRoot "verify.ps1") `
        -Database $VerificationDatabase `
        -DatabaseUser $DatabaseUser `
        -DatabasePassword $DatabasePassword `
        -MySqlPath $MySqlPath `
        -MavenPath $MavenPath `
        -BaseUrl $BaseUrl `
        -ServerPort $ServerPort `
        -AdminPassword $AdminPassword `
        -ResetE2EAbuseWindows
    if ($LASTEXITCODE -ne 0) {
        throw "Isolated verification failed with code $LASTEXITCODE"
    }
}
finally {
    if ($databaseCreated -and -not $KeepDatabase) {
        try {
            Invoke-MySql -WithoutDatabase -Sql "DROP DATABASE IF EXISTS ``$VerificationDatabase``;"
            Write-Host "[PASS] Removed isolated database $VerificationDatabase"
        }
        catch {
            Write-Error "Failed to remove isolated database ${VerificationDatabase}: $($_.Exception.Message)"
        }
    }
    $env:MYSQL_PWD = $oldMySqlPassword
}
