param(
    [string]$MavenPath = "D:\Coding\software\environment\apache-maven-3.9.9\bin\mvn.cmd"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$backendRoot = Join-Path $root "pxczxn-backend"
$webRoot = Join-Path $root "pxczxn-web"
$adminRoot = Join-Path $root "pxczxn-admin"
$outputDirectory = Join-Path $root "outputs\security"
$mavenSettingsPath = Join-Path $PSScriptRoot "maven-central-settings.xml"
$utf8WithoutBom = New-Object System.Text.UTF8Encoding($false)

function Write-Utf8File {
    param(
        [string]$Path,
        [object[]]$Content
    )

    $text = ($Content | ForEach-Object { [string]$_ }) -join [Environment]::NewLine
    [System.IO.File]::WriteAllText($Path, $text, $utf8WithoutBom)
}

function Invoke-NpmJson {
    param(
        [string]$Name,
        [string]$WorkingDirectory,
        [string[]]$Arguments,
        [string]$OutputPath
    )

    Write-Host "Running $Name..."
    Push-Location $WorkingDirectory
    try {
        $output = & npm.cmd @Arguments 2>&1
        $exitCode = $LASTEXITCODE
        Write-Utf8File -Path $OutputPath -Content $output
        if ($exitCode -ne 0) {
            throw "$Name exited with code $exitCode. Report: $OutputPath"
        }
    }
    finally {
        Pop-Location
    }
}

if (-not (Test-Path -LiteralPath $MavenPath -PathType Leaf)) {
    throw "Maven executable not found: $MavenPath"
}
if (-not (Test-Path -LiteralPath $mavenSettingsPath -PathType Leaf)) {
    throw "Maven security settings not found: $mavenSettingsPath"
}

New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null

Write-Host "Running Maven vulnerability scan and backend SBOM generation..."
Push-Location $backendRoot
try {
    $commonMavenArguments = @(
        "--settings",
        $mavenSettingsPath
    )
    & $MavenPath @commonMavenArguments "-DskipTests" "install"
    if ($LASTEXITCODE -ne 0) {
        throw "Maven dependency preparation exited with code $LASTEXITCODE"
    }

    $securityMavenArguments = $commonMavenArguments + @(
        "-Psecurity",
        "-DskipTests",
        "verify"
    )
    if (-not [string]::IsNullOrWhiteSpace($env:NVD_API_KEY)) {
        $securityMavenArguments += "-DnvdApiKey=$($env:NVD_API_KEY)"
    }
    & $MavenPath @securityMavenArguments
    if ($LASTEXITCODE -ne 0) {
        throw "Maven dependency security verification exited with code $LASTEXITCODE"
    }
}
finally {
    Pop-Location
}

$frontends = @(
    @{
        Name = "blog"
        Directory = $webRoot
    },
    @{
        Name = "admin"
        Directory = $adminRoot
    }
)

foreach ($frontend in $frontends) {
    $name = $frontend.Name
    $directory = $frontend.Directory

    Invoke-NpmJson `
        -Name "$name production dependency audit" `
        -WorkingDirectory $directory `
        -Arguments @("audit", "--omit=dev", "--audit-level=high", "--json") `
        -OutputPath (Join-Path $outputDirectory "$name-audit-production.json")

    Invoke-NpmJson `
        -Name "$name complete dependency audit" `
        -WorkingDirectory $directory `
        -Arguments @("audit", "--audit-level=critical", "--json") `
        -OutputPath (Join-Path $outputDirectory "$name-audit-complete.json")

    Invoke-NpmJson `
        -Name "$name CycloneDX SBOM" `
        -WorkingDirectory $directory `
        -Arguments @(
            "sbom",
            "--package-lock-only",
            "--sbom-format=cyclonedx",
            "--sbom-type=application"
        ) `
        -OutputPath (Join-Path $outputDirectory "$name-sbom.cdx.json")
}

Write-Host "[PASS] Dependency vulnerability scans and SBOM generation"
