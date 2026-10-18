package com.mediflow.pharmacy.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.util.Properties;
import com.mediflow.pharmacy.util.PasswordUtil;

/**
 * CST 205 OOPJ - Module 5: Java DataBase Connectivity (JDBC) using MySQL Database.
 * 
 * Implements the standard 7 steps of JDBC:
 * Step 1: Import the Packages (java.sql.*)
 * Step 2: Loading the drivers using Class.forName("com.mysql.cj.jdbc.Driver")
 * Step 3: Establish a connection using DriverManager.getConnection()
 * Step 4: Create a Statement / PreparedStatement
 * Step 5: Execute the query using executeQuery() or executeUpdate()
 * Step 6: Retrieve and process ResultSet
 * Step 7: Close the connections
 */
public class DatabaseConnection {

    private static final String DRIVER = "com.mysql.cj.jdbc.Driver";
    private static String url = "jdbc:mysql://localhost:3306/pharmacy_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static String username = "root";
    private static String password = "";

    // Load configuration & Driver
    static {
        loadConfiguration();
        try {
            // Step 2: Load MySQL Driver
            Class.forName(DRIVER);
            System.out.println("JDBC Step 2: MySQL driver loaded successfully (" + DRIVER + ").");
        } catch (ClassNotFoundException e) {
            System.err.println("JDBC Step 2 Error: MySQL Driver not found: " + e.getMessage());
        }
    }

    private static void loadConfiguration() {
        Properties props = new Properties();
        InputStream is = null;
        try {
            File[] candidates = new File[]{
                    new File("database.properties"),
                    new File("src/main/resources/database.properties")
            };
            for (File f : candidates) {
                if (f.exists() && !f.isDirectory()) {
                    is = new FileInputStream(f);
                    break;
                }
            }
            if (is == null) {
                is = DatabaseConnection.class.getClassLoader().getResourceAsStream("database.properties");
            }

            if (is != null) {
                props.load(is);
                url = props.getProperty("db.mysql.url", url).trim();
                username = props.getProperty("db.mysql.username", username).trim();
                password = props.getProperty("db.mysql.password", password).trim();
            }
        } catch (Exception ignored) {
        } finally {
            closeQuietly(is);
        }
    }

    /**
     * Step 3: Establish Connection to MySQL
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    public static String getActiveDatabaseEngine() {
        return "MySQL (" + DRIVER + ")";
    }

    /**
     * Initializes relational tables and pre-seeds initial pharmacy catalog and accounts.
     * Uses standard SQL statements: CREATE TABLE, SELECT, INSERT INTO.
     */
    public static void initializeDatabase() {
        Connection con = null;
        Statement st = null;
        try {
            // Step 3: Establish Connection
            con = getConnection();
            // Step 4: Create Statement
            st = con.createStatement();

            // SQL CREATE TABLE Statements for MySQL
            String createMedicinesTable = "CREATE TABLE IF NOT EXISTS medicines (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(255) NOT NULL, " +
                    "quantity INT NOT NULL, " +
                    "available BOOLEAN NOT NULL DEFAULT TRUE, " +
                    "details TEXT" +
                    ")";
            st.executeUpdate(createMedicinesTable);

            String createUsersTable = "CREATE TABLE IF NOT EXISTS users (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "username VARCHAR(255) NOT NULL UNIQUE, " +
                    "password_hash VARCHAR(255) NOT NULL, " +
                    "email VARCHAR(255), " +
                    "role VARCHAR(50) NOT NULL" +
                    ")";
            st.executeUpdate(createUsersTable);

            String createResetCodesTable = "CREATE TABLE IF NOT EXISTS reset_codes (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "email VARCHAR(255) NOT NULL, " +
                    "code VARCHAR(50) NOT NULL, " +
                    "expires TIMESTAMP NOT NULL, " +
                    "user_type VARCHAR(50) NOT NULL, " +
                    "username VARCHAR(255) NOT NULL, " +
                    "used BOOLEAN DEFAULT FALSE" +
                    ")";
            st.executeUpdate(createResetCodesTable);

            String createLoginLogsTable = "CREATE TABLE IF NOT EXISTS login_logs (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "username VARCHAR(255) NOT NULL, " +
                    "role VARCHAR(50) NOT NULL, " +
                    "login_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "ip_address VARCHAR(100), " +
                    "status VARCHAR(50) DEFAULT 'SUCCESS'" +
                    ")";
            st.executeUpdate(createLoginLogsTable);

            System.out.println("JDBC Step 5: MySQL Database schema initialized successfully.");

            // Seed Users if table is empty
            seedUsers(con);

            // Seed Medicines if table is empty
            seedMedicines(con);

        } catch (SQLException e) {
            System.err.println("MySQL database connection/initialization error: " + e.getMessage());
            System.err.println("Please verify MySQL is running on localhost:3306 with credentials in database.properties");
        } finally {
            // Step 7: Close Statement and Connection
            closeQuietly(st, con);
        }
    }

