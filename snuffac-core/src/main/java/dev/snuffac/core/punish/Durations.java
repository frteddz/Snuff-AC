package dev.snuffac.core.punish;

import java.util.Locale;

public final class Durations {

    private static final long MINUTE_MILLIS = 60_000L;
    private static final long HOUR_MILLIS = 3_600_000L;
    private static final long DAY_MILLIS = 86_400_000L;
    private static final long WEEK_MILLIS = 604_800_000L;
    private static final long YEAR_MILLIS = 365L * DAY_MILLIS;
    private static final long MAX_MILLIS = 10L * 365L * DAY_MILLIS;

    private Durations() {
    }

    public static long parseMillis(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("duration is empty");
        }
        String text = input.trim().toLowerCase(Locale.ROOT);
        long total = 0L;
        StringBuilder digits = new StringBuilder();
        int matched = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= '0' && c <= '9') {
                digits.append(c);
                continue;
            }
            if (digits.length() == 0) {
                if (c == ' ') {
                    continue;
                }
                throw new IllegalArgumentException("duration starts with a unit: " + input);
            }
            long value;
            try {
                value = Long.parseLong(digits.toString());
            } catch (NumberFormatException overflow) {
                throw new IllegalArgumentException("duration value is too large: " + input);
            }
            digits.setLength(0);
            int unitLength = unitLength(text, i);
            String unit = text.substring(i, i + unitLength);
            total += Math.multiplyExact(value, unitMillis(unit, input));
            i += unitLength - 1;
            matched++;
        }
        if (digits.length() > 0) {
            throw new IllegalArgumentException("duration has no unit: " + input);
        }
        if (matched == 0) {
            throw new IllegalArgumentException("no duration found: " + input);
        }
        if (total <= 0L) {
            throw new IllegalArgumentException("duration must be positive: " + input);
        }
        if (total > MAX_MILLIS) {
            throw new IllegalArgumentException("duration is longer than ten years: " + input);
        }
        return total;
    }

    private static int unitLength(String text, int index) {
        String[] two = {"mo"};
        String[] one = {"s", "m", "h", "d", "w", "y"};
        for (String candidate : two) {
            if (text.startsWith(candidate, index)) {
                return candidate.length();
            }
        }
        for (String candidate : one) {
            if (text.startsWith(candidate, index)) {
                return candidate.length();
            }
        }
        throw new IllegalArgumentException("unknown duration unit at " + index + " in " + text);
    }

    private static long unitMillis(String unit, String original) {
        return switch (unit) {
            case "s" -> 1_000L;
            case "m" -> MINUTE_MILLIS;
            case "h" -> HOUR_MILLIS;
            case "d" -> DAY_MILLIS;
            case "w" -> WEEK_MILLIS;
            case "mo" -> 30L * DAY_MILLIS;
            case "y" -> 365L * DAY_MILLIS;
            default -> throw new IllegalArgumentException("unknown duration unit: " + unit);
        };
    }

    public static String describe(long millis) {
        if (millis <= 0L) {
            return "permanent";
        }
        long remaining = millis;
        StringBuilder builder = new StringBuilder();
        append(builder, remaining / YEAR_MILLIS, "y");
        remaining %= YEAR_MILLIS;
        append(builder, remaining / DAY_MILLIS, "d");
        remaining %= DAY_MILLIS;
        append(builder, remaining / HOUR_MILLIS, "h");
        remaining %= HOUR_MILLIS;
        append(builder, remaining / MINUTE_MILLIS, "m");
        remaining %= MINUTE_MILLIS;
        if (remaining > 0L) {
            builder.append(remaining / 1000L).append('s');
        }
        return builder.isEmpty() ? "permanent" : builder.toString();
    }

    private static void append(StringBuilder builder, long value, String unit) {
        if (value > 0L) {
            builder.append(value).append(unit);
        }
    }
}
