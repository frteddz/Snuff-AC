package dev.snuffac.core.alert;

import dev.snuffac.api.violation.ViolationInfo;
import java.util.LinkedHashMap;
import java.util.Map;

public final class AlertFormatter {

    private final String prefix;
    private final String baseFormat;
    private final String verboseFormat;

    public AlertFormatter(String prefix, String baseFormat, String verboseFormat) {
        this.prefix = prefix;
        this.baseFormat = baseFormat;
        this.verboseFormat = verboseFormat;
    }

    public String format(ViolationInfo info, Map<String, Object> evidence) {
        String template = verboseFormat == null || verboseFormat.isBlank() ? baseFormat : verboseFormat;
        Map<String, Object> placeholders = new LinkedHashMap<>();
        placeholders.put("prefix", prefix);
        placeholders.put("player", info.playerName());
        placeholders.put("check", info.checkName());
        placeholders.put("checkKey", info.checkKey());
        placeholders.put("category", info.category().name());
        placeholders.put("vl", round(info.violationLevel(), 2));
        placeholders.put("buffer", round(info.buffer(), 2));
        placeholders.put("confidence", round(info.confidence(), 3));
        placeholders.put("ping", round(info.pingMillis(), 1));
        placeholders.put("tps", round(info.tps(), 2));
        placeholders.put("detail", info.detail());
        placeholders.put("platform", info.platform().name());
        if (evidence != null) {
            for (Map.Entry<String, Object> entry : evidence.entrySet()) {
                placeholders.put(entry.getKey(), formatValue(entry.getValue()));
            }
        }
        return apply(template, placeholders);
    }

    private static String apply(String template, Map<String, Object> placeholders) {
        String result = template;
        for (Map.Entry<String, Object> entry : placeholders.entrySet()) {
            String key = entry.getKey();
            if (key.isEmpty()) {
                continue;
            }
            result = replaceAll(result, "{" + key + "}", String.valueOf(entry.getValue()));
        }
        return result;
    }

    private static String replaceAll(String input, String search, String replacement) {
        int index = input.indexOf(search);
        if (index < 0) {
            return input;
        }
        StringBuilder builder = new StringBuilder(input.length());
        int start = 0;
        while (index >= 0) {
            builder.append(input, start, index).append(replacement);
            start = index + search.length();
            index = input.indexOf(search, start);
        }
        builder.append(input, start, input.length());
        return builder.toString();
    }

    private static String formatValue(Object value) {
        if (value instanceof Double number) {
            return round(number, 4);
        }
        if (value instanceof Float number) {
            return round(number.doubleValue(), 4);
        }
        return String.valueOf(value);
    }

    private static String round(double value, int decimals) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "n/a";
        }
        double factor = Math.pow(10.0, decimals);
        return String.valueOf(Math.round(value * factor) / factor);
    }
}
