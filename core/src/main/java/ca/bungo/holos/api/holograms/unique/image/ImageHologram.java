package ca.bungo.holos.api.holograms.unique.image;

import ca.bungo.holos.BungosHolos;
import ca.bungo.holos.api.holograms.Hologram;
import ca.bungo.holos.api.holograms.SimpleHologram;
import ca.bungo.holos.registries.HologramRegistry;
import ca.bungo.holos.utility.NetworkUtility;
import ca.bungo.holos.utility.PixelUtility;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
public class ImageHologram implements Hologram {

    private record Pixel(int x, int y, int width, Color color){}

    private float pixelSize;
    private String uuid;
    private Location location;
    private Color[][] colors;

    private List<TextDisplay> displays = new ArrayList<>();

    private List<Pixel> pixels = new ArrayList<>();

    public ImageHologram(Color[][] colors) {
        uuid = UUID.randomUUID().toString();
        this.colors = colors;
        this.pixelSize = 0.25f;

        buildPixelMap();
    }

    protected void buildPixelMap() {
        pixels.clear();
        int width = colors.length;
        if(width == 0) return;
        int height = colors[0].length;

        for(int y = 0; y < height; y++) {
            int x = 0;
            while(x < width) {
                Color color = colors[x][y];
                int startX = x;
                while(x < width && colors[x][y].equals(color)) {
                    x++;
                }
                Pixel pixel = new Pixel(startX, y, x - startX, color);
                pixels.add(pixel);
            }
        }
    }

    public void spawn(Location location) {
        this.location = location;
        int rowSize = colors.length;
        cleanup();
        new BukkitRunnable() {
            int index = 0;
            @Override
            public void run() {
                int count = 0;
                while(count++ < rowSize && index < pixels.size()) {
                    Pixel pixel = pixels.get(index);
                    if(pixel.color.getAlpha() == 0) {
                        index++;
                        continue;
                    }

                    Bukkit.getScheduler().runTask(BungosHolos.get(), () -> {
                        TextDisplay display = createBasicDisplay(location);
                        displays.add(display);
                        display.setBackgroundColor(pixel.color);

                        float posX = pixel.x*pixelSize/(8) + (pixel.width*pixelSize/20f);
                        float posY = pixel.y*pixelSize/(8); //I honestly dont know why these magic numbers work. But they made everything aligned

                        Transformation transformation = new Transformation(
                                new Vector3f(posX, posY, 0),
                                new AxisAngle4f(),
                                new Vector3f(pixelSize*pixel.width, pixelSize/2, 1),
                                new AxisAngle4f()
                        );

                        display.setTransformation(transformation);
                    });
                    index++;
                }
                if(index >= pixels.size()) this.cancel();
            }
        }.runTaskTimerAsynchronously(BungosHolos.get(), 0, 1);
    }

    /**
     * Set the scale of the pixels in the image
     * @param scale Scale to set to (Default: 0.25f)
     * */
    public void setScale(float scale) {
        this.pixelSize = scale;
        spawn(location);
    }

    @Override
    public String getUniqueIdentifier() {
        return uuid;
    }

    @Override
    public void onDisable() throws IOException {
        //Create configuration file
        File file = new File(JavaPlugin.getProvidingPlugin(SimpleHologram.class).getDataFolder(), "holograms.yml");
        if(!file.exists()) {
            file.createNewFile();
        }

        //Load the configuration file api
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        //Grab holograms: section
        ConfigurationSection holograms = config.getConfigurationSection("holograms");
        if(holograms == null) {
            holograms = config.createSection("holograms");
        }
        if(this.getUniqueIdentifier() != null && this instanceof ConfigurationSerializable) {
            holograms.set(this.getUniqueIdentifier(), this);
        }
        config.save(file);
        for(Display display : displays) {
            display.setPersistent(false);
        }
    }

    @Override
    public void remove() {
        cleanup();
        BungosHolos.get().hologramRegistry.unregisterHologram(this);
    }

    @Override
    public void teleport(Location location) {
        this.location = location;
        for(Display display : displays) {
            display.teleport(location);
        }
    }

    /**
     * Teleport the hologram to supplied location
     * @param location Where to teleport to
     * @param updateLocation Should we update the holograms base location
     * */
    public void teleport(Location location, boolean updateLocation) {
        if(updateLocation) this.location = location;
        for(Display display : displays) {
            display.teleport(location);
        }
    }

    private TextDisplay createBasicDisplay(Location baseLocation) {
        TextDisplay display = baseLocation.getWorld().spawn(baseLocation, TextDisplay.class);
        display.text(Component.text(" "));
        display.setBillboard(Display.Billboard.FIXED);
        display.setPersistent(true);
        display.setAlignment(TextDisplay.TextAlignment.CENTER);
        display.setBackgroundColor(Color.BLACK);
        return display;
    }

