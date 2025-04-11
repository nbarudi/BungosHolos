package ca.bungo.holos.api.holograms;

import ca.bungo.holos.HologramRegistry;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.NotImplementedException;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.joml.AxisAngle4f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.UUID;

/**
 * Generic hologram type that relies on standard Text Display features.
 * Used as a base type for Block, Item, Text, and Entity holograms
 * as all have minimal custom logic required to make them function.
 * <br><br>
 * If implementing your own version. Make sure to handle any data
 * saving with the {@link #onDisable()} method.
 * */
@Getter
@Setter
public abstract class SimpleHologram<T extends Display> implements Hologram {

    @Setter(AccessLevel.PROTECTED)
    protected String uuid;

    private Matrix4f transform;

    @Getter(AccessLevel.PROTECTED)
    @Setter(AccessLevel.NONE)
    private Location location;

    private T display;
    final Class<T> clazz;

    /**
     * Create a new simple hologram
     * @param clazz The Display Entity type this hologram represents
     * */
    public SimpleHologram(Class<T> clazz) {
        uuid = UUID.randomUUID().toString();

        this.transform = new Matrix4f();
        this.clazz = clazz;
        HologramRegistry.registerHologram(this);
    }

    /**
     * Implementable function to allow for modifying unique features about the display entity
     * Useful for creating text on Text Displays
     * Setting the block type for Block Displays
     * Setting the entity for Entity Displays
     * */
    protected abstract void modifyDisplay();

    /**
     * Apply all modified settings to an already spawned Hologram
     * */
    public abstract void redraw();

    /**
     * Method triggered when Hologram is removed
     * This is called after display entity is deleted
     * */
    public abstract void onRemove();

    @Override
    public void onDisable() {
        //ToDo: Save data to a file for this hologram
        throw new NotImplementedException("Not implemented yet");
    }

    @Override
    public String getUniqueIdentifier() {
        return uuid;
    }

    /**
     * Spawn Hologram at supplied location
     * @param location Where to spawn the hologram
     * */
    public void spawn(Location location) {
        this.location = location;
        display = location.getWorld().spawn(location, clazz);
        display.setTransformationMatrix(getTransform());
        modifyDisplay();
    }

    /**
     * Delete the Hologram
     * */
    public void remove() {
        display.remove();
        display = null;
        //Might as well unregister myself if im being removed
        HologramRegistry.unregisterHologram(this);
        onRemove();
    }

    /**
     * Teleport the hologram to supplied location
     * @param location Where to teleport to
     * @return This TextHologram
     * */
    public SimpleHologram<T> teleport(Location location) {
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
    public SimpleHologram<T> translate(Vector3f offset) {
        transform.translate(offset);
        return this;
    }

    /**
     * Scale the hologram by a universal amount
     * @param scale Universal scale for the hologram
     * @return This TextHologram
     * */
    public SimpleHologram<T> scale(float scale) {
        transform.scale(scale);
        return this;
    }

    /**
     * Scale the hologram by a dynamic amount
     * @param scale Universal scale for the hologram
     * @return This TextHologram
     * */
    public SimpleHologram<T> scale(Vector3f scale) {
        transform.scale(scale);
        return this;
    }

    /**
     * Rotate the text display along the Local X Axis.
     * @param angle Angle in Degrees to rotate by
     * @return This TextHologram
     * */
    public SimpleHologram<T> rotateX(float angle) {
        transform.rotate(new AxisAngle4f((float) Math.toRadians(angle), 1, 0, 0));
        return this;
    }

    /**
     * Rotate the text display along the Local Y Axis.
     * @param angle Angle in Degrees to rotate by
     * @return This TextHologram
     * */
    public SimpleHologram<T> rotateY(float angle) {
        transform.rotate(new AxisAngle4f((float) Math.toRadians(angle), 0, 1, 0));
        return this;
    }

    /**
     * Rotate the text display along the Local Z Axis.
     * @param angle Angle in Degrees to rotate by
     * @return This TextHologram
     * */
    public SimpleHologram<T> rotateZ(float angle) {
        transform.rotate(new AxisAngle4f((float) Math.toRadians(angle), 0, 0, 1));
        return this;
    }

}
