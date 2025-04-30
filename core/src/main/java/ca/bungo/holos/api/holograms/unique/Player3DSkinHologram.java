package ca.bungo.holos.api.holograms.unique;

import ca.bungo.holos.BungosHolos;
import ca.bungo.holos.api.animations.Animation;
import ca.bungo.holos.api.holograms.*;
import ca.bungo.holos.utility.ComponentUtility;
import ca.bungo.holos.utility.NetworkUtility;
import ca.bungo.holos.utility.PixelUtility;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;

@Getter
@Setter
public class Player3DSkinHologram implements Hologram, ConfigurationSerializable, Editable, Animatable {

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
    float pixelSize = 0.75f;
    private boolean isSlim = false;

    private List<TextDisplay> displays = new ArrayList<>();

    private boolean loaded = false;
    private boolean loadAttempted = false;

    private Animation animation;
    private boolean playing;
    private BukkitTask animationTask;

    private Display.Billboard billboard = Display.Billboard.FIXED;

    private Map<SkinZone, List<Pixel>> zonePixels = new HashMap<>();
    private Map<BodyPart, List<TextDisplay>> perLimbDisplays = new HashMap<>();
    private Map<String, List<Pixel>> playerPixels = new HashMap<>();

    public Player3DSkinHologram(String playerUUID) {
        uuid = UUID.randomUUID().toString();
        this.playerUUID = playerUUID;
        this.location = null;

        BungosHolos.get().hologramRegistry.registerHologram(this);

        loadPlayerSkin();
    }


