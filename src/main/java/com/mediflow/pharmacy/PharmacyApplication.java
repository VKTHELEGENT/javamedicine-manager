package com.mediflow.pharmacy;

import com.mediflow.pharmacy.db.DatabaseConnection;
import com.mediflow.pharmacy.gui.MediFlowSwingApp;
import com.mediflow.pharmacy.server.MediFlowHttpServer;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;

/**
 * Main Application Entry Point
 * Implements CST 205 OOPJ Syllabus Concepts:
 * - Module 2: Java Classes, Objects, Static Methods, Encapsulation
 * - Module 3: Packages, Exception Handling (try-catch-finally)
 * - Module 4 & 5: Swing GUI Desktop Controls (JFrame, JTable, JButton, ActionListener)
 * - Module 5: MySQL Database Connectivity using pure JDBC (7 Steps of JDBC)
 */
public class PharmacyApplication {

    public static final int DEFAULT_PORT = 8080;

    public static void main(String[] args) {
        System.out.println("========================================================");
        System.out.println("  Starting MediFlow Pharmacy System                    ");
        System.out.println("  Core Engine: Pure Core Java & MySQL JDBC             ");
        System.out.println("  Demo Credentials:                                    ");
        System.out.println("    Patient Access:  patient1 / patient123             ");
        System.out.println("    Faculty Staff:   admin / faculty123  or  faculty1 / faculty123");
        System.out.println("========================================================");

        // 1. Initialize relational schema & seed initial records via JDBC
        System.out.println("Initializing relational database via JDBC...");
        DatabaseConnection.initializeDatabase();

        // 2. Start HTTP Server for Web Access
        int port = DEFAULT_PORT;
        try {
            MediFlowHttpServer server = new MediFlowHttpServer(port);
            server.start();
            System.out.println("Web Platform accessible on: http://localhost:" + port + "/");
        } catch (Exception e) {
            System.err.println("Error starting HTTP Server: " + e.getMessage());
            e.printStackTrace();
        }

        // 3. Launch Swing Desktop GUI (Module 5) if running in a desktop environment
        boolean enableGui = true;
        for (String arg : args) {
            if ("--no-gui".equalsIgnoreCase(arg) || "--headless".equalsIgnoreCase(arg)) {
                enableGui = false;
            }
        }

        if (enableGui && !GraphicsEnvironment.isHeadless()) {
            System.out.println("Launching Swing Desktop GUI (JFrame & JTable)...");
            SwingUtilities.invokeLater(() -> {
                try {
                    MediFlowSwingApp gui = new MediFlowSwingApp();
                    gui.setVisible(true);
                } catch (Exception e) {
                    System.err.println("Could not launch Swing GUI: " + e.getMessage());
                }
            });
        }
    }
}
