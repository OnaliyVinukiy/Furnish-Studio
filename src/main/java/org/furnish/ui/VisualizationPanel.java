package org.furnish.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import org.furnish.core.Design;
import org.furnish.core.Furniture;
import org.furnish.core.Room;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.awt.TextRenderer;
import com.jogamp.opengl.util.gl2.GLUT;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;

public class VisualizationPanel extends GLJPanel implements GLEventListener {
    private Design design;
    private FurnitureDesignApp parent;
    private Furniture draggedFurniture;
    private GLU glu = new GLU();
    private Point dragOffset;
    private float rotationX = 0;
    private float rotationY = 0;
    private double lastMouseX, lastMouseY;
    private float zoomFactor = 1.0f;
    private boolean is3DView = false;
    private FPSAnimator animator;
    private Furniture selectedFurniture;
    private String selectedPart;
    private Map<Integer, Furniture> pickMap;
    private Map<Integer, String> partPickMap;

    // refactor --

    private Texture wallDecorTexture;
    private Point selectionStart = null;
    private Point selectionEnd = null;
    private GLUT glut = new GLUT();
    private boolean toggleGrid = false;

    // -- end refactor    

    public VisualizationPanel(FurnitureDesignApp parent) {
        super(new GLCapabilities(GLProfile.get(GLProfile.GL2)));
        this.parent = parent;
        initializePanel();
        setupMouseListeners();

        animator = new FPSAnimator(this, 60);
        animator.start();
    }

    private void initializePanel() {
        setPreferredSize(new Dimension(1000, 900));
        addGLEventListener(this);
    }




    @Override
    public void init(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glEnable(GL2.GL_DEPTH_TEST);
        gl.glClearColor(0.2f, 0.2f, 0.3f, 1.0f);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        if (animator != null) {
            animator.stop();
        }
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        gl.glLoadIdentity();
        gl.glTranslatef(0.0f, 0.0f, -10.0f * zoomFactor);
        gl.glRotatef(rotationX, 1.0f, 0.0f, 0.0f);
        gl.glRotatef(rotationY, 0.0f, 1.0f, 0.0f);

        if (design != null) {
            Room room = design.getRoom();
            float scaleX = getWidth() / (float) room.getLength();
            float scaleY = getHeight() / (float) room.getWidth();

            gl.glPushMatrix();
            if (is3DView) {
                gl.glScalef(1.0f, 1.0f, 1.0f);
                draw3D(gl);
            } else {
                gl.glTranslatef((float) (-room.getLength() / 2), (float) (-room.getWidth() / 2), 0f);
                draw2D(gl);
            }
            gl.glPopMatrix();
        }
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glViewport(0, 0, width, height);

        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        float aspect = (float) width / height;
        gl.glFrustum(-aspect, aspect, -1.0, 1.0, 5.0, 60.0);

        gl.glMatrixMode(GL2.GL_MODELVIEW);
    }

    private void draw2D(GL2 gl) {
        Room room = design.getRoom();
        gl.glDisable(GL2.GL_DEPTH_TEST);

        setColor(gl, room.getFloorColor());
        drawRect(gl, 0f, 0f, 0f, (float) room.getLength(), (float) room.getWidth(), 0f);

        for (Furniture f : design.getFurnitureList()) {
            float x = (float) f.getX();
            float z = (float) f.getZ();
            float w = (float) f.getWidth();
            float d = (float) f.getDepth();
    
            // Adjust for orientation
            if (f.getOrientation() == Furniture.Orientation.EAST || 
                f.getOrientation() == Furniture.Orientation.WEST) {
                // Swap width and depth for east/west facing furniture
                float temp = w;
                w = d;
                d = temp;
            }
    
            setColor(gl, f.getColor());
            if (f.getType().equals("Chair"))
                drawChair2D(gl, x, z, w, d, f.getSubtype());
            else if (f.getType().equals("Table"))
                drawTable2D(gl, x, z, w, d);
            else if (f.getType().equals("Sofa"))
                drawSofa2D(gl, x, z, w, d);
            else if (f.getType().equals("Bed"))
                drawBed2D(gl, x, z, w, d);
            else if (f.getType().equals("Cabinet"))
                drawCabinet2D(gl, x, z, w, d);
            else
                drawRect(gl, x, z, 0f, w, d, 0f);
        }
        
        
        // Add directional labels
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        glu.gluOrtho2D(0, getWidth(), getHeight(), 0); // Note inverted Y-axis for text
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glLoadIdentity();

        Color labelColor = Color.WHITE;
        int padding = 20;
        
        // Top label (North)
        drawText(gl, "ROOM BACK", getWidth()/2 - 15, padding, labelColor);
        
        // Bottom label (South)
        drawText(gl, "ROOM FRONT", getWidth()/2 - 25, getHeight() - padding, labelColor);
        
        // Left label (West)
        drawText(gl, "LEFT", padding, getHeight()/2, labelColor);
        
        // Right label (East)
        drawText(gl, "RIGHT", getWidth() - padding - 30, getHeight()/2, labelColor);

        gl.glPopMatrix();
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glPopMatrix();
        gl.glMatrixMode(GL2.GL_MODELVIEW);


        gl.glEnable(GL2.GL_DEPTH_TEST);
    }

    private void drawText(GL2 gl, String text, float x, float y, Color color) {
        gl.glColor3f(color.getRed()/255f, color.getGreen()/255f, color.getBlue()/255f);
        gl.glRasterPos2f(x, y);
        
        try {
            Font font = new Font("Arial", Font.BOLD, 12);
            TextRenderer textRenderer = new TextRenderer(font, true, true);
            textRenderer.beginRendering(getWidth(), getHeight());
            textRenderer.setColor(color);
            textRenderer.draw(text, (int)x, (int)y);
            textRenderer.endRendering();
        } catch (Exception e) {
            // Fallback if TextRenderer fails
            for (char c : text.toCharArray()) {
                glut.glutBitmapCharacter(GLUT.BITMAP_HELVETICA_12, c);
            }
        }
    }

    // 2D drawing for chair
    private void drawChair2D(GL2 gl, float x, float z, float w, float d, String subtype) {
        gl.glBegin(GL2.GL_QUADS);
        switch (subtype) {
            case "Standard":
                // Seat
                gl.glVertex3f(x, z, 0f);
                gl.glVertex3f(x + w, z, 0f);
                gl.glVertex3f(x + w, z + d * 0.8f, 0f);
                gl.glVertex3f(x, z + d * 0.8f, 0f);
                // Backrest
                gl.glVertex3f(x + w * 0.1f, z + d * 0.8f, 0f);
                gl.glVertex3f(x + w * 0.9f, z + d * 0.8f, 0f);
                gl.glVertex3f(x + w * 0.9f, z + d, 0f);
                gl.glVertex3f(x + w * 0.1f, z + d, 0f);
                break;
            case "Armchair":
                // Seat
                gl.glVertex3f(x + w * 0.1f, z + d * 0.1f, 0f);
                gl.glVertex3f(x + w * 0.9f, z + d * 0.1f, 0f);
                gl.glVertex3f(x + w * 0.9f, z + d * 0.7f, 0f);
                gl.glVertex3f(x + w * 0.1f, z + d * 0.7f, 0f);
                // Backrest
                gl.glVertex3f(x + w * 0.1f, z + d * 0.7f, 0f);
                gl.glVertex3f(x + w * 0.9f, z + d * 0.7f, 0f);
                gl.glVertex3f(x + w * 0.9f, z + d, 0f);
                gl.glVertex3f(x + w * 0.1f, z + d, 0f);
                // Left armrest
                gl.glVertex3f(x, z + d * 0.1f, 0f);
                gl.glVertex3f(x + w * 0.1f, z + d * 0.1f, 0f);
                gl.glVertex3f(x + w * 0.1f, z + d * 0.7f, 0f);
                gl.glVertex3f(x, z + d * 0.7f, 0f);
                // Right armrest
                gl.glVertex3f(x + w * 0.9f, z + d * 0.1f, 0f);
                gl.glVertex3f(x + w, z + d * 0.1f, 0f);
                gl.glVertex3f(x + w, z + d * 0.7f, 0f);
                gl.glVertex3f(x + w * 0.9f, z + d * 0.7f, 0f);
                break;
            case "Dining":
                // Seat
                gl.glVertex3f(x + w * 0.05f, z, 0f);
                gl.glVertex3f(x + w * 0.95f, z, 0f);
                gl.glVertex3f(x + w * 0.95f, z + d * 0.9f, 0f);
                gl.glVertex3f(x + w * 0.05f, z + d * 0.9f, 0f);
                // Backrest
                gl.glVertex3f(x + w * 0.3f, z + d * 0.9f, 0f);
                gl.glVertex3f(x + w * 0.7f, z + d * 0.9f, 0f);
                gl.glVertex3f(x + w * 0.7f, z + d * 1.1f, 0f);
                gl.glVertex3f(x + w * 0.3f, z + d * 1.1f, 0f);
                break;
            default:
                // Fallback to standard chair
                gl.glVertex3f(x, z, 0f);
                gl.glVertex3f(x + w, z, 0f);
                gl.glVertex3f(x + w, z + d * 0.8f, 0f);
                gl.glVertex3f(x, z + d * 0.8f, 0f);
                gl.glVertex3f(x + w * 0.1f, z + d * 0.8f, 0f);
                gl.glVertex3f(x + w * 0.9f, z + d * 0.8f, 0f);
                gl.glVertex3f(x + w * 0.9f, z + d, 0f);
                gl.glVertex3f(x + w * 0.1f, z + d, 0f);
        }
        gl.glEnd();
    }

