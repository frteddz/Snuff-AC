package dev.snuffac.core.violation;

import dev.snuffac.api.CheckCategory;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ViolationRecord {

    private final String checkKey;
    private final CheckCategory category;
    private final String detail;
    private final double buffer;
    private final double violationLevel;
    private final long timestampMillis;
    private final Map<String, Object> evidence;

    public ViolationRecord(
            String checkKey,
            CheckCategory category,
            String detail,
            double violationLevel,
            double buffer,
            long timestampMillis,
            Map<String, Object> evidence) {
        this.checkKey = checkKey;
        this.category = category;
        this.detail = detail;
        this.buffer = buffer;
        this.violationLevel = violationLevel;
        this.timestampMillis = timestampMillis;
        this.evidence = evidence == null ? new LinkedHashMap<>() : new LinkedHashMap<>(evidence);
    }

    public String checkKey() {
        return checkKey;
    }

    public CheckCategory category() {
        return category;
    }

    public String detail() {
        return detail;
    }

    public double buffer() {
        return buffer;
    }

    public double violationLevel() {
        return violationLevel;
    }

    public long timestampMillis() {
        return timestampMillis;
    }

    public Map<String, Object> evidence() {
        return evidence;
    }

    @Override
    public String toString() {
        return checkKey + " VL=" + violationLevel + " " + detail + " " + evidence;
    }
}
