package org.furnish.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Design implements Serializable {
    private static final long serialVersionUID = 1L;
    private Room room;
    private List<Furniture> furnitureList;

    public Design(Room room) {
        this.room = room;
        this.furnitureList = new ArrayList<>();
    }

    public Room getRoom() {
        return room;
    }

    // public List<Furniture> getFurnitureList() {
    //     return furnitureList;
    // }

    public List<Furniture> getFurnitureList() {
        return Collections.unmodifiableList(furnitureList);
    }

    // public void addFurniture(Furniture f) {
    //     furnitureList.add(f);
    // }

    public void addFurniture(Furniture f) {
        if (f != null && !furnitureList.contains(f)) {
            furnitureList.add(f);
        }
    }

    // refactor --
    
    public void removeFurniture(Furniture furniture) {
        furnitureList.remove(furniture);
    }

    public void clearFurniture() {
        furnitureList.clear();
    }
}