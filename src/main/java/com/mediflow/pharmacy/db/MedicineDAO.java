package com.mediflow.pharmacy.db;

import com.mediflow.pharmacy.model.Medicine;
import com.mediflow.pharmacy.exception.PharmacyException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * CST 205 OOPJ - Module 5: Data Access Object using pure JDBC
 * Performs CRUD (Create, Read, Update, Delete) operations on the medicines table.
 */
public class MedicineDAO {

    // Resilient fallback storage if MySQL is temporarily offline
    private static final List<Medicine> fallbackMeds = new CopyOnWriteArrayList<>();

    static {
        fallbackMeds.add(new Medicine(1L, "Paracetamol", 20, true, "Pain reliever and fever reducer. Dosage: 500mg tablets. Take 1-2 tablets every 4-6 hours as needed."));
        fallbackMeds.add(new Medicine(2L, "Ibuprofen", 14, true, "Anti-inflammatory medication. Dosage: 200mg tablets. Take with food. Maximum 1200mg per day."));
        fallbackMeds.add(new Medicine(3L, "Amoxicillin", 25, true, "Antibiotic for bacterial infections. Dosage: 250mg capsules. Take as prescribed by doctor."));
        fallbackMeds.add(new Medicine(4L, "Aspirin", 12, true, "Blood thinner and pain reliever. Dosage: 75-100mg tablets. Take as directed by doctor."));
        fallbackMeds.add(new Medicine(5L, "Cetirizine", 30, true, "Antihistamine for allergies. Dosage: 10mg tablets. Take one tablet daily as needed."));
        fallbackMeds.add(new Medicine(6L, "Montasulac", 25, true, "It is medicine used for treating asthma and allergy symptoms."));
        fallbackMeds.add(new Medicine(7L, "Dollo-650", 18, true, "Medicine used as pain killer and also fever relief. Dosage: 650mg. Take after food, maintaining 6-8 hr gaps."));
        fallbackMeds.add(new Medicine(8L, "Fruticote", 6, false, "Used to maintain glucose levels in blood and body. Consumed before food."));
    }

