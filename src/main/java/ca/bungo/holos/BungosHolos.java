package ca.bungo.holos;

import ca.bungo.holos.api.holograms.simple.ItemSimpleHologram;
import ca.bungo.holos.api.holograms.simple.TextSimpleHologram;
import ca.bungo.holos.commands.HologramCommand;
import ca.bungo.holos.commands.TestCommand;
import co.aikar.commands.PaperCommandManager;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

public class BungosHolos extends JavaPlugin {

    public static Logger LOGGER;
    public static PaperCommandManager commandManager;
    public static boolean DISABLED = false;

    @Override
    public void onEnable() {
        DISABLED = false;
        ConfigurationSerialization.registerClass(TextSimpleHologram.class);
        ConfigurationSerialization.registerClass(ItemSimpleHologram.class);
        LOGGER = getSLF4JLogger();

        registerCommands();

        saveDefaultConfig();

        HologramRegistry.onServerEnable();
    }

    private void registerCommands() {
        commandManager = new PaperCommandManager(this);
        commandManager.enableUnstableAPI("help");
        commandManager.enableUnstableAPI("brigadier");

        commandManager.registerCommand(new TestCommand());
        commandManager.registerCommand(new HologramCommand());
    }

    @Override
    public void onDisable() {
        DISABLED = true;
        HologramRegistry.onServerDisable();
    }
}
