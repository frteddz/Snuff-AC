package dev.snuffac.core.check;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.core.config.CheckConfig;
import dev.snuffac.core.packet.PacketType;import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class CheckRegistry {

    private final Map<String, Check> checks = new LinkedHashMap<>();
    private final Map<String, CheckConfig> configurations = new LinkedHashMap<>();

    private final Map<PacketType, List<Check>> packetDispatch = new EnumMap<>(PacketType.class);
    private final List<Check> tickDispatch = new ArrayList<>();
    private final List<Check> allDispatch = new ArrayList<>();
    private final Map<CheckCategory, List<Check>> byCategory = new EnumMap<>(CheckCategory.class);

    private boolean frozen;

    public void register(Check check) {
        if (frozen) {
            throw new IllegalStateException("Check registry is already frozen");
        }
        String key = check.key().toLowerCase(Locale.ROOT);
        if (checks.containsKey(key)) {
            throw new IllegalStateException("Duplicate check key: " + key);
        }
        checks.put(key, check);
        configurations.put(key, CheckConfig.defaults(check.category()));
    }

    public void freeze() {
        if (frozen) {
            return;
        }
        frozen = true;
        for (Check check : checks.values()) {
            check.onEnable(this);
        }
        rebuildDispatch();
    }

    public void rebuildDispatch() {
        packetDispatch.clear();
        tickDispatch.clear();
        allDispatch.clear();
        byCategory.clear();

        for (Check check : checks.values()) {
            allDispatch.add(check);
            byCategory.computeIfAbsent(check.category(), ignored -> new ArrayList<>()).add(check);
            Set<PacketType> interests = check.packetInterests();
            if (interests == null || interests.isEmpty()) {
                continue;
            }
            for (PacketType type : interests) {
                packetDispatch.computeIfAbsent(type, ignored -> new ArrayList<>()).add(check);
            }
        }
        for (Check check : checks.values()) {
            if (!check.packetInterests().isEmpty()) {
                tickDispatch.add(check);
            }
        }
    }

    public List<Check> dispatchFor(PacketType type) {
        List<Check> result = packetDispatch.get(type);
        return result == null ? Collections.emptyList() : result;
    }

    public List<Check> tickDispatch() {
        return Collections.unmodifiableList(allDispatch);
    }

    public List<Check> all() {
        return Collections.unmodifiableList(allDispatch);
    }

    public List<Check> byCategory(CheckCategory category) {
        return Collections.unmodifiableList(byCategory.getOrDefault(category, Collections.emptyList()));
    }

    public Collection<Check> registered() {
        return Collections.unmodifiableCollection(checks.values());
    }

    public Check check(String key) {
        return checks.get(key.toLowerCase(Locale.ROOT));
    }

    public boolean exists(String key) {
        return checks.containsKey(key.toLowerCase(Locale.ROOT));
    }

    public CheckConfig config(String key) {
        return configurations.get(key.toLowerCase(Locale.ROOT));
    }

    public CheckConfig config(Check check) {
        return configurations.get(check.key().toLowerCase(Locale.ROOT));
    }

    public Map<String, CheckConfig> configurations() {
        return Collections.unmodifiableMap(configurations);
    }

    public boolean isFrozen() {
        return frozen;
    }

    public void loadConfigurations(dev.snuffac.core.config.ConfigSource source) {
        for (Check check : checks.values()) {
            CheckConfig checkConfig = configurations.get(check.key().toLowerCase(Locale.ROOT));
            if (checkConfig == null) {
                continue;
            }
            String base = "checks." + check.category().name().toLowerCase(Locale.ROOT) + "." + check.key().toLowerCase(Locale.ROOT);
            checkConfig.applyFrom(source, base);
        }
    }

    public void saveConfigurations(dev.snuffac.core.config.ConfigSource source) {
        for (Check check : checks.values()) {
            CheckConfig checkConfig = configurations.get(check.key().toLowerCase(Locale.ROOT));
            if (checkConfig == null) {
                continue;
            }
            String base = "checks." + check.category().name().toLowerCase(Locale.ROOT) + "." + check.key().toLowerCase(Locale.ROOT);
            checkConfig.writeTo(source, base);
        }
    }
}
