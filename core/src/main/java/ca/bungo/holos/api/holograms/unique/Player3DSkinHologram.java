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
        HEAD(0, 3f, 0),
        BODY(0, 1.5f, -0.25f),
        LEFT_ARM(1f, 1.5f, -0.25f),
        RIGHT_ARM(-0.5f, 1.5f, -0.25f),
        LEFT_LEG(0.5f, 0, -0.25f),
        RIGHT_LEG(0, 0, -0.25f),
        ;

        private final float xOffset;
        private final float yOffset;
        private final float zOffset;

        BodyPart(float xOffset, float yOffset, float zOffset) {
            this.xOffset = xOffset;
            this.yOffset = yOffset;
            this.zOffset = zOffset;
        }

    }

    private record Pixel(int x, int y, int width, Color color){}
    private record SkinZone(int startX, int startY, int startSlimX, int startSlimY, int xSize, int ySize, int xSlimSize, int ySlimSize, boolean isOverlay, BodyPart bodyPart, BodyPosition bodyPosition) {}

    private static List<SkinZone> skinZones = List.of(

            //Main Skin

            //Head
            new SkinZone(8, 48, 8, 48, 8, 8, 8, 8,false, BodyPart.HEAD, BodyPosition.FRONT),
            new SkinZone(24, 48, 24, 48,8, 8, 8, 8, false, BodyPart.HEAD, BodyPosition.BACK),
            new SkinZone(0, 48, 0, 48,8, 8, 8, 8,false, BodyPart.HEAD, BodyPosition.RIGHT),
            new SkinZone(16, 48, 16, 48, 8, 8, 8, 8,false, BodyPart.HEAD, BodyPosition.LEFT),
            new SkinZone(8, 56, 8, 56, 8, 8, 8, 8,false, BodyPart.HEAD, BodyPosition.TOP),
            new SkinZone(16, 56, 16, 56, 8, 8, 8, 8,false, BodyPart.HEAD, BodyPosition.BOTTOM),

            //Body
            new SkinZone(20, 32,20, 32, 8, 12,8, 12, false, BodyPart.BODY, BodyPosition.FRONT),
            new SkinZone(32, 32,32, 32, 8, 12,8, 12, false, BodyPart.BODY, BodyPosition.BACK),
            new SkinZone(16, 32,16, 32, 4, 12,4, 12, false, BodyPart.BODY, BodyPosition.RIGHT),
            new SkinZone(28, 32,28, 32, 4, 12,4, 12, false, BodyPart.BODY, BodyPosition.LEFT),
            new SkinZone(20, 44,20, 44, 8, 4,8, 4, false, BodyPart.BODY, BodyPosition.TOP),
            new SkinZone(28, 44,28, 44, 8, 4,8, 4, false, BodyPart.BODY, BodyPosition.BOTTOM),

            //Right Arm
            new SkinZone(44, 32,44, 32, 4, 12,3, 12, false, BodyPart.RIGHT_ARM, BodyPosition.FRONT),
            new SkinZone(52, 32,51, 32, 4, 12,3, 12, false, BodyPart.RIGHT_ARM, BodyPosition.BACK),
            new SkinZone(40, 32,40, 32, 4, 12,4, 12, false, BodyPart.RIGHT_ARM, BodyPosition.RIGHT),
            new SkinZone(48, 32,47, 32, 4, 12,4, 12, false, BodyPart.RIGHT_ARM, BodyPosition.LEFT),
            new SkinZone(44, 44,44, 44, 4, 4,3, 4, false, BodyPart.RIGHT_ARM, BodyPosition.TOP),
            new SkinZone(48, 44,47, 44, 4, 4,3, 4, false, BodyPart.RIGHT_ARM, BodyPosition.BOTTOM),

            //Left Arm
            new SkinZone(36, 0,36, 0, 4, 12,3, 12, false, BodyPart.LEFT_ARM, BodyPosition.FRONT),
            new SkinZone(44, 0,43, 0, 4, 12,3, 12, false, BodyPart.LEFT_ARM, BodyPosition.BACK),
            new SkinZone(32, 0,32, 0, 4, 12,4, 12, false, BodyPart.LEFT_ARM, BodyPosition.RIGHT),
            new SkinZone(40, 0,39, 0, 4, 12,4, 12, false, BodyPart.LEFT_ARM, BodyPosition.LEFT),
            new SkinZone(36, 12,36, 12, 4, 4,3, 4, false, BodyPart.LEFT_ARM, BodyPosition.TOP),
            new SkinZone(40, 12,39, 12, 4, 4,3, 4, false, BodyPart.LEFT_ARM, BodyPosition.BOTTOM),

            //Right Leg
            new SkinZone(4, 32,4, 32, 4, 12,4, 12, false, BodyPart.RIGHT_LEG, BodyPosition.FRONT),
            new SkinZone(12, 32, 12, 32, 4, 12,4, 12, false, BodyPart.RIGHT_LEG, BodyPosition.BACK),
            new SkinZone(0, 32,0, 32, 4, 12,4, 12, false, BodyPart.RIGHT_LEG, BodyPosition.RIGHT),
            new SkinZone(8, 32,8, 32, 4, 12,4, 12, false, BodyPart.RIGHT_LEG, BodyPosition.LEFT),
            new SkinZone(4, 44,4, 44, 4, 4,4, 4, false, BodyPart.RIGHT_LEG, BodyPosition.TOP),
            new SkinZone(8, 44,8, 44, 4, 4,4, 4, false, BodyPart.RIGHT_LEG, BodyPosition.BOTTOM),

            //Left Leg
            new SkinZone(20, 0,20, 0, 4, 12,4, 12, false, BodyPart.LEFT_LEG, BodyPosition.FRONT),
            new SkinZone(28, 0,28, 0, 4, 12,4, 12, false, BodyPart.LEFT_LEG, BodyPosition.BACK),
            new SkinZone(16, 0,16, 0, 4, 12,4, 12, false, BodyPart.LEFT_LEG, BodyPosition.RIGHT),
            new SkinZone(24, 0,24, 0, 4, 12,4, 12, false, BodyPart.LEFT_LEG, BodyPosition.LEFT),
            new SkinZone(20, 12,20, 12, 4, 4,4, 4, false, BodyPart.LEFT_LEG, BodyPosition.TOP),
            new SkinZone(24, 12,24, 12, 4, 4,4, 4, false, BodyPart.LEFT_LEG, BodyPosition.BOTTOM),

            //Overlays

            //Head
            new SkinZone(40, 48,40, 48, 8, 8,8, 8, true, BodyPart.HEAD, BodyPosition.FRONT),
            new SkinZone(56, 48,56, 48, 8, 8,8, 8, true, BodyPart.HEAD, BodyPosition.BACK),
            new SkinZone(32, 48,32, 48, 8, 8,8, 8, true, BodyPart.HEAD, BodyPosition.RIGHT),
            new SkinZone(48, 48,48, 48, 8, 8,8, 8, true, BodyPart.HEAD, BodyPosition.LEFT),
            new SkinZone(40, 56,40, 56, 8, 8,8, 8, true, BodyPart.HEAD, BodyPosition.TOP),
            new SkinZone(48, 56,48, 56, 8, 8,8, 8, true, BodyPart.HEAD, BodyPosition.BOTTOM),

            //Body
            new SkinZone(20, 16,20, 16, 8, 12,8, 12, true, BodyPart.BODY, BodyPosition.FRONT),
            new SkinZone(32, 16,32, 16, 8, 12,8, 12, true, BodyPart.BODY, BodyPosition.BACK),
            new SkinZone(16, 16,16, 16, 4, 12,4, 12, true, BodyPart.BODY, BodyPosition.RIGHT),
            new SkinZone(28, 16,28, 16, 4, 12,4, 12, true, BodyPart.BODY, BodyPosition.LEFT),
            new SkinZone(20, 28,20, 28, 8, 4,8, 4, true, BodyPart.BODY, BodyPosition.TOP),
            new SkinZone(28, 28,28, 28, 8, 4,8, 4, true, BodyPart.BODY, BodyPosition.BOTTOM),

            //Right Arm
            new SkinZone(44, 16,44, 16, 4, 12,3, 12, true, BodyPart.RIGHT_ARM, BodyPosition.FRONT),
            new SkinZone(52, 16,51, 16, 4, 12,3, 12, true, BodyPart.RIGHT_ARM, BodyPosition.BACK),
            new SkinZone(40, 16,40, 16, 4, 12,4, 12, true, BodyPart.RIGHT_ARM, BodyPosition.RIGHT),
            new SkinZone(48, 16,47, 16, 4, 12,4, 12, true, BodyPart.RIGHT_ARM, BodyPosition.LEFT),
            new SkinZone(44, 28,44, 28, 4, 4,3, 4, true, BodyPart.RIGHT_ARM, BodyPosition.TOP),
            new SkinZone(48, 28,47, 28, 4, 4,3, 4, true, BodyPart.RIGHT_ARM, BodyPosition.BOTTOM),

            //Left Arm
            new SkinZone(52, 0,52, 0, 4, 12,3, 12, true, BodyPart.LEFT_ARM, BodyPosition.FRONT),
            new SkinZone(60, 0,59, 0, 4, 12,3, 12, true, BodyPart.LEFT_ARM, BodyPosition.BACK),
            new SkinZone(48, 0,48, 0, 4, 12,4, 12, true, BodyPart.LEFT_ARM, BodyPosition.RIGHT),
            new SkinZone(56, 0,55, 0, 4, 12,4, 12, true, BodyPart.LEFT_ARM, BodyPosition.LEFT),
            new SkinZone(52, 12,52, 12, 4, 4,3, 4, true, BodyPart.LEFT_ARM, BodyPosition.TOP),
            new SkinZone(56, 12,55, 12, 4, 4,3, 4, true, BodyPart.LEFT_ARM, BodyPosition.BOTTOM),

            //Right Leg
            new SkinZone(4, 16,4, 16, 4, 12,4, 12, true, BodyPart.RIGHT_LEG, BodyPosition.FRONT),
            new SkinZone(12, 16,12, 16, 4, 12,4, 12, true, BodyPart.RIGHT_LEG, BodyPosition.BACK),
            new SkinZone(0, 16,0, 16, 4, 12,4, 12, true, BodyPart.RIGHT_LEG, BodyPosition.RIGHT),
            new SkinZone(8, 16,8, 16, 4, 12,4, 12, true, BodyPart.RIGHT_LEG, BodyPosition.LEFT),
            new SkinZone(4, 28,4, 28, 4, 4,4, 4, true, BodyPart.RIGHT_LEG, BodyPosition.TOP),
            new SkinZone(8, 28,8, 28, 4, 4,4, 4, true, BodyPart.RIGHT_LEG, BodyPosition.BOTTOM),

            //Left Leg
            new SkinZone(4, 0,4, 0, 4, 12,4, 12, true, BodyPart.LEFT_LEG, BodyPosition.FRONT),
            new SkinZone(12, 0,12, 0, 4, 12,4, 12, true, BodyPart.LEFT_LEG, BodyPosition.BACK),
            new SkinZone(0, 0,0, 0, 4, 12,4, 12, true, BodyPart.LEFT_LEG, BodyPosition.RIGHT),
            new SkinZone(8, 0,8, 0, 4, 12,4, 12, true, BodyPart.LEFT_LEG, BodyPosition.LEFT),
            new SkinZone(4, 12,4, 12, 4, 4,4, 4, true, BodyPart.LEFT_LEG, BodyPosition.TOP),
            new SkinZone(8, 12,8, 12, 4, 4,4, 4, true, BodyPart.LEFT_LEG, BodyPosition.BOTTOM)
    );

    private String uuid;

    private String playerUUID;
    private Location location;
    float pixelSize = 0.25f;
    private boolean isSlim = false;

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
                isSlim = skin[55][54].getAlpha() == 0;

                for(SkinZone zone : skinZones) {
                    List<Pixel> optimizedPixels = buildPixelMap(PixelUtility.extractRegion(
                            skin,
                            isSlim ? zone.startSlimX : zone.startX,
                            isSlim ? zone.startSlimY : zone.startY,
                            isSlim ? zone.xSlimSize : zone.xSize,
                            isSlim ? zone.ySlimSize : zone.ySize
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


            OffsetDefiner offsetDefiner = isSlim ? calculateSlimSkin(zone) : calculateClassicSkin(zone);
            spawnPixels(pixels, zone.bodyPosition.rotation, offsetDefiner, zone.xSize);
        }
    }

    private OffsetDefiner calculateClassicSkin(SkinZone zone){
        OffsetDefiner offsetDefiner = (p,v) -> new Vector3f();
        float overlayOffset = zone.isOverlay ? 0.05f * pixelSize : 0;
        float pixelSizeDiv8 = pixelSize / 8f;
        float pixelSizeDiv20 = pixelSize / 20f;

        float xBodyOffset = zone.bodyPart.xOffset * pixelSize;
        float yBodyOffset = zone.bodyPart.yOffset * pixelSize;
        float zBodyOffset = zone.bodyPart.zOffset * pixelSize;


        if(zone.bodyPart.equals(BodyPart.HEAD)) {
            offsetDefiner = switch (zone.bodyPosition) {
                case FRONT -> (pixel, base) -> new Vector3f(
                        base.x + xBodyOffset,
                        base.y + yBodyOffset,
                        base.z + overlayOffset + zBodyOffset
                );
                case BACK -> (pixel, base) -> new Vector3f(
                        (zone.xSize - pixel.x) * pixelSizeDiv8 - (pixel.width * pixelSizeDiv20) + xBodyOffset,
                        base.y + yBodyOffset,
                        base.z - pixelSizeDiv8 * zone.xSize - overlayOffset + zBodyOffset
                );
                case RIGHT -> (pixel, base) -> new Vector3f(
                        base.z - overlayOffset + xBodyOffset,
                        base.y + yBodyOffset,
                        base.x - pixelSizeDiv8 * zone.xSize + zBodyOffset
                );
                case LEFT -> (pixel, base) -> new Vector3f(
                        base.z + pixelSizeDiv8 * zone.xSize + overlayOffset + xBodyOffset,
                        base.y + yBodyOffset,
                        (zone.xSize - pixel.x) * pixelSizeDiv8 - (pixel.width * pixelSizeDiv20) - pixelSizeDiv8 * zone.xSize + zBodyOffset
                );
                case TOP -> (pixel, base) -> new Vector3f(
                        base.x + xBodyOffset,
                        base.z + pixelSizeDiv8 * zone.xSize + overlayOffset + yBodyOffset,
                        (zone.ySize - pixel.y) * pixelSizeDiv8 - pixelSizeDiv8 * zone.xSize + zBodyOffset
                );
                case BOTTOM -> (pixel, base) -> new Vector3f(
                        (zone.xSize - pixel.x) * pixelSizeDiv8 - (pixel.width * pixelSizeDiv20) + xBodyOffset,
                        base.z - overlayOffset + yBodyOffset,
                        (zone.xSize - pixel.y) * pixelSizeDiv8 - pixelSizeDiv8 * zone.xSize + zBodyOffset
                );
            };

        }
        else if(zone.bodyPart.equals(BodyPart.BODY)) {
            offsetDefiner = switch (zone.bodyPosition) {
                case FRONT -> (pixel, base) -> new Vector3f(
                        base.x + xBodyOffset,
                        base.y + yBodyOffset,
                        base.z + overlayOffset + zBodyOffset
                );
                case BACK -> (pixel, base) -> new Vector3f(
                        (zone.xSize - pixel.x) * pixelSizeDiv8 - (pixel.width * pixelSizeDiv20) + xBodyOffset,
                        base.y + yBodyOffset,
                        base.z - pixelSizeDiv8 * (zone.xSize/2f) - overlayOffset + zBodyOffset
                );
                case RIGHT -> (pixel, base) -> new Vector3f(
                        base.z - overlayOffset + xBodyOffset,
                        base.y + yBodyOffset,
                        base.x - pixelSizeDiv8 * zone.xSize + zBodyOffset
                );
                case LEFT -> (pixel, base) -> new Vector3f(
                        base.z + pixelSizeDiv8 * (zone.xSize*2) + overlayOffset + xBodyOffset,
                        base.y + yBodyOffset,
                        (zone.xSize - pixel.x) * pixelSizeDiv8 - (pixel.width * pixelSizeDiv20) - pixelSizeDiv8 * zone.xSize + zBodyOffset
                );
                case TOP -> (pixel, base) -> new Vector3f(
                        base.x + xBodyOffset,
                        base.z + pixelSizeDiv8 * (zone.xSize*1.5f) + overlayOffset + yBodyOffset,
                        (zone.ySize - pixel.y) * pixelSizeDiv8 - pixelSizeDiv8 * (zone.xSize/2f) + zBodyOffset
                );
                case BOTTOM -> (pixel, base) -> new Vector3f(
                        (zone.xSize - pixel.x) * pixelSizeDiv8 - (pixel.width * pixelSizeDiv20) + xBodyOffset,
                        base.z - overlayOffset + yBodyOffset,
                        (zone.xSize - pixel.y) * pixelSizeDiv8 - pixelSizeDiv8 * (zone.xSize) + zBodyOffset
                );
            };
        }
        else if(zone.bodyPart.equals(BodyPart.RIGHT_ARM) ||
                zone.bodyPart.equals(BodyPart.LEFT_ARM) ||
                zone.bodyPart.equals(BodyPart.RIGHT_LEG) ||
                zone.bodyPart.equals(BodyPart.LEFT_LEG)) {
            offsetDefiner = switch (zone.bodyPosition) {
                case FRONT -> (pixel, base) -> new Vector3f(
                        base.x + xBodyOffset,
                        base.y + yBodyOffset,
                        base.z + overlayOffset + zBodyOffset
                );
                case BACK -> (pixel, base) -> new Vector3f(
                        (zone.xSize - pixel.x) * pixelSizeDiv8 - (pixel.width * pixelSizeDiv20) + xBodyOffset,
                        base.y + yBodyOffset,
                        base.z - pixelSizeDiv8 * (zone.xSize) - overlayOffset + zBodyOffset
                );
                case RIGHT -> (pixel, base) -> new Vector3f(
                        base.z - overlayOffset + xBodyOffset,
                        base.y + yBodyOffset,
                        base.x - pixelSizeDiv8 * zone.xSize + zBodyOffset
                );
                case LEFT -> (pixel, base) -> new Vector3f(
                        base.z + pixelSizeDiv8 * (zone.xSize) + overlayOffset + xBodyOffset,
                        base.y + yBodyOffset,
                        (zone.xSize - pixel.x) * pixelSizeDiv8 - (pixel.width * pixelSizeDiv20) - pixelSizeDiv8 * zone.xSize + zBodyOffset
                );
                case TOP -> (pixel, base) -> new Vector3f(
                        base.x + xBodyOffset,
                        base.z + pixelSizeDiv8 * (zone.xSize*3f) + overlayOffset + yBodyOffset,
                        (zone.ySize - pixel.y) * pixelSizeDiv8 - pixelSizeDiv8 * (zone.xSize) + zBodyOffset
                );
                case BOTTOM -> (pixel, base) -> new Vector3f(
                        (zone.xSize - pixel.x) * pixelSizeDiv8 - (pixel.width * pixelSizeDiv20) + xBodyOffset,
                        base.z - overlayOffset + yBodyOffset,
                        (zone.xSize - pixel.y) * pixelSizeDiv8 - pixelSizeDiv8 * (zone.xSize) + zBodyOffset
                );
            };
        }
        return offsetDefiner;
    }
    private OffsetDefiner calculateSlimSkin(SkinZone zone){
        OffsetDefiner offsetDefiner = (p,v) -> new Vector3f();
        float overlayOffset = zone.isOverlay ? 0.05f * pixelSize : 0;
        float pixelSizeDiv8 = pixelSize / 8f;
        float pixelSizeDiv20 = pixelSize / 20f;

        float xBodyOffset = zone.bodyPart.xOffset * pixelSize;
        float yBodyOffset = zone.bodyPart.yOffset * pixelSize;
        float zBodyOffset = zone.bodyPart.zOffset * pixelSize;

        if(zone.bodyPart.equals(BodyPart.HEAD)) {
            offsetDefiner = switch (zone.bodyPosition) {
                case FRONT -> (pixel, base) -> new Vector3f(
                        base.x + xBodyOffset,
                        base.y + yBodyOffset,
                        base.z + overlayOffset + zBodyOffset
                );
                case BACK -> (pixel, base) -> new Vector3f(
                        (zone.xSize - pixel.x) * pixelSizeDiv8 - (pixel.width * pixelSizeDiv20) + xBodyOffset,
                        base.y + yBodyOffset,
                        base.z - pixelSizeDiv8 * zone.xSize - overlayOffset + zBodyOffset
                );
                case RIGHT -> (pixel, base) -> new Vector3f(
                        base.z - overlayOffset + xBodyOffset,
                        base.y + yBodyOffset,
                        base.x - pixelSizeDiv8 * zone.xSize + zBodyOffset
                );
                case LEFT -> (pixel, base) -> new Vector3f(
                        base.z + pixelSizeDiv8 * zone.xSize + overlayOffset + xBodyOffset,
                        base.y + yBodyOffset,
                        (zone.xSize - pixel.x) * pixelSizeDiv8 - (pixel.width * pixelSizeDiv20) - pixelSizeDiv8 * zone.xSize + zBodyOffset
                );
                case TOP -> (pixel, base) -> new Vector3f(
                        base.x + xBodyOffset,
                        base.z + pixelSizeDiv8 * zone.xSize + overlayOffset + yBodyOffset,
                        (zone.ySize - pixel.y) * pixelSizeDiv8 - pixelSizeDiv8 * zone.xSize + zBodyOffset
                );
                case BOTTOM -> (pixel, base) -> new Vector3f(
                        (zone.xSize - pixel.x) * pixelSizeDiv8 - (pixel.width * pixelSizeDiv20) + xBodyOffset,
                        base.z - overlayOffset + yBodyOffset,
                        (zone.xSize - pixel.y) * pixelSizeDiv8 - pixelSizeDiv8 * zone.xSize + zBodyOffset
                );
            };

        }
        else if(zone.bodyPart.equals(BodyPart.BODY)) {
            offsetDefiner = switch (zone.bodyPosition) {
                case FRONT -> (pixel, base) -> new Vector3f(
                        base.x + xBodyOffset,
                        base.y + yBodyOffset,
                        base.z + overlayOffset + zBodyOffset
                );
                case BACK -> (pixel, base) -> new Vector3f(
                        (zone.xSize - pixel.x) * pixelSizeDiv8 - (pixel.width * pixelSizeDiv20) + xBodyOffset,
                        base.y + yBodyOffset,
                        base.z - pixelSizeDiv8 * (zone.xSize/2f) - overlayOffset + zBodyOffset
                );
                case RIGHT -> (pixel, base) -> new Vector3f(
                        base.z - overlayOffset + xBodyOffset,
                        base.y + yBodyOffset,
                        base.x - pixelSizeDiv8 * zone.xSize + zBodyOffset
                );
                case LEFT -> (pixel, base) -> new Vector3f(
                        base.z + pixelSizeDiv8 * (zone.xSize*2) + overlayOffset + xBodyOffset,
                        base.y + yBodyOffset,
                        (zone.xSize - pixel.x) * pixelSizeDiv8 - (pixel.width * pixelSizeDiv20) - pixelSizeDiv8 * zone.xSize + zBodyOffset
                );
                case TOP -> (pixel, base) -> new Vector3f(
                        base.x + xBodyOffset,
                        base.z + pixelSizeDiv8 * (zone.xSize*1.5f) + overlayOffset + yBodyOffset,
                        (zone.ySize - pixel.y) * pixelSizeDiv8 - pixelSizeDiv8 * (zone.xSize/2f) + zBodyOffset
                );
                case BOTTOM -> (pixel, base) -> new Vector3f(
                        (zone.xSize - pixel.x) * pixelSizeDiv8 - (pixel.width * pixelSizeDiv20) + xBodyOffset,
                        base.z - overlayOffset + yBodyOffset,
                        (zone.xSize - pixel.y) * pixelSizeDiv8 - pixelSizeDiv8 * (zone.xSize) + zBodyOffset
                );
            };
        }
        else if(zone.bodyPart.equals(BodyPart.RIGHT_ARM)) {
            offsetDefiner = switch (zone.bodyPosition) {
                case FRONT -> (pixel, base) -> new Vector3f(
                        base.x + xBodyOffset + (0.125f * pixelSize),
                        base.y + yBodyOffset,
                        base.z + overlayOffset + zBodyOffset
                );
                case BACK -> (pixel, base) -> new Vector3f(
                        (zone.xSize - pixel.x) * pixelSizeDiv8 - (pixel.width * pixelSizeDiv20) + xBodyOffset,
                        base.y + yBodyOffset,
                        base.z - pixelSizeDiv8 * (zone.xSize) - overlayOffset + zBodyOffset
                );
                case RIGHT -> (pixel, base) -> new Vector3f(
                        base.z - overlayOffset + xBodyOffset + (0.125f * pixelSize),
                        base.y + yBodyOffset,
                        base.x - pixelSizeDiv8 * zone.xSize + zBodyOffset
                );
                case LEFT -> (pixel, base) -> new Vector3f(
                        base.z + pixelSizeDiv8 * (zone.xSize) + overlayOffset + xBodyOffset,
                        base.y + yBodyOffset,
                        (zone.xSize - pixel.x) * pixelSizeDiv8 - (pixel.width * pixelSizeDiv20) - pixelSizeDiv8 * zone.xSize + zBodyOffset
                );
                case TOP -> (pixel, base) -> new Vector3f(
                        base.x + xBodyOffset + ((0.125f * pixelSize)),
                        base.z + pixelSizeDiv8 * (zone.xSize*3f) + overlayOffset + yBodyOffset,
                        (zone.ySize - pixel.y) * pixelSizeDiv8 - pixelSizeDiv8 * (zone.xSize) + zBodyOffset
                );
                case BOTTOM -> (pixel, base) -> new Vector3f(
                        (zone.xSize - pixel.x) * pixelSizeDiv8 - (pixel.width * pixelSizeDiv20) + xBodyOffset,
                        base.z - overlayOffset + yBodyOffset,
                        (zone.xSize - pixel.y) * pixelSizeDiv8 - pixelSizeDiv8 * (zone.xSize) + zBodyOffset
                );
            };
        }
        else if(zone.bodyPart.equals(BodyPart.LEFT_ARM)) {
            offsetDefiner = switch (zone.bodyPosition) {
                case FRONT -> (pixel, base) -> new Vector3f(
                        base.x + xBodyOffset,
                        base.y + yBodyOffset,
                        base.z + overlayOffset + zBodyOffset
                );
                case BACK -> (pixel, base) -> new Vector3f(
                        (zone.xSize - pixel.x) * pixelSizeDiv8 - (pixel.width * pixelSizeDiv20) + xBodyOffset - (0.125f * pixelSize),
                        base.y + yBodyOffset,
                        base.z - pixelSizeDiv8 * (zone.xSize) - overlayOffset + zBodyOffset
                );
                case RIGHT -> (pixel, base) -> new Vector3f(
                        base.z - overlayOffset + xBodyOffset,
                        base.y + yBodyOffset,
                        base.x - pixelSizeDiv8 * zone.xSize + zBodyOffset
                );
                case LEFT -> (pixel, base) -> new Vector3f(
                        base.z + pixelSizeDiv8 * (zone.xSize) + overlayOffset + xBodyOffset - (0.125f * pixelSize),
                        base.y + yBodyOffset,
                        (zone.xSize - pixel.x) * pixelSizeDiv8 - (pixel.width * pixelSizeDiv20) - pixelSizeDiv8 * zone.xSize + zBodyOffset
                );
                case TOP -> (pixel, base) -> new Vector3f(
                        base.x + xBodyOffset,
                        base.z + pixelSizeDiv8 * (zone.xSize*3f) + overlayOffset + yBodyOffset,
                        (zone.ySize - pixel.y) * pixelSizeDiv8 - pixelSizeDiv8 * (zone.xSize) + zBodyOffset
                );
                case BOTTOM -> (pixel, base) -> new Vector3f(
                        (zone.xSize - pixel.x) * pixelSizeDiv8 - (pixel.width * pixelSizeDiv20) + xBodyOffset - (0.125f * pixelSize),
                        base.z - overlayOffset + yBodyOffset,
                        (zone.xSize - pixel.y) * pixelSizeDiv8 - pixelSizeDiv8 * (zone.xSize) + zBodyOffset
                );
            };
        }
        else if(zone.bodyPart.equals(BodyPart.RIGHT_LEG) || zone.bodyPart.equals(BodyPart.LEFT_LEG)) {
            offsetDefiner = switch (zone.bodyPosition) {
                case FRONT -> (pixel, base) -> new Vector3f(
                        base.x + xBodyOffset,
                        base.y + yBodyOffset,
                        base.z + overlayOffset + zBodyOffset
                );
                case BACK -> (pixel, base) -> new Vector3f(
                        (zone.xSize - pixel.x) * pixelSizeDiv8 - (pixel.width * pixelSizeDiv20) + xBodyOffset,
                        base.y + yBodyOffset,
                        base.z - pixelSizeDiv8 * (zone.xSize) - overlayOffset + zBodyOffset
                );
                case RIGHT -> (pixel, base) -> new Vector3f(
                        base.z - overlayOffset + xBodyOffset,
                        base.y + yBodyOffset,
                        base.x - pixelSizeDiv8 * zone.xSize + zBodyOffset
                );
                case LEFT -> (pixel, base) -> new Vector3f(
                        base.z + pixelSizeDiv8 * (zone.xSize) + overlayOffset + xBodyOffset,
                        base.y + yBodyOffset,
                        (zone.xSize - pixel.x) * pixelSizeDiv8 - (pixel.width * pixelSizeDiv20) - pixelSizeDiv8 * zone.xSize + zBodyOffset
                );
                case TOP -> (pixel, base) -> new Vector3f(
                        base.x + xBodyOffset,
                        base.z + pixelSizeDiv8 * (zone.xSize*3f) + overlayOffset + yBodyOffset,
                        (zone.ySize - pixel.y) * pixelSizeDiv8 - pixelSizeDiv8 * (zone.xSize) + zBodyOffset
                );
                case BOTTOM -> (pixel, base) -> new Vector3f(
                        (zone.xSize - pixel.x) * pixelSizeDiv8 - (pixel.width * pixelSizeDiv20) + xBodyOffset,
                        base.z - overlayOffset + yBodyOffset,
                        (zone.xSize - pixel.y) * pixelSizeDiv8 - pixelSizeDiv8 * (zone.xSize) + zBodyOffset
                );
            };
        }

        return offsetDefiner;
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
