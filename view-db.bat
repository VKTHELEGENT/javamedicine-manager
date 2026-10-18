@echo off
title MediFlow Database Viewer
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0view-db.ps1"
pause
