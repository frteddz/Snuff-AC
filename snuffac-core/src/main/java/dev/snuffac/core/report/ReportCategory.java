package dev.snuffac.core.report;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class ReportCategory {

    public static final String CHEATING = "cheating";
    public static final String EXPLOITING = "exploiting";
    public static final String LANGUAGE = "language";
    public static final String OFFENSIVE = "offensive";
    public static final String GRIEFING = "griefing";
    public static final String NAME = "name";
    public static final String IMPERSONATION = "impersonation";

    public static final List<String> ALL = List.of(
            CHEATING, EXPLOITING, LANGUAGE, OFFENSIVE, GRIEFING, NAME, IMPERSONATION);

    public record Definition(String id, String label, String description) {
    }

    private static final Map<String, Definition> DEFINITIONS = new LinkedHashMap<>();

    static {
        add(CHEATING, "Cheating", "Use of a client that gives an unfair advantage");
        add(EXPLOITING, "Exploiting", "Duplication, crashes, or abusing a flaw");
        add(LANGUAGE, "Explicit Language", "Sexual or graphic language");
        add(OFFENSIVE, "Offensive Behaviour", "Harassment, toxicity, or slurs");
        add(GRIEFING, "Griefing", "Destroying or stealing other people's work");
        add(NAME, "Inappropriate Name", "A username that breaks the rules");
        add(IMPERSONATION, "Staff Impersonation", "Pretending to be an administrator");
    }

    private ReportCategory() {
    }

    private static void add(String id, String label, String description) {
        DEFINITIONS.put(id, new Definition(id, label, description));
    }

    public static Definition get(String id) {
        if (id == null) {
            return null;
        }
        return DEFINITIONS.get(id.trim().toLowerCase(Locale.ROOT));
    }

    public static boolean isValid(String id) {
        return get(id) != null;
    }

    public static List<Definition> all() {
        return List.copyOf(DEFINITIONS.values());
    }

    public static String sanitiseNote(String note, int maxLength) {
        if (note == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder(Math.min(note.length(), maxLength));
        for (int i = 0; i < note.length() && builder.length() < maxLength; i++) {
            char c = note.charAt(i);
            if (c == '&' || c == '<' || c == '>' || c == '§' || c == '\n' || c == '\r') {
                continue;
            }
            builder.append(c);
        }
        return builder.toString().trim();
    }

    public static final class Entry {

        private final UUID id;
        private final UUID reporter;
        private final String reporterName;
        private final UUID target;
        private final String targetName;
        private final String category;
        private final String note;
        private final long createdMillis;
        private final long sequence;
        private String status;
        private UUID handledBy;
        private String handledByName;
        private long handledMillis;
        private String resolution;

        public Entry(UUID id, UUID reporter, String reporterName, UUID target, String targetName,
                String category, String note, long createdMillis) {
            this(id, reporter, reporterName, target, targetName, category, note, createdMillis, 0L);
        }

        public Entry(UUID id, UUID reporter, String reporterName, UUID target, String targetName,
                String category, String note, long createdMillis, long sequence) {
            this.sequence = sequence;
            this.id = id;
            this.reporter = reporter;
            this.reporterName = reporterName;
            this.target = target;
            this.targetName = targetName;
            this.category = category;
            this.note = note;
            this.createdMillis = createdMillis;
            this.status = "OPEN";
        }

        public UUID id() {
            return id;
        }

        public UUID reporter() {
            return reporter;
        }

        public String reporterName() {
            return reporterName;
        }

        public UUID target() {
            return target;
        }

        public String targetName() {
            return targetName;
        }

        public String category() {
            return category;
        }

        public String note() {
            return note;
        }

        public long createdMillis() {
            return createdMillis;
        }

        public long sequence() {
            return sequence;
        }

        public int compareNewestFirst(Entry other) {
            int byTime = Long.compare(other.createdMillis, this.createdMillis);
            if (byTime != 0) {
                return byTime;
            }
            return Long.compare(other.sequence, this.sequence);
        }

        public String status() {
            return status;
        }

        public UUID handledBy() {
            return handledBy;
        }

        public String handledByName() {
            return handledByName;
        }

        public long handledMillis() {
            return handledMillis;
        }

        public String resolution() {
            return resolution;
        }

        public void claim(UUID staff, String staffName) {
            this.status = "CLAIMED";
            this.handledBy = staff;
            this.handledByName = staffName;
            this.handledMillis = System.currentTimeMillis();
        }

        public void release() {
            this.status = "OPEN";
            this.handledBy = null;
            this.handledByName = null;
            this.handledMillis = 0L;
        }

        public void resolve(String outcome) {
            this.status = "RESOLVED";
            this.resolution = outcome == null ? "" : outcome;
            this.handledMillis = System.currentTimeMillis();
        }

        public boolean open() {
            return "OPEN".equals(status);
        }

        public boolean claimed() {
            return "CLAIMED".equals(status);
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", id.toString());
            map.put("reporter", reporter.toString());
            map.put("reporterName", reporterName);
            map.put("target", target.toString());
            map.put("targetName", targetName);
            map.put("category", category);
            map.put("note", note);
            map.put("createdMillis", createdMillis);
            map.put("sequence", sequence);
            map.put("status", status);
            map.put("handledBy", handledBy == null ? "" : handledBy.toString());
            map.put("handledByName", handledByName == null ? "" : handledByName);
            map.put("handledMillis", handledMillis);
            map.put("resolution", resolution == null ? "" : resolution);
            return map;
        }

        public static Entry fromMap(Map<String, Object> map) {
            UUID id = parseUuid(map.get("id"));
            UUID reporter = parseUuid(map.get("reporter"));
            UUID target = parseUuid(map.get("target"));
            if (id == null || reporter == null || target == null) {
                return null;
            }
            Entry entry = new Entry(
                    id,
                    reporter,
                    string(map.get("reporterName")),
                    target,
                    string(map.get("targetName")),
                    string(map.get("category")),
                    string(map.get("note")),
                    number(map.get("createdMillis")),
                    number(map.get("sequence")));
            entry.status = string(map.get("status"));
            if (entry.status == null || entry.status.isBlank()) {
                entry.status = "OPEN";
            }
            entry.handledBy = parseUuid(map.get("handledBy"));
            entry.handledByName = string(map.get("handledByName"));
            entry.handledMillis = number(map.get("handledMillis"));
            entry.resolution = string(map.get("resolution"));
            return entry;
        }

        private static UUID parseUuid(Object value) {
            if (value == null) {
                return null;
            }
            try {
                return UUID.fromString(String.valueOf(value));
            } catch (IllegalArgumentException malformed) {
                return null;
            }
        }

        private static String string(Object value) {
            return value == null ? "" : String.valueOf(value);
        }

        private static long number(Object value) {
            if (value instanceof Number n) {
                return n.longValue();
            }
            try {
                return Long.parseLong(String.valueOf(value));
            } catch (NumberFormatException malformed) {
                return 0L;
            }
        }
    }

    public static void sortNewestFirst(List<Entry> entries) {
        entries.sort(Entry::compareNewestFirst);
    }

    public static List<Entry> filter(List<Entry> all, String status, String category) {
        List<Entry> result = new ArrayList<>();
        for (Entry entry : all) {
            if (status != null && !status.isBlank() && !status.equalsIgnoreCase(entry.status())) {
                continue;
            }
            if (category != null && !category.isBlank()
                    && !category.equalsIgnoreCase(entry.category())) {
                continue;
            }
            result.add(entry);
        }
        return result;
    }
}
