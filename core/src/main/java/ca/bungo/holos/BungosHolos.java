package ca.bungo.holos;

import ca.bungo.holos.abstracted.PacketHelper;
import ca.bungo.holos.abstracted.PacketService;
import ca.bungo.holos.api.animations.simple.BounceSimpleAnimation;
import ca.bungo.holos.api.animations.simple.HorizontalSimpleAnimation;
import ca.bungo.holos.api.animations.simple.VerticalSimpleAnimation;
import ca.bungo.holos.api.holograms.simple.BlockSimpleHologram;
import ca.bungo.holos.api.holograms.simple.ItemSimpleHologram;
import ca.bungo.holos.api.holograms.simple.TextSimpleHologram;
import ca.bungo.holos.api.holograms.unique.Player3DSkinHologram;
import ca.bungo.holos.api.holograms.unique.image.ImageHologram;
import ca.bungo.holos.commands.HologramCommand;
import ca.bungo.holos.commands.TestCommand;
import ca.bungo.holos.handlers.PlaceholderHandler;
import ca.bungo.holos.handlers.placeholder.GenericPlaceholders;
import ca.bungo.holos.handlers.placeholder.MiniPlaceholdersPlaceholders;
import ca.bungo.holos.handlers.placeholder.PAPIPlaceholders;
import ca.bungo.holos.registries.AnimationRegistry;
import ca.bungo.holos.registries.HologramRegistry;
import ca.bungo.holos.utility.ComponentUtility;
import co.aikar.commands.PaperCommandManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

import java.lang.reflect.InvocationTargetException;

public class BungosHolos extends JavaPlugin {

    public static Logger LOGGER;
    public static PaperCommandManager commandManager;
    public static boolean DISABLED = false;

    private static BungosHolos instance;

    public HologramRegistry hologramRegistry = new HologramRegistry();
    public AnimationRegistry animationRegistry = new AnimationRegistry();
    public PacketService packetService;
    public PlaceholderHandler placeholderHandler = new GenericPlaceholders();

    @Override
    public void onEnable() {
        instance = this;
        DISABLED = false;

        //Registering Holograms
        ConfigurationSerialization.registerClass(TextSimpleHologram.class);
        ConfigurationSerialization.registerClass(ItemSimpleHologram.class);
        ConfigurationSerialization.registerClass(BlockSimpleHologram.class);
        ConfigurationSerialization.registerClass(ImageHologram.Player2DSkinHologram.class);
        ConfigurationSerialization.registerClass(Player3DSkinHologram.class);

        //Registering Animations
        ConfigurationSerialization.registerClass(BounceSimpleAnimation.class);
        ConfigurationSerialization.registerClass(VerticalSimpleAnimation.class);
        ConfigurationSerialization.registerClass(HorizontalSimpleAnimation.class);

        LOGGER = getSLF4JLogger();

        loadPacketService();

        registerCommands();
        registerEvents();

        saveDefaultConfig();

        hologramRegistry.onServerEnable();

        if(Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")){
           placeholderHandler = new PAPIPlaceholders();
           LOGGER.info("Running PlaceholderAPI Placeholder Handler.");
        }
        else if(Bukkit.getPluginManager().isPluginEnabled("MiniPlaceholders")){
            placeholderHandler = new MiniPlaceholdersPlaceholders();
            LOGGER.info("Running MiniPlaceholders Placeholder Handler.");
        }
        else {
            LOGGER.info("Running Generic Placeholder Handler.");
        }
    }

    private void registerCommands() {
        commandManager = new PaperCommandManager(this);
        commandManager.enableUnstableAPI("help");

        commandManager.registerCommand(new TestCommand());
        commandManager.registerCommand(new HologramCommand());
    }

    private void registerEvents() {
    }

    private void loadPacketService() {
        String version = Bukkit.getServer().getMinecraftVersion();

        PacketHelper helper = () -> instance;
        String packageString = "ca.bungo.holos.abstracted.v{}.VersionedPacketService";
        switch (version) {
            case "1.21.1":
            case "1.21.3":
            case "1.21.4":
            default:
                try {
                    Class<?> handler = Class.forName(ComponentUtility.format(packageString, "1_21_R1"));
                    this.packetService = (PacketService) handler.getConstructor(PacketHelper.class).newInstance(helper);
                } catch(ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException |
                        InvocationTargetException e){
                    LOGGER.error("Failed to load packet service for {}!", version, e);
                }
                break;
        }

        LOGGER.info("BungosHolos PacketService Loaded: {}", packetService.getVersion());
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
