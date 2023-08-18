package ru.lebedinets.mc.autochunkloader;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class AutoChunkLoader extends JavaPlugin {

    private ConfigManager configManager;
    private EventHandlers eventHandlers;

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("AutoChunkLoader has been started!");

        configManager = new ConfigManager(this);

        eventHandlers = new EventHandlers(this, configManager);
        getServer().getPluginManager().registerEvents(eventHandlers, this);

        // Schedule a repeating task to check and unload chunks without minecarts
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, eventHandlers::unloadExpiredChunks, 0, configManager.getUnloadPeriod());

        Commands commands = new Commands(this, configManager, eventHandlers);
        getCommand("acl").setExecutor(commands);
        getCommand("autochunkloader").setExecutor(commands);

        // bStats
        int pluginId = 19552;
        Metrics metrics = new Metrics(this, pluginId);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("AutoChunkLoader has been stopped!");
    }
}
