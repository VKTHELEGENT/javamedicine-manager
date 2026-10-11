# MediFlow Startup Script
$ErrorActionPreference = "Stop"
$root = $PSScriptRoot

# Free port 8080 if an older server instance is still bound to it
$existingPort = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
if ($existingPort) {
    $existingPid = $existingPort.OwningProcess | Select-Object -First 1
    $proc = Get-Process -Id $existingPid -ErrorAction SilentlyContinue
    if ($proc -and $proc.ProcessName -eq 'java') {
        Write-Host "Stopping previous server instance on port 8080 (PID $existingPid)..." -ForegroundColor Yellow
        Stop-Process -Id $existingPid -Force -ErrorAction SilentlyContinue
        Start-Sleep -Seconds 1
    }
}

Write-Host "Compiling Java backend sources..." -ForegroundColor Cyan
$jars = (Get-ChildItem -Path "$env:USERPROFILE\.m2\repository" -Filter "*.jar" -Recurse | Where-Object { 
    $_.FullName -notmatch "slf4j-api-1\.7" -and 
    $_.FullName -notmatch "maven-" -and 
    $_.FullName -notmatch "plexus-" -and
    $_.FullName -notmatch "surefire" -and
    $_.FullName -notmatch "devtools" -and
    $_.FullName -notmatch "qdox"
} | ForEach-Object { $_.FullName }) -join ";"

if (-not (Test-Path "$root\target\classes")) {
    New-Item -ItemType Directory -Path "$root\target\classes" -Force | Out-Null
}

$sources = (Get-ChildItem -Path "$root\src\main\java" -Filter "*.java" -Recurse | ForEach-Object { $_.FullName })
javac --release 21 -cp $jars -d "$root\target\classes" $sources

# Copy static frontend files to target and resources
if (-not (Test-Path "$root\target\classes\static")) {
    New-Item -ItemType Directory -Path "$root\target\classes\static" -Force | Out-Null
}
@("app.js", "index.html", "style.css") | ForEach-Object {
    $rootFile = "$root\$_"
    $resFile = "$root\src\main\resources\static\$_"
    $tgtFile = "$root\target\classes\static\$_"
    if ((Test-Path $resFile) -and (Test-Path $rootFile)) {
        if ((Get-Item $resFile).LastWriteTime -gt (Get-Item $rootFile).LastWriteTime) {
            Copy-Item -Path $resFile -Destination $rootFile -Force
        } else {
            Copy-Item -Path $rootFile -Destination $resFile -Force
        }
    }
    if (Test-Path $rootFile) {
        Copy-Item -Path $rootFile -Destination $tgtFile -Force
    }
}

Write-Host "========================================================" -ForegroundColor Green
Write-Host "  Launching MediFlow on http://localhost:8080/          " -ForegroundColor Green
Write-Host "  Demo Credentials:                                    " -ForegroundColor Yellow
Write-Host "    Patient Access:  patient1 / patient123             " -ForegroundColor Yellow
Write-Host "    Faculty Staff:   admin / faculty123                " -ForegroundColor Yellow
Write-Host "========================================================" -ForegroundColor Green

# Automatically open browser when server is ready
Start-Job -ScriptBlock {
    for ($i = 0; $i -lt 30; $i++) {
        Start-Sleep -Seconds 1
        $conn = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
        if ($conn) {
            Start-Process "http://localhost:8080/"
            break
        }
    }
} | Out-Null

$env:CLASSPATH = "$root\target\classes;$root\src\main\resources;$jars"
java com.mediflow.pharmacy.PharmacyApplication
