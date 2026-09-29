package dev.snuffac.core.violation;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.api.SnuffPlatform;
import dev.snuffac.api.violation.ViolationInfo;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

public final class ViolationHistoryStore implements AutoCloseable {

    private static final String EXTENSION = ".history";
    private static final char FIELD = '\t';

    private final Path directory;
    private final int retentionDays;
    private final int perPlayerLimit;
    private final Executor writer;
    private final SnuffPlatform platform;
    private final ConcurrentHashMap<UUID, Deque<ViolationInfo>> cache = new ConcurrentHashMap<>();

    public ViolationHistoryStore(Path directory, int retentionDays, int perPlayerLimit, Executor writer, SnuffPlatform platform) {
        this.directory = directory;
        this.retentionDays = retentionDays;
        this.perPlayerLimit = perPlayerLimit;
        this.writer = writer;
        this.platform = platform;
    }

    public int clear(UUID playerId) {
        if (playerId == null) {
            return 0;
        }
        Deque<ViolationInfo> history = cache.remove(playerId);
        int removed = history == null ? 0 : history.size();
        if (directory != null) {
            try {
                Files.deleteIfExists(fileFor(playerId));
            } catch (java.io.IOException ignored) {
            }
        }
        return removed;
    }

    public int clearAll() {
        int removed = 0;
        for (UUID id : java.util.List.copyOf(cache.keySet())) {
            removed += clear(id);
        }
        return removed;
    }

    public void record(ViolationInfo info) {
        if (info == null) {
            return;
        }
        Deque<ViolationInfo> history = cache.computeIfAbsent(info.playerId(), key -> new ArrayDeque<>());
        synchronized (history) {
            history.addLast(info);
            while (history.size() > perPlayerLimit) {
                history.removeFirst();
            }
        }
        submit(info);
    }

    private void submit(ViolationInfo info) {
        Runnable task = () -> appendLine(info);
        if (writer == null) {
            task.run();
            return;
        }
        try {
            writer.execute(task);
        } catch (RuntimeException rejected) {
            task.run();
        }
    }

    public List<ViolationInfo> history(UUID playerId) {
        load(playerId, null);
        Deque<ViolationInfo> history = cache.get(playerId);
        if (history == null || history.isEmpty()) {
            return List.of();
        }
        synchronized (history) {
            return new ArrayList<>(history);
        }
    }

    public int total(UUID playerId) {
        load(playerId, null);
        return history(playerId).size();
    }

    public int totalInLastHours(UUID playerId, int hours) {
        load(playerId, null);
        long cutoff = System.currentTimeMillis() - Duration.ofHours(hours).toMillis();
        int count = 0;
        for (ViolationInfo info : history(playerId)) {
            if (info.timestampMillis() >= cutoff) {
                count++;
            }
        }
        return count;
    }

    public void load(UUID playerId, String playerName) {
        if (cache.containsKey(playerId)) {
            return;
        }
        Path file = fileFor(playerId);
        if (!Files.isRegularFile(file)) {
            return;
        }
        Deque<ViolationInfo> loaded = new ArrayDeque<>();
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                ViolationInfo parsed = parse(line, playerId, playerName);
                if (parsed != null) {
                    loaded.addLast(parsed);
                }
            }
        } catch (IOException | UncheckedIOException exception) {
            return;
        }
        while (loaded.size() > perPlayerLimit) {
            loaded.removeFirst();
        }
        cache.put(playerId, loaded);
    }

    public void flush(UUID playerId) {
        cache.computeIfPresent(playerId, (key, history) -> {
            synchronized (history) {
                return new ArrayDeque<>(history);
            }
        });
    }

    private Path fileFor(UUID playerId) {
        return directory.resolve(playerId.toString().replace("-", "") + EXTENSION);
    }

    private void appendLine(ViolationInfo info) {
        try {
            Files.createDirectories(directory);
            Files.writeString(fileFor(info.playerId()), encode(info) + System.lineSeparator(),
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException | UncheckedIOException exception) {
            return;
        }
    }

    private String encode(ViolationInfo info) {
        return String.join(String.valueOf(FIELD),
                Long.toString(info.timestampMillis()),
                info.checkKey(),
                info.checkName(),
                info.category().name(),
                Double.toString(info.violationLevel()),
                Double.toString(info.buffer()),
                Double.toString(info.confidence()),
                Double.toString(info.pingMillis()),
                Double.toString(info.tps()),
                oneLine(info.playerName()),
                oneLine(info.detail()),
                info.worldName() == null ? "" : oneLine(info.worldName()),
                Double.toString(info.x()),
                Double.toString(info.y()),
                Double.toString(info.z()));
    }

    private ViolationInfo parse(String line, UUID playerId, String fallbackName) {
        if (line == null || line.isBlank()) {
            return null;
        }
        String[] parts = line.split(String.valueOf(FIELD), -1);
        if (parts.length < 11) {
            return null;
        }

        try {
            CheckCategory category = CheckCategory.valueOf(parts[3].toUpperCase(Locale.ROOT));
            return new ViolationInfo(
                    playerId,
                    parts[9].isBlank() ? fallbackName : parts[9],
                    category,
                    parts[1],
                    parts[2],
                    parts[10],
                    Double.parseDouble(parts[4]),
                    Double.parseDouble(parts[5]),
                    Double.parseDouble(parts[6]),
                    Double.parseDouble(parts[7]),
                    Double.parseDouble(parts[8]),
                    Long.parseLong(parts[0]),
                    platform,
                    parts.length > 11 ? parts[11] : "",
                    parts.length > 12 ? Double.parseDouble(parts[12]) : 0.0,
                    parts.length > 13 ? Double.parseDouble(parts[13]) : 0.0,
                    parts.length > 14 ? Double.parseDouble(parts[14]) : 0.0);
        } catch (IllegalArgumentException malformed) {
            return null;
        }
    }

    private static String oneLine(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ').trim();
    }

    public void purgeExpired() {
        if (retentionDays <= 0 || !Files.isDirectory(directory)) {
            return;
        }
        long cutoff = Instant.now().minus(Duration.ofDays(retentionDays)).toEpochMilli();
        try (var stream = Files.list(directory)) {
            stream.filter(path -> path.getFileName().toString().endsWith(EXTENSION)).forEach(path -> {
                try {
                    if (Files.getLastModifiedTime(path).toMillis() < cutoff) {
                        Files.deleteIfExists(path);
                    }
                } catch (IOException | UncheckedIOException ignored) {
                }
            });
        } catch (IOException | UncheckedIOException exception) {
            return;
        }
    }

    @Override
    public void close() {
        cache.clear();
    }
}
