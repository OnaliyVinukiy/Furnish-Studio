package org.Ghapurachchi;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

// Main application frame
class FurnitureDesignApp extends JFrame {
    private VisualizationPanel visualizationPanel;
    PropertiesPanel propertiesPanel;
    private Design currentDesign;
    private Furniture selectedFurniture;

    public FurnitureDesignApp() {
        setTitle("Furniture Design App");
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Menu bar
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem newItem = new JMenuItem("New");
        JMenuItem saveItem = new JMenuItem("Save");
        JMenuItem openItem = new JMenuItem("Open");
        fileMenu.add(newItem);
        fileMenu.add(saveItem);
        fileMenu.add(openItem);

        JMenu viewMenu = new JMenu("View");
        JMenuItem view2DItem = new JMenuItem("2D View");
        JMenuItem view3DItem = new JMenuItem("3D View");
        viewMenu.add(view2DItem);
        viewMenu.add(view3DItem);

        JMenu editMenu = new JMenu("Edit");
        JMenuItem addChairItem = new JMenuItem("Add Chair");
        JMenuItem addTableItem = new JMenuItem("Add Table");
        JMenuItem deleteItem = new JMenuItem("Delete Selected");
        editMenu.add(addChairItem);
        editMenu.add(addTableItem);
        editMenu.add(deleteItem);

        menuBar.add(fileMenu);
        menuBar.add(viewMenu);
        menuBar.add(editMenu);
        setJMenuBar(menuBar);

        // Visualization panel
        visualizationPanel = new VisualizationPanel();
        add(visualizationPanel, BorderLayout.CENTER);

        // Properties panel
        propertiesPanel = new PropertiesPanel();
        add(propertiesPanel, BorderLayout.EAST);

        // Menu actions
        newItem.addActionListener(e -> newDesign());
        saveItem.addActionListener(e -> saveDesign());
        openItem.addActionListener(e -> openDesign());
        view2DItem.addActionListener(e -> {
            visualizationPanel.set3DView(false);
            repaint();
        });
        view3DItem.addActionListener(e -> {
            visualizationPanel.set3DView(true);
            repaint();
        });
        addChairItem.addActionListener(e -> addFurniture("Chair"));
        addTableItem.addActionListener(e -> addFurniture("Table"));
        deleteItem.addActionListener(e -> deleteSelectedFurniture());

        // Set the parent for the visualization panel to access setSelectedFurniture
        visualizationPanel.setParent(this);
    }

    private void newDesign() {
        RoomDialog dialog = new RoomDialog(this);
        dialog.setVisible(true);
        if (dialog.isOk()) {
            currentDesign = new Design(dialog.getRoom());
            visualizationPanel.setDesign(currentDesign);
            repaint();
        }
    }

    private void saveDesign() {
        if (currentDesign == null) return;
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fileChooser.getSelectedFile()))) {
                oos.writeObject(currentDesign);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error saving design: " + ex.getMessage());
            }
        }
    }

    private void openDesign() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileChooser.getSelectedFile()))) {
                currentDesign = (Design) ois.readObject();
                visualizationPanel.setDesign(currentDesign);
                repaint();
            } catch (IOException | ClassNotFoundException ex) {
                JOptionPane.showMessageDialog(this, "Error loading design: " + ex.getMessage());
            }
        }
    }

    private void addFurniture(String type) {
        if (currentDesign == null) return;
        Furniture f = new Furniture(type, 1.0, 0, 1.0, type.equals("Chair") ? 1.0 : 2.0, 1.0, Color.GRAY);
        currentDesign.addFurniture(f);
        repaint();
    }

    private void deleteSelectedFurniture() {
        if (currentDesign != null && selectedFurniture != null) {
            currentDesign.getFurnitureList().remove(selectedFurniture);
            selectedFurniture = null;
            propertiesPanel.update((Graphics)null);
            repaint();
        }
    }

    public void setSelectedFurniture(Furniture f) {
        selectedFurniture = f;
        propertiesPanel.update(f);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FurnitureDesignApp().setVisible(true));
    }
}

