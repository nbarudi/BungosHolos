package ca.bungo.holos.commands;

import ca.bungo.holos.types.holograms.Hologram;
import ca.bungo.holos.types.holograms.simple.TextHologram;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import me.lucko.spark.paper.common.command.sender.CommandSender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@CommandAlias("test|t|te")
public class TestCommand extends BaseCommand {

    private final List<Hologram> holograms = new ArrayList<>();

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
        TextHologram hologram = new TextHologram(message);
        Location baseLocation = player.getEyeLocation().add(player.getLocation().getDirection().multiply(2));
        baseLocation.setPitch(0);
        baseLocation.setYaw(player.getYaw() - 180);

        hologram.setBackgroundColor(Color.AQUA);
        hologram.setBillboard(Display.Billboard.VERTICAL);

        hologram.spawn(baseLocation);
        holograms.add(hologram);
    }

    @Subcommand("clean")
    @Description("Clean up spawned holograms")
    public void onCleanupHolograms(Player player) {
        for(Hologram hologram : holograms) {
            hologram.remove();
        }
        holograms.clear();
        player.sendMessage(Component.text("Holograms cleaned!", NamedTextColor.YELLOW));
    }

}