    private void cleanup() {
        for(TextDisplay display : displays) {
            display.remove();
        }
    }

    public static class PlayerSkinHologram extends ImageHologram {
        public enum HologramType {
            HEAD,FLAT,FULL
        }

        @Getter
        @Setter
        private String playerUUID;

        @Getter
        @Setter
        private HologramType type;

        private boolean isLoaded = false;
        private boolean loadAttempted = false;

        // First, we need to add a list to store all the face holograms in the PlayerSkinHologram class
        private final List<ImageHologram> faceHolograms = new ArrayList<>();

        public PlayerSkinHologram(@NotNull String playerUUID, @NotNull HologramType type) {
            super(new Color[0][0]);

            this.playerUUID = playerUUID;
            this.type = type;

            loadPlayerSkin();
        }

        private void loadPlayerSkin() {
            try {
                NetworkUtility.getPlayerSkin(this.playerUUID).thenAccept(skin -> {
                    this.setColors(skin);
                    boolean isSlim = skin[55][54].getAlpha() == 0;

                    if(type == HologramType.HEAD) {
                        Color[][] head = PixelUtility.extractRegion(skin, 8, 48, 8, 8);
                        this.setColors(head);

                    }
                    else if(type == HologramType.FLAT) {
                        Color[][] fullBody = new Color[16][32];
                        Color transparent = Color.fromARGB(0,0,0,0);
                        for(int i = 0; i < 16; i++) {
                            for(int j = 0; j < 32; j++) {
                                fullBody[i][j] = transparent;
                            }
                        }

                        Color[][] head = PixelUtility.extractRegion(skin, 8, 48, 8, 8);
                        Color[][] body = PixelUtility.extractRegion(skin, 20, 32, 8, 12);

                        placeAt(fullBody, head, 4, 24, 8, 8);
                        placeAt(fullBody, body, 4, 12, 8, 12);

                        Color[][] lLeg = PixelUtility.extractRegion(skin, 20, 0, 4, 12);
                        Color[][] rLeg = PixelUtility.extractRegion(skin, 4, 32, 4, 12);

                        placeAt(fullBody, rLeg, 4, 0, 4, 12);
                        placeAt(fullBody, lLeg, 8, 0, 4, 12);

                        if(isSlim){
                            Color[][] lArm = PixelUtility.extractRegion(skin, 36, 0, 3, 12);
                            Color[][] rArm = PixelUtility.extractRegion(skin, 44, 32, 3, 12);

                            placeAt(fullBody, rArm, 1, 12, 3, 12);
                            placeAt(fullBody, lArm, 12, 12, 3, 12);
                        }

                        else {
                            Color[][] lArm = PixelUtility.extractRegion(skin, 36, 0, 4, 12);
                            Color[][] rArm = PixelUtility.extractRegion(skin, 44, 32, 4, 12);

                            placeAt(fullBody, rArm, 0, 12, 4, 12);
                            placeAt(fullBody, lArm, 12, 12, 4, 12);
                        }

                        this.setColors(fullBody);
                    }
                    else if (type == HologramType.FULL) {
                        this.setColors(skin);
                    }

                    buildPixelMap();
                    isLoaded = true;
                    if(loadAttempted && getLocation() != null) {
                        Bukkit.getScheduler().runTask(BungosHolos.get(), () -> spawn(this.getLocation()));
                    }
                });
            } catch(URISyntaxException e) {
                BungosHolos.LOGGER.warn("Failed to load player skin for hologram: {}", this.getUniqueIdentifier(), e);
            }
        }

        private void placeAt(Color[][] dest, Color[][] src, int x, int y, int srcWidth, int srcHeight) {
            for(int xPos = 0; xPos < srcWidth; xPos++) {
                for(int yPos = 0; yPos < srcHeight; yPos++) {
                    dest[x + xPos][y + yPos] = src[xPos][yPos];
                }
            }
        }


        // Then, we need to override the spawn method to handle the 3D positioning of each face
        @Override
        public void spawn(Location location) {
            this.setLocation(location);
            if (!isLoaded) {
                loadAttempted = true;
                BungosHolos.LOGGER.info("Hologram {} is not loaded yet. When complete skin will render...", this.getUniqueIdentifier());
                return;
            }

            // If this is a FULL type hologram, we need to create and position all the face holograms
            if (type == HologramType.FULL) {
                // Clean up any existing face holograms
                for (ImageHologram hologram : faceHolograms) {
                    hologram.remove();
                }
                faceHolograms.clear();

                // Create and position all the face holograms
                spawnFullSkinHologram(location);
            } else {
                // For other types, use the default spawn method
                super.spawn(location);
            }
        }

