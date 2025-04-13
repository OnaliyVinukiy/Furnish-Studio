package org.furnish.models;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

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

    private JButton pickWidthButton, pickHeightButton;


    private boolean enableColorPick = false;
    private Color selectedWallColor = Color.BLACK;

    public WallDrawingPanel() {
        setLayout(null);
        setBackground(Color.WHITE);

        // Toggle 3D
        toggle3DButton = new JButton("3D Mode");
        toggle3DButton.setBounds(10, 10, 100, 30);
        toggle3DButton.addActionListener(e -> toggle3DMode());
        add(toggle3DButton);

        // Undo
        undoButton = new JButton(new ImageIcon(getClass().getResource("/images/undo.png")));
        undoButton.setBounds(120, 10, 40, 30);
        undoButton.setToolTipText("Undo");
        undoButton.addActionListener(e -> {
            if (!walls.isEmpty()) {
                Wall removed = walls.remove(walls.size() - 1);
                undoStack.push(removed);
                redoStack.clear();
                repaint();
            }
        });
        add(undoButton);

        // Redo
        redoButton = new JButton(new ImageIcon(getClass().getResource("/images/forward.png")));
        redoButton.setBounds(170, 10, 40, 30);
        redoButton.setToolTipText("Redo");
        redoButton.addActionListener(e -> {
            if (!undoStack.isEmpty()) {
                Wall restored = undoStack.pop();
                walls.add(restored);
                repaint();
            }
        });
        add(redoButton);

        // Reset
        resetButton = new JButton(new ImageIcon(getClass().getResource("/images/reset.png")));
        resetButton.setBounds(220, 10, 40, 30);
        resetButton.setToolTipText("Reset");
        resetButton.addActionListener(e -> {
            walls.clear();
            undoStack.clear();
            redoStack.clear();
            selectedWall = null;
            repaint();
        });
        add(resetButton);

        // Pick Color
        pickColorButton = new JButton("Pick Color");
        pickColorButton.setBounds(10, 50, 120, 30);
        pickColorButton.addActionListener(e -> {
            Color picked = JColorChooser.showDialog(this, "Choose Wall Color", selectedWallColor);
            if (picked != null) {
                selectedWallColor = picked;
                enableColorPick = true;
            }
        });
        add(pickColorButton);

        // Pick Width
        pickWidthButton = new JButton("Set Width");
        pickWidthButton.setBounds(140, 90, 120, 30);
        pickWidthButton.addActionListener(e -> {
            String input = JOptionPane.showInputDialog(this, "Enter Wall Width (px):", wallWidth);
            if (input != null && !input.isEmpty()) {
                try {
                    wallWidth = Integer.parseInt(input);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid width!");
                }
            }
        });
        add(pickWidthButton);

        // Pick Height
        pickHeightButton = new JButton("Set Height");
        pickHeightButton.setBounds(270, 90, 120, 30);
        pickHeightButton.addActionListener(e -> {
            String input = JOptionPane.showInputDialog(this, "Enter Wall Height (px):", wallHeight);
            if (input != null && !input.isEmpty()) {
                try {
                    wallHeight = Integer.parseInt(input);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid height!");
                }
            }
        });
        add(pickHeightButton);


        // Mouse Handling
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
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
                    int xMarkX = (selectedWall.p1.x + selectedWall.p2.x) / 2 - 10;
                    int xMarkY = (selectedWall.p1.y + selectedWall.p2.y) / 2 - 10;
                    if (clicked.distance(new Point(xMarkX, xMarkY)) < POINT_RADIUS) {
                        int result = JOptionPane.showConfirmDialog(null, "Do you want to delete this wall?", "Confirm", JOptionPane.YES_NO_OPTION);
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
                if (selectedPoint != null) {
                    selectedPoint.setLocation(snapToGrid(e.getPoint()));
                    selectedPoint = null;
                    repaint();
                    return;
                }

                Point endPoint = snapToGrid(e.getPoint());

                if (startPoint != null && !startPoint.equals(endPoint)) {
                    Color colorToUse = enableColorPick ? selectedWallColor : Color.BLACK;
                    walls.add(new Wall(startPoint.x, startPoint.y, endPoint.x, endPoint.y, colorToUse, wallWidth, wallHeight));
                    startPoint = null;
                    enableColorPick = false;
                    repaint();
                }
                
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (selectedPoint != null) {
                    selectedPoint.setLocation(snapToGrid(e.getPoint()));
                    repaint();
                }
            }
        });
    }

    private void toggle3DMode() {
        is3DMode = !is3DMode;
        repaint();
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

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (is3DMode) {
            render3DWalls(g);
        } else {
            render2DWalls(g);
        }
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
        g2d.setStroke(new BasicStroke(2)); // Set the stroke for border visibility
    
        for (Wall wall : walls) {
            // Set color for the selected wall or the wall's default color
            g2d.setColor(wall == selectedWall ? Color.GREEN : wall.color);
    
            int dx = wall.p2.x - wall.p1.x;
            int dy = wall.p2.y - wall.p1.y;
            double length = wall.p1.distance(wall.p2);
            double ux = dx / length;
            double uy = dy / length;
    
            // Normal vector for wall thickness (perpendicular to wall)
            int offsetX = (int) (-uy * wall.width);
            int offsetY = (int) (ux * wall.width);
    
            // Top vertices
            int x1 = wall.p1.x, y1 = wall.p1.y;
            int x2 = wall.p2.x, y2 = wall.p2.y;
            int x3 = x2 + offsetX, y3 = y2 + offsetY;
            int x4 = x1 + offsetX, y4 = y1 + offsetY;
    
            // Bottom vertices (height down)
            int z = wall.height;
            int bx1 = x1, by1 = y1 + z;
            int bx2 = x2, by2 = y2 + z;
            int bx3 = x3, by3 = y3 + z;
            int bx4 = x4, by4 = y4 + z;
    
            // Fill top face
            g2d.fillPolygon(new int[]{x1, x2, x3, x4}, new int[]{y1, y2, y3, y4}, 4);
    
            // Fill bottom face
            g2d.fillPolygon(new int[]{bx1, bx2, bx3, bx4}, new int[]{by1, by2, by3, by4}, 4);
    
            // Fill side face 1
            g2d.fillPolygon(new int[]{x1, x2, bx2, bx1}, new int[]{y1, y2, by2, by1}, 4);
    
            // Fill side face 2
            g2d.fillPolygon(new int[]{x2, x3, bx3, bx2}, new int[]{y2, y3, by3, by2}, 4);
    
            // Fill side face 3
            g2d.fillPolygon(new int[]{x3, x4, bx4, bx3}, new int[]{y3, y4, by4, by3}, 4);
    
            // Fill side face 4
            g2d.fillPolygon(new int[]{x4, x1, bx1, bx4}, new int[]{y4, y1, by1, by4}, 4);
    
            // Set border color for visibility
            g2d.setColor(Color.BLACK);
    
            // Draw the borders for each face
            g2d.drawPolygon(new int[]{x1, x2, x3, x4}, new int[]{y1, y2, y3, y4}, 4);
            g2d.drawPolygon(new int[]{bx1, bx2, bx3, bx4}, new int[]{by1, by2, by3, by4}, 4);
            g2d.drawPolygon(new int[]{x1, x2, bx2, bx1}, new int[]{y1, y2, by2, by1}, 4);
            g2d.drawPolygon(new int[]{x2, x3, bx3, bx2}, new int[]{y2, y3, by3, by2}, 4);
            g2d.drawPolygon(new int[]{x3, x4, bx4, bx3}, new int[]{y3, y4, by4, by3}, 4);
            g2d.drawPolygon(new int[]{x4, x1, bx1, bx4}, new int[]{y4, y1, by1, by4}, 4);
        }
    
        // Highlight selected wall
        if (selectedWall != null) {
            int x = (selectedWall.p1.x + selectedWall.p2.x) / 2 - 10;
            int y = (selectedWall.p1.y + selectedWall.p2.y) / 2 - 10;
            g2d.setColor(Color.RED);
            g2d.drawLine(x, y, x + 20, y + 20);
            g2d.drawLine(x + 20, y, x, y + 20);
        }
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
    
}
