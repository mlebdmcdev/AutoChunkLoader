package ru.lebedinets.mc.autochunkloader;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Commands implements CommandExecutor, TabCompleter {

    private final Plugin plugin;
    private final ConfigManager configManager;
    private final EventHandlers eventHandlers;

    public Commands(Plugin plugin, ConfigManager configMgr, EventHandlers eventHndl) {
        this.plugin = plugin;
        this.configManager = configMgr;
        this.eventHandlers = eventHndl;
    }

    private void reloadConfiguration(CommandSender sender) {
        configManager.reloadConfig();
        sender.sendMessage(ChatColor.GREEN + "Configuration reloaded.");
    }

    private void resetCooldown(CommandSender sender) {
        eventHandlers.resetCooldown();
        sender.sendMessage(ChatColor.GREEN + "Warning cooldown reset.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("autochunkloader") || command.getName().equalsIgnoreCase("acl")) {
            if (args.length > 0) {
                String subcommand = args[0].toLowerCase();
                switch (subcommand) {
                    case "reloadconfig":
                    case "recfg":
                    case "re":
                        if (sender.hasPermission("autochunkloader.reloadconfig")) {
                            reloadConfiguration(sender);
                        } else {
                            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                        }
                        break;

                    case "resetcooldown":
                    case "resetcd":
                    case "recd":
                    case "rst":
                    case "rs":
                        if (sender.hasPermission("autochunkloader.resetcooldown")) {
                            resetCooldown(sender);
                        } else {
                            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                        }
                        break;

                    case "stats":
                    case "stat":
                    case "st":
                    case "s":
                        if (sender.hasPermission("autochunkloader.stats")) {
                            sender.sendMessage(ChatColor.GREEN + "Total loaded chunks: " + eventHandlers.getLoadedChunksCount() + "/" + configManager.getMaxLoadedChunks());
                        } else {
                            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                        }
                        break;

                    case "about":
                    case "ab":
                    case "a":
                        sender.sendMessage(ChatColor.GREEN + "AutoChunkLoader plugin by mlebd (iwalfy) // v" + plugin.getDescription().getVersion() + "\n" +
                                "A simple plugin that loads chunks around long railways and redstone signals\n" +
                                ChatColor.GOLD + ChatColor.UNDERLINE + "https://github.com/iwalfy/AutoChunkLoader");
                        break;

                    default:
                        sender.sendMessage(ChatColor.RED + "Unknown subcommand");
                        break;
                }
            } else {
                sender.sendMessage(ChatColor.RED + "Usage: /acl <subcommand>");
            }
            return true;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> tabCompletions = new ArrayList<>();

        if (command.getName().equalsIgnoreCase("autochunkloader") || command.getName().equalsIgnoreCase("acl")) {
            if (args.length == 1) {
                String typedSubcommand = args[0].toLowerCase();
                for (String subcommand : Arrays.asList("reloadconfig", "resetcooldown", "stats", "about")) {
                    if (subcommand.startsWith(typedSubcommand)) {
                        tabCompletions.add(subcommand);
                    }
                }
            }
        }

        return tabCompletions;
    }
}
