package org.furnish.models;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;


public class WallDrawingPanel extends JPanel {
    private List<Wall> walls = new ArrayList<>();
    private Stack<Wall> undoStack = new Stack<>();
    private Stack<Wall> redoStack = new Stack<>();

    private Point startPoint = null;
    private Point selectedPoint = null;
    private Wall selectedWall = null;

    private static final int SNAP_SIZE = 10;
    private static final int POINT_RADIUS = 6;
    private boolean is3DMode = false;

    private JButton toggle3DButton, pickColorButton;
    private JButton undoButton, redoButton, resetButton;

    private int wallWidth = 10;
    private int wallHeight = 100;

    private boolean enableColorPick = false;
    private Color selectedWallColor = Color.BLACK;

    // 3D View Controls
    private double viewAngleX = 0.3;
    private double viewAngleY = 0;
    private double zoom = 1.0;
    private Point lastMousePos;
    private boolean isRotating = false;
    private final double rotationSensitivity = 0.005;
    private final double zoomSensitivity = 0.1;

    // Scroll bars
    private JScrollBar horizontalScrollBar;
    private JScrollBar verticalScrollBar;

    public WallDrawingPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // Main drawing panel
        JPanel drawingPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (is3DMode) {
                    render3DWalls(g);
                } else {
                    render2DWalls(g);
                }
            }
        };
        drawingPanel.setBackground(Color.WHITE);
        add(drawingPanel, BorderLayout.CENTER);

        // Control panel at top
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        controlPanel.setBackground(new Color(240, 240, 240));
        controlPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        // Toggle 3D
        toggle3DButton = createStyledButton("3D Mode");
        toggle3DButton.addActionListener(e -> toggle3DMode());
        controlPanel.add(toggle3DButton);

        // Undo
        undoButton = createStyledButton("Undo");
        undoButton.addActionListener(e -> undo());
        controlPanel.add(undoButton);

        // Redo
        redoButton = createStyledButton("Redo");
        redoButton.addActionListener(e -> redo());
        controlPanel.add(redoButton);

        // Reset
        resetButton = createStyledButton("Reset");
        resetButton.addActionListener(e -> reset());
        controlPanel.add(resetButton);

        // Pick Color
        pickColorButton = createStyledButton("Pick Color");
        pickColorButton.addActionListener(e -> pickColor());
        controlPanel.add(pickColorButton);

        // Set Size Button
        JButton setSizeButton = createStyledButton("Set Size");
        setSizeButton.addActionListener(e -> setSize());
        controlPanel.add(setSizeButton);

        // View control buttons
        JPanel viewControlPanel = new JPanel();
        viewControlPanel.setLayout(new GridBagLayout());
        viewControlPanel.setBackground(new Color(240, 240, 240));
        GridBagConstraints gbc = new GridBagConstraints();

        // Rotate buttons
        JButton rotateUpButton = createRoundButton("↑");
        rotateUpButton.addActionListener(e -> { 
            viewAngleX += 0.1; 
            verticalScrollBar.setValue((int)((viewAngleX / 0.02) + 50));
            repaint(); 
        });
        gbc.gridx = 1;
        gbc.gridy = 0;
        viewControlPanel.add(rotateUpButton, gbc);

        JButton rotateDownButton = createRoundButton("↓");
        rotateDownButton.addActionListener(e -> { 
            viewAngleX -= 0.1;
            verticalScrollBar.setValue((int)((viewAngleX / 0.02) + 50));
            repaint(); 
        });
        gbc.gridx = 1;
        gbc.gridy = 2;
        viewControlPanel.add(rotateDownButton, gbc);

        JButton rotateLeftButton = createRoundButton("←");
        rotateLeftButton.addActionListener(e -> { 
            viewAngleY -= 0.1;
            horizontalScrollBar.setValue((int)((viewAngleY / 0.02) + 50));
            repaint(); 
        });
        gbc.gridx = 0;
        gbc.gridy = 1;
        viewControlPanel.add(rotateLeftButton, gbc);

        JButton rotateRightButton = createRoundButton("→");
        rotateRightButton.addActionListener(e -> { 
            viewAngleY += 0.1;
            horizontalScrollBar.setValue((int)((viewAngleY / 0.02) + 50));
            repaint(); 
        });
        gbc.gridx = 2;
        gbc.gridy = 1;
        viewControlPanel.add(rotateRightButton, gbc);

        // Zoom buttons
        JButton zoomInButton = createRoundButton("+");
        zoomInButton.addActionListener(e -> { zoom *= 1.1; repaint(); });
        gbc.gridx = 3;
        gbc.gridy = 0;
        viewControlPanel.add(zoomInButton, gbc);

        JButton zoomOutButton = createRoundButton("-");
        zoomOutButton.addActionListener(e -> { zoom /= 1.1; repaint(); });
        gbc.gridx = 3;
        gbc.gridy = 2;
        viewControlPanel.add(zoomOutButton, gbc);

        controlPanel.add(viewControlPanel);
        add(controlPanel, BorderLayout.NORTH);

        // Horizontal scroll bar at bottom
        horizontalScrollBar = new JScrollBar(JScrollBar.HORIZONTAL, 50, 10, 0, 100);
        horizontalScrollBar.addAdjustmentListener(e -> {
            if (!horizontalScrollBar.getValueIsAdjusting()) {
                viewAngleY = (horizontalScrollBar.getValue() - 50) * 0.02;
                repaint();
            }
        });
        horizontalScrollBar.setPreferredSize(new Dimension(0, 20));
        horizontalScrollBar.setBackground(new Color(220, 220, 220));
        add(horizontalScrollBar, BorderLayout.SOUTH);

        // Vertical scroll bar at right
        verticalScrollBar = new JScrollBar(JScrollBar.VERTICAL, 50, 10, 0, 100);
        verticalScrollBar.addAdjustmentListener(e -> {
            if (!verticalScrollBar.getValueIsAdjusting()) {
                viewAngleX = (verticalScrollBar.getValue() - 50) * 0.02;
                repaint();
            }
        });
        verticalScrollBar.setPreferredSize(new Dimension(20, 0));
        verticalScrollBar.setBackground(new Color(220, 220, 220));
        add(verticalScrollBar, BorderLayout.EAST);

        // Mouse listeners
        drawingPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (is3DMode && SwingUtilities.isMiddleMouseButton(e)) {
                    isRotating = true;
                    lastMousePos = e.getPoint();
                    return;
                }

                if (e.getButton() == MouseEvent.BUTTON3) {
                    selectedWall = null;
                    repaint();
                    return;
                }

                Point clicked = e.getPoint();
                Wall wall = is3DMode ? getNearbyWall3D(clicked) : getNearbyWall2D(clicked);

                if (wall != null && e.getClickCount() == 2) {
                    selectedWall = wall;
                    repaint();
                } else if (selectedWall != null) {
                    Point center = new Point(
                        (selectedWall.p1.x + selectedWall.p2.x) / 2,
                        (selectedWall.p1.y + selectedWall.p2.y) / 2
                    );
                    if (clicked.distance(center) < POINT_RADIUS) {
                        int result = JOptionPane.showConfirmDialog(null, 
                            "Do you want to delete this wall?", "Confirm", JOptionPane.YES_NO_OPTION);
                        if (result == JOptionPane.YES_OPTION) {
                            walls.remove(selectedWall);
                            selectedWall = null;
                            repaint();
                        }
                    }
                } else {
                    selectedPoint = getNearbyEndpoint(clicked);
                    if (selectedPoint == null) {
                        startPoint = snapToGrid(clicked);
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isMiddleMouseButton(e)) {
                    isRotating = false;
                    return;
                }

                if (selectedPoint != null) {
                    selectedPoint.setLocation(snapToGrid(e.getPoint()));
                    selectedPoint = null;
                    repaint();
                    return;
                }

                Point endPoint = snapToGrid(e.getPoint());

                if (startPoint != null && !startPoint.equals(endPoint)) {
                    Color colorToUse = enableColorPick ? selectedWallColor : Color.BLACK;
                    walls.add(new Wall(startPoint.x, startPoint.y, endPoint.x, endPoint.y, 
                                     colorToUse, wallWidth, wallHeight));
                    startPoint = null;
                    enableColorPick = false;
                    repaint();
                }
            }
        });

        drawingPanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (is3DMode && isRotating) {
                    Point currentPos = e.getPoint();
                    int dx = currentPos.x - lastMousePos.x;
                    int dy = currentPos.y - lastMousePos.y;
                    
                    viewAngleY += dx * rotationSensitivity;
                    viewAngleX += dy * rotationSensitivity;
                    
                    // Update scroll bars
                    horizontalScrollBar.setValue((int)((viewAngleY / 0.02) + 50));
                    verticalScrollBar.setValue((int)((viewAngleX / 0.02) + 50));
                    
                    lastMousePos = currentPos;
                    repaint();
                    return;
                }

                if (selectedPoint != null) {
                    selectedPoint.setLocation(snapToGrid(e.getPoint()));
                    repaint();
                }
            }
        });

        drawingPanel.addMouseWheelListener(e -> {
            if (is3DMode) {
                int notches = e.getWheelRotation();
                zoom *= (1 + (notches > 0 ? -zoomSensitivity : zoomSensitivity));
                repaint();
            }
        });

        // Keyboard controls
        drawingPanel.setFocusable(true);
        drawingPanel.requestFocusInWindow();
        drawingPanel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!is3DMode) return;
                
                double rotationAmount = 0.1;
                
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT:
                        viewAngleY -= rotationAmount;
                        horizontalScrollBar.setValue((int)((viewAngleY / 0.02) + 50));
                        break;
                    case KeyEvent.VK_RIGHT:
                        viewAngleY += rotationAmount;
                        horizontalScrollBar.setValue((int)((viewAngleY / 0.02) + 50));
                        break;
                    case KeyEvent.VK_UP:
                        viewAngleX += rotationAmount;
                        verticalScrollBar.setValue((int)((viewAngleX / 0.02) + 50));
                        break;
                    case KeyEvent.VK_DOWN:
                        viewAngleX -= rotationAmount;
                        verticalScrollBar.setValue((int)((viewAngleX / 0.02) + 50));
                        break;
                    case KeyEvent.VK_EQUALS:
                    case KeyEvent.VK_PLUS:
                        zoom *= 1.1;
                        break;
                    case KeyEvent.VK_MINUS:
                        zoom /= 1.1;
                        break;
                    default:
                        return;
                }
                repaint();
            }
        });
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(new Color(220, 220, 220));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(180, 180, 180)),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        return button;
    }

    private JButton createRoundButton(String text) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(30, 30));
        button.setBackground(new Color(220, 220, 220));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(new Color(180, 180, 180), 1, true));
        return button;
    }

    private void toggle3DMode() {
        is3DMode = !is3DMode;
        if (is3DMode) {
            viewAngleX = 0.3;
            viewAngleY = 0;
            zoom = 1.0;
            horizontalScrollBar.setValue(50);
            verticalScrollBar.setValue(50);
        }
        repaint();
    }

    private void undo() {
        if (!walls.isEmpty()) {
            Wall removed = walls.remove(walls.size() - 1);
            undoStack.push(removed);
            redoStack.clear();
            repaint();
        }
    }

    private void redo() {
        if (!undoStack.isEmpty()) {
            Wall restored = undoStack.pop();
            walls.add(restored);
            repaint();
        }
    }

    private void reset() {
        walls.clear();
        undoStack.clear();
        redoStack.clear();
        selectedWall = null;
        repaint();
    }

    private void pickColor() {
        Color picked = JColorChooser.showDialog(this, "Choose Wall Color", selectedWallColor);
        if (picked != null) {
            selectedWallColor = picked;
            enableColorPick = true;
        }
    }

    private void setSize() {
        String input = JOptionPane.showInputDialog(this, 
            "Enter Wall Width and Height (px) separated by a comma (e.g., 100,200):", 
            wallWidth + "," + wallHeight);
        if (input != null && !input.isEmpty()) {
            String[] dimensions = input.split(",");
            if (dimensions.length == 2) {
                try {
                    wallWidth = Integer.parseInt(dimensions[0].trim());
                    wallHeight = Integer.parseInt(dimensions[1].trim());
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid input! Please enter valid numbers.");
                }
            } else {
                JOptionPane.showMessageDialog(this, "Invalid input! Please enter two values separated by a comma.");
            }
        }
    }

    private Point snapToGrid(Point p) {
        int x = ((p.x + SNAP_SIZE / 2) / SNAP_SIZE) * SNAP_SIZE;
        int y = ((p.y + SNAP_SIZE / 2) / SNAP_SIZE) * SNAP_SIZE;
        return new Point(x, y);
    }

    private Point getNearbyEndpoint(Point p) {
        for (Wall wall : walls) {
            if (p.distance(wall.p1) < POINT_RADIUS) return wall.p1;
            if (p.distance(wall.p2) < POINT_RADIUS) return wall.p2;
        }
        return null;
    }

    private Wall getNearbyWall2D(Point p) {
        for (Wall wall : walls) {
            if (lineDistance(wall.p1, wall.p2, p) < POINT_RADIUS) return wall;
        }
        return null;
    }

    private Wall getNearbyWall3D(Point p) {
        return getNearbyWall2D(p);
    }

    private double lineDistance(Point p1, Point p2, Point p) {
        double A = p.y - p1.y;
        double B = p1.x - p.x;
        double C = p1.x * p.y - p.x * p1.y;
        return Math.abs(A * p2.x + B * p2.y + C) / Math.sqrt(A * A + B * B);
    }

    private void render2DWalls(Graphics g) {
        drawGrid(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setStroke(new BasicStroke(2));

        for (Wall wall : walls) {
            g2d.setColor(wall == selectedWall ? Color.GREEN : wall.color);
            g2d.drawLine(wall.p1.x, wall.p1.y, wall.p2.x, wall.p2.y);
            g2d.setColor(Color.RED);
            g2d.fillOval(wall.p1.x - POINT_RADIUS / 2, wall.p1.y - POINT_RADIUS / 2, POINT_RADIUS, POINT_RADIUS);
            g2d.fillOval(wall.p2.x - POINT_RADIUS / 2, wall.p2.y - POINT_RADIUS / 2, POINT_RADIUS, POINT_RADIUS);
            g2d.setColor(Color.BLUE);
            int midX = (wall.p1.x + wall.p2.x) / 2;
            int midY = (wall.p1.y + wall.p2.y) / 2;
            g2d.drawString(String.format("%.1f px", wall.p1.distance(wall.p2)), midX + 5, midY - 5);
        }
    }

    private void render3DWalls(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        
        // Clear the background
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, getWidth(), getHeight());
        
        // Center of the view
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        
        // Draw each wall in 3D
        for (Wall wall : walls) {
            // Calculate wall direction vectors
            double dx = wall.p2.x - wall.p1.x;
            double dy = wall.p2.y - wall.p1.y;
            double length = Math.sqrt(dx * dx + dy * dy);
            double ux = dx / length;
            double uy = dy / length;
            
            // Normal vector for wall thickness (perpendicular to wall)
            double offsetX = -uy * wall.width;
            double offsetY = ux * wall.width;
            
            // Create the 8 corners of the 3D wall
            Point3D[] corners = new Point3D[8];
            corners[0] = new Point3D(wall.p1.x, wall.p1.y, 0);
            corners[1] = new Point3D(wall.p2.x, wall.p2.y, 0);
            corners[2] = new Point3D(wall.p2.x + offsetX, wall.p2.y + offsetY, 0);
            corners[3] = new Point3D(wall.p1.x + offsetX, wall.p1.y + offsetY, 0);
            
            // Top corners (height)
            for (int i = 0; i < 4; i++) {
                corners[i + 4] = new Point3D(corners[i].x, corners[i].y, wall.height);
            }
            
            // Transform points for viewing
            Point[] screenPoints = new Point[8];
            for (int i = 0; i < 8; i++) {
                // Apply rotation
                Point3D p = rotatePoint(corners[i], viewAngleX, viewAngleY);
                
                // Apply zoom and center on screen
                int screenX = centerX + (int)(p.x * zoom);
                int screenY = centerY - (int)(p.z * zoom);
                
                screenPoints[i] = new Point(screenX, screenY);
            }
            
            // Draw the wall faces with different shades
            Color baseColor = wall == selectedWall ? Color.GREEN : wall.color;
            draw3DFace(g2d, screenPoints, new int[]{0, 1, 2, 3}, baseColor); // Top
            draw3DFace(g2d, screenPoints, new int[]{4, 5, 6, 7}, baseColor.darker()); // Bottom
            draw3DFace(g2d, screenPoints, new int[]{0, 1, 5, 4}, baseColor.brighter()); // Front
            draw3DFace(g2d, screenPoints, new int[]{1, 2, 6, 5}, baseColor.brighter()); // Right
            draw3DFace(g2d, screenPoints, new int[]{2, 3, 7, 6}, baseColor.brighter()); // Back
            draw3DFace(g2d, screenPoints, new int[]{3, 0, 4, 7}, baseColor.brighter()); // Left
        }
    }

    private Point3D rotatePoint(Point3D p, double angleX, double angleY) {
        // Rotate around Y axis (left/right)
        double cosY = Math.cos(angleY);
        double sinY = Math.sin(angleY);
        double x1 = p.x * cosY - p.z * sinY;
        double z1 = p.x * sinY + p.z * cosY;
        
        // Rotate around X axis (up/down)
        double cosX = Math.cos(angleX);
        double sinX = Math.sin(angleX);
        double y1 = p.y * cosX - z1 * sinX;
        double z2 = p.y * sinX + z1 * cosX;
        
        return new Point3D(x1, y1, z2);
    }

    private void draw3DFace(Graphics2D g2d, Point[] points, int[] indices, Color color) {
        Path2D path = new Path2D.Double();
        path.moveTo(points[indices[0]].x, points[indices[0]].y);
        for (int i = 1; i < indices.length; i++) {
            path.lineTo(points[indices[i]].x, points[indices[i]].y);
        }
        path.closePath();
        
        g2d.setColor(color);
        g2d.fill(path);
        g2d.setColor(Color.BLACK);
        g2d.draw(path);
    }

    private void drawGrid(Graphics g) {
        g.setColor(new Color(220, 220, 220));
        for (int x = 0; x < getWidth(); x += SNAP_SIZE)
            g.drawLine(x, 0, x, getHeight());
        for (int y = 0; y < getHeight(); y += SNAP_SIZE)
            g.drawLine(0, y, getWidth(), y);
    }

    public List<Wall> getWalls() {
        return walls;
    }

    private static class Wall {
        Point p1, p2;
        Color color;
        int width, height;
    
        Wall(int x1, int y1, int x2, int y2, Color color, int width, int height) {
            this.p1 = new Point(x1, y1);
            this.p2 = new Point(x2, y2);
            this.color = color;
            this.width = width;
            this.height = height;
        }
    }

    private static class Point3D {
        double x, y, z;
        
        public Point3D(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}