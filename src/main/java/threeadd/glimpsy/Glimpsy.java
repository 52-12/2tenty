package threeadd.glimpsy;

import com.github.retrooper.packetevents.PacketEvents;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.NamespacedKey;
import org.bukkit.event.Listener;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import threeadd.glimpsy.util.command.CustomCommand;
import threeadd.glimpsy.util.command.CustomCommandRegistry;
import threeadd.glimpsy.util.listener.ListenerRegistry;
import threeadd.glimpsy.util.manager.Manager;
import threeadd.glimpsy.util.manager.ManagerRegistry;
import threeadd.glimpsy.util.config.ConfigManager;
import threeadd.glimpsy.util.config.Messages;
import threeadd.glimpsy.util.persistence.DataBase;
import threeadd.glimpsy.util.recipes.CustomRecipe;
import threeadd.glimpsy.util.recipes.CustomRecipeRegistry;
import threeadd.glimpsy.util.worldguard.WorldGuardFlags;

import java.util.List;
import java.util.function.Predicate;

public final class Glimpsy extends JavaPlugin {

    private static final Logger log = LoggerFactory.getLogger(Glimpsy.class);
    private static Glimpsy instance;
    private static DataBase dataBase;

    public static Glimpsy getInstance() {
        return instance;
    }

    public static DataBase getDataBase() {
        return dataBase;
    }

    @Override
    public void onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();

        // Must happen here, not onEnable() - WorldGuard's flag registry only accepts new flags
        // before WorldGuard's own onEnable() runs, and Bukkit finishes every plugin's onLoad()
        // before starting any plugin's onEnable(), regardless of load order between us and it.
        if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            WorldGuardFlags.register();
            log.info("Registered custom WorldGuard flags");
        }
    }

    @Override
    public void onEnable() {
        long start = System.nanoTime();

        Glimpsy.instance = this;

        ConfigManager.load();
        Messages.load();
        PacketEvents.getAPI().init();
        PacketEvents.getAPI().getEventManager().registerListener(new threeadd.glimpsy.feature.simple.FishingSoundFilter());

        if (!connectDataBase()) // If connecting failed, stop plugin from enabling
            return;

        List<Manager> managers = ManagerRegistry.INSTANCE.getRegisteredItems();
        managers.forEach(Manager::enable); // enable managers, allow listener managers to register to listener registry
        log.info("Enabled {} managers", managers.size());

        List<CustomRecipe> recipes = CustomRecipeRegistry.INSTANCE.getRegisteredItems();
        for (CustomRecipe recipe : recipes)
            if (recipe.isRegistered()) getServer().addRecipe(recipe.createRecipe());
        List<NamespacedKey> removedRecipeKeys = CustomRecipeRegistry.INSTANCE.getRemovedRecipes();
        for (NamespacedKey recipeKey : removedRecipeKeys)
            getServer().removeRecipe(recipeKey);
        log.info("Registered {} recipes, removed {} default recipes", recipes.size(), removedRecipeKeys.size());

        List<Listener> listeners = ListenerRegistry.INSTANCE.getRegisteredItems();
        listeners.forEach(listener ->
                getServer().getPluginManager().registerEvents(listener, this)); // register the listeners to make for the server
        log.info("Registered {} listeners", listeners.size());

        List<CustomCommand> commands = CustomCommandRegistry.INSTANCE.getRegisteredItems();

        // Every command gets a "2tenty.command.<name>" node, granted to everyone by default so
        // a permissions plugin (e.g. LuckPerms) can revoke it per group/player without any
        // default-group setup. Guarded against re-registration for plugin-reload tools.
        for (CustomCommand command : commands) {
            if (getServer().getPluginManager().getPermission(command.getPermissionNode()) == null)
                getServer().getPluginManager().addPermission(
                        new Permission(command.getPermissionNode(), command.getPermissionDefault()));
        }

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, registrar -> {
            for (CustomCommand command : commands) {
                LiteralArgumentBuilder<CommandSourceStack> builder = command.create();

                // Layer the node check on top of any .requires() the command already set
                // (staff commands keep their own stricter node) - both must pass.
                Predicate<CommandSourceStack> existing = builder.getRequirement();
                String node = command.getPermissionNode();
                builder.requires(source -> existing.test(source) && source.getSender().hasPermission(node));

                registrar.registrar().register(builder.build(), command.getAliases());
            }
        });
        log.info("Registered {} commands", commands.size());

        // DiscordSRV dispatches its own API events through its own event bus, not Bukkit's -
        // referencing DiscordSRVChatBridgeListener at all resolves DiscordSRV's classes, so it
        // must only happen once we've confirmed DiscordSRV is actually installed.
        if (getServer().getPluginManager().isPluginEnabled("DiscordSRV")) {
            threeadd.glimpsy.feature.discordsrv.DiscordSRVChatBridgeListener.register();
            log.info("Registered DiscordSRV chat mute bridge");
        }

        long end = System.nanoTime();
        double milliTime = (end - start) / 1_000_000D;
        log.info("2tenty has finished starting up in {}ms", milliTime);
    }

    @Override
    public void onDisable() {
        long start = System.nanoTime();

        PacketEvents.getAPI().terminate();

        if (dataBase != null && dataBase.isConnected()) {
            ManagerRegistry.INSTANCE.getRegisteredItems().forEach(Manager::disable);
            dataBase.disconnect();
        }

        long end = System.nanoTime();
        double milliTime = (end - start) / 1_000_000D;
        log.info("2tenty has been stopped in {}ms", milliTime);
    }

    private boolean connectDataBase() {
        Glimpsy.dataBase = new DataBase(new java.io.File(getDataFolder(), "database.db"));
        try {
            Glimpsy.dataBase.connect();
        } catch (Throwable t) {
            log.error("Couldn't connect to the database or init tables", t);
            getServer().getPluginManager().disablePlugin(this);

            return false; // Failed
        }

        log.info("Successfully connected to the database");
        return true; // Success
    }
}
