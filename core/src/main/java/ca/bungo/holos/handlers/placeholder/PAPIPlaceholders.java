package ca.bungo.holos.handlers.placeholder;

import ca.bungo.holos.handlers.PlaceholderHandler;
import ca.bungo.holos.utility.ComponentUtility;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public class PAPIPlaceholders implements PlaceholderHandler {

    @Override
    public boolean hasPlaceholder(String input) {
        return PlaceholderAPI.containsPlaceholders(input);
    }

    @Override
    public Component handlePlaceholder(String input, Player player) {
        return ComponentUtility.convertToComponent(PlaceholderAPI.setPlaceholders(player, input));
    }
}
