package com.mediflow.pharmacy.gui;

import com.mediflow.pharmacy.db.MedicineDAO;
import com.mediflow.pharmacy.db.LoginLogDAO;
import com.mediflow.pharmacy.db.DatabaseConnection;
import com.mediflow.pharmacy.model.Medicine;
import com.mediflow.pharmacy.model.LoginLog;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * CST 205 OOPJ - Module 4 & 5: Java Swing GUI and Event Handling
 * Standalone Desktop graphical interface for Pharmacy Inventory Management.
 * Explores: JFrame, JLabel, JTextField, JTextArea, JButton, JCheckBox, JTable,
 * Layout Managers (BorderLayout, FlowLayout, GridLayout), and ActionListener.
 */
public class MediFlowSwingApp extends JFrame implements ActionListener {

    private static final long serialVersionUID = 1L;

    private final transient MedicineDAO medicineDAO;
    private final transient LoginLogDAO loginLogDAO;

    // Swing Form Components
    private JTextField txtId;
    private JTextField txtName;
    private JTextField txtQuantity;
    private JCheckBox chkAvailable;
    private JTextArea txtDetails;
    private JTextField txtSearch;

    // Swing Buttons
    private JButton btnAdd;
    private JButton btnUpdate;
    private JButton btnDelete;
    private JButton btnClear;
    private JButton btnSearch;
    private JButton btnRefresh;

    // Swing Table Components
    private DefaultTableModel tableModel;
    private JTable medicineTable;
    private JLabel lblStatus;

    @SuppressWarnings("this-escape")
    public MediFlowSwingApp() {
        super("MediFlow - Pharmacy Management System (Swing GUI)");
        this.medicineDAO = new MedicineDAO();
        this.loginLogDAO = new LoginLogDAO();
        initComponents();
        loadTableData();
    }