// Room class
class Room implements Serializable {
    private double length, width, height;
    private Color floorColor, wallColor;

    public Room(double length, double width, double height, Color floorColor, Color wallColor) {
        this.length = length;
        this.width = width;
        this.height = height;
        this.floorColor = floorColor;
        this.wallColor = wallColor;
    }

    public double getLength() { return length; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public Color getFloorColor() { return floorColor; }
    public Color getWallColor() { return wallColor; }
}

// Furniture class
class Furniture implements Serializable {
    private String type;
    private double xPosition, zPosition;
    private double width, depth, height;
    private Color color;
    private float shadeFactor = 1.0f;

    public Furniture(String type, double xPosition, double zPosition, double width, double depth, double height, Color color) {
        this.type = type;
        this.xPosition = xPosition;
        this.zPosition = zPosition;
        this.width = width;
        this.depth = depth;
        this.height = height;
        this.color = color;
    }

    public String getType() { return type; }
    public double getX() { return xPosition; }
    public double getZ() { return zPosition; }
    public double getWidth() { return width; }
    public double getDepth() { return depth; }
    public double getHeight() { return height; }
    public Color getColor() { return new Color((int)(color.getRed() * shadeFactor), (int)(color.getGreen() * shadeFactor), (int)(color.getBlue() * shadeFactor)); }
    public float getShadeFactor() { return shadeFactor; }

    public void setX(double x) { this.xPosition = x; }
    public void setZ(double z) { this.zPosition = z; }
    public void setWidth(double width) { this.width = width; }
    public void setDepth(double depth) { this.depth = depth; }
    public void setHeight(double height) { this.height = height; }
    public void setColor(Color color) { this.color = color; }
    public void setShadeFactor(float factor) { this.shadeFactor = Math.max(0.1f, Math.min(1.0f, factor)); }
}

// Design class
class Design implements Serializable {
    private Room room;
    private List<Furniture> furnitureList;

    public Design(Room room) {
        this.room = room;
        this.furnitureList = new ArrayList<>();
    }

    public Room getRoom() { return room; }
    public List<Furniture> getFurnitureList() { return furnitureList; }
    public void addFurniture(Furniture f) { furnitureList.add(f); }
}

// Visualization panel
class VisualizationPanel extends JPanel {
    private Design design;
    private boolean is3DView = false;
    private FurnitureDesignApp parent;
    private Furniture draggedFurniture;
    private Point dragStartPoint;
    private double rotationX = 0;
    private double rotationY = 0;
    private double lastMouseX, lastMouseY;

