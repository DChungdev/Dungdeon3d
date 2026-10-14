# Dungeon Delvers Online — Verify dev environment (Windows, no Docker)
# Usage: .\scripts\windows\verify-setup.ps1

$ErrorActionPreference = "Continue"
$ok = $true

function Check-Pass($msg) { Write-Host "[OK]   $msg" -ForegroundColor Green }
function Check-Fail($msg) { Write-Host "[FAIL] $msg" -ForegroundColor Red; $script:ok = $false }
function Check-Warn($msg) { Write-Host "[WARN] $msg" -ForegroundColor Yellow }

Write-Host "`n=== Dungeon Delvers — Environment Check ===`n" -ForegroundColor Cyan

# Java
try {
    $javaVer = java -version 2>&1 | Select-Object -First 1
    if ($javaVer -match 'version "(\d+)') {
        $major = [int]$Matches[1]
        if ($major -ge 21) { Check-Pass "Java $major : $javaVer" }
        else { Check-Fail "Java $major found — need Java 21+. $javaVer" }
    } else {
        Check-Pass "Java: $javaVer"
    }
} catch {
    Check-Fail "Java not found. Install JDK 21 from https://adoptium.net/"
}

# PostgreSQL psql
$psqlPaths = @(
    "C:\Program Files\PostgreSQL\16\bin\psql.exe",
    "C:\Program Files\PostgreSQL\15\bin\psql.exe",
    "psql"
)
$psql = $null
foreach ($p in $psqlPaths) {
    if ($p -eq "psql") {
        $found = Get-Command psql -ErrorAction SilentlyContinue
        if ($found) { $psql = "psql"; break }
    } elseif (Test-Path $p) {
        $psql = $p
        break
    }
}

if ($psql) {
    Check-Pass "psql found: $psql"
    $env:PGPASSWORD = "ddo"
    $dbTest = & $psql -U ddo -d dungeon_delvers -t -c "SELECT 1;" 2>&1
    if ($LASTEXITCODE -eq 0) {
        Check-Pass "PostgreSQL: dungeon_delvers accessible (user ddo)"
    } else {
        Check-Fail "PostgreSQL: cannot connect as ddo/dungeon_delvers. Run init-postgres.sql"
        Write-Host "       $dbTest" -ForegroundColor DarkGray
    }
    Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
} else {
    Check-Fail "PostgreSQL psql not found. Install from https://www.postgresql.org/download/windows/"
}

# Redis / Memurai (optional)
$redisOk = $false
foreach ($cmd in @("memurai-cli", "redis-cli")) {
    $found = Get-Command $cmd -ErrorAction SilentlyContinue
    if ($found) {
        $ping = & $cmd ping 2>&1
        if ($ping -match "PONG") {
            Check-Pass "Redis/Memurai: $cmd ping PONG"
            $redisOk = $true
            break
        }
    }
}
if (-not $redisOk) {
    Check-Warn "Redis not running — OK for Phase 0. Use Spring profile: local-native"
}

# Config file
$nativeConfig = "server\src\main\resources\application-local-native.yml"
if (Test-Path $nativeConfig) {
    Check-Pass "Config: application-local-native.yml exists"
} else {
    $example = "server\src\main\resources\application-local-native.yml.example"
    if (Test-Path $example) {
        Check-Warn "Copy $example -> application-local-native.yml"
    }
}

# Maven wrapper (after scaffold)
if (Test-Path "server\mvnw.cmd") {
    Check-Pass "Maven wrapper: server\mvnw.cmd"
} else {
    Check-Warn "Server not scaffolded yet — Spring Boot project pending (Phase 0 week 1)"
}

Write-Host ""
if ($ok) {
    Write-Host "Result: READY for Phase 0 (use profile local-native)" -ForegroundColor Green
} else {
    Write-Host "Result: FIX failures above before starting server" -ForegroundColor Red
}
Write-Host ""
