package dev.snuffac.core.report;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class ReportStore {

    public static final int MAX_NOTE_LENGTH = 280;
    private static final long RETENTION_MILLIS = 30L * 24L * 60L * 60L * 1000L;
    private static final int RATE_LIMIT = 5;
    private static final long RATE_WINDOW_MILLIS = 600_000L;

    private final Map<UUID, ReportCategory.Entry> byId = new ConcurrentHashMap<>();
    private final Map<UUID, List<Long>> reporterTimestamps = new ConcurrentHashMap<>();
    private final AtomicInteger rateCounter = new AtomicInteger();
    private final AtomicInteger sequence = new AtomicInteger();
    private final Path file;
    private volatile java.util.function.Predicate<String> allowed = ReportCategory::isValid;
    private volatile long retentionDays = 30L;
    private volatile boolean loaded;

    public ReportStore(Path file) {
        this.file = file;
    }

    public void allowedCategories(java.util.function.Predicate<String> allowed) {
        this.allowed = allowed == null ? ReportCategory::isValid : allowed;
    }

    public void retentionDays(long days) {
        this.retentionDays = Math.max(0L, days);
    }

    public long retentionDays() {
        return retentionDays;
    }

    public synchronized void load() {
        loaded = true;
        if (file == null || !Files.isRegularFile(file)) {
            return;
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException failure) {
            return;
        }
        for (String line : lines) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            int split = line.indexOf('\t');
            if (split <= 0) {
                continue;
            }
            Map<String, Object> map = decode(line.substring(split + 1));
            ReportCategory.Entry entry = ReportCategory.Entry.fromMap(map);
            if (entry != null) {
                byId.put(entry.id(), entry);
            }
        }
        prune(System.currentTimeMillis());
    }

    public boolean loaded() {
        return loaded;
    }

    public boolean rateLimited(UUID reporter) {
        long now = System.currentTimeMillis();
        List<Long> stamps = reporterTimestamps.computeIfAbsent(reporter, ignored -> new ArrayList<>());
        stamps.removeIf(stamp -> now - stamp > RATE_WINDOW_MILLIS);
        return stamps.size() >= RATE_LIMIT;
    }

    public ReportCategory.Entry file(
            UUID reporter,
            String reporterName,
            UUID target,
            String targetName,
            String category,
            String note) {

        if (category == null || category.isBlank() || !allowed.test(category.trim().toLowerCase(Locale.ROOT))) {
            return null;
        }
        String safeNote = ReportCategory.sanitiseNote(note, MAX_NOTE_LENGTH);
        long now = System.currentTimeMillis();
        ReportCategory.Entry entry = new ReportCategory.Entry(
                UUID.randomUUID(),
                reporter,
                reporterName == null ? "unknown" : reporterName,
                target,
                targetName == null ? "unknown" : targetName,
                category.trim().toLowerCase(Locale.ROOT),
                safeNote,
                now,
                sequence.incrementAndGet());
        byId.put(entry.id(), entry);
        reporterTimestamps
                .computeIfAbsent(reporter, ignored -> new ArrayList<>())
                .add(now);
        rateCounter.incrementAndGet();
        return entry;
    }

    public ReportCategory.Entry byId(UUID id) {
        return id == null ? null : byId.get(id);
    }

    public List<ReportCategory.Entry> all() {
        List<ReportCategory.Entry> result = new ArrayList<>(byId.values());
        ReportCategory.sortNewestFirst(result);
        return result;
    }

    public List<ReportCategory.Entry> byTarget(UUID target) {
        List<ReportCategory.Entry> result = new ArrayList<>();
        if (target == null) {
            return result;
        }
        for (ReportCategory.Entry entry : byId.values()) {
            if (target.equals(entry.target())) {
                result.add(entry);
            }
        }
        ReportCategory.sortNewestFirst(result);
        return result;
    }

    public int openCount() {
        int count = 0;
        for (ReportCategory.Entry entry : byId.values()) {
            if (entry.open()) {
                count++;
            }
        }
        return count;
    }

    public int filedCount() {
        return byId.size();
    }

    public int prune(long now) {
        long cutoff = now - retentionDays * 24L * 60L * 60L * 1000L;
        List<UUID> expired = new ArrayList<>();
        for (Map.Entry<UUID, ReportCategory.Entry> entry : byId.entrySet()) {
            if (entry.getValue().createdMillis() < cutoff) {
                expired.add(entry.getKey());
            }
        }
        for (UUID id : expired) {
            byId.remove(id);
        }
        if (!expired.isEmpty()) {
            save();
        }
        return expired.size();
    }

    public synchronized void save() {
        if (file == null) {
            return;
        }
        StringBuilder builder = new StringBuilder();
        builder.append("# Snuff AC player reports\n");
        builder.append("# id\tdata\n");
        for (ReportCategory.Entry entry : all()) {
            builder.append(entry.id()).append('\t').append(encode(entry.toMap())).append('\n');
        }
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(file, builder.toString(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
        }
    }

    static final String FIELD = "\u001f";
    static final String ENTRY = "\u001e";

    static String encode(Map<String, Object> map) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                builder.append(FIELD);
            }
            first = false;
            builder.append(entry.getKey()).append(ENTRY).append(entry.getValue());
        }
        return builder.toString();
    }

    static Map<String, Object> decode(String line) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        for (String pair : line.split(FIELD)) {
            int split = pair.indexOf(ENTRY);
            if (split <= 0) {
                continue;
            }
            map.put(pair.substring(0, split), pair.substring(split + ENTRY.length()));
        }
        return map;
    }

}
