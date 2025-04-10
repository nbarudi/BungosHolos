package ca.bungo.holos;

import ca.bungo.holos.commands.TestCommand;
import co.aikar.commands.PaperCommandManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

public class BungosHolos extends JavaPlugin {

    public static Logger LOGGER;

    @Override
    public void onEnable() {
        LOGGER = getSLF4JLogger();

        registerCommands();
    }

    private void registerCommands() {
        PaperCommandManager manager = new PaperCommandManager(this);

        manager.registerCommand(new TestCommand());
    }

    @Override
    public void onDisable() {

    }
}