    private static void seedUsers(Connection con) {
        Statement st = null;
        ResultSet rs = null;
        PreparedStatement pst = null;
        try {
            st = con.createStatement();
            rs = st.executeQuery("SELECT COUNT(*) AS total FROM users");
            if (rs.next() && rs.getInt("total") == 0) {
                System.out.println("Seeding users accounts in MySQL via JDBC INSERT statements...");
                String insertSql = "INSERT INTO users (username, password_hash, email, role) VALUES (?, ?, ?, ?)";
                pst = con.prepareStatement(insertSql);

                String patientHash = PasswordUtil.hashPassword("patient123");
                String facultyHash = PasswordUtil.hashPassword("faculty123");
                String adminHash = PasswordUtil.hashPassword("admin123");

                String[][] seedData = {
                        {"patient1", patientHash, "patient1@example.com", "PATIENT"},
                        {"patient2", patientHash, "patient2@example.com", "PATIENT"},
                        {"patient_admin", adminHash, "admin@example.com", "PATIENT"},
                        {"faculty1", facultyHash, "faculty1@example.com", "FACULTY"},
                        {"admin", facultyHash, "admin@example.com", "FACULTY"}
                };

                for (String[] u : seedData) {
                    pst.setString(1, u[0]);
                    pst.setString(2, u[1]);
                    pst.setString(3, u[2]);
                    pst.setString(4, u[3]);
                    pst.executeUpdate();
                }
                System.out.println("Users seeded successfully in MySQL.");
            }
        } catch (SQLException e) {
            System.err.println("MySQL User seeding error: " + e.getMessage());
        } finally {
            closeQuietly(rs, st, pst);
        }
    }

    private static void seedMedicines(Connection con) {
        Statement st = null;
        ResultSet rs = null;
        PreparedStatement pst = null;
        try {
            st = con.createStatement();
            rs = st.executeQuery("SELECT COUNT(*) AS total FROM medicines");
            if (rs.next() && rs.getInt("total") == 0) {
                System.out.println("Seeding medicine catalog in MySQL via JDBC INSERT statements...");
                String insertSql = "INSERT INTO medicines (name, quantity, available, details) VALUES (?, ?, ?, ?)";
                pst = con.prepareStatement(insertSql);

                Object[][] seedMeds = {
                        {"Paracetamol", 20, true, "Pain reliever and fever reducer. Dosage: 500mg tablets. Take 1-2 tablets every 4-6 hours as needed."},
                        {"Ibuprofen", 14, true, "Anti-inflammatory medication. Dosage: 200mg tablets. Take with food. Maximum 1200mg per day."},
                        {"Amoxicillin", 25, true, "Antibiotic for bacterial infections. Dosage: 250mg capsules. Take as prescribed by doctor."},
                        {"Aspirin", 12, true, "Blood thinner and pain reliever. Dosage: 75-100mg tablets. Take as directed by doctor."},
                        {"Cetirizine", 30, true, "Antihistamine for allergies. Dosage: 10mg tablets. Take one tablet daily as needed."},
                        {"Montasulac", 25, true, "It is medicine used for treating asthma and allergy symptoms."},
                        {"Dollo-650", 18, true, "Medicine used as pain killer and also fever relief. Dosage: 650mg. Take after food, maintaining 6-8 hr gaps."},
                        {"Fruticote", 6, false, "Used to maintain glucose levels in blood and body. Consumed before food."}
                };

                for (Object[] m : seedMeds) {
                    pst.setString(1, (String) m[0]);
                    pst.setInt(2, (Integer) m[1]);
                    pst.setBoolean(3, (Boolean) m[2]);
                    pst.setString(4, (String) m[3]);
                    pst.executeUpdate();
                }
                System.out.println("Medicines catalog seeded successfully in MySQL.");
            }
        } catch (SQLException e) {
            System.err.println("MySQL Medicine seeding error: " + e.getMessage());
        } finally {
            closeQuietly(rs, st, pst);
        }
    }

    /**
     * Step 7: Close helper methods
     */
    public static void closeQuietly(AutoCloseable... closeables) {
        for (AutoCloseable c : closeables) {
            if (c != null) {
                try {
                    c.close();
                } catch (Exception ignored) {
                }
            }
        }
    }
}
