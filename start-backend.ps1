# ============================================================================
# GlobalCart - Start Spring Boot Backend with Cloud Spanner Emulator
# ============================================================================

[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"

Write-Host "========================================================================" -ForegroundColor Cyan
Write-Host " Launching GlobalCart Spring Boot Backend" -ForegroundColor Cyan
Write-Host "========================================================================" -ForegroundColor Cyan

# Set environment variables for Spanner Emulator
$env:SPANNER_EMULATOR_HOST = "localhost:9010"
$env:USE_ANONYMOUS_CREDENTIALS = "true"
$env:GCP_PROJECT_ID = "globalcart-dev-project"
$env:SPANNER_INSTANCE_ID = "globalcart-spanner-instance"
$env:SPANNER_DATABASE_ID = "globalcart_db"

Write-Host "[INFO] Environment variables configured:" -ForegroundColor Yellow
Write-Host "       SPANNER_EMULATOR_HOST = $env:SPANNER_EMULATOR_HOST" -ForegroundColor White
Write-Host "       GCP_PROJECT_ID        = $env:GCP_PROJECT_ID" -ForegroundColor White
Write-Host "       SPANNER_INSTANCE_ID   = $env:SPANNER_INSTANCE_ID" -ForegroundColor White
Write-Host "       SPANNER_DATABASE_ID   = $env:SPANNER_DATABASE_ID" -ForegroundColor White
Write-Host ""

$mvnExecutable = $null
$mavenCmd = Get-Command mvn -ErrorAction SilentlyContinue

if (Test-Path ".\mvnw.cmd") {
    $mvnExecutable = ".\mvnw.cmd"
} elseif ($mavenCmd) {
    $mvnExecutable = "mvn"
} else {
    $fallbackPaths = @(
        "C:\Users\karth\.m2\wrapper\dists\apache-maven-3.9.6-bin\3311e1d4\apache-maven-3.9.6\bin\mvn.cmd",
        "C:\Users\karth\.maven\maven-3.10.0-rc-1\bin\mvn.cmd"
    )
    foreach ($p in $fallbackPaths) {
        if (Test-Path $p) {
            $mvnExecutable = $p
            break
        }
    }
}

if ($mvnExecutable) {
    Write-Host "[INFO] Running Spring Boot backend via $mvnExecutable spring-boot:run..." -ForegroundColor Green
    & $mvnExecutable spring-boot:run
} else {
    Write-Host "[ERROR] Neither mvnw nor mvn command was found." -ForegroundColor Red
    Write-Host "Please install Apache Maven or use Spring Tool Suite (STS) / IntelliJ IDEA to run the project." -ForegroundColor Yellow
    exit 1
}
