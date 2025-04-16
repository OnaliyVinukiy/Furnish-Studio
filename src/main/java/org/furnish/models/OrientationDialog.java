package org.furnish.models;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class OrientationDialog extends JDialog {
    private int selectedOrientation = 0;
    
    public OrientationDialog(Frame parent) {
        super(parent, "Select Orientation", true);
        setSize(300, 200);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());
        
        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        ButtonGroup group = new ButtonGroup();
        JRadioButton frontBtn = new JRadioButton("Front");
        JRadioButton rightBtn = new JRadioButton("Right");
        JRadioButton backBtn = new JRadioButton("Back");
        JRadioButton leftBtn = new JRadioButton("Left");
        
        group.add(frontBtn);
        group.add(rightBtn);
        group.add(backBtn);
        group.add(leftBtn);
        frontBtn.setSelected(true);
        
        panel.add(frontBtn);
        panel.add(rightBtn);
        panel.add(backBtn);
        panel.add(leftBtn);
        
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            if (frontBtn.isSelected()) selectedOrientation = 0;
            else if (rightBtn.isSelected()) selectedOrientation = 1;
            else if (backBtn.isSelected()) selectedOrientation = 2;
            else if (leftBtn.isSelected()) selectedOrientation = 3;
            dispose();
        });
        
        add(panel, BorderLayout.CENTER);
        add(okButton, BorderLayout.SOUTH);
    }
    
    public int getSelectedOrientation() {
        return selectedOrientation;
    }
}