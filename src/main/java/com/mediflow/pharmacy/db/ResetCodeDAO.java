package com.mediflow.pharmacy.db;

import com.mediflow.pharmacy.model.ResetCode;
import com.mediflow.pharmacy.exception.PharmacyException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * CST 205 OOPJ - Module 5: Data Access Object for Password Reset Codes using JDBC.
 */
public class ResetCodeDAO {

    // Fallback store in memory if MySQL database is temporarily unavailable
    private static final List<ResetCode> fallbackCodes = new CopyOnWriteArrayList<>();

    public void save(ResetCode rc) throws PharmacyException {
        if (rc == null) return;
        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rsKeys = null;
        try {
            con = DatabaseConnection.getConnection();
            String sql = "INSERT INTO reset_codes (email, code, expires, user_type, username, used) VALUES (?, ?, ?, ?, ?, ?)";
            pst = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            pst.setString(1, rc.getEmail());
            pst.setString(2, rc.getCode());
            pst.setTimestamp(3, Timestamp.valueOf(rc.getExpires()));
            pst.setString(4, rc.getUserType());
            pst.setString(5, rc.getUsername());
            pst.setBoolean(6, false);
            pst.executeUpdate();
            rsKeys = pst.getGeneratedKeys();
            if (rsKeys.next()) {
                rc.setId(rsKeys.getLong(1));
            }
            fallbackCodes.add(rc);
        } catch (SQLException e) {
            if (rc.getId() == null) {
                rc.setId((long) (fallbackCodes.size() + 1));
            }
            fallbackCodes.add(rc);
        } finally {
            DatabaseConnection.closeQuietly(rsKeys, pst, con);
        }
    }

    public ResetCode findLatestByEmail(String email) throws PharmacyException {
        if (email == null) return null;
        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            String sql = "SELECT id, email, code, expires, user_type, username FROM reset_codes " +
                         "WHERE LOWER(email) = ? AND used = FALSE ORDER BY expires DESC";
            pst = con.prepareStatement(sql);
            pst.setString(1, email.trim().toLowerCase());
            rs = pst.executeQuery();
            if (rs.next()) {
                return new ResetCode(
                        rs.getLong("id"),
                        rs.getString("email"),
                        rs.getString("code"),
                        rs.getTimestamp("expires").toLocalDateTime(),
                        rs.getString("user_type"),
                        rs.getString("username")
                );
            }
            return null;
        } catch (SQLException e) {
            String targetEmail = email.trim().toLowerCase();
            for (int i = fallbackCodes.size() - 1; i >= 0; i--) {
                ResetCode c = fallbackCodes.get(i);
                if (c.getEmail() != null && c.getEmail().equalsIgnoreCase(targetEmail)) {
                    return c;
                }
            }
            return null;
        } finally {
            DatabaseConnection.closeQuietly(rs, pst, con);
        }
    }

    public void deleteByEmail(String email) throws PharmacyException {
        if (email == null) return;
        Connection con = null;
        PreparedStatement pst = null;
        try {
            con = DatabaseConnection.getConnection();
            pst = con.prepareStatement("DELETE FROM reset_codes WHERE LOWER(email) = ?");
            pst.setString(1, email.trim().toLowerCase());
            pst.executeUpdate();
            fallbackCodes.removeIf(c -> c.getEmail() != null && c.getEmail().equalsIgnoreCase(email.trim()));
        } catch (SQLException e) {
            fallbackCodes.removeIf(c -> c.getEmail() != null && c.getEmail().equalsIgnoreCase(email.trim()));
        } finally {
            DatabaseConnection.closeQuietly(pst, con);
        }
    }

    public void deleteById(Long id) throws PharmacyException {
        if (id == null) return;
        Connection con = null;
        PreparedStatement pst = null;
        try {
            con = DatabaseConnection.getConnection();
            pst = con.prepareStatement("DELETE FROM reset_codes WHERE id = ?");
            pst.setLong(1, id);
            pst.executeUpdate();
            fallbackCodes.removeIf(c -> c.getId() != null && c.getId().equals(id));
        } catch (SQLException e) {
            fallbackCodes.removeIf(c -> c.getId() != null && c.getId().equals(id));
        } finally {
            DatabaseConnection.closeQuietly(pst, con);
        }
    }
}
