package ca.bungo.holos.commands;

import ca.bungo.holos.BungosHolos;
import ca.bungo.holos.api.animations.Animation;
import ca.bungo.holos.api.holograms.Animatable;
import ca.bungo.holos.api.holograms.Editable;
import ca.bungo.holos.api.holograms.Hologram;
import ca.bungo.holos.api.holograms.simple.BlockSimpleHologram;
import ca.bungo.holos.api.holograms.simple.ItemSimpleHologram;
import ca.bungo.holos.api.holograms.simple.TextSimpleHologram;
import ca.bungo.holos.api.holograms.unique.Player3DSkinHologram;
import ca.bungo.holos.api.holograms.unique.image.ImageHologram;
import ca.bungo.holos.utility.ComponentUtility;
import co.aikar.commands.*;
import co.aikar.commands.annotation.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@CommandAlias("holo|hologram|bungoshologram|bh")
public class HologramCommand extends BaseCommand {

    private Map<String, String> selectedHolo;

    public HologramCommand(){
        CommandCompletions<BukkitCommandCompletionContext> commandCompletions = BungosHolos.commandManager.getCommandCompletions();
        commandCompletions.registerCompletion("animtypes", c -> BungosHolos.get().animationRegistry.getRegisteredAnimations());
        commandCompletions.registerCompletion("holotypes", c -> List.of("text", "block", "item", "player2d", "player3d"));
        commandCompletions.registerCompletion("holouuids", c -> BungosHolos.get().hologramRegistry.getValidHologramIdentifiers());
        commandCompletions.registerCompletion("holofields", c -> {
            String selected = selectedHolo.get(c.getIssuer().getUniqueId().toString());
            if(selected == null){
                return List.of("No Hologram Selected!");
            }
            Hologram hologram = BungosHolos.get().hologramRegistry.getHologram(selected);
            if(hologram == null) {
                return List.of("Hologram Not Found!");
            }
            if(hologram instanceof Editable) {
                return ((Editable) hologram).fields();
            }
            return List.of("Hologram is not editable!");
        });
        commandCompletions.registerCompletion("holooptions", c -> {
            String selected = selectedHolo.get(c.getIssuer().getUniqueId().toString());
            if(selected == null){
                return List.of("No Hologram Selected!");
            }
            Hologram hologram = BungosHolos.get().hologramRegistry.getHologram(selected);
            if(hologram == null) {
                return List.of("Hologram Not Found!");
            }
            if(hologram instanceof Editable) {
                return ((Editable) hologram).options(c.getContextValue(String.class));
            }
            return List.of("Hologram is not editable!");
        });
        selectedHolo = new HashMap<>();
    }

    @Default
    public void onDefault(CommandSender sender) {
        String defaultMessage = """
                &eWelcome to &bBungo's Hololograms&e!
                Created by Bungo/nbarudi (https://bungo.ca)""";
        sender.sendMessage(ComponentUtility.convertToComponent(defaultMessage));
    }

    @Subcommand("help")
    @HelpCommand
    @CatchUnknown
    public static void onHelp(CommandSender sender, CommandHelp help) {
        String helpMessage = """
                &eWelcome to &bBungo's Hololograms&e!
                Created by Bungo/nbarudi (https://bungo.ca)""";
        sender.sendMessage(ComponentUtility.convertToComponent(helpMessage));
        help.showHelp();
    }

    @Subcommand("create")
    @Syntax("<type>")
    @Description("Create a new hologram of the supplied type (You will set it up after).")
    @CommandCompletion("@holotypes")
    @CommandPermission("bungosholos.create")
    public void createHologram(Player sender, String type) {
        switch (type.toLowerCase()) {
            case "block":
                BlockSimpleHologram blockHologram = new BlockSimpleHologram(Material.STONE);
                blockHologram.spawn(sender.getEyeLocation());
                sender.sendMessage(ComponentUtility.generateEditComponent(blockHologram));
                break;
            case "text":
                TextSimpleHologram hologram = new TextSimpleHologram("Hello World!");
                hologram.setBillboard(Display.Billboard.CENTER);
                hologram.spawn(sender.getEyeLocation());
                sender.sendMessage(ComponentUtility.generateEditComponent(hologram));
                break;
            case "item":
                ItemStack possibleItem = sender.getInventory().getItemInMainHand();
                ItemSimpleHologram simpleHologram = new ItemSimpleHologram(
                        possibleItem.getType().equals(Material.AIR) ? new ItemStack(Material.DIAMOND_SWORD) : possibleItem);
                simpleHologram.spawn(sender.getEyeLocation());
                sender.sendMessage(ComponentUtility.generateEditComponent(simpleHologram));
                break;
            case "player2d":
                ImageHologram.Player2DSkinHologram player2DSkinHologram = new ImageHologram.Player2DSkinHologram(
                        sender.getUniqueId().toString(),
                        ImageHologram.Player2DSkinHologram.HologramType.FULL
                );
                player2DSkinHologram.spawn(sender.getEyeLocation());
                sender.sendMessage(ComponentUtility.generateEditComponent(player2DSkinHologram));
                break;
            case "player3d":
                Player3DSkinHologram player3DSkinHologram = new Player3DSkinHologram(sender.getUniqueId().toString());
                player3DSkinHologram.spawn(sender.getLocation());
                sender.sendMessage(ComponentUtility.generateEditComponent(player3DSkinHologram));
                break;
            default:
                sender.sendMessage(Component.text("Unknown hologram type.", NamedTextColor.RED));
        }
    }

