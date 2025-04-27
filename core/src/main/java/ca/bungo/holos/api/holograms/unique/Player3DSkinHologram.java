package ca.bungo.holos.api.holograms.unique;

import ca.bungo.holos.BungosHolos;
import ca.bungo.holos.api.holograms.Hologram;
import ca.bungo.holos.api.holograms.unique.image.ImageHologram;
import ca.bungo.holos.utility.NetworkUtility;
import ca.bungo.holos.utility.PixelUtility;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Consumer;

@Getter
@Setter
public class Player3DSkinHologram implements Hologram {

    private interface OffsetDefiner {
        Vector3f defineOffset(Pixel pixel, Vector3f baseOffset);
    }

    private enum BodyPosition {
        FRONT(new Vector2f(0, 0)),
        BACK(new Vector2f(0, 180)),
        RIGHT(new Vector2f(0, -90)),
        LEFT(new Vector2f(0, 90)),
        TOP(new Vector2f(-90, 0)),
        BOTTOM(new Vector2f(90, 180)),
        ;

        public final Vector2f rotation;

        BodyPosition(Vector2f rotation) {
            this.rotation = rotation;
        }
    }
    private enum BodyPart {
        HEAD,
        BODY,
        LEFT_ARM,
        RIGHT_ARM,
        LEFT_LEG,
        RIGHT_LEG,
        ;
        ;
    }

    private record Pixel(int x, int y, int width, Color color){}
    private record SkinZone(int startX, int startY, int xSize, int ySize, boolean isOverlay, BodyPart bodyPart, BodyPosition bodyPosition) {}

    private static List<SkinZone> skinZones = List.of(
            new SkinZone(8, 48, 8, 8, false, BodyPart.HEAD, BodyPosition.FRONT),
            new SkinZone(24, 48, 8, 8, false, BodyPart.HEAD, BodyPosition.BACK),
            new SkinZone(0, 48, 8, 8, false, BodyPart.HEAD, BodyPosition.RIGHT),
            new SkinZone(16, 48, 8, 8, false, BodyPart.HEAD, BodyPosition.LEFT),
            new SkinZone(8, 56, 8, 8, false, BodyPart.HEAD, BodyPosition.TOP),
            new SkinZone(16, 56, 8, 8, false, BodyPart.HEAD, BodyPosition.BOTTOM),

            new SkinZone(40, 48, 8, 8, true, BodyPart.HEAD, BodyPosition.FRONT),
            new SkinZone(56, 48, 8, 8, true, BodyPart.HEAD, BodyPosition.BACK),
            new SkinZone(32, 48, 8, 8, true, BodyPart.HEAD, BodyPosition.RIGHT),
            new SkinZone(48, 48, 8, 8, true, BodyPart.HEAD, BodyPosition.LEFT),
            new SkinZone(40, 56, 8, 8, true, BodyPart.HEAD, BodyPosition.TOP),
            new SkinZone(48, 56, 8, 8, true, BodyPart.HEAD, BodyPosition.BOTTOM)
    );

    private String uuid;

    private String playerUUID;
    private Location location;
    float pixelSize = 1f;

    private List<TextDisplay> displays = new ArrayList<>();

    private boolean loaded = false;
    private boolean loadAttempted = false;

    private Map<SkinZone, List<Pixel>> zonePixels = new HashMap<>();

    public Player3DSkinHologram(String playerUUID) {
        uuid = UUID.randomUUID().toString();
        this.playerUUID = playerUUID;
        this.location = null;

        loadPlayerSkin();
    }

    private void loadPlayerSkin() {
        try {
            NetworkUtility.getPlayerSkin(playerUUID).thenAccept(skin -> {
                boolean isSlim = skin[55][54].getAlpha() == 0; //ToDo: Slim Skin Zones

                for(SkinZone zone : skinZones) {
                    List<Pixel> optimizedPixels = buildPixelMap(PixelUtility.extractRegion(
                            skin, zone.startX, zone.startY, zone.xSize, zone.ySize
                    ));
                    zonePixels.put(zone, optimizedPixels);
                }

                loaded = true;
                if(loadAttempted) {
                    Bukkit.getScheduler().runTask(BungosHolos.get(), () -> spawn(this.getLocation()));
                }
            });
        } catch (URISyntaxException e) {
            BungosHolos.LOGGER.warn("Failed to load player skin for {}", playerUUID, e);
        }

    }

    private List<Pixel> buildPixelMap(Color[][] colors) {
        List<Pixel> pixels = new ArrayList<>();
        int width = colors.length;
        if(width == 0) return List.of();
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
        return pixels;
    }

    public void spawn(Location location) {
        this.location = location;
        if(!isLoaded()) {
            loadAttempted = true;
            return;
        }
        spawn3DHologram();
    }

