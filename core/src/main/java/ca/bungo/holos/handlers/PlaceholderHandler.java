package ca.bungo.holos.handlers;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public interface PlaceholderHandler {

    /**
     * Does the input string have a placeholder
     * @param input Input to check
     * @return True if yes, false otherwise
     * */
    boolean hasPlaceholder(String input);

    /**
     * Handle and replace all placeholders within the string
     * @param input String to handle placeholders with
     * @param player Player to check placeholders against
     * @return Formatted String
     * */
    Component handlePlaceholder(String input, Player player);
}
