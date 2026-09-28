package dev.snuffac.core.violation;

import dev.snuffac.core.config.CheckConfig;
import java.util.LinkedHashMap;
import java.util.Map;

public final class CheckState {

    private final String checkKey;
    private final CheckConfig config;

    private final ViolationBuffer buffer;
    private final ViolationLevel level;
    private final Map<String, Long> lastCommandExecution = new LinkedHashMap<>();

    private long violations;
    private long lastFlagMillis;
    private ViolationRecord lastViolation;

    public CheckState(String checkKey, CheckConfig config) {
        this.checkKey = checkKey;
        this.config = config;
        this.buffer = new ViolationBuffer(
                config.effectiveBufferThreshold(),
                config.bufferDecay(),
                config.bufferMaximum());
        this.level = new ViolationLevel(Double.MAX_VALUE, 0.0);
    }

    public String checkKey() {
        return checkKey;
    }

    public CheckConfig config() {
        return config;
    }

    public ViolationBuffer buffer() {
        return buffer;
    }

    public ViolationLevel level() {
        return level;
    }

    public long violations() {
        return violations;
    }

    public long totalViolations() {
        return violations;
    }

    public long lastFlagMillis() {
        return lastFlagMillis;
    }

    public ViolationRecord lastViolation() {
        return lastViolation;
    }

    public double pendingBufferValue;

    public void recordFlag(ViolationRecord record) {
        this.lastViolation = record;
        this.lastFlagMillis = record.timestampMillis();
        this.violations++;
        this.buffer.drain();
        this.level.increase(config.violationIncrement());
    }

    public void tick() {
        buffer.tick();
        level.tick();
    }

    public void reset() {
        buffer.reset();
        level.reset();
        lastViolation = null;
    }

    public boolean commandReady(long now, String identifier) {
        if (!config.commandsEnabled()) {
            return false;
        }
        if (level.value() < config.commandThreshold()) {
            return false;
        }
        Long last = lastCommandExecution.get(identifier);
        if (last != null && now - last < config.commandCooldownMillis()) {
            return false;
        }
        lastCommandExecution.put(identifier, now);
        return true;
    }

    public double setbackProgress() {
        double threshold = config.setbackThreshold();
        if (threshold <= 0.0) {
            return 0.0;
        }
        return Math.min(level.value() / threshold, 1.0);
    }
}
