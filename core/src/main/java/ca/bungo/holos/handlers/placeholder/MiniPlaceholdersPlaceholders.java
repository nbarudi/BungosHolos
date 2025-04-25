package ca.bungo.holos.handlers.placeholder;

import ca.bungo.holos.handlers.PlaceholderHandler;
import ca.bungo.holos.utility.ComponentUtility;
import io.github.miniplaceholders.api.MiniPlaceholders;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

public class MiniPlaceholdersPlaceholders implements PlaceholderHandler {

    @Override
    public boolean hasPlaceholder(String input) {
        // Could be a placeholder if contains a mini message tag <>
        // Sadly not many better validation options
        return input.contains("<") && input.contains(">");
    }

    @Override
    public Component handlePlaceholder(String input, Player player) {
        TagResolver globalResolver = MiniPlaceholders.getGlobalPlaceholders();
        TagResolver playerResolver = MiniPlaceholders.getAudiencePlaceholders(player);
        return ComponentUtility.convertToComponent(input, globalResolver, playerResolver);
    }
}
