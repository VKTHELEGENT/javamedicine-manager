# MediFlow - Medical Pharmacy Management System

A medical pharmacy management web platform designed for patient catalog search, inventory restocking, medicine availability monitoring, and staff administration.

## ✨ Features
- **Patient Access Portal:** Live stock search, interactive autocomplete with highlight matching, dosage & usage detail view, and stock status indicators.
- **Faculty & Staff Control Center:** Real-time medicine management, quantity stepper, availability toggles, editing medication information, and stock deletion with confirmation dialog.
- **Hybrid Online/Offline Architecture:**
  - **Online Mode:** Connected to Java Spring Boot & H2 Relational Database.
  - **Offline/Cloud Static Mode (Vercel):** Seamless client-side database emulation via `localStorage` pre-seeded with initial catalog records.
- **Theme Switcher:** Light & Dark modes with persistent user preference.
- **Security:** Bcrypt password hashing and session management.

---

## 🚀 Live Deployment on Vercel
1. Go to [Vercel](https://vercel.com/new).
2. Click **"Import Git Repository"** and select `VKTHELEGENT/javamedicine-manager`.
3. Framework Preset: **Other**.
4. Root Directory: `./` (default).
5. Click **Deploy**.

---

## 💻 Local Running (Java Spring Boot Backend)
Double-click `run.bat` or execute in PowerShell:
```powershell
.\run.ps1
```
The server will start on: **http://localhost:8080/**

---

## 🔑 Demo Accounts
| Role | Username | Password |
|---|---|---|
| **Patient Access** | `patient1` | `patient123` |
| **Faculty Staff** | `admin` | `faculty123` |
| **Faculty Staff** | `faculty1` | `faculty123` |
