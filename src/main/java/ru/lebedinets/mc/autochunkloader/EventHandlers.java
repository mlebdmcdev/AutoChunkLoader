package ru.lebedinets.mc.autochunkloader;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventHandlers implements Listener {
    private final Plugin plugin;
    private final ConfigManager configManager;

    private long lastCooldownTime = 0L;
    private int loadedChunksCount = 0;
    private Map<Chunk, Long> loadedChunks = new HashMap<>();

    public EventHandlers(Plugin plugin, ConfigManager configMgr) {
        this.plugin = plugin;
        this.configManager = configMgr;
    }

    private boolean checkChunkLimit() {
        if (loadedChunksCount > configManager.getMaxLoadedChunks()) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastCooldownTime >= configManager.getWarningCooldown()) {
                lastCooldownTime = currentTime;

                if (configManager.getDisableWarnings()) {
                    return false;
                }
                // Print a warning to the console
                plugin.getLogger().warning("Force loaded chunks limit reached! (" +configManager.getMaxLoadedChunks() + ")");
                // Notify ops
                for (Player op : Bukkit.getOnlinePlayers()) {
                    if (op.isOp()) {
                        op.sendMessage(ChatColor.RED + "[AutoChunkLoader] Force loaded chunks limit reached! (" +
                                configManager.getMaxLoadedChunks() + ")");
                    }
                }
            }
            return false;
        } else {
            return true;
        }
    }

    private boolean isWorldAllowed(World world) {
        List<String> worlds = configManager.getWorlds();
        String worldFilterMode = configManager.getWorldFilterMode();
        String worldName = world.getName();

        if (worldFilterMode == "whitelist" && worlds.contains(worldName)) {
            return true;
        }

        if (worldFilterMode == "blacklist" && !worlds.contains(worldName)) {
            return true;
        }

        return false;
    }

    @EventHandler
    public void onMinecartMove(VehicleMoveEvent event) {
        if (configManager.getDisableMinecarts()) {
            return;
        }

        if (isWorldAllowed(event.getVehicle().getWorld())) {
            return;
        }

        int chunkLoadRadius = configManager.getChunkLoadRadius();
        long unloadDelay = configManager.getUnloadDelay();

        if (event.getVehicle() instanceof Minecart) {
            Minecart minecart = (Minecart) event.getVehicle();
            if (configManager.getDebugLog()) {
                plugin.getLogger().info("Minecart signal detected at " + minecart.getLocation());
            }

            Chunk chunk = minecart.getLocation().getChunk();
            World world = chunk.getWorld();

            if (!checkChunkLimit()) {
                return;
            }

            if (configManager.getDebugLog()) {
                plugin.getLogger().info("Loading additional chunks...");
            }

            // Load and set force-loaded for chunks around the minecart
            for (int x = -chunkLoadRadius; x <= chunkLoadRadius; x++) {
                for (int z = -chunkLoadRadius; z <= chunkLoadRadius; z++) {
                    int targetX = chunk.getX() + x;
                    int targetZ = chunk.getZ() + z;

                    Chunk targetChunk = world.getChunkAt(targetX, targetZ);
                    if (!loadedChunks.containsKey(targetChunk)) {
                        world.setChunkForceLoaded(targetX, targetZ, true);
                        chunk.load();
                        loadedChunksCount++;
                        loadedChunks.put(targetChunk, System.currentTimeMillis() + unloadDelay);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onRedstoneSignal(BlockRedstoneEvent event) {
        if (configManager.getDisableRedstone()) {
            return;
        }

        if (isWorldAllowed(event.getBlock().getWorld())) {
            return;
        }

        int chunkLoadRadius = configManager.getChunkLoadRadius();
        long unloadDelay = configManager.getUnloadDelay();

        Block redstoneBlock = event.getBlock();
        if (configManager.getDebugLog()) {
            plugin.getLogger().info("Redstone signal detected at " + redstoneBlock.getLocation());
        }

        World world = redstoneBlock.getWorld();
        Location redstoneLocation = redstoneBlock.getLocation();
        Chunk centerChunk = redstoneLocation.getChunk();

        if (!checkChunkLimit()) {
            return;
        }

        // Load and set force-loaded for chunks around the redstone block
        for (int x = -chunkLoadRadius; x <= chunkLoadRadius; x++) {
            for (int z = -chunkLoadRadius; z <= chunkLoadRadius; z++) {
                int targetX = centerChunk.getX() + x;
                int targetZ = centerChunk.getZ() + z;

                Chunk targetChunk = world.getChunkAt(targetX, targetZ);
                if (!loadedChunks.containsKey(targetChunk)) {
                    world.setChunkForceLoaded(targetX, targetZ, true);
                    centerChunk.load();
                    loadedChunksCount++;
                    loadedChunks.put(targetChunk, System.currentTimeMillis() + unloadDelay);
                }
            }
        }
    }

    public void unloadExpiredChunks() {
        long currentTime = System.currentTimeMillis();
        for (Map.Entry<Chunk, Long> entry : loadedChunks.entrySet()) {
            Chunk chunk = entry.getKey();
            long expirationTime = entry.getValue();

            if (currentTime >= expirationTime) {
                World world = chunk.getWorld();
                world.setChunkForceLoaded(chunk.getX(), chunk.getZ(), false);
                chunk.unload();
                loadedChunksCount--;
                if (configManager.getDebugLog()) {
                    plugin.getLogger().info("Unloading chunk (" + chunk.getX() + ", " + chunk.getZ() + ")...");
                }
            }
        }
        loadedChunks.entrySet().removeIf(entry -> currentTime >= entry.getValue());
    }

    public int getLoadedChunksCount() {
        return loadedChunksCount;
    }

    public void resetCooldown() {
        lastCooldownTime = 0L;
    }
}
