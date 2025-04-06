package org.furnish.ui;

import org.furnish.core.Furniture;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class PropertiesPanel extends JPanel {
    private JTextField xField, zField, widthField, depthField, heightField;
    private JButton colorButton;
    private JSlider shadeSlider;
    private Furniture furniture;

    public PropertiesPanel() {
        initializePanel();
        setupPropertyFields();
    }

    private void initializePanel() {
        setPreferredSize(new Dimension(300, 600));
        setLayout(new GridLayout(0, 2));
        setBorder(BorderFactory.createTitledBorder("Properties"));
    }

    private void setupPropertyFields() {
        // X Position
        add(new JLabel("X:"));
        xField = new JTextField(5);
        xField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                updateProperty("x");
            }
        });
        add(xField);

        // Z Position
        add(new JLabel("Z:"));
        zField = new JTextField(5);
        zField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                updateProperty("z");
            }
        });
        add(zField);

        // Width
        add(new JLabel("Width:"));
        widthField = new JTextField(5);
        widthField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                updateProperty("width");
            }
        });
        add(widthField);

        // Depth
        add(new JLabel("Depth:"));
        depthField = new JTextField(5);
        depthField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                updateProperty("depth");
            }
        });
        add(depthField);

        // Height
        add(new JLabel("Height:"));
        heightField = new JTextField(5);
        heightField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                updateProperty("height");
            }
        });
        add(heightField);

        // Color
        add(new JLabel("Color:"));
        colorButton = new JButton("Choose");
        colorButton.addActionListener(e -> {
            if (furniture != null) {
                Color newColor = JColorChooser.showDialog(
                        this, "Choose Color", furniture.getColor());
                if (newColor != null) {
                    furniture.setColor(newColor);
                    repaintParent();
                }
            }
        });
        add(colorButton);

        // Shade
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
            clearFields();
        } else {
            populateFields(f);
        }
    }

    private void clearFields() {
        xField.setText("");
        zField.setText("");
        widthField.setText("");
        depthField.setText("");
        heightField.setText("");
        shadeSlider.setValue(100);
    }

    private void populateFields(Furniture f) {
        xField.setText(String.format("%.1f", f.getX()));
        zField.setText(String.format("%.1f", f.getZ()));
        widthField.setText(String.format("%.1f", f.getWidth()));
        depthField.setText(String.format("%.1f", f.getDepth()));
        heightField.setText(String.format("%.1f", f.getHeight()));
        shadeSlider.setValue((int) (f.getShadeFactor() * 100));
    }

    private void updateProperty(String property) {
        if (furniture == null)
            return;

        try {
            double value = Double.parseDouble(getFieldValue(property));

            switch (property) {
                case "x":
                    furniture.setX(value);
                    break;
                case "z":
                    furniture.setZ(value);
                    break;
                case "width":
                    furniture.setWidth(value);
                    break;
                case "depth":
                    furniture.setDepth(value);
                    break;
                case "height":
                    furniture.setHeight(value);
                    break;
            }

            repaintParent();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid number format");
            update(furniture);
        }
    }

    private String getFieldValue(String property) {
        switch (property) {
            case "x":
                return xField.getText();
            case "z":
                return zField.getText();
            case "width":
                return widthField.getText();
            case "depth":
                return depthField.getText();
            case "height":
                return heightField.getText();
            default:
                return "";
        }
    }

    private void repaintParent() {
        SwingUtilities.getWindowAncestor(this).repaint();
    }
}