        // Method to create and position all the face holograms for the 3D player skin
        private void spawnFullSkinHologram(Location baseLocation) {

            //Main
            Color[][] headFront = PixelUtility.extractRegion(getColors(), 8, 48, 8, 8);
            Color[][] headBack = PixelUtility.extractRegion(getColors(), 24, 48, 8, 8);
            Color[][] headRight = PixelUtility.extractRegion(getColors(), 0, 48, 8, 8);
            Color[][] headLeft = PixelUtility.extractRegion(getColors(), 16, 48, 8, 8);
            Color[][] headTop = PixelUtility.extractRegion(getColors(), 8, 56, 8, 8);
            Color[][] headBottom = PixelUtility.extractRegion(getColors(), 16, 56, 8, 8);

            //Overlay
            Color[][] overlayFront = PixelUtility.extractRegion(getColors(), 40, 48, 8, 8);
            Color[][] overlayBack = PixelUtility.extractRegion(getColors(), 56, 48, 8, 8);
            Color[][] overlayRight = PixelUtility.extractRegion(getColors(), 32, 48, 8, 8);
            Color[][] overlayLeft = PixelUtility.extractRegion(getColors(), 48, 48, 8, 8);
            Color[][] overlayTop = PixelUtility.extractRegion(getColors(), 40, 56, 8, 8);
            Color[][] overlayBottom = PixelUtility.extractRegion(getColors(), 48, 56, 8, 8);

            // Scale factor for the hologram
            float scale = this.getPixelSize();

            // Create and position head faces
            // Head is positioned at the top of the model
            Location headLocation = baseLocation.clone().add(0, 1.5 * scale, 0);

            // Head Main
            spawnFace(headLocation.clone().add(0, 0, 0.25 * scale), headFront, 8, 8, 0, scale);
            spawnFace(headLocation.clone().add(scale, 0, -0.75 * scale), headBack, 8, 8, 180, scale);
            spawnFace(headLocation.clone().add(0, 0, -0.75 * scale), headRight, 8, 8, 90, scale);
            spawnFace(headLocation.clone().add(scale, 0, 0.25 * scale), headLeft, 8, 8, -90, scale);
            spawnFace(headLocation.clone().add(0, scale, 0.25 * scale), headTop, 8, 8, 0, -90, scale);
            spawnFace(headLocation.clone().add(scale, 0, 0.25 * scale), headBottom, 8, 8, 180, 90, scale);

            //Head Overlay
            spawnFace(headLocation.clone().add(0, 0, 0.25 * scale + 0.05*scale), overlayFront, 8, 8, 0, scale);
            spawnFace(headLocation.clone().add(scale, 0, -0.75 * scale - 0.05*scale), overlayBack, 8, 8, 180, scale);
            spawnFace(headLocation.clone().add(-0.05*scale, 0, -0.75 * scale), overlayRight, 8, 8, 90, scale);
            spawnFace(headLocation.clone().add(scale + 0.05*scale, 0, 0.25 * scale), overlayLeft, 8, 8, -90, scale);
            spawnFace(headLocation.clone().add(0, scale + 0.05*scale, 0.25 * scale), overlayTop, 8, 8, 0, -90, scale);
            spawnFace(headLocation.clone().add(scale, 0 - 0.05*scale, 0.25 * scale), overlayBottom, 8, 8, 180, 90, scale);
        }

        private void spawnFace(Location location, Color[][] colors, int width, int height, float yaw, float scale) {
            ImageHologram hologram = new ImageHologram(colors);
            hologram.setPixelSize(scale);

            // Set the yaw of the location to rotate the face
            location.setYaw(yaw);

            hologram.spawn(location);
            faceHolograms.add(hologram);
        }

        // Add an overloaded method that also handles pitch
        private void spawnFace(Location location, Color[][] colors, int width, int height, float yaw, float pitch, float scale) {
            ImageHologram hologram = new ImageHologram(colors);
            hologram.setPixelSize(scale);

            // Set both yaw and pitch
            location.setYaw(yaw);
            location.setPitch(pitch);

            hologram.spawn(location);
            faceHolograms.add(hologram);
        }

        // Override the remove method to clean up all face holograms
        @Override
        public void remove() {
            super.remove();

            // Clean up any existing face holograms
            for (ImageHologram hologram : faceHolograms) {
                hologram.remove();
            }
            faceHolograms.clear();
        }

        // Override the teleport method to teleport all face holograms
        @Override
        public void teleport(Location location) {
            super.teleport(location);

            // If this is a FULL type hologram, we need to respawn all the face holograms at the new location
            if (type == HologramType.FULL && isLoaded) {
                // Clean up any existing face holograms
                for (ImageHologram hologram : faceHolograms) {
                    hologram.remove();
                }
                faceHolograms.clear();

                // Create and position all the face holograms at the new location
                spawnFullSkinHologram(location);
            }
        }
    }

}
