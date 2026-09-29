package dev.snuffac.core.punish;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

public final class PunishmentService implements AutoCloseable {

    public enum Kind {
        BAN("Ban"),
        TIMEOUT("Timeout"),
        TEMPBAN("Temporary Ban"),
        IPBAN("IP Ban"),
        TEMPIPBAN("Temporary IP Ban"),
        MUTE("Mute"),
        TEMPMUTE("Temporary Mute"),
        WARN("Warning");

        private final String label;

        Kind(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public record Punishment(
            UUID id,
            Kind kind,
            UUID targetId,
            String targetName,
            String targetIpHash,
            String reason,
            String staff,
            long createdMillis,
            long expiresMillis,
            boolean active,
            String removedBy,
            long removedMillis) {

        public long sequence() {
            return id().getLeastSignificantBits();
        }

        public boolean expired(long now) {
            return expiresMillis > 0L && expiresMillis <= now;
        }

        public boolean live(long now) {
            return active && !expired(now);
        }
    }

    private static final char FIELD = '\t';

    private final Path directory;
    private final Executor writer;
    private final ConcurrentHashMap<UUID, List<Punishment>> byTarget = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<Punishment>> byIpHash = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    public PunishmentService(Path directory, Executor writer) {
        this.directory = directory;
        this.writer = writer;
    }

    public Punishment punish(
            Kind kind,
            UUID targetId,
            String targetName,
            String targetIpHash,
            String reason,
            String staff,
            long durationMillis) {
        long now = System.currentTimeMillis();
        long expires = durationMillis > 0L ? now + durationMillis : 0L;
        Punishment record = new Punishment(
                new UUID(now, sequence.incrementAndGet()),
                kind,
                targetId,
                targetName == null ? "unknown" : targetName,
                targetIpHash,
                sanitize(reason),
                staff,
                now,
                expires,
                true,
                "",
                0L);
        byTarget.computeIfAbsent(targetId, key -> new ArrayList<>()).add(record);
        if (targetIpHash != null && !targetIpHash.isBlank()) {
            byIpHash.computeIfAbsent(targetIpHash, key -> new ArrayList<>()).add(record);
        }
        submit(() -> append(record));
        return record;
    }

    public List<Punishment> remove(UUID targetId, Kind kind, String staff) {
        List<Punishment> stored = byTarget.get(targetId);
        if (stored == null || stored.isEmpty()) {
            return List.of();
        }
        long now = System.currentTimeMillis();
        List<Punishment> removed = new ArrayList<>();
        synchronized (stored) {
            for (Punishment record : new ArrayList<>(stored)) {
                if (record.kind() == kind && record.live(now)) {
                    Punishment revoked = new Punishment(
                            record.id(), record.kind(), record.targetId(), record.targetName(),
                            record.targetIpHash(), record.reason(), record.staff(),
                            record.createdMillis(), record.expiresMillis(), false,
                            staff, now);
                    stored.remove(record);
                    removed.add(revoked);
                    submit(() -> append(revoked));
                }
            }
        }
        return removed;
    }

    public List<Punishment> active(UUID targetId) {
        List<Punishment> stored = byTarget.get(targetId);
        if (stored == null || stored.isEmpty()) {
            return List.of();
        }
        long now = System.currentTimeMillis();
        List<Punishment> live = new ArrayList<>();
        synchronized (stored) {
            for (Punishment record : stored) {
                if (record.live(now)) {
                    live.add(record);
                }
            }
        }
        return live;
    }

    public int clearAll(UUID targetId) {
        List<Punishment> stored = byTarget.get(targetId);
        if (stored == null) {
            return 0;
        }
        long now = System.currentTimeMillis();
        int removed;
        List<Punishment> kept;
        synchronized (stored) {
            removed = (int) stored.stream().filter(record -> record.live(now)).count();
            kept = stored.stream().filter(record -> !record.live(now)).toList();
        }
        if (removed == 0) {
            return 0;
        }
        byTarget.put(targetId, new java.util.ArrayList<>(kept));
        return removed;
    }

    public List<Punishment> history(UUID targetId) {
        List<Punishment> stored = byTarget.get(targetId);
        if (stored == null || stored.isEmpty()) {
            return List.of();
        }
        synchronized (stored) {
            return new ArrayList<>(stored);
        }
    }

    public boolean isBanned(UUID targetId) {
        for (Punishment record : active(targetId)) {
            if (record.kind() == Kind.BAN || record.kind() == Kind.TEMPBAN) {
                return true;
            }
        }
        return false;
    }

    public boolean isMuted(UUID targetId) {
        for (Punishment record : active(targetId)) {
            if (record.kind() == Kind.MUTE || record.kind() == Kind.TEMPMUTE) {
                return true;
            }
        }
        return false;
    }

    public boolean isIpBanned(String ipHash) {
        if (ipHash == null || ipHash.isBlank()) {
            return false;
        }
        List<Punishment> stored = byIpHash.get(ipHash);
        if (stored == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        synchronized (stored) {
            for (Punishment record : stored) {
                if ((record.kind() == Kind.IPBAN || record.kind() == Kind.TEMPIPBAN) && record.live(now)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void load() {
        if (!Files.isDirectory(directory)) {
            return;
        }
        try (var stream = Files.list(directory)) {
            for (Path file : stream.toList()) {
                if (!file.getFileName().toString().endsWith(".punish")) {
                    continue;
                }
                for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    Punishment record = parse(line);
                    if (record == null) {
                        continue;
                    }
                    byTarget.computeIfAbsent(record.targetId(), key -> new ArrayList<>()).add(record);
                    if (record.targetIpHash() != null && !record.targetIpHash().isBlank()) {
                        byIpHash.computeIfAbsent(record.targetIpHash(), key -> new ArrayList<>()).add(record);
                    }
                }
            }
        } catch (Exception failure) {
            return;
        }
    }

    private void submit(Runnable task) {
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

    private void append(Punishment record) {
        try {
            Files.createDirectories(directory);
            String file = record.targetId().toString().replace("-", "") + ".punish";
            Files.writeString(directory.resolve(file), encode(record) + System.lineSeparator(),
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception failure) {
            return;
        }
    }

    private String encode(Punishment record) {
        return String.join(String.valueOf(FIELD),
                record.id().getMostSignificantBits() + ":" + record.id().getLeastSignificantBits(),
                record.kind().name(),
                record.targetId().toString(),
                sanitize(record.targetName()),
                value(record.targetIpHash()),
                sanitize(record.reason()),
                sanitize(record.staff()),
                Long.toString(record.createdMillis()),
                Long.toString(record.expiresMillis()),
                Boolean.toString(record.active()),
                sanitize(record.removedBy()),
                Long.toString(record.removedMillis()));
    }

    private Punishment parse(String line) {
        if (line == null || line.isBlank()) {
            return null;
        }
        String[] parts = line.split(String.valueOf(FIELD), -1);
        if (parts.length < 12) {
            return null;
        }
        try {
            String[] raw = parts[0].split(":", 2);
            UUID id = new UUID(Long.parseLong(raw[0]), Long.parseLong(raw[1]));
            return new Punishment(
                    id,
                    Kind.valueOf(parts[1].toUpperCase(Locale.ROOT)),
                    UUID.fromString(parts[2]),
                    parts[3],
                    parts[4].isBlank() ? null : parts[4],
                    parts[5],
                    parts[6],
                    Long.parseLong(parts[7]),
                    Long.parseLong(parts[8]),
                    Boolean.parseBoolean(parts[9]),
                    parts[10],
                    Long.parseLong(parts[11]));
        } catch (RuntimeException malformed) {
            return null;
        }
    }

    private static String sanitize(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\t', ' ')
                .replace('\n', ' ')
                .replace('\r', ' ')
                .trim();
    }

    private static String value(String value) {
        return value == null ? "" : sanitize(value);
    }

    @Override
    public void close() {
        byTarget.clear();
        byIpHash.clear();
    }

    public static String hashIp(String address) {
        if (address == null || address.isBlank()) {
            return "";
        }
        return Integer.toHexString(address.hashCode());
    }
}
