@echo off
title MediFlow Pharmacy - Swing GUI Desktop (MySQL JDBC)
echo ========================================================
echo  Launching MediFlow Swing GUI (JFrame, JTable, MySQL JDBC)
echo ========================================================
powershell -NoProfile -ExecutionPolicy Bypass -Command "& { $root = '%~dp0'.TrimEnd('\'); $jars = (Get-ChildItem -Path \"$root\lib\*.jar\" | ForEach-Object { $_.FullName }) -join ';'; if (-not (Test-Path \"$root\target\classes\com\mediflow\pharmacy\gui\MediFlowSwingApp.class\")) { & powershell -NoProfile -ExecutionPolicy Bypass -File \"$root\run.ps1\" } else { java -cp \"$root\target\classes;$root\src\main\resources;$jars\" com.mediflow.pharmacy.gui.MediFlowSwingApp } }"
pause
