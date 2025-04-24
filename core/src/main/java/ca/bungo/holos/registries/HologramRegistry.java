package ca.bungo.holos.registries;


import ca.bungo.holos.BungosHolos;
import ca.bungo.holos.api.holograms.Hologram;
import ca.bungo.holos.api.holograms.Packetable;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.*;

@Getter
public class HologramRegistry {

    private final Map<String, Hologram> registeredHolograms;
    private final Map<String, String> hologramAliases;

    public HologramRegistry() {
        this.registeredHolograms = new HashMap<>();
        this.hologramAliases = new HashMap<>();
    }

    /**
     * Handle packeted holograms ticking
     * */
    private void startPacketRunner() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for(Hologram hologram : registeredHolograms.values()) {
                    if(hologram instanceof Packetable packetable){
                        packetable.globalTickUpdate();
                    }
                }
            }
        }.runTaskTimer(BungosHolos.get(), 10, 10);
    }

    /**
     * Register a new hologram for event handling.
     * This will allow it to automatically clean up and save data on server boot and shutdown
     * Generally should be handled automatically by the hologram its self
     * @param hologram Hologram to register
     * */
    public void registerHologram(Hologram hologram) {
        if(BungosHolos.DISABLED) return;
        registeredHolograms.put(hologram.getUniqueIdentifier(), hologram);
    }

    /**
     * Remove a hologram from the registry
     * @param hologram Hologram to remove
     * */
    public void unregisterHologram(Hologram hologram) {
        if(BungosHolos.DISABLED) return;
        registeredHolograms.remove(hologram.getUniqueIdentifier());
        if(fetchAlias(hologram.getUniqueIdentifier()) != null) {
            defineAlias(hologram.getUniqueIdentifier(), null, false);
        }
    }

    /**
     * Remove a hologram from the registry
     * @param uniqueIdentifier Identifier of the hologram to remove
     * */
    public void unregisterHologram(String uniqueIdentifier) {
        if(BungosHolos.DISABLED) return;
        registeredHolograms.remove(uniqueIdentifier);
        if(fetchAlias(uniqueIdentifier) != null) {
            defineAlias(uniqueIdentifier, null, false);
        }
    }

    /**
     * Fetch a registered hologram from its identifier
     * @param identifier Identifier of the hologram to fetch
     * @return Hologram or NULL
     * */
    public @Nullable Hologram getHologram(String identifier) {
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
    public boolean defineAlias(String identifier, String alias, boolean forceAlias) {
        if(alias == null){
            alias = fetchAlias(identifier);
            if(alias == null){
                return false;
            }
            hologramAliases.remove(alias);
            return true;
        }
        Hologram hologram = registeredHolograms.get(identifier); //Does the hologram exist?
        if (hologram == null && !forceAlias) return false; //If not, return false
        if(hologramAliases.containsKey(alias)) return false; //Does this alias already exist?
        hologramAliases.put(alias, identifier); //Create the alias
        return true;
    }

    /**
     * Fetch alias from the Holograms UUID
     * @param identifier - UUID to find the alias of
     * @return The alias of the Hologram or NULL of none exists
     * */
    public String fetchAlias(String identifier) {
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
    public void onServerDisable(){
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
    public void onServerEnable(){
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

        startPacketRunner();
    }

    /**
     * Get all hologram UUIDs and all Hologram Aliases
     * @return A Set of all valid hologram identifiers
     * */
    public Set<String> getValidHologramIdentifiers(){
        Set<String> validIdentifiers = new HashSet<>();
        validIdentifiers.addAll(registeredHolograms.keySet());
        validIdentifiers.addAll(hologramAliases.keySet());
        return validIdentifiers;
    }


    /**
     * Handle any logic required for when a player joins the server.
     * This is mainly designed for Packeted holograms which have per-user data
     * @param whoJoined The player who joined the server
     * */
    public void handlePlayerJoin(Player whoJoined) {
        for(Hologram hologram : registeredHolograms.values()) {
            if(hologram instanceof Packetable packetable) {
                packetable.updatePlayer(whoJoined);
            }
        }
    }

    /**
     * Handle any logic required for when a player loads a chunk.
     * This is mainly designed for Packeted holograms which have per-user data
     * @param player Player who loaded the chunk
     * @param chunk Chunk the player loaded
     * */
    public void handlePlayerChunkLoad(Player player, Chunk chunk) {
        List<Hologram> inChunk = registeredHolograms.values()
                .stream()
                .filter((h) -> h.getLocation() != null)
                .filter((h) -> h.getLocation().getChunk().equals(chunk)).toList();
        for(Hologram hologram : inChunk) {
            if(hologram instanceof Packetable packetable) {
                packetable.updatePlayer(player);
            }
        }
    }

}
