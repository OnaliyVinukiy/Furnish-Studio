package org.furnish.models;

import javax.swing.event.*;
import javax.swing.undo.*;

import org.furnish.core.Design;
import org.furnish.core.Furniture;

public class FurnitureUndoManager extends UndoManager {
    private static final int MAX_UNDO_STEPS = 100;
    private final UndoableEditSupport editSupport = new UndoableEditSupport();

    public FurnitureUndoManager() {
        setLimit(MAX_UNDO_STEPS);
    }

    public void addFurnitureEdit(Design design, Furniture furniture, boolean wasAdded) {
        FurnitureEdit edit = new FurnitureEdit(design, furniture, wasAdded);
        addEdit(edit);
        editSupport.postEdit(edit); // Notify listeners
    }

    public void addUndoableEditListener(UndoableEditListener listener) {
        editSupport.addUndoableEditListener(listener);
    }

    public void removeUndoableEditListener(UndoableEditListener listener) {
        editSupport.removeUndoableEditListener(listener);
    }

    private static class FurnitureEdit extends AbstractUndoableEdit {
        private final Design design;
        private final Furniture furniture;
        private final boolean wasAdded;

        public FurnitureEdit(Design design, Furniture furniture, boolean wasAdded) {
            this.design = design;
            this.furniture = furniture;
            this.wasAdded = wasAdded;
        }

        @Override
        public void undo() throws CannotUndoException {
            super.undo();
            if (wasAdded) {
                design.removeFurniture(furniture);
            } else {
                design.addFurniture(furniture);
            }
        }

        @Override
        public void redo() throws CannotRedoException {
            super.redo();
            if (wasAdded) {
                design.addFurniture(furniture);
            } else {
                design.removeFurniture(furniture);
            }
        }
        
        @Override
        public boolean canRedo() {
            return true;  // Ensure this returns true when redo is possible
        }

        @Override
        public String getPresentationName() {
            return wasAdded ? "Add Furniture" : "Remove Furniture";
        }
    }
}