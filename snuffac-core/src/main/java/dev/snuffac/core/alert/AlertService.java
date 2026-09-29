package dev.snuffac.core.alert;

import dev.snuffac.api.violation.ViolationInfo;
import dev.snuffac.core.config.SnuffConfig;
import dev.snuffac.core.log.FileViolationLogger;
import dev.snuffac.core.log.SnuffLogger;
import dev.snuffac.core.platform.SnuffMessenger;
import dev.snuffac.core.platform.SnuffPermissionChecker;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class AlertService {

    private final SnuffConfig config;
    private final SnuffMessenger messenger;
    private final SnuffPermissionChecker permissions;
    private final SnuffLogger logger;
    private final FileViolationLogger fileLogger;
    private final Map<String, Long> lastAlertMillis = new ConcurrentHashMap<>();

    private AlertFormatter formatter;

    public AlertService(
            SnuffConfig config,
            SnuffMessenger messenger,
            SnuffPermissionChecker permissions,
            SnuffLogger logger,
            FileViolationLogger fileLogger) {
        this.config = config;
        this.messenger = messenger;
        this.permissions = permissions;
        this.logger = logger;
        this.fileLogger = fileLogger;
        this.formatter = buildFormatter();
    }

    public void onReload() {
        this.formatter = buildFormatter();
    }

    private AlertFormatter buildFormatter() {
        return new AlertFormatter(
                config.alertPrefix(),
                config.consoleAlertFormat(),
                config.consoleAlertFormatVerbose());
    }

    public boolean onCooldown(String playerId, long nowMillis) {
        if (config.alertCooldownMillis() <= 0) {
            return false;
        }
        Long last = lastAlertMillis.get(playerId);
        if (last != null && nowMillis - last < config.alertCooldownMillis()) {
            return true;
        }
        lastAlertMillis.put(playerId, nowMillis);
        return false;
    }

    public void alert(ViolationInfo info, Map<String, Object> evidence) {
        String message = formatter.format(info, evidence);
        String chat = AlertFormatter.withPrefix(message, config.chatPrefix());
        if (config.alertsEnabled()) {
            if (config.consoleAlerts()) {
                messenger.sendConsole(AlertFormatter.stripMarkup(message));
            }
            messenger.broadcast(chat, config.alertPermission());
        }
        if (config.logToFile() && fileLogger != null) {
            fileLogger.log(renderFileLine(info, evidence));
        }
        if (logger != null) {
            logger.violation(renderFileLine(info, evidence));
        }
    }

    private String renderFileLine(ViolationInfo info, Map<String, Object> evidence) {
        StringBuilder builder = new StringBuilder(160);
        builder.append('[')
                .append(Instant.ofEpochMilli(info.timestampMillis()))
                .append("] ")
                .append(info.playerName())
                .append(" | ")
                .append(info.checkKey())
                .append(" | VL=")
                .append(round(info.violationLevel()))
                .append(" buffer=")
                .append(round(info.buffer()))
                .append(" ping=")
                .append(round(info.pingMillis()))
                .append("ms tps=")
                .append(round(info.tps()))
                .append(" detail=")
                .append(info.detail());
        if (evidence != null && !evidence.isEmpty()) {
            builder.append(" | ");
            boolean first = true;
            for (Map.Entry<String, Object> entry : evidence.entrySet()) {
                if (!first) {
                    builder.append(", ");
                }
                first = false;
                builder.append(entry.getKey()).append('=').append(entry.getValue());
            }
        }
        return builder.toString();
    }

    private static String round(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "n/a";
        }
        return String.valueOf(Math.round(value * 100.0) / 100.0);
    }

    public FileViolationLogger fileLogger() {
        return fileLogger;
    }

    public void clearCooldowns() {
        lastAlertMillis.clear();
    }
}