    @Subcommand("delete")
    @Syntax("[uuid/alias] - Or currently selected if no uuid supplied")
    @Description("Deletes referenced hologram or currently selected one")
    @CommandPermission("bungosholos.delete")
    @CommandCompletion("@holouuids")
    public void deleteHologram(Player sender, @Optional String uuid) {
        if(uuid == null){
            String selected = selectedHolo.get(sender.getUniqueId().toString());
            if(selected == null){
                sender.sendMessage(Component.text("No hologram selected and no identifier supplied", NamedTextColor.RED));
                return;
            }
            Hologram hologram = BungosHolos.get().hologramRegistry.getHologram(selected);
            if(hologram == null){
                sender.sendMessage(Component.text("Hologram not found!", NamedTextColor.RED));
                return;
            }
            BungosHolos.get().hologramRegistry.unregisterHologram(hologram);
            hologram.remove();
            selectedHolo.remove(selected);
            sender.sendMessage(Component.text("Hologram deleted", NamedTextColor.YELLOW));
        }
        else {
            Hologram hologram = BungosHolos.get().hologramRegistry.getHologram(uuid);
            if(hologram == null){
                sender.sendMessage(Component.text("Hologram not found!", NamedTextColor.RED));
                return;
            }
            BungosHolos.get().hologramRegistry.unregisterHologram(hologram);
            hologram.remove();
            selectedHolo.remove(uuid);
            sender.sendMessage(Component.text("Hologram deleted", NamedTextColor.YELLOW));
        }
    }

    @Subcommand("select")
    @Syntax("[uuid/alias] - Empty for current selected.")
    @Description("Select a hologram for future commands.")
    @CommandPermission("bungosholos.select")
    @CommandCompletion("@holouuids @nothing")
    public void selectHologram(Player player, @Optional String identifier){
        if(identifier == null){
            String selected = selectedHolo.get(player.getUniqueId().toString());
            if(selected == null){
                player.sendMessage(ComponentUtility.convertToComponent("&eYou do not have a hologram selected!"));
            }
            else {
                player.sendMessage(ComponentUtility.convertToComponent("&eYour current hologram is: &9" + selected + "&e!"));
            }
            return;
        }
        Hologram hologram = BungosHolos.get().hologramRegistry.getHologram(identifier);
        if(hologram == null) {
            player.sendMessage(Component.text("Hologram not found.", NamedTextColor.RED));
            return;
        }
        selectedHolo.put(player.getUniqueId().toString(), identifier);
        player.sendMessage(ComponentUtility.convertToComponent("&eYou have selected the hologram: &9" + identifier + "&e!"));
        player.sendMessage(ComponentUtility.convertToComponent(
                        "<hover:show_text:'Click to see editable fields'><click:run_command:'/holo edit'>&eClick to see editable fields</hover>")
        );
    }

    @Subcommand("edit")
    @Syntax("[field] [data]")
    @Description("Edit the hologram defined by the UUID.")
    @CommandPermission("bungosholos.edit")
    @CommandCompletion("@holofields @holooptions")
    public void editHologram(Player sender, @Optional String field, @Optional String... data) {
        String selected = selectedHolo.get(sender.getUniqueId().toString());
        if(selected == null){
            sender.sendMessage(Component.text("You do not have a hologram selected!.", NamedTextColor.RED));
            sender.sendMessage(Component.text("/holo select IDENTIFIER.", NamedTextColor.YELLOW));
        }
        Hologram hologram = BungosHolos.get().hologramRegistry.getHologram(selected);
        if(hologram == null) {
            sender.sendMessage(Component.text("Hologram not found.", NamedTextColor.RED));
            return;
        }
        if(!(hologram instanceof Editable)) {
            sender.sendMessage(Component.text("Hologram is not editable.", NamedTextColor.RED));
            return;
        }
        sender.sendMessage(ComponentUtility.convertToComponent("&eNow editing hologram: &9" + hologram.getUniqueIdentifier()));
        if(!((Editable) hologram).onEdit(sender, field, data)){
            sender.sendMessage(Component.text("Something went wrong when editing the hologram...", NamedTextColor.RED));
        }
    }