    private void initComponents() {
        // Frame Settings
        setSize(950, 650);
        setMinimumSize(new Dimension(800, 500));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // --- North Panel: Header & Search ---
        JPanel northPanel = new JPanel(new BorderLayout(5, 5));
        northPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 5, 15));

        JLabel titleLabel = new JLabel("🏥 MediFlow Pharmacy Management System", JLabel.LEFT);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setForeground(new Color(24, 76, 120));
        northPanel.add(titleLabel, BorderLayout.NORTH);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        searchPanel.add(new JLabel("Search Catalog:"));
        txtSearch = new JTextField(18);
        btnSearch = new JButton("Search");
        btnRefresh = new JButton("Reset");
        btnSearch.addActionListener(this);
        btnRefresh.addActionListener(this);
        JButton btnViewLogs = new JButton("Login History Table");
        btnViewLogs.setBackground(new Color(230, 240, 255));
        btnViewLogs.addActionListener(e -> showLoginLogsDialog());

        searchPanel.add(txtSearch);
        searchPanel.add(btnSearch);
        searchPanel.add(btnRefresh);
        searchPanel.add(btnViewLogs);
        northPanel.add(searchPanel, BorderLayout.SOUTH);

        add(northPanel, BorderLayout.NORTH);

        // --- Center Panel: JTable inside JScrollPane ---
        String[] columns = {"ID", "Medicine Name", "Stock Quantity", "Availability", "Usage / Dosage Details"};
        tableModel = new DefaultTableModel(columns, 0) {
            private static final long serialVersionUID = 1L;
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // read-only cells
            }
        };
        medicineTable = new JTable(tableModel);
        medicineTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        medicineTable.setRowHeight(24);
        medicineTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                populateFormFromSelectedRow();
            }
        });
        JScrollPane scrollPane = new JScrollPane(medicineTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Medicine Inventory Records"));
        add(scrollPane, BorderLayout.CENTER);

        // --- West Panel: Form Editor ---
        JPanel westPanel = new JPanel(new BorderLayout());
        westPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(5, 15, 10, 5),
                BorderFactory.createTitledBorder("Medicine Form Editor")
        ));
        westPanel.setPreferredSize(new Dimension(320, 400));

        JPanel formGrid = new JPanel(new GridLayout(4, 2, 8, 8));
        formGrid.add(new JLabel("Record ID:"));
        txtId = new JTextField();
        txtId.setEditable(false);
        formGrid.add(txtId);

        formGrid.add(new JLabel("Medicine Name:*"));
        txtName = new JTextField();
        formGrid.add(txtName);

        formGrid.add(new JLabel("Stock Quantity:*"));
        txtQuantity = new JTextField();
        formGrid.add(txtQuantity);

        formGrid.add(new JLabel("Stock Status:"));
        chkAvailable = new JCheckBox("In Stock (Available)");
        chkAvailable.setSelected(true);
        formGrid.add(chkAvailable);

        JPanel detailsPanel = new JPanel(new BorderLayout(5, 5));
        detailsPanel.add(new JLabel("Dosage & Medical Details:"), BorderLayout.NORTH);
        txtDetails = new JTextArea(4, 20);
        txtDetails.setLineWrap(true);
        txtDetails.setWrapStyleWord(true);
        detailsPanel.add(new JScrollPane(txtDetails), BorderLayout.CENTER);

        JPanel westCenter = new JPanel(new BorderLayout(5, 8));
        westCenter.add(formGrid, BorderLayout.NORTH);
        westCenter.add(detailsPanel, BorderLayout.CENTER);
        westPanel.add(westCenter, BorderLayout.CENTER);

        // Action Buttons Panel
        JPanel btnPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        btnAdd = new JButton("Add Medicine");
        btnUpdate = new JButton("Update Selected");
        btnDelete = new JButton("Delete Selected");
        btnClear = new JButton("Clear Form");

        btnAdd.addActionListener(this);
        btnUpdate.addActionListener(this);
        btnDelete.addActionListener(this);
        btnClear.addActionListener(this);

        btnPanel.add(btnAdd);
        btnPanel.add(btnUpdate);
        btnPanel.add(btnDelete);
        btnPanel.add(btnClear);
        westPanel.add(btnPanel, BorderLayout.SOUTH);

        add(westPanel, BorderLayout.WEST);

        // --- South Panel: Status Bar ---
        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        southPanel.setBorder(BorderFactory.createEmptyBorder(2, 10, 5, 10));
        lblStatus = new JLabel("Database: Initializing MySQL connection...");
        lblStatus.setFont(new Font("Arial", Font.ITALIC, 11));
        lblStatus.setForeground(Color.GRAY);
        southPanel.add(lblStatus);
        add(southPanel, BorderLayout.SOUTH);
    }

    /**
     * Loads or refreshes data from JDBC database into JTable model.
     */
    public final void loadTableData() {
        try {
            List<Medicine> meds = medicineDAO.getAllMedicines();
            renderTableRows(meds);
            if (lblStatus != null) {
                lblStatus.setText("Database: Connected via MySQL JDBC (pharmacy_db) | Total Records: " + meds.size() + " | Port: 8080 Active");
                lblStatus.setForeground(new Color(16, 120, 60));
            }
        } catch (Exception e) {
            if (lblStatus != null) {
                lblStatus.setText("Database: Connection error - verify MySQL is running on localhost:3306");
                lblStatus.setForeground(new Color(180, 40, 40));
            }
            JOptionPane.showMessageDialog(this, "Error reading database: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void renderTableRows(List<Medicine> meds) {
        tableModel.setRowCount(0);
        for (Medicine m : meds) {
            tableModel.addRow(new Object[]{
                    m.getId(),
                    m.getName(),
                    m.getQuantity(),
                    m.getAvailable() ? "In Stock" : "Out of Stock",
                    m.getDetails()
            });
        }
    }

    private void populateFormFromSelectedRow() {
        int selectedRow = medicineTable.getSelectedRow();
        if (selectedRow >= 0) {
            txtId.setText(String.valueOf(tableModel.getValueAt(selectedRow, 0)));
            txtName.setText(String.valueOf(tableModel.getValueAt(selectedRow, 1)));
            txtQuantity.setText(String.valueOf(tableModel.getValueAt(selectedRow, 2)));
            String status = String.valueOf(tableModel.getValueAt(selectedRow, 3));
            chkAvailable.setSelected("In Stock".equalsIgnoreCase(status));
            txtDetails.setText(String.valueOf(tableModel.getValueAt(selectedRow, 4)));
        }
    }

    private void clearForm() {
        txtId.setText("");
        txtName.setText("");
        txtQuantity.setText("");
        chkAvailable.setSelected(true);
        txtDetails.setText("");
        medicineTable.clearSelection();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();

        if (source == btnAdd) {
            handleAddMedicine();
        } else if (source == btnUpdate) {
            handleUpdateMedicine();
        } else if (source == btnDelete) {
            handleDeleteMedicine();
        } else if (source == btnClear) {
            clearForm();
        } else if (source == btnSearch) {
            handleSearch();
        } else if (source == btnRefresh) {
            txtSearch.setText("");
            loadTableData();
        }
    }

    private void handleAddMedicine() {
        String name = txtName.getText().trim();
        String qtyStr = txtQuantity.getText().trim();
        String details = txtDetails.getText().trim();
        boolean available = chkAvailable.isSelected();

        if (name.isEmpty() || qtyStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please provide medicine name and quantity.", "Validation Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            int qty = Integer.parseInt(qtyStr);
            Medicine newMed = new Medicine(name, qty, available, details);
            medicineDAO.addMedicine(newMed);
            loadTableData();
            clearForm();
            JOptionPane.showMessageDialog(this, "Medicine '" + name + "' added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Quantity must be an integer number.", "Input Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error inserting record: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleUpdateMedicine() {
        String idStr = txtId.getText().trim();
        if (idStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a medicine record from the table to update.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Long id = Long.parseLong(idStr);
            String name = txtName.getText().trim();
            int qty = Integer.parseInt(txtQuantity.getText().trim());
            boolean available = chkAvailable.isSelected();
            String details = txtDetails.getText().trim();

            Medicine med = new Medicine(id, name, qty, available, details);
            medicineDAO.updateMedicine(med);
            loadTableData();
            JOptionPane.showMessageDialog(this, "Medicine updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid quantity number format.", "Input Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error updating record: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleDeleteMedicine() {
        String idStr = txtId.getText().trim();
        if (idStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a medicine record to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete medicine record ID " + idStr + "?",
                "Confirm Deletion", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                Long id = Long.parseLong(idStr);
                medicineDAO.deleteMedicine(id);
                loadTableData();
                clearForm();
                JOptionPane.showMessageDialog(this, "Medicine deleted successfully.", "Deleted", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error deleting record: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleSearch() {
        String query = txtSearch.getText().trim();
        try {
            List<Medicine> matches = medicineDAO.searchMedicines(query);
            renderTableRows(matches);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error during search: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showLoginLogsDialog() {
        JDialog dialog = new JDialog(this, "User & Faculty Login History (Database Table)", true);
        dialog.setSize(750, 420);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(5, 5));

        String[] logCols = {"ID", "Username", "Role Type", "Login Timestamp", "IP Address", "Status"};
        DefaultTableModel logModel = new DefaultTableModel(logCols, 0) {
            private static final long serialVersionUID = 1L;
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable logTable = new JTable(logModel);
        logTable.setRowHeight(24);

        try {
            List<LoginLog> logs = loginLogDAO.getAllLogs(100);
            for (LoginLog log : logs) {
                logModel.addRow(new Object[]{
                        log.getId(),
                        log.getUsername(),
                        log.getRole(),
                        log.getLoginTimeStr(),
                        log.getIpAddress(),
                        log.getStatus()
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not load login logs from database: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }

        dialog.add(new JScrollPane(logTable), BorderLayout.CENTER);
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dialog.dispose());
        JPanel pnl = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pnl.add(closeBtn);
        dialog.add(pnl, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    public static void main(String[] args) {
        DatabaseConnection.initializeDatabase();
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            new MediFlowSwingApp().setVisible(true);
        });
    }
}
