-- ========================================================
-- MediFlow Pharmacy Management System - MySQL Setup Script
-- CST 205 OOPJ: Relational Database Schema & Data Seeding
-- ========================================================

CREATE DATABASE IF NOT EXISTS pharmacy_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE pharmacy_db;

-- 1. Medicines Table
CREATE TABLE IF NOT EXISTS medicines (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    quantity INT NOT NULL,
    available BOOLEAN NOT NULL DEFAULT TRUE,
    details TEXT
);

-- 2. Users Table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    role VARCHAR(50) NOT NULL
);

-- 3. Password Reset Codes Table
CREATE TABLE IF NOT EXISTS reset_codes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    code VARCHAR(50) NOT NULL,
    expires TIMESTAMP NOT NULL,
    user_type VARCHAR(50) NOT NULL,
    username VARCHAR(255) NOT NULL,
    used BOOLEAN DEFAULT FALSE
);

-- 4. User Login Audit History Table
CREATE TABLE IF NOT EXISTS login_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    login_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(100),
    status VARCHAR(50) DEFAULT 'SUCCESS'
);

-- 5. Initial Seed Data (Users)
INSERT IGNORE INTO users (username, password_hash, email, role) VALUES
('patient1', '$2a$12$KgjIoviVuyDjRDqfYyvbw.It/GX.f94m6lCRrybamgG474s1HhfuW', 'patient1@example.com', 'PATIENT'),
('patient2', '$2a$12$KgjIoviVuyDjRDqfYyvbw.It/GX.f94m6lCRrybamgG474s1HhfuW', 'patient2@example.com', 'PATIENT'),
('patient_admin', '$2a$12$GSYDEHPqAcKCwEJlLvFBJOF2s7tnSjR9DfV//M9K7KF0vKMGZtqui', 'admin@example.com', 'PATIENT'),
('faculty1', '$2a$12$qDqP55P2jp2O03DpfQV/r.tpxT1FdWEMclNG4EETfco7fGzUiv7Pm', 'faculty1@example.com', 'FACULTY'),
('admin', '$2a$12$qDqP55P2jp2O03DpfQV/r.tpxT1FdWEMclNG4EETfco7fGzUiv7Pm', 'admin@example.com', 'FACULTY');

-- 6. Initial Seed Data (Medicines)
INSERT IGNORE INTO medicines (id, name, quantity, available, details) VALUES
(1, 'Paracetamol', 20, TRUE, 'Pain reliever and fever reducer. Dosage: 500mg tablets. Take 1-2 tablets every 4-6 hours as needed.'),
(2, 'Ibuprofen', 14, TRUE, 'Anti-inflammatory medication. Dosage: 200mg tablets. Take with food. Maximum 1200mg per day.'),
(3, 'Amoxicillin', 25, TRUE, 'Antibiotic for bacterial infections. Dosage: 250mg capsules. Take as prescribed by doctor.'),
(4, 'Aspirin', 12, TRUE, 'Blood thinner and pain reliever. Dosage: 75-100mg tablets. Take as directed by doctor.'),
(5, 'Cetirizine', 30, TRUE, 'Antihistamine for allergies. Dosage: 10mg tablets. Take one tablet daily as needed.'),
(6, 'Montasulac', 25, TRUE, 'It is medicine used for treating asthma and allergy symptoms.'),
(7, 'Dollo-650', 18, TRUE, 'Medicine used as pain killer and also fever relief. Dosage: 650mg. Take after food, maintaining 6-8 hr gaps.'),
(8, 'Fruticote', 6, FALSE, 'Used to maintain glucose levels in blood and body. Consumed before food.');