    public VisualizationPanel() {
        setPreferredSize(new Dimension(1000, 900));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragStartPoint = e.getPoint();
                lastMouseX = e.getX();
                lastMouseY = e.getY();
                if (design != null && is3DView) {
                    double scale = Math.min(getWidth() / (design.getRoom().getLength() + design.getRoom().getWidth()), getHeight() / (design.getRoom().getHeight() + design.getRoom().getWidth())) * 0.5;
                    int offsetX = getWidth() / 2;
                    int offsetY = getHeight() / 2;

                    for (Furniture f : design.getFurnitureList()) {
                        Point center = project(f.getX() + f.getWidth() / 2, f.getHeight() / 2, f.getZ() + f.getDepth() / 2, scale, offsetX, offsetY);
                        double distance = Math.sqrt(Math.pow(e.getX() - center.x, 2) + Math.pow(e.getY() - center.y, 2));
                        if (distance < 20) {
                            draggedFurniture = f;
                            parent.setSelectedFurniture(f);
                            break;
                        }
                    }
                } else if (design != null && !is3DView) {
                    double scale = Math.min(getWidth() / design.getRoom().getLength(), getHeight() / design.getRoom().getWidth());
                    for (Furniture f : design.getFurnitureList()) {
                        int x = (int)(f.getX() * scale);
                        int z = (int)(f.getZ() * scale);
                        int w = (int)(f.getWidth() * scale);
                        int d = (int)(f.getDepth() * scale);
                        if (e.getX() >= x && e.getX() <= x + w && e.getY() >= z && e.getY() <= z + d) {
                            draggedFurniture = f;
                            dragStartPoint = new Point(e.getX() - x, e.getY() - z);
                            parent.setSelectedFurniture(f);
                            break;
                        }
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                draggedFurniture = null;
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (is3DView && draggedFurniture == null) {
                    double deltaX = e.getX() - lastMouseX;
                    double deltaY = e.getY() - lastMouseY;
                    rotationY += deltaX * 0.01;
                    rotationX += deltaY * 0.01;
                    repaint();
                } else if (draggedFurniture != null) {
                    if (!is3DView) {
                        double scale = Math.min(getWidth() / design.getRoom().getLength(), getHeight() / design.getRoom().getWidth());
                        double newX = (e.getX() - dragStartPoint.x) / scale;
                        double newZ = (e.getY() - dragStartPoint.y) / scale;
                        draggedFurniture.setX(Math.max(0, Math.min(newX, design.getRoom().getLength() - draggedFurniture.getWidth())));
                        draggedFurniture.setZ(Math.max(0, Math.min(newZ, design.getRoom().getWidth() - draggedFurniture.getDepth())));
                        parent.propertiesPanel.update(draggedFurniture);
                        repaint();
                    }
                }
                lastMouseX = e.getX();
                lastMouseY = e.getY();
            }
        });
    }

    public void setDesign(Design design) {
        this.design = design;
        rotationX = 0;
        rotationY = 0;
        repaint();
    }

    public void setParent(FurnitureDesignApp parent) {
        this.parent = parent;
    }

    public void set3DView(boolean is3DView) {
        this.is3DView = is3DView;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        if (design != null) {
            if (is3DView) {
                draw3D(g2d);
            } else {
                draw2D(g2d);
            }
        }
    }

    private void draw2D(Graphics2D g2d) {
        Room room = design.getRoom();
        double scale = Math.min(getWidth() / room.getLength(), getHeight() / room.getWidth());

        // Draw floor
        g2d.setColor(room.getFloorColor());
        g2d.fillRect(0, 0, (int)(room.getLength() * scale), (int)(room.getWidth() * scale));

        // Draw furniture
        for (Furniture f : design.getFurnitureList()) {
            g2d.setColor(f.getColor());
            int x = (int)(f.getX() * scale);
            int z = (int)(f.getZ() * scale);
            int w = (int)(f.getWidth() * scale);
            int d = (int)(f.getDepth() * scale);
            g2d.fillRect(x, z, w, d);
            g2d.setColor(Color.BLACK);
            g2d.drawRect(x, z, w, d);
        }
    }

