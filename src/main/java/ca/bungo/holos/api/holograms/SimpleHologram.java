package ca.bungo.holos.api.holograms;

import ca.bungo.holos.HologramRegistry;
import ca.bungo.holos.utility.ComponentUtility;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.lang.NotImplementedException;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
public abstract class SimpleHologram<T extends Display> implements Hologram, Editable {

    @Setter(AccessLevel.PROTECTED)
    protected String uuid;

    private Matrix4f transform;

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
     * */
    public void teleport(Location location) {
        this.location = location;
        if(display == null) return;
        display.teleport(location);
    }

    /**
     * Set the yaw of the display entity
     * @param yaw What to set the yaw to
     * */
    public void setYaw(float yaw) {
        this.location.setYaw(yaw);
        this.display.teleport(location);
    }

    /**
     * Set the pitch of the display entity
     * @param pitch What to set the pitch to
     * @return This SimpleHologram
     * */
    public void setPitch(float pitch) {
        this.location.setPitch(pitch);
        this.display.teleport(location);
    }

    /**
     * Translate the text display by supplied offset in local space
     * If you want to fully move the display use the {@link #teleport(Location)}
     * @param offset Offset to translate the hologram by in local space
     * */
    public void translate(Vector3f offset) {
        transform.translate(offset);
    }

    /**
     * Scale the hologram by a universal amount
     * @param scale Universal scale for the hologram
     * */
    public void scale(float scale) {
        transform.scale(scale);
    }

    /**
     * Scale the hologram by a dynamic amount
     * @param scale Universal scale for the hologram
     * */
    public void scale(Vector3f scale) {
        transform.scale(scale);
    }

    /**
     * Rotate the text display along the Local X Axis.
     *
     * @param angle Angle in Degrees to rotate by
     */
    public void rotateX(float angle) {
        transform.rotate(new AxisAngle4f((float) Math.toRadians(angle), 1, 0, 0));
    }

    /**
     * Rotate the text display along the Local Y Axis.
     *
     * @param angle Angle in Degrees to rotate by
     */
    public void rotateY(float angle) {
        transform.rotate(new AxisAngle4f((float) Math.toRadians(angle), 0, 1, 0));
    }

    /**
     * Rotate the text display along the Local Z Axis.
     *
     * @param angle Angle in Degrees to rotate by
     */
    public void rotateZ(float angle) {
        transform.rotate(new AxisAngle4f((float) Math.toRadians(angle), 0, 0, 1));
    }

