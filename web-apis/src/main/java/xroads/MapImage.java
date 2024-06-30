package xroads;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.imageio.ImageIO;

import com.google.gson.Gson;

/**
 * Class representing a map image enhanced with intersection routes.
 * @see #load(String)
 * @see #getMapName()
 * @see #getRoutes()
 * @see #setOverlays(String...)
 * @see #collide(String...)
 */
public class MapImage extends Drawing {
    // File name for the base map image (i.e. "Ravenna")
    private String _mapName;
    // Map<overlay_name, overlay_image> (i.e. {<"AB", imageAB>, <"AC", imageAC>, ..})
    private HashMap<String, BufferedImage> _mapOverlays;
    // Routes to be overlaid on the map
    private Set<String> _overlays = new HashSet<String>();

    // Top-Left and Bottom-Right pixel coordinates for the intersection area of the image
    private Point _centerTL = null;
    private Point _centerBR = null;

    // Region: [private] File IO
    /**
     * Private class definition used for serialization/deserialization of the
     * MapImage object to/from an enhanced .jpeg file.
     */
    private class MapMetadata {
        private String _mapName = "";
        private HashMap<String, String> _mapOverlaysRaw = new HashMap<String, String>();
        private Point _centerTL;
        private Point _centerBR;
    };
    