    private void draw3D(Graphics2D g2d) {
        Room room = design.getRoom();
        double scale = Math.min(getWidth() / (room.getLength() + room.getWidth()), getHeight() / (room.getHeight() + room.getWidth())) * 0.5;
        int offsetX = getWidth() / 2;
        int offsetY = getHeight() / 2;

        g2d.translate(offsetX, offsetY);
        g2d.rotate(rotationY); // Y-axis rotation
        g2d.rotate(rotationX); // X-axis rotation
        g2d.translate(-offsetX, -offsetY);

        // Draw floor
        Point[] floorPoints = {
                project(0, 0, 0, scale, offsetX, offsetY),
                project(room.getLength(), 0, 0, scale, offsetX, offsetY),
                project(room.getLength(), 0, room.getWidth(), scale, offsetX, offsetY),
                project(0, 0, room.getWidth(), scale, offsetX, offsetY)
        };
        g2d.setColor(room.getFloorColor());
        g2d.fillPolygon(new int[]{floorPoints[0].x, floorPoints[1].x, floorPoints[2].x, floorPoints[3].x},
                new int[]{floorPoints[0].y, floorPoints[1].y, floorPoints[2].y, floorPoints[3].y}, 4);

        // Draw walls
        g2d.setColor(room.getWallColor());
        Point[] wall1Points = {
                project(0, 0, 0, scale, offsetX, offsetY),
                project(room.getLength(), 0, 0, scale, offsetX, offsetY),
                project(room.getLength(), room.getHeight(), 0, scale, offsetX, offsetY),
                project(0, room.getHeight(), 0, scale, offsetX, offsetY)
        };
        g2d.fillPolygon(new int[]{wall1Points[0].x, wall1Points[1].x, wall1Points[2].x, wall1Points[3].x},
                new int[]{wall1Points[0].y, wall1Points[1].y, wall1Points[2].y, wall1Points[3].y}, 4);

        Point[] wall2Points = {
                project(0, 0, 0, scale, offsetX, offsetY),
                project(0, 0, room.getWidth(), scale, offsetX, offsetY),
                project(0, room.getHeight(), room.getWidth(), scale, offsetX, offsetY),
                project(0, room.getHeight(), 0, scale, offsetX, offsetY)
        };
        g2d.fillPolygon(new int[]{wall2Points[0].x, wall2Points[1].x, wall2Points[2].x, wall2Points[3].x},
                new int[]{wall2Points[0].y, wall2Points[1].y, wall2Points[2].y, wall2Points[3].y}, 4);

        // Draw furniture
        for (Furniture f : design.getFurnitureList()) {
            if (f.getType().equals("Chair")) {
                drawChair3D(g2d, f.getX(), 0, f.getZ(), f.getWidth(), f.getDepth(), f.getHeight(), scale, offsetX, offsetY, f.getColor());
            } else if (f.getType().equals("Table")) {
                drawTable3D(g2d, f.getX(), 0, f.getZ(), f.getWidth(), f.getDepth(), f.getHeight(), scale, offsetX, offsetY, f.getColor());
            }
        }
    }

    private Point project(double x, double y, double z, double scale, int offsetX, int offsetY) {
        double isoX = (x - z) * Math.cos(Math.toRadians(30)) * scale;
        double isoY = ((x + z) * Math.sin(Math.toRadians(30)) - y) * scale;
        return new Point((int)isoX + offsetX, (int)isoY + offsetY);
    }

    private void drawChair3D(Graphics2D g2d, double x, double y, double z, double w, double d, double h,
                             double scale, int offsetX, int offsetY, Color color) {
        Color darkColor = color.darker();
        drawBox3D(g2d, x, y + h * 0.6, z, w, d, h * 0.1, scale, offsetX, offsetY, color); // Seat
        drawBox3D(g2d, x + w * 0.1, y + h * 0.7, z - d * 0.2, w * 0.8, h * 0.05, h * 0.4, scale, offsetX, offsetY, darkColor); // Back
        double legWidth = w * 0.1;
        double legDepth = d * 0.1;
        drawBox3D(g2d, x + w * 0.1, y, z + d * 0.1, legWidth, legDepth, h * 0.6, scale, offsetX, offsetY, darkColor); // Front legs
        drawBox3D(g2d, x + w * 0.8, y, z + d * 0.1, legWidth, legDepth, h * 0.6, scale, offsetX, offsetY, darkColor);
        drawBox3D(g2d, x + w * 0.1, y, z + d * 0.8, legWidth, legDepth, h, scale, offsetX, offsetY, darkColor); // Back legs
        drawBox3D(g2d, x + w * 0.8, y, z + d * 0.8, legWidth, legDepth, h, scale, offsetX, offsetY, darkColor);
    }

