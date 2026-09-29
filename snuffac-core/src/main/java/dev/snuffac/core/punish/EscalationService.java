package dev.snuffac.core.punish;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EscalationService implements AutoCloseable {

    public static final int DEFAULT_MAX_WARNINGS = 5;
    public static final long DEFAULT_BAN_MILLIS = 3_600_000L;

    private final PunishmentService punishments;
    private final ConcurrentHashMap<UUID, EscalationState> states = new ConcurrentHashMap<>();
    private volatile boolean enabled = true;
    private volatile int maxWarnings = DEFAULT_MAX_WARNINGS;
    private volatile long banMillis = DEFAULT_BAN_MILLIS;
    private volatile double minConfidence;
    private volatile boolean warnOnly;
    private volatile long cooldownMillis = 600_000L;

    public EscalationService(PunishmentService punishments, double minConfidence) {
        this.punishments = punishments;
        this.minConfidence = minConfidence;
    }

    public void enabled(boolean value) {
        this.enabled = value;
    }

    public boolean enabled() {
        return enabled;
    }

    public void maxWarnings(int value) {
        this.maxWarnings = Math.max(1, value);
    }

    public int maxWarnings() {
        return maxWarnings;
    }

    public void banMillis(long value) {
        this.banMillis = Math.max(0L, value);
    }

    public long banMillis() {
        return banMillis;
    }

    public void minConfidence(double value) {
        this.minConfidence = Math.max(0.0, value);
    }

    public double minConfidence() {
        return minConfidence;
    }

    public void cooldownMillis(long value) {
        this.cooldownMillis = Math.max(0L, value);
    }

    public long cooldownMillis() {
        return cooldownMillis;
    }

    public void warnOnly(boolean value) {
        this.warnOnly = value;
    }

    public boolean warnOnly() {
        return warnOnly;
    }

    public EscalationState state(UUID playerId) {
        return states.computeIfAbsent(playerId, key -> new EscalationState());
    }

    public void resetForStaffAction(UUID playerId) {
        states.remove(playerId);
    }

    public void clearCooldown(UUID playerId) {
        state(playerId).cooldownUntil = 0L;
    }

    @Override
    public void close() {
        states.clear();
    }

    public void clearAll() {
        states.clear();
    }

    public Outcome onFlag(UUID playerId, String playerName, String checkName, String checkLabel,
            double confidence, String staff) {

        if (!enabled) {
            return new Outcome(OutcomeKind.IGNORED_DISABLED, 0, maxWarnings, 0L, "");
        }
        if (confidence < minConfidence) {
            return new Outcome(OutcomeKind.IGNORED_LOW_CONFIDENCE, 0, maxWarnings, 0L, "");
        }

        EscalationState current = state(playerId);
        if (current.cooldownActive(System.currentTimeMillis())) {
            return new Outcome(OutcomeKind.IGNORED_COOLDOWN, 0, maxWarnings, 0L, "");
        }

        synchronized (current) {
            current.cooldownUntil = System.currentTimeMillis() + cooldownMillis;
            if (current.warnings >= maxWarnings) {
                return new Outcome(OutcomeKind.ALREADY_BANNED, 0, maxWarnings, 0L, "");
            }
            current.warnings++;
            current.lastCheck = checkName;
            current.lastCheckLabel = checkLabel;
            current.lastFlagMillis = System.currentTimeMillis();

            PunishmentService.Punishment warning = punishments.punish(
                    PunishmentService.Kind.WARN,
                    playerId,
                    playerName,
                    "",
                    checkLabel,
                    "Snuff AC",
                    0L);
            current.lastWarningId = warning.id();

            if (current.warnings < maxWarnings) {
                return new Outcome(OutcomeKind.WARNED, current.warnings, maxWarnings, 0L, checkLabel);
            }
            if (warnOnly || banMillis <= 0L) {
                return new Outcome(OutcomeKind.WARN_LIMIT_REACHED, current.warnings, maxWarnings, 0L, checkLabel);
            }
        }

        String reason = "Warned " + maxWarnings + " times for " + checkLabel
                + ". If you believe this is a mistake, contact the admins after the ban expires.";
        punishments.punish(
                PunishmentService.Kind.TEMPBAN,
                playerId,
                playerName,
                "",
                reason,
                "Snuff AC",
                banMillis);
        return new Outcome(
                OutcomeKind.BANNED,
                maxWarnings,
                maxWarnings,
                banMillis,
                checkLabel);
    }

    public enum OutcomeKind {
        WARNED,
        WARN_LIMIT_REACHED,
        BANNED,
        ALREADY_BANNED,
        IGNORED_DISABLED,
        IGNORED_LOW_CONFIDENCE,
        IGNORED_COOLDOWN
    }

    public record Outcome(
            OutcomeKind kind,
            int warnings,
            int maxWarnings,
            long banMillis,
            String checkLabel) {

        public boolean actionable() {
            return kind == OutcomeKind.WARNED
                    || kind == OutcomeKind.WARN_LIMIT_REACHED
                    || kind == OutcomeKind.BANNED;
        }
    }

    public static final class EscalationState {

        private volatile int warnings;
        private volatile String lastCheck = "";
        private volatile String lastCheckLabel = "";
        private volatile long lastFlagMillis;
        private volatile long cooldownUntil;
        private volatile UUID lastWarningId;

        public int warnings() {
            return warnings;
        }

        public String lastCheck() {
            return lastCheck;
        }

        public String lastCheckLabel() {
            return lastCheckLabel;
        }

        public long lastFlagMillis() {
            return lastFlagMillis;
        }

        public UUID lastWarningId() {
            return lastWarningId;
        }

        public boolean cooldownActive(long now) {
            return now < cooldownUntil;
        }

        public long cooldownRemaining(long now) {
            return Math.max(0L, cooldownUntil - now);
        }
    }
}
