package ca.bungo.holos;

import ca.bungo.holos.commands.HologramCommand;
import ca.bungo.holos.commands.TestCommand;
import co.aikar.commands.PaperCommandManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

public class BungosHolos extends JavaPlugin {

    public static Logger LOGGER;
    public static PaperCommandManager commandManager;

    @Override
    public void onEnable() {
        LOGGER = getSLF4JLogger();

        registerCommands();
    }

    private void registerCommands() {
        commandManager = new PaperCommandManager(this);
        commandManager.enableUnstableAPI("help");

        commandManager.registerCommand(new TestCommand());
        commandManager.registerCommand(new HologramCommand());
    }

    @Override
    public void onDisable() {
        HologramRegistry.onServerDisable();
    }
}
