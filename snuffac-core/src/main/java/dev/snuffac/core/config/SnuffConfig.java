package dev.snuffac.core.config;

import dev.snuffac.core.util.MathUtil;

public final class SnuffConfig {

    private boolean enabled = true;
    private boolean debug;
    private String alertPrefix = "Snuff";
    private boolean alertsEnabled = true;
    private boolean consoleAlerts = true;
    private String consoleAlertFormat = "[{prefix}] {player} failed {check} | VL: {vl} | ping: {ping}ms";
    private String consoleAlertFormatVerbose = "";
    private int alertCooldownMillis = 1500;
    private boolean logToFile = true;
    private String logDirectory = "plugins/SnuffAC/logs";
    private int historyRetentionDays = 90;
    private int logRetentionDays = 30;
    private int historyPerPlayer = 200;
    private boolean alertOnRejoinWithHistory = true;
    private String debugPermission = "snuffac.debug";
    private String bypassPermission = "snuffac.bypass";
    private String adminPermission = "snuffac.admin";
    private String alertPermission = "snuffac.alerts";

    private double setbackHorizontal = 0.0;
    private double setbackVertical = 0.0;
    private int maxViolationsPerSecond = 40;
    private boolean preventionEnabled = true;
    private double minConfidenceForPrevention = 0.55;
    private int antiXrayBandStart = -16;
    private int antiXrayBandEnd = 320;
    private boolean escalationEnabled = false;
    private int escalationMaxWarnings = 5;
    private long escalationBanMillis = 3_600_000L;
    private double escalationMinConfidence = 0.75;
    private boolean escalationWarnOnly = false;
    private java.util.List<String> mutedAllowedCommands = new java.util.ArrayList<>();
    private boolean staffSounds = true;
    private boolean antiXrayHideContainers;
    private boolean visualEntityHiding = true;
    private boolean visualSoundFuzzing = true;
    private double visualRevealRadius = 16.0;
    private double visualRevealPadding = 24.0;
    private double visualSoundJitter = 1.5;
    private int visualIntervalTicks = 4;
    private dev.snuffac.core.world.ObfuscationPolicy antiXrayMode =
            dev.snuffac.core.world.ObfuscationPolicy.HIDDEN_ORES;
    private boolean exemptBedrock = true;
    private boolean exemptLegacyProtocol = true;
    private boolean allowUnknownProtocols = true;
    private boolean exemptVoidWorlds = true;
    private int voidWorldFloor = -60;

    private double toleranceDecayPerTick = 0.35;
    private double toleranceMaximum = 0.45;
    private double toleranceCarryOverCap = 1.0;
    private double toleranceCarryOverRetention = 0.4;
    private double baseTolerance = 0.001;
    private String tuningProfile = "strict";
    private String chatPrefix = "";
    private int reportRetentionDays = 30;
    private String bypassFile = "bypass.tsv";
    private double pingToleranceFloor = 0.001;
    private double pingTolerancePerMilli = 0.00002;
    private double maxPingTolerance = 0.06;
    private double tpsToleranceFloor = 0.001;
    private double tpsTolerancePerMiss = 0.004;
    private double maxTpsTolerance = 0.05;
    private double safeTps = 19.0;
    private double lowTpsSafety = 17.0;

    private double reachMaximum = 3.35;
    private double reachTolerance = 0.12;
    private double reachSparrowTolerance = 0.45;
    private double reachLatencyMultiplier = 0.0022;
    private double reachMaxLatencyBonus = 0.35;
    private double clickerMaxCps = 22.0;
    private int clickerWindowSize = 40;
    private double badPacketMaxPerSecond = 60.0;
    private double timerMinimumTps = 20.0;
    private double timerMaxDeviation = 1.2;
    private double timerExemptionPing = 400.0;
    private int maxBlocksPerSecondDigging = 22;

