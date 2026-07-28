$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$migrations = @(Get-ChildItem (Join-Path $root "database\migrations") -File -Filter "V*.sql" | Sort-Object Name)
$verifies = @(Get-ChildItem (Join-Path $root "database\verify") -File -Filter "V*.sql" | Sort-Object Name)

if ($migrations.Count -eq 0) { throw "No database migrations found" }
$migrationVersions = @($migrations | ForEach-Object {
    if ($_.Name -notmatch '^V(\d{3})__([a-z0-9_]+)\.sql$') { throw "Invalid migration filename: $($_.Name)" }
    [int]$Matches[1]
})
$verifyVersions = @($verifies | ForEach-Object {
    if ($_.Name -notmatch '^V(\d{3})__verify_([a-z0-9_]+)\.sql$') { throw "Invalid verification filename: $($_.Name)" }
    [int]$Matches[1]
})

if (@($migrationVersions | Group-Object | Where-Object Count -gt 1).Count) { throw "Duplicate migration version" }
for ($index = 0; $index -lt $migrationVersions.Count; $index++) {
    if ($migrationVersions[$index] -ne ($index + 1)) { throw "Migration version gap at index $index" }
}
if (($migrationVersions -join ',') -ne ($verifyVersions -join ',')) { throw "Migration and verification versions differ" }
Write-Host "[PASS] $($migrations.Count) ordered migrations and matching verification scripts"
