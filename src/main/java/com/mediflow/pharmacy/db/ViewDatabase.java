package com.mediflow.pharmacy.db;

import java.sql.*;

/**
 * CST 205 OOPJ - Module 5: Database Inspector Tool
 * Uses JDBC and ResultSetMetaData to inspect and display relational tables in formatted ASCII grids.
 */
public class ViewDatabase {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("  MEDIFLOW RELATIONAL DATABASE VIEWER (MYSQL JDBC)                              ");
        System.out.println("  Active Engine: " + DatabaseConnection.getActiveDatabaseEngine());
        System.out.println("================================================================================");

        try (Connection con = DatabaseConnection.getConnection()) {
            displayTable(con, "medicines", "SELECT id, name, quantity, available, details FROM medicines ORDER BY id ASC");
            displayTable(con, "users", "SELECT id, username, role, email FROM users ORDER BY id ASC");
            displayTable(con, "reset_codes", "SELECT id, email, code, expires, user_type, username FROM reset_codes ORDER BY id DESC");
            displayTable(con, "login_logs", "SELECT id, username, role, login_time, ip_address, status FROM login_logs ORDER BY id DESC");
        } catch (SQLException e) {
            System.out.println("\n[Note] MySQL server is not currently reachable on localhost:3306.");
            System.out.println("Displaying active in-memory cache data (fallback storage):\n");
            displayFallbackData();
        }
    }

    private static void displayFallbackData() {
        try {
            System.out.println("TABLE: MEDICINES (IN-MEMORY CACHE)");
            System.out.println("--------------------------------------------------------------------------------");
            System.out.println(String.format("%-6s %-20s %-10s %-12s %-25s", "ID", "NAME", "QUANTITY", "AVAILABLE", "DETAILS"));
            System.out.println("------ -------------------- ---------- ------------ -------------------------");
            MedicineDAO medDao = new MedicineDAO();
            for (com.mediflow.pharmacy.model.Medicine m : medDao.getAllMedicines()) {
                System.out.println(String.format("%-6s %-20s %-10s %-12s %-25s",
                        m.getId(),
                        truncate(m.getName(), 19),
                        m.getQuantity(),
                        m.getAvailable() ? "In Stock" : "Out of Stock",
                        truncate(m.getDetails(), 24)));
            }

            System.out.println("\nTABLE: LOGIN_LOGS (IN-MEMORY CACHE)");
            System.out.println("--------------------------------------------------------------------------------");
            System.out.println(String.format("%-6s %-15s %-12s %-22s %-15s %-10s", "ID", "USERNAME", "ROLE", "LOGIN TIME", "IP ADDRESS", "STATUS"));
            System.out.println("------ --------------- ------------ ---------------------- --------------- ----------");
            LoginLogDAO logDao = new LoginLogDAO();
            for (com.mediflow.pharmacy.model.LoginLog l : logDao.getAllLogs(20)) {
                System.out.println(String.format("%-6s %-15s %-12s %-22s %-15s %-10s",
                        l.getId(),
                        truncate(l.getUsername(), 14),
                        truncate(l.getRole(), 11),
                        truncate(l.getLoginTimeStr(), 21),
                        truncate(l.getIpAddress(), 14),
                        truncate(l.getStatus(), 9)));
            }
        } catch (Exception ex) {
            System.err.println("Could not display fallback data: " + ex.getMessage());
        }
    }

    private static void displayTable(Connection con, String tableName, String query) {
        System.out.println("\nTABLE: " + tableName.toUpperCase());
        System.out.println("--------------------------------------------------------------------------------");
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(query)) {

            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();

            // Print Header
            StringBuilder header = new StringBuilder();
            for (int i = 1; i <= columnCount; i++) {
                String col = meta.getColumnLabel(i).toUpperCase();
                header.append(String.format("%-20s ", truncate(col, 19)));
            }
            System.out.println(header);

            StringBuilder separator = new StringBuilder();
            for (int i = 1; i <= columnCount; i++) {
                separator.append("-------------------- ");
            }
            System.out.println(separator);

            // Print Rows
            int rowCount = 0;
            while (rs.next()) {
                rowCount++;
                StringBuilder row = new StringBuilder();
                for (int i = 1; i <= columnCount; i++) {
                    Object obj = rs.getObject(i);
                    String val;
                    if (obj instanceof Clob) {
                        Clob clob = (Clob) obj;
                        val = clob.getSubString(1, (int) Math.min(100, clob.length()));
                    } else {
                        val = String.valueOf(obj);
                    }
                    row.append(String.format("%-20s ", truncate(val, 19)));
                }
                System.out.println(row);
            }
            if (rowCount == 0) {
                System.out.println("(No records found in " + tableName + ")");
            }
            System.out.println("Total rows: " + rowCount);
        } catch (SQLException e) {
            System.err.println("Could not display table " + tableName + ": " + e.getMessage());
        }
    }

    private static String truncate(String str, int maxLen) {
        if (str == null) return "null";
        str = str.replace("\n", " ").replace("\r", "");
        if (str.length() <= maxLen) return str;
        return str.substring(0, maxLen - 3) + "...";
    }
}
