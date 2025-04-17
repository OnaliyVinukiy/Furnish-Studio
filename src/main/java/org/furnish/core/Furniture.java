package org.furnish.core;

import java.awt.*;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Furniture implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public enum Orientation {
        NORTH, EAST, SOUTH, WEST
    }
    
    private String type;
    private String subtype;
    private double xPosition;
    private double zPosition;
    private double width;
    private double depth;
    private double height;
    private Color color;
    private Orientation orientation;
    private Map<String, Color> partColors;
    private boolean isSelected;
    private float shadeFactor = 1.0f;

    // Constructors
    public Furniture(String type, double xPosition, double zPosition,
            double width, double depth, double height, Color color) {
        this(type, type.equals("Chair") ? "Standard" : "", xPosition, zPosition, 
             width, depth, height, color, Orientation.NORTH);
    }

    public Furniture(String type, String subtype, double xPosition, double zPosition,
            double width, double depth, double height, Color color) {
        this(type, subtype, xPosition, zPosition, width, depth, height, color, Orientation.NORTH);
    }

    public Furniture(String type, String subtype, double xPosition, double zPosition,
            double width, double depth, double height, Color color, Orientation orientation) {
        this.type = type;
        this.subtype = subtype != null ? subtype : "";
        this.xPosition = xPosition;
        this.zPosition = zPosition;
        this.width = width;
        this.depth = depth;
        this.height = height;
        this.color = color;
        this.orientation = orientation;
        this.partColors = new HashMap<>();
        this.isSelected = false;
        initializePartColors();
    }

    private void initializePartColors() {
        partColors = new HashMap<>();
        
        if (type.equals("Chair")) {
            partColors.put("seat", color);
            partColors.put("backrest", color);
            partColors.put("legs", new Color(70, 50, 30));
            if (subtype.equals("Armchair")) {
                partColors.put("arms", color.darker());
            }
        } 
        else if (type.equals("Table")) {
            partColors.put("top", color);
            partColors.put("legs", color.darker());
            partColors.put("shelves", color.brighter());
            if (subtype.equals("Coffee")) {
                partColors.put("glass", new Color(200, 230, 255, 150));
            }
        } 
        else if (type.equals("Sofa")) {
            partColors.put("base", color);
            partColors.put("cushions", color.brighter());
            partColors.put("backrest", color);
            partColors.put("arms", color.darker());
            partColors.put("legs", new Color(70, 50, 30));
        } 
        else if (type.equals("Bed")) {
            partColors.put("frame", color.darker());
            partColors.put("mattress", new Color(240, 240, 250));
            partColors.put("headboard", color.darker());
            partColors.put("pillows", new Color(255, 255, 255));
            partColors.put("sheet", new Color(220, 230, 255));
            partColors.put("legs", new Color(70, 50, 30));
            if (subtype.equals("King")) {
                partColors.put("footboard", color.darker());
            }
        } 
        else if (type.equals("Cabinet")) {
            partColors.put("body", color);
            partColors.put("doors", color.darker());
            partColors.put("handles", new Color(200, 200, 200));
            if (subtype.equals("Bookshelf")) {
                partColors.put("shelves", color.brighter());
            }
        } 
        else {
            partColors.put("body", color);
        }
    }

    // Getters and Setters
    public String getType() {
        return type;
    }

    public String getSubtype() {
        return subtype;
    }

    public void setSubtype(String subtype) {
        this.subtype = subtype != null ? subtype : "";
        initializePartColors();
    }

    public double getX() {
        return xPosition;
    }

    public void setX(double xPosition) {
        this.xPosition = xPosition;
    }

    public double getZ() {
        return zPosition;
    }

    public void setZ(double zPosition) {
        this.zPosition = zPosition;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public double getDepth() {
        return depth;
    }

    public void setDepth(double depth) {
        this.depth = depth;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public Color getColor() {
        return new Color(
                (int) (color.getRed() * shadeFactor),
                (int) (color.getGreen() * shadeFactor),
                (int) (color.getBlue() * shadeFactor));
    }

    public void setColor(Color color) {
        this.color = color;
        initializePartColors();
    }

    public Color getPartColor(String part) {
        Color c = partColors.getOrDefault(part, color);
        return new Color(
                (int) (c.getRed() * shadeFactor),
                (int) (c.getGreen() * shadeFactor),
                (int) (c.getBlue() * shadeFactor));
    }

    public void setPartColor(String part, Color color) {
        partColors.put(part, color);
    }

    public Map<String, Color> getPartColors() {
        return partColors;
    }

    public Orientation getOrientation() {
        return orientation;
    }

    public void setOrientation(Orientation orientation) {
        this.orientation = orientation;
    }

    public float getShadeFactor() {
        return shadeFactor;
    }

    public void setShadeFactor(float shadeFactor) {
        this.shadeFactor = Math.max(0.1f, Math.min(1.0f, shadeFactor));
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        this.isSelected = selected;
    }

    // Helper methods
    public double getEffectiveWidth() {
        return (orientation == Orientation.EAST || orientation == Orientation.WEST) ? depth : width;
    }

    public double getEffectiveDepth() {
        return (orientation == Orientation.EAST || orientation == Orientation.WEST) ? width : depth;
    }

    @Override
    public String toString() {
        return type + (subtype.isEmpty() ? "" : " (" + subtype + ")") + 
               " at (" + xPosition + ", " + zPosition + ") " +
               "Size: " + width + "x" + depth + "x" + height +
               " Facing: " + orientation;
    }
}