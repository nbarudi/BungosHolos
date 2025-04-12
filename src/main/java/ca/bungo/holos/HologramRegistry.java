package ca.bungo.holos;


import ca.bungo.holos.api.holograms.Hologram;
import lombok.Getter;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class HologramRegistry {

    @Getter
    private static final Map<String, Hologram> registeredHolograms = new HashMap<>();
    @Getter
    private static final Map<String, String> hologramAliases = new HashMap<>();

    /**
     * Register a new hologram for event handling.
     * This will allow it to automatically clean up and save data on server boot and shutdown
     * Generally should be handled automatically by the hologram its self
     * @param hologram Hologram to register
     * */
    public static void registerHologram(Hologram hologram) {
        registeredHolograms.put(hologram.getUniqueIdentifier(), hologram);
    }

    /**
     * Remove a hologram from the registry
     * @param hologram Hologram to remove
     * */
    public static void unregisterHologram(Hologram hologram) {
        registeredHolograms.remove(hologram.getUniqueIdentifier());
        if(fetchAlias(hologram.getUniqueIdentifier()) != null) {
            defineAlias(hologram.getUniqueIdentifier(), null);
        }
    }

    /**
     * Remove a hologram from the registry
     * @param uniqueIdentifier Identifier of the hologram to remove
     * */
    public static void unregisterHologram(String uniqueIdentifier) {
        registeredHolograms.remove(uniqueIdentifier);
        if(fetchAlias(uniqueIdentifier) != null) {
            defineAlias(uniqueIdentifier, null);
        }
    }

    /**
     * Fetch a registered hologram from its identifier
     * @param identifier Identifier of the hologram to fetch
     * @return Hologram or NULL
     * */
    public static @Nullable Hologram getHologram(String identifier) {
        Hologram hologram = registeredHolograms.get(identifier);
        if (hologram == null) {
            hologram = registeredHolograms.get(hologramAliases.get(identifier));
        }
        return hologram;
    }

    /**
     * Define an Alias for a holograms UUID
     * @param identifier UUID of the hologram
     * @param alias Alias to define for the UUID or NULL to clear Alias for the UUID
     * @return True if the alias is created, False if the hologram does not exist or the alias is already defined.
     * */
    public static boolean defineAlias(String identifier, String alias) {
        if(alias == null){
            alias = fetchAlias(identifier);
            if(alias == null){
                return false;
            }
            hologramAliases.remove(alias);
            return true;
        }
        Hologram hologram = registeredHolograms.get(identifier); //Does the hologram exist?
        if (hologram == null) return false; //If not, return false
        if(hologramAliases.containsKey(alias)) return false; //Does this alias already exist?
        hologramAliases.put(alias, identifier); //Create the alias
        return true;
    }

    /**
     * Fetch alias from the Holograms UUID
     * @param identifier - UUID to find the alias of
     * @return The alias of the Hologram or NULL of none exists
     * */
    public static String fetchAlias(String identifier) {
        for (Map.Entry<String, String> entry : hologramAliases.entrySet()) {
            if(identifier.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Event triggered when the server is shutting down.
     * Generally handled by the plugin its self and should not need third party triggers
     * */
    public static void onServerDisable(){
        registeredHolograms.forEach((id, hologram) -> {
            hologram.onDisable();
        });
    }

    /**
     * Get all hologram UUIDs and all Hologram Aliases
     * @return A Set of all valid hologram identifiers
     * */
    public static Set<String> getValidHologramIdentifiers(){
        Set<String> validIdentifiers = new HashSet<>();
        validIdentifiers.addAll(registeredHolograms.keySet());
        validIdentifiers.addAll(hologramAliases.keySet());
        return validIdentifiers;
    }

}
