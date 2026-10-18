# MediFlow Startup Script - MySQL & Core Java JDBC Edition
$ErrorActionPreference = "Stop"
$root = $PSScriptRoot
if (-not $root) { $root = (Get-Location).Path }

# Free port 8080 if an older server instance is still bound to it
$existingPort = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
if ($existingPort) {
    $pidsToKill = $existingPort.OwningProcess | Select-Object -Unique
    foreach ($pId in $pidsToKill) {
        $proc = Get-Process -Id $pId -ErrorAction SilentlyContinue
        if ($proc -and $proc.ProcessName -match '^java') {
            Write-Host "Stopping previous server instance on port 8080 (PID $pId)..." -ForegroundColor Yellow
            Stop-Process -Id $pId -Force -ErrorAction SilentlyContinue
        }
    }
    Start-Sleep -Seconds 1
}

Write-Host "Resolving Java dependencies (MySQL Connector/J & BCrypt)..." -ForegroundColor Cyan
$libJars = Get-ChildItem -Path "$root\lib\*.jar" -ErrorAction SilentlyContinue
if ($libJars) {
    $jars = ($libJars | ForEach-Object { $_.FullName }) -join ";"
} else {
    $m2Jars = Get-ChildItem -Path "$env:USERPROFILE\.m2\repository" -Filter "*.jar" -Recurse -ErrorAction SilentlyContinue | Where-Object {
        $_.Name -match "mysql" -or $_.Name -match "jbcrypt"
    }
    $jars = ($m2Jars | ForEach-Object { $_.FullName }) -join ";"
}

Write-Host "Compiling Java backend sources..." -ForegroundColor Cyan
if (-not (Test-Path "$root\target\classes")) {
    New-Item -ItemType Directory -Path "$root\target\classes" -Force | Out-Null
}

$sources = (Get-ChildItem -Path "$root\src\main\java" -Filter "*.java" -Recurse | ForEach-Object { $_.FullName })
javac -cp $jars -d "$root\target\classes" $sources

# Copy static frontend files (app.js, index.html, style.css)
if (-not (Test-Path "$root\src\main\resources\static")) {
    New-Item -ItemType Directory -Path "$root\src\main\resources\static" -Force | Out-Null
}
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
    } elseif (Test-Path $rootFile) {
        Copy-Item -Path $rootFile -Destination $resFile -Force
    }
    if (Test-Path $rootFile) {
        Copy-Item -Path $rootFile -Destination $tgtFile -Force
    }
}

# Copy database properties to resources and target classes
$dbPropRoot = "$root\database.properties"
if (Test-Path $dbPropRoot) {
    Copy-Item -Path $dbPropRoot -Destination "$root\src\main\resources\database.properties" -Force
    Copy-Item -Path $dbPropRoot -Destination "$root\target\classes\database.properties" -Force
}

Write-Host "========================================================" -ForegroundColor Green
Write-Host "  Launching MediFlow on http://localhost:8080/          " -ForegroundColor Green
Write-Host "  Database Engine: MySQL Database (via standard JDBC)   " -ForegroundColor Cyan
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
java -cp "$root\target\classes;$root\src\main\resources;$jars" com.mediflow.pharmacy.PharmacyApplication
