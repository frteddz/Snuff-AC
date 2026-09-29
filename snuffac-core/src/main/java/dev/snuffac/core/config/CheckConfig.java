package dev.snuffac.core.config;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.core.util.MathUtil;

public final class CheckConfig {

    public static final String ACTION_NONE = "NONE";
    public static final String ACTION_ALERT = "ALERT";
    public static final String ACTION_LOG = "LOG";
    public static final String ACTION_COMMAND = "COMMAND";
    public static final String ACTION_SETBACK = "SETBACK";
    public static final String ACTION_KICK = "KICK";

    public static final String EVIDENCE_STRUCTURAL = "STRUCTURAL";
    public static final String EVIDENCE_DERIVED = "DERIVED";

    private final CheckCategory category;
    private boolean enabled;
    private double bufferThreshold;
    private double bufferDecay;
    private double bufferMaximum;
    private double violationIncrement;
    private double setbackThreshold;
    private double alertThreshold;
    private String action;
    private String command;
    private double commandThreshold;
    private int commandCooldownMillis;
    private String description;
    private String evidenceKind;

    public CheckConfig(
            CheckCategory category,
            boolean enabled,
            double bufferThreshold,
            double bufferDecay,
            double bufferMaximum,
            double violationIncrement,
            double setbackThreshold,
            double alertThreshold,
            String action,
            String command,
            double commandThreshold,
            int commandCooldownMillis,
            String description,
            String evidenceKind) {
        this.category = category;
        this.enabled = enabled;
        this.bufferThreshold = bufferThreshold;
        this.bufferDecay = bufferDecay;
        this.bufferMaximum = bufferMaximum;
        this.violationIncrement = violationIncrement;
        this.setbackThreshold = setbackThreshold;
        this.alertThreshold = alertThreshold;
        this.action = action;
        this.command = command;
        this.commandThreshold = commandThreshold;
        this.commandCooldownMillis = commandCooldownMillis;
        this.description = description;
        this.evidenceKind = evidenceKind;
    }

    public static CheckConfig defaults(CheckCategory category) {
        return new CheckConfig(
                category, true, 1.0, 0.1, 100.0, 1.0, 1.0, 1.0,
                ACTION_ALERT, "", 12.0, 1000, "", EVIDENCE_DERIVED);
    }

    public CheckCategory category() {
        return category;
    }

    public boolean enabled() {
        return enabled;
    }

    public void enabled(boolean value) {
        this.enabled = value;
    }

    public double bufferThreshold() {
        return bufferThreshold;
    }

    public double bufferDecay() {
        return bufferDecay;
    }

    public double bufferMaximum() {
        return bufferMaximum;
    }

    public double violationIncrement() {
        return violationIncrement;
    }

    public double setbackThreshold() {
        return setbackThreshold;
    }

    public boolean setbacksEnabled() {
        return setbackThreshold > 0.0;
    }

    public double alertThreshold() {
        return alertThreshold;
    }

    public String action() {
        return action;
    }

    public String command() {
        return command;
    }

    public double commandThreshold() {
        return commandThreshold;
    }

    public int commandCooldownMillis() {
        return commandCooldownMillis;
    }

    public String description() {
        return description;
    }

    public String evidenceKind() {
        return evidenceKind;
    }

    public boolean structural() {
        return EVIDENCE_STRUCTURAL.equalsIgnoreCase(evidenceKind);
    }

    public void evidenceKind(String value) {
        this.evidenceKind = value;
    }

    public boolean alertsEnabled() {
        return ACTION_ALERT.equalsIgnoreCase(action) || ACTION_NONE.equalsIgnoreCase(action);
    }

    public boolean logsEnabled() {
        return ACTION_LOG.equalsIgnoreCase(action)
                || ACTION_COMMAND.equalsIgnoreCase(action)
                || ACTION_SETBACK.equalsIgnoreCase(action)
                || ACTION_KICK.equalsIgnoreCase(action);
    }

    public boolean commandsEnabled() {
        return ACTION_COMMAND.equalsIgnoreCase(action) && command != null && !command.isBlank();
    }

    public double effectiveBufferThreshold() {
        return Math.max(bufferThreshold, 0.01);
    }

    public double effectiveSetbackThreshold() {
        double threshold = Math.max(setbackThreshold, 0.0);
        if (!structural()) {
            return Math.max(threshold, alertThreshold + 1.0);
        }
        return threshold;
    }

    public void applyFrom(ConfigSource source, String basePath) {
        String prefix = basePath == null || basePath.isEmpty() ? "" : basePath + ".";
        enabled = source.getBoolean(prefix + "enabled", enabled);
        bufferThreshold = Math.max(source.getDouble(prefix + "buffer-threshold", bufferThreshold), 0.01);
        bufferDecay = Math.max(source.getDouble(prefix + "buffer-decay", bufferDecay), 0.0);
        bufferMaximum = Math.max(source.getDouble(prefix + "buffer-maximum", bufferMaximum), bufferThreshold);
        violationIncrement = Math.max(source.getDouble(prefix + "violation-increment", violationIncrement), 0.0);
        setbackThreshold = Math.max(source.getDouble(prefix + "setback-threshold", setbackThreshold), 0.0);
        alertThreshold = Math.max(source.getDouble(prefix + "alert-threshold", alertThreshold), 0.0);
        action = source.getString(prefix + "action", action).toUpperCase(java.util.Locale.ROOT);
        command = source.getString(prefix + "command", command);
        commandThreshold = source.getDouble(prefix + "command-threshold", commandThreshold);
        commandCooldownMillis = source.getInt(prefix + "command-cooldown-ms", commandCooldownMillis);
        description = source.getString(prefix + "description", description);
        evidenceKind = source.getString(prefix + "evidence", evidenceKind).toUpperCase(java.util.Locale.ROOT);
    }

    public void writeTo(ConfigSource source, String basePath) {
        String prefix = basePath == null || basePath.isEmpty() ? "" : basePath + ".";
        source.set(prefix + "enabled", enabled);
        source.set(prefix + "buffer-threshold", MathUtil.roundTo(bufferThreshold, 4));
        source.set(prefix + "buffer-decay", MathUtil.roundTo(bufferDecay, 4));
        source.set(prefix + "violation-increment", MathUtil.roundTo(violationIncrement, 4));
        source.set(prefix + "setback-threshold", MathUtil.roundTo(setbackThreshold, 4));
        source.set(prefix + "alert-threshold", MathUtil.roundTo(alertThreshold, 4));
        source.set(prefix + "action", action);
        source.set(prefix + "command", command);
        source.set(prefix + "command-threshold", MathUtil.roundTo(commandThreshold, 4));
        source.set(prefix + "evidence", evidenceKind);
    }
}
