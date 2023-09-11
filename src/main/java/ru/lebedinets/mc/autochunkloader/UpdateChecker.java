package ru.lebedinets.mc.autochunkloader;

import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.plugin.PluginDescriptionFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class UpdateChecker {
    private final PluginDescriptionFile pluginDescription;
    private final Server server;
    private final String pluginName;
    private final String pluginVersion;

    UpdateChecker(PluginDescriptionFile pluginDescription, Server server) {
        this.pluginDescription = pluginDescription;
        this.server = server;
        this.pluginVersion = pluginDescription.getVersion();
        this.pluginName = pluginDescription.getName();
    }

    private void sendLog(String message) {
        server.getConsoleSender().sendMessage("[" + pluginName + "] " + message);
    }

    public void checkForUpdates() {
        try {
            URL url = new URL("https://lebedinets.ru/mcupd/check.php?group=bukkit&plugin=" + pluginName + "&cv=" + pluginVersion);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            if (con.getResponseCode() != HttpURLConnection.HTTP_OK) {
                sendLog(ChatColor.RED + "Failed to check updates for plugin! HTTP Code: " + con.getResponseCode());
                return;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            String responseData = response.toString();
            parseString(responseData);

        } catch (IOException e) {
            sendLog(ChatColor.RED + "Failed to check updates for plugin! Exception:\n" + e);
        }
    }

    private void parseString(String responseData) {
        String[] parts = responseData.split("\\|");
        Map<String, String> keyValueMap = new HashMap<>();

        for (int i = 0; i < parts.length; i += 2) {
            if (i + 1 < parts.length) {
                String key = parts[i];
                String value = parts[i + 1];
                keyValueMap.put(key, value);
            }
        }

        if (keyValueMap.containsKey("message")) {
            String message = keyValueMap.get("message").replaceAll("<br/>", "\n");
            String messageType = "info";
            if (keyValueMap.containsKey("messageType")) {
                messageType = keyValueMap.get("messageType");
            }

            if (messageType.equals("info")) {
                sendLog(ChatColor.GOLD + "Update message: " + message);
            }
            if (messageType.equals("critical")) {
                sendLog(ChatColor.RED + "!!! Critical update message: " + message);
            }
        }

        if (!keyValueMap.containsKey("lastVer")) {
            sendLog(ChatColor.RED + "Failed to check updates for plugin! Last version not found in response!");
            return;
        }

        String lastVersion = keyValueMap.get("lastVer");
        String downloadUrl = pluginDescription.getWebsite();

        if (keyValueMap.containsKey("downloadUrl")) {
            downloadUrl = keyValueMap.get("downloadUrl");
        }

        if (!pluginVersion.equals(lastVersion)) {
            sendLog(ChatColor.AQUA + "New update available: v" + lastVersion + " / Current: v" + pluginVersion + "\n" + "You can download it here: " + downloadUrl);
        }
    }
}
