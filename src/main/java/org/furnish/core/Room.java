package org.furnish.core;

import java.awt.Color;
import java.io.Serializable;

public class Room implements Serializable {
    private static final long serialVersionUID = 1L;

    private double length;
    private double width;
    private double height;
    private Color floorColor;
    private Color wallColor;

    public Room(double length, double width, double height, Color floorColor, Color wallColor) {
        this.length = length;
        this.width = width;
        this.height = height;
        this.floorColor = floorColor;
        this.wallColor = wallColor;
    }

    // Getters and setters
    public double getLength() {
        return length;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public Color getFloorColor() {
        return floorColor;
    }

    public Color getWallColor() {
        return wallColor;
    }

    public void setFloorColor(Color floorColor) {
        this.floorColor = floorColor;
    }

    public void setWallColor(Color wallColor) {
        this.wallColor = wallColor;
    }

    @Override
    public String toString() {
        return String.format("%.1fm x %.1fm x %.1fm", length, width, height);
    }
}