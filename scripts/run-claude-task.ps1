param(
    [Parameter(Mandatory = $true)]
    [string]$Worktree,

    [Parameter(Mandatory = $true)]
    [string]$TaskFile,

    [Parameter(Mandatory = $false)]
    [string]$ResultFile = "outputs/agent-results/latest.json",

    [Parameter(Mandatory = $false)]
    [string]$Model = "opus",

    [Parameter(Mandatory = $false)]
    [ValidateSet("low", "medium", "high", "xhigh", "max")]
    [string]$Effort = "max"
)

$ErrorActionPreference = "Stop"

if (-not (Get-Command claude -ErrorAction SilentlyContinue)) {
    throw "Claude Code CLI is not installed or not on PATH. Install it and sign in before retrying."
}
if (-not (Test-Path -LiteralPath $Worktree -PathType Container)) {
    throw "Worktree does not exist: $Worktree"
}
if (-not (Test-Path -LiteralPath $TaskFile -PathType Leaf)) {
    throw "Task file does not exist: $TaskFile"
}

$absoluteWorktree = (Resolve-Path -LiteralPath $Worktree).Path
$absoluteTask = (Resolve-Path -LiteralPath $TaskFile).Path
$absoluteResult = [System.IO.Path]::GetFullPath($ResultFile)
$resultParent = Split-Path -Parent $absoluteResult

if ($resultParent) {
    New-Item -ItemType Directory -Path $resultParent -Force | Out-Null
}

$prompt = @"
Read CLAUDE.md in the project root first.

Then read the task file:
@$absoluteTask

Implement only the task-file requirements. When finished:
1. Inspect git diff.
2. Run the task-required tests.
3. Report modified files, implementation, test results, and risks.
4. Do not commit Git changes; Codex owns review and commits.
"@

Push-Location $absoluteWorktree
try {
    Write-Host "Starting Claude Code..."
    Write-Host "Worktree: $absoluteWorktree"
    Write-Host "Task file: $absoluteTask"

    & claude -p $prompt `
        --output-format json `
        --model $Model `
        --effort $Effort `
        --permission-mode acceptEdits 2>&1 |
        Tee-Object -FilePath $absoluteResult
    if ($LASTEXITCODE -ne 0) {
        throw "Claude Code failed with exit code: $LASTEXITCODE"
    }
}
finally {
    Pop-Location
}