    // 2D drawing for table
    private void drawTable2D(GL2 gl, float x, float z, float w, float d) {
        gl.glBegin(GL2.GL_QUADS);
        // Tabletop
        gl.glVertex3f(x, z, 0f);
        gl.glVertex3f(x + w, z, 0f);
        gl.glVertex3f(x + w, z + d, 0f);
        gl.glVertex3f(x, z + d, 0f);
        gl.glEnd();

        // Legs
        float legSize = w * 0.1f;
        setColor(gl, Color.DARK_GRAY);
        gl.glBegin(GL2.GL_QUADS);
        gl.glVertex3f(x, z, 0f);
        gl.glVertex3f(x + legSize, z, 0f);
        gl.glVertex3f(x + legSize, z + legSize, 0f);
        gl.glVertex3f(x, z + legSize, 0f);

        gl.glVertex3f(x + w - legSize, z, 0f);
        gl.glVertex3f(x + w, z, 0f);
        gl.glVertex3f(x + w, z + legSize, 0f);
        gl.glVertex3f(x + w - legSize, z + legSize, 0f);

        gl.glVertex3f(x, z + d - legSize, 0f);
        gl.glVertex3f(x + legSize, z + d - legSize, 0f);
        gl.glVertex3f(x + legSize, z + d, 0f);
        gl.glVertex3f(x, z + d, 0f);

        gl.glVertex3f(x + w - legSize, z + d - legSize, 0f);
        gl.glVertex3f(x + w, z + d - legSize, 0f);
        gl.glVertex3f(x + w, z + d, 0f);
        gl.glVertex3f(x + w - legSize, z + d, 0f);
        gl.glEnd();
    }

    // 2D drawing for sofa
    private void drawSofa2D(GL2 gl, float x, float z, float w, float d) {
        gl.glBegin(GL2.GL_QUADS);
        // Base
        gl.glVertex3f(x, z, 0f);
        gl.glVertex3f(x + w, z, 0f);
        gl.glVertex3f(x + w, z + d * 0.6f, 0f);
        gl.glVertex3f(x, z + d * 0.6f, 0f);
        // Backrest
        gl.glVertex3f(x, z + d * 0.6f, 0f);
        gl.glVertex3f(x + w, z + d * 0.6f, 0f);
        gl.glVertex3f(x + w, z + d, 0f);
        gl.glVertex3f(x, z + d, 0f);
        // Left armrest
        gl.glVertex3f(x, z + d * 0.2f, 0f);
        gl.glVertex3f(x + w * 0.2f, z + d * 0.2f, 0f);
        gl.glVertex3f(x + w * 0.2f, z + d * 0.8f, 0f);
        gl.glVertex3f(x, z + d * 0.8f, 0f);
        // Right armrest
        gl.glVertex3f(x + w - w * 0.2f, z + d * 0.2f, 0f);
        gl.glVertex3f(x + w, z + d * 0.2f, 0f);
        gl.glVertex3f(x + w, z + d * 0.8f, 0f);
        gl.glVertex3f(x + w - w * 0.2f, z + d * 0.8f, 0f);
        gl.glEnd();
    }

    // 2D drawing for bed
    private void drawBed2D(GL2 gl, float x, float z, float w, float d) {
        gl.glBegin(GL2.GL_QUADS);
        // Mattress
        gl.glVertex3f(x, z, 0f);
        gl.glVertex3f(x + w, z, 0f);
        gl.glVertex3f(x + w, z + d * 0.8f, 0f);
        gl.glVertex3f(x, z + d * 0.8f, 0f);
        // Headboard
        gl.glVertex3f(x, z + d * 0.8f, 0f);
        gl.glVertex3f(x + w, z + d * 0.8f, 0f);
        gl.glVertex3f(x + w, z + d, 0f);
        gl.glVertex3f(x, z + d, 0f);
        gl.glEnd();
    }

    // 2D drawing for cabinet
    private void drawCabinet2D(GL2 gl, float x, float z, float w, float d) {
        gl.glBegin(GL2.GL_QUADS);
        // Body
        gl.glVertex3f(x, z, 0f);
        gl.glVertex3f(x + w, z, 0f);
        gl.glVertex3f(x + w, z + d, 0f);
        gl.glVertex3f(x, z + d, 0f);
        gl.glEnd();

        // Handles
        setColor(gl, Color.BLACK);
        gl.glBegin(GL2.GL_LINES);
        gl.glVertex3f(x + w * 0.4f, z + d * 0.5f, 0f);
        gl.glVertex3f(x + w * 0.6f, z + d * 0.5f, 0f);
        gl.glEnd();
    }

    private void draw3D(GL2 gl) {
        if (design == null) return;

        Room room = design.getRoom();

        // Enable lighting for better 3D effect
        enableLighting(gl);
        
        // Draw floor
        drawFloor(gl, room);

        if (getToggleGrid()) {
            toggleGrid(gl, room);
        }
        

        // Draw walls (4 walls to form a complete room)
        drawWalls(gl, room);
        
        // Draw furniture (keep your existing furniture drawing code)
        drawFurniture(gl);
        
        // Disable lighting if you don't need it for other elements
        disableLighting(gl);


        // Draw floor
        setColor(gl, room.getFloorColor());
        drawRect(gl, 0f, 0f, 0f,
                (float) room.getLength(),
                0f,
                (float) room.getWidth());

        // Draw walls
        setColor(gl, room.getWallColor());
        drawRect(gl, 0f, 0f, 0f,
                (float) room.getLength(),
                (float) room.getHeight(),
                0f);
        drawRect(gl, 0f, 0f, 0f,
                0f,
                (float) room.getHeight(),
                (float) room.getWidth());
        

        for (Furniture f : design.getFurnitureList()) {
            double centerX = design.getRoom().getLength() / 2;
            double centerZ = design.getRoom().getWidth() / 2;

            // Define a threshold distance to consider furniture as centered
            double threshold = 0.5; // Adjust as needed

            double furnitureX = f.getX();
            double furnitureZ = f.getZ();

            // Check if furniture is near the center
            boolean isCentered = Math.abs(furnitureX - centerX) < threshold && Math.abs(furnitureZ - centerZ) < threshold;

            if (isCentered || !isCentered && f.getType().equals("Chair")) {
                // Skip drawing this furniture to make it invisible
                continue;
            }

            // Existing drawing code for furniture
            if (f.getType().equals("Chair")) {
                drawChair3D(gl, f);
            } else if (f.getType().equals("Table")) {
                drawTable3D(gl, f);
            } else if (f.getType().equals("Sofa")) {
                drawSofa3D(gl, f);
            } else if (f.getType().equals("Bed")) {
                drawBed3D(gl, f);
            } else {
                setColor(gl, f.getPartColor("body"));
                drawBox(gl,
                        (float) f.getX(),
                        0f,
                        (float) f.getZ(),
                        (float) f.getWidth(),
                        (float) f.getHeight(),
                        (float) f.getDepth());
            }
        }

    }

    // refactor --

    private void enableLighting(GL2 gl) {
        float[] lightAmbient = {0.5f, 0.5f, 0.5f, 1.0f};
        float[] lightDiffuse = {0.7f, 0.7f, 0.7f, 1.0f};
        float[] lightSpecular = {1.0f, 1.0f, 1.0f, 1.0f};
        float[] lightPosition = {0.0f, 10.0f, 0.0f, 1.0f};

        gl.glEnable(GL2.GL_LIGHTING);
        gl.glEnable(GL2.GL_LIGHT0);
        
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_AMBIENT, lightAmbient, 0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE, lightDiffuse, 0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_SPECULAR, lightSpecular, 0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, lightPosition, 0);
        
