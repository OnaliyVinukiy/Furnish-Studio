package org.furnish.core;

import java.awt.*;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Furniture implements Serializable {
    private String type;
    private String subtype;
    private double xPosition, zPosition;
    private double width, depth, height;
    private Color color;
    private float shadeFactor = 1.0f;
    private Map<String, Color> partColors;
    private boolean isSelected;

    public Furniture(String type, double xPosition, double zPosition,
            double width, double depth, double height, Color color) {
        this(type, type.equals("Chair") ? "Standard" : "", xPosition, zPosition, width, depth, height, color);
    }

    public Furniture(String type, String subtype, double xPosition, double zPosition,
            double width, double depth, double height, Color color) {
        this.type = type;
        this.subtype = subtype != null ? subtype : "";
        this.xPosition = xPosition;
        this.zPosition = zPosition;
        this.width = width;
        this.depth = depth;
        this.height = height;
        this.color = color;
        this.partColors = new HashMap<>();
        this.isSelected = false;
        initializePartColors();
    }

    private void initializePartColors() {

        if (type.equals("Chair")) {
            partColors.put("seat", color);
            partColors.put("backrest", color);
            partColors.put("legs", new Color(70, 50, 30)); // Default leg color
            if (subtype.equals("Armchair")) {
                partColors.put("arms", color.darker());
            }
        } else if (type.equals("Table")) {
            partColors.put("top", color);
            partColors.put("legs", color.darker());
        } else if (type.equals("Sofa")) {
            partColors.put("base", color);
            partColors.put("cushions", color.brighter());
            partColors.put("backrest", color);
            partColors.put("arms", color.darker());
            partColors.put("legs", new Color(70, 50, 30));
        } else if (type.equals("Bed")) {
            partColors.put("frame", color.darker());
            partColors.put("mattress", new Color(240, 240, 250));
            partColors.put("headboard", color.darker());
            partColors.put("pillows", new Color(255, 255, 255));
            partColors.put("sheet", new Color(220, 230, 255));
            partColors.put("legs", new Color(70, 50, 30));
        } else if (type.equals("Cabinet")) {
            partColors.put("body", color);
        } else {
            partColors.put("body", color);
        }
    }

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

    public double getZ() {
        return zPosition;
    }

    public double getWidth() {
        return width;
    }

    public double getDepth() {
        return depth;
    }

    public double getHeight() {
        return height;
    }

    public Color getColor() {
        return new Color(
                (int) (color.getRed() * shadeFactor),
                (int) (color.getGreen() * shadeFactor),
                (int) (color.getBlue() * shadeFactor));
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

    public float getShadeFactor() {
        return shadeFactor;
    }

    public void setX(double x) {
        this.xPosition = x;
    }

    public void setZ(double z) {
        this.zPosition = z;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public void setDepth(double depth) {
        this.depth = depth;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public void setColor(Color color) {
        this.color = color;
        if (type.equals("Chair")) {
            if (!partColors.containsKey("seat") || partColors.get("seat").equals(this.color)) {
                partColors.put("seat", color);
            }
            if (!partColors.containsKey("backrest") || partColors.get("backrest").equals(this.color)) {
                partColors.put("backrest", color);
            }
            if (subtype.equals("Armchair")
                    && (!partColors.containsKey("arms") || partColors.get("arms").equals(this.color.darker()))) {
                partColors.put("arms", color.darker());
            }
        } else if (type.equals("Table")) {
            if (!partColors.containsKey("top") || partColors.get("top").equals(this.color)) {
                partColors.put("top", color);
            }
            if (!partColors.containsKey("legs") || partColors.get("legs").equals(this.color.darker())) {
                partColors.put("legs", color.darker());
            }
        } else if (type.equals("Sofa")) {
            if (!partColors.containsKey("base") || partColors.get("base").equals(this.color)) {
                partColors.put("base", color);
            }
            if (!partColors.containsKey("cushions") || partColors.get("cushions").equals(this.color.brighter())) {
                partColors.put("cushions", color.brighter());
            }
            if (!partColors.containsKey("backrest") || partColors.get("backrest").equals(this.color)) {
                partColors.put("backrest", color);
            }
            if (!partColors.containsKey("arms") || partColors.get("arms").equals(this.color.darker())) {
                partColors.put("arms", color.darker());
            }
        } else if (type.equals("Bed")) {
            if (!partColors.containsKey("frame") || partColors.get("frame").equals(this.color.darker())) {
                partColors.put("frame", color.darker());
            }
            if (!partColors.containsKey("headboard") || partColors.get("headboard").equals(this.color.darker())) {
                partColors.put("headboard", color.darker());
            }
        } else if (type.equals("Cabinet")) {
            if (!partColors.containsKey("body") || partColors.get("body").equals(this.color)) {
                partColors.put("body", color);
            }
        } else {
            partColors.put("body", color);
        }
    }

    public void setShadeFactor(float factor) {
        this.shadeFactor = Math.max(0.1f, Math.min(1.0f, factor));
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        this.isSelected = selected;
    }
}