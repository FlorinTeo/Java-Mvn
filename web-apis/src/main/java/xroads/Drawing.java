package xroads;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * Encapsulates a representation of a generic image file. A Drawing object 
 * can be created only by providing a valid image file as argument to its constructor. 
 * In return, the object can be used for accessing and modifying the image at pixel level.
 */
public class Drawing {
    
    protected BufferedImage _image = null;
    
    /**
     * Creates an instance of a Drawing object encapsulating the representation of 
     * the imageFile given as argument.
     */
    public static Drawing read(String imageFile) throws IOException {
        File drwFile = new File(imageFile);
        if (!drwFile.exists() || drwFile.isDirectory()) {
            throw new IOException();
        }
        return new Drawing(ImageIO.read(drwFile));
    }
    
    public Drawing(BufferedImage image) {
        _image = image;
    }
    
    public BufferedImage getImage() {
        return _image;
    }

    /**
     * Gets the width of the drawing image.
     * @return the width of the drawing image in pixels.
     */
    public int getWidth() {
        return _image.getWidth();
    }
    
    /**
     * Gets the height of the drawing image.
     * @return the height of the drawing image in pixels.
     */
    public int getHeight() {
        return _image.getHeight();
    }
    
    /**
     * Gets the color of the pixel at the given x and y coordinates.
     * @param x - x coordinate value.
     * @param y - y coordinate value.
     * @return the Color value at the given coordinates.
     */
    public Color getPixel(int x, int y) {
        return new Color(_image.getRGB(x, y));
    }
    
    /**
     * Sets the pixel of the given x and y coordinates to the given color. 
     * @param x - x coordinate value.
     * @param y - y coordinate value.
     * @param c - the Color value to be set at the given coordinates.
     */
    public void setPixel(int x, int y, Color c) {
        _image.setRGB(x, y, c.getRGB());
    }
}
