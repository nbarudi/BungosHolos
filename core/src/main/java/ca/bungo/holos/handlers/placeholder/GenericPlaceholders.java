package ca.bungo.holos.handlers.placeholder;

import ca.bungo.holos.handlers.PlaceholderHandler;
import ca.bungo.holos.utility.ComponentUtility;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

import java.util.Objects;

public class GenericPlaceholders implements PlaceholderHandler {


    @Override
    public boolean hasPlaceholder(String input) {
        return input.contains("%"); //It probably has a placeholder if there is a percentage
    }

    @Override
    public Component handlePlaceholder(String input, Player player) {
        //Super generic, basically let intellisense fill in all this data
        //This method exists in preperation for Placeholder and MiniPlaceholders, but is testing to start
        String output = input;
        output = output.replaceAll("%player%", player.getName());
        output = output.replaceAll("%uuid%", player.getUniqueId().toString());
        output = output.replaceAll("%displayname%", LegacyComponentSerializer.legacySection().serialize(player.displayName()));
        output = output.replaceAll("%health%", String.valueOf(player.getHealth()));
        output = output.replaceAll("%maxhealth%", String.valueOf(Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).getBaseValue()));
        output = output.replaceAll("%level%", String.valueOf(player.getLevel()));
        output = output.replaceAll("%exp%", String.valueOf(player.getTotalExperience()));
        output = output.replaceAll("%food%", String.valueOf(player.getFoodLevel()));
        output = output.replaceAll("%gamemode%", player.getGameMode().name());
        output = output.replaceAll("%world%", player.getWorld().getName());
        output = output.replaceAll("%x%", String.valueOf(player.getLocation().getBlockX()));
        output = output.replaceAll("%y%", String.valueOf(player.getLocation().getBlockY()));
        output = output.replaceAll("%z%", String.valueOf(player.getLocation().getBlockZ()));
        output = output.replaceAll("%yaw%", String.valueOf(player.getLocation().getYaw()));
        output = output.replaceAll("%pitch%", String.valueOf(player.getLocation().getPitch()));
        return ComponentUtility.convertToComponent(output);
    }
}
