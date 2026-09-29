package dev.snuffac.paper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;

public final class PermitManager implements AutoCloseable {

    private static final char FIELD = '\t';

    private final Path file;
    private final Map<UUID, Entry> bypass = new LinkedHashMap<>();
    private final Map<UUID, String> names = new LinkedHashMap<>();

    public record Entry(boolean granted, String by, long millis) {
    }

    public PermitManager(Path file) {
        this.file = file;
    }

    public synchronized void load() {
        bypass.clear();
        names.clear();
        if (file == null || !Files.isRegularFile(file)) {
            return;
        }
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                String[] parts = line.split("\t");
                if (parts.length < 4) {
                    continue;
                }
                UUID id = parse(parts[0]);
                if (id == null) {
                    continue;
                }
                bypass.put(id, new Entry(
                        !"false".equalsIgnoreCase(parts[1]), parts[2], Long.parseLong(parts[3])));
                if (parts.length > 4) {
                    names.put(id, parts[4]);
                }
            }
        } catch (IOException | RuntimeException ignored) {
        }
    }

    public synchronized boolean bypassed(UUID id) {
        if (id == null) {
            return false;
        }
        Entry entry = bypass.get(id);
        return entry != null && entry.granted();
    }

    public synchronized String setBypass(UUID id, boolean grant, String staff, String name) {
        if (id == null || name == null || name.isBlank()) {
            return null;
        }
        bypass.put(id, new Entry(grant, staff == null ? "console" : staff,
                System.currentTimeMillis()));
        names.put(id, name);
        save();
        return name;
    }

    public synchronized String setBypass(UUID id, boolean grant, String staff) {
        return setBypass(id, grant, staff, null);
    }

    public synchronized boolean isBypassed(UUID id) {
        return bypassed(id);
    }

    public synchronized String nameOf(UUID id) {
        return names.get(id);
    }

    public synchronized int count() {
        return (int) bypass.values().stream().filter(Entry::granted).count();
    }

    @Override
    public synchronized void close() {
        save();
    }

    private static UUID parse(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException malformed) {
            return null;
        }
    }

    private synchronized void save() {
        if (file == null) {
            return;
        }
        StringBuilder builder = new StringBuilder("# Snuff AC bypass grants\n");
        for (Map.Entry<UUID, Entry> entry : bypass.entrySet()) {
            builder.append(entry.getKey())
                    .append(FIELD)
                    .append(entry.getValue().granted())
                    .append(FIELD)
                    .append(entry.getValue().by().replace(FIELD, ' '))
                    .append(FIELD)
                    .append(entry.getValue().millis())
                    .append(FIELD)
                    .append(names.getOrDefault(entry.getKey(), ""))
                    .append('\n');
        }
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(file, builder.toString(), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }
}
