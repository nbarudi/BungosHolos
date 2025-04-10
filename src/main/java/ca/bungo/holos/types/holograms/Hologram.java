package ca.bungo.holos.types.holograms;

import org.bukkit.Location;

public interface Hologram {

    /**
     * Spawn Hologram at supplied location
     * @param location Where to spawn the hologram
     * */
    void spawn(Location location);

    /**
     * Apply all modified settings to an already spawned Hologram
     * */
    void redraw();

    /**
     * Delete the Hologram
     * */
    void remove();

}
