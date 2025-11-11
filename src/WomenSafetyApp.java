import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.security.MessageDigest;
import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.Vector;

public class WomenSafetyApp {
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/womensafetydb";
    private static final String DB_USER = "postgres";
    private static final String DB_PASS = "1234";
    private static final Color COLOR_BG_LIGHT_PURPLE = new Color(230, 220, 250);
    private static final Color COLOR_BUTTON_PINK = new Color(255, 200, 220);
    private static final Color COLOR_TITLE_CORAL = new Color(220, 80, 40);
    private static final Color COLOR_BORDER_CORAL = new Color(255, 127, 80);

    private static final Font FONT_TITLE = new Font("Arial", Font.BOLD, 24);
    private static final Font FONT_HEADING = new Font("Arial", Font.BOLD, 18);
    private static final Font FONT_LABEL = new Font("Arial", Font.BOLD, 14);
    private static final Font FONT_FIELD = new Font("Arial", Font.PLAIN, 14);


    private final JFrame frame;
    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);

    private User currentUser = null;

    public WomenSafetyApp() {
        frame = new JFrame("Women Safety App - Swing + Postgres");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 600);
        frame.setLocationRelativeTo(null);

        root.add(new LoginPanel(), "login");
        root.add(new RegisterPanel(), "register");
        root.add(new DashboardPanel(), "dashboard");
        root.add(new AdminPanel(), "admin");

        frame.setContentPane(root);
        cards.show(root, "login");
        frame.setVisible(true);
    }

    private void styleButton(JButton button) {
        button.setBackground(COLOR_BUTTON_PINK);
        button.setOpaque(true);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setFont(FONT_LABEL);
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                button.setBackground(COLOR_BUTTON_PINK.brighter());
            }
            public void mouseExited(MouseEvent evt) {
                button.setBackground(COLOR_BUTTON_PINK);
            }
            public void mousePressed(MouseEvent evt) {
                button.setBackground(COLOR_BUTTON_PINK.darker());
            }
            public void mouseReleased(MouseEvent evt) {
                if (button.contains(evt.getPoint())) {
                    button.setBackground(COLOR_BUTTON_PINK.brighter());
                } else {
                    button.setBackground(COLOR_BUTTON_PINK);
                }
            }
        });
    }

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] b = md.digest(s.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte x : b) sb.append(String.format("%02x", x));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class User {
        int id;
        String username;
        String fullname;
        String phone;
        boolean isAdmin;

        User(int id, String username, String fullname, String phone, boolean isAdmin) {
            this.id = id;
            this.username = username;
            this.fullname = fullname;
            this.phone = phone;
            this.isAdmin = isAdmin;
        }
    }

    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

    private boolean registerUser(String username, String password, String fullname, String phone) {
        String sql = "INSERT INTO users (username, password_hash, fullname, phone) VALUES (?, ?, ?, ?)";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, sha256(password));
            ps.setString(3, fullname);
            ps.setString(4, phone);
            ps.executeUpdate();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private User loginUser(String username, String password) {
        String sql = "SELECT id, username, fullname, phone, is_admin FROM users WHERE username = ? AND password_hash = ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, sha256(password));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("fullname"),
                            rs.getString("phone"),
                            rs.getBoolean("is_admin")
                    );
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private static boolean addEmergencyContact(int userId, String name, String phone, String relation) {
        String sql = "INSERT INTO emergency_contacts (user_id, name, phone, relationship) VALUES (?, ?, ?, ?)";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, name);
            ps.setString(3, phone);
            ps.setString(4, relation);
            ps.executeUpdate();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private Vector<Vector<Object>> getContactsForUser(int userId) {
        String sql = "SELECT id, name, phone, relationship, created_at FROM emergency_contacts WHERE user_id = ? ORDER BY created_at DESC";
        Vector<Vector<Object>> data = new Vector<>();
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Vector<Object> row = new Vector<>();
                    row.add(rs.getInt("id"));
                    row.add(rs.getString("name"));
                    row.add(rs.getString("phone"));
                    row.add(rs.getString("relationship"));
                    row.add(rs.getTimestamp("created_at").toString());
                    data.add(row);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return data;
    }

    private Vector<Vector<Object>> getIncidentsForUser(int userId) {
        String sql = "SELECT id, title, location, severity, status, created_at FROM incidents WHERE user_id = ? ORDER BY created_at DESC";
        Vector<Vector<Object>> data = new Vector<>();
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Vector<Object> row = new Vector<>();
                    row.add(rs.getInt("id"));
                    row.add(rs.getString("title"));
                    row.add(rs.getString("location"));
                    row.add(rs.getString("severity"));
                    row.add(rs.getString("status"));
                    row.add(rs.getTimestamp("created_at").toString());
                    data.add(row);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return data;
    }

    private static boolean reportIncident(Integer userId, String title, String description, String location, String severity) {
        String sql = "INSERT INTO incidents (user_id, title, description, location, severity) VALUES (?, ?, ?, ?, ?)";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (userId == null) ps.setNull(1, Types.INTEGER); else ps.setInt(1, userId);
            ps.setString(2, title);
            ps.setString(3, description);
            ps.setString(4, location);
            ps.setString(5, severity);
            ps.executeUpdate();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private Vector<Vector<Object>> getAllIncidents() {
        String sql = "SELECT i.id, u.username, i.title, i.description, i.location, i.severity, i.created_at, i.status " +
                "FROM incidents i LEFT JOIN users u ON u.id = i.user_id " +
                "ORDER BY i.created_at DESC";
        Vector<Vector<Object>> data = new Vector<>();
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("id"));
                row.add(rs.getString("username"));
                row.add(rs.getString("title"));
                row.add(rs.getString("description"));
                row.add(rs.getString("location"));
                row.add(rs.getString("severity"));
                row.add(rs.getTimestamp("created_at").toString());
                row.add(rs.getString("status"));
                data.add(row);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return data;
    }

    private boolean updateIncidentStatus(int incidentId, String newStatus) {
        String sql = "UPDATE incidents SET status = ? WHERE id = ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, incidentId);
            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private Vector<Vector<Object>> getAllAlerts() {
        String sql = "SELECT a.id, u.username, a.message, a.created_at FROM alerts a LEFT JOIN users u ON u.id = a.user_id ORDER BY a.created_at DESC";
        Vector<Vector<Object>> data = new Vector<>();
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("id"));
                row.add(rs.getString("username"));
                row.add(rs.getString("message"));
                row.add(rs.getTimestamp("created_at").toString());
                data.add(row);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return data;
    }

    private Vector<Vector<Object>> getAllUsers() {
        String sql = "SELECT id, username, fullname, phone, created_at FROM users WHERE is_admin = false ORDER BY created_at DESC";
        Vector<Vector<Object>> data = new Vector<>();
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("id"));
                row.add(rs.getString("username"));
                row.add(rs.getString("fullname"));
                row.add(rs.getString("phone"));
                row.add(rs.getTimestamp("created_at").toString());
                data.add(row);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return data;
    }

    private static boolean sendAlert(Integer userId, String message) {
        String sql = "INSERT INTO alerts (user_id, message) VALUES (?, ?)";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (userId == null) ps.setNull(1, Types.INTEGER); else ps.setInt(1, userId);
            ps.setString(2, message);
            ps.executeUpdate();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private boolean sendBroadcast(String message) {
        String sql = "INSERT INTO announcements (message) VALUES (?)";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, message);
            ps.executeUpdate();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private Vector<String> getAllAnnouncements() {
        String sql = "SELECT message, created_at FROM announcements ORDER BY created_at DESC";
        Vector<String> data = new Vector<>();
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a");

            while (rs.next()) {
                String message = rs.getString("message");
                String timestamp = rs.getTimestamp("created_at").toLocalDateTime().format(formatter);

                String formattedEntry = "On " + timestamp + ":\n" + message + "\n\n";
                data.add(formattedEntry);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return data;
    }

    private class LoginPanel extends JPanel {
        LoginPanel() {
            setLayout(new GridBagLayout());
            setBackground(COLOR_BG_LIGHT_PURPLE);

            JPanel center = new JPanel(new GridBagLayout());
            center.setBackground(Color.WHITE);
            center.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(COLOR_BORDER_CORAL),
                    BorderFactory.createEmptyBorder(20, 25, 20, 25)
            ));

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 10, 10, 10);
            gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.EAST;
            JLabel userLabel = new JLabel("Username:");
            userLabel.setFont(FONT_LABEL);
            center.add(userLabel, gbc);

            gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
            JTextField tfUser = new JTextField(18);
            tfUser.setFont(FONT_FIELD);
            center.add(tfUser, gbc);

            gbc.gridx = 0; gbc.gridy++;
            gbc.anchor = GridBagConstraints.EAST;
            JLabel passLabel = new JLabel("Password:");
            passLabel.setFont(FONT_LABEL);
            center.add(passLabel, gbc);

            gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
            JPasswordField pf = new JPasswordField(18);
            pf.setFont(FONT_FIELD);
            center.add(pf, gbc);

            gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2;
            gbc.anchor = GridBagConstraints.CENTER;
            JButton btnLogin = new JButton("Login");
            styleButton(btnLogin);

            JButton btnRegister = new JButton("Register");
            styleButton(btnRegister);

            JPanel pBtns = new JPanel();
            pBtns.setOpaque(false);
            pBtns.add(btnLogin);
            pBtns.add(btnRegister);
            center.add(pBtns, gbc);

            JLabel lblTitle = new JLabel("Women Safety - Login", SwingConstants.CENTER);
            lblTitle.setFont(FONT_TITLE);
            lblTitle.setForeground(COLOR_TITLE_CORAL);

            GridBagConstraints rootGbc = new GridBagConstraints();
            rootGbc.gridy = 0;
            rootGbc.insets = new Insets(0, 0, 20, 0);
            rootGbc.anchor = GridBagConstraints.PAGE_START;
            add(lblTitle, rootGbc);

            rootGbc.gridy = 1;
            rootGbc.insets = new Insets(0, 0, 0, 0);
            rootGbc.anchor = GridBagConstraints.CENTER;
            add(center, rootGbc);

            btnLogin.addActionListener(e -> {
                String user = tfUser.getText().trim();
                String pass = new String(pf.getPassword());
                if (user.isEmpty() || pass.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Enter username and password.", "Validation", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                User u = loginUser(user, pass);
                if (u == null) {
                    JOptionPane.showMessageDialog(this, "Invalid credentials.", "Login failed", JOptionPane.ERROR_MESSAGE);
                } else {
                    currentUser = u;
                    JOptionPane.showMessageDialog(this, "Welcome, " + (u.fullname != null ? u.fullname : u.username));

                    if (u.isAdmin) {
                        cards.show(root, "admin");
                    } else {
                        cards.show(root, "dashboard");
                    }
                }
            });

            btnRegister.addActionListener(e -> cards.show(root, "register"));
        }
    }

    private class RegisterPanel extends JPanel {
        RegisterPanel() {
            setLayout(new GridBagLayout());
            setBackground(COLOR_BG_LIGHT_PURPLE);

            JPanel p = new JPanel(new GridBagLayout());
            p.setBackground(Color.WHITE);
            p.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(COLOR_BORDER_CORAL),
                    BorderFactory.createEmptyBorder(20, 25, 20, 25)
            ));

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(8, 8, 8, 8);
            gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.EAST;
            JLabel nameLabel = new JLabel("Full name:");
            nameLabel.setFont(FONT_LABEL);
            p.add(nameLabel, gbc);

            gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
            JTextField tfName = new JTextField(18);
            tfName.setFont(FONT_FIELD);
            p.add(tfName, gbc);

            gbc.gridx = 0; gbc.gridy++;
            gbc.anchor = GridBagConstraints.EAST;
            JLabel userLabel = new JLabel("Username:");
            userLabel.setFont(FONT_LABEL);
            p.add(userLabel, gbc);

            gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
            JTextField tfUser = new JTextField(18);
            tfUser.setFont(FONT_FIELD);
            p.add(tfUser, gbc);

            gbc.gridx = 0; gbc.gridy++;
            gbc.anchor = GridBagConstraints.EAST;
            JLabel phoneLabel = new JLabel("Phone:");
            phoneLabel.setFont(FONT_LABEL);
            p.add(phoneLabel, gbc);

            gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
            JTextField tfPhone = new JTextField(18);
            tfPhone.setFont(FONT_FIELD);
            p.add(tfPhone, gbc);

            gbc.gridx = 0; gbc.gridy++;
            gbc.anchor = GridBagConstraints.EAST;
            JLabel pass1Label = new JLabel("Password:");
            pass1Label.setFont(FONT_LABEL);
            p.add(pass1Label, gbc);

            gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
            JPasswordField pf1 = new JPasswordField(18);
            pf1.setFont(FONT_FIELD);
            p.add(pf1, gbc);

            gbc.gridx = 0; gbc.gridy++;
            gbc.anchor = GridBagConstraints.EAST;
            JLabel pass2Label = new JLabel("Confirm:");
            pass2Label.setFont(FONT_LABEL);
            p.add(pass2Label, gbc);

            gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
            JPasswordField pf2 = new JPasswordField(18);
            pf2.setFont(FONT_FIELD);
            p.add(pf2, gbc);

            gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2;
            gbc.anchor = GridBagConstraints.CENTER;
            JButton btnRegister = new JButton("Create account");
            styleButton(btnRegister);

            JButton btnBack = new JButton("Back to Login");
            styleButton(btnBack);

            JPanel pb = new JPanel();
            pb.setOpaque(false);
            pb.add(btnRegister); pb.add(btnBack);
            p.add(pb, gbc);

            JLabel lblTitle = new JLabel("Register New User", SwingConstants.CENTER);
            lblTitle.setFont(FONT_TITLE);
            lblTitle.setForeground(COLOR_TITLE_CORAL);

            GridBagConstraints rootGbc = new GridBagConstraints();
            rootGbc.gridy = 0;
            rootGbc.insets = new Insets(0, 0, 20, 0);
            rootGbc.anchor = GridBagConstraints.PAGE_START;
            add(lblTitle, rootGbc);

            rootGbc.gridy = 1;
            rootGbc.insets = new Insets(0, 0, 0, 0);
            rootGbc.anchor = GridBagConstraints.CENTER;
            add(p, rootGbc);

            btnRegister.addActionListener(e -> {
                String name = tfName.getText().trim();
                String user = tfUser.getText().trim();
                String phone = tfPhone.getText().trim();
                String p1 = new String(pf1.getPassword());
                String p2 = new String(pf2.getPassword());
                if (user.isEmpty() || p1.isEmpty() || !p1.equals(p2)) {
                    JOptionPane.showMessageDialog(this, "Check username/password and confirm.", "Validation", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                boolean ok = registerUser(user, p1, name, phone);
                if (ok) {
                    JOptionPane.showMessageDialog(this, "Account created. Please login.");
                    cards.show(root, "login");
                } else {
                    JOptionPane.showMessageDialog(this, "Registration failed (username may already exist).", "Error", JOptionPane.ERROR_MESSAGE);
                }
            });

            btnBack.addActionListener(e -> cards.show(root, "login"));
        }
    }

    private class DashboardPanel extends JPanel {
        private final DefaultTableModel contactsModel = new DefaultTableModel(new String[]{"ID","Name","Phone","Relation","Added At"},0);
        private final JTable contactsTable = new JTable(contactsModel);

        private final DefaultTableModel myIncidentsModel = new DefaultTableModel(new String[]{"ID","Title","Location","Severity","Status","Reported At"},0);
        private final JTable myIncidentsTable = new JTable(myIncidentsModel);

        private final JTextArea taAnnouncements = new JTextArea();

        DashboardPanel() {
            setLayout(new BorderLayout(15, 15));
            setBackground(COLOR_BG_LIGHT_PURPLE);
            setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

            JPanel topBar = new JPanel(new BorderLayout());
            topBar.setOpaque(false);
            JLabel lblTitle = new JLabel("Dashboard", SwingConstants.LEFT);
            lblTitle.setFont(FONT_TITLE);
            lblTitle.setForeground(COLOR_TITLE_CORAL);
            topBar.add(lblTitle, BorderLayout.WEST);

            JButton btnLogout = new JButton("Logout");
            btnLogout.setIcon(UIManager.getIcon("InternalFrame.closeIcon"));
            styleButton(btnLogout);
            topBar.add(btnLogout, BorderLayout.EAST);
            add(topBar, BorderLayout.NORTH);

            JPanel center = new JPanel(new GridLayout(1,2,15,15));
            center.setOpaque(false);

            JPanel left = new JPanel(new BorderLayout(10, 10));
            left.setBackground(Color.WHITE);
            left.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(COLOR_BORDER_CORAL),
                    BorderFactory.createEmptyBorder(15, 15, 15, 15)
            ));

            JLabel actionTitle = new JLabel("Actions");
            actionTitle.setFont(FONT_HEADING);
            actionTitle.setHorizontalAlignment(SwingConstants.CENTER);
            left.add(actionTitle, BorderLayout.NORTH);

            JPanel actionButtonsPanel = new JPanel(new GridBagLayout());
            actionButtonsPanel.setOpaque(false);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;

            Dimension buttonSize = new Dimension(220, 40);

            JButton btnAddContact = new JButton("Add Emergency Contact");
            btnAddContact.setIcon(UIManager.getIcon("FileChooser.newFolderIcon"));
            styleButton(btnAddContact);
            btnAddContact.setPreferredSize(buttonSize);
            actionButtonsPanel.add(btnAddContact, gbc);

            gbc.gridy++;
            JButton btnRefreshTables = new JButton("Refresh My Data");
            btnRefreshTables.setIcon(UIManager.getIcon("FileChooser.listViewIcon"));
            styleButton(btnRefreshTables);
            btnRefreshTables.setPreferredSize(buttonSize);
            actionButtonsPanel.add(btnRefreshTables, gbc);

            gbc.gridy++;
            JButton btnReport = new JButton("Report Incident");
            btnReport.setIcon(UIManager.getIcon("OptionPane.warningIcon"));
            styleButton(btnReport);
            btnReport.setPreferredSize(buttonSize);
            actionButtonsPanel.add(btnReport, gbc);

            gbc.gridy++;
            JButton btnSendAlert = new JButton("Send Quick Alert");
            btnSendAlert.setIcon(UIManager.getIcon("FileView.floppyDriveIcon"));
            styleButton(btnSendAlert);
            btnSendAlert.setPreferredSize(buttonSize);
            actionButtonsPanel.add(btnSendAlert, gbc);

            gbc.gridy++;
            JButton btnSafetyTips = new JButton("Safety Tips & Resources");
            btnSafetyTips.setIcon(UIManager.getIcon("OptionPane.informationIcon"));
            styleButton(btnSafetyTips);
            btnSafetyTips.setPreferredSize(buttonSize);
            actionButtonsPanel.add(btnSafetyTips, gbc);

            left.add(actionButtonsPanel, BorderLayout.CENTER);
            center.add(left);

            JTabbedPane rightTabs = new JTabbedPane();
            rightTabs.setFont(FONT_LABEL);

            JPanel announcementsPanel = new JPanel(new BorderLayout(10, 10));
            announcementsPanel.setOpaque(false);
            announcementsPanel.setBackground(Color.WHITE);
            announcementsPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(COLOR_BORDER_CORAL),
                    BorderFactory.createEmptyBorder(15, 15, 15, 15)
            ));
            JLabel announcementsTitle = new JLabel("Announcements");
            announcementsTitle.setFont(FONT_HEADING);
            announcementsTitle.setHorizontalAlignment(SwingConstants.CENTER);
            announcementsPanel.add(announcementsTitle, BorderLayout.NORTH);

            taAnnouncements.setFont(FONT_FIELD);
            taAnnouncements.setWrapStyleWord(true);
            taAnnouncements.setLineWrap(true);
            taAnnouncements.setEditable(false);
            taAnnouncements.setMargin(new Insets(10, 10, 10, 10));

            JScrollPane announcementsScrollPane = new JScrollPane(taAnnouncements);
            announcementsScrollPane.getViewport().setBackground(Color.WHITE);
            announcementsPanel.add(announcementsScrollPane, BorderLayout.CENTER);

            rightTabs.addTab("Announcements", UIManager.getIcon("OptionPane.informationIcon"), announcementsPanel);


            JPanel contactsPanel = new JPanel(new BorderLayout(10, 10));
            contactsPanel.setOpaque(false);
            contactsPanel.setBackground(Color.WHITE);
            contactsPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(COLOR_BORDER_CORAL),
                    BorderFactory.createEmptyBorder(15, 15, 15, 15)
            ));

            JLabel contactsTitle = new JLabel("Emergency Contacts");
            contactsTitle.setFont(FONT_HEADING);
            contactsTitle.setHorizontalAlignment(SwingConstants.CENTER);
            contactsPanel.add(contactsTitle, BorderLayout.NORTH);

            contactsTable.setFont(FONT_FIELD);
            contactsTable.setRowHeight(24);
            contactsTable.getTableHeader().setFont(FONT_LABEL);
            contactsTable.getTableHeader().setBackground(new Color(240, 245, 250));
            contactsTable.setFillsViewportHeight(true);

            JScrollPane contactsScrollPane = new JScrollPane(contactsTable);
            contactsScrollPane.getViewport().setBackground(Color.WHITE);
            contactsPanel.add(contactsScrollPane, BorderLayout.CENTER);

            rightTabs.addTab("Contacts", UIManager.getIcon("FileChooser.newFolderIcon"), contactsPanel);

            JPanel incidentsPanel = new JPanel(new BorderLayout(10, 10));
            incidentsPanel.setOpaque(false);
            incidentsPanel.setBackground(Color.WHITE);
            incidentsPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(COLOR_BORDER_CORAL),
                    BorderFactory.createEmptyBorder(15, 15, 15, 15)
            ));

            JLabel incidentsTitle = new JLabel("My Reported Incidents");
            incidentsTitle.setFont(FONT_HEADING);
            incidentsTitle.setHorizontalAlignment(SwingConstants.CENTER);
            incidentsPanel.add(incidentsTitle, BorderLayout.NORTH);

            myIncidentsTable.setFont(FONT_FIELD);
            myIncidentsTable.setRowHeight(24);
            myIncidentsTable.getTableHeader().setFont(FONT_LABEL);
            myIncidentsTable.getTableHeader().setBackground(new Color(240, 245, 250));
            myIncidentsTable.setFillsViewportHeight(true);

            JScrollPane incidentsScrollPane = new JScrollPane(myIncidentsTable);
            incidentsScrollPane.getViewport().setBackground(Color.WHITE);
            incidentsPanel.add(incidentsScrollPane, BorderLayout.CENTER);

            rightTabs.addTab("My Incidents", UIManager.getIcon("OptionPane.warningIcon"), incidentsPanel);

            center.add(rightTabs);
            add(center, BorderLayout.CENTER);

            JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
            bottom.setOpaque(false);
            JButton btnProfile = new JButton("My Profile");
            btnProfile.setIcon(UIManager.getIcon("FileView.computerIcon"));
            styleButton(btnProfile);
            bottom.add(btnProfile);
            add(bottom, BorderLayout.SOUTH);

            btnLogout.addActionListener(e -> {
                currentUser = null;
                contactsModel.setRowCount(0);
                myIncidentsModel.setRowCount(0);
                taAnnouncements.setText("");
                cards.show(root, "login");
            });

            btnAddContact.addActionListener(e -> {
                AddContactDialog dialog = new AddContactDialog(frame, currentUser, this::loadContacts);
                dialog.setVisible(true);
            });

            btnRefreshTables.addActionListener(e -> {
                loadContacts();
                loadMyIncidents();
                loadAnnouncements();
            });

            btnReport.addActionListener(e -> {
                ReportDialog rd = new ReportDialog(frame, currentUser, this::loadMyIncidents);
                rd.setVisible(true);
            });

            btnSendAlert.addActionListener(e -> {
                SendAlertDialog dialog = new SendAlertDialog(frame, currentUser);
                dialog.setVisible(true);
            });

            btnProfile.addActionListener(e -> {
                ProfileDialog dialog = new ProfileDialog(frame, currentUser);
                dialog.setVisible(true);
            });

            btnSafetyTips.addActionListener(e -> {
                SafetyTipsDialog dialog = new SafetyTipsDialog(frame);
                dialog.setVisible(true);
            });

            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentShown(ComponentEvent e) {
                    if (currentUser != null) {
                        loadContacts();
                        loadMyIncidents();
                        loadAnnouncements();
                    }
                }
            });
        }

        private void loadContacts() {
            if (currentUser == null) return;
            contactsModel.setRowCount(0);
            Vector<Vector<Object>> rows = getContactsForUser(currentUser.id);
            for (Vector<Object> r : rows) {
                contactsModel.addRow(r);
            }
        }

        private void loadMyIncidents() {
            if (currentUser == null) return;
            myIncidentsModel.setRowCount(0);
            Vector<Vector<Object>> rows = getIncidentsForUser(currentUser.id);
            for (Vector<Object> r : rows) {
                myIncidentsModel.addRow(r);
            }
        }

        private void loadAnnouncements() {
            if (currentUser == null) return;
            taAnnouncements.setText("");
            Vector<String> rows = getAllAnnouncements();
            for (String s : rows) {
                taAnnouncements.append(s);
            }
            taAnnouncements.setCaretPosition(0);
        }
    }

    private class AdminPanel extends JPanel {
        private final DefaultTableModel incidentsModel = new DefaultTableModel(new String[]{"ID","User","Title","Description","Location","Severity","Created At", "Status"},0);
        private final JTable incidentsTable = new JTable(incidentsModel);

        private final DefaultTableModel alertsModel = new DefaultTableModel(new String[]{"ID","User","Message","Sent At"},0);
        private final JTable alertsTable = new JTable(alertsModel);

        private final DefaultTableModel usersModel = new DefaultTableModel(new String[]{"ID","Username","Full Name","Phone","Registered At"},0);
        private final JTable usersTable = new JTable(usersModel);

        private final JTextArea taBroadcast = new JTextArea(15, 40);

        AdminPanel() {
            setLayout(new BorderLayout(15, 15));
            setBackground(COLOR_BG_LIGHT_PURPLE);
            setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

            JPanel topBar = new JPanel(new BorderLayout());
            topBar.setOpaque(false);
            JLabel lblTitle = new JLabel("Admin Panel", SwingConstants.LEFT);
            lblTitle.setFont(FONT_TITLE);
            lblTitle.setForeground(COLOR_TITLE_CORAL);
            topBar.add(lblTitle, BorderLayout.WEST);
            add(topBar, BorderLayout.NORTH);

            JTabbedPane tabbedPane = new JTabbedPane();
            tabbedPane.setFont(FONT_LABEL);

            JPanel usersPanel = new JPanel(new BorderLayout(10, 10));
            usersPanel.setOpaque(false);
            usersPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
            usersTable.setFont(FONT_FIELD);
            usersTable.setRowHeight(24);
            usersTable.getTableHeader().setFont(FONT_LABEL);
            usersTable.getTableHeader().setBackground(new Color(240, 245, 250));
            usersTable.setFillsViewportHeight(true);
            JScrollPane usersScrollPane = new JScrollPane(usersTable);
            usersScrollPane.getViewport().setBackground(Color.WHITE);
            usersPanel.add(usersScrollPane, BorderLayout.CENTER);
            tabbedPane.addTab("Registered Users", UIManager.getIcon("FileView.computerIcon"), usersPanel);

            JPanel incidentsPanel = new JPanel(new BorderLayout(10, 10));
            incidentsPanel.setOpaque(false);
            incidentsPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

            incidentsTable.setFont(FONT_FIELD);
            incidentsTable.setRowHeight(24);
            incidentsTable.getTableHeader().setFont(FONT_LABEL);
            incidentsTable.getTableHeader().setBackground(new Color(240, 245, 250));
            incidentsTable.setFillsViewportHeight(true);
            JScrollPane incidentsScrollPane = new JScrollPane(incidentsTable);
            incidentsScrollPane.getViewport().setBackground(Color.WHITE);

            incidentsPanel.add(incidentsScrollPane, BorderLayout.CENTER);

            JPanel incidentButtonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            incidentButtonsPanel.setOpaque(false);

            JButton btnInProgress = new JButton("Mark In Progress");
            styleButton(btnInProgress);

            JButton btnResolved = new JButton("Mark Resolved");
            styleButton(btnResolved);

            incidentButtonsPanel.add(btnInProgress);
            incidentButtonsPanel.add(btnResolved);

            incidentsPanel.add(incidentButtonsPanel, BorderLayout.SOUTH);

            tabbedPane.addTab("Reported Incidents", UIManager.getIcon("OptionPane.warningIcon"), incidentsPanel);

            JPanel alertsPanel = new JPanel(new BorderLayout(10, 10));
            alertsPanel.setOpaque(false);
            alertsPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
            alertsTable.setFont(FONT_FIELD);
            alertsTable.setRowHeight(24);
            alertsTable.getTableHeader().setFont(FONT_LABEL);
            alertsTable.getTableHeader().setBackground(new Color(240, 245, 250));
            alertsTable.setFillsViewportHeight(true);
            JScrollPane alertsScrollPane = new JScrollPane(alertsTable);
            alertsScrollPane.getViewport().setBackground(Color.WHITE);
            alertsPanel.add(alertsScrollPane, BorderLayout.CENTER);
            tabbedPane.addTab("Sent Alerts", UIManager.getIcon("FileView.floppyDriveIcon"), alertsPanel);

            JPanel broadcastPanel = new JPanel(new BorderLayout(10, 10));
            broadcastPanel.setOpaque(false);
            broadcastPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

            JLabel broadcastTitle = new JLabel("Write a broadcast message to all users:");
            broadcastTitle.setFont(FONT_LABEL);
            broadcastPanel.add(broadcastTitle, BorderLayout.NORTH);

            taBroadcast.setFont(FONT_FIELD);
            taBroadcast.setLineWrap(true);
            taBroadcast.setWrapStyleWord(true);
            JScrollPane broadcastScrollPane = new JScrollPane(taBroadcast);
            broadcastScrollPane.setBorder(BorderFactory.createLineBorder(COLOR_BORDER_CORAL));
            broadcastPanel.add(broadcastScrollPane, BorderLayout.CENTER);

            JPanel broadcastButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            broadcastButtonPanel.setOpaque(false);
            JButton btnSendBroadcast = new JButton("Send Broadcast");
            styleButton(btnSendBroadcast);
            broadcastButtonPanel.add(btnSendBroadcast);
            broadcastPanel.add(broadcastButtonPanel, BorderLayout.SOUTH);

            tabbedPane.addTab("Broadcast", UIManager.getIcon("OptionPane.informationIcon"), broadcastPanel);

            add(tabbedPane, BorderLayout.CENTER);

            JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            bottom.setOpaque(false);
            JButton btnRefresh = new JButton("Refresh All");
            btnRefresh.setIcon(UIManager.getIcon("FileChooser.listViewIcon"));
            styleButton(btnRefresh);

            JButton btnLogout = new JButton("Logout");
            btnLogout.setIcon(UIManager.getIcon("InternalFrame.closeIcon"));
            styleButton(btnLogout);

            bottom.add(btnRefresh);
            bottom.add(btnLogout);
            add(bottom, BorderLayout.SOUTH);

            btnRefresh.addActionListener(e -> {
                loadUsers();
                loadIncidents();
                loadAlerts();
            });
            btnLogout.addActionListener(e -> {
                currentUser = null;
                cards.show(root, "login");
            });

            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentShown(ComponentEvent e) {
                    loadUsers();
                    loadIncidents();
                    loadAlerts();
                }
            });

            btnInProgress.addActionListener(e -> updateSelectedIncidentStatus("In Progress"));
            btnResolved.addActionListener(e -> updateSelectedIncidentStatus("Resolved"));

            btnSendBroadcast.addActionListener(e -> {
                String message = taBroadcast.getText().trim();
                if (message.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Message cannot be empty.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                boolean success = sendBroadcast(message);
                if (success) {
                    JOptionPane.showMessageDialog(this, "Broadcast sent to all users.");
                    taBroadcast.setText("");
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to send broadcast.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            });
        }

        private void updateSelectedIncidentStatus(String newStatus) {
            int selectedRow = incidentsTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Please select an incident from the table first.", "No Incident Selected", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int incidentId = (Integer) incidentsModel.getValueAt(incidentsTable.convertRowIndexToModel(selectedRow), 0);

            boolean success = updateIncidentStatus(incidentId, newStatus);
            if (success) {
                JOptionPane.showMessageDialog(this, "Incident status updated.");
                loadIncidents();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to update status.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void loadUsers() {
            usersModel.setRowCount(0);
            Vector<Vector<Object>> rows = getAllUsers();
            for (Vector<Object> r : rows) usersModel.addRow(r);
        }

        private void loadIncidents() {
            incidentsModel.setRowCount(0);
            Vector<Vector<Object>> rows = getAllIncidents();
            for (Vector<Object> r : rows) incidentsModel.addRow(r);
        }

        private void loadAlerts() {
            alertsModel.setRowCount(0);
            Vector<Vector<Object>> rows = getAllAlerts();
            for (Vector<Object> r : rows) alertsModel.addRow(r);
        }
    }

    private class ProfileDialog extends JDialog {
        ProfileDialog(Window owner, User user) {
            super(owner, "My Profile", ModalityType.APPLICATION_MODAL);
            setSize(500, 350);
            setLocationRelativeTo(owner);
            getContentPane().setBackground(COLOR_BG_LIGHT_PURPLE);
            setLayout(new BorderLayout(15, 15));
            getRootPane().setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

            JLabel lblTitle = new JLabel("My Profile", SwingConstants.CENTER);
            lblTitle.setFont(FONT_TITLE);
            lblTitle.setForeground(COLOR_TITLE_CORAL);
            add(lblTitle, BorderLayout.NORTH);

            JPanel infoPanel = new JPanel(new GridBagLayout());
            infoPanel.setBackground(Color.WHITE);
            infoPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(COLOR_BORDER_CORAL),
                    BorderFactory.createEmptyBorder(20, 20, 20, 20)
            ));

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 10, 10, 10);
            gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.EAST;
            JLabel userLabel = new JLabel("Username:");
            userLabel.setFont(FONT_LABEL);
            infoPanel.add(userLabel, gbc);

            gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
            JLabel userName = new JLabel(user.username);
            userName.setFont(FONT_FIELD);
            infoPanel.add(userName, gbc);

            gbc.gridx = 0; gbc.gridy++; gbc.anchor = GridBagConstraints.EAST;
            JLabel nameLabel = new JLabel("Full Name:");
            nameLabel.setFont(FONT_LABEL);
            infoPanel.add(nameLabel, gbc);

            gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
            JLabel fullName = new JLabel(user.fullname);
            fullName.setFont(FONT_FIELD);
            infoPanel.add(fullName, gbc);

            gbc.gridx = 0; gbc.gridy++; gbc.anchor = GridBagConstraints.EAST;
            JLabel phoneLabel = new JLabel("Phone:");
            phoneLabel.setFont(FONT_LABEL);
            infoPanel.add(phoneLabel, gbc);

            gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
            JLabel userPhone = new JLabel(user.phone);
            userPhone.setFont(FONT_FIELD);
            infoPanel.add(userPhone, gbc);

            add(infoPanel, BorderLayout.CENTER);

            JPanel bottom = new JPanel();
            bottom.setOpaque(false);
            JButton btnClose = new JButton("Close");
            styleButton(btnClose);
            bottom.add(btnClose);
            add(bottom, BorderLayout.SOUTH);

            btnClose.addActionListener(e -> dispose());
        }
    }


    private class AddContactDialog extends JDialog {
        AddContactDialog(Window owner, User user, Runnable onSuccess) {
            super(owner, "Add Emergency Contact", ModalityType.APPLICATION_MODAL);
            setSize(500, 350);
            setLocationRelativeTo(owner);
            getContentPane().setBackground(COLOR_BG_LIGHT_PURPLE);
            setLayout(new BorderLayout(15, 15));
            getRootPane().setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

            JLabel lblTitle = new JLabel("Add New Contact", SwingConstants.CENTER);
            lblTitle.setFont(FONT_TITLE);
            lblTitle.setForeground(COLOR_TITLE_CORAL);
            add(lblTitle, BorderLayout.NORTH);

            JPanel formPanel = new JPanel(new GridBagLayout());
            formPanel.setBackground(Color.WHITE);
            formPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(COLOR_BORDER_CORAL),
                    BorderFactory.createEmptyBorder(20, 20, 20, 20)
            ));

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(8, 8, 8, 8);
            gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.EAST;
            JLabel nameLabel = new JLabel("Name:");
            nameLabel.setFont(FONT_LABEL);
            formPanel.add(nameLabel, gbc);

            gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
            JTextField tfName = new JTextField(20);
            tfName.setFont(FONT_FIELD);
            formPanel.add(tfName, gbc);

            gbc.gridx = 0; gbc.gridy++; gbc.anchor = GridBagConstraints.EAST;
            JLabel phoneLabel = new JLabel("Phone:");
            phoneLabel.setFont(FONT_LABEL);
            formPanel.add(phoneLabel, gbc);

            gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
            JTextField tfPhone = new JTextField(20);
            tfPhone.setFont(FONT_FIELD);
            formPanel.add(tfPhone, gbc);

            gbc.gridx = 0; gbc.gridy++; gbc.anchor = GridBagConstraints.EAST;
            JLabel relLabel = new JLabel("Relationship:");
            relLabel.setFont(FONT_LABEL);
            formPanel.add(relLabel, gbc);

            gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
            JTextField tfRel = new JTextField(20);
            tfRel.setFont(FONT_FIELD);
            formPanel.add(tfRel, gbc);

            add(formPanel, BorderLayout.CENTER);

            JPanel bottom = new JPanel();
            bottom.setOpaque(false);
            JButton btnSubmit = new JButton("Add Contact");
            styleButton(btnSubmit);

            JButton btnCancel = new JButton("Cancel");
            styleButton(btnCancel);

            bottom.add(btnSubmit);
            bottom.add(btnCancel);
            add(bottom, BorderLayout.SOUTH);

            btnCancel.addActionListener(e -> dispose());
            btnSubmit.addActionListener(e -> {
                String name = tfName.getText().trim();
                String phone = tfPhone.getText().trim();
                String rel = tfRel.getText().trim();

                if (name.isEmpty() || phone.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Name and phone are required.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                boolean ok = WomenSafetyApp.addEmergencyContact(user.id, name, phone, rel);
                if (ok) {
                    JOptionPane.showMessageDialog(this, "Contact added successfully.");
                    dispose();
                    if (onSuccess != null) onSuccess.run();
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to add contact.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            });
        }
    }

    private class SendAlertDialog extends JDialog {
        SendAlertDialog(Window owner, User user) {
            super(owner, "Send Quick Alert", ModalityType.APPLICATION_MODAL);
            setSize(500, 400);
            setLocationRelativeTo(owner);
            getContentPane().setBackground(COLOR_BG_LIGHT_PURPLE);
            setLayout(new BorderLayout(15, 15));
            getRootPane().setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

            JLabel lblTitle = new JLabel("Send Quick Alert", SwingConstants.CENTER);
            lblTitle.setFont(FONT_TITLE);
            lblTitle.setForeground(COLOR_TITLE_CORAL);
            add(lblTitle, BorderLayout.NORTH);

            JPanel formPanel = new JPanel(new BorderLayout(10, 10));
            formPanel.setBackground(Color.WHITE);
            formPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(COLOR_BORDER_CORAL),
                    BorderFactory.createEmptyBorder(20, 20, 20, 20)
            ));

            JLabel msgLabel = new JLabel("Write a quick alert message to save/send:");
            msgLabel.setFont(FONT_LABEL);
            formPanel.add(msgLabel, BorderLayout.NORTH);

            JTextArea taMessage = new JTextArea(10, 28);
            taMessage.setLineWrap(true);
            taMessage.setWrapStyleWord(true);
            taMessage.setFont(FONT_FIELD);
            formPanel.add(new JScrollPane(taMessage), BorderLayout.CENTER);

            add(formPanel, BorderLayout.CENTER);

            JPanel bottom = new JPanel();
            bottom.setOpaque(false);
            JButton btnSubmit = new JButton("Send Alert");
            styleButton(btnSubmit);

            JButton btnCancel = new JButton("Cancel");
            styleButton(btnCancel);

            bottom.add(btnSubmit);
            bottom.add(btnCancel);
            add(bottom, BorderLayout.SOUTH);

            btnCancel.addActionListener(e -> dispose());
            btnSubmit.addActionListener(e -> {
                String msg = taMessage.getText().trim();
                if (msg.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Message cannot be empty.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                boolean ok = WomenSafetyApp.sendAlert(user.id, msg);
                if (ok) {
                    JOptionPane.showMessageDialog(this, "Alert saved and (simulated) sent.");
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to send alert.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            });
        }
    }


    private class ReportDialog extends JDialog {
        ReportDialog(Window owner, User user, Runnable onSuccess) {
            super(owner, "Report Incident", ModalityType.APPLICATION_MODAL);
            setSize(500, 510);
            setLocationRelativeTo(owner);
            getContentPane().setBackground(COLOR_BG_LIGHT_PURPLE);
            setLayout(new BorderLayout(15, 15));
            getRootPane().setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

            JLabel lblTitle = new JLabel("Report Incident", SwingConstants.CENTER);
            lblTitle.setFont(FONT_TITLE);
            lblTitle.setForeground(COLOR_TITLE_CORAL);
            add(lblTitle, BorderLayout.NORTH);

            JPanel form = new JPanel(new GridBagLayout());
            form.setBackground(Color.WHITE);
            form.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(COLOR_BORDER_CORAL),
                    BorderFactory.createEmptyBorder(20, 20, 20, 20)
            ));

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(6, 6, 12, 6);
            gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.EAST;
            JLabel titleLabel = new JLabel("Title:");
            titleLabel.setFont(FONT_LABEL);
            form.add(titleLabel, gbc);

            gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            JTextField tfTitle = new JTextField();
            tfTitle.setFont(FONT_FIELD);
            form.add(tfTitle, gbc);
            gbc.fill = GridBagConstraints.NONE;
            gbc.weightx = 0.0;


            gbc.gridx = 0; gbc.gridy++;
            gbc.anchor = GridBagConstraints.NORTHEAST;
            JLabel descLabel = new JLabel("Description:");
            descLabel.setFont(FONT_LABEL);
            form.add(descLabel, gbc);

            gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            JTextArea taDesc = new JTextArea(12, 0);
            taDesc.setLineWrap(true);
            taDesc.setWrapStyleWord(true);
            taDesc.setFont(FONT_FIELD);
            JScrollPane descScrollPane = new JScrollPane(taDesc);
            form.add(descScrollPane, gbc);
            gbc.fill = GridBagConstraints.NONE;
            gbc.weightx = 0.0;


            gbc.gridx = 0; gbc.gridy++;
            gbc.anchor = GridBagConstraints.EAST;
            JLabel locLabel = new JLabel("Location (text):");
            locLabel.setFont(FONT_LABEL);
            form.add(locLabel, gbc);

            gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            JTextField tfLoc = new JTextField();
            tfLoc.setFont(FONT_FIELD);
            form.add(tfLoc, gbc);
            gbc.fill = GridBagConstraints.NONE;
            gbc.weightx = 0.0;


            gbc.gridx = 0; gbc.gridy++;
            gbc.anchor = GridBagConstraints.EAST;
            JLabel sevLabel = new JLabel("Severity:");
            sevLabel.setFont(FONT_LABEL);
            form.add(sevLabel, gbc);

            gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
            JComboBox<String> cbSeverity = new JComboBox<>(new String[]{"Low","Medium","High","Critical"});
            cbSeverity.setFont(FONT_FIELD);
            form.add(cbSeverity, gbc);

            add(form, BorderLayout.CENTER);

            JPanel bottom = new JPanel();
            bottom.setOpaque(false);
            JButton btnSubmit = new JButton("Submit");
            styleButton(btnSubmit);

            JButton btnCancel = new JButton("Cancel");
            styleButton(btnCancel);

            bottom.add(btnSubmit); bottom.add(btnCancel);
            add(bottom, BorderLayout.SOUTH);

            btnCancel.addActionListener(e -> dispose());

            btnSubmit.addActionListener(e -> {
                String t = tfTitle.getText().trim();
                String d = taDesc.getText().trim();
                String l = tfLoc.getText().trim();
                String s = (String) cbSeverity.getSelectedItem();
                if (t.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Title required.");
                    return;
                }

                boolean r = WomenSafetyApp.reportIncident(user == null ? null : user.id, t, d, l, s);

                if (r) {
                    JOptionPane.showMessageDialog(this, "Incident recorded. Authorities/contacts will be notified (simulated).");
                    dispose();
                    if (onSuccess != null) onSuccess.run();
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to record incident.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            });
        }
    }

    private class SafetyTipsDialog extends JDialog {
        SafetyTipsDialog(Window owner) {
            super(owner, "Safety Tips & Resources", ModalityType.APPLICATION_MODAL);
            setSize(550, 450);
            setLocationRelativeTo(owner);
            getContentPane().setBackground(COLOR_BG_LIGHT_PURPLE);
            setLayout(new BorderLayout(15, 15));
            getRootPane().setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

            JLabel lblTitle = new JLabel("Safety Tips & Resources", SwingConstants.CENTER);
            lblTitle.setFont(FONT_TITLE);
            lblTitle.setForeground(COLOR_TITLE_CORAL);
            add(lblTitle, BorderLayout.NORTH);

            JTabbedPane tipsTabs = new JTabbedPane();
            tipsTabs.setFont(FONT_LABEL);

            JTextArea taTips = new JTextArea();
            taTips.setFont(FONT_FIELD);
            taTips.setEditable(false);
            taTips.setWrapStyleWord(true);
            taTips.setLineWrap(true);
            taTips.setOpaque(false);
            taTips.setMargin(new Insets(10, 10, 10, 10));
            taTips.setText(
                    "1. Be Aware of Your Surroundings:\n" +
                            "   - Avoid distractions like your phone or loud music when walking alone.\n" +
                            "   - Stick to well-lit, busy streets, especially at night.\n\n" +
                            "2. Trust Your Instincts:\n" +
                            "   - If a situation or person feels wrong, leave immediately. Don't worry about being polite.\n\n" +
                            "3. Share Your Plans:\n" +
                            "   - Tell a trusted friend or family member where you are going and when you expect to be back.\n" +
                            "   - Share your live location if using a ride-sharing app.\n\n" +
                            "4. Be Confident:\n" +
                            "   - Walk confidently and at a steady pace. Make eye contact with people you pass.\n\n" +
                            "5. Secure Your Home:\n" +
                            "   - Always lock your doors and windows, even if you're only gone for a few minutes.\n" +
                            "   - Don't open the door to strangers without verification.\n\n" +
                            "6. Public Transport Safety:\n" +
                            "   - Try to sit near the driver or in a car with other people.\n" +
                            "   - Be aware of who gets on and off with you.\n\n" +
                            "7. Digital Safety:\n" +
                            "   - Be cautious about sharing personal information online.\n" +
                            "   - Use strong, unique passwords for your accounts."
            );
            taTips.setCaretPosition(0);
            JScrollPane tipsScrollPane = new JScrollPane(taTips);
            tipsScrollPane.setBorder(BorderFactory.createLineBorder(COLOR_BORDER_CORAL));
            tipsTabs.addTab("Safety Tips", UIManager.getIcon("OptionPane.informationIcon"), tipsScrollPane);

            JTextArea taHelplines = new JTextArea();
            taHelplines.setFont(new Font("Monospaced", Font.BOLD, 16));
            taHelplines.setEditable(false);
            taHelplines.setOpaque(false);
            taHelplines.setMargin(new Insets(10, 10, 10, 10));
            taHelplines.setText(
                    "=== NATIONAL EMERGENCY NUMBERS ===\n\n" +
                            "Police:                    100 / 112\n\n" +
                            "Women Helpline:            1091 / 181\n\n" +
                            "Child Helpline:            1098\n\n" +
                            "Ambulance:                 102 / 108\n\n" +
                            "Cyber Crime Helpline:      1930\n\n" +
                            "Railway Protection Force:  139 / 182\n\n" +
                            "\n=== OTHER RESOURCES ===\n\n" +
                            "National Commission for Women (NCW):\n" +
                            "   - Website: http://ncw.nic.in/\n\n" +
                            "Central Social Welfare Board (CSWB):\n" +
                            "   - Website: http://cswb.gov.in/"
            );
            taHelplines.setCaretPosition(0);
            JScrollPane helplineScrollPane = new JScrollPane(taHelplines);
            helplineScrollPane.setBorder(BorderFactory.createLineBorder(COLOR_BORDER_CORAL));
            tipsTabs.addTab("Helpline Numbers", UIManager.getIcon("FileView.floppyDriveIcon"), helplineScrollPane);

            add(tipsTabs, BorderLayout.CENTER);

            JPanel bottom = new JPanel();
            bottom.setOpaque(false);
            JButton btnClose = new JButton("Close");
            styleButton(btnClose);
            bottom.add(btnClose);
            add(bottom, BorderLayout.SOUTH);

            btnClose.addActionListener(e -> dispose());
        }
    }


    public static void main(String[] args) {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException ex) {
            JOptionPane.showMessageDialog(null, "Postgres JDBC driver not found. Add the driver to the classpath.", "Driver missing", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        SwingUtilities.invokeLater(() -> {
            WomenSafetyApp app = new WomenSafetyApp();
        });
    }
}