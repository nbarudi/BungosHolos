package ca.bungo.holos.api.holograms;

import ca.bungo.holos.HologramRegistry;
import ca.bungo.holos.utility.ComponentUtility;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public abstract class SimpleHologram<T extends Display> implements Hologram, Editable, ConfigurationSerializable {

    @Setter(AccessLevel.PROTECTED)
    protected String uuid;

    private Transformation transform;

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

        this.transform = new Transformation(
                new Vector3f(0,0,0),
                new Quaternionf(0,0,0,1),
                new Vector3f(1,1,1),
                new Quaternionf(0,0,0,1)
        );
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
    public void redraw() {
        if(display == null) return;
        display.setTransformation(transform);
        modifyDisplay();
    }

    /**
     * Method triggered when Hologram is removed
     * This is called after display entity is deleted
     * */
    public abstract void onRemove();

    public abstract void saveUniqueContent(Map<String, Object> map);

    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();

        Map<String, Object> transformationMap = new HashMap<>();

        Map<String, Object> translate = new HashMap<>();
        translate.put("x", transform.getTranslation().x);
        translate.put("y", transform.getTranslation().y);
        translate.put("z", transform.getTranslation().z);

        AxisAngle4f leftRot = new AxisAngle4f(transform.getLeftRotation());
        Map<String, Object> leftRotation = new HashMap<>();
        leftRotation.put("x", leftRot.x);
        leftRotation.put("y", leftRot.y);
        leftRotation.put("z", leftRot.z);
        leftRotation.put("w", leftRot.angle);

        Map<String, Object> scale = new HashMap<>();
        scale.put("x", transform.getScale().x);
        scale.put("y", transform.getScale().y);
        scale.put("z", transform.getScale().z);

        AxisAngle4f rightRot = new AxisAngle4f(transform.getRightRotation());
        Map<String, Object> rightRotation = new HashMap<>();
        rightRotation.put("x", rightRot.x);
        rightRotation.put("y", rightRot.y);
        rightRotation.put("z", rightRot.z);
        rightRotation.put("w", rightRot.angle);


        transformationMap.put("translate", translate);
        transformationMap.put("left_rotation", leftRotation);
        transformationMap.put("scale", scale);
        transformationMap.put("right_rotation", rightRotation);

        map.put("transform", transformationMap);

        map.put("location", location);
        map.put("uuid", uuid);
        if (HologramRegistry.fetchAlias(this.getUniqueIdentifier()) != null) {
            map.put("alias", HologramRegistry.fetchAlias(this.getUniqueIdentifier()));
        }

        Map<String, Object> uniqueData = new HashMap<>();
        saveUniqueContent(uniqueData);

        map.put("unique_data", uniqueData);
        return map;
    }

    protected void deserializeGeneric(Map<String, Object> map) {
        Map<String, Map<String, Object>> _transformation = (Map<String, Map<String, Object>>) map.get("transform");
        Map<String, Object> _translate = _transformation.get("translate");
        Map<String, Object> _leftRotation = _transformation.get("left_rotation");
        Map<String, Object> _scale = _transformation.get("scale");
        Map<String, Object> _rightRotation = _transformation.get("right_rotation");

        Vector3f translate = new Vector3f(
                (float)((double)_translate.get("x")),
                (float)((double)_translate.get("y")),
                (float)((double)_translate.get("z"))
        );

        AxisAngle4f leftRotation = new AxisAngle4f(
                (float)((double)_leftRotation.get("w")),
                (float)((double)_leftRotation.get("x")),
                (float)((double)_leftRotation.get("y")),
                (float)((double)_leftRotation.get("z"))
        );

        Vector3f scale = new Vector3f(
                (float)((double)_scale.get("x")),
                (float)((double)_scale.get("y")),
                (float)((double)_scale.get("z"))
        );

        AxisAngle4f rightRotation = new AxisAngle4f(
                (float)((double)_rightRotation.get("w")),
                (float)((double)_rightRotation.get("x")),
                (float)((double)_rightRotation.get("y")),
                (float)((double)_rightRotation.get("z"))
        );

        setOffset(translate);
        transform.getLeftRotation().set(leftRotation);
        scale(scale);
        transform.getRightRotation().set(rightRotation);

        this.location = (Location)map.get("location");
        this.setUuid((String) map.get("uuid"));

        String alias = (String) map.get("alias");
        if(alias != null) {
            HologramRegistry.defineAlias(this.getUniqueIdentifier(), alias);
        }
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
        if(display != null) display.setPersistent(false);
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
        display.setTransformation(transform);
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
     * */
    public void setPitch(float pitch) {
        this.location.setPitch(pitch);
        this.display.teleport(location);
    }

    /**
     * Offset the text display by supplied vector in local space
     * If you want to fully move the display use the {@link #teleport(Location)}
     * @param offset Offset to translate the hologram by in local space
     * */
    public void setOffset(Vector3f offset) {
        transform.getTranslation().set(offset);
    }

    /**
     * Offset the text display by supplied vector in local space
     * If you want to fully move the display use the {@link #teleport(Location)}
     * @param offset Offset to translate the hologram by in local space
     * */
    public void addOffset(Vector3f offset) {
        transform.getTranslation().add(offset);
    }

    /**
     * Scale the hologram by a universal amount
     * @param scale Universal scale for the hologram
     * */
    public void scale(float scale) {
        transform.getScale().mul(scale);
    }

    /**
     * Scale the hologram by a dynamic amount
     * @param scale Universal scale for the hologram
     * */
    public void scale(Vector3f scale) {
        transform.getScale().mul(scale);
    }

    /**
     * Set the hologram's size to supplied value
     * @param size Size to set the holograms scale to
     * */
    public void setSize(float size) {
        transform.getScale().set(size);
    }

    /**
     * Set the hologram's size to supplied value
     * @param size Size to set the holograms scale to
     * */
    public void setSize(Vector3f size) {
        transform.getScale().set(size);
    }

    /**
     * Rotate the text display along the Local X Axis.
     *
     * @param angle Angle in Degrees to rotate by
     */
    public void rotateX(float angle) {
        AxisAngle4f axisAngle = new AxisAngle4f((float)Math.toRadians(angle), 1, 0, 0);
        transform.getLeftRotation().premul(new Quaternionf(axisAngle));
    }

    /**
     * Rotate the text display along the Local Y Axis.
     *
     * @param angle Angle in Degrees to rotate by
     */
    public void rotateY(float angle) {
        AxisAngle4f axisAngle = new AxisAngle4f((float)Math.toRadians(angle), 0, 1, 0);
        transform.getLeftRotation().premul(new Quaternionf(axisAngle));
    }

    /**
     * Rotate the text display along the Local Z Axis.
     *
     * @param angle Angle in Degrees to rotate by
     */
    public void rotateZ(float angle) {
        AxisAngle4f axisAngle = new AxisAngle4f((float)Math.toRadians(angle), 0, 0, 1);
        transform.getLeftRotation().premul(new Quaternionf(axisAngle));
    }

    @Override
    public boolean onEdit(@NotNull Player editor, @Nullable String field, String... values) {
        if(field == null){
            String editMessage = ComponentUtility.format(
                    """
                    &dHere are the fields that you're able to edit for Simple Holograms:
                    <hover:show_text:'&eClick to edit field'><click:suggest_command:'/holo edit yaw VALUE'>&byaw Number &e- Set the yaw of the hologram
                    <hover:show_text:'&eClick to edit field'><click:suggest_command:'/holo edit pitch VALUE'>&bpitch Number &e- Set the pitch of the hologram
                    <hover:show_text:'&eClick to edit field'><click:suggest_command:'/holo edit scale VALUE'>&bscale Number &e- Scale the hologram by value (Multiplies does not set)
                    <hover:show_text:'&eClick to edit field'><click:suggest_command:'/holo edit scale x y z'>&bscale Number Number Number &e- Scale the hologram x,y,z
                    <hover:show_text:'&eClick to edit field'><click:suggest_command:'/holo edit size x y z'>&bsize Number Number Number &e- Set the hologram's x,y,z size
                    <hover:show_text:'&eClick to edit field'><click:suggest_command:'/holo edit size VALUE'>&bsize Number &e- Set the hologram's size
                    <hover:show_text:'&eClick to edit field'><click:suggest_command:'/holo edit rotatex VALUE'>&brotatex Number &e- Rotate along the local X (Adds, does not set)
                    <hover:show_text:'&eClick to edit field'><click:suggest_command:'/holo edit rotatey VALUE'>&brotatey Number &e- Rotate along the local Y (Adds, does not set)
                    <hover:show_text:'&eClick to edit field'><click:suggest_command:'/holo edit rotatez VALUE'>&brotatez Number &e- Rotate along the local Z (Adds, does not set)
                    <hover:show_text:'&eClick to edit field'><click:suggest_command:'/holo edit offsetx VALUE'>&boffsetx Number &e- Offset along the local X (Adds, does not set)
                    <hover:show_text:'&eClick to edit field'><click:suggest_command:'/holo edit offsety VALUE'>&boffsety Number &e- Offset along the local Y (Adds, does not set)
                    <hover:show_text:'&eClick to edit field'><click:suggest_command:'/holo edit offsetz VALUE'>&boffsetz Number &e- Offset along the local Z (Adds, does not set)
                    <hover:show_text:'&eClick to edit field'><click:suggest_command:'/holo edit setoffset x y z'>&bsetoffset Number Number Number &e- Offset to set position (Set's Offset)""");
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
                        float y = Float.parseFloat(values[1]);
                        float z = Float.parseFloat(values[2]);
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
            case "size":
                if(values.length == 1){
                    try {
                        float scale = Float.parseFloat(values[0]);
                        this.setSize(scale);
                        succeeded = true;
                        this.redraw();
                        editor.sendMessage(Component.text("Set the hologram's size to " + scale, NamedTextColor.YELLOW));
                        break;
                    } catch (NumberFormatException e) {
                        editor.sendMessage(Component.text("Invalid number!", NamedTextColor.RED));
                    }
                }
                else if (values.length == 3){
                    try {
                        float x = Float.parseFloat(values[0]);
                        float y = Float.parseFloat(values[1]);
                        float z = Float.parseFloat(values[2]);
                        this.setSize(new Vector3f(x, y, z));
                        succeeded = true;
                        this.redraw();
                        editor.sendMessage(Component.text("Set the hologram's size to " + x + ", " + y + ", " + z, NamedTextColor.YELLOW));
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
            case "offsetx":
                if(values.length == 0){
                    editor.sendMessage(Component.text("You must supply a number!", NamedTextColor.RED));
                    break;
                }
                try {
                    float offsetX = Float.parseFloat(values[0]);
                    this.addOffset(new Vector3f(offsetX, 0, 0));
                    this.redraw();
                    editor.sendMessage(Component.text("Offset " + offsetX + " units along the X axis!", NamedTextColor.YELLOW));
                    succeeded = true;
                } catch (NumberFormatException e) {
                    editor.sendMessage(Component.text("Invalid number!", NamedTextColor.RED));
                }
                break;
            case "offsety":
                if(values.length == 0){
                    editor.sendMessage(Component.text("You must supply a number!", NamedTextColor.RED));
                    break;
                }
                try {
                    float offsetY = Float.parseFloat(values[0]);
                    this.addOffset(new Vector3f(0, offsetY, 0));
                    this.redraw();
                    editor.sendMessage(Component.text("Offset " + offsetY + " units along the Y axis!", NamedTextColor.YELLOW));
                    succeeded = true;
                } catch (NumberFormatException e) {
                    editor.sendMessage(Component.text("Invalid number!", NamedTextColor.RED));
                }
                break;
            case "offsetz":
                if(values.length == 0){
                    editor.sendMessage(Component.text("You must supply a number!", NamedTextColor.RED));
                    break;
                }
                try {
                    float offsetZ = Float.parseFloat(values[0]);
                    this.addOffset(new Vector3f(0, 0, offsetZ));
                    this.redraw();
                    editor.sendMessage(Component.text("Offset " + offsetZ + " units along the Z axis!", NamedTextColor.YELLOW));
                    succeeded = true;
                } catch (NumberFormatException e) {
                    editor.sendMessage(Component.text("Invalid number!", NamedTextColor.RED));
                }
                break;
            case "setoffset":
                if(values.length != 3){
                    editor.sendMessage(Component.text("You must supply 3 numbers!", NamedTextColor.RED));
                    break;
                }
                try {
                    float offsetX = Float.parseFloat(values[0]);
                    float offsetY = Float.parseFloat(values[1]);
                    float offsetZ = Float.parseFloat(values[2]);
                    this.setOffset(new Vector3f(offsetX, offsetY, offsetZ));
                    this.redraw();
                    editor.sendMessage(Component.text("Set offset to " + offsetX + ", " + offsetY + ", " + offsetZ, NamedTextColor.YELLOW));
                    succeeded = true;
                } catch (NumberFormatException e) {
                    editor.sendMessage(Component.text("Invalid number!", NamedTextColor.RED));
                }
                break;
        }
        return succeeded;
    }

    @Override
    public List<String> options(String option) {
        return switch (option.toLowerCase()) {
            case "scale", "size" -> List.of("<number>", "<x> <y> <z>");
            case "yaw", "pitch", "rotatex", "rotatey", "rotatez", "offsetx", "offsety", "offsetz" ->
                    List.of("<number>");
            case "setoffset" -> List.of("<x> <y> <z>");
            default -> List.of();
        };
    }

    @Override
    public List<String> fields() {
        return List.of(
                "yaw", "pitch", "scale",
                "size", "rotatex", "rotatey",
                "rotatez", "offsetx", "offsety",
                "offsetz", "setoffset");
    }
}
