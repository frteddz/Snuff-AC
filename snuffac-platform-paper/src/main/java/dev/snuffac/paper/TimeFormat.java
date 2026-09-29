package dev.snuffac.paper;

import java.text.SimpleDateFormat;
import java.util.Date;

public final class TimeFormat {

    private static final String PATTERN = "yyyy-MM-dd HH:mm:ss";

    private TimeFormat() {
    }

    public static String absolute(long epochMillis) {
        return new SimpleDateFormat(PATTERN).format(new Date(epochMillis));
    }

    public static String relative(long epochMillis) {
        long seconds = Math.max(0L, (System.currentTimeMillis() - epochMillis) / 1000L);
        if (seconds < 60L) {
            return seconds + "s ago";
        }
        long minutes = seconds / 60L;
        if (minutes < 60L) {
            return minutes + "m ago";
        }
        long hours = minutes / 60L;
        if (hours < 24L) {
            return hours + "h ago";
        }
        long days = hours / 24L;
        if (days < 30L) {
            return days + "d ago";
        }
        long months = days / 30L;
        if (months < 12L) {
            return months + "mo ago";
        }
        return (days / 365L) + "y ago";
    }

    public static String both(long epochMillis) {
        return absolute(epochMillis) + " (" + relative(epochMillis) + ")";
    }
}
