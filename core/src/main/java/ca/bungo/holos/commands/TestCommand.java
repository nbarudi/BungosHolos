package ca.bungo.holos.commands;

import ca.bungo.holos.BungosHolos;
import ca.bungo.holos.api.holograms.Hologram;
import ca.bungo.holos.api.holograms.SimpleHologram;
import ca.bungo.holos.api.holograms.simple.TextSimpleHologram;
import ca.bungo.holos.api.holograms.unique.image.ImageHologram;
import ca.bungo.holos.utility.ComponentUtility;
import ca.bungo.holos.utility.NetworkUtility;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import javax.print.URIException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

@CommandAlias("test|t|te")
public class TestCommand extends BaseCommand {

    private final List<SimpleHologram<?>> simpleHolograms = new ArrayList<>();

    @Dependency
    private Plugin bungosHolos;

    @Default
    public void onDefaultCommand(Player player) {
        try {
            player.sendMessage(Component.text("Loading image test", NamedTextColor.YELLOW));
            NetworkUtility.getPlayerSkin(player.getUniqueId().toString()).thenAccept((colors -> {
                ImageHologram hologram = new ImageHologram(colors);
                Location location = player.getEyeLocation().add(player.getLocation().getDirection().multiply(2));
                Bukkit.getScheduler().runTask(BungosHolos.get(), () -> hologram.spawn(location));
            }));
        } catch (URISyntaxException e){
            player.sendMessage(ComponentUtility.convertToComponent("&cSomething went wrong when trying to get the image."));
        }
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
