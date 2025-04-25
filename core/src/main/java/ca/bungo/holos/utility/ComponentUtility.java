package ca.bungo.holos.utility;

import ca.bungo.holos.api.holograms.Hologram;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.slf4j.helpers.MessageFormatter;

public class ComponentUtility {

    public static Component convertToComponent(String message){
        message = message.replace("&1", "<dark_blue>");
        message = message.replace("&2", "<dark_green>");
        message = message.replace("&3", "<dark_aqua>");
        message = message.replace("&4", "<dark_red>");
        message = message.replace("&5", "<dark_purple>");
        message = message.replace("&6", "<gold>");
        message = message.replace("&7", "<gray>");
        message = message.replace("&8", "<dark_gray>");
        message = message.replace("&9", "<blue>");
        message = message.replace("&0", "<black>");

        message = message.replace("&a", "<green>");
        message = message.replace("&b", "<aqua>");
        message = message.replace("&c", "<red>");
        message = message.replace("&d", "<light_purple>");
        message = message.replace("&e", "<yellow>");
        message = message.replace("&f", "<white>");


        message = message.replace("&k", "<obf>");
        message = message.replace("&l", "<b>");
        message = message.replace("&m", "<st>");
        message = message.replace("&n", "<u>");
        message = message.replace("&o", "<i>");

        message = message.replaceAll("&#([A-Fa-f0-9]{6})", "<color:#$1>");

        message = message.replace("&r", "<reset>");

        return MiniMessage.miniMessage().deserialize(message).decoration(TextDecoration.ITALIC, false);
    }

    public static Component convertToComponent(String message, TagResolver... resolvers){
        message = message.replace("&1", "<dark_blue>");
        message = message.replace("&2", "<dark_green>");
        message = message.replace("&3", "<dark_aqua>");
        message = message.replace("&4", "<dark_red>");
        message = message.replace("&5", "<dark_purple>");
        message = message.replace("&6", "<gold>");
        message = message.replace("&7", "<gray>");
        message = message.replace("&8", "<dark_gray>");
        message = message.replace("&9", "<blue>");
        message = message.replace("&0", "<black>");

        message = message.replace("&a", "<green>");
        message = message.replace("&b", "<aqua>");
        message = message.replace("&c", "<red>");
        message = message.replace("&d", "<light_purple>");
        message = message.replace("&e", "<yellow>");
        message = message.replace("&f", "<white>");


        message = message.replace("&k", "<obf>");
        message = message.replace("&l", "<b>");
        message = message.replace("&m", "<st>");
        message = message.replace("&n", "<u>");
        message = message.replace("&o", "<i>");

        message = message.replaceAll("&#([A-Fa-f0-9]{6})", "<color:#$1>");

        message = message.replace("&r", "<reset>");

        return MiniMessage.miniMessage().deserialize(message, resolvers).decoration(TextDecoration.ITALIC, false);
    }

    public static Component generateEditComponent(Hologram hologram){
        return convertToComponent(
                format("""
                        &eYou have created a new hologram.
                        &eThe UUID of the hologram can be used to select/edit the hologram.
                        
                        <hover:show_text:'&9Click here to copy the UUID'><click:copy_to_clipboard:'{}'><blue>&eUUID: &9{}</click></hover>
                        
                        <hover:show_text:'<yellow>Click here to select the hologram'><click:run_command:'/holo select {}'><green>Click Here To Select</hover>
                        
                        &eYou can also define an Alias for the Hologram
                        
                        <hover:show_text:'<yellow>Click here to define an alias'><click:suggest_command:'/holo alias {} ALIAS'><green>Click Here To Define an Alias</hover>""",
                        hologram.getUniqueIdentifier(), hologram.getUniqueIdentifier(), hologram.getUniqueIdentifier(), hologram.getUniqueIdentifier())
        );
    }

    public static String format(String toFormat, Object... params){
        return MessageFormatter.arrayFormat(toFormat, params).getMessage();
    }

}
