package ca.bungo.holos.commands;

import ca.bungo.holos.BungosHolos;
import ca.bungo.holos.HologramRegistry;
import ca.bungo.holos.api.holograms.Editable;
import ca.bungo.holos.api.holograms.Hologram;
import ca.bungo.holos.api.holograms.simple.TextSimpleHologram;
import ca.bungo.holos.utility.ComponentUtility;
import co.aikar.commands.*;
import co.aikar.commands.annotation.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;

import java.util.List;


@CommandAlias("holo|hologram|bungoshologram|bh")
public class HologramCommand extends BaseCommand {

    public HologramCommand(){
        CommandCompletions<BukkitCommandCompletionContext> commandCompletions = BungosHolos.commandManager.getCommandCompletions();
        commandCompletions.registerCompletion("holotypes", c -> List.of("text", "block", "item", "entity"));
        commandCompletions.registerCompletion("holouuids", c -> HologramRegistry.getRegisteredHolograms().keySet());
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
    @Description("Create a new hologram of the supplied type (You will set it up after)")
    @CommandCompletion("@holotypes")
    @CommandPermission("bungosholos.create")
    public void createHologram(Player sender, String type) {
        switch (type.toLowerCase()) {
            case "block":
                break;
            case "text":
                TextSimpleHologram hologram = new TextSimpleHologram("Hello World!");
                hologram.setBillboard(Display.Billboard.CENTER);
                hologram.spawn(sender.getEyeLocation());
                sender.sendMessage(ComponentUtility.generateEditComponent(hologram));
                break;
            case "entity":
                break;
            case "item":
                break;
            default:
                sender.sendMessage(Component.text("Unknown hologram type.", NamedTextColor.RED));
        }
    }

    @Subcommand("edit")
    @Syntax("<uuid/alias> [field] [data]")
    @Description("Edit the hologram defined by the UUID.")
    @CommandPermission("bungosholos.edit")
    @CommandCompletion("@holouuids")
    public void editHologram(Player sender, String uuid, @Optional String field, @Optional String... data) {
        Hologram hologram = HologramRegistry.getHologram(uuid);
        if(hologram == null) {
            sender.sendMessage(Component.text("Hologram not found.", NamedTextColor.RED));
            return;
        }
        if(!(hologram instanceof Editable)) {
            sender.sendMessage(Component.text("Hologram is not editable.", NamedTextColor.RED));
            return;
        }
        sender.sendMessage(ComponentUtility.convertToComponent("&eNow editing hologram: &9" + hologram.getUniqueIdentifier()));
        ((Editable) hologram).onEdit(sender, field, data);
    }


    @Subcommand("alias")
    @Syntax("<uuid> <alias>")
    @Description("Set a friendly alias for a holograms UUID")
    @CommandPermission("bungosholos.alias")
    @CommandCompletion("@holouuids")
    public void setHologramAlias(Player sender, String uuid, String alias) {
        if(HologramRegistry.defineAlias(uuid, alias)) {
            sender.sendMessage(ComponentUtility.convertToComponent("&eDefined Alias &b" + alias + " &efor hologram &b" + uuid));
            return;
        }
        sender.sendMessage(ComponentUtility.convertToComponent("&cCould not define an alias for the hologram."));
    }

}
