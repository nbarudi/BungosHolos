package ca.bungo.holos.handlers.placeholder;

import ca.bungo.holos.handlers.PlaceholderHandler;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

public class PAPIPlaceholders implements PlaceholderHandler {

    @Override
    public boolean hasPlaceholder(String input) {
        return PlaceholderAPI.containsPlaceholders(input);
    }

    @Override
    public String handlePlaceholder(String input, Player player) {
        return PlaceholderAPI.setPlaceholders(player, input);
    }
}
