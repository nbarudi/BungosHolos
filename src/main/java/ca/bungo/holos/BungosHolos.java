package ca.bungo.holos;

import ca.bungo.holos.api.animations.simple.BounceSimpleAnimation;
import ca.bungo.holos.api.holograms.simple.BlockSimpleHologram;
import ca.bungo.holos.api.holograms.simple.ItemSimpleHologram;
import ca.bungo.holos.api.holograms.simple.TextSimpleHologram;
import ca.bungo.holos.commands.HologramCommand;
import ca.bungo.holos.commands.TestCommand;
import ca.bungo.holos.registries.AnimationRegistry;
import ca.bungo.holos.registries.HologramRegistry;
import co.aikar.commands.PaperCommandManager;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

public class BungosHolos extends JavaPlugin {

    public static Logger LOGGER;
    public static PaperCommandManager commandManager;
    public static boolean DISABLED = false;

    private static BungosHolos instance;

    public HologramRegistry hologramRegistry = new HologramRegistry();
    public AnimationRegistry animationRegistry = new AnimationRegistry();

    @Override
    public void onEnable() {
        instance = this;

        DISABLED = false;

        //Registering Holograms
        ConfigurationSerialization.registerClass(TextSimpleHologram.class);
        ConfigurationSerialization.registerClass(ItemSimpleHologram.class);
        ConfigurationSerialization.registerClass(BlockSimpleHologram.class);

        //Registering Animations
        ConfigurationSerialization.registerClass(BounceSimpleAnimation.class);


        LOGGER = getSLF4JLogger();

        registerCommands();

        saveDefaultConfig();

        hologramRegistry.onServerEnable();
    }

    private void registerCommands() {
        commandManager = new PaperCommandManager(this);
        commandManager.enableUnstableAPI("help");

        commandManager.registerCommand(new TestCommand());
        commandManager.registerCommand(new HologramCommand());
    }

    @Override
    public void onDisable() {
        DISABLED = true;
        hologramRegistry.onServerDisable();
    }

    public static BungosHolos get() {
        return instance;
    }
}
