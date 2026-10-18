package com.mediflow.pharmacy.db;

import com.mediflow.pharmacy.model.LoginLog;
import com.mediflow.pharmacy.exception.PharmacyException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * CST 205 OOPJ - Module 5: Data Access Object for User Login Audit Trail
 * Records and retrieves login session records in MySQL database table `login_logs`.
 */
public class LoginLogDAO {

    // Fallback store in memory if MySQL database is temporarily unavailable
    private static final List<LoginLog> fallbackLogs = new CopyOnWriteArrayList<>();

    static {
        fallbackLogs.add(new LoginLog(1L, "faculty1", "FACULTY", LocalDateTime.now().minusHours(2), "127.0.0.1", "SUCCESS"));
        fallbackLogs.add(new LoginLog(2L, "patient1", "PATIENT", LocalDateTime.now().minusMinutes(45), "127.0.0.1", "SUCCESS"));
    }

    /**
     * Record a new user login or registration event in the database.
     */
    public LoginLog recordLogin(String username, String role, String ipAddress, String status) throws PharmacyException {
        if (username == null) return null;
        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rsKeys = null;
        try {
            con = DatabaseConnection.getConnection();
            String sql = "INSERT INTO login_logs (username, role, login_time, ip_address, status) VALUES (?, ?, ?, ?, ?)";
            pst = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            pst.setString(1, username);
            pst.setString(2, role != null ? role.toUpperCase() : "PATIENT");
            pst.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            pst.setString(4, ipAddress != null ? ipAddress : "127.0.0.1");
            pst.setString(5, status != null ? status : "SUCCESS");

            pst.executeUpdate();
            rsKeys = pst.getGeneratedKeys();
            Long id = null;
            if (rsKeys.next()) {
                id = rsKeys.getLong(1);
            }
            LoginLog log = new LoginLog(id, username, role, LocalDateTime.now(), ipAddress, status);
            fallbackLogs.add(0, log);
            return log;
        } catch (SQLException e) {
            // Graceful fallback to memory storage
            LoginLog log = new LoginLog((long) (fallbackLogs.size() + 1), username, role, LocalDateTime.now(), ipAddress, status);
            fallbackLogs.add(0, log);
            return log;
        } finally {
            DatabaseConnection.closeQuietly(rsKeys, pst, con);
        }
    }

    /**
     * Retrieve all recent login log entries in reverse chronological order.
     */
    public List<LoginLog> getAllLogs(int limit) throws PharmacyException {
        List<LoginLog> list = new ArrayList<>();
        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            String sql = "SELECT id, username, role, login_time, ip_address, status FROM login_logs ORDER BY id DESC LIMIT ?";
            pst = con.prepareStatement(sql);
            pst.setInt(1, limit > 0 ? limit : 50);
            rs = pst.executeQuery();
            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("login_time");
                LocalDateTime ldt = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();
                list.add(new LoginLog(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("role"),
                        ldt,
                        rs.getString("ip_address"),
                        rs.getString("status")
                ));
            }
            return list;
        } catch (SQLException e) {
            return new ArrayList<>(fallbackLogs);
        } finally {
            DatabaseConnection.closeQuietly(rs, pst, con);
        }
    }

    /**
     * Retrieve login log entries for a specific username.
     */
    public List<LoginLog> getLogsByUsername(String username, int limit) throws PharmacyException {
        List<LoginLog> list = new ArrayList<>();
        if (username == null) return list;
        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            String sql = "SELECT id, username, role, login_time, ip_address, status FROM login_logs WHERE LOWER(username) = ? ORDER BY id DESC LIMIT ?";
            pst = con.prepareStatement(sql);
            pst.setString(1, username.trim().toLowerCase());
            pst.setInt(2, limit > 0 ? limit : 20);
            rs = pst.executeQuery();
            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("login_time");
                LocalDateTime ldt = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();
                list.add(new LoginLog(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("role"),
                        ldt,
                        rs.getString("ip_address"),
                        rs.getString("status")
                ));
            }
            return list;
        } catch (SQLException e) {
            List<LoginLog> filtered = new ArrayList<>();
            for (LoginLog l : fallbackLogs) {
                if (l.getUsername() != null && l.getUsername().equalsIgnoreCase(username)) {
                    filtered.add(l);
                }
            }
            return filtered;
        } finally {
            DatabaseConnection.closeQuietly(rs, pst, con);
        }
    }
}
