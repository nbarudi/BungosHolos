package ca.bungo.holos.api.holograms;

import org.bukkit.Location;

public interface Hologram {

    /**
     * Get the unique identifier for the hologram
     * @return The Identifier unique to this hologram
     * */
    String getUniqueIdentifier();

    /**
     * Method triggered when the server is shut down / reloaded
     * */
    void onDisable();

    /**
     * Method to remove the hologram
     * */
    void remove();

    /**
     * Teleport hologram to defined position
     * @param location Where to teleport to
     * */
    void teleport(Location location);

    /**
     * Teleport hologram to defined position
     * @return location where the hologram is located
     * */
    Location getLocation();

}
