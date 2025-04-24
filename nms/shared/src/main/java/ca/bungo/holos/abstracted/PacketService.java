package ca.bungo.holos.abstracted;

import org.bukkit.entity.Display;
import org.bukkit.entity.Player;

public abstract class PacketService {

    private final PacketHelper helper;

    public PacketService(PacketHelper helper) {
        this.helper = helper;
    }

    /**
     * Simple debug string to print out what packet handler the server is using
     * @return Debug string for the packet service
     */
    public abstract String getVersion();

    /**
     * Send a EntityData packet to the player for the supplied entity ID
     * @param player Player to send the entity data to
     * @param entityID Id of the entity whos data needs to be updated
     * @param display The display containing all of the data which will be sent
     * @return True if the update is successful, false if an error occures
     * */
    public abstract boolean updateEntityData(Player player, int entityID, Display display);
}
