package ca.bungo.holos.commands;

import ca.bungo.holos.api.holograms.SimpleHologram;
import ca.bungo.holos.api.holograms.simple.TextSimpleHologram;
import ca.bungo.holos.api.holograms.unique.Player3DSkinHologram;
import ca.bungo.holos.api.holograms.unique.image.ImageHologram;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
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

    @Default
    public void onDefaultCommand(Player player) {
        player.sendMessage(Component.text("Loading skin test", NamedTextColor.YELLOW));
        Player3DSkinHologram hologram = new Player3DSkinHologram(player.getUniqueId().toString());
        Location location = player.getEyeLocation().clone().add(player.getLocation().getDirection().multiply(2));
        location.setPitch(0);
        location.setYaw(player.getYaw() - 180);
        hologram.spawn(location);
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
