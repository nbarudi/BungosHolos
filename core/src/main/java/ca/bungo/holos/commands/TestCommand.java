package ca.bungo.holos.commands;

import ca.bungo.holos.api.holograms.SimpleHologram;
import ca.bungo.holos.api.holograms.simple.TextSimpleHologram;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

@CommandAlias("test|t|te")
public class TestCommand extends BaseCommand {

    private final List<SimpleHologram<?>> simpleHolograms = new ArrayList<>();

    @Dependency
    private Plugin bungosHolos;

    @PreCommand
    public boolean preCommand(CommandSender sender, String[] args) {
        return sender instanceof Player;
    }

    @Default
    public void onDefaultCommand(Player player) {
        player.sendMessage("Hello World!");
    }

    @Subcommand("text")
    @Syntax("[message]")
    @Description("Spawn a text hologram")
    public void onTextHologram(Player player, String message) {
        TextSimpleHologram hologram = new TextSimpleHologram(message);
        Location baseLocation = player.getEyeLocation().add(player.getLocation().getDirection().multiply(2));
        baseLocation.setPitch(0);
        baseLocation.setYaw(player.getYaw() - 180);

        hologram.setBackgroundColor(Color.AQUA);
        hologram.setBillboard(Display.Billboard.VERTICAL);

        hologram.spawn(baseLocation);
        simpleHolograms.add(hologram);
    }

    @Subcommand("clean")
    @Description("Clean up spawned holograms")
    public void onCleanupHolograms(Player player) {
        for(SimpleHologram<?> simpleHologram : simpleHolograms) {
            simpleHologram.remove();
        }
        simpleHolograms.clear();
        player.sendMessage(Component.text("Holograms cleaned!", NamedTextColor.YELLOW));
    }

}
