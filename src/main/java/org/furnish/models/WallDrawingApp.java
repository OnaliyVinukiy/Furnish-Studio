package org.furnish.models;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class WallDrawingApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Wall Drawing Panel");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 600);
            frame.add(new WallDrawingPanel());
            frame.setVisible(true);
        });
    }
}
