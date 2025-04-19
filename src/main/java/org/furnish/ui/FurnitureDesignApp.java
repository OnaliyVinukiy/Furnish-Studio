package org.furnish.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;

import org.furnish.core.Design;
import org.furnish.core.Furniture;
import org.furnish.core.Room;
import org.furnish.models.FurnitureUndoManager;
import org.furnish.utils.CloseButtonUtil;
import org.furnish.utils.FirebaseUtil;
import org.json.JSONObject;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;

public class FurnitureDesignApp extends JFrame {
    private VisualizationPanel visualizationPanel;
    PropertiesPanel propertiesPanel;
    private Design currentDesign;
    private Furniture selectedFurniture;
    private JLabel statusLabel;
    private JToggleButton view2D3DToggle;
    private double zoomFactor = 1.0;
    private static final double ZOOM_STEP = 0.1;
    private static final double MIN_ZOOM = 0.5;
    private static final double MAX_ZOOM = 3.0;
    private final FurnitureUndoManager undoManager = new FurnitureUndoManager();
    private JButton undoButton;
    private JButton redoButton;
    private JButton deleteButton;
    private JButton gridButton;

    public FurnitureDesignApp() {
        initializeModernUI();
        setupModernMenuBar();
        setupToolbar();
        setupPanels();
        setupStatusBar();
    }

    public void zoomIn() {
        zoomFactor += ZOOM_STEP;
        if (zoomFactor > MAX_ZOOM) {
            zoomFactor = MAX_ZOOM;
        }
        repaint();
    }

    public void zoomOut() {
        zoomFactor -= ZOOM_STEP;
        if (zoomFactor < MIN_ZOOM) {
            zoomFactor = MIN_ZOOM;
        }
        repaint();
    }

    public void resetView() {
        zoomFactor = 1.0;
        repaint();
    }

    private void initializeModernUI() {
        setTitle("Furnish Studio - Designer");
        setSize(1200, 850);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setUndecorated(true);
        setShape(new RoundRectangle2D.Double(0, 0, 1200, 850, 30, 30));

        JPanel mainPanel = createMainPanel();
        add(mainPanel);
    }

    private JPanel createMainPanel() {
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                GradientPaint gradient = new GradientPaint(
                        0, 0, new Color(23, 23, 38),
                        getWidth(), getHeight(), new Color(42, 42, 74));
                g2d.setPaint(gradient);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);

