package org.furnish.ui;

import javax.swing.*;
import org.furnish.utils.CloseButtonUtil;
import org.furnish.utils.FirebaseUtil;
import org.json.JSONObject;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.net.URL;

public class ProfileScreen extends JFrame {
    private JTextField nameField;
    private JTextField usernameField;
    private JTextField emailField;
    private JTextField roleField;

    public ProfileScreen() {
        setTitle("Furnish Studio - Profile");
        setSize(500, 900);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setUndecorated(true);
        setShape(new RoundRectangle2D.Double(0, 0, 500, 900, 30, 30));

        // Main panel with gradient background
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                GradientPaint gradient = new GradientPaint(0, 0, new Color(23, 23, 38), 0, getHeight(),
                        new Color(42, 42, 74));
                g2d.setPaint(gradient);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);

                g2d.setColor(new Color(255, 255, 255, 20));
                for (int i = 0; i < 3; i++) {
                    g2d.fillOval(100 + i * 150, 50, 80, 80);
                }
            }
        };
        mainPanel.setLayout(new BorderLayout());
        add(mainPanel);

        // Close button
        JButton closeButton = CloseButtonUtil.createCloseButton();
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topPanel.setOpaque(false);
        topPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        topPanel.add(closeButton);
        mainPanel.add(topPanel, BorderLayout.NORTH);

        // Content panel
        JPanel contentPanel = new JPanel();
        contentPanel.setOpaque(false);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(30, 50, 30, 50));

        // Logo
        ImageIcon logoIcon = null;
        URL imageUrl = getClass().getResource("/images/sofa.png");
        if (imageUrl != null) {
            logoIcon = new ImageIcon(imageUrl);
        } else {
            System.err.println("Image not found!");
            logoIcon = new ImageIcon(new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB));
        }
        JLabel logo = new JLabel(logoIcon);
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Profile picture placeholder
        JLabel profilePicture = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(new Color(80, 80, 110));
                g2d.fillOval(0, 0, getWidth(), getHeight());
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Montserrat", Font.BOLD, 40));
                FontMetrics fm = g2d.getFontMetrics();
                String initial = FirebaseUtil.getCurrentUser() != null
                        ? FirebaseUtil.getCurrentUser().getString("email").substring(0, 1).toUpperCase()
                        : "U";
                int x = (getWidth() - fm.stringWidth(initial)) / 2;
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2d.drawString(initial, x, y);
            }
        };
        profilePicture.setPreferredSize(new Dimension(120, 120));
        profilePicture.setMaximumSize(new Dimension(120, 120));
        profilePicture.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Title
        JLabel titleLabel = new JLabel("USER PROFILE");
        titleLabel.setFont(new Font("Montserrat", Font.BOLD, 28));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 30, 0));

        // Input fields panel
        JPanel inputPanel = new JPanel();
        inputPanel.setOpaque(false);
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(0, 30, 0, 30));

        // Name field
        nameField = createStyledTextField("Loading...");
        nameField.setEditable(false);
        inputPanel.add(nameField);
        inputPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        // Username field
        usernameField = createStyledTextField("Loading...");
        usernameField.setEditable(false);
        inputPanel.add(usernameField);
        inputPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        // Email field
        emailField = createStyledTextField("Loading...");
        emailField.setEditable(false);
        inputPanel.add(emailField);
        inputPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        // Role field
        roleField = createStyledTextField("Loading...");
        roleField.setEditable(false);
        inputPanel.add(roleField);
        inputPanel.add(Box.createRigidArea(new Dimension(0, 30)));

        // Back to Design button
        JButton backButton = new RoundedButton("BACK TO DESIGN");
        backButton.setFont(new Font("Montserrat", Font.BOLD, 16));
        backButton.setBackground(new Color(92, 184, 92));
        backButton.setForeground(Color.WHITE);
        backButton.setFocusPainted(false);
        backButton.setBorder(BorderFactory.createEmptyBorder(15, 40, 15, 40));
        backButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        backButton.addActionListener(e -> {
            new FurnitureDesignApp().setVisible(true);
            dispose();
        });

        // Logout button
        JButton logoutButton = new RoundedButton("LOGOUT");
        logoutButton.setFont(new Font("Montserrat", Font.BOLD, 16));
        logoutButton.setBackground(new Color(255, 100, 100));
        logoutButton.setForeground(Color.WHITE);
        logoutButton.setFocusPainted(false);
        logoutButton.setBorder(BorderFactory.createEmptyBorder(15, 40, 15, 40));
        logoutButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        logoutButton.addActionListener(e -> {
            FirebaseUtil.clearCurrentUser();
            new LoginScreen().setVisible(true);
            dispose();
        });

        // Add components to content panel
        contentPanel.add(logo);
        contentPanel.add(profilePicture);
        contentPanel.add(titleLabel);
        contentPanel.add(inputPanel);
        contentPanel.add(backButton);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        contentPanel.add(logoutButton);

        mainPanel.add(contentPanel, BorderLayout.CENTER);

        // Footer
        JLabel footer = new JLabel("© 2025 Furnish Studio | Terms | Privacy");
        footer.setFont(new Font("Montserrat", Font.PLAIN, 12));
        footer.setForeground(new Color(150, 150, 150));
        footer.setHorizontalAlignment(SwingConstants.CENTER);
        footer.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        mainPanel.add(footer, BorderLayout.SOUTH);

        // Fetch user data asynchronously
        fetchUserData();
    }

    // Fetch user data from Firebase
    private void fetchUserData() {
        SwingWorker<JSONObject, Void> worker = new SwingWorker<>() {
            @Override
            protected JSONObject doInBackground() {
                JSONObject user = FirebaseUtil.getCurrentUser();
                if (user == null) {
                    JSONObject error = new JSONObject();
                    error.put("error", "No user is logged in");
                    return error;
                }
                String uid = user.getString("uid");
                System.out.println("Fetching user data for UID: " + uid);
                return FirebaseUtil.getUserData(uid);
            }

            @Override
            protected void done() {
                try {
                    JSONObject userData = get();
                    if (userData.has("error")) {
                        JOptionPane.showMessageDialog(ProfileScreen.this,
                                userData.getString("error"), "Error", JOptionPane.ERROR_MESSAGE);
                        nameField.setText("N/A");
                        usernameField.setText("N/A");
                        emailField.setText(
                                FirebaseUtil.getCurrentUser() != null ? FirebaseUtil.getCurrentUser().getString("email")
                                        : "N/A");
                        roleField.setText("N/A");
                    } else {
                        nameField.setText(userData.getString("name"));
                        usernameField.setText(userData.getString("username"));
                        emailField.setText(userData.getString("email"));
                        roleField.setText(userData.getString("role"));
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ProfileScreen.this,
                            "Error fetching user data: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    nameField.setText("N/A");
                    usernameField.setText("N/A");
                    emailField.setText(
                            FirebaseUtil.getCurrentUser() != null ? FirebaseUtil.getCurrentUser().getString("email")
                                    : "N/A");
                    roleField.setText("N/A");
                }
            }
        };
        worker.execute();
    }

    // Custom styled text field
    private JTextField createStyledTextField(String text) {
        JTextField textField = new JTextField(text, 20);
        textField.setFont(new Font("Montserrat", Font.PLAIN, 16));
        textField.setForeground(Color.WHITE);
        textField.setBackground(new Color(60, 60, 90));
        textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(80, 80, 110), 2),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)));
        textField.setCaretColor(Color.WHITE);
        textField.setAlignmentX(Component.CENTER_ALIGNMENT);
        textField.setEditable(false);
        return textField;
    }

    // Custom rounded button class
    class RoundedButton extends JButton {
        public RoundedButton(String text) {
            super(text);
            setContentAreaFilled(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (getModel().isPressed()) {
                g2.setColor(getBackground().darker());
            } else if (getModel().isRollover()) {
                g2.setColor(getBackground().brighter());
            } else {
                g2.setColor(getBackground());
            }

            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 25, 25);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}