    private void spawn3DHologram() {
        cleanup();
        for(Map.Entry<SkinZone, List<Pixel>> entry : zonePixels.entrySet()) {
            SkinZone zone = entry.getKey();
            List<Pixel> pixels = entry.getValue();

            float overlayOffset = zone.isOverlay ? 0.05f * pixelSize : 0;
            float pixelSizeDiv8 = pixelSize / 8f;
            float pixelSizeDiv20 = pixelSize / 20f;

            OffsetDefiner offsetDefiner = (p,v) -> new Vector3f();

            if(zone.bodyPart.equals(BodyPart.HEAD)) {
                offsetDefiner = switch (zone.bodyPosition) {
                    case FRONT -> (pixel, base) -> new Vector3f(
                            base.x,
                            base.y,
                            base.z + overlayOffset
                    );
                    case BACK -> (pixel, base) -> new Vector3f(
                            (zone.xSize - pixel.x) * pixelSizeDiv8 - (pixel.width * pixelSizeDiv20),
                            base.y,
                            base.z - pixelSizeDiv8 * zone.xSize - overlayOffset
                    );
                    case RIGHT -> (pixel, base) -> new Vector3f(
                            base.z - overlayOffset,
                            base.y,
                            base.x - pixelSizeDiv8 * zone.xSize
                    );
                    case LEFT -> (pixel, base) -> new Vector3f(
                            base.z + pixelSizeDiv8 * zone.xSize + overlayOffset,
                            base.y,
                            (zone.xSize - pixel.x) * pixelSizeDiv8 - (pixel.width * pixelSizeDiv20) - pixelSizeDiv8 * zone.xSize
                    );
                    case TOP -> (pixel, base) -> new Vector3f(
                            base.x,
                            base.z + pixelSizeDiv8 * zone.xSize + overlayOffset,
                            (zone.ySize - pixel.y) * pixelSizeDiv8 - pixelSizeDiv8 * zone.xSize
                    );
                    case BOTTOM -> (pixel, base) -> new Vector3f(
                            (zone.xSize - pixel.x) * pixelSizeDiv8 - (pixel.width * pixelSizeDiv20),
                            base.z - overlayOffset,
                            (zone.xSize - pixel.y) * pixelSizeDiv8 - pixelSizeDiv8 * zone.xSize
                    );
                };

            }


            spawnPixels(pixels, zone.bodyPosition.rotation, offsetDefiner, zone.xSize);
        }
    }

    private void spawnPixels(List<Pixel> toSpawn, Vector2f rotation, OffsetDefiner offsetDefiner, int rowSize) {
        new BukkitRunnable() {
            int index = 0;
            @Override
            public void run() {
                int count = 0;
                while(count++ < rowSize && index < toSpawn.size()) {
                    Pixel pixel = toSpawn.get(index);
                    if(pixel.color.getAlpha() == 0) {
                        index++;
                        continue;
                    }

                    Bukkit.getScheduler().runTask(BungosHolos.get(), () -> {
                        TextDisplay display = createBasicDisplay(location);
                        displays.add(display);
                        display.setBackgroundColor(pixel.color);

                        float posX = pixel.x*pixelSize/(8) + (pixel.width*pixelSize/20f);
                        float posY = pixel.y*pixelSize/(8);

                        Transformation transformation = new Transformation(
                                offsetDefiner.defineOffset(pixel, new Vector3f(posX, posY, 0)),
                                new AxisAngle4f(),
                                new Vector3f(pixelSize*pixel.width, pixelSize/2, 1),
                                new AxisAngle4f()
                        );

                        AxisAngle4f xAngle = new AxisAngle4f((float)Math.toRadians(rotation.x), 1, 0, 0);
                        transformation.getLeftRotation().premul(new Quaternionf(xAngle));
                        AxisAngle4f yAngle = new AxisAngle4f((float)Math.toRadians(rotation.y), 0, 1, 0);
                        transformation.getLeftRotation().premul(new Quaternionf(yAngle));

                        display.setTransformation(transformation);
                    });
                    index++;
                }
                if(index >= toSpawn.size()) this.cancel();
            }
        }.runTaskTimerAsynchronously(BungosHolos.get(), 0, 1);
    }

    private void cleanup() {
        for(TextDisplay display : displays) {
            display.remove();
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

    @Override
    public String getUniqueIdentifier() {
        return uuid;
    }

    @Override
    public void onDisable() throws IOException {

    }

    @Override
    public void remove() {
        cleanup();
        BungosHolos.get().hologramRegistry.unregisterHologram(this);
        loaded = false;
        loadAttempted = false;
        uuid = null;
        playerUUID = null;
        location = null;
        displays.clear();
        zonePixels.clear();
    }

    @Override
    public void teleport(Location location) {

    }
}
