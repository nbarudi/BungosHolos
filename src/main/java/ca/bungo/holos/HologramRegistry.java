package ca.bungo.holos;


import ca.bungo.holos.api.holograms.Hologram;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.*;

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
        if(BungosHolos.DISABLED) return;
        registeredHolograms.put(hologram.getUniqueIdentifier(), hologram);
    }

    /**
     * Remove a hologram from the registry
     * @param hologram Hologram to remove
     * */
    public static void unregisterHologram(Hologram hologram) {
        if(BungosHolos.DISABLED) return;
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
        if(BungosHolos.DISABLED) return;
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
        for(Hologram hologram : registeredHolograms.values()) {
            try {
                hologram.onDisable();
            } catch(IOException e){
                BungosHolos.LOGGER.error("Error while disabling hologram {}", hologram.getUniqueIdentifier(), e);
            }
        }

        File file = new File(JavaPlugin.getProvidingPlugin(HologramRegistry.class).getDataFolder(), "holograms.yml");
        if(!file.exists()) return; //If no file that means no holograms to load.
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("holograms");
        if(section == null) return;

        List<String> toRemove = new ArrayList<>();
        for(String uuid : section.getKeys(false)) {
            if(!registeredHolograms.containsKey(uuid)) {
                toRemove.add(uuid);
            }
        }
        for(String uuid : toRemove) {
            section.set(uuid, null);
        }
        try {
            config.save(file);
        } catch (IOException e) {
            BungosHolos.LOGGER.error("Error while saving holograms", e);
        }
    }

    /**
     * Triggered when the plugin is enabled
     * Used to load any persistent holograms into the plugins record
     * to make them editable via the plugin commands
     * */
    public static void onServerEnable(){
        File file = new File(JavaPlugin.getProvidingPlugin(HologramRegistry.class).getDataFolder(), "holograms.yml");
        if(!file.exists()) return; //If no file that means no holograms to load.
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("holograms");
        if(section == null) return;

        for(String uuid : section.getKeys(false)) {
            try {
                Hologram hologram = (Hologram) section.get(uuid);
                if(hologram == null || hologram.getUniqueIdentifier() == null) continue;
                registerHologram(hologram);
            } catch (ClassCastException e) {
                BungosHolos.LOGGER.warn("Failed to load hologram {}", uuid, e);
            }
        }
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
