package ca.bungo.holos.api.holograms.unique.image;

import ca.bungo.holos.BungosHolos;
import ca.bungo.holos.api.holograms.Hologram;
import ca.bungo.holos.registries.HologramRegistry;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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

    private void buildPixelMap() {
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

    @Override
    public String getUniqueIdentifier() {
        return uuid;
    }

    @Override
    public void onDisable() throws IOException {
        //ToDo: Load file and save data
    }

    @Override
    public void remove() {
        cleanup();
        BungosHolos.get().hologramRegistry.unregisterHologram(this);
        //ToDo: Remove all displays
    }

    @Override
    public void teleport(Location location) {
        this.location = location;
        //ToDo: Teleport all displays
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
}
