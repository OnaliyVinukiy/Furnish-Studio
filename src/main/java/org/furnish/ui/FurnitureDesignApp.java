package org.furnish.ui;

import org.furnish.core.*;
import javax.swing.*;
import java.awt.*;
import java.io.*;

public class FurnitureDesignApp extends JFrame {
    private VisualizationPanel visualizationPanel;
    PropertiesPanel propertiesPanel;
    private Design currentDesign;
    private Furniture selectedFurniture;

    public FurnitureDesignApp() {
        initializeUI();
        setupMenuBar();
        setupPanels();
    }

    private void initializeUI() {
        setTitle("Furniture Design App");
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
    }

    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // File Menu
        JMenu fileMenu = new JMenu("File");
        JMenuItem newItem = new JMenuItem("New");
        JMenuItem saveItem = new JMenuItem("Save");
        JMenuItem openItem = new JMenuItem("Open");

        newItem.addActionListener(e -> newDesign());
        saveItem.addActionListener(e -> saveDesign());
        openItem.addActionListener(e -> openDesign());

        fileMenu.add(newItem);
        fileMenu.add(saveItem);
        fileMenu.add(openItem);

        // View Menu
        JMenu viewMenu = new JMenu("View");
        JMenuItem view2DItem = new JMenuItem("2D View");
        JMenuItem view3DItem = new JMenuItem("3D View");

        view2DItem.addActionListener(e -> {
            visualizationPanel.set3DView(false);
            repaint();
        });
        view3DItem.addActionListener(e -> {
            visualizationPanel.set3DView(true);
            repaint();
        });

        viewMenu.add(view2DItem);
        viewMenu.add(view3DItem);

        // Edit Menu
        JMenu editMenu = new JMenu("Edit");
        JMenuItem addChairItem = new JMenuItem("Add Chair");
        JMenuItem addTableItem = new JMenuItem("Add Table");
        JMenuItem deleteItem = new JMenuItem("Delete Selected");

        addChairItem.addActionListener(e -> addFurniture("Chair"));
        addTableItem.addActionListener(e -> addFurniture("Table"));
        deleteItem.addActionListener(e -> deleteSelectedFurniture());

        editMenu.add(addChairItem);
        editMenu.add(addTableItem);
        editMenu.add(deleteItem);

        menuBar.add(fileMenu);
        menuBar.add(viewMenu);
        menuBar.add(editMenu);
        setJMenuBar(menuBar);
    }

    private void setupPanels() {
        visualizationPanel = new VisualizationPanel(this);
        add(visualizationPanel, BorderLayout.CENTER);

        propertiesPanel = new PropertiesPanel();
        add(propertiesPanel, BorderLayout.EAST);
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
        if (currentDesign == null)
            return;
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (ObjectOutputStream oos = new ObjectOutputStream(
                    new FileOutputStream(fileChooser.getSelectedFile()))) {
                oos.writeObject(currentDesign);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error saving design: " + ex.getMessage());
            }
        }
    }

    private void openDesign() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (ObjectInputStream ois = new ObjectInputStream(
                    new FileInputStream(fileChooser.getSelectedFile()))) {
                currentDesign = (Design) ois.readObject();
                visualizationPanel.setDesign(currentDesign);
                repaint();
            } catch (IOException | ClassNotFoundException ex) {
                JOptionPane.showMessageDialog(this, "Error loading design: " + ex.getMessage());
            }
        }
    }

    private void addFurniture(String type) {
        if (currentDesign == null)
            return;
        Furniture f = new Furniture(type, 1.0, 0, 1.0,
                type.equals("Chair") ? 1.0 : 2.0, 1.0, Color.GRAY);
        currentDesign.addFurniture(f);
        repaint();
    }

    private void deleteSelectedFurniture() {
        if (currentDesign != null && selectedFurniture != null) {
            currentDesign.getFurnitureList().remove(selectedFurniture);
            selectedFurniture = null;
            propertiesPanel.update((Graphics) null);
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