                g2d.setColor(new Color(255, 255, 255, 15));
                for (int i = 0; i < 5; i++) {
                    g2d.fillOval(150 + i * 180, 50, 100, 100);
                }
            }
        };
        mainPanel.setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel titleLabel = new JLabel("Furnish Studio");
        titleLabel.setFont(new Font("Montserrat", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
        topPanel.add(titleLabel, BorderLayout.WEST);

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        controlPanel.setOpaque(false);

        JButton minimizeButton = new JButton("—");
        styleControlButton(minimizeButton);
        minimizeButton.addActionListener(e -> setState(Frame.ICONIFIED));
        controlPanel.add(minimizeButton);

        JButton closeButton = CloseButtonUtil.createCloseButton();
        styleControlButton(closeButton);
        controlPanel.add(closeButton);

        topPanel.add(controlPanel, BorderLayout.EAST);
        mainPanel.add(topPanel, BorderLayout.NORTH);

        return mainPanel;
    }

    private void styleControlButton(JButton button) {
        button.setFont(new Font("Montserrat", Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(60, 60, 90));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));
        button.setContentAreaFilled(false);
        button.setOpaque(true);

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(80, 80, 110));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(new Color(60, 60, 90));
            }
        });
    }

    private void setupModernMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(new Color(30, 30, 45));
        menuBar.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

        JMenu fileMenu = createStyledMenu("File");

        addMenuItems(fileMenu,
                createStyledMenuItem("New Design", "/images/add.png", e -> newDesign()),
                createStyledMenuItem("Open Design", "/images/share.png", e -> showSavedDesigns()),
                createStyledMenuItem("Save Design", "/images/diskette.png", e -> saveDesign()),
                new JSeparator(),
                createStyledMenuItem("Exit", "/images/exit.png", e -> dispose()));

        JMenu editMenu = createStyledMenu("Edit");
        addMenuItems(editMenu,
                createStyledMenuItem("Undo", "/images/undo.png", e -> performUndo()),
                createStyledMenuItem("Redo", "/images/forward.png", e -> performRedo()),
                new JSeparator(),
                createStyledMenuItem("Delete Selected", "/images/delete.png", e -> deleteSelectedFurniture()));

        JMenu viewMenu = createStyledMenu("View");
        addMenuItems(viewMenu,
                createStyledMenuItem("Zoom In", "/images/zoom-in.png", e -> visualizationPanel.zoomIn()),
                createStyledMenuItem("Zoom Out", "/images/magnifying-glass.png", e -> visualizationPanel.zoomOut()),
                new JSeparator(),
                createStyledMenuItem("Reset View", "/images/reset.png", e -> {
                    visualizationPanel.resetView();
                    visualizationPanel.zoomIn();
                    visualizationPanel.zoomIn();
                    visualizationPanel.zoomIn();
                }));

        JMenu profileMenu = createStyledMenu("Profile");
        addMenuItems(profileMenu,
                createStyledMenuItem("View Profile", "/images/user.png", e -> {
                    new ProfileScreen().setVisible(true);
                    FurnitureDesignApp.this.dispose();
                }));

        JMenu furnitureMenu = createStyledMenu("Furniture");

        // Chair menu with subtypes
        JMenu chairMenu = createStyledMenu("Add Chair");
        addMenuItems(chairMenu,
                createStyledMenuItem("Standard Chair", "/images/close.png", e -> addFurniture("Chair", "Standard")),
                createStyledMenuItem("Armchair", "/images/close.png", e -> addFurniture("Chair", "Armchair")),
                createStyledMenuItem("Dining Chair", "/images/close.png", e -> addFurniture("Chair", "Dining")));

        // Add Table menu with subtypes
        JMenu tableMenu = createStyledMenu("Add Table");
        addMenuItems(tableMenu,
                createStyledMenuItem("Coffee Table", "/images/box.png", e -> addFurniture("Table", "Coffee")),
                createStyledMenuItem("Dining Table", "/images/box.png", e -> addFurniture("Table", "Dining")),
                createStyledMenuItem("Desk", "/images/box.png", e -> addFurniture("Table", "Desk")));

        // Add Sofa menu with subtypes
        JMenu sofaMenu = createStyledMenu("Add Sofa");
        addMenuItems(sofaMenu,
                createStyledMenuItem("2-Seater", "/images/box.png", e -> addFurniture("Sofa", "2-Seater")),
                createStyledMenuItem("3-Seater", "/images/box.png", e -> addFurniture("Sofa", "3-Seater")),
                createStyledMenuItem("Sectional", "/images/box.png", e -> addFurniture("Sofa", "Sectional")));

        // Add Bed menu with subtypes
        JMenu bedMenu = createStyledMenu("Add Bed");
        addMenuItems(bedMenu,
                createStyledMenuItem("Single", "/images/box.png", e -> addFurniture("Bed", "Single")),
                createStyledMenuItem("Double", "/images/box.png", e -> addFurniture("Bed", "Double")),
                createStyledMenuItem("King", "/images/box.png", e -> addFurniture("Bed", "King")));

        // Add Cabinet menu with subtypes
        JMenu cabinetMenu = createStyledMenu("Add Cabinet");
        addMenuItems(cabinetMenu,
                createStyledMenuItem("Bookshelf", "/images/box.png", e -> addFurniture("Cabinet", "Bookshelf")),
                createStyledMenuItem("Wardrobe", "/images/box.png", e -> addFurniture("Cabinet", "Wardrobe")),
                createStyledMenuItem("Kitchen", "/images/box.png", e -> addFurniture("Cabinet", "Kitchen")));

        addMenuItems(furnitureMenu,
                chairMenu,
                tableMenu,
                sofaMenu,
                bedMenu,
                cabinetMenu);

        // Create the close button with perfect circular shape
        JButton closeButton = new JButton("×") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(32, 32);
            }

            @Override
            public Dimension getMinimumSize() {
                return getPreferredSize();
            }

            @Override
            public Dimension getMaximumSize() {
                return getPreferredSize();
            }
        };

        closeButton.setFocusPainted(false);
        closeButton.setContentAreaFilled(false);
        closeButton.setOpaque(false);
        closeButton.setBackground(new Color(255, 100, 100));
        closeButton.setForeground(Color.WHITE);
        closeButton.setFont(new Font("Arial", Font.BOLD, 16));
        closeButton.setBorder(BorderFactory.createEmptyBorder());
        closeButton.setHorizontalTextPosition(JButton.CENTER);
        closeButton.setVerticalTextPosition(JButton.CENTER);

        closeButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                closeButton.setBackground(new Color(255, 70, 70));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                closeButton.setBackground(new Color(255, 100, 100));
            }
        });

        closeButton.addActionListener(e -> dispose());
        closeButton.setToolTipText("Close the application");
        closeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(viewMenu);
        menuBar.add(furnitureMenu);
        menuBar.add(profileMenu);
        menuBar.add(Box.createHorizontalGlue());
        menuBar.add(closeButton);

        setJMenuBar(menuBar);
    }

    private JMenu createStyledMenu(String text) {
        JMenu menu = new JMenu(text) {
            @Override
            public JPopupMenu getPopupMenu() {
                JPopupMenu popupMenu = super.getPopupMenu();
                popupMenu.setBackground(new Color(60, 60, 90));
                return popupMenu;
            }
        };
        menu.setForeground(Color.WHITE);
        menu.setBackground(new Color(60, 60, 90));
        menu.setFont(new Font("Montserrat", Font.PLAIN, 14));
        menu.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        return menu;
    }

    private JMenuItem createStyledMenuItem(String text, String iconPath, ActionListener action) {
        JMenuItem item = new JMenuItem(text);
        item.setForeground(Color.WHITE);
        item.setBackground(new Color(60, 60, 90));
        item.setFont(new Font("Montserrat", Font.PLAIN, 13));
        item.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        item.addActionListener(action);

        try {
            java.net.URL imageUrl = getClass().getResource(iconPath);
            if (imageUrl != null) {
                BufferedImage originalImage = ImageIO.read(imageUrl);
                Image resizedImage = originalImage.getScaledInstance(13, 13, Image.SCALE_SMOOTH);
                item.setIcon(new ImageIcon(resizedImage));
            } else {
                System.out.println("Icon not found: " + iconPath);
            }
        } catch (Exception e) {
            System.out.println("Error loading icon: " + e.getMessage());
        }

        item.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                item.setBackground(new Color(80, 80, 110));
            }

            public void mouseExited(MouseEvent evt) {
                item.setBackground(new Color(60, 60, 90));
            }
        });

        return item;
    }

    private ImageIcon loadResizedIcon(String path, int width, int height) {
        try {
            URL imageUrl = getClass().getResource(path);
            if (imageUrl != null) {
                BufferedImage originalImage = ImageIO.read(imageUrl);
                Image resizedImage = originalImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                return new ImageIcon(resizedImage);
            } else {
                System.out.println("Icon not found: " + path);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to load icon: " + path);
        }
        return null;
    }

    private void setupToolbar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setBackground(new Color(40, 40, 60));
        toolBar.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        JButton newButton = createToolbarButton("New", "/images/add.png");
        newButton.addActionListener(e -> newDesign());
        toolBar.add(newButton);

        JButton openButton = createToolbarButton("Open", "/images/share.png");
        openButton.addActionListener(e -> showSavedDesigns());
        toolBar.add(openButton);

        JButton saveButton = createToolbarButton("Save", "/images/diskette.png");
        saveButton.addActionListener(e -> saveDesign());
        toolBar.add(saveButton);

        toolBar.addSeparator();
        toolBar.addSeparator();

        view2D3DToggle = new JToggleButton("3D View", loadResizedIcon("/images/3d.png", 20, 20));
        styleToolbarButton(view2D3DToggle);

        view2D3DToggle.addActionListener(e -> {
            boolean is3D = view2D3DToggle.isSelected();

            if (is3D) {
                gridButton.setEnabled(true);
            } else {
                gridButton.setEnabled(false);
            }

            visualizationPanel.set3DView(is3D);

            String newText = is3D ? "2D View" : "3D View";
            String newIconPath = is3D ? "/images/2d.png" : "/images/3d.png";

            view2D3DToggle.setText(newText);
            view2D3DToggle.setIcon(loadResizedIcon(newIconPath, 20, 20));

            if (is3D) {
                visualizationPanel.zoomIn();
                visualizationPanel.zoomIn();
                visualizationPanel.zoomIn();
                visualizationPanel.zoomIn();
            } else {
                visualizationPanel.resetView();
                visualizationPanel.zoomIn();
                visualizationPanel.zoomIn();
                visualizationPanel.zoomIn();
                visualizationPanel.zoomIn();
            }
        });

        toolBar.add(view2D3DToggle);

        JButton zoomInButton = createToolbarButton("Zoom In", "/images/magnifying-glass.png");
        zoomInButton.addActionListener(e -> visualizationPanel.zoomIn());
        toolBar.add(zoomInButton);

        JButton zoomOutButton = createToolbarButton("Zoom Out", "/images/zoom-in.png ");
        zoomOutButton.addActionListener(e -> visualizationPanel.zoomOut());
        toolBar.add(zoomOutButton);

        undoButton = createToolbarButton("Undo", "/images/undo.png");
        undoButton.addActionListener(e -> performUndo());
        undoButton.setEnabled(false);
        undoButton.setToolTipText("Undo last action");
        toolBar.add(undoButton);

        redoButton = createToolbarButton("Redo", "/images/forward.png");
        redoButton.addActionListener(e -> performRedo());
        redoButton.setEnabled(false);
        redoButton.setToolTipText("Redo last action");
        toolBar.add(redoButton);

        deleteButton = createToolbarButton("Delete", "/images/delete.png");
        deleteButton.addActionListener(e -> deleteSelectedFurniture());
        deleteButton.setEnabled(true);
        deleteButton.setToolTipText("Delete selected furniture item");
        deleteButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        toolBar.add(deleteButton);

        gridButton = createToolbarButton("Grid", "/images/pixels.png");
        gridButton.addActionListener(e -> {
            boolean updatedState = !visualizationPanel.getToggleGrid();
            visualizationPanel.setToggleGrid(updatedState);
        });
        gridButton.setToolTipText("Toggle 3D View Grid Visibility");
        gridButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        toolBar.add(gridButton);

        undoManager.addUndoableEditListener(e -> updateUndoRedoButtons());

        getContentPane().add(toolBar, BorderLayout.NORTH);
    }

    private void performUndo() {
        if (undoManager.canUndo()) {
            undoManager.undo();
            repaint();
        }
    }

    private void performRedo() {
        if (undoManager.canRedo()) {
            undoManager.redo();
            repaint();
        }
    }

    private void updateUndoRedoButtons() {
        SwingUtilities.invokeLater(() -> {
            undoButton.setEnabled(true);
            redoButton.setEnabled(true);
        });
    }

    private JButton createToolbarButton(String tooltip, String iconPath) {
        JButton button = new JButton();

        try {
            java.net.URL imageUrl = getClass().getResource(iconPath);
            if (imageUrl != null) {
                BufferedImage originalImage = ImageIO.read(imageUrl);
                Image resizedImage = originalImage.getScaledInstance(20, 20, Image.SCALE_SMOOTH);
                ImageIcon resizedIcon = new ImageIcon(resizedImage);
                button.setIcon(resizedIcon);
            } else {
                System.out.println("Icon not found: " + iconPath);
            }
        } catch (Exception e) {
            System.out.println("Error loading icon: " + iconPath);
            e.printStackTrace();
        }

        button.setToolTipText(tooltip);
        styleToolbarButton(button);
        return button;
    }

    private void styleToolbarButton(AbstractButton button) {
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        button.setOpaque(true);
        button.setContentAreaFilled(false);
        button.setBackground(new Color(60, 60, 90));
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Montserrat", Font.PLAIN, 12));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(80, 80, 110));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (button instanceof JToggleButton && ((JToggleButton) button).isSelected()) {
                    button.setBackground(new Color(40, 40, 70));
                } else {
                    button.setBackground(new Color(60, 60, 90));
                }
            }
        });

        if (button instanceof JToggleButton) {
            ((JToggleButton) button).addItemListener(e -> {
                boolean selected = ((JToggleButton) button).isSelected();
                button.setBackground(selected ? new Color(40, 40, 70) : new Color(60, 60, 90));
            });
        }
    }

    private void setupPanels() {
        visualizationPanel = new VisualizationPanel(this);
        visualizationPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 10));
        visualizationPanel.setBackground(new Color(50, 50, 70));

        propertiesPanel = new PropertiesPanel();
        propertiesPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(15, 10, 15, 15),
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(new Color(80, 80, 110)),
                        "Properties",
                        javax.swing.border.TitledBorder.LEFT,
                        javax.swing.border.TitledBorder.TOP,
                        new Font("Montserrat", Font.BOLD, 14),
                        new Color(200, 200, 200))));
        propertiesPanel.setBackground(new Color(40, 40, 60));

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                visualizationPanel,
                propertiesPanel);
        splitPane.setDividerLocation(800);
        splitPane.setDividerSize(3);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        splitPane.setBackground(new Color(40, 40, 60));

        getContentPane().add(splitPane, BorderLayout.CENTER);
    }

    private void setupStatusBar() {
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(80, 80, 110)),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)));
        statusPanel.setBackground(new Color(40, 40, 60));

        statusLabel = new JLabel("Ready");
        statusLabel.setFont(new Font("Montserrat", Font.PLAIN, 12));
        statusLabel.setForeground(new Color(180, 180, 180));
        statusPanel.add(statusLabel, BorderLayout.WEST);

        JLabel versionLabel = new JLabel("Furnish Studio v1.0");
        versionLabel.setFont(new Font("Montserrat", Font.PLAIN, 12));
        versionLabel.setForeground(new Color(180, 180, 180));
        statusPanel.add(versionLabel, BorderLayout.EAST);

        getContentPane().add(statusPanel, BorderLayout.SOUTH);
    }

    void updateStatus(String message) {
        statusLabel.setText(message);
    }

    private void newDesign() {
        RoomDialog dialog = new RoomDialog(this);
        dialog.setVisible(true);
        if (dialog.isOk()) {
            currentDesign = new Design(dialog.getRoom());
            visualizationPanel.setDesign(currentDesign);
            visualizationPanel.set3DView(false);
            view2D3DToggle.setSelected(false);
            view2D3DToggle.setText("2D View");
            updateStatus("New room created: " + dialog.getRoom().toString());
            repaint();

            visualizationPanel.resetView();
            visualizationPanel.zoomIn();
            visualizationPanel.zoomIn();
            visualizationPanel.zoomIn();
            visualizationPanel.zoomIn();
        }
    }

    private void saveDesign() {
        if (currentDesign == null) {
            showErrorDialog("No design to save. Please create a new design first.");
            return;
        }

        JSONObject currentUser = FirebaseUtil.getCurrentUser();
        if (currentUser == null) {
            showErrorDialog("You must be logged in to save a design.");
            return;
        }

        String userId = currentUser.getString("uid");
        String designName = JOptionPane.showInputDialog(this, "Enter a name for your design:");
        if (designName == null || designName.trim().isEmpty()) {
            showErrorDialog("Design name cannot be empty.");
            return;
        }

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(currentDesign);
            oos.close();
            byte[] designData = baos.toByteArray();

            String designDataStr = Base64.getEncoder().encodeToString(designData);

            Map<String, Object> designMap = new HashMap<>();
            designMap.put("name", designName);
            designMap.put("designData", designDataStr);
            designMap.put("createdAt", System.currentTimeMillis());
            designMap.put("userId", userId);

            DocumentReference docRef = FirebaseUtil.firestore.collection("designs").document();
            ApiFuture<WriteResult> future = docRef.set(designMap);
            WriteResult result = future.get();
            updateStatus("Design '" + designName + "' saved at " + result.getUpdateTime());
        } catch (Exception ex) {
            showErrorDialog("Error saving design: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void showSavedDesigns() {
        JSONObject currentUser = FirebaseUtil.getCurrentUser();
        if (currentUser == null) {
            showErrorDialog("You must be logged in to view saved designs.");
            return;
        }

        String userId = currentUser.getString("uid");

        try {
            ApiFuture<QuerySnapshot> future = FirebaseUtil.firestore.collection("designs")
                    .whereEqualTo("userId", userId)
                    .get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            if (documents.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No saved designs found.", "Saved Designs",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            JDialog designsDialog = new JDialog(this, "Saved Designs", true);
            designsDialog.setLayout(new BorderLayout());

            DefaultListModel<String> listModel = new DefaultListModel<>();
            Map<String, String> designIdMap = new HashMap<>();
            for (QueryDocumentSnapshot doc : documents) {
                String designName = doc.getString("name");
                String docId = doc.getId();
                listModel.addElement(designName);
                designIdMap.put(designName, docId);
            }

            JList<String> designsList = new JList<>(listModel);
            designsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            JScrollPane scrollPane = new JScrollPane(designsList);
            designsDialog.add(scrollPane, BorderLayout.CENTER);

            JPanel buttonPanel = new JPanel();
            JButton loadButton = new JButton("Load");
            JButton deleteButton = new JButton("Delete");
            JButton cancelButton = new JButton("Cancel");

            loadButton.addActionListener(e -> {
                String selectedDesignName = designsList.getSelectedValue();
                if (selectedDesignName != null) {
                    String designId = designIdMap.get(selectedDesignName);
                    loadDesign(designId);
                    designsDialog.dispose();
                } else {
                    JOptionPane.showMessageDialog(designsDialog, "Please select a design to load.");
                }
            });

            deleteButton.addActionListener(e -> {
                String selectedDesignName = designsList.getSelectedValue();
                if (selectedDesignName != null) {
                    int confirm = JOptionPane.showConfirmDialog(designsDialog,
                            "Are you sure you want to delete '" + selectedDesignName + "'?",
                            "Confirm Delete", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        String designId = designIdMap.get(selectedDesignName);
                        try {
                            FirebaseUtil.firestore.collection("designs").document(designId).delete().get();
                            listModel.removeElement(selectedDesignName);
                            designIdMap.remove(selectedDesignName);
                            JOptionPane.showMessageDialog(designsDialog, "Design deleted successfully.");
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(designsDialog, "Error deleting design: " + ex.getMessage());
                        }
                    }
                } else {
                    JOptionPane.showMessageDialog(designsDialog, "Please select a design to delete.");
                }
            });

            cancelButton.addActionListener(e -> designsDialog.dispose());

            buttonPanel.add(loadButton);
            buttonPanel.add(deleteButton);
            buttonPanel.add(cancelButton);
            designsDialog.add(buttonPanel, BorderLayout.SOUTH);

            designsDialog.setSize(400, 300);
            designsDialog.setLocationRelativeTo(this);
            designsDialog.setVisible(true);
        } catch (Exception ex) {
            showErrorDialog("Error retrieving saved designs: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void loadDesign(String designId) {
        try {
            DocumentReference docRef = FirebaseUtil.firestore.collection("designs").document(designId);
            ApiFuture<DocumentSnapshot> future = docRef.get();
            DocumentSnapshot document = future.get();

            if (document.exists()) {
                String designDataStr = document.getString("designData");
                byte[] designData = Base64.getDecoder().decode(designDataStr);

                ByteArrayInputStream bais = new ByteArrayInputStream(designData);
                ObjectInputStream ois = new ObjectInputStream(bais);
                Design loadedDesign = (Design) ois.readObject();
                ois.close();

                currentDesign = loadedDesign;
                visualizationPanel.setDesign(currentDesign);
                updateStatus("Loaded design: " + document.getString("name"));
                repaint();
            } else {
                showErrorDialog("Design not found.");
            }
        } catch (Exception ex) {
            showErrorDialog("Error loading design: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void openDesign() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Open Design");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Furnish Design Files (*.furnish)", "furnish"));

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (ObjectInputStream ois = new ObjectInputStream(
                    new FileInputStream(fileChooser.getSelectedFile()))) {
                currentDesign = (Design) ois.readObject();
                visualizationPanel.setDesign(currentDesign);
                updateStatus("Design loaded from: " + fileChooser.getSelectedFile().getAbsolutePath());
                repaint();
            } catch (IOException | ClassNotFoundException ex) {
                showErrorDialog("Error loading design: " + ex.getMessage());
            }
        }
    }

    private void addFurniture(String type, String subtype) {
        if (currentDesign == null) {
            showErrorDialog("Please create a room design first before adding furniture.");
            return;
        }

        Room room = currentDesign.getRoom();
        Color defaultColor;
        double width, depth, height;
        Furniture.Orientation orientation = Furniture.Orientation.NORTH;

        if (type.equals("Chair")) {
            switch (subtype) {
                case "Standard":
                    defaultColor = new Color(180, 120, 70);
                    width = 0.5;
                    depth = 0.5;
                    height = 0.8;
                    break;
                case "Armchair":
                    defaultColor = new Color(100, 80, 120);
                    width = 0.7;
                    depth = 0.7;
                    height = 0.9;
                    break;
                case "Dining":
                    defaultColor = new Color(120, 80, 50);
                    width = 0.45;
                    depth = 0.45;
                    height = 0.85;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown chair subtype: " + subtype);
            }
        } else if (type.equals("Table")) {
            switch (subtype) {
                case "Coffee":
                    defaultColor = new Color(150, 100, 50);
                    width = 1.0;
                    depth = 0.8;
                    height = 0.4;
                    break;
                case "Dining":
                    defaultColor = new Color(160, 110, 60);
                    width = 1.5;
                    depth = 0.9;
                    height = 0.7;
                    break;
                case "Desk":
                    defaultColor = new Color(140, 90, 40);
                    width = 1.2;
                    depth = 0.6;
                    height = 0.75;
                    break;
                default:
                    defaultColor = new Color(150, 100, 50);
                    width = 1.0;
                    depth = 0.8;
                    height = 0.7;
            }
        } else if (type.equals("Sofa")) {
            switch (subtype) {
                case "2-Seater":
                    defaultColor = new Color(120, 80, 180);
                    width = 1.4;
                    depth = 0.7;
                    height = 0.8;
                    break;
                case "3-Seater":
                    defaultColor = new Color(130, 90, 190);
                    width = 2.0;
                    depth = 0.8;
                    height = 0.85;
                    break;
                case "Sectional":
                    defaultColor = new Color(110, 70, 170);
                    width = 2.5;
                    depth = 1.0;
                    height = 0.9;
                    break;
                default:
                    defaultColor = new Color(120, 80, 180);
                    width = 1.8;
                    depth = 0.8;
                    height = 0.8;
            }
        } else if (type.equals("Bed")) {
            switch (subtype) {
                case "Single":
                    defaultColor = new Color(100, 150, 100);
                    width = 0.9;
                    depth = 2.0;
                    height = 0.5;
                    break;
                case "Double":
                    defaultColor = new Color(110, 160, 110);
                    width = 1.4;
                    depth = 2.0;
                    height = 0.5;
                    break;
                case "King":
                    defaultColor = new Color(120, 170, 120);
                    width = 1.8;
                    depth = 2.0;
                    height = 0.5;
                    break;
                default:
                    defaultColor = new Color(100, 150, 100);
                    width = 1.5;
                    depth = 2.0;
                    height = 0.5;
            }
        } else if (type.equals("Cabinet")) {
            switch (subtype) {
                case "Bookshelf":
                    defaultColor = new Color(139, 69, 19);
                    width = 0.8;
                    depth = 0.3;
                    height = 1.8;
                    break;
                case "Wardrobe":
                    defaultColor = new Color(149, 79, 29);
                    width = 1.2;
                    depth = 0.6;
                    height = 1.8;
                    break;
                case "Kitchen":
                    defaultColor = new Color(159, 89, 39);
                    width = 0.6;
                    depth = 0.5;
                    height = 0.9;
                    break;
                default:
                    defaultColor = new Color(139, 69, 19);
                    width = 0.8;
                    depth = 0.4;
                    height = 1.0;
            }
        } else {
            defaultColor = Color.GRAY;
            width = 1.0;
            depth = 1.0;
            height = 1.0;
        }

        double x = (room.getLength() - (orientation == Furniture.Orientation.EAST ||
                orientation == Furniture.Orientation.WEST ? depth : width)) / 2;
        double z = (room.getWidth() - (orientation == Furniture.Orientation.EAST ||
                orientation == Furniture.Orientation.WEST ? width : depth)) / 2;

        Furniture f = new Furniture(type, subtype, x, z, width, depth, height, defaultColor, orientation);
        currentDesign.addFurniture(f);
        undoManager.addFurnitureEdit(currentDesign, f, true);
        setSelectedFurniture(f);

        updateStatus("Added " + type + (subtype.isEmpty() ? "" : " " + subtype) + " facing " + orientation);
        repaint();
    }

    private void deleteSelectedFurniture() {
        if (currentDesign == null) {
            showErrorDialog("No design loaded to delete from.");
            return;
        }

        if (selectedFurniture == null) {
            showErrorDialog("No furniture selected to delete.");
            return;
        }

        if (selectedFurniture != null && currentDesign != null) {
            Furniture toDelete = selectedFurniture;
            currentDesign.removeFurniture(toDelete);
            undoManager.addFurnitureEdit(currentDesign, toDelete, false);
            setSelectedFurniture(null);
            updateStatus("Removed " + toDelete.getType());
            repaint();
        }
    }

    public void setSelectedFurniture(Furniture f) {
        selectedFurniture = f;
        propertiesPanel.update(f);
        if (f != null) {
            updateStatus("Selected " + f.getType() + " - Click and drag to move, use properties panel to edit");
        } else {
            updateStatus("No furniture selected");
        }
    }

    private void showErrorDialog(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                "Error",
                JOptionPane.ERROR_MESSAGE);
        updateStatus("Error: " + message);
    }

    private void addMenuItems(JMenu menu, Component... items) {
        for (Component item : items) {
            if (item instanceof JSeparator) {
                menu.addSeparator();
            } else if (item instanceof JMenuItem) {
                ((JMenuItem) item).setFont(new Font("Montserrat", Font.PLAIN, 13));
                menu.add(item);
            }
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel("com.formdev.flatlaf.FlatDarkLaf");
        } catch (Exception ex) {
            System.err.println("Failed to set modern look and feel");
        }

        FirebaseUtil.initializeFirebase();
        SwingUtilities.invokeLater(() -> {
            FurnitureDesignApp app = new FurnitureDesignApp();
            app.setOpacity(0f);
            app.setVisible(true);

            Timer timer = new Timer(10, new ActionListener() {
                float opacity = 0f;

                @Override
                public void actionPerformed(ActionEvent e) {
                    opacity += 0.05f;
                    if (opacity >= 1f) {
                        ((Timer) e.getSource()).stop();
                    }
                    app.setOpacity(opacity);
                }
            });
            timer.start();
        });
    }
}