package dev.snuffac.paper;

import dev.snuffac.core.config.ConfigSource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;

public final class YamlConfigSource implements ConfigSource {

    private final ConfigurationSection configuration;

    public YamlConfigSource(ConfigurationSection configuration) {
        this.configuration = configuration;
    }

    @Override
    public Object raw(String path) {
        return configuration.get(path);
    }

    @Override
    public boolean contains(String path) {
        return configuration.contains(path);
    }

    @Override
    public Set<String> keys(String path) {
        ConfigurationSection section = configuration.getConfigurationSection(path);
        if (section == null) {
            return Set.of();
        }
        return section.getKeys(false);
    }

    @Override
    public void set(String path, Object value) {
        configuration.set(path, value);
    }

    @Override
    public void save() {
    }

    @Override
    public Map<String, Object> asMap() {
        return toMap(configuration);
    }

    public ConfigurationSection configuration() {
        return configuration;
    }

    public static Map<String, Object> toMap(ConfigurationSection section) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            Object value = section.get(key);
            if (value instanceof ConfigurationSection child) {
                map.put(key, toMap(child));
            } else {
                map.put(key, value);
            }
        }
        return map;
    }

    @Override
    public List<String> getStringList(String path, List<String> fallback) {
        List<String> configured = configuration.getStringList(path);
        return configured.isEmpty() && fallback != null ? fallback : configured;
    }

    @Override
    public List<Integer> getIntegerList(String path, List<Integer> fallback) {
        List<Integer> configured = configuration.getIntegerList(path);
        if (configured.isEmpty() && fallback != null) {
            return new ArrayList<>(fallback);
        }
        return configured;
    }
}
