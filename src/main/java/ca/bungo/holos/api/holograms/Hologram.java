package ca.bungo.holos.api.holograms;

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

}
