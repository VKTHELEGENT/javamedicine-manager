$root = $PSScriptRoot
if (-not $root) { $root = (Get-Location).Path }
$jars = (Get-ChildItem -Path "$root\lib\*.jar" | ForEach-Object { $_.FullName }) -join ";"
if (-not $jars) {
    $jars = (Get-ChildItem -Path "$env:USERPROFILE\.m2\repository" -Filter "*.jar" -Recurse | Where-Object {
        $_.Name -match "mysql" -or $_.Name -match "jbcrypt"
    } | ForEach-Object { $_.FullName }) -join ";"
}

# Compile if needed
if (-not (Test-Path "$root\target\classes\com\mediflow\pharmacy\db\ViewDatabase.class")) {
    Write-Host "Compiling ViewDatabase tool..." -ForegroundColor Cyan
    $sources = (Get-ChildItem -Path "$root\src\main\java" -Filter "*.java" -Recurse | ForEach-Object { $_.FullName })
    javac -cp $jars -d "$root\target\classes" $sources
}

java -cp "$root\target\classes;$root\src\main\resources;$jars" com.mediflow.pharmacy.db.ViewDatabase
