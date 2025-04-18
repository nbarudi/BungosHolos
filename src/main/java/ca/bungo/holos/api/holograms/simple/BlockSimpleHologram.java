package ca.bungo.holos.api.holograms.simple;

import ca.bungo.holos.BungosHolos;
import ca.bungo.holos.HologramRegistry;
import ca.bungo.holos.api.holograms.SimpleHologram;
import ca.bungo.holos.utility.ComponentUtility;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class BlockSimpleHologram extends SimpleHologram<BlockDisplay> {

    private BlockData blockData;
    private boolean persistent;

    public BlockSimpleHologram(Material material) {
        super(BlockDisplay.class);
        this.blockData = material.createBlockData();
        this.persistent = true;
    }

    public BlockSimpleHologram(BlockData blockData) {
        super(BlockDisplay.class);
        this.blockData = blockData;
        this.persistent = true;
    }

    @Override
    protected void modifyDisplay() {
        getDisplay().setBlock(blockData);
        getDisplay().setPersistent(persistent);
    }

    @Override
    public void onRemove() {}

    @Override
    public void onDisable() throws IOException {
        if(persistent)
            super.onDisable();
    }

    @Override
    public void saveUniqueContent(Map<String, Object> map) {
        map.put("block_data", blockData.getAsString());
        map.put("persistent", persistent);
    }

    public static BlockSimpleHologram deserialize(Map<String, Object> data) {
        Map<String, Object> uniqueData = (Map<String, Object>)data.get("unique_data");
        String blockDataString = (String)uniqueData.get("block_data");
        BlockData blockData = org.bukkit.Bukkit.createBlockData(blockDataString);

        BlockSimpleHologram hologram = new BlockSimpleHologram(blockData);
        HologramRegistry.unregisterHologram(hologram);
        hologram.setPersistent((boolean)uniqueData.get("persistent"));

        hologram.deserializeGeneric(data);

        if(!BungosHolos.DISABLED) hologram.spawn(hologram.getLocation());

        return hologram;
    }

    @Override
    public boolean onEdit(@NotNull Player editor, @Nullable String field, String... values) {
        if(field == null){
            String editMessage = ComponentUtility.format(
                    """
                    &eHere are the fields that you're able to edit for this Block Hologram:
                    <hover:show_text:'&eClick to edit field'><click:suggest_command:'/holo edit material VALUE'>&bmaterial String &e- Set the block material type
                    <hover:show_text:'&eClick to edit field'><click:suggest_command:'/holo edit block'>&bblock &e- Set the block to the held block type
                    <hover:show_text:'&eClick to edit field'><click:suggest_command:'/holo edit persist'>&bpersist True|False &e- Is the hologram permanent""");

            editor.sendMessage(ComponentUtility.convertToComponent(editMessage));
            return super.onEdit(editor, null, values);
        }
        boolean succeeded = false;
        switch (field.toLowerCase()) {
            case "material":
                if (values.length > 0) {
                    try {
                        Material material = Material.valueOf(values[0].toUpperCase());
                        this.blockData = material.createBlockData();
                        this.redraw();
                        editor.sendMessage(Component.text("Set the block material to: ").append(ComponentUtility.convertToComponent(material.name())));
                        succeeded = true;
                    } catch (IllegalArgumentException e) {
                        editor.sendMessage(Component.text("Invalid material type: ").append(ComponentUtility.convertToComponent(values[0])));
                    }
                }
                break;
            case "block":
                ItemStack itemStack = editor.getInventory().getItemInMainHand();
                if(!itemStack.getType().isBlock()) {
                    editor.sendMessage(Component.text("You must be holding a block to use this command!"));
                    return true;
                }
                Material material = itemStack.getType();
                this.blockData = material.createBlockData();
                this.redraw();
                editor.sendMessage(Component.text("Set the block to: ").append(ComponentUtility.convertToComponent(material.name())));
                succeeded = true;
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
            case "material" -> List.of(Material.values()).stream()
                    .filter(Material::isBlock)
                    .filter(material -> !material.isLegacy())
                    .map(Material::name)
                    .toList();
            case "persist" -> List.of("true", "false");
            default -> super.options(option);
        };
    }

    @Override
    public List<String> fields() {
        List<String> fields = new ArrayList<>(super.fields());
        fields.add("material");
        fields.add("persist");
        fields.add("block");
        return fields;
    }
}