    @Subcommand("alias")
    @Syntax("<uuid> <alias>")
    @Description("Set a friendly alias for a holograms UUID.")
    @CommandPermission("bungosholos.alias")
    @CommandCompletion("@holouuids @nothing")
    public void setHologramAlias(Player sender, String uuid, String alias) {
        if(BungosHolos.get().hologramRegistry.defineAlias(uuid, alias, false)) {
            sender.sendMessage(ComponentUtility.convertToComponent("&eDefined Alias &b" + alias + " &efor hologram &b" + uuid));
            return;
        }
        sender.sendMessage(ComponentUtility.convertToComponent("&cCould not define an alias for the hologram."));
    }

    @Subcommand("list")
    @Description("List all currently registered Holograms.")
    @CommandPermission("bungosholos.list")
    public void listHolograms(Player sender) {
        Map<String, String> uuidToAlias = new HashMap<>();
        for(Map.Entry<String, String> entry : BungosHolos.get().hologramRegistry.getHologramAliases().entrySet()) {
            uuidToAlias.put(entry.getValue(), entry.getKey());
        }

        StringBuilder hologramList = new StringBuilder("&eHere are currently registered Holograms:\n");
        for(String uuid : BungosHolos.get().hologramRegistry.getRegisteredHolograms().keySet()) {
            if(uuidToAlias.containsKey(uuid)) {
                hologramList.append("&e- &9").append(uuidToAlias.remove(uuid)).append("&7(").append(uuid).append(")\n");
            }
            else {
                hologramList.append("&e- &9").append(uuid).append("\n");
            }
        }
        sender.sendMessage(ComponentUtility.convertToComponent(hologramList.substring(0, hologramList.length() - 1)));
    }

    @Subcommand("bring")
    @Description("Bring selected hologram to you.")
    @Syntax("[eye/front/feet] - Optional what position to teleport to")
    @CommandCompletion("eye|front|feet")
    @CommandPermission("bungosholos.bring")
    public void bringHologram(Player sender, @Default("feet") String position) {
        String selected = selectedHolo.get(sender.getUniqueId().toString());
        if(selected == null){
            sender.sendMessage(Component.text("You do not have a hologram selected!", NamedTextColor.RED));
            return;
        }

        Hologram hologram = BungosHolos.get().hologramRegistry.getHologram(selected);
        if(hologram == null) {
            sender.sendMessage(Component.text("Hologram not found.", NamedTextColor.RED));
            return;
        }

        Location goingTo;
        if(position.equalsIgnoreCase("feet")){
            goingTo = sender.getLocation();
        }
        else if(position.equalsIgnoreCase("front")){
            Location baseLocation = sender.getEyeLocation();
            baseLocation.add(sender.getLocation().getDirection().normalize().multiply(2f));
            baseLocation.setYaw(sender.getLocation().getYaw() - 180);
            goingTo = baseLocation;
        }
        else {
            goingTo = sender.getEyeLocation();
        }


        hologram.teleport(goingTo);
        sender.sendMessage(Component.text("Hologram has been teleported to your position!", NamedTextColor.YELLOW));
    }

    @Subcommand("goto")
    @Description("Goto selected hologram.")
    @CommandPermission("bungosholos.goto")
    public void gotoHologram(Player sender) {
        String selected = selectedHolo.get(sender.getUniqueId().toString());
        if(selected == null){
            sender.sendMessage(Component.text("You do not have a hologram selected!", NamedTextColor.RED));
            return;
        }

        Hologram hologram = BungosHolos.get().hologramRegistry.getHologram(selected);
        if(hologram == null) {
            sender.sendMessage(Component.text("Hologram not found.", NamedTextColor.RED));
            return;
        }
        sender.teleport(hologram.getLocation());
        sender.sendMessage(Component.text("Teleported you to the hologram!", NamedTextColor.YELLOW));
    }

    @Subcommand("animate")
    @Description("Set selected holograms animation to supplied animation")
    @Syntax("[animation]")
    @CommandCompletion("@animtypes")
    @CommandPermission("bungosholos.animate")
    public void animateHologram(Player sender, String animationName) {
        String selected = selectedHolo.get(sender.getUniqueId().toString());
        if(selected == null){
            sender.sendMessage(Component.text("You do not have a hologram selected!", NamedTextColor.RED));
            return;
        }

        Hologram hologram = BungosHolos.get().hologramRegistry.getHologram(selected);
        if(hologram == null) {
            sender.sendMessage(Component.text("Hologram not found.", NamedTextColor.RED));
            return;
        }

        if(!(hologram instanceof Animatable animatable)) {
            sender.sendMessage(Component.text("Hologram is not animatable.", NamedTextColor.RED));
            return;
        }

        Animation selectedAnimation = BungosHolos.get().animationRegistry.getAnimation(animationName);
        if(selectedAnimation == null) {
            sender.sendMessage(Component.text("Animation not found.", NamedTextColor.RED));
        }

        animatable.loadAnimation(selectedAnimation);
        animatable.playAnimation();
    }

}
