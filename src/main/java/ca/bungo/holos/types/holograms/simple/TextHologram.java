package ca.bungo.holos.types.holograms.simple;

import ca.bungo.holos.types.holograms.Hologram;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@Getter
@Setter
public class TextHologram implements Hologram {

    private String text;

    @Getter(AccessLevel.PROTECTED)
    @Setter(AccessLevel.NONE)
    private Location location;

    @Getter(AccessLevel.PROTECTED)
    @Setter(AccessLevel.NONE)
    private TextDisplay display = null;

    private boolean persistent;
    private Display.Billboard billboard = Display.Billboard.FIXED;
    private Color textColor;
    private Color backgroundColor;
    private TextDisplay.TextAlignment textAlignment;


    private Matrix4f transform;


    public TextHologram(String text) {
        this.text = text;

        this.transform = new Matrix4f();

        this.persistent = false;
        this.textColor = Color.WHITE;
        this.backgroundColor = Color.fromARGB(0,0,0,0);
        this.textAlignment = TextDisplay.TextAlignment.CENTER;
    }

    @Override
    public void spawn(Location location) {
        this.location = location;

        display = location.getWorld().spawn(location, TextDisplay.class);
        display.text(Component.text(text, TextColor.color(textColor.asRGB())));
        display.setBackgroundColor(backgroundColor);
        display.setPersistent(persistent);
        display.setAlignment(textAlignment);
        display.setBillboard(billboard);

        display.setTransformationMatrix(transform);
    }

    @Override
    public void redraw() {
        if(display == null) return;
        display.text(Component.text(text, TextColor.color(textColor.asRGB())));
        display.setBackgroundColor(backgroundColor);
        display.setPersistent(persistent);
        display.setTransformationMatrix(transform);
        display.setAlignment(textAlignment);
        display.setBillboard(billboard);
    }

    /**
     * Teleport the hologram to supplied location
     * @param location Where to teleport to
     * @return This TextHologram
     * */
    public TextHologram teleport(Location location) {
        this.location = location;
        if(display == null) return this;
        display.teleport(location);
        return this;
    }

    /**
     * Translate the text display by supplied offset in local space
     * If you want to fully move the display use the {@link #teleport(Location)}
     * @param offset Offset to translate the hologram by in local space
     * @return This TextHologram
     * */
    public TextHologram translate(Vector3f offset) {
        transform.translate(offset);
        return this;
    }

    /**
     * Scale the hologram by a universal amount
     * @param scale Universal scale for the hologram
     * @return This TextHologram
     * */
    public TextHologram scale(float scale) {
        transform.scale(scale);
        return this;
    }

    /**
     * Scale the hologram by a dynamic amount
     * @param scale Universal scale for the hologram
     * @return This TextHologram
     * */
    public TextHologram scale(Vector3f scale) {
        transform.scale(scale);
        return this;
    }

    /**
     * Rotate the text display along the Local X Axis.
     * @param angle Angle in Degrees to rotate by
     * @return This TextHologram
     * */
    public TextHologram rotateX(float angle) {
        transform.rotate(new AxisAngle4f((float) Math.toRadians(angle), 1, 0, 0));
        return this;
    }

    /**
     * Rotate the text display along the Local Y Axis.
     * @param angle Angle in Degrees to rotate by
     * @return This TextHologram
     * */
    public TextHologram rotateY(float angle) {
        transform.rotate(new AxisAngle4f((float) Math.toRadians(angle), 0, 1, 0));
        return this;
    }

    /**
     * Rotate the text display along the Local Z Axis.
     * @param angle Angle in Degrees to rotate by
     * @return This TextHologram
     * */
    public TextHologram rotateZ(float angle) {
        transform.rotate(new AxisAngle4f((float) Math.toRadians(angle), 0, 0, 1));
        return this;
    }


    @Override
    public void remove() {
        display.remove();
        display = null;
    }
}