    public void load(ConfigSource source) {
        enabled = source.getBoolean("general.enabled", enabled);
        debug = source.getBoolean("general.debug", debug);
        alertPrefix = source.getString("general.alert-prefix", alertPrefix);
        alertsEnabled = source.getBoolean("general.alerts", alertsEnabled);
        consoleAlerts = source.getBoolean("general.console-alerts", consoleAlerts);
        consoleAlertFormat = source.getString("general.console-alert-format", consoleAlertFormat);
        consoleAlertFormatVerbose = source.getString("general.console-alert-format-verbose", consoleAlertFormatVerbose);
        alertCooldownMillis = source.getInt("general.alert-cooldown-ms", alertCooldownMillis);
        logToFile = source.getBoolean("general.log-to-file", logToFile);
        logDirectory = source.getString("general.log-directory", logDirectory);
        historyRetentionDays = source.getInt("general.history-retention-days", historyRetentionDays);
        logRetentionDays = source.getInt("general.log-retention-days", logRetentionDays);
        historyPerPlayer = source.getInt("general.history-per-player", historyPerPlayer);
        alertOnRejoinWithHistory = source.getBoolean("general.alert-on-rejoin-with-history", alertOnRejoinWithHistory);
        debugPermission = source.getString("general.permissions.debug", debugPermission);
        bypassPermission = source.getString("general.permissions.bypass", bypassPermission);
        adminPermission = source.getString("general.permissions.admin", adminPermission);
        alertPermission = source.getString("general.permissions.alerts", alertPermission);

        setbackHorizontal = source.getDouble("setback.horizontal", setbackHorizontal);
        setbackVertical = source.getDouble("setback.vertical", setbackVertical);
        maxViolationsPerSecond = source.getInt("setback.max-violations-per-second", maxViolationsPerSecond);
        preventionEnabled = source.getBoolean("prevention.enabled", preventionEnabled);
        minConfidenceForPrevention = source.getDouble("prevention.min-confidence", minConfidenceForPrevention);
        antiXrayBandStart = source.getInt("anti-xray.band-start", antiXrayBandStart);
        antiXrayBandEnd = source.getInt("anti-xray.band-end", antiXrayBandEnd);
        antiXrayHideContainers = source.getBoolean("anti-xray.hide-containers", antiXrayHideContainers);
        escalationEnabled = source.getBoolean("escalation.enabled", escalationEnabled);
        escalationMaxWarnings = source.getInt("escalation.max-warnings", escalationMaxWarnings);
        escalationBanMillis = source.getLong("escalation.ban-duration-millis", escalationBanMillis);
        escalationMinConfidence = source.getDouble("escalation.min-confidence", escalationMinConfidence);
        escalationWarnOnly = source.getBoolean("escalation.warn-only", escalationWarnOnly);
        mutedAllowedCommands.clear();
        for (String entry : source.getStringList("mute.allowed-commands", new java.util.ArrayList<>())) {
            String cleaned = entry.trim().toLowerCase(java.util.Locale.ROOT);
            if (cleaned.startsWith("/")) {
                cleaned = cleaned.substring(1);
            }
            if (!cleaned.isBlank() && !mutedAllowedCommands.contains(cleaned)) {
                mutedAllowedCommands.add(cleaned);
            }
        }
        staffSounds = source.getBoolean("sounds.enabled", staffSounds);
        String mode = source.getString("anti-xray.mode", antiXrayMode.name());
        if ("NONE".equalsIgnoreCase(mode.trim())) {
            mode = "OFF";
        }
        try {
            antiXrayMode = dev.snuffac.core.world.ObfuscationPolicy.valueOf(mode.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException invalid) {
            antiXrayMode = dev.snuffac.core.world.ObfuscationPolicy.OFF;
        }
        exemptBedrock = source.getBoolean("compatibility.exempt-bedrock", exemptBedrock);
        exemptLegacyProtocol = source.getBoolean("compatibility.exempt-legacy-protocol", exemptLegacyProtocol);
        allowUnknownProtocols = source.getBoolean("compatibility.allow-unknown-protocols", allowUnknownProtocols);
        exemptVoidWorlds = source.getBoolean("compatibility.exempt-void-worlds", exemptVoidWorlds);
        voidWorldFloor = source.getInt("compatibility.void-world-floor", voidWorldFloor);

        toleranceDecayPerTick = source.getDouble("tolerance.decay-per-tick", toleranceDecayPerTick);
        toleranceMaximum = source.getDouble("tolerance.maximum", toleranceMaximum);
        toleranceCarryOverCap = source.getDouble("tolerance.carry-over-cap", toleranceCarryOverCap);
        toleranceCarryOverRetention = source.getDouble("tolerance.carry-over-retention", toleranceCarryOverRetention);
        baseTolerance = source.getDouble("tolerance.base", baseTolerance);
        tuningProfile = source.getString("tuning.profile", tuningProfile);
        chatPrefix = source.getString("general.chat-prefix", chatPrefix);
        reportRetentionDays = Math.max(0, source.getInt("reports.retention-days", reportRetentionDays));
        bypassFile = source.getString("bypass.file", bypassFile);
        visualEntityHiding = source.getBoolean("visual.entity-hiding", visualEntityHiding);
        visualSoundFuzzing = source.getBoolean("visual.sound-fuzzing", visualSoundFuzzing);
        visualRevealRadius = source.getDouble("visual.reveal-radius", visualRevealRadius);
        visualRevealPadding = source.getDouble("visual.reveal-padding", visualRevealPadding);
        visualSoundJitter = source.getDouble("visual.sound-jitter", visualSoundJitter);
        visualIntervalTicks = Math.max(1, source.getInt("visual.interval-ticks", visualIntervalTicks));
        pingToleranceFloor = source.getDouble("tolerance.ping-floor", pingToleranceFloor);
        pingTolerancePerMilli = source.getDouble("tolerance.ping-per-ms", pingTolerancePerMilli);
        maxPingTolerance = source.getDouble("tolerance.ping-maximum", maxPingTolerance);
        tpsToleranceFloor = source.getDouble("tolerance.tps-floor", tpsToleranceFloor);
        tpsTolerancePerMiss = source.getDouble("tolerance.tps-per-miss", tpsTolerancePerMiss);
        maxTpsTolerance = source.getDouble("tolerance.tps-maximum", maxTpsTolerance);
        safeTps = source.getDouble("tolerance.safe-tps", safeTps);
        lowTpsSafety = source.getDouble("tolerance.low-tps-safety", lowTpsSafety);

        reachMaximum = source.getDouble("combat.reach.maximum", reachMaximum);
        reachTolerance = source.getDouble("combat.reach.tolerance", reachTolerance);
        reachSparrowTolerance = source.getDouble("combat.reach.vehicle-tolerance", reachSparrowTolerance);
        reachLatencyMultiplier = source.getDouble("combat.reach.latency-multiplier", reachLatencyMultiplier);
        reachMaxLatencyBonus = source.getDouble("combat.reach.max-latency-bonus", reachMaxLatencyBonus);
        clickerMaxCps = source.getDouble("combat.autoclicker.max-cps", clickerMaxCps);
        clickerWindowSize = source.getInt("combat.autoclicker.window-size", clickerWindowSize);
        badPacketMaxPerSecond = source.getDouble("packets.bad-packets.max-per-second", badPacketMaxPerSecond);
        timerMinimumTps = source.getDouble("packets.timer.minimum-tps", timerMinimumTps);
        timerMaxDeviation = source.getDouble("packets.timer.max-deviation", timerMaxDeviation);
        timerExemptionPing = source.getDouble("packets.timer.exemption-ping", timerExemptionPing);
        maxBlocksPerSecondDigging = source.getInt("world.fast-break.max-per-second", maxBlocksPerSecondDigging);
    }

    public boolean enabled() {
        return enabled;
    }

    public void enabled(boolean value) {
        this.enabled = value;
    }

    public boolean debug() {
        return debug;
    }

    public void debug(boolean value) {
        this.debug = value;
    }

    public String alertPrefix() {
        return alertPrefix;
    }

    public boolean alertsEnabled() {
        return alertsEnabled;
    }

    public boolean consoleAlerts() {
        return consoleAlerts;
    }

    public String consoleAlertFormat() {
        return consoleAlertFormat;
    }

    public String consoleAlertFormatVerbose() {
        return consoleAlertFormatVerbose;
    }

    public int alertCooldownMillis() {
        return alertCooldownMillis;
    }

    public boolean alertOnRejoinWithHistory() {
        return alertOnRejoinWithHistory;
    }

    public void logRetentionDays(int value) {
        this.logRetentionDays = Math.max(1, value);
    }

    public int logRetentionDays() {
        return logRetentionDays;
    }

    public void historyRetentionDays(int value) {
        this.historyRetentionDays = Math.max(1, value);
    }

    public void preventionEnabled(boolean value) {
        this.preventionEnabled = value;
    }

    public void alertCooldownMillis(int value) {
        this.alertCooldownMillis = Math.max(0, value);
    }

    public int historyRetentionDays() {
        return historyRetentionDays;
    }

    public int historyPerPlayer() {
        return historyPerPlayer;
    }

    public boolean logToFile() {
        return logToFile;
    }

    public String logDirectory() {
        return logDirectory;
    }

    public String debugPermission() {
        return debugPermission;
    }

    public String bypassPermission() {
        return bypassPermission;
    }

    public String adminPermission() {
        return adminPermission;
    }

    public String alertPermission() {
        return alertPermission;
    }

    public double setbackHorizontal() {
        return setbackHorizontal;
    }

    public double setbackVertical() {
        return setbackVertical;
    }

    public boolean preventionEnabled() {
        return preventionEnabled;
    }

    public double minConfidenceForPrevention() {
        return minConfidenceForPrevention;
    }

    public dev.snuffac.core.world.ObfuscationPolicy antiXrayMode() {
        return antiXrayMode;
    }

    public int antiXrayBandStart() {
        return antiXrayBandStart;
    }

    public int antiXrayBandEnd() {
        return antiXrayBandEnd;
    }

    public boolean exemptBedrock() {
        return exemptBedrock;
    }

    public boolean exemptLegacyProtocol() {
        return exemptLegacyProtocol;
    }

    public boolean allowUnknownProtocols() {
        return allowUnknownProtocols;
    }

    public boolean exemptVoidWorlds() {
        return exemptVoidWorlds;
    }

    public int voidWorldFloor() {
        return voidWorldFloor;
    }

    public boolean escalationEnabled() {
        return escalationEnabled;
    }

    public void escalationEnabled(boolean value) {
        this.escalationEnabled = value;
    }

    public void escalationMaxWarnings(int value) {
        this.escalationMaxWarnings = Math.max(1, value);
    }

    public void escalationBanMillis(long value) {
        this.escalationBanMillis = Math.max(0L, value);
    }

    public void escalationMinConfidence(double value) {
        this.escalationMinConfidence = value;
    }

    public void escalationWarnOnly(boolean value) {
        this.escalationWarnOnly = value;
    }

    public int escalationMaxWarnings() {
        return escalationMaxWarnings;
    }

    public long escalationBanMillis() {
        return escalationBanMillis;
    }

    public double escalationMinConfidence() {
        return escalationMinConfidence;
    }

    public boolean escalationWarnOnly() {
        return escalationWarnOnly;
    }

    public java.util.List<String> mutedAllowedCommands() {
        return java.util.Collections.unmodifiableList(mutedAllowedCommands);
    }

    public boolean mutedCommandAllowed(String command) {
        if (command == null) {
            return false;
        }
        String cleaned = command.trim().toLowerCase(java.util.Locale.ROOT);
        if (cleaned.startsWith("/")) {
            cleaned = cleaned.substring(1);
        }
        int space = cleaned.indexOf(' ');
        if (space > 0) {
            cleaned = cleaned.substring(0, space);
        }
        int colon = cleaned.indexOf(':');
        if (colon > 0) {
            cleaned = cleaned.substring(colon + 1);
        }
        return mutedAllowedCommands.contains(cleaned);
    }

    public boolean staffSounds() {
        return staffSounds;
    }

    public boolean antiXrayHideContainers() {
        return antiXrayHideContainers;
    }

    public int maxViolationsPerSecond() {
        return maxViolationsPerSecond;
    }

    public double toleranceDecayPerTick() {
        return toleranceDecayPerTick;
    }

    public double toleranceMaximum() {
        return toleranceMaximum;
    }

    public double toleranceCarryOverCap() {
        return toleranceCarryOverCap;
    }

    public double toleranceCarryOverRetention() {
        return toleranceCarryOverRetention;
    }

    public double baseTolerance() {
        return baseTolerance;
    }

    public boolean visualEntityHiding() {
        return visualEntityHiding;
    }

    public boolean visualSoundFuzzing() {
        return visualSoundFuzzing;
    }

    public double visualRevealRadius() {
        return visualRevealRadius;
    }

    public double visualRevealPadding() {
        return visualRevealPadding;
    }

    public double visualSoundJitter() {
        return visualSoundJitter;
    }

    public int visualIntervalTicks() {
        return visualIntervalTicks;
    }

    public String bypassFile() {
        return bypassFile;
    }

    public int reportRetentionDays() {
        return reportRetentionDays;
    }

    public String chatPrefix() {
        return chatPrefix;
    }

    public void chatPrefix(String value) {
        this.chatPrefix = value == null ? "" : value;
    }

    public String tuningProfile() {
        return tuningProfile;
    }

    public double profileToleranceScale() {
        String value = tuningProfile == null ? "" : tuningProfile.trim().toLowerCase(java.util.Locale.ROOT);
        if (value.equals("lenient")) {
            return 2.0;
        }
        if (value.equals("balanced")) {
            return 1.0;
        }
        return 0.5;
    }

    public double profileReachScale() {
        String value = tuningProfile == null ? "" : tuningProfile.trim().toLowerCase(java.util.Locale.ROOT);
        if (value.equals("lenient")) {
            return 1.6;
        }
        if (value.equals("balanced")) {
            return 1.0;
        }
        return 0.6;
    }

    public double safeTps() {
        return safeTps;
    }

    public double lowTpsSafety() {
        return lowTpsSafety;
    }

    public double toleranceFor(double pingMillis, double tps, double toleranceScale) {
        double pingPart = pingToleranceFloor
                + Math.max(0.0, pingMillis - 40.0) * pingTolerancePerMilli;
        pingPart = Math.min(pingPart, maxPingTolerance);

        double tpsPart = tpsToleranceFloor;
        if (tps < safeTps) {
            tpsPart += (safeTps - tps) * tpsTolerancePerMiss;
        }
        tpsPart = Math.min(tpsPart, maxTpsTolerance);

        return (baseTolerance + (pingPart + tpsPart) * Math.max(toleranceScale, 0.0))
                * profileToleranceScale();
    }

    public double reachToleranceFor(double pingMillis, boolean inVehicle) {
        double base = inVehicle ? reachSparrowTolerance : reachTolerance;
        double latencyBonus = MathUtil.clamp(pingMillis * reachLatencyMultiplier, 0.0, reachMaxLatencyBonus);
        return (base + latencyBonus) * profileReachScale();
    }

    public double reachMaximum() {
        return reachMaximum;
    }

    public double clickerMaxCps() {
        return clickerMaxCps;
    }

    public int clickerWindowSize() {
        return clickerWindowSize;
    }

    public double badPacketMaxPerSecond() {
        return badPacketMaxPerSecond;
    }

    public double timerMinimumTps() {
        return timerMinimumTps;
    }

    public double timerMaxDeviation() {
        return timerMaxDeviation;
    }

    public double timerExemptionPing() {
        return timerExemptionPing;
    }

    public int maxBlocksPerSecondDigging() {
        return maxBlocksPerSecondDigging;
    }
}
