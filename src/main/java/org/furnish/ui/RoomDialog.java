package org.furnish.ui;

import org.furnish.core.Room;
import javax.swing.*;
import java.awt.*;

class RoomDialog extends JDialog {
    private JTextField lengthField, widthField, heightField;
    private JButton floorColorButton, wallColorButton;
    private Color floorColor = Color.LIGHT_GRAY, wallColor = Color.WHITE;
    private boolean ok = false;

    public RoomDialog(JFrame parent) {
        super(parent, "New Room", true);
        setLayout(new GridLayout(0, 2, 10, 10));
        setSize(300, 250);
        setLocationRelativeTo(parent);

        add(new JLabel("Length (m):"));
        lengthField = new JTextField("5.0", 5);
        add(lengthField);

        add(new JLabel("Width (m):"));
        widthField = new JTextField("5.0", 5);
        add(widthField);

        add(new JLabel("Height (m):"));
        heightField = new JTextField("3.0", 5);
        add(heightField);

        add(new JLabel("Floor Color:"));
        floorColorButton = new JButton("Choose");
        floorColorButton.setBackground(floorColor);
        floorColorButton.addActionListener(e -> {
            Color c = JColorChooser.showDialog(this, "Choose Floor Color", floorColor);
            if (c != null) {
                floorColor = c;
                floorColorButton.setBackground(c);
            }
        });
        add(floorColorButton);

        add(new JLabel("Wall Color:"));
        wallColorButton = new JButton("Choose");
        wallColorButton.setBackground(wallColor);
        wallColorButton.addActionListener(e -> {
            Color c = JColorChooser.showDialog(this, "Choose Wall Color", wallColor);
            if (c != null) {
                wallColor = c;
                wallColorButton.setBackground(c);
            }
        });
        add(wallColorButton);

        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            if (validateInputs()) {
                ok = true;
                setVisible(false);
            }
        });
        add(okButton);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> setVisible(false));
        add(cancelButton);
    }

    private boolean validateInputs() {
        try {
            double length = Double.parseDouble(lengthField.getText());
            double width = Double.parseDouble(widthField.getText());
            double height = Double.parseDouble(heightField.getText());
            if (length <= 0 || width <= 0 || height <= 0) {
                JOptionPane.showMessageDialog(this, "Dimensions must be positive numbers.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }
            return true;
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter valid numbers for dimensions.", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    public boolean isOk() {
        return ok;
    }

    public Room getRoom() {
        double length = Double.parseDouble(lengthField.getText());
        double width = Double.parseDouble(widthField.getText());
        double height = Double.parseDouble(heightField.getText());
        return new Room(length, width, height, floorColor, wallColor);
    }
}