        gl.glEnable(GL2.GL_COLOR_MATERIAL);
        gl.glColorMaterial(GL2.GL_FRONT_AND_BACK, GL2.GL_AMBIENT_AND_DIFFUSE);
    }
    private void disableLighting(GL2 gl) {
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glDisable(GL2.GL_LIGHT0);
    }

    private void drawFloor(GL2 gl, Room room) {
        setColor(gl, room.getFloorColor());
        
        float length = (float) room.getLength();
        float width = (float) room.getWidth();
        
        // Draw floor as a quad
        gl.glBegin(GL2.GL_QUADS);
        gl.glNormal3f(0, 1, 0); // Normal pointing up
        gl.glVertex3f(0, 0, 0);
        gl.glVertex3f(length, 0, 0);
        gl.glVertex3f(length, 0, width);
        gl.glVertex3f(0, 0, width);
        gl.glEnd();
    }

    public void setToggleGrid(boolean value) {
        this.toggleGrid = value;
    }
    
    public boolean getToggleGrid() {
        return this.toggleGrid;
    }
    

    private  void toggleGrid(GL2 gl, Room room){
        setColor(gl, room.getFloorColor());
        
        float length = (float) room.getLength();
        float width = (float) room.getWidth();
        // Optional: Add grid lines for better visibility
        setColor(gl, Color.DARK_GRAY);
        gl.glBegin(GL2.GL_LINES);
        float gridSize = 1.0f; // 1 meter grid
        for (float x = 0; x <= length; x += gridSize) {
            gl.glVertex3f(x, 0.01f, 0);
            gl.glVertex3f(x, 0.01f, width);
        }
        for (float z = 0; z <= width; z += gridSize) {
            gl.glVertex3f(0, 0.01f, z);
            gl.glVertex3f(length, 0.01f, z);
        }
        gl.glEnd();
    }

    private void drawWalls(GL2 gl, Room room) {
        setColor(gl, room.getWallColor());
        
        float length = (float) room.getLength();
        float width = (float) room.getWidth();
        float height = (float) room.getHeight();
        float borderThickness = 0.02f;
        
        // Draw two connected walls forming a corner
        gl.glBegin(GL2.GL_QUADS);
        
        // Wall 1 (Front wall - along length)
        gl.glNormal3f(0, 0, 1);
        gl.glVertex3f(0, 0, 0);
        gl.glVertex3f(length, 0, 0);
        gl.glVertex3f(length, height, 0);
        gl.glVertex3f(0, height, 0);
        
        // Wall 2 (Left wall - along width, connected to Wall 1)
        gl.glNormal3f(-1, 0, 0);
        gl.glVertex3f(0, 0, 0);
        gl.glVertex3f(0, 0, width);
        gl.glVertex3f(0, height, width);
        gl.glVertex3f(0, height, 0);
        
        gl.glEnd();
        
        // Draw border lines (now from inside)
        setColor(gl, Color.BLACK);
        gl.glLineWidth(2.0f);
        
        // Border for front wall (drawn slightly inside)
        gl.glBegin(GL2.GL_LINE_LOOP);
        gl.glVertex3f(borderThickness, borderThickness, borderThickness); // bottom left
        gl.glVertex3f(length - borderThickness, borderThickness, borderThickness); // bottom right
        gl.glVertex3f(length - borderThickness, height - borderThickness, borderThickness); // top right
        gl.glVertex3f(borderThickness, height - borderThickness, borderThickness); // top left
        gl.glEnd();
        
        // Border for left wall (drawn slightly inside)
        gl.glBegin(GL2.GL_LINE_LOOP);
        gl.glVertex3f(borderThickness, borderThickness, borderThickness); // bottom front
        gl.glVertex3f(borderThickness, borderThickness, width - borderThickness); // bottom back
        gl.glVertex3f(borderThickness, height - borderThickness, width - borderThickness); // top back
        gl.glVertex3f(borderThickness, height - borderThickness, borderThickness); // top front
        gl.glEnd();
        
        // Reset line width
        gl.glLineWidth(1.0f);
        
        // Add image decoration to left wall (centered, inside the room)
        float decorWidth = width * 0.3f;  // 30% of wall width
        float decorHeight = height * 0.25f; // 25% of wall height
        float decorYPos = height * 0.5f - decorHeight/2; // Vertical center
        float decorZPos = width * 0.5f - decorWidth/2;   // Horizontal center
        
        // Load texture if not already loaded
        if (wallDecorTexture == null) {
            wallDecorTexture = loadTexture(gl);
            if (wallDecorTexture == null) {
                // Fallback: draw a colored rectangle if texture fails
                setColor(gl, Color.RED);
                gl.glBegin(GL2.GL_QUADS);
                gl.glVertex3f(0.01f, decorYPos, decorZPos);
                gl.glVertex3f(0.01f, decorYPos, decorZPos + decorWidth);
                gl.glVertex3f(0.01f, decorYPos + decorHeight, decorZPos + decorWidth);
                gl.glVertex3f(0.01f, decorYPos + decorHeight, decorZPos);
                gl.glEnd();
                return;
            }
        }
        
        // Draw the image decoration
        if (wallDecorTexture != null) {
            drawWallImage(gl, decorYPos, decorZPos, decorWidth, decorHeight);
        }
        
        addWallLamp(gl, room, decorYPos + decorHeight, decorZPos + decorWidth/2);
    }

    private Texture loadTexture(GL2 gl) {
        try {
            // Load from resources
            InputStream stream = getClass().getResourceAsStream("/images/decor.png");
            if (stream == null) {
                System.err.println("Could not find texture in resources");
                return null;
            }
            
            Texture texture = TextureIO.newTexture(stream, true, "png");
            texture.setTexParameteri(gl, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR);
            texture.setTexParameteri(gl, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
            return texture;
        } catch (IOException | GLException e) {
            System.err.println("Error loading texture: " + e.getMessage());
            return null;
        }
    }

    private void drawWallImage(GL2 gl, float yPos, float zPos, float width, float height) {
        float depth = 0.01f;
        
        // Enable texture and set parameters
        gl.glEnable(GL2.GL_TEXTURE_2D);
        wallDecorTexture.enable(gl);
        wallDecorTexture.bind(gl);
        
        // Set texture environment
        gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_REPLACE);
        
        // Enable blending for transparency if needed
        gl.glEnable(GL2.GL_BLEND);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
        
        gl.glBegin(GL2.GL_QUADS);
        gl.glNormal3f(1, 0, 0);
        
        gl.glTexCoord2f(0, 1);  // Bottom-left
        gl.glVertex3f(depth, yPos, zPos);
        
        gl.glTexCoord2f(1, 1);  // Bottom-right
        gl.glVertex3f(depth, yPos, zPos + width);
        
        gl.glTexCoord2f(1, 0);  // Top-right
        gl.glVertex3f(depth, yPos + height, zPos + width);
        
        gl.glTexCoord2f(0, 0);  // Top-left
        gl.glVertex3f(depth, yPos + height, zPos);
        
        gl.glEnd();
        
        // Clean up state
        wallDecorTexture.disable(gl);
        gl.glDisable(GL2.GL_TEXTURE_2D);
        gl.glDisable(GL2.GL_BLEND);
    }
    
    private void addWallLamp(GL2 gl, Room room, float baseY, float centerZ) {

        float lampHeight = (float)(room.getHeight() * 0.15);
        float lampWidth = (float)(room.getWidth() * 0.08);
        float lampDepth = 0.1f;
        
        // Lamp base (wall mount)
        setColor(gl, new Color(200, 200, 200));
        gl.glBegin(GL2.GL_QUADS);
        gl.glNormal3f(1, 0, 0);
        gl.glVertex3f(0.02f, baseY, centerZ - lampWidth/2);
        gl.glVertex3f(0.02f, baseY, centerZ + lampWidth/2); 
        gl.glVertex3f(0.02f, baseY - lampHeight*0.3f, centerZ + lampWidth/2); // Using lampHeight
        gl.glVertex3f(0.02f, baseY - lampHeight*0.3f, centerZ - lampWidth/2);
        gl.glEnd();
        
        // Lamp arm (extending from wall)
        setColor(gl, new Color(180, 180, 180));
        gl.glBegin(GL2.GL_QUADS);
        // Top
        gl.glNormal3f(0, 1, 0);
        gl.glVertex3f(0.02f, baseY - lampHeight*0.25f, centerZ - 0.02f);
        gl.glVertex3f(lampDepth, baseY - lampHeight*0.25f, centerZ - 0.02f);
        gl.glVertex3f(lampDepth, baseY - lampHeight*0.25f, centerZ + 0.02f);
        gl.glVertex3f(0.02f, baseY - lampHeight*0.25f, centerZ + 0.02f);
        // Bottom
        gl.glNormal3f(0, -1, 0);
        gl.glVertex3f(0.02f, baseY - 0.07f, centerZ - 0.02f);
        gl.glVertex3f(lampDepth, baseY - 0.07f, centerZ - 0.02f);
        gl.glVertex3f(lampDepth, baseY - 0.07f, centerZ + 0.02f);
        gl.glVertex3f(0.02f, baseY - 0.07f, centerZ + 0.02f);
        // Front
        gl.glNormal3f(0, 0, 1);
        gl.glVertex3f(0.02f, baseY - 0.07f, centerZ + 0.02f);
        gl.glVertex3f(lampDepth, baseY - 0.07f, centerZ + 0.02f);
        gl.glVertex3f(lampDepth, baseY - 0.03f, centerZ + 0.02f);
        gl.glVertex3f(0.02f, baseY - 0.03f, centerZ + 0.02f);
        // Back
        gl.glNormal3f(0, 0, -1);
        gl.glVertex3f(0.02f, baseY - 0.07f, centerZ - 0.02f);
        gl.glVertex3f(lampDepth, baseY - 0.07f, centerZ - 0.02f);
        gl.glVertex3f(lampDepth, baseY - 0.03f, centerZ - 0.02f);
        gl.glVertex3f(0.02f, baseY - 0.03f, centerZ - 0.02f);
        gl.glEnd();
        
        // Lamp shade (bulb housing)
        setColor(gl, new Color(240, 240, 200)); // Off-white shade
        gl.glBegin(GL2.GL_QUADS);
        // Front
        gl.glNormal3f(0, 0, 1);
        gl.glVertex3f(lampDepth, baseY - lampHeight*0.7f, centerZ - 0.05f);
        gl.glVertex3f(lampDepth, baseY - lampHeight*0.7f, centerZ + 0.05f);
        gl.glVertex3f(lampDepth, baseY - lampHeight*0.2f, centerZ + 0.03f); 
        gl.glVertex3f(lampDepth, baseY - lampHeight*0.2f, centerZ - 0.03f);
        // Back
        gl.glNormal3f(0, 0, -1);
        gl.glVertex3f(lampDepth - 0.02f, baseY - 0.1f, centerZ - 0.05f);
        gl.glVertex3f(lampDepth - 0.02f, baseY - 0.1f, centerZ + 0.05f);
        gl.glVertex3f(lampDepth - 0.02f, baseY - 0.03f, centerZ + 0.03f);
        gl.glVertex3f(lampDepth - 0.02f, baseY - 0.03f, centerZ - 0.03f);
        // Sides
        gl.glNormal3f(1, 0, 0);
        gl.glVertex3f(lampDepth, baseY - 0.1f, centerZ - 0.05f);
        gl.glVertex3f(lampDepth - 0.02f, baseY - 0.1f, centerZ - 0.05f);
        gl.glVertex3f(lampDepth - 0.02f, baseY - 0.03f, centerZ - 0.03f);
        gl.glVertex3f(lampDepth, baseY - 0.03f, centerZ - 0.03f);
        
        gl.glVertex3f(lampDepth, baseY - 0.1f, centerZ + 0.05f);
        gl.glVertex3f(lampDepth - 0.02f, baseY - 0.1f, centerZ + 0.05f);
        gl.glVertex3f(lampDepth - 0.02f, baseY - 0.03f, centerZ + 0.03f);
        gl.glVertex3f(lampDepth, baseY - 0.03f, centerZ + 0.03f);
        gl.glEnd();
        
        // Light bulb (yellow glow)
        setColor(gl, new Color(255, 255, 150));
        gl.glBegin(GL2.GL_QUADS);
        gl.glNormal3f(-1, 0, 0);
        gl.glVertex3f(lampDepth - 0.01f, baseY - lampHeight*0.5f, centerZ - 0.03f);
        gl.glVertex3f(lampDepth - 0.01f, baseY - lampHeight*0.5f, centerZ + 0.03f);
        gl.glVertex3f(lampDepth - 0.01f, baseY - lampHeight*0.4f, centerZ + 0.03f);
        gl.glVertex3f(lampDepth - 0.01f, baseY - lampHeight*0.4f, centerZ - 0.03f);
        gl.glEnd();
    }

    private boolean isCentered(Furniture f, Room room) {
        double centerX = room.getLength() / 2.0;
        double centerZ = room.getWidth() / 2.0;
        double furnitureCenterX = f.getX() + f.getWidth() / 2.0;
        double furnitureCenterZ = f.getZ() + f.getDepth() / 2.0;
        
        // Define a threshold for what's considered "centered"
        double threshold = 20.5; // Half a meter from center
        return Math.abs(furnitureCenterX - centerX) < threshold && 
               Math.abs(furnitureCenterZ - centerZ) < threshold;
    }

    private boolean isWithinRoomBounds(Furniture f, Room room) {
        // Check if furniture is completely within the room's boundaries
        return f.getX() >= 0 && 
               f.getX() + f.getWidth() <= room.getLength() && 
               f.getZ() >= 0 && 
               f.getZ() + f.getDepth() <= room.getWidth();
    }
    private boolean isAtOrigin(Furniture f) {
        return f.getX() == 0 && f.getZ() == 0;
    }
    

    private void drawFurniture(GL2 gl) {
        if (design == null) return;
    
        Room room = design.getRoom();

        for (Furniture f : design.getFurnitureList()) {

            if (is3DView && ((f.getType().equals("Chair") && isAtOrigin(f)) || 
                (!f.getType().equals("Chair") && isCentered(f, room)))) {
                continue;
            }

            // if (is3DView && !isWithinRoomBounds(f, room)) {
            //     continue;
            // }
            

            gl.glPushMatrix();
            
            // Position the furniture
            gl.glTranslatef((float)f.getX(), 0, (float)f.getZ());
            
            // Apply orientation rotation
            switch(f.getOrientation()) {
                case NORTH:
                    break; // Default, no rotation
                case EAST:
                    gl.glRotatef(90, 0, 1, 0);
                    break;
                case SOUTH:
                    gl.glRotatef(180, 0, 1, 0);
                    break;
                case WEST:
                    gl.glRotatef(270, 0, 1, 0);
                    break;
            }
    
            // Draw centered at origin
            if (f.getType().equals("Chair")) {
                drawChair3D(gl, f);
            } else if (f.getType().equals("Table")) {
                drawTable3D(gl, f);
            } else if (f.getType().equals("Sofa")) {
                drawSofa3D(gl, f);
            } else if (f.getType().equals("Bed")) {
                drawBed3D(gl, f);
            } else {
                setColor(gl, f.getColor());
                drawBox(gl, 
                      -(float)f.getWidth()/2, 0, -(float)f.getDepth()/2,
                      (float)f.getWidth(), 
                      (float)f.getHeight(), 
                      (float)f.getDepth());
            }
            
            gl.glPopMatrix();
        }
    }
    
    // end refactor --



    // 3D drawing for chair
    private void drawChair3D(GL2 gl, Furniture f) {
        float chairWidth = (float)f.getWidth();
        float chairDepth = (float)f.getDepth();
        float chairHeight = (float)f.getHeight();
        String subtype = f.getSubtype();
        float seatHeight = chairHeight * 0.5f;
        float seatThickness = chairHeight * 0.05f;
        float legThickness = chairWidth * 0.08f;
        float legHeight = seatHeight;
        float backHeight = chairHeight * 0.7f;
        float backThickness = chairWidth * 0.05f;
    
        // Selection highlight
        if (f.isSelected()) {
            gl.glPushAttrib(GL2.GL_CURRENT_BIT);
            gl.glColor3f(1.0f, 1.0f, 0.0f);
            gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_LINE);
            drawBox(gl, -chairWidth/2 - 0.05f, -0.05f, -chairDepth/2 - 0.05f,
                    chairWidth + 0.1f, chairHeight + 0.1f, chairDepth + 0.1f);
            gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_FILL);
            gl.glPopAttrib();
        }
    
        // Draw based on subtype
        switch (subtype) {
            case "Standard":
                // Seat
                setColor(gl, f.getPartColor("seat"));
                drawBox(gl, -chairWidth/2, seatHeight, -chairDepth/2, 
                       chairWidth, seatThickness, chairDepth);
                
                // Backrest
                setColor(gl, f.getPartColor("backrest"));
                drawBox(gl, -chairWidth/2, seatHeight + seatThickness, chairDepth/2 - backThickness, 
                       chairWidth, backHeight, backThickness);
                
                // Legs
                setColor(gl, f.getPartColor("legs"));
                drawBox(gl, -chairWidth/2, 0, -chairDepth/2, 
                       legThickness, legHeight, legThickness);
                drawBox(gl, chairWidth/2 - legThickness, 0, -chairDepth/2, 
                       legThickness, legHeight, legThickness);
                drawBox(gl, -chairWidth/2, 0, chairDepth/2 - legThickness, 
                       legThickness, legHeight, legThickness);
                drawBox(gl, chairWidth/2 - legThickness, 0, chairDepth/2 - legThickness, 
                       legThickness, legHeight, legThickness);
                break;
    
            case "Armchair":
                // Seat
                setColor(gl, f.getPartColor("seat"));
                float armRadius = chairWidth * 0.05f;
                drawRoundedCube(gl, -chairWidth*0.4f, seatHeight, -chairDepth*0.4f,
                              chairWidth*0.8f, seatThickness*1.5f, chairDepth*0.8f, 
                              armRadius, 16);
                
                // Backrest
                setColor(gl, f.getPartColor("backrest"));
                drawRoundedCube(gl, -chairWidth*0.4f, seatHeight + seatThickness*1.5f, -chairDepth/2,
                              chairWidth*0.8f, backHeight, backThickness*3f, 
                              armRadius, 16);
                
                // Armrests
                setColor(gl, f.getPartColor("arms"));
                drawRoundedCube(gl, -chairWidth/2, seatHeight, -chairDepth/2,
                              chairWidth*0.1f, chairHeight*0.4f, chairDepth, 
                              armRadius, 16);
                drawRoundedCube(gl, chairWidth/2 - chairWidth*0.1f, seatHeight, -chairDepth/2,
                              chairWidth*0.1f, chairHeight*0.4f, chairDepth, 
                              armRadius, 16);
                
                // Legs
                setColor(gl, f.getPartColor("legs"));
                float adjustedLegHeight = seatHeight + seatThickness*1.5f;
                drawBox(gl, -chairWidth/2 + legThickness, 0, -chairDepth/2 + legThickness, 
                       legThickness, adjustedLegHeight, legThickness);
                drawBox(gl, chairWidth/2 - legThickness*2, 0, -chairDepth/2 + legThickness, 
                       legThickness, adjustedLegHeight, legThickness);
                drawBox(gl, -chairWidth/2 + legThickness, 0, chairDepth/2 - legThickness*2, 
                       legThickness, adjustedLegHeight, legThickness);
                drawBox(gl, chairWidth/2 - legThickness*2, 0, chairDepth/2 - legThickness*2, 
                       legThickness, adjustedLegHeight, legThickness);
                break;
    
            case "Dining":
                // Seat
                setColor(gl, f.getPartColor("seat"));
                drawRoundedCube(gl, -chairWidth/2, seatHeight, -chairDepth/2,
                              chairWidth, seatThickness*1.5f, chairDepth, 
                              chairWidth*0.03f, 16);
                
                // Backrest slats
                setColor(gl, f.getPartColor("backrest"));
                float slatWidth = chairWidth * 0.25f;
                float slatSpacing = chairWidth * 0.05f;
                float slatHeight = chairHeight * 0.9f;
                float slatDepth = chairDepth * 0.05f;
                for (int i = 0; i < 3; i++) {
                    float slatX = -chairWidth/2 + chairWidth*0.1f + i*(slatWidth + slatSpacing);
                    drawBox(gl, slatX, seatHeight + seatThickness*1.5f, chairDepth/2 - slatDepth,
                           slatWidth, slatHeight, slatDepth);
                }
                
                // Legs (cylindrical)
                setColor(gl, f.getPartColor("legs"));
                float legRadius = legThickness * 0.35f;
                drawCylinder(gl, -chairWidth/2 + legRadius, 0, -chairDepth/2 + legRadius,
                            legRadius, seatHeight, 16);
                drawCylinder(gl, chairWidth/2 - legRadius, 0, -chairDepth/2 + legRadius,
                            legRadius, seatHeight, 16);
                drawCylinder(gl, -chairWidth/2 + legRadius, 0, chairDepth/2 - legRadius,
                            legRadius, seatHeight, 16);
                drawCylinder(gl, chairWidth/2 - legRadius, 0, chairDepth/2 - legRadius,
                            legRadius, seatHeight, 16);
                break;
    
            default:
                // Default chair (similar to Standard)
                setColor(gl, f.getPartColor("seat"));
                drawBox(gl, -chairWidth/2, seatHeight, -chairDepth/2, 
                       chairWidth, seatThickness, chairDepth);
                
                setColor(gl, f.getPartColor("backrest"));
                drawBox(gl, -chairWidth/2, seatHeight + seatThickness, chairDepth/2 - backThickness, 
                       chairWidth, backHeight, backThickness);
                
                setColor(gl, f.getPartColor("legs"));
                drawBox(gl, -chairWidth/2, 0, -chairDepth/2, 
                       legThickness, legHeight, legThickness);
                drawBox(gl, chairWidth/2 - legThickness, 0, -chairDepth/2, 
                       legThickness, legHeight, legThickness);
                drawBox(gl, -chairWidth/2, 0, chairDepth/2 - legThickness, 
                       legThickness, legHeight, legThickness);
                drawBox(gl, chairWidth/2 - legThickness, 0, chairDepth/2 - legThickness, 
                       legThickness, legHeight, legThickness);
        }
    }
    


    // 3D drawing for table
    private void drawTable3D(GL2 gl, Furniture f) {
        float tableX = (float) f.getX();
        float tableZ = (float) f.getZ();
        float tableWidth = (float) f.getWidth();
        float tableDepth = (float) f.getDepth();
        float tableHeight = (float) f.getHeight();

        float topThickness = tableHeight * 0.05f;
        float legThickness = tableWidth * 0.08f;
        float legHeight = tableHeight - topThickness;

        if (f.isSelected()) {
            gl.glPushAttrib(GL2.GL_CURRENT_BIT);
            gl.glColor3f(1.0f, 1.0f, 0.0f);
            gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_LINE);
            drawBox(gl, tableX - 0.05f, 0, tableZ - 0.05f,
                    tableWidth + 0.1f, tableHeight + 0.1f, tableDepth + 0.1f);
            gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_FILL);
            gl.glPopAttrib();
        }

        // Draw Table Top
        setColor(gl, f.getPartColor("top"));
        drawBox(gl, tableX, legHeight, tableZ, tableWidth, topThickness, tableDepth);

        // Draw Legs
        setColor(gl, f.getPartColor("legs"));
        drawBox(gl, tableX, 0, tableZ, legThickness, legHeight, legThickness);
        drawBox(gl, tableX + tableWidth - legThickness, 0, tableZ, legThickness, legHeight, legThickness);
        drawBox(gl, tableX, 0, tableZ + tableDepth - legThickness, legThickness, legHeight, legThickness);
        drawBox(gl, tableX + tableWidth - legThickness, 0, tableZ + tableDepth - legThickness, legThickness, legHeight,
                legThickness);
    }

    // 3D drawing for bed
    private void drawBed3D(GL2 gl, Furniture f) {
        float bedX = (float) f.getX();
        float bedZ = (float) f.getZ();
        float bedWidth = (float) f.getWidth();
        float bedDepth = (float) f.getDepth();
        float bedHeight = (float) f.getHeight();

        float frameThickness = 0.15f;
        float mattressHeight = bedHeight * 0.25f;
        float headboardHeight = bedHeight * 0.7f;
        float legHeight = bedHeight * 0.1f;
        float pillowWidth = bedWidth * 0.4f;
        float pillowDepth = bedDepth * 0.15f;
        float pillowHeight = mattressHeight * 0.3f;
        int slices = 8;

        if (f.isSelected()) {
            gl.glPushAttrib(GL2.GL_CURRENT_BIT);
            gl.glColor3f(1.0f, 1.0f, 0.0f);
            gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_LINE);
            drawBox(gl, bedX - 0.05f, 0, bedZ - 0.05f,
                    bedWidth + 0.1f, bedHeight + 0.1f, bedDepth + 0.1f);
            gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_FILL);
            gl.glPopAttrib();
        }

        // Draw Bed Frame
        setColor(gl, f.getPartColor("frame"));
        drawRoundedCube(gl, bedX, legHeight, bedZ,
                bedWidth, frameThickness, bedDepth,
                frameThickness * 0.5f, slices);

        // Draw Headboard
        setColor(gl, f.getPartColor("headboard"));
        drawRoundedCube(gl, bedX, legHeight, bedZ - frameThickness * 0.5f,
                bedWidth, headboardHeight, frameThickness * 1.5f,
                frameThickness * 0.7f, slices);

        // Draw Mattress
        setColor(gl, f.getPartColor("mattress"));
        drawRoundedCube(gl, bedX + frameThickness * 0.5f,
                legHeight + frameThickness,
                bedZ + frameThickness * 0.5f,
                bedWidth - frameThickness,
                mattressHeight,
                bedDepth * 1.05f - frameThickness,
                frameThickness * 0.8f, slices);

        // Draw Sheet
        setColor(gl, f.getPartColor("sheet"));
        drawRoundedCube(gl, bedX + frameThickness * 0.5f,
                legHeight + frameThickness + mattressHeight * 0.7f,
                bedZ + frameThickness * 0.5f,
                bedWidth - frameThickness,
                mattressHeight * 0.3f,
                bedDepth - frameThickness,
                frameThickness * 0.8f, slices);

        // Draw Pillows
        setColor(gl, f.getPartColor("pillows"));
        drawRoundedCube(gl, bedX + (bedWidth - pillowWidth) / 2,
                legHeight + frameThickness + mattressHeight,
                bedZ + frameThickness * 2f,
                pillowWidth,
                pillowHeight,
                pillowDepth,
                pillowHeight * 0.5f, slices);
        drawRoundedCube(gl, bedX + (bedWidth - pillowWidth) / 2,
                legHeight + frameThickness + mattressHeight,
                bedZ + frameThickness * 2f + pillowDepth * 0.8f,
                pillowWidth,
                pillowHeight,
                pillowDepth,
                pillowHeight * 0.5f, slices);

        // Draw Legs
        setColor(gl, f.getPartColor("legs"));
        float legThickness = frameThickness * 1.2f;
        drawCylinder(gl, bedX + legThickness, 0, bedZ + legThickness,
                legThickness * 0.5f, legHeight, slices);
        drawCylinder(gl, bedX + bedWidth - legThickness, 0, bedZ + legThickness,
                legThickness * 0.5f, legHeight, slices);
        drawCylinder(gl, bedX + legThickness, 0, bedZ + bedDepth - legThickness,
                legThickness * 0.5f, legHeight, slices);
        drawCylinder(gl, bedX + bedWidth - legThickness, 0, bedZ + bedDepth - legThickness,
                legThickness * 0.5f, legHeight, slices);
    }

    // 3D drawing for sofa
    private void drawSofa3D(GL2 gl, Furniture f) {
        float x = (float) f.getX();
        float z = (float) f.getZ();
        float w = (float) f.getWidth();
        float d = (float) f.getDepth() * 0.3f;
        float h = (float) f.getHeight();

        float legH = h * 0.15f;
        float seatH = h * 0.12f;
        float cushionH = h * 0.12f;
        float backH = h * 0.45f;
        float armW = w * 0.08f;
        int slices = 16;

        if (f.isSelected()) {
            gl.glPushAttrib(GL2.GL_CURRENT_BIT);
            gl.glColor3f(1.0f, 1.0f, 0.0f);
            gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_LINE);
            drawBox(gl, x - 0.05f, 0, z - 0.05f,
                    w + 0.1f, h + 0.1f, d + 0.1f);
            gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_FILL);
            gl.glPopAttrib();
        }

        // Base Seat Platform
        setColor(gl, f.getPartColor("base"));
        drawRoundedCube(gl, x, legH, z, w, seatH, d, 0.03f, slices);

        // Seat Cushions
        float gap = w * 0.015f;
        float cWidth = (w - 4 * gap) / 3;
        setColor(gl, f.getPartColor("cushions"));
        for (int i = 0; i < 3; i++) {
            drawRoundedCube(gl, x + gap * (i + 1) + cWidth * i, legH + seatH, z + d * 0.05f,
                    cWidth, cushionH, d * 0.9f, 0.04f, slices);
        }

        // Backrest
        setColor(gl, f.getPartColor("backrest"));
        drawRoundedCube(gl, x + armW * 0.5f, legH + seatH + cushionH, z + d * 0.01f,
                w - armW, backH, d * 0.12f, 0.04f, slices);

        // Armrests
        setColor(gl, f.getPartColor("arms"));
        drawRoundedCube(gl, x, legH + seatH, z, armW, backH, d, 0.05f, slices);
        drawRoundedCube(gl, x + w - armW, legH + seatH, z, armW, backH, d, 0.05f, slices);

        // Legs
        setColor(gl, f.getPartColor("legs"));
        float legR = w * 0.015f;
        drawCylinder(gl, x + legR, 0, z + legR, legR, legH, slices);
        drawCylinder(gl, x + w - legR * 2, 0, z + legR, legR, legH, slices);
        drawCylinder(gl, x + legR, 0, z + d - legR * 2, legR, legH, slices);
        drawCylinder(gl, x + w - legR * 2, 0, z + d - legR * 2, legR, legH, slices);
    }

    private void drawRoundedCube(GL2 gl, float x, float y, float z,
        float width, float height, float depth, float radius, int slices) {
        // Main center
        drawBox(gl, x + radius, y, z + radius,
                width - 2 * radius, height, depth - 2 * radius);
        
        // Sides
        drawBox(gl, x + radius, y, z, width - 2 * radius, height, radius);
        drawBox(gl, x + radius, y, z + depth - radius, width - 2 * radius, height, radius);
        drawBox(gl, x, y, z + radius, radius, height, depth - 2 * radius);
        drawBox(gl, x + width - radius, y, z + radius, radius, height, depth - 2 * radius);
        
        // Corners
        for (int i = 0; i < 4; i++) {
            float cx = (i % 2 == 0) ? x + radius : x + width - radius;
            float cz = (i < 2) ? z + radius : z + depth - radius;
            drawCylinder(gl, cx, y, cz, radius, height, slices);
        }
    }

    private void drawCylinder(GL2 gl, float x, float y, float z,
        float radius, float height, int slices) {
        gl.glPushMatrix();
        gl.glTranslatef(x, y + height/2, z);

        gl.glBegin(GL2.GL_QUAD_STRIP);
        for (int i = 0; i <= slices; i++) {
            float angle = (float) (2.0 * Math.PI * i / slices);
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);

            gl.glNormal3f(cos, 0, sin);
            gl.glVertex3f(radius * cos, height/2, radius * sin);
            gl.glVertex3f(radius * cos, -height/2, radius * sin);
        }
        gl.glEnd();

        // Top and bottom caps
        gl.glBegin(GL2.GL_TRIANGLE_FAN);
        gl.glNormal3f(0, 1, 0);
        gl.glVertex3f(0, height/2, 0);
        for (int i = 0; i <= slices; i++) {
            float angle = (float) (2.0 * Math.PI * i / slices);
            gl.glVertex3f(radius * (float) Math.cos(angle), height/2, radius * (float) Math.sin(angle));
        }
        gl.glEnd();

        gl.glBegin(GL2.GL_TRIANGLE_FAN);
        gl.glNormal3f(0, -1, 0);
        gl.glVertex3f(0, -height/2, 0);
        for (int i = slices; i >= 0; i--) {
            float angle = (float) (2.0 * Math.PI * i / slices);
            gl.glVertex3f(radius * (float) Math.cos(angle), -height/2, radius * (float) Math.sin(angle));
        }
        gl.glEnd();

        gl.glPopMatrix();
    }

    
    private void drawBox(GL2 gl, float x, float y, float z, float width, float height, float depth) {
        gl.glBegin(GL2.GL_QUADS);
        gl.glVertex3f(x, y, z + depth);
        gl.glVertex3f(x + width, y, z + depth);
        gl.glVertex3f(x + width, y + height, z + depth);
        gl.glVertex3f(x, y + height, z + depth);
        gl.glVertex3f(x, y, z);
        gl.glVertex3f(x + width, y, z);
        gl.glVertex3f(x + width, y + height, z);
        gl.glVertex3f(x, y + height, z);
        gl.glVertex3f(x, y, z);
        gl.glVertex3f(x, y, z + depth);
        gl.glVertex3f(x, y + height, z + depth);
        gl.glVertex3f(x, y + height, z);
        gl.glVertex3f(x + width, y, z);
        gl.glVertex3f(x + width, y, z + depth);
        gl.glVertex3f(x + width, y + height, z + depth);
        gl.glVertex3f(x + width, y + height, z);
        gl.glVertex3f(x, y + height, z);
        gl.glVertex3f(x + width, y + height, z);
        gl.glVertex3f(x + width, y + height, z + depth);
        gl.glVertex3f(x, y + height, z + depth);
        gl.glVertex3f(x, y, z);
        gl.glVertex3f(x + width, y, z);
        gl.glVertex3f(x + width, y, z + depth);
        gl.glVertex3f(x, y, z + depth);
        gl.glEnd();
    }

    private void drawRect(GL2 gl, float x, float y, float z, float width, float height, float depth) {
        gl.glBegin(GL2.GL_QUADS);
        gl.glVertex3f(x, y, z);
        gl.glVertex3f(x + width, y, z);
        gl.glVertex3f(x + width, y + height, z);
        gl.glVertex3f(x, y + height, z);
        gl.glEnd();
    }

    private void setColor(GL2 gl, Color c) {
        gl.glColor3f(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f);
    }

    private void drawFurnitureForPicking(GL2 gl, Furniture f, int baseId) {
        float x = (float) f.getX();
        float z = (float) f.getZ();
        float w = (float) f.getWidth();
        float d = (float) f.getDepth();
        float h = (float) f.getHeight();
        String subtype = f.getSubtype();

        if (f.getType().equals("Chair")) {
            float seatHeight = h * 0.5f;
            float seatThickness = h * 0.05f;
            float legThickness = w * 0.08f;
            float legHeight = seatHeight;

            switch (subtype) {
                case "Standard":
                    // Seat
                    setPickColor(gl, baseId);
                    partPickMap.put(baseId, "seat");
                    drawBox(gl, x, seatHeight, z, w, seatThickness, d);
                    // Backrest
                    setPickColor(gl, baseId + 1);
                    partPickMap.put(baseId + 1, "backrest");
                    float backHeight = h * 0.7f;
                    float backThickness = w * 0.05f;
                    drawBox(gl, x, seatHeight + seatThickness, z, w, backHeight, backThickness);
                    // Legs
                    setPickColor(gl, baseId + 2);
                    partPickMap.put(baseId + 2, "legs");
                    drawBox(gl, x, 0, z, legThickness, legHeight, legThickness);
                    drawBox(gl, x + w - legThickness, 0, z, legThickness, legHeight, legThickness);
                    drawBox(gl, x, 0, z + d - legThickness, legThickness, legHeight, legThickness);
                    drawBox(gl, x + w - legThickness, 0, z + d - legThickness, legThickness, legHeight, legThickness);
                    break;

                case "Armchair":
                    // Seat
                    setPickColor(gl, baseId);
                    partPickMap.put(baseId, "seat");
                    float radius = w * 0.05f;
                    drawRoundedCube(gl, x + w * 0.1f, seatHeight, z + d * 0.1f,
                            w * 0.8f, seatThickness * 1.5f, d * 0.8f, radius, 16);
                    // Backrest
                    setPickColor(gl, baseId + 1);
                    partPickMap.put(baseId + 1, "backrest");
                    drawRoundedCube(gl, x + w * 0.1f, seatHeight + seatThickness * 1.5f, z,
                            w * 0.8f, h * 0.8f, d * 0.15f, radius, 16);
                    // Armrests
                    setPickColor(gl, baseId + 2);
                    partPickMap.put(baseId + 2, "arms");
                    drawRoundedCube(gl, x, seatHeight, z,
                            w * 0.1f, h * 0.4f, d, radius, 16);
                    drawRoundedCube(gl, x + w * 0.9f, seatHeight, z,
                            w * 0.1f, h * 0.4f, d, radius, 16);
                    // Legs
                    setPickColor(gl, baseId + 3);
                    partPickMap.put(baseId + 3, "legs");
                    float adjustedLegHeight = seatHeight + seatThickness * 1.5f;
                    drawBox(gl, x, 0, z,
                            legThickness, adjustedLegHeight, legThickness);
                    drawBox(gl, x + w - legThickness, 0, z,
                            legThickness, adjustedLegHeight, legThickness);
                    drawBox(gl, x, 0, z + d - legThickness,
                            legThickness, adjustedLegHeight, legThickness);
                    drawBox(gl, x + w - legThickness, 0, z + d - legThickness,
                            legThickness, adjustedLegHeight, legThickness);
                    break;

                case "Dining":
                    // Seat
                    setPickColor(gl, baseId);
                    partPickMap.put(baseId, "seat");
                    drawRoundedCube(gl, x, seatHeight, z,
                            w, seatThickness * 1.5f, d, w * 0.03f, 16);
                    // Backrest
                    setPickColor(gl, baseId + 1);
                    partPickMap.put(baseId + 1, "backrest");
                    float slatWidth = w * 0.25f;
                    float slatSpacing = w * 0.05f;
                    float slatHeight = h * 0.9f;
                    float slatDepth = d * 0.05f;
                    for (int i = 0; i < 3; i++) {
                        float slatX = x + w * 0.1f + i * (slatWidth + slatSpacing);
                        drawBox(gl, slatX, seatHeight + seatThickness * 1.5f, z,
                                slatWidth, slatHeight, slatDepth);
                    }
                    // Legs
                    setPickColor(gl, baseId + 2);
                    partPickMap.put(baseId + 2, "legs");
                    float legRadius = legThickness * 0.35f;
                    float diningLegHeight = seatHeight;
                    drawCylinder(gl, x + legRadius, 0, z + legRadius,
                            legRadius, diningLegHeight, 16);
                    drawCylinder(gl, x + w - legRadius, 0, z + legRadius,
                            legRadius, diningLegHeight, 16);
                    drawCylinder(gl, x + legRadius, 0, z + d - legRadius,
                            legRadius, diningLegHeight, 16);
                    drawCylinder(gl, x + w - legRadius, 0, z + d - legRadius,
                            legRadius, diningLegHeight, 16);
                    break;

                default:
                    // Fallback
                    setPickColor(gl, baseId);
                    partPickMap.put(baseId, "seat");
                    drawBox(gl, x, seatHeight, z, w, seatThickness, d);
                    setPickColor(gl, baseId + 1);
                    partPickMap.put(baseId + 1, "backrest");
                    drawBox(gl, x, seatHeight + seatThickness, z, w, h * 0.7f,
                            w * 0.05f);
                    setPickColor(gl, baseId + 2);
                    partPickMap.put(baseId + 2, "legs");
                    drawBox(gl, x, 0, z, legThickness, legHeight, legThickness);
                    drawBox(gl, x + w - legThickness, 0, z, legThickness, legHeight, legThickness);
                    drawBox(gl, x, 0, z + d - legThickness, legThickness, legHeight, legThickness);
                    drawBox(gl, x + w - legThickness, 0, z + d - legThickness, legThickness, legHeight, legThickness);
            }
        } else if (f.getType().equals("Table")) {
            float topThickness = h * 0.05f;
            float legThickness = w * 0.08f;
            float legHeight = h - topThickness;

            setPickColor(gl, baseId);
            partPickMap.put(baseId, "top");
            drawBox(gl, x, legHeight, z, w, topThickness, d);

            setPickColor(gl, baseId + 1);
            partPickMap.put(baseId + 1, "legs");
            drawBox(gl, x, 0, z, legThickness, legHeight, legThickness);
            drawBox(gl, x + w - legThickness, 0, z, legThickness, legHeight, legThickness);
            drawBox(gl, x, 0, z + d - legThickness, legThickness, legHeight, legThickness);
            drawBox(gl, x + w - legThickness, 0, z + d - legThickness, legThickness, legHeight, legThickness);
        } else if (f.getType().equals("Sofa")) {
            float legH = h * 0.15f;
            float seatH = h * 0.12f;
            float cushionH = h * 0.12f;
            float backH = h * 0.45f;
            float armW = w * 0.08f;
            int slices = 16;

            setPickColor(gl, baseId);
            partPickMap.put(baseId, "base");
            drawRoundedCube(gl, x, legH, z, w, seatH, d, 0.03f, slices);

            setPickColor(gl, baseId + 1);
            partPickMap.put(baseId + 1, "cushions");
            float gap = w * 0.015f;
            float cWidth = (w - 4 * gap) / 3;
            for (int i = 0; i < 3; i++) {
                drawRoundedCube(gl, x + gap * (i + 1) + cWidth * i, legH + seatH, z + d * 0.05f,
                        cWidth, cushionH, d * 0.9f, 0.04f, slices);
            }

            setPickColor(gl, baseId + 2);
            partPickMap.put(baseId + 2, "backrest");
            drawRoundedCube(gl, x + armW * 0.5f, legH + seatH + cushionH, z + d * 0.01f,
                    w - armW, backH, d * 0.12f, 0.04f, slices);

            setPickColor(gl, baseId + 3);
            partPickMap.put(baseId + 3, "arms");
            drawRoundedCube(gl, x, legH + seatH, z, armW, backH, d, 0.05f, slices);
            drawRoundedCube(gl, x + w - armW, legH + seatH, z, armW, backH, d, 0.05f, slices);

            setPickColor(gl, baseId + 4);
            partPickMap.put(baseId + 4, "legs");
            float legR = w * 0.015f;
            drawCylinder(gl, x + legR, 0, z + legR, legR, legH, slices);
            drawCylinder(gl, x + w - legR * 2, 0, z + legR, legR, legH, slices);
            drawCylinder(gl, x + legR, 0, z + d - legR * 2, legR, legH, slices);
            drawCylinder(gl, x + w - legR * 2, 0, z + d - legR * 2, legR, legH, slices);
        } else if (f.getType().equals("Bed")) {
            float frameThickness = 0.15f;
            float mattressHeight = h * 0.25f;
            float headboardHeight = h * 0.7f;
            float legHeight = h * 0.1f;
            float pillowWidth = w * 0.4f;
            float pillowDepth = d * 0.15f;
            float pillowHeight = mattressHeight * 0.3f;
            int slices = 8;

            setPickColor(gl, baseId);
            partPickMap.put(baseId, "frame");
            drawRoundedCube(gl, x, legHeight, z,
                    w, frameThickness, d,
                    frameThickness * 0.5f, slices);

            setPickColor(gl, baseId + 1);
            partPickMap.put(baseId + 1, "headboard");
            drawRoundedCube(gl, x, legHeight, z - frameThickness * 0.5f,
                    w, headboardHeight, frameThickness * 1.5f,
                    frameThickness * 0.7f, slices);

            setPickColor(gl, baseId + 2);
            partPickMap.put(baseId + 2, "mattress");
            drawRoundedCube(gl, x + frameThickness * 0.5f,
                    legHeight + frameThickness,
                    z + frameThickness * 0.5f,
                    w - frameThickness,
                    mattressHeight,
                    d * 1.05f - frameThickness,
                    frameThickness * 0.8f, slices);

            setPickColor(gl, baseId + 3);
            partPickMap.put(baseId + 3, "sheet");
            drawRoundedCube(gl, x + frameThickness * 0.5f,
                    legHeight + frameThickness + mattressHeight * 0.7f,
                    z + frameThickness * 0.5f,
                    w - frameThickness,
                    mattressHeight * 0.3f,
                    d - frameThickness,
                    frameThickness * 0.8f, slices);

            setPickColor(gl, baseId + 4);
            partPickMap.put(baseId + 4, "pillows");
            drawRoundedCube(gl, x + (w - pillowWidth) / 2,
                    legHeight + frameThickness + mattressHeight,
                    z + frameThickness * 2f,
                    pillowWidth,
                    pillowHeight,
                    pillowDepth,
                    pillowHeight * 0.5f, slices);
            drawRoundedCube(gl, x + (w - pillowWidth) / 2,
                    legHeight + frameThickness + mattressHeight,
                    z + frameThickness * 2f + pillowDepth * 0.8f,
                    pillowWidth,
                    pillowHeight,
                    pillowDepth,
                    pillowHeight * 0.5f, slices);

            setPickColor(gl, baseId + 5);
            partPickMap.put(baseId + 5, "legs");
            float legThickness = frameThickness * 1.2f;
            drawCylinder(gl, x + legThickness, 0, z + legThickness,
                    legThickness * 0.5f, legHeight, slices);
            drawCylinder(gl, x + w - legThickness, 0, z + legThickness,
                    legThickness * 0.5f, legHeight, slices);
            drawCylinder(gl, x + legThickness, 0, z + d - legThickness,
                    legThickness * 0.5f, legHeight, slices);
            drawCylinder(gl, x + w - legThickness, 0, z + d - legThickness,
                    legThickness * 0.5f, legHeight, slices);
        } else {
            setPickColor(gl, baseId);
            partPickMap.put(baseId, "body");
            drawBox(gl, x, 0, z, w, h, d);
        }
    }

    private void setPickColor(GL2 gl, int id) {
        gl.glColor3ub((byte) (id & 0xff), (byte) 0, (byte) 0);
    }

    private void setupMouseListeners() {
        setFocusable(true);
        requestFocusInWindow();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastMouseX = e.getX();
                lastMouseY = e.getY();
                
                if (!hasFocus()) {
                    requestFocusInWindow();
                    System.out.println("Regained focus on mouse press");
                }
                
                if (design != null) {
                    if (is3DView) {
                        // Handle 3D picking
                        pickFurnitureAt(e.getX(), e.getY());
                    } else {
                        // 2D selection logic
                        Room room = design.getRoom();
                        float scale = Math.min(getWidth() / (float) room.getLength(),
                                getHeight() / (float) room.getWidth()) * 0.8f;

                        System.out.println("Mouse pressed at (" + e.getX() + ", " + e.getY() + "), Scale: " + scale);
                        draggedFurniture = null;

                        float panelCenterX = getWidth() / 2f;
                        float panelCenterY = getHeight() / 2f;
                        float roomCenterX = (float) (room.getLength() / 2) * scale;
                        float roomCenterY = (float) (room.getWidth() / 2) * scale;

                        for (Furniture f : design.getFurnitureList()) {
                            float x = (float) f.getX() * scale - roomCenterX + panelCenterX;
                            float z = (float) f.getZ() * scale - roomCenterY + panelCenterY;
                            float w = (float) f.getWidth() * scale;
                            float d = (float) f.getDepth() * scale;

                            float adjustedD = f.getType().equals("Sofa") ? d * 0.6f : d;

                            if (e.getX() >= x && e.getX() <= x + w &&
                                    e.getY() >= z && e.getY() <= z + adjustedD) {
                                draggedFurniture = f;
                                dragOffset = new Point(e.getX(), e.getY());
                                parent.setSelectedFurniture(f);
                                f.setSelected(true);
                                for (Furniture other : design.getFurnitureList()) {
                                    if (other != f)
                                        other.setSelected(false);
                                }
                                System.out.println(
                                        "Selected " + f.getType() + " at (" + f.getX() + ", " + f.getZ() + ")");
                                break;
                            }
                        }

                        if (draggedFurniture == null) {
                            System.out.println("No furniture selected at (" + e.getX() + ", " + e.getY() + ")");
                            parent.setSelectedFurniture(null);
                            for (Furniture f : design.getFurnitureList()) {
                                f.setSelected(false);
                            }
                            // Initialize selection rectangle
                            selectionStart = new Point(e.getX(), e.getY());
                            selectionEnd = selectionStart;
                        } else {
                            System.out.println(
                                    "Drag offset initialized to: (" + dragOffset.x + ", " + dragOffset.y + ")");
                        }
                    }
                }
                repaint();
            }


            @Override
            public void mouseReleased(MouseEvent e) {
                if (!is3DView) {
                    if (selectionStart != null) {
                        // Handle selection completion
                        if (selectionStart.equals(selectionEnd)) {
                            // Click without drag - clear selection
                            parent.setSelectedFurniture(null);
                            for (Furniture f : design.getFurnitureList()) {
                                f.setSelected(false);
                            }
                        }
                        selectionStart = null;
                        selectionEnd = null;
                    }
                    draggedFurniture = null;
                }
                repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (!hasFocus()) {
                    requestFocusInWindow();
                    System.out.println("Regained focus on click");
                }
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (design != null && !is3DView) {
                    float scale = Math.min(getWidth() / (float) design.getRoom().getLength(),
                            getHeight() / (float) design.getRoom().getWidth()) * 0.8f;
                    boolean overFurniture = false;
                    for (Furniture f : design.getFurnitureList()) {
                        float x = (float) (f.getX() - design.getRoom().getLength() / 2) * scale;
                        float z = (float) (f.getZ() - design.getRoom().getWidth() / 2) * scale;
                        float w = (float) f.getWidth() * scale;
                        float d = (float) f.getDepth() * scale;

                        float panelCenterX = getWidth() / 2f;
                        float panelCenterY = getHeight() / 2f;
                        x += panelCenterX;
                        z += panelCenterY;

                        float adjustedD = f.getType().equals("Sofa") ? d : d;

                        if (e.getX() >= x && e.getX() <= x + w && e.getY() >= z && e.getY() <= z + adjustedD) {
                            overFurniture = true;
                            break;
                        }
                    }
                    setCursor(
                            overFurniture ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
                }
            }

            // @Override
            // public void mouseDragged(MouseEvent e) {
            //     if (is3DView && draggedFurniture == null) {
            //         double deltaX = e.getX() - lastMouseX;
            //         double deltaY = e.getY() - lastMouseY;
            //         rotationY += deltaX * 0.5f;
            //         rotationX += deltaY * 0.5f;
            //         repaint();
            //     } else if (!is3DView && draggedFurniture != null) {
            //         Room room = design.getRoom();
            //         float scale = Math.min(getWidth() / (float) room.getLength(),
            //                 getHeight() / (float) room.getWidth()) * 0.8f;

            //         double deltaX = e.getX() - dragOffset.x;
            //         double deltaY = e.getY() - dragOffset.y;
            //         double newX = draggedFurniture.getX() + deltaX / scale;
            //         double newZ = draggedFurniture.getZ() + deltaY / scale;

            //         newX = Math.max(0, Math.min(newX, room.getLength() - draggedFurniture.getWidth()));
            //         newZ = Math.max(0, Math.min(newZ, room.getWidth() - draggedFurniture.getDepth()));

            //         draggedFurniture.setX(newX);
            //         draggedFurniture.setZ(newZ);
            //         parent.propertiesPanel.update(draggedFurniture);
            //         System.out.println("Dragging " + draggedFurniture.getType() + " to (" + newX + ", " + newZ + ")");

            //         dragOffset = new Point(e.getX(), e.getY());
            //         repaint();
            //     }
            //     lastMouseX = e.getX();
            //     lastMouseY = e.getY();
            // }
        

            @Override
            public void mouseDragged(MouseEvent e) {
                if (is3DView && draggedFurniture == null) {
                    // 3D view rotation
                    double deltaX = e.getX() - lastMouseX;
                    double deltaY = e.getY() - lastMouseY;
                    rotationY += deltaX * 0.5f;
                    rotationX += deltaY * 0.5f;
                } 
                else if (!is3DView && draggedFurniture != null && design != null) {
                    Room room = design.getRoom();
                    float scale = Math.min(getWidth() / (float) room.getLength(),
                                        getHeight() / (float) room.getWidth()) * 0.8f;

                    double deltaX = e.getX() - dragOffset.getX();
                    double deltaY = dragOffset.getY() - e.getY();  // Inverted Y movement

                    double newX = draggedFurniture.getX() + deltaX / scale;
                    double newZ = draggedFurniture.getZ() + deltaY / scale;

                    // Adjust boundaries based on orientation
                    double effectiveWidth = (draggedFurniture.getOrientation() == Furniture.Orientation.EAST || 
                                        draggedFurniture.getOrientation() == Furniture.Orientation.WEST) ? 
                                        draggedFurniture.getDepth() : draggedFurniture.getWidth();
                    
                    double effectiveDepth = (draggedFurniture.getOrientation() == Furniture.Orientation.EAST || 
                                        draggedFurniture.getOrientation() == Furniture.Orientation.WEST) ? 
                                        draggedFurniture.getWidth() : draggedFurniture.getDepth();

                    newX = Math.max(0, Math.min(newX, room.getLength() - effectiveWidth));
                    newZ = Math.max(0, Math.min(newZ, room.getWidth() - effectiveDepth));

                    draggedFurniture.setX(newX);
                    draggedFurniture.setZ(newZ);
                    
                    parent.propertiesPanel.update(draggedFurniture);
                    dragOffset = new Point(e.getX(), e.getY());
                
                }
                
                // Update last mouse position
                lastMouseX = e.getX();
                lastMouseY = e.getY();
                repaint();
            }       
        
        
        
        
        });

        addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                System.out.println("VisualizationPanel gained focus");
            }

            @Override
            public void focusLost(FocusEvent e) {
                System.out.println("VisualizationPanel lost focus");
            }
        });
    }

    private void pickFurnitureAt(int x, int y) {
        GL2 gl = getGL().getGL2();

        // Initialize picking maps
        pickMap = new HashMap<>();
        partPickMap = new HashMap<>();
        int pickingId = 1;

        // Set up selection buffer
        IntBuffer selectBuffer = IntBuffer.allocate(512);
        gl.glSelectBuffer(512, selectBuffer);

        // Enter selection mode
        gl.glRenderMode(GL2.GL_SELECT);

        // Initialize name stack
        gl.glInitNames();
        gl.glPushName(0);

        // Set up picking matrix
        IntBuffer viewport = IntBuffer.allocate(4);
        gl.glGetIntegerv(GL2.GL_VIEWPORT, viewport);
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        glu.gluPickMatrix(x, getHeight() - y, 5.0, 5.0, viewport);
        float aspect = (float) getWidth() / getHeight();
        gl.glFrustum(-aspect, aspect, -1.0, 1.0, 5.0, 60.0);

        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
        gl.glTranslatef(0.0f, 0.0f, -10.0f * zoomFactor);
        gl.glRotatef(rotationX, 1.0f, 0.0f, 0.0f);
        gl.glRotatef(rotationY, 0.0f, 1.0f, 0.0f);

        // Draw furniture for picking
        if (design != null) {
            for (Furniture f : design.getFurnitureList()) {
                pickMap.put(pickingId, f);
                gl.glLoadName(pickingId);
                drawFurnitureForPicking(gl, f, pickingId);
                pickingId += 5;
            }
        }

        // Restore matrices
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glPopMatrix();
        gl.glMatrixMode(GL2.GL_MODELVIEW);

        // Exit selection mode and get hits
        int hits = gl.glRenderMode(GL2.GL_RENDER);
        System.out.println("Picking hits: " + hits + " at (" + x + ", " + y + ")");

        // Process hits
        int selectedId = 0;
        float minZ = Float.MAX_VALUE;
        int bufferIndex = 0;

        for (int i = 0; i < hits; i++) {
            int nameCount = selectBuffer.get(bufferIndex);
            float z1 = (float) selectBuffer.get(bufferIndex + 1) / 0x7fffffff;
            bufferIndex += 3;
            for (int j = 0; j < nameCount; j++) {
                int id = selectBuffer.get(bufferIndex + j);
                if (z1 < minZ) {
                    minZ = z1;
                    selectedId = id;
                }
            }
            bufferIndex += nameCount;
        }

        // Select furniture and part
        selectedFurniture = pickMap.get(selectedId);
        selectedPart = partPickMap.get(selectedId);

        if (selectedFurniture != null) {
            System.out.println("Selected furniture: " + selectedFurniture.getType() + ", part: " + selectedPart);
            for (Furniture f : design.getFurnitureList()) {
                f.setSelected(f == selectedFurniture);
            }
            parent.setSelectedFurniture(selectedFurniture);
            parent.propertiesPanel.setSelectedPart(selectedPart);
        } else {
            System.out.println("No furniture selected at (" + x + ", " + y + ")");
            for (Furniture f : design.getFurnitureList()) {
                f.setSelected(false);
            }
            parent.setSelectedFurniture(null);
            parent.propertiesPanel.setSelectedPart(null);
        }

        gl.glFlush();
        repaint();
    }

    public void setDesign(Design design) {
        this.design = design;
        rotationX = 0;
        rotationY = 0;
        repaint();
    }

    public void set3DView(boolean is3DView) {
        this.is3DView = is3DView;
        System.out.println("View mode set to: " + (is3DView ? "3D" : "2D"));
        repaint();
    }

    public boolean get3DView() {
        return this.is3DView;
    }


    public void zoomIn() {
        zoomFactor *= 1.1f;
        repaint();
    }

    public void zoomOut() {
        zoomFactor /= 1.1f;
        repaint();
    }

    public void resetView() {
        zoomFactor = 1.0f;
        rotationX = 0;
        rotationY = 0;
        repaint();
    }
}