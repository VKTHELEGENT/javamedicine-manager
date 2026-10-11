@echo off
title MediFlow Pharmacy Manager
echo ========================================================
echo  Starting MediFlow Pharmacy Server on http://localhost:8080/
echo  Demo Credentials:
echo    Patient Access:  patient1 / patient123
echo    Faculty Staff:   admin / faculty123  or  faculty1 / faculty123
echo ========================================================
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0run.ps1"
pause
