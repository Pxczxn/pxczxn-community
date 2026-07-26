param(
    [string]$SourceDatabase = "mars-system",
    [string]$TargetDatabase = "pxczxn_community",
    [string]$DatabaseUser = "root",
    [string]$DatabasePassword = "root",
    [string]$MySqlPath = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe",
    [string]$MySqlDumpPath = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqldump.exe",
    [string]$BackupDirectory = ""
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$root = Split-Path -Parent $PSScriptRoot
$oldMySqlPassword = $env:MYSQL_PWD
$targetCreated = $false

function Assert-DatabaseName {
    param([string]$Name)
    if ($Name -notmatch '^[A-Za-z0-9_-]+$') {
        throw "Unsafe database name: $Name"
    }
}

function Quote-Identifier {
    param([string]$Name)
    return "``$Name``"
}

function Invoke-ServerQuery {
    param(
        [string]$Sql,
        [string]$Database = ""
    )

    $arguments = @(
        "--user=$DatabaseUser"
        "--default-character-set=utf8mb4"
        "--batch"
        "--raw"
        "--skip-column-names"
    )
    if ($Database) {
        $arguments += "--database=$Database"
    }
    $arguments += "--execute=$Sql"

    $output = @(& $MySqlPath @arguments 2>&1)
    if ($LASTEXITCODE -ne 0) {
        throw "MySQL query failed: $($output -join [Environment]::NewLine)"
    }
    return $output
}

function Get-ExactRowCounts {
    param([string]$Database)

    $databaseLiteral = $Database.Replace("'", "''")
    $tableNames = @(Invoke-ServerQuery @"
SELECT TABLE_NAME
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = '$databaseLiteral'
  AND TABLE_TYPE = 'BASE TABLE'
ORDER BY TABLE_NAME;
"@)

    $result = [ordered]@{}
    foreach ($tableNameValue in $tableNames) {
        $tableName = $tableNameValue.ToString()
        Assert-DatabaseName $tableName
        $databaseIdentifier = Quote-Identifier $Database
        $tableIdentifier = Quote-Identifier $tableName
        $countRows = @(Invoke-ServerQuery `
                "SELECT COUNT(*) FROM $databaseIdentifier.$tableIdentifier;")
        $count = [long]$countRows[0]
        $result[$tableName] = $count
    }
    return $result
}

try {
    Assert-DatabaseName $SourceDatabase
    Assert-DatabaseName $TargetDatabase
    if ($SourceDatabase -eq $TargetDatabase) {
        throw "Source and target database names must differ"
    }
    if (-not (Test-Path -LiteralPath $MySqlPath -PathType Leaf)) {
        throw "MySQL client not found: $MySqlPath"
    }
    if (-not (Test-Path -LiteralPath $MySqlDumpPath -PathType Leaf)) {
        throw "mysqldump not found: $MySqlDumpPath"
    }

    $env:MYSQL_PWD = $DatabasePassword
    $sourceLiteral = $SourceDatabase.Replace("'", "''")
    $targetLiteral = $TargetDatabase.Replace("'", "''")
    $sourceMetadata = @(Invoke-ServerQuery @"
SELECT DEFAULT_CHARACTER_SET_NAME, DEFAULT_COLLATION_NAME
FROM information_schema.SCHEMATA
WHERE SCHEMA_NAME = '$sourceLiteral';
"@)
    if ($sourceMetadata.Count -ne 1) {
        throw "Source database does not exist: $SourceDatabase"
    }

    $targetCountRows = @(Invoke-ServerQuery `
            "SELECT COUNT(*) FROM information_schema.SCHEMATA WHERE SCHEMA_NAME = '$targetLiteral';")
    $targetCount = [int]$targetCountRows[0]
    if ($targetCount -ne 0) {
        throw "Target database already exists; refusing to overwrite: $TargetDatabase"
    }

    $metadataParts = $sourceMetadata[0].ToString().Split("`t")
    $characterSet = $metadataParts[0]
    $collation = $metadataParts[1]
    Assert-DatabaseName $characterSet
    Assert-DatabaseName $collation

    if (-not $BackupDirectory) {
        $backupParent = Join-Path (Split-Path -Parent $root) "pxczxn-backups"
        $BackupDirectory = Join-Path $backupParent `
            "$(Get-Date -Format 'yyyyMMdd-HHmmss')-stage5-database-migration"
    }
    $resolvedBackupDirectory = [System.IO.Path]::GetFullPath($BackupDirectory)
    New-Item -ItemType Directory -Force -Path $resolvedBackupDirectory | Out-Null
    $dumpPath = Join-Path $resolvedBackupDirectory "legacy-database-before-stage5.sql"

    Write-Host "Creating consistent source backup..."
    $dumpArguments = @(
        "--user=$DatabaseUser"
        "--single-transaction"
        "--routines"
        "--triggers"
        "--events"
        "--hex-blob"
        "--set-gtid-purged=OFF"
        "--no-tablespaces"
        "--default-character-set=utf8mb4"
        "--result-file=$dumpPath"
        $SourceDatabase
    )
    $dumpOutput = @(& $MySqlDumpPath @dumpArguments 2>&1)
    if ($LASTEXITCODE -ne 0) {
        throw "mysqldump failed: $($dumpOutput -join [Environment]::NewLine)"
    }
    if (-not (Test-Path -LiteralPath $dumpPath -PathType Leaf) -or
        (Get-Item -LiteralPath $dumpPath).Length -eq 0) {
        throw "Backup dump is empty: $dumpPath"
    }
    $dumpHash = (Get-FileHash -LiteralPath $dumpPath -Algorithm SHA256).Hash

    $targetIdentifier = Quote-Identifier $TargetDatabase
    Invoke-ServerQuery @"
CREATE DATABASE $targetIdentifier
  CHARACTER SET $characterSet
  COLLATE $collation;
"@ | Out-Null
    $targetCreated = $true

    Write-Host "Importing backup into $TargetDatabase..."
    $sourcePath = $dumpPath.Replace('\', '/')
    Invoke-ServerQuery `
        -Database $TargetDatabase `
        -Sql "source $sourcePath" | Out-Null

    Write-Host "Comparing exact table row counts..."
    $sourceCounts = Get-ExactRowCounts $SourceDatabase
    $targetCounts = Get-ExactRowCounts $TargetDatabase
    if (($sourceCounts.Keys -join "`n") -ne ($targetCounts.Keys -join "`n")) {
        throw "Source and target table sets differ"
    }
    foreach ($tableName in $sourceCounts.Keys) {
        if ($sourceCounts[$tableName] -ne $targetCounts[$tableName]) {
            throw "Row count mismatch for $tableName`: source=$($sourceCounts[$tableName]) target=$($targetCounts[$tableName])"
        }
    }

    Write-Host "Running live schema verification..."
    & powershell.exe `
        -NoProfile `
        -ExecutionPolicy Bypass `
        -File (Join-Path $PSScriptRoot "check-database-migrations.ps1") `
        -Database $TargetDatabase `
        -DatabaseUser $DatabaseUser `
        -DatabasePassword $DatabasePassword `
        -MySqlPath $MySqlPath
    if ($LASTEXITCODE -ne 0) {
        throw "Live schema verification failed"
    }

    Write-Host "Recording verified migration baseline..."
    & powershell.exe `
        -NoProfile `
        -ExecutionPolicy Bypass `
        -File (Join-Path $PSScriptRoot "invoke-database-migrations.ps1") `
        -Database $TargetDatabase `
        -DatabaseUser $DatabaseUser `
        -DatabasePassword $DatabasePassword `
        -MySqlPath $MySqlPath `
        -BaselineExisting
    if ($LASTEXITCODE -ne 0) {
        throw "Migration baseline recording failed"
    }

    $manifestPath = Join-Path $resolvedBackupDirectory "migration-manifest.txt"
    @(
        "source_database=$SourceDatabase"
        "target_database=$TargetDatabase"
        "source_charset=$characterSet"
        "source_collation=$collation"
        "source_table_count=$($sourceCounts.Count)"
        "backup_file=$dumpPath"
        "backup_sha256=$dumpHash"
        "source_database_retained=true"
        "completed_at=$((Get-Date).ToString('o'))"
    ) | Set-Content -LiteralPath $manifestPath -Encoding UTF8

    Write-Host "[PASS] database copied and verified"
    Write-Host "       source=$SourceDatabase retained=true"
    Write-Host "       target=$TargetDatabase tables=$($sourceCounts.Count)"
    Write-Host "       backup=$dumpPath"
    Write-Host "       sha256=$dumpHash"
}
catch {
    if ($targetCreated) {
        Write-Warning "Target database was created and has been retained for inspection: $TargetDatabase"
        Write-Warning "No automatic DROP was attempted."
    }
    throw
}
finally {
    $env:MYSQL_PWD = $oldMySqlPassword
}