    private static String imageToBase64(BufferedImage image) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return Base64.getEncoder().encodeToString(out.toByteArray());
    }
    
    private static BufferedImage base64ToImage(String base64) throws IOException {
        byte[] imgBytes = Base64.getDecoder().decode(base64);
        InputStream imgStream = new ByteArrayInputStream(imgBytes);
        BufferedImage image = ImageIO.read(imgStream);
        return image;
    }
    
    private static byte[] toByteArray(BigInteger big, int minLength) {
        byte[] base=big.toByteArray();
        byte[] returnArray=new byte[Math.max(base.length, minLength)];
        if ((base[0]&128)!=0) {
            Arrays.fill(returnArray, (byte) 0xFF);
        }
        System.arraycopy(base,0,returnArray,returnArray.length-base.length,base.length);
        return returnArray;
    }
    
    private static MapImage loadFromFile(File file) throws IOException {
        Path filePath = Paths.get(file.getAbsolutePath());
        byte[] rawBytes = Files.readAllBytes(filePath);
        byte[] rawOffset = Arrays.copyOfRange(rawBytes, rawBytes.length-4, rawBytes.length);
        BigInteger offset = new BigInteger(rawOffset);
        byte[] rawJsonBytes = Arrays.copyOfRange(rawBytes,  offset.intValue(), rawBytes.length - 4);
        String rawjson = new String(rawJsonBytes);
        Gson deserializer = new Gson();
        MapMetadata mapMetadata = deserializer.fromJson(rawjson, MapMetadata.class);
        byte[] rawImageBytes = Arrays.copyOfRange(rawBytes, 0, offset.intValue());
        InputStream imageStream = new ByteArrayInputStream(rawImageBytes);
        BufferedImage image = ImageIO.read(imageStream);
        
        MapImage mapImage = new MapImage(mapMetadata._mapName, image);
        mapImage._centerTL = mapMetadata._centerTL;
        mapImage._centerBR = mapMetadata._centerBR;

        for(Map.Entry<String, String> mapOverlayRaw : mapMetadata._mapOverlaysRaw.entrySet())
        {
            mapImage._mapOverlays.put(
                    mapOverlayRaw.getKey(), 
                    base64ToImage(mapOverlayRaw.getValue()));
        }
        
        return mapImage;
    }
    
    private static MapImage loadFromDir(File dir) throws IOException {
        // Load the baseMap and create the mapImage
        String mapName = dir.getName();
        File mapFile = new File(dir.getName() + "/" + mapName + "_.jpg");
        MapImage mapImage = new MapImage(mapName, ImageIO.read(mapFile));
        
        // Load the overlays into the mapImage
        FilenameFilter overlayFilter = (file, name)-> { return name.matches(dir + "_.+\\.png"); };
        for (String overlayFileName : dir.list(overlayFilter)) {
            File overlayFile = new File(dir.getName() + "/" + overlayFileName);
            String overlayName = overlayFileName.split("_|\\.")[1];
            mapImage._mapOverlays.put(
                    overlayName,
                    ImageIO.read(overlayFile));
        }
        
        // return the newly created and loaded mapImage
        return mapImage;
    }
    // EndRegion: [private] File IO

    // Region: [public] File IO
    /**
     * Loads the content of a folder or a file into a new MapImage object.<p>
     * If mapImagePath points to a folder, the folder is expected to contain
     * the base map as a one-part name (i.e. "Ravenna_.jpg") and a set of overlay
     * file names as a two-parts names (i.e. "Ravenna_AB.png").<p>
     * If mapImagePath points to a file, the file is expected to be an enhanced .jpg
     * image, embedding route overlays.
     * @param mapImagePath - path to an existing folder or an enhanced .jpg file<br>
     * e.g.: "C:/MyFolder/Ravenna" or "C:/MyFolder/Ravenna/Ravenna.jpg"<br>
     * @returns a new MapImage object containing the map.
     * @throws IOException - failure locating or loading data from the disk.
     * @see #save(String)
     */
    public static MapImage load(String mapImagePath) throws IOException {
        File file = new File(mapImagePath);
        if (!file.exists()) {
            throw new IOException();
        }
        
        if (file.isDirectory()) {
            return loadFromDir(file);
        } else {
            return loadFromFile(file);
        }
    }
    
    /**
     * Saves the content of this MapImage object into an enhanced .jpeg file.
     * The resulting .jpeg file is an image of the base map followed by a 
     * JSON serialized object containing the routes overlays.
     * @param mapImageFileName - the name of the .jpg file to be created.<br>
     * e.g.: "Ravenna.jpg"
     * @throws IOException - failure in writing to the disk.
     * @see #load(String)
     */
    public void save(String mapImageFileName) throws IOException {
        MapMetadata mapMetadata = new MapMetadata();
        mapMetadata._mapName = _mapName;
        for(Map.Entry<String, BufferedImage> mapOverlay : _mapOverlays.entrySet())
        {
            mapMetadata._mapOverlaysRaw.put(
                    mapOverlay.getKey(),
                    imageToBase64(mapOverlay.getValue()));
        }
        mapMetadata._centerTL = _centerTL;
        mapMetadata._centerBR = _centerBR;
        Gson serializer = new Gson();
        String jsonMapRoutes = serializer.toJson(mapMetadata);
        Path mapImagePath = Paths.get(mapImageFileName);
        ByteArrayOutputStream mapImageStream = new ByteArrayOutputStream();
        ImageIO.write(this._image, "jpg", mapImageStream);
        byte[] mapImageBytes = mapImageStream.toByteArray();
        Files.write(mapImagePath, mapImageBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        byte[] mapRoutesBytes = jsonMapRoutes.getBytes();
        Files.write(mapImagePath, mapRoutesBytes, StandardOpenOption.APPEND);
        byte[] mapImageLenBytes = toByteArray(BigInteger.valueOf(mapImageBytes.length), 4);
        Files.write(mapImagePath, mapImageLenBytes, StandardOpenOption.APPEND);
    }
    // EndRegion: [public] FileIO
    
    /**
     * Creates a new MapImage object for a given map name and image.
     * @param mapName - the name of the map to be created.
     * @param baseMap - the pixels (BufferedImage) for the map.
     * @see #MapImage.load(String)
     */
    public MapImage(String mapName, BufferedImage baseMap) {
        super(baseMap);
        _mapName = mapName;
        _mapOverlays = new HashMap<String, BufferedImage>();
    }

    public void setCenter(Point tl, Point br) {
        _centerTL = tl;
        _centerBR = br;
    }
    
    /**
     * Gets the name of the map in this MapImage.
     * @return The name of the map.
     * @see #MapImage(String, BufferedImage)
     * @see #load(String)
     */
    public String getMapName() {
        return _mapName;
    }
    
    /**
     * Gets the set of all the routes, given by name, embedded in this map.
     * @return The names of all the routes embedded with this map.<br>
     * e.g.: {"AB", "AC", "AD", "BA", "BC", "BD", ...}
     */
    public Set<String> getRoutes() {
        return _mapOverlays.keySet();
    }
    
    /**
     * Gets the set of routes overlaid on the map. This is a subset
     * of all the routes embedded in this map.
     * @return The names of the routes overlayed on the map.<br>
     * e.g.: {"AB", "AC", "CB", ...}
     * @see #setOverlays(String...)
     * @see #getRoutes()
     */
    public Set<String> getOverlays() {
        return new TreeSet<String>(_overlays);
    }
    
    /**
     * Sets the routes to be overlaid on the map. This is expected
     * to be a subset of all the routes embedded in this map.
     * @param routes - var arg array with the routes to be overlaid on the map.
     * @see #getOverlays()
     * @see #getRoutes()
     */
    public void setOverlays(String... routes) {
        setOverlays(Arrays.asList(routes));
    }
    
    /**
     * Sets the routes to be overlaid on the map. This is expected
     * to be a subset of all the routes embedded in this map.
     * @param routes - collection (List, or Set) with the routes to be overlaid on the map.
     * @see #getOverlays()
     * @see #getRoutes()
     */
    public void setOverlays(Collection<String> routes) {
        _overlays.clear();
        _overlays.addAll(routes);
    }
    
    /**
     * Indicates whether any of the given routes are colliding with any other.
     * A collision is detected if any of the route overlays have non-transparent
     * pixels of different colors at the same coordinates on the map <br>
     * <p><u>Examples:</u><br>
     * assuming "AB" and "AC" have same color, collide("AB", "AC")
     * returns false<br>
     * assuming "AB" and "CA" have different colors and have overlapping pixels,
     * collide("AB","CA") returns true. 
     * @param routes - the list of route names to be tested.
     * @return True if the routes do not collide, false otherwise.
     */
    public boolean collide(String... routes) {
        int xMin = 0;
        int yMin = 0;
        if (_centerTL != null) {
            xMin = (int)_centerTL.getX();
            yMin = (int)_centerTL.getY();
        }
        int xMax = getWidth();
        int yMax = getHeight();
        if (_centerBR != null) {
            xMax = (int)_centerBR.getX()+1;
            yMax = (int)_centerBR.getY()+1;
        }
        for (int x = xMin; x < xMax; x++) {
            for (int y = yMin; y < yMax; y++) {
                String lastOpaque = null;
                for(String route : routes) {
                    if (!_mapOverlays.containsKey(route)) {
                        continue;
                    }
                    
                    int overlayPix = _mapOverlays.get(route).getRGB(x, y);
                    if ((overlayPix >> 24) != 0) {
                        if (lastOpaque == null) {
                            lastOpaque = route;
                        }
                        if (route.charAt(0) != lastOpaque.charAt(0)) {
                            return true;
                        }
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Gets the buffered image composing the map with all the requested overlays.
     * @return The bufferd image for the composited map.
     * @see #getOverlays()
     * @see #setOverlays(String...)
     */
    @Override
    public BufferedImage getImage() {
        BufferedImage image = new BufferedImage(getWidth(),getHeight(),BufferedImage.TYPE_INT_ARGB);
        Graphics g=image.getGraphics();
        g.drawImage(_image,0,0,null);
        for (String overlay : _overlays) {
            if (_mapOverlays.containsKey(overlay)) {
                g.drawImage(_mapOverlays.get(overlay),0,0,null);
            }
        }
        return image;
    }
}
