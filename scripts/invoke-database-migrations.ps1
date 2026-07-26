param(
    [string]$Database = "pxczxn_community",
    [string]$DatabaseUser = "root",
    [string]$DatabasePassword = "root",
    [string]$MySqlPath = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe",
    [string]$MigrationDirectory = "",
    [string]$VerifyDirectory = "",
    [switch]$BaselineExisting,
    [switch]$CheckOnly
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$root = Split-Path -Parent $PSScriptRoot
if (-not $MigrationDirectory) {
    $MigrationDirectory = Join-Path $root "database\migrations"
}
if (-not $VerifyDirectory) {
    $VerifyDirectory = Join-Path $root "database\verify"
}
$oldMySqlPassword = $env:MYSQL_PWD

function ConvertTo-SqlLiteral {
    param([string]$Value)
    return $Value.Replace("'", "''")
}

function Invoke-MySqlQuery {
    param([string]$Sql)

    $arguments = @(
        "--user=$DatabaseUser"
        "--database=$Database"
        "--default-character-set=utf8mb4"
        "--batch"
        "--raw"
        "--skip-column-names"
        "--execute=$Sql"
    )
    $output = @(& $MySqlPath @arguments 2>&1)
    if ($LASTEXITCODE -ne 0) {
        throw "MySQL query failed: $($output -join [Environment]::NewLine)"
    }
    return $output
}

function Invoke-MySqlFile {
    param([System.IO.FileInfo]$File)

    $sourcePath = $File.FullName.Replace('\', '/')
    $arguments = @(
        "--user=$DatabaseUser"
        "--database=$Database"
        "--default-character-set=utf8mb4"
        "--batch"
        "--raw"
        "--execute=source $sourcePath"
    )
    $output = @(& $MySqlPath @arguments 2>&1)
    if ($LASTEXITCODE -ne 0) {
        throw "MySQL file failed ($($File.Name)): $($output -join [Environment]::NewLine)"
    }
    if (($output | Out-String) -match '(?im)(^|\s)FAIL(\s|$)') {
        throw "MySQL file reported FAIL ($($File.Name)): $($output -join [Environment]::NewLine)"
    }
    return $output
}

function Set-MigrationHistory {
    param(
        [string]$Version,
        [string]$Description,
        [string]$Checksum,
        [int]$Success
    )

    $versionLiteral = ConvertTo-SqlLiteral $Version
    $descriptionLiteral = ConvertTo-SqlLiteral $Description
    $checksumLiteral = ConvertTo-SqlLiteral $Checksum
    Invoke-MySqlQuery @"
INSERT INTO pxczxn_schema_version (
    version,
    description,
    checksum,
    executed_at,
    success
) VALUES (
    '$versionLiteral',
    '$descriptionLiteral',
    '$checksumLiteral',
    CURRENT_TIMESTAMP(3),
    $Success
)
ON DUPLICATE KEY UPDATE
    description = VALUES(description),
    checksum = VALUES(checksum),
    executed_at = VALUES(executed_at),
    success = VALUES(success);
"@ | Out-Null
}

try {
    if ($BaselineExisting -and $CheckOnly) {
        throw "BaselineExisting and CheckOnly cannot be used together"
    }
    if (-not (Test-Path -LiteralPath $MySqlPath -PathType Leaf)) {
        throw "MySQL client not found: $MySqlPath"
    }

    $migrations = @(Get-ChildItem -LiteralPath $MigrationDirectory -File -Filter "V*.sql" |
        Sort-Object Name)
    if ($migrations.Count -eq 0) {
        throw "No migration files found in $MigrationDirectory"
    }

    $env:MYSQL_PWD = $DatabasePassword
    $historyTableRows = @(Invoke-MySqlQuery @"
SELECT COUNT(*)
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'pxczxn_schema_version';
"@)
    $historyTableCount = [int]$historyTableRows[0]
    if ($CheckOnly -and $historyTableCount -ne 1) {
        throw "pxczxn_schema_version does not exist in $Database"
    }
    if (-not $CheckOnly) {
        Invoke-MySqlQuery @"
CREATE TABLE IF NOT EXISTS pxczxn_schema_version (
    version VARCHAR(32) NOT NULL,
    description VARCHAR(255) NOT NULL,
    checksum CHAR(64) NOT NULL,
    executed_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    success TINYINT(1) NOT NULL,
    PRIMARY KEY (version),
    CONSTRAINT chk_pxczxn_schema_version_success CHECK (success IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
"@ | Out-Null
    }

    $applied = 0
    $skipped = 0
    $baselined = 0

    foreach ($migration in $migrations) {
        if ($migration.Name -notmatch '^(V\d{3})__([a-z0-9_]+)\.sql$') {
            throw "Invalid migration filename: $($migration.Name)"
        }

        $version = $Matches[1]
        $description = $Matches[2].Replace('_', ' ')
        $checksum = (Get-FileHash -LiteralPath $migration.FullName -Algorithm SHA256).Hash
        $versionLiteral = ConvertTo-SqlLiteral $version
        $rows = @(Invoke-MySqlQuery @"
SELECT version, checksum, success
FROM pxczxn_schema_version
WHERE version = '$versionLiteral';
"@)

        $existingChecksum = $null
        $existingSuccess = $null
        if ($rows.Count -gt 0) {
            $parts = $rows[0].ToString().Split("`t")
            if ($parts.Count -ne 3) {
                throw "Invalid migration history row for $version"
            }
            $existingChecksum = $parts[1]
            $existingSuccess = [int]$parts[2]

            if ($existingChecksum -ne $checksum) {
                throw "Checksum mismatch for $version. Recorded=$existingChecksum Current=$checksum"
            }
            if ($existingSuccess -eq 1) {
                Write-Host "[SKIP] $version checksum verified"
                $skipped++
                continue
            }
            if ($CheckOnly) {
                throw "$version is recorded as failed"
            }
            if ($BaselineExisting) {
                throw "$version has a previous failed execution and cannot be baselined"
            }
        }
        elseif ($CheckOnly) {
            throw "$version is missing from pxczxn_schema_version"
        }

        $verifyFiles = @(Get-ChildItem -LiteralPath $VerifyDirectory -File `
                -Filter "$($version)__verify_*.sql")
        if ($verifyFiles.Count -ne 1) {
            throw "Expected exactly one verification file for $version, found $($verifyFiles.Count)"
        }

        try {
            if ($BaselineExisting) {
                Invoke-MySqlFile $verifyFiles[0] | Out-Null
                Set-MigrationHistory `
                    -Version $version `
                    -Description $description `
                    -Checksum $checksum `
                    -Success 1
                Write-Host "[BASELINE] $version $description"
                $baselined++
            }
            else {
                Invoke-MySqlFile $migration | Out-Null
                Invoke-MySqlFile $verifyFiles[0] | Out-Null
                Set-MigrationHistory `
                    -Version $version `
                    -Description $description `
                    -Checksum $checksum `
                    -Success 1
                Write-Host "[APPLY] $version $description"
                $applied++
            }
        }
        catch {
            try {
                Set-MigrationHistory `
                    -Version $version `
                    -Description $description `
                    -Checksum $checksum `
                    -Success 0
            }
            catch {
                Write-Warning "Unable to record failed migration $version"
            }
            throw
        }
    }

    $failedCountRows = @(Invoke-MySqlQuery `
            "SELECT COUNT(*) FROM pxczxn_schema_version WHERE success <> 1;")
    $failedCount = [int]$failedCountRows[0]
    if ($failedCount -ne 0) {
        throw "Migration history contains $failedCount failed entries"
    }
    $historyCountRows = @(Invoke-MySqlQuery `
            "SELECT COUNT(*) FROM pxczxn_schema_version;")
    $historyCount = [int]$historyCountRows[0]
    if ($historyCount -ne $migrations.Count) {
        throw "Migration history count $historyCount does not match repository count $($migrations.Count)"
    }

    Write-Host "[PASS] migration history verified"
    Write-Host "       checkOnly=$CheckOnly applied=$applied baselined=$baselined skipped=$skipped total=$($migrations.Count)"
}
finally {
    $env:MYSQL_PWD = $oldMySqlPassword
}
