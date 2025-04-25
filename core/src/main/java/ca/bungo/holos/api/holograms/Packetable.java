package ca.bungo.holos.api.holograms;

import org.bukkit.entity.Display;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;


/**
 * Packet typed holograms require some customization handling
 * If you're making a hologram, honestly, probably should design your own handler
 * as mine is by no means going to be the best option but it works for what I'm doing.
 * */
public interface Packetable {

    /**
     * Get the text display we need to update
     * */
    Display getDisplay();

    /**
     * Is the hologram currently packet based?
     * @return True if yes
     * */
    boolean isPacketed();

    /**
     * Set if the hologram is packet based
     * @param packeted Boolean for if the hologram is packeted
     * */
    void setPacketed(boolean packeted);

    /**
     * Update the display for all players
     * */
    void updateAll();

    /**
     * Update the display for the supplied player
     * @param player Who to update the display for
     * */
    void updatePlayer(Player player);

    /**
     * Helper function to automatically update packet holograms when players are rendering them
     * */
    default void globalTickUpdate() {
        Set<Player> tracked = getDisplay().getTrackedBy();
        for (Player player : tracked) {
            updatePlayer(player);
        }
    }
}