    private void drawTable3D(Graphics2D g2d, double x, double y, double z, double w, double d, double h,
                             double scale, int offsetX, int offsetY, Color color) {
        Color darkColor = color.darker();
        drawBox3D(g2d, x, y + h * 0.8, z, w, d, h * 0.05, scale, offsetX, offsetY, color); // Top
        double legWidth = w * 0.1;
        double legDepth = d * 0.1;
        drawBox3D(g2d, x + w * 0.1, y, z + d * 0.1, legWidth, legDepth, h * 0.8, scale, offsetX, offsetY, darkColor); // Legs
        drawBox3D(g2d, x + w * 0.8, y, z + d * 0.1, legWidth, legDepth, h * 0.8, scale, offsetX, offsetY, darkColor);
        drawBox3D(g2d, x + w * 0.1, y, z + d * 0.8, legWidth, legDepth, h * 0.8, scale, offsetX, offsetY, darkColor);
        drawBox3D(g2d, x + w * 0.8, y, z + d * 0.8, legWidth, legDepth, h * 0.8, scale, offsetX, offsetY, darkColor);
    }

    private void drawBox3D(Graphics2D g2d, double x, double y, double z, double w, double d, double h,
                           double scale, int offsetX, int offsetY, Color color) {
        Point[] points = new Point[8];
        points[0] = project(x, y, z, scale, offsetX, offsetY);
        points[1] = project(x + w, y, z, scale, offsetX, offsetY);
        points[2] = project(x + w, y, z + d, scale, offsetX, offsetY);
        points[3] = project(x, y, z + d, scale, offsetX, offsetY);
        points[4] = project(x, y + h, z, scale, offsetX, offsetY);
        points[5] = project(x + w, y + h, z, scale, offsetX, offsetY);
        points[6] = project(x + w, y + h, z + d, scale, offsetX, offsetY);
        points[7] = project(x, y + h, z + d, scale, offsetX, offsetY);

        Color topColor = color.brighter();
        Color sideColor1 = color;
        Color sideColor2 = color.darker();

        g2d.setColor(sideColor1);
        g2d.fillPolygon(new int[]{points[0].x, points[1].x, points[5].x, points[4].x},
                new int[]{points[0].y, points[1].y, points[5].y, points[4].y}, 4);
        g2d.setColor(Color.BLACK);
        g2d.drawPolygon(new int[]{points[0].x, points[1].x, points[5].x, points[4].x},
                new int[]{points[0].y, points[1].y, points[5].y, points[4].y}, 4);

        g2d.setColor(topColor);
        g2d.fillPolygon(new int[]{points[4].x, points[5].x, points[6].x, points[7].x},
                new int[]{points[4].y, points[5].y, points[6].y, points[7].y}, 4);
        g2d.setColor(Color.BLACK);
        g2d.drawPolygon(new int[]{points[4].x, points[5].x, points[6].x, points[7].x},
                new int[]{points[4].y, points[5].y, points[6].y, points[7].y}, 4);

        g2d.setColor(sideColor2);
        g2d.fillPolygon(new int[]{points[1].x, points[2].x, points[6].x, points[5].x},
                new int[]{points[1].y, points[2].y, points[6].y, points[5].y}, 4);
        g2d.setColor(Color.BLACK);
        g2d.drawPolygon(new int[]{points[1].x, points[2].x, points[6].x, points[5].x},
                new int[]{points[1].y, points[2].y, points[6].y, points[5].y}, 4);
    }
}

// Properties panel
class PropertiesPanel extends JPanel {
    private JTextField xField, zField, widthField, depthField, heightField;
    private JButton colorButton;
    private JSlider shadeSlider;
    private Furniture furniture;

