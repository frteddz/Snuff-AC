package dev.snuffac.paper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class YamlConfigurationLoader {

    private YamlConfigurationLoader() {
    }

    public static org.bukkit.configuration.file.YamlConfiguration load(JavaPlugin plugin, String name) {
        File file = new File(plugin.getDataFolder(), name);
        if (file.exists()) {
            return YamlConfiguration.loadConfiguration(file);
        }
        try (InputStream stream = plugin.getResource(name)) {
            if (stream == null) {
                return new YamlConfiguration();
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return YamlConfiguration.loadConfiguration(reader);
            }
        } catch (IOException exception) {
            plugin.getLogger().warning("failed to read " + name + ": " + exception.getMessage());
            return new YamlConfiguration();
        }
    }

    public static void store(JavaPlugin plugin, String name, YamlConfigSource source) {
        File file = new File(plugin.getDataFolder(), name);
        try {
            if (!plugin.getDataFolder().isDirectory() && !plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().warning("could not create data folder " + plugin.getDataFolder());
                return;
            }
            org.bukkit.configuration.file.FileConfiguration configuration =
                    new org.bukkit.configuration.file.YamlConfiguration();
            for (Map.Entry<String, Object> entry : source.asMap().entrySet()) {
                configuration.set(entry.getKey(), entry.getValue());
            }
            configuration.save(file);
        } catch (IOException exception) {
            plugin.getLogger().warning("failed to save " + name + ": " + exception.getMessage());
        }
    }
}
