package com.mediflow.pharmacy.db;

import com.mediflow.pharmacy.model.User;
import com.mediflow.pharmacy.exception.PharmacyException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CST 205 OOPJ - Module 5: Data Access Object for Users using pure JDBC.
 */
public class UserDAO {

    // Fallback in-memory map if MySQL is temporarily offline
    private static final Map<String, User> fallbackUsers = new ConcurrentHashMap<>();

    static {
        fallbackUsers.put("patient1", new User(1L, "patient1", "$2a$12$KgjIoviVuyDjRDqfYyvbw.It/GX.f94m6lCRrybamgG474s1HhfuW", "patient1@example.com", "PATIENT"));
        fallbackUsers.put("patient2", new User(2L, "patient2", "$2a$12$KgjIoviVuyDjRDqfYyvbw.It/GX.f94m6lCRrybamgG474s1HhfuW", "patient2@example.com", "PATIENT"));
        fallbackUsers.put("patient_admin", new User(3L, "patient_admin", "$2a$12$GSYDEHPqAcKCwEJlLvFBJOF2s7tnSjR9DfV//M9K7KF0vKMGZtqui", "admin@example.com", "PATIENT"));
        fallbackUsers.put("faculty1", new User(4L, "faculty1", "$2a$12$qDqP55P2jp2O03DpfQV/r.tpxT1FdWEMclNG4EETfco7fGzUiv7Pm", "faculty1@example.com", "FACULTY"));
        fallbackUsers.put("admin", new User(5L, "admin", "$2a$12$qDqP55P2jp2O03DpfQV/r.tpxT1FdWEMclNG4EETfco7fGzUiv7Pm", "admin@example.com", "FACULTY"));
    }

    /**
     * Find user by username (case-insensitive).
     */
    public User findByUsername(String username) throws PharmacyException {
        if (username == null || username.trim().isEmpty()) return null;
        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            pst = con.prepareStatement("SELECT id, username, password_hash, email, role FROM users WHERE LOWER(username) = ?");
            pst.setString(1, username.trim().toLowerCase());
            rs = pst.executeQuery();
            if (rs.next()) {
                return new User(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getString("email"),
                        rs.getString("role")
                );
            }
            return null;
        } catch (SQLException e) {
            return fallbackUsers.get(username.trim().toLowerCase());
        } finally {
            DatabaseConnection.closeQuietly(rs, pst, con);
        }
    }

    /**
     * Find user by email and role.
     */
    public User findByEmailAndRole(String email, String role) throws PharmacyException {
        if (email == null || role == null) return null;
        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            pst = con.prepareStatement("SELECT id, username, password_hash, email, role FROM users WHERE LOWER(email) = ? AND UPPER(role) = ?");
            pst.setString(1, email.trim().toLowerCase());
            pst.setString(2, role.trim().toUpperCase());
            rs = pst.executeQuery();
            if (rs.next()) {
                return new User(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getString("email"),
                        rs.getString("role")
                );
            }
            return null;
        } catch (SQLException e) {
            for (User u : fallbackUsers.values()) {
                if (u.getEmail() != null && u.getEmail().equalsIgnoreCase(email) &&
                    u.getRole() != null && u.getRole().equalsIgnoreCase(role)) {
                    return u;
                }
            }
            return null;
        } finally {
            DatabaseConnection.closeQuietly(rs, pst, con);
        }
    }

    /**
     * Insert new user record.
     */
    public User save(User user) throws PharmacyException {
        if (user == null) throw new PharmacyException("User object cannot be null.");
        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rsKeys = null;
        try {
            con = DatabaseConnection.getConnection();
            pst = con.prepareStatement("INSERT INTO users (username, password_hash, email, role) VALUES (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            pst.setString(1, user.getUsername());
            pst.setString(2, user.getPasswordHash());
            pst.setString(3, user.getEmail() != null ? user.getEmail() : "");
            pst.setString(4, user.getRole());
            pst.executeUpdate();
            rsKeys = pst.getGeneratedKeys();
            if (rsKeys.next()) {
                user.setId(rsKeys.getLong(1));
            }
            fallbackUsers.put(user.getUsername().trim().toLowerCase(), user);
            return user;
        } catch (SQLException e) {
            user.setId((long) (fallbackUsers.size() + 1));
            fallbackUsers.put(user.getUsername().trim().toLowerCase(), user);
            return user;
        } finally {
            DatabaseConnection.closeQuietly(rsKeys, pst, con);
        }
    }

    /**
     * Update user password hash.
     */
    public boolean updatePassword(String username, String newPasswordHash) throws PharmacyException {
        Connection con = null;
        PreparedStatement pst = null;
        try {
            con = DatabaseConnection.getConnection();
            pst = con.prepareStatement("UPDATE users SET password_hash = ? WHERE LOWER(username) = ?");
            pst.setString(1, newPasswordHash);
            pst.setString(2, username.trim().toLowerCase());
            boolean ok = pst.executeUpdate() > 0;
            User fallback = fallbackUsers.get(username.trim().toLowerCase());
            if (fallback != null) {
                fallback.setPasswordHash(newPasswordHash);
            }
            return ok;
        } catch (SQLException e) {
            User fallback = fallbackUsers.get(username.trim().toLowerCase());
            if (fallback != null) {
                fallback.setPasswordHash(newPasswordHash);
                return true;
            }
            return false;
        } finally {
            DatabaseConnection.closeQuietly(pst, con);
        }
    }
}
