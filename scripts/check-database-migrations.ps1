param(
    [string]$Database = "pxczxn_community",
    [string]$DatabaseUser = "root",
    [string]$DatabasePassword = "root",
    [string]$MySqlPath = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$migrationDirectory = Join-Path $root "database\migrations"
$verifyDirectory = Join-Path $root "database\verify"
$oldMySqlPassword = $env:MYSQL_PWD

function Resolve-MySqlCommand {
    if (Test-Path -LiteralPath $MySqlPath -PathType Leaf) {
        return (Resolve-Path -LiteralPath $MySqlPath).Path
    }
    $command = Get-Command $MySqlPath -CommandType Application -ErrorAction SilentlyContinue
    if ($null -eq $command) {
        throw "MySQL client not found: $MySqlPath"
    }
    return $command.Source
}

function Get-NormalizedMigrationChecksum {
    param([System.IO.FileInfo]$File)

    $content = [System.IO.File]::ReadAllText($File.FullName)
    $normalizedContent = $content.Replace("`r`n", "`n").Replace("`r", "`n")
    $bytes = [System.Text.UTF8Encoding]::new($false).GetBytes($normalizedContent)
    $hasher = [System.Security.Cryptography.SHA256]::Create()
    try {
        return (-join ($hasher.ComputeHash($bytes) | ForEach-Object {
            $_.ToString("x2")
        })).ToUpperInvariant()
    }
    finally {
        $hasher.Dispose()
    }
}

try {
    $MySqlPath = Resolve-MySqlCommand

    $migrations = @(Get-ChildItem -LiteralPath $migrationDirectory -Filter "V*.sql" |
        Sort-Object Name)
    if ($migrations.Count -eq 0) {
        throw "No migration files found in $migrationDirectory"
    }

    $versions = @()
    foreach ($migration in $migrations) {
        if ($migration.Name -notmatch '^V(\d{3})__([a-z0-9_]+)\.sql$') {
            throw "Invalid migration filename: $($migration.Name)"
        }
        $versions += [int]$Matches[1]
    }

    $duplicates = @($versions | Group-Object | Where-Object Count -gt 1)
    if ($duplicates.Count -gt 0) {
        throw "Duplicate migration versions: $($duplicates.Name -join ', ')"
    }

    for ($index = 0; $index -lt $versions.Count; $index++) {
        $expected = $index + 1
        if ($versions[$index] -ne $expected) {
            throw "Migration version gap: expected V$($expected.ToString('000')) but found V$($versions[$index].ToString('000'))"
        }
    }

    $verifyFiles = @(Get-ChildItem -LiteralPath $verifyDirectory -Filter "V*.sql" |
        Sort-Object Name)
    $verifyVersions = @($verifyFiles | ForEach-Object {
        if ($_.Name -notmatch '^V(\d{3})__verify_([a-z0-9_]+)\.sql$') {
            throw "Invalid verification filename: $($_.Name)"
        }
        [int]$Matches[1]
    })

    if (($versions -join ",") -ne ($verifyVersions -join ",")) {
        throw "Migration and verification versions do not match"
    }

    $env:MYSQL_PWD = $DatabasePassword
    foreach ($verifyFile in $verifyFiles) {
        $sourcePath = $verifyFile.FullName.Replace('\', '/')
        $output = & $MySqlPath `
            "--user=$DatabaseUser" `
            "--database=$Database" `
            "--default-character-set=utf8mb4" `
            "--batch" `
            "--raw" `
            "--execute=source $sourcePath" 2>&1
        if ($LASTEXITCODE -ne 0) {
            throw "Database verification failed for $($verifyFile.Name): $output"
        }
        if (($output | Out-String) -match '(?im)(^|\s)FAIL(\s|$)') {
            throw "Database verification reported FAIL for $($verifyFile.Name): $output"
        }
        Write-Host "[PASS] $($verifyFile.Name)"
    }

    Write-Host "[PASS] $($migrations.Count) ordered migrations and $($verifyFiles.Count) live verification scripts"
    foreach ($migration in $migrations) {
        $checksum = Get-NormalizedMigrationChecksum $migration
        Write-Host "       $($migration.Name) $checksum"
    }

    $powerShell = (Get-Command pwsh -CommandType Application -ErrorAction SilentlyContinue).Source
    if (-not $powerShell) {
        $powerShell = (Get-Command powershell.exe -CommandType Application -ErrorAction SilentlyContinue).Source
    }
    if (-not $powerShell) {
        throw "PowerShell executable not found"
    }
    & $powerShell `
        -NoProfile `
        -ExecutionPolicy Bypass `
        -File (Join-Path $PSScriptRoot "invoke-database-migrations.ps1") `
        -Database $Database `
        -DatabaseUser $DatabaseUser `
        -DatabasePassword $DatabasePassword `
        -MySqlPath $MySqlPath `
        -CheckOnly
    if ($LASTEXITCODE -ne 0) {
        throw "Migration history or checksum verification failed"
    }
}
finally {
    $env:MYSQL_PWD = $oldMySqlPassword
}
