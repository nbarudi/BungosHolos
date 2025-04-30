package ca.bungo.holos.api.holograms.unique.image;

import ca.bungo.holos.BungosHolos;
import ca.bungo.holos.api.animations.Animation;
import ca.bungo.holos.api.holograms.Animatable;
import ca.bungo.holos.api.holograms.Editable;
import ca.bungo.holos.api.holograms.Hologram;
import ca.bungo.holos.api.holograms.SimpleHologram;
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
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;

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

        cleanup();
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

    @Getter
    @Setter
    public static class Player2DSkinHologram extends ImageHologram implements ConfigurationSerializable, Editable, Animatable {

        public enum HologramType {
            HEAD,FULL
        }

        private String playerUUID;
        private HologramType type;

        private boolean playing = false;
        private Animation animation;
        private BukkitTask animationTask;


        private boolean isLoaded = false;
        private boolean loadAttempted = false;

        public Player2DSkinHologram(@NotNull String playerUUID, @NotNull HologramType type) {
            super(new Color[0][0]);

            this.playerUUID = playerUUID;
            this.type = type;
            BungosHolos.get().hologramRegistry.registerHologram(this);
            loadPlayerSkin();
        }

        private void loadPlayerSkin() {
            try {
                NetworkUtility.getPlayerSkin(this.playerUUID).thenAccept(skin -> {
                    this.setColors(skin);
                    boolean isSlim = skin[55][40].getAlpha() == 0;

                    if(type == HologramType.HEAD) {
                        Color[][] head = PixelUtility.extractRegion(skin, 8, 48, 8, 8);
                        this.setColors(head);
                    }
                    else if(type == HologramType.FULL) {
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

        @Override
        public void spawn(Location location) {
            this.setLocation(location);
            if(!isLoaded) {
                loadAttempted = true;
                return;
            }
            super.spawn(location);
        }

        @Override
        public @NotNull Map<String, Object> serialize() {
            Map<String, Object> result = new HashMap<>();
            result.put("location", this.getLocation());
            result.put("uuid", this.getUniqueIdentifier());
            result.put("player_uuid", this.getPlayerUUID());
            result.put("pixel_size", this.getPixelSize());
            result.put("type", this.getType().name());
            return result;
        }

        public static Player2DSkinHologram deserialize(@NotNull Map<String, Object> args) {
            Location location = (Location) args.get("location");
            String uuid = (String) args.get("uuid");
            String playerUUID = (String) args.get("player_uuid");
            float pixelSize = (float) ((double)args.get("pixel_size"));
            HologramType type = HologramType.valueOf((String) args.get("type"));

            Player2DSkinHologram hologram = new Player2DSkinHologram(playerUUID, type);
            BungosHolos.get().hologramRegistry.unregisterHologram(hologram);
            hologram.setUuid(uuid);
            hologram.setPixelSize(pixelSize);
            hologram.setLocation(location);
            if(!BungosHolos.DISABLED && location != null) hologram.spawn(location);
            return hologram;
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


        @Override
        public boolean onEdit(@NotNull Player editor, @Nullable String field, String... values) {
            String editMessage = """
                &eHere are the fields that you're able to edit for this 3D Player Hologram:
                <hover:show_text:'&eClick to edit field'><click:suggest_command:'/holo edit player UUID/Name'>&bplayer UUID/Name &e- What skin should this be
                <hover:show_text:'&eClick to edit field'><click:suggest_command:'/holo edit type HEAD/FULL'>&btype Head/Type &e- What hologram type should be used
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
                        isLoaded = false;
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
                        isLoaded = false;
                        loadPlayerSkin();
                        spawn(this.getLocation());
                        successful = true;
                    }
                }
                case "type" -> {
                    if(values.length == 0) {
                        editor.sendMessage(ComponentUtility.convertToComponent("&cYou must specify a type!"));
                        return false;
                    }
                    try {
                        HologramType type = HologramType.valueOf(values[0].toUpperCase());
                        this.setType(type);
                        isLoaded = false;
                        loadPlayerSkin();
                        spawn(this.getLocation());
                        successful = true;
                        editor.sendMessage(ComponentUtility.convertToComponent("&aSuccessfully set type to &e" + values[0]));
                    } catch (IllegalArgumentException e) {
                        editor.sendMessage(ComponentUtility.convertToComponent("&cInvalid type!"));
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
                case "type" -> List.of(HologramType.HEAD.name(), HologramType.FULL.name());
                default -> List.of();
            };
        }

        @Override
        public List<String> fields() {
            return List.of(
                    "player",
                    "type",
                    "yaw",
                    "pitch",
                    "size",
                    "animation"
            );
        }
    }

}