    @Override
    public boolean onEdit(@NotNull Player editor, @Nullable String field, String... values) {
        if(field == null){
            String editMessage = ComponentUtility.format(
                    """
                    &eHere are the fields that you're able to edit for Simple Holograms:
                    <hover:show_text:'&eClick to edit field'><click:suggest_command:'/holo edit yaw VALUE'>&byaw Number &e- Set the yaw of the hologram
                    <hover:show_text:'&eClick to edit field'><click:suggest_command:'/holo edit pitch VALUE'>&bpitch Number &e- Set the pitch of the hologram
                    <hover:show_text:'&eClick to edit field'><click:suggest_command:'/holo edit scale VALUE'>&bscale Number &e- Scale the hologram by value (Multiplies does not set)
                    <hover:show_text:'&eClick to edit field'><click:suggest_command:'/holo edit scale x y z'>&bscale Number Number Number &e- Scale the hologram x,y,z
                    <hover:show_text:'&eClick to edit field'><click:suggest_command:'/holo edit rotatex VALUE'>&brotatex Number &e- Rotate along the local X (Adds, does not set)
                    <hover:show_text:'&eClick to edit field'><click:suggest_command:'/holo edit rotatey VALUE'>&brotatey Number &e- Rotate along the local Y (Adds, does not set)
                    <hover:show_text:'&eClick to edit field'><click:suggest_command:'/holo edit rotatez VALUE'>&brotatez Number &e- Rotate along the local Z (Adds, does not set)""");
            editor.sendMessage(ComponentUtility.convertToComponent(editMessage));
            return true;
        }

        boolean succeeded = false;
        switch (field.toLowerCase()){
            case "scale":
                if(values.length == 1){
                    try {
                        float scale = Float.parseFloat(values[0]);
                        this.scale(scale);
                        succeeded = true;
                        this.redraw();
                        editor.sendMessage(Component.text("Scaled the hologram by " + scale, NamedTextColor.YELLOW));
                        break;
                    } catch (NumberFormatException e) {
                        editor.sendMessage(Component.text("Invalid number!", NamedTextColor.RED));
                    }
                }
                else if (values.length == 3){
                    try {
                        float x = Float.parseFloat(values[0]);
                        float y = Float.parseFloat(values[0]);
                        float z = Float.parseFloat(values[0]);
                        this.scale(new Vector3f(x, y, z));
                        succeeded = true;
                        this.redraw();
                        editor.sendMessage(Component.text("Scaled the hologram by " + x + ", " + y + ", " + z, NamedTextColor.YELLOW));
                        break;
                    } catch (NumberFormatException e) {
                        editor.sendMessage(Component.text("Invalid number!", NamedTextColor.RED));
                    }
                }
                else {
                    editor.sendMessage(Component.text("Bad arguments", NamedTextColor.RED));
                    break;
                }
                break;
            case "yaw":
                if(values.length == 0){
                    editor.sendMessage(Component.text("You must supply a number!", NamedTextColor.RED));
                    break;
                }
                try {
                    float value = Float.parseFloat(values[0]);
                    this.setYaw(value);
                    editor.sendMessage(Component.text("Set the yaw of the hologram to " + value, NamedTextColor.YELLOW));
                    succeeded = true;
                } catch (NumberFormatException e) {
                    editor.sendMessage(Component.text("Invalid number!", NamedTextColor.RED));
                }
                break;
            case "pitch":
                if(values.length == 0){
                    editor.sendMessage(Component.text("You must supply a number!", NamedTextColor.RED));
                    break;
                }
                try {
                    float value = Float.parseFloat(values[0]);
                    this.setPitch(value);
                    editor.sendMessage(Component.text("Set the pitch of the hologram to " + value, NamedTextColor.YELLOW));
                    succeeded = true;
                } catch (NumberFormatException e) {
                    editor.sendMessage(Component.text("Invalid number!", NamedTextColor.RED));
                }
                break;
            case "rotatex":
                if(values.length == 0){
                    editor.sendMessage(Component.text("You must supply a number!", NamedTextColor.RED));
                    break;
                }
                try {
                    float rotateX = Float.parseFloat(values[0]);
                    this.rotateX(rotateX);
                    this.redraw();
                    editor.sendMessage(Component.text("Rotated " + rotateX + " degrees along the X axis!", NamedTextColor.YELLOW));
                    succeeded = true;
                } catch (NumberFormatException e) {
                    editor.sendMessage(Component.text("Invalid number!", NamedTextColor.RED));
                }
                break;
            case "rotatey":
                if(values.length == 0){
                    editor.sendMessage(Component.text("You must supply a number!", NamedTextColor.RED));
                    break;
                }
                try {
                    float rotateY = Float.parseFloat(values[0]);
                    this.rotateY(rotateY);
                    this.redraw();
                    editor.sendMessage(Component.text("Rotated " + rotateY + " degrees along the Y axis!", NamedTextColor.YELLOW));
                    succeeded = true;
                } catch (NumberFormatException e) {
                    editor.sendMessage(Component.text("Invalid number!", NamedTextColor.RED));
                }
                break;
            case "rotatez":
                if(values.length == 0){
                    editor.sendMessage(Component.text("You must supply a number!", NamedTextColor.RED));
                    break;
                }
                try {
                    float rotateZ = Float.parseFloat(values[0]);
                    this.rotateZ(rotateZ);
                    this.redraw();
                    editor.sendMessage(Component.text("Rotated " + rotateZ + " degrees along the Z axis!", NamedTextColor.YELLOW));
                    succeeded = true;
                } catch (NumberFormatException e) {
                    editor.sendMessage(Component.text("Invalid number!", NamedTextColor.RED));
                }
                break;
        }
        return succeeded;
    }
}