    private void loadPlayerSkin() {
        try {
            NetworkUtility.getPlayerSkin(playerUUID).thenAccept(skin -> {
                isSlim = skin[55][40].getAlpha() == 0;

                for (SkinZone zone : skinZones) {
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
            BungosHolos.LOGGER.warn("Failed to get player pixel map for {}", uuid, e);
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
        spawn3DHologram(zonePixels);
    }

    private void spawn3DHologram(Map<SkinZone, List<Pixel>> data) {
        cleanup();
        for(Map.Entry<SkinZone, List<Pixel>> entry : data.entrySet()) {
            SkinZone zone = entry.getKey();
            List<Pixel> pixels = entry.getValue();


            OffsetDefiner offsetDefiner = isSlim ? calculateSlimSkin(zone) : calculateClassicSkin(zone);
            spawnPixels(pixels, zone.bodyPosition.rotation, offsetDefiner, zone.xSize, zone.bodyPart);
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

    private void spawnPixels(List<Pixel> toSpawn, Vector2f rotation, OffsetDefiner offsetDefiner, int rowSize, BodyPart bodyPart) {
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
                        perLimbDisplays.getOrDefault(bodyPart, new ArrayList<>()).add(display);
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
                        display.setBillboard(billboard);
                    });
                    index++;
                }
                if(index >= toSpawn.size()) {
                    this.cancel();
                }

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
        if(this.getUniqueIdentifier() != null) {
            holograms.set(this.getUniqueIdentifier(), this);
        }
        config.save(file);
        for(Display display : displays) {
            display.setPersistent(false);
        }
        cleanup();
    }

    @Override
    public void remove() {
        cleanup();
        BungosHolos.get().hologramRegistry.unregisterHologram(this);
        loaded = false;
        loadAttempted = false;
        displays.clear();
        zonePixels.clear();
    }

    public void setDisplayMode(Display.Billboard billboard) {
        this.billboard = billboard;
        for(TextDisplay display : getDisplays()){
            display.setBillboard(billboard);
        }
    }

    @Override
    public void teleport(Location location) {
        this.location = location;
        for(TextDisplay display : displays) {
            display.teleport(location);
        }
    }

    public void teleport(Location location, boolean update) {
        if(update) this.location = location;
        for(TextDisplay display : displays) {
            display.teleport(location);
        }
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> result = new HashMap<>();
        result.put("uuid", uuid);
        result.put("player_uuid", playerUUID);
        result.put("location", location);
        result.put("pixel_size", pixelSize);
        result.put("billboard", billboard.name());
        if (BungosHolos.get().hologramRegistry.fetchAlias(this.getUniqueIdentifier()) != null) {
            result.put("alias", BungosHolos.get().hologramRegistry.fetchAlias(this.getUniqueIdentifier()));
        }
        return result;
    }

    public static Player3DSkinHologram deserialize(Map<String, Object> data){
        Location location = (Location) data.get("location");
        String uuid = (String) data.get("uuid");
        String playerUUID = (String) data.get("player_uuid");
        float pixelSize = (float) ((double)data.get("pixel_size"));
        Player3DSkinHologram hologram = new Player3DSkinHologram(playerUUID);
        String billboard = (String) data.get("billboard");
        BungosHolos.get().hologramRegistry.unregisterHologram(hologram);
        hologram.setPixelSize(pixelSize);
        hologram.setUuid(uuid);
        hologram.setLocation(location);
        hologram.setBillboard(Display.Billboard.valueOf(billboard));
        String alias = (String) data.get("alias");
        if(alias != null) {
            BungosHolos.get().hologramRegistry.defineAlias(hologram.getUniqueIdentifier(), alias, true);
        }

        if(location != null && !BungosHolos.DISABLED) hologram.spawn(hologram.getLocation());

        return hologram;
    }

    @Override
    public boolean onEdit(@NotNull Player editor, @Nullable String field, String... values) {
        String editMessage = """
                &eHere are the fields that you're able to edit for this 3D Player Hologram:
                <hover:show_text:'&eClick to edit field'><click:suggest_command:'/holo edit player UUID/Name'>&bplayer UUID/Name &e- What skin should this be
                <hover:show_text:'&eClick to edit field'><click:suggest_command:'/holo edit billboard Billboard'>&bbillboard Billboard Type &e- Set billboard type for the hologram
                <hover:show_text:'&eClick to edit field'><click:suggest_command:'/holo edit yaw VALUE'>&byaw Number &e- Set the yaw of the hologram
                <hover:show_text:'&eClick to edit field'><click:suggest_command:'/holo edit pitch VALUE'>&bpitch Number &e- Set the pitch of the hologram
                <hover:show_text:'&eClick to edit field'><click:suggest_command:'/holo edit size VALUE'>&bsize Number &e- Set the size of the hologram (0.5 default)""";
        if(field == null || field.isEmpty()){
            editor.sendMessage(ComponentUtility.convertToComponent(editMessage));
            return true;
        }

        boolean successful = false;
        switch(field.toLowerCase()){
            case "player" -> {
                if(values.length == 0) {
                    editor.sendMessage(ComponentUtility.convertToComponent("&cYou must specify a player UUID!"));
                    return false;
                }
                String who = values[0];

                if(who.length() == 36) {
                    setPlayerUUID(who);
                    loaded = false;
                    loadPlayerSkin();
                    spawn(this.getLocation());
                    return true;
                }
                else {
                    Player target = Bukkit.getPlayer(who);
                    if(target == null) {
                        editor.sendMessage(ComponentUtility.convertToComponent("&cThat player doesn't exist!"));
                        return false;
                    }
                    this.setPlayerUUID(target.getUniqueId().toString());
                    loaded = false;
                    loadPlayerSkin();
                    spawn(this.getLocation());
                    successful = true;
                }
            }
            case "billboard" -> {
                if(values.length == 0) {
                    editor.sendMessage(ComponentUtility.convertToComponent("&cYou must specify a billboard!"));
                    return false;
                }
                try {
                    Display.Billboard billboard = Display.Billboard.valueOf(values[0].toUpperCase());
                    this.setDisplayMode(billboard);
                    successful = true;
                    editor.sendMessage(ComponentUtility.convertToComponent("&aSuccessfully set billboard to &e" + values[0]));
                } catch (IllegalArgumentException e) {
                    editor.sendMessage(ComponentUtility.convertToComponent("&cInvalid billboard!"));
                    return true;
                }
            }
            case "yaw" -> {
                if(values.length == 0) {
                    editor.sendMessage(ComponentUtility.convertToComponent("&cYou must specify a yaw!"));
                    return false;
                }
                Location location = this.getLocation().clone();
                try {
                    float yaw = Float.parseFloat(values[0]);
                    location.setYaw(yaw);
                    this.teleport(location);
                    successful = true;
                    editor.sendMessage(ComponentUtility.convertToComponent("&aSuccessfully set yaw to &e" + yaw));
                } catch (NumberFormatException e){
                    editor.sendMessage(ComponentUtility.convertToComponent("&cInvalid number format!"));
                    return true;
                }
            }
            case "pitch" -> {
                if(values.length == 0) {
                    editor.sendMessage(ComponentUtility.convertToComponent("&cYou must specify a pitch!"));
                    return false;
                }
                Location location = this.getLocation().clone();
                try {
                    float pitch = Float.parseFloat(values[0]);
                    location.setPitch(pitch);
                    this.teleport(location);
                    successful = true;
                    editor.sendMessage(ComponentUtility.convertToComponent("&aSuccessfully set pitch to &e" + pitch));
                } catch (NumberFormatException e){
                    editor.sendMessage(ComponentUtility.convertToComponent("&cInvalid number format!"));
                    return true;
                }
            }
            case "size" -> {
                if(values.length == 0) {
                    editor.sendMessage(ComponentUtility.convertToComponent("&cYou must specify a scale!"));
                    return false;
                }
                float size = Float.parseFloat(values[0]);
                this.setPixelSize(size);
                spawn(this.getLocation());
                successful = true;
                editor.sendMessage(ComponentUtility.convertToComponent("&aSuccessfully set size to &e" + size));
            }
            case "animation" -> {
                if(values.length == 0){
                    editor.sendMessage(Component.text("You must supply a value!", NamedTextColor.RED));
                    break;
                }

                if(animation == null || !(animation instanceof Editable editable)){
                    editor.sendMessage(Component.text("This hologram animation does not support editing!", NamedTextColor.RED));
                    break;
                }

                String argument = values[0];
                if(argument.equalsIgnoreCase("play")){
                    playAnimation();
                    successful = true;
                    editor.sendMessage(Component.text("Started the animation!", NamedTextColor.YELLOW));
                }
                else if(argument.equalsIgnoreCase("stop")){
                    stopAnimation();
                    successful = true;
                    editor.sendMessage(Component.text("Stopped the animation!", NamedTextColor.YELLOW));
                }
                else if(argument.equalsIgnoreCase("toggle")){
                    toggleAnimation();
                    successful = true;
                    editor.sendMessage(Component.text("Toggled the animation!", NamedTextColor.YELLOW));
                }
                else {
                    successful = editable.onEdit(editor, argument, Arrays.copyOfRange(values, 1, values.length));
                }
            }
        }

        return successful;
    }

    @Override
    public List<String> options(String option) {
        return switch(option.toLowerCase()) {
            case "player" -> List.of("<uuid/name>");
            case "yaw", "pitch", "size" -> List.of("<number>");
            case "animation" -> {
                if(animation != null && animation instanceof Editable editable){
                    List<String> editableOptions = new ArrayList<>(editable.fields());
                    editableOptions.add("toggle");
                    editableOptions.add("stop");
                    editableOptions.add("play");
                    editableOptions.add("remove");
                    yield editableOptions;
                }
                yield List.of("");
            }
            case "billboard" -> Arrays.stream(Display.Billboard.values()).map(Enum::name).toList();
            default -> List.of();
        };
    }

    @Override
    public List<String> fields() {
        return List.of(
                "player",
                "yaw",
                "pitch",
                "size",
                "animation",
                "billboard"
        );
    }

    @Override
    public void loadAnimation(Animation animation) {
        this.animation = animation.clone();
    }


    @Override
    public void playAnimation() {
        if(animation == null) return;
        if(animationTask != null) stopAnimation();

        animationTask = new BukkitRunnable() {
            private int progress = 0;

            public void run() {
                if(progress >= animation.getTickTime() && animation.isLoopable()) progress = 0;
                if(playing) return;

                Vector3f offset = animation.getPositionOffset(progress);
                Vector2f rotation = animation.getRotationOffset(progress);
                Location newLocation = getLocation().clone().add(new Vector(offset.x, offset.y, offset.z));
                newLocation.setYaw(newLocation.getYaw() + rotation.x);
                newLocation.setPitch(newLocation.getPitch() + rotation.y);

                teleport(newLocation, false);

                progress++;
            }
        }.runTaskTimer(BungosHolos.get(), 1, 1);
    }

    @Override
    public void stopAnimation() {
        if(animationTask != null) {
            animationTask.cancel();
            animationTask = null;
            teleport(getLocation(), false);
            return;
        }
    }

    @Override
    public void toggleAnimation() {
        playing = !playing;
    }


}
