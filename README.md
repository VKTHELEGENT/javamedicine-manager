# MediFlow - Medical Pharmacy Management System (Java OOP, Swing & MySQL JDBC)

A comprehensive pharmacy management application built strictly aligned with the **CST 205 Object Oriented Programming with Java (OOPJ)** curriculum, featuring both a modern Web Platform and a standalone Desktop **Java Swing GUI**, backed by standard **Java Database Connectivity (JDBC)** and **MySQL** relational database.

---

## 📚 CST 205 OOPJ Syllabus Concepts Implemented

### 1. Object-Oriented Programming (Module 2)
- **Class and Object Fundamentals:** Entities defined with states and behaviors (`Medicine`, `User`, `ResetCode`).
- **Constructors:** Default constructors and parameterized constructors with constructor chaining using `this()`.
- **Reference Variables & The `this` Keyword:** Explicit use of `this.field` for instance variable initialization.
- **Encapsulation & Access Control:** Private instance variables with public getter and setter methods.
- **Inheritance & Polymorphism:** Custom exception hierarchy (`PharmacyException extends Exception`) invoking `super()`, method overriding (`@Override public String toString()`).

### 2. Packages & Exception Handling (Module 3)
- **Package Architecture:**
  - `com.mediflow.pharmacy.model`
  - `com.mediflow.pharmacy.db`
  - `com.mediflow.pharmacy.server`
  - `com.mediflow.pharmacy.gui`
  - `com.mediflow.pharmacy.exception`
  - `com.mediflow.pharmacy.util`
- **User-Defined Exception:** `PharmacyException` with `super(message)`.
- **Exception Handling Mechanism:** Explicit `try`, `catch`, `finally`, `throw`, and `throws` with resource closing.

### 3. Java Swing GUI Desktop Controls (Module 4 & 5)
- **Top-Level Container:** `JFrame` window container (`MediFlowSwingApp`).
- **Swing Components:** `JLabel`, `JTextField`, `JTextArea`, `JButton`, `JCheckBox`, `JTable`, and `JScrollPane`.
- **Layout Managers:** `BorderLayout`, `FlowLayout`, and `GridLayout`.
- **Event Handling:** `ActionListener` interface with `actionPerformed(ActionEvent e)`.

### 4. Java DataBase Connectivity - MySQL JDBC (Module 5)
Strictly adheres to the **7 Steps of JDBC** as specified in syllabus notes:
1. **Step 1: Import Packages** — `import java.sql.*;`
2. **Step 2: Load Driver** — `Class.forName("com.mysql.cj.jdbc.Driver");`
3. **Step 3: Establish Connection** — `Connection con = DriverManager.getConnection(url, user, password);`
4. **Step 4: Create Statement** — `Statement st = con.createStatement();` and `PreparedStatement pst = con.prepareStatement(sql);`
5. **Step 5: Execute Query** — `st.executeQuery()`, `pst.executeUpdate()`, `st.executeUpdate()`
6. **Step 6: Process Results** — `while (rs.next()) { ... }`
7. **Step 7: Close Connections** — `rs.close(); st.close(); con.close();` inside `finally` blocks.
- **SQL Statements:** `CREATE TABLE`, `INSERT INTO`, `SELECT`, `UPDATE`, and `DELETE`.

---

## 🗄️ MySQL Database Setup

### Configuration File: `database.properties`
The database connection is configured in `database.properties`:
```properties
db.type=mysql
db.mysql.driver=com.mysql.cj.jdbc.Driver
db.mysql.url=jdbc:mysql://localhost:3306/pharmacy_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
db.mysql.username=root
db.mysql.password=
```
- Change `db.mysql.username` or `db.mysql.password` if your local MySQL has a custom password.
- When running, relational tables (`medicines`, `users`, `reset_codes`, `login_logs`) are automatically created and seeded via JDBC on the first connection.

### Optional: Manual MySQL Import
You can directly import `mysql_setup.sql` into **phpMyAdmin**, **MySQL Workbench**, or the MySQL CLI:
```bash
mysql -u root -p < mysql_setup.sql
```

---

## ✨ Features
- **Dual Presentation Layer:**
  - **Web Platform:** Browser-based UI accessible on `http://localhost:8080/`.
  - **Desktop Swing GUI:** Java Swing Desktop Application with interactive `JTable`, search, form editor, and login audit dialog.
- **Patient Access Portal:** Live stock search, interactive autocomplete with highlight matching, dosage details, availability indicators, and personal login history table.
- **Faculty & Staff Control Center:** Real-time medicine management, quantity stepper, availability toggles, editing medication information, stock deletion with confirmation dialog, and live user & faculty login history table.
- **User & Faculty Audit Trail:** Every login/registration automatically recorded in relational table `login_logs`.
- **Relational Persistence:** Pure JDBC connected to MySQL.
- **Offline Client-Side Mode:** Seamless database emulation via `localStorage` pre-seeded with initial catalog records.
- **Theme Switcher:** Light & Dark modes with persistent user preference.

---

## 💻 Running the Application

### Option 1: Full System (Web Platform + Swing GUI)
Double-click `run.bat` or execute in PowerShell:
```powershell
.\run.ps1
```
The server will compile and start on: **http://localhost:8080/** and open the Swing Desktop GUI window.

### Option 2: Swing GUI Desktop Window Directly
Double-click `run-swing.bat`.

---

## 🔑 Demo Accounts
| Role | Username | Password |
|---|---|---|
| **Patient Access** | `patient1` | `patient123` |
| **Patient Access** | `patient2` | `patient123` |
| **Faculty Staff** | `admin` | `faculty123` |
| **Faculty Staff** | `faculty1` | `faculty123` |