    public PropertiesPanel() {
        setPreferredSize(new Dimension(300, 600));
        setLayout(new GridLayout(0, 2));
        setBorder(BorderFactory.createTitledBorder("Properties"));

        add(new JLabel("X:"));
        xField = new JTextField(5);
        xField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                updateProperty("x");
            }
        });
        add(xField);

        add(new JLabel("Z:"));
        zField = new JTextField(5);
        zField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                updateProperty("z");
            }
        });
        add(zField);

        add(new JLabel("Width:"));
        widthField = new JTextField(5);
        widthField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                updateProperty("width");
            }
        });
        add(widthField);

        add(new JLabel("Depth:"));
        depthField = new JTextField(5);
        depthField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                updateProperty("depth");
            }
        });
        add(depthField);

        add(new JLabel("Height:"));
        heightField = new JTextField(5);
        heightField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                updateProperty("height");
            }
        });
        add(heightField);

        add(new JLabel("Color:"));
        colorButton = new JButton("Choose");
        colorButton.addActionListener(e -> {
            if (furniture != null) {
                Color newColor = JColorChooser.showDialog(this, "Choose Color", furniture.getColor());
                if (newColor != null) {
                    furniture.setColor(newColor);
                    repaintParent();
                }
            }
        });
        add(colorButton);

        add(new JLabel("Shade:"));
        shadeSlider = new JSlider(0, 100, 100);
        shadeSlider.addChangeListener(e -> {
            if (furniture != null) {
                furniture.setShadeFactor(shadeSlider.getValue() / 100f);
                repaintParent();
            }
        });
        add(shadeSlider);
    }

    public void update(Furniture f) {
        this.furniture = f;
        if (f == null) {
            xField.setText("");
            zField.setText("");
            widthField.setText("");
            depthField.setText("");
            heightField.setText("");
            shadeSlider.setValue(100);
        } else {
            xField.setText(String.format("%.1f", f.getX()));
            zField.setText(String.format("%.1f", f.getZ()));
            widthField.setText(String.format("%.1f", f.getWidth()));
            depthField.setText(String.format("%.1f", f.getDepth()));
            heightField.setText(String.format("%.1f", f.getHeight()));
            shadeSlider.setValue((int)(f.getShadeFactor() * 100));
        }
    }

    private void updateProperty(String property) {
        if (furniture == null) return;
        try {
            switch (property) {
                case "x": furniture.setX(Double.parseDouble(xField.getText())); break;
                case "z": furniture.setZ(Double.parseDouble(zField.getText())); break;
                case "width": furniture.setWidth(Double.parseDouble(widthField.getText())); break;
                case "depth": furniture.setDepth(Double.parseDouble(depthField.getText())); break;
                case "height": furniture.setHeight(Double.parseDouble(heightField.getText())); break;
            }
            repaintParent();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid number format");
            update(furniture);
        }
    }

    private void repaintParent() {
        SwingUtilities.getWindowAncestor(this).repaint();
    }
}

// Room dialog
class RoomDialog extends JDialog {
    private JTextField lengthField, widthField, heightField;
    private JButton floorColorButton, wallColorButton;
    private Color floorColor = Color.LIGHT_GRAY, wallColor = Color.WHITE;
    private boolean ok = false;

    public RoomDialog(JFrame parent) {
        super(parent, "New Room", true);
        setLayout(new GridLayout(0, 2));
        setSize(300, 200);

        add(new JLabel("Length:"));
        lengthField = new JTextField("5.0", 5);
        add(lengthField);

        add(new JLabel("Width:"));
        widthField = new JTextField("5.0", 5);
        add(widthField);

        add(new JLabel("Height:"));
        heightField = new JTextField("3.0", 5);
        add(heightField);

        add(new JLabel("Floor Color:"));
        floorColorButton = new JButton("Choose");
        floorColorButton.addActionListener(e -> {
            Color c = JColorChooser.showDialog(this, "Choose Floor Color", floorColor);
            if (c != null) floorColor = c;
        });
        add(floorColorButton);

        add(new JLabel("Wall Color:"));
        wallColorButton = new JButton("Choose");
        wallColorButton.addActionListener(e -> {
            Color c = JColorChooser.showDialog(this, "Choose Wall Color", wallColor);
            if (c != null) wallColor = c;
        });
        add(wallColorButton);

        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            ok = true;
            setVisible(false);
        });
        add(okButton);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> setVisible(false));
        add(cancelButton);
    }

    public boolean isOk() { return ok; }

    public Room getRoom() {
        try {
            double length = Double.parseDouble(lengthField.getText());
            double width = Double.parseDouble(widthField.getText());
            double height = Double.parseDouble(heightField.getText());
            return new Room(length, width, height, floorColor, wallColor);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid room dimensions");
        }
    }
}