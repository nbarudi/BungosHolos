package ca.bungo.holos.api.holograms.simple;

import ca.bungo.holos.BungosHolos;
import ca.bungo.holos.HologramRegistry;
import ca.bungo.holos.api.holograms.SimpleHologram;
import ca.bungo.holos.utility.ComponentUtility;
import io.netty.handler.codec.base64.Base64Encoder;
import io.papermc.paper.datacomponent.DataComponentType;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Getter
@Setter
public class ItemSimpleHologram extends SimpleHologram<ItemDisplay> {


    private ItemStack itemStack;
    private boolean persistent;
    private ItemDisplay.ItemDisplayTransform displayTransform;

    public ItemSimpleHologram(ItemStack itemStack) {
        super(ItemDisplay.class);
        this.itemStack = itemStack;

        this.persistent = true;
        this.displayTransform = ItemDisplay.ItemDisplayTransform.FIRSTPERSON_LEFTHAND;
    }

    @Override
    protected void modifyDisplay() {
        getDisplay().setItemDisplayTransform(displayTransform);
        getDisplay().setItemStack(itemStack);
        getDisplay().setPersistent(persistent);
    }

    public void editItemMeta(Consumer<? super ItemMeta> consumer){
        ItemMeta meta = itemStack.getItemMeta();
        consumer.accept(meta);
        itemStack.setItemMeta(meta);
    }

    public <T> void setData(DataComponentType.Valued<T> component, T data) {
        itemStack.setData(component, data);
    }

    public void setData(DataComponentType.NonValued component) {
        itemStack.setData(component);
    }

    public <T> T getData(DataComponentType.Valued<T> component) {
        return itemStack.getData(component);
    }

    public <T> void resetData(DataComponentType.Valued<T> component) {
        itemStack.resetData(component);
    }

    public <T> void removeData(DataComponentType.Valued<T> component) {
        itemStack.unsetData(component);
    }

    @Override
    public void onRemove() {}

    @Override
    public void saveUniqueContent(Map<String, Object> map) {
        byte[] itemData = itemStack.serializeAsBytes();
        String encoded = Base64.getEncoder().encodeToString(itemData);
        map.put("item", encoded);
        map.put("persistent", persistent);
        map.put("display_transform", displayTransform.name());
    }

    public static ItemSimpleHologram deserialize(Map<String, Object> data) {
        Map<String, Object> uniqueData = (Map<String, Object>)data.get("unique_data");
        String encoded = (String)uniqueData.get("item");
        byte[] itemData = Base64.getDecoder().decode(encoded);
        ItemSimpleHologram hologram = new ItemSimpleHologram(ItemStack.deserializeBytes(itemData));
        HologramRegistry.unregisterHologram(hologram);
        hologram.setPersistent((boolean)uniqueData.get("persistent"));
        hologram.setDisplayTransform(ItemDisplay.ItemDisplayTransform.valueOf((String)uniqueData.get("display_transform")));
        hologram.deserializeGeneric(data);

        if(!BungosHolos.DISABLED) hologram.spawn(hologram.getLocation());

        return hologram;
    }

    @Override
    public boolean onEdit(@NotNull Player editor, @Nullable String field, String... values) {
        if(field == null){
            String editMessage = ComponentUtility.format(
                    """
                    &eHere are the fields that you're able to edit for this Item Hologram:
                    <hover:show_text:'&eClick to edit field'><click:suggest_command:'/holo edit item'>&bitem &e- Set the hologram item to item in main hand
                    <hover:show_text:'&eClick to edit field'><click:suggest_command:'/holo edit type'>&btype String &e- Set the hologram item type
                    <hover:show_text:'&eClick to edit field'><click:suggest_command:'/holo edit model'>&bmodel Number &e- Set the hologram item custom model data
                    <hover:show_text:'&eClick to edit field'><click:suggest_command:'/holo edit display'>&bdisplay DisplayType &e- Set the hologram item display type
                    <hover:show_text:'&eClick to edit field'><click:suggest_command:'/holo edit persist'>&bpersist True|False &e- Is the hologram permanent""");
            
            editor.sendMessage(ComponentUtility.convertToComponent(editMessage));
            return super.onEdit(editor, null, values);
        }
        boolean succeeded = false;
        switch (field.toLowerCase()) {
            case "item":
                if (values.length == 0) {
                    succeeded = true;
                    this.setItemStack(editor.getInventory().getItemInMainHand());
                    this.redraw();
                    editor.sendMessage(Component.text("Set the item to: ").append(ComponentUtility.convertToComponent(itemStack.getType().name())));
                    break;
                }
                break;
            case "type":
                if (values.length > 0) {
                    try {
                        this.itemStack = this.itemStack.withType(Material.valueOf(values[0].toUpperCase()));
                        this.redraw();
                        editor.sendMessage(Component.text("Set the item type to: ").append(ComponentUtility.convertToComponent(this.itemStack.getType().name())));
                        succeeded = true;
                    } catch (IllegalArgumentException e) {
                        editor.sendMessage(Component.text("Invalid material type: ").append(ComponentUtility.convertToComponent(values[0])));
                    }
                }
                break;
            case "model":
                if (values.length > 0) {
                    try {
                        int modelData = Integer.parseInt(values[0]);
                        this.editItemMeta(meta -> meta.setCustomModelData(modelData));
                        this.redraw();
                        editor.sendMessage(Component.text("Set custom model data to: ").append(ComponentUtility.convertToComponent(String.valueOf(modelData))));
                        succeeded = true;
                    } catch (NumberFormatException e) {
                        editor.sendMessage(Component.text("Invalid model data: ").append(ComponentUtility.convertToComponent(values[0])));
                    }
                }
                break;
            case "display":
                if (values.length > 0) {
                    try {
                        this.displayTransform = ItemDisplay.ItemDisplayTransform.valueOf(values[0].toUpperCase());
                        this.redraw();
                        editor.sendMessage(Component.text("Set display transform to: ").append(ComponentUtility.convertToComponent(values[0])));
                        succeeded = true;
                    } catch (IllegalArgumentException e) {
                        editor.sendMessage(Component.text("Invalid display transform: ").append(ComponentUtility.convertToComponent(values[0])));
                    }
                }
                break;
            case "persist":
                if (values.length > 0) {
                    if (values[0].equalsIgnoreCase("true") || values[0].equalsIgnoreCase("false")) {
                        this.persistent = Boolean.parseBoolean(values[0]);
                        this.redraw();
                        editor.sendMessage(Component.text("Set persistent to: ").append(ComponentUtility.convertToComponent(String.valueOf(this.persistent))));
                        succeeded = true;
                    } else {
                        editor.sendMessage(Component.text("Invalid persist value: ").append(ComponentUtility.convertToComponent(values[0])));
                    }
                }
                break;
        }
        return succeeded ? succeeded : super.onEdit(editor, field, values);
    }

    @Override
    public List<String> options(String option) {
        return switch (option.toLowerCase()) {
            case "item" -> List.of();
            case "type" -> Stream.of(Material.values()).map(Material::name).toList();
            case "model" -> List.of("<number>");
            case "display" -> Stream.of(ItemDisplay.ItemDisplayTransform.values()).map(Enum::name).toList();
            case "persist" -> List.of("true", "false");
            default -> super.options(option);
        };
    }

    @Override
    public List<String> fields() {
        List<String> fields = new ArrayList<>(super.fields());
        fields.add("item");
        fields.add("type");
        fields.add("model");
        fields.add("display");
        fields.add("persist");
        return fields;
    }
}
