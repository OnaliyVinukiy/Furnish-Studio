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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;

import org.furnish.core.Design;
import org.furnish.core.Furniture;
import org.furnish.core.Room;
import org.furnish.models.FurnitureUndoManager;
import org.furnish.utils.CloseButtonUtil;

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

    // refactor
    private final FurnitureUndoManager undoManager = new FurnitureUndoManager();
    private JButton undoButton;
    private JButton redoButton;
    private JButton deleteButton;
    private JButton gridButton;

    //-- refactor

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
                createStyledMenuItem("Open Design", "/images/share.png", e -> openDesign()),
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

        JMenu furnitureMenu = createStyledMenu("Furniture");
        JMenu chairMenu = createStyledMenu("Add Chair");
        addMenuItems(chairMenu,
                createStyledMenuItem("Standard Chair", "../images/close.png", e -> addFurniture("Chair", "Standard")),
                createStyledMenuItem("Armchair", "../images/close.png", e -> addFurniture("Chair", "Armchair")),
                createStyledMenuItem("Dining Chair", "../images/close.png", e -> addFurniture("Chair", "Dining")));

        addMenuItems(furnitureMenu,

                chairMenu,
                createStyledMenuItem("Add Table", "/images/box.png", e -> addFurniture("Table", "")),
                createStyledMenuItem("Add Sofa", "/images/box.png", e -> addFurniture("Sofa", "")),
                createStyledMenuItem("Add Cabinet", "/images/box.png", e -> addFurniture("Cabinet", "")),
                createStyledMenuItem("Add Bed", "/images/box.png", e -> addFurniture("Bed", "")));


        // Create the close button with perfect circular shape
        JButton closeButton = new JButton("×") {
            @Override
            protected void paintComponent(Graphics g) {
                // First paint the background circle
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
                
                // Then paint the text centered
                super.paintComponent(g);
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(32, 32); // MUST be square for perfect circle
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

        // Basic styling
        closeButton.setFocusPainted(false);
        closeButton.setContentAreaFilled(false);
        closeButton.setOpaque(false);
        closeButton.setBackground(new Color(255, 100, 100));
        closeButton.setForeground(Color.WHITE);
        closeButton.setFont(new Font("Arial", Font.BOLD, 16));
        closeButton.setBorder(BorderFactory.createEmptyBorder());

        // Perfect text centering
        closeButton.setHorizontalTextPosition(JButton.CENTER);
        closeButton.setVerticalTextPosition(JButton.CENTER);

        // Hover effects
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

        // Add glue to push the button to the right
        menuBar.add(Box.createHorizontalGlue());
        menuBar.add(closeButton);

        setJMenuBar(menuBar);
    }

    private JMenu createStyledMenu(String text) {
        JMenu menu = new JMenu(text);
        menu.setForeground(Color.WHITE);
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
                // menu item image size
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
        openButton.addActionListener(e -> openDesign());
        toolBar.add(openButton);

        JButton saveButton = createToolbarButton("Save", "/images/diskette.png");
        saveButton.addActionListener(e -> saveDesign());
        toolBar.add(saveButton);

        toolBar.addSeparator();

        JButton chairButton = createToolbarButton("Chair", "/images/box.png");
        JPopupMenu chairPopup = new JPopupMenu();
        JMenuItem standardChair = new JMenuItem("Standard Chair");
        JMenuItem armchair = new JMenuItem("Armchair");
        JMenuItem diningChair = new JMenuItem("Dining Chair");

        standardChair.addActionListener(e -> addFurniture("Chair", "Standard"));
        armchair.addActionListener(e -> addFurniture("Chair", "Armchair"));
        diningChair.addActionListener(e -> addFurniture("Chair", "Dining"));

        chairPopup.add(standardChair);
        chairPopup.add(armchair);
        chairPopup.add(diningChair);

        chairButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                chairPopup.show(chairButton, e.getX(), e.getY());
            }
        });
        toolBar.add(chairButton);

        JButton tableButton = createToolbarButton("Table", "../images/close.png");
        tableButton.addActionListener(e -> addFurniture("Table", ""));
        toolBar.add(tableButton);

        JButton sofaButton = createToolbarButton("Sofa", "../images/close.png");
        sofaButton.addActionListener(e -> addFurniture("Sofa", ""));

        toolBar.add(sofaButton);

        toolBar.addSeparator();

        view2D3DToggle = new JToggleButton("3D View", loadResizedIcon("/images/3d.png", 20, 20));
        styleToolbarButton(view2D3DToggle);


        view2D3DToggle.addActionListener(e -> {
            boolean is3D = view2D3DToggle.isSelected();

            if (is3D) {
                gridButton.setEnabled(true);
            }else{
                gridButton.setEnabled(false);
            }

            visualizationPanel.set3DView(is3D);

            String newText = is3D ? "2D View" : "3D View";
            String newIconPath = is3D ? "/images/2d.png" : "/images/3d.png";

            view2D3DToggle.setText(newText);
            view2D3DToggle.setIcon(loadResizedIcon(newIconPath, 20, 20));

            if(is3D) {
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

        // Undo Button
        undoButton = createToolbarButton("Undo", "/images/undo.png");
        undoButton.addActionListener(e -> performUndo());
        undoButton.setEnabled(false);
        undoButton.setToolTipText("Undo last action");
        toolBar.add(undoButton);

        // Redo Button
        redoButton = createToolbarButton("Redo", "/images/forward.png");
        redoButton.addActionListener(e -> performRedo()); 
        redoButton.setEnabled(false);
        redoButton.setToolTipText("Redo last action");
        toolBar.add(redoButton);

        // Delete Button
        deleteButton = createToolbarButton("Delete", "/images/delete.png");
        deleteButton.addActionListener(e -> deleteSelectedFurniture()); 
        deleteButton.setEnabled(true);
        deleteButton.setToolTipText("Delete selected furniture item");
        deleteButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        toolBar.add(deleteButton);

        this.visualizationPanel = new VisualizationPanel(this);
        
        gridButton = createToolbarButton("Grid", "/images/pixels.png");
        gridButton.addActionListener(e -> {
            boolean updatedState = !visualizationPanel.getToggleGrid();
            visualizationPanel.setToggleGrid(updatedState);
        });
        gridButton.setToolTipText("Toggle 3D View Grid Visibility");
        gridButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        toolBar.add(gridButton);
        
        
        // Setup listener for undo/redo state changes
        undoManager.addUndoableEditListener(e -> updateUndoRedoButtons());

        getContentPane().add(toolBar, BorderLayout.NORTH);
    }

    // refactor --

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

    // -- refactor

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

        // Update background when toggled
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

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Design");
        fileChooser.setSelectedFile(new File("design.furnish"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".furnish")) {
                file = new File(file.getParentFile(), file.getName() + ".furnish");
            }

            try (ObjectOutputStream oos = new ObjectOutputStream(
                    new FileOutputStream(file))) {
                oos.writeObject(currentDesign);
                updateStatus("Design saved to: " + file.getAbsolutePath());
            } catch (IOException ex) {
                showErrorDialog("Error saving design: " + ex.getMessage());
            }
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

        // Ask for orientation
        // Object[] options = {"North", "East", "South", "West"};
        // int orientationChoice = JOptionPane.showOptionDialog(this,
        //         "Select furniture orientation:",
        //         "Furniture Orientation",
        //         JOptionPane.DEFAULT_OPTION,
        //         JOptionPane.QUESTION_MESSAGE,
        //         null,
        //         options,
        //         options[0]);
        
        // if (orientationChoice == JOptionPane.CLOSED_OPTION) {
        //     return; // User cancelled
        // }
        
        // Furniture.Orientation orientation = Furniture.Orientation.values()[orientationChoice];
        
        Furniture.Orientation orientation = Furniture.Orientation.NORTH;


        Room room = currentDesign.getRoom();
        Color defaultColor;
        double width, depth, height;

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
        } else {
            defaultColor = type.equals("Table") ? new Color(150, 100, 50)
                    : type.equals("Sofa") ? new Color(120, 80, 180)
                    : type.equals("Bed") ? new Color(100, 150, 100)
                    : type.equals("Cabinet") ? new Color(139, 69, 19)
                    : Color.GRAY;
            width = type.equals("Table") ? 1.0 : type.equals("Sofa") ? 1.2 : type.equals("Bed") ? 1.5 : 0.8;
            depth = type.equals("Table") ? 0.8 : type.equals("Sofa") ? 0.6 : type.equals("Bed") ? 2.0 : 0.4;
            height = type.equals("Table") ? 0.7 : type.equals("Sofa") ? 0.6 : type.equals("Bed") ? 0.5 : 1.0;
            subtype = "";
        }

        // Calculate position based on orientation
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
            undoManager.addFurnitureEdit(currentDesign, toDelete, false); // Record removal
            setSelectedFurniture(null);
            updateStatus("Removed " + toDelete.getType());
            repaint();
        }

        // String type = selectedFurniture.getType();
        // currentDesign.getFurnitureList().remove(selectedFurniture);
        // selectedFurniture = null;
        // propertiesPanel.update((Graphics) null);
        // updateStatus("Deleted " + type + " from the design");
        // repaint();
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