    /**
     * Retrieve all medicines from the database (SQL SELECT).
     */
    public List<Medicine> getAllMedicines() throws PharmacyException {
        List<Medicine> list = new ArrayList<>();
        Connection con = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            st = con.createStatement();
            rs = st.executeQuery("SELECT id, name, quantity, available, details FROM medicines ORDER BY id ASC");
            while (rs.next()) {
                Long id = rs.getLong("id");
                String name = rs.getString("name");
                int quantity = rs.getInt("quantity");
                boolean available = rs.getBoolean("available");
                String details = rs.getString("details");
                list.add(new Medicine(id, name, quantity, available, details));
            }
            return list;
        } catch (SQLException e) {
            return new ArrayList<>(fallbackMeds);
        } finally {
            DatabaseConnection.closeQuietly(rs, st, con);
        }
    }

    /**
     * Search medicines by name or details (SQL SELECT with LIKE).
     */
    public List<Medicine> searchMedicines(String query) throws PharmacyException {
        if (query == null || query.trim().isEmpty()) {
            return getAllMedicines();
        }
        List<Medicine> list = new ArrayList<>();
        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            String sql = "SELECT id, name, quantity, available, details FROM medicines " +
                         "WHERE LOWER(name) LIKE ? OR LOWER(details) LIKE ? ORDER BY id ASC";
            pst = con.prepareStatement(sql);
            String term = "%" + query.trim().toLowerCase() + "%";
            pst.setString(1, term);
            pst.setString(2, term);
            rs = pst.executeQuery();
            while (rs.next()) {
                list.add(new Medicine(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getInt("quantity"),
                        rs.getBoolean("available"),
                        rs.getString("details")
                ));
            }
            return list;
        } catch (SQLException e) {
            List<Medicine> filtered = new ArrayList<>();
            String q = query.trim().toLowerCase();
            for (Medicine m : fallbackMeds) {
                if (m.getName().toLowerCase().contains(q) || (m.getDetails() != null && m.getDetails().toLowerCase().contains(q))) {
                    filtered.add(m);
                }
            }
            return filtered;
        } finally {
            DatabaseConnection.closeQuietly(rs, pst, con);
        }
    }

    /**
     * Retrieve single medicine by its primary key ID.
     */
    public Medicine getMedicineById(Long id) throws PharmacyException {
        if (id == null) return null;
        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            pst = con.prepareStatement("SELECT id, name, quantity, available, details FROM medicines WHERE id = ?");
            pst.setLong(1, id);
            rs = pst.executeQuery();
            if (rs.next()) {
                return new Medicine(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getInt("quantity"),
                        rs.getBoolean("available"),
                        rs.getString("details")
                );
            }
            return null;
        } catch (SQLException e) {
            for (Medicine m : fallbackMeds) {
                if (m.getId() != null && m.getId().equals(id)) {
                    return m;
                }
            }
            return null;
        } finally {
            DatabaseConnection.closeQuietly(rs, pst, con);
        }
    }

    /**
     * Insert a new medicine into the database (SQL INSERT INTO).
     */
    public Medicine addMedicine(Medicine med) throws PharmacyException {
        if (med == null) throw new PharmacyException("Medicine record cannot be null.");
        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rsKeys = null;
        try {
            con = DatabaseConnection.getConnection();
            String sql = "INSERT INTO medicines (name, quantity, available, details) VALUES (?, ?, ?, ?)";
            pst = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            pst.setString(1, med.getName());
            pst.setInt(2, med.getQuantity() != null ? med.getQuantity() : 0);
            pst.setBoolean(3, med.getAvailable() != null ? med.getAvailable() : false);
            pst.setString(4, med.getDetails() != null ? med.getDetails() : "");
            pst.executeUpdate();

            rsKeys = pst.getGeneratedKeys();
            if (rsKeys.next()) {
                med.setId(rsKeys.getLong(1));
            }
            fallbackMeds.add(med);
            return med;
        } catch (SQLException e) {
            med.setId((long) (fallbackMeds.size() + 1));
            fallbackMeds.add(med);
            return med;
        } finally {
            DatabaseConnection.closeQuietly(rsKeys, pst, con);
        }
    }

    /**
     * Update an existing medicine record (SQL UPDATE).
     */
    public Medicine updateMedicine(Medicine med) throws PharmacyException {
        if (med == null || med.getId() == null) {
            throw new PharmacyException("Medicine or medicine ID cannot be null for update.");
        }
        Connection con = null;
        PreparedStatement pst = null;
        try {
            con = DatabaseConnection.getConnection();
            String sql = "UPDATE medicines SET name = ?, quantity = ?, available = ?, details = ? WHERE id = ?";
            pst = con.prepareStatement(sql);
            pst.setString(1, med.getName());
            pst.setInt(2, med.getQuantity() != null ? med.getQuantity() : 0);
            pst.setBoolean(3, med.getAvailable() != null ? med.getAvailable() : false);
            pst.setString(4, med.getDetails() != null ? med.getDetails() : "");
            pst.setLong(5, med.getId());
            pst.executeUpdate();

            for (int i = 0; i < fallbackMeds.size(); i++) {
                if (fallbackMeds.get(i).getId() != null && fallbackMeds.get(i).getId().equals(med.getId())) {
                    fallbackMeds.set(i, med);
                    break;
                }
            }
            return med;
        } catch (SQLException e) {
            for (int i = 0; i < fallbackMeds.size(); i++) {
                if (fallbackMeds.get(i).getId() != null && fallbackMeds.get(i).getId().equals(med.getId())) {
                    fallbackMeds.set(i, med);
                    break;
                }
            }
            return med;
        } finally {
            DatabaseConnection.closeQuietly(pst, con);
        }
    }

    /**
     * Delete an existing medicine record (SQL DELETE).
     */
    public boolean deleteMedicine(Long id) throws PharmacyException {
        if (id == null) return false;
        Connection con = null;
        PreparedStatement pst = null;
        try {
            con = DatabaseConnection.getConnection();
            pst = con.prepareStatement("DELETE FROM medicines WHERE id = ?");
            pst.setLong(1, id);
            boolean ok = pst.executeUpdate() > 0;
            fallbackMeds.removeIf(m -> m.getId() != null && m.getId().equals(id));
            return ok;
        } catch (SQLException e) {
            return fallbackMeds.removeIf(m -> m.getId() != null && m.getId().equals(id));
        } finally {
            DatabaseConnection.closeQuietly(pst, con);
        }
    }
}
