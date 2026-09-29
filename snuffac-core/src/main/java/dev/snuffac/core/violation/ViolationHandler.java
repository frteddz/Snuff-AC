package dev.snuffac.core.violation;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.api.violation.ViolationInfo;
import dev.snuffac.core.alert.AlertService;
import dev.snuffac.core.config.CheckConfig;
import dev.snuffac.core.config.SnuffConfig;
import dev.snuffac.core.log.SnuffLogger;
import dev.snuffac.core.player.PlayerData;
import dev.snuffac.core.server.ServerHealth;
import dev.snuffac.api.SnuffPlatform;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class ViolationHandler {

    private final SnuffConfig config;
    private final ServerHealth server;
    private final AlertService alerts;
    private final SnuffLogger logger;
    private final SnuffPlatform platform;

    private final List<Consumer<ViolationInfo>> listeners = new CopyOnWriteArrayList<>();
    private volatile ViolationHistoryStore historyStore;
    private volatile EnforcementChannel enforcementChannel;

    private volatile SetbackHandler setbackHandler;

    public ViolationHandler(
            SnuffConfig config,
            ServerHealth server,
            AlertService alerts,
            SnuffLogger logger,
            SnuffPlatform platform) {
        this.config = config;
        this.server = server;
        this.alerts = alerts;
        this.logger = logger;
        this.platform = platform;
    }

    public void setbackHandler(SetbackHandler handler) {
        this.setbackHandler = handler;
    }

    public void addListener(Consumer<ViolationInfo> listener) {
        listeners.add(listener);
    }

    public void removeListener(Consumer<ViolationInfo> listener) {
        listeners.remove(listener);
    }

    public void report(
            PlayerData player,
            CheckConfig checkConfig,
            String checkKey,
            String checkName,
            String detail,
            Map<String, Object> evidence,
            double bufferAmount,
            boolean immediate) {

        CheckState state = player.checkState(checkKey);
        if (state == null) {
            return;
        }

        if (player.exempt() || !player.alive() || !player.joined()) {
            return;
        }

        if (!config.enabled() || !checkConfig.enabled()) {
            return;
        }

        double level;
        long now = System.currentTimeMillis();

        if (immediate) {
            state.buffer().add(checkConfig.effectiveBufferThreshold());
            level = state.level().value() + checkConfig.violationIncrement();
            state.level().set(level);
        } else {
            state.buffer().add(bufferAmount);
            if (!state.buffer().crossed()) {
                return;
            }
            level = state.level().value() + checkConfig.violationIncrement();
        }

        boolean shouldAlert = immediate || level >= checkConfig.alertThreshold();
        double bufferAtFlag = state.buffer().value();

        ViolationRecord record = new ViolationRecord(
                checkKey, checkConfig.category(), detail, level, bufferAtFlag, now, evidence);
        state.recordFlag(record);
        player.addHistory(record);
        player.incrementViolationCounter();

        var position = player.position();
        ViolationInfo info = new ViolationInfo(
                player.id(),
                player.name(),
                checkConfig.category(),
                checkKey,
                checkName,
                detail,
                level,
                bufferAtFlag,
                bufferAmount,
                player.network().ping(),
                server.tps(),
                now,
                platform,
                player.worldName(),
                position.x(),
                position.y(),
                position.z());

        if (shouldAlert && player.alertsEnabled() && !alerts.onCooldown(player.id().toString(), now)) {
            alerts.alert(info, evidence);
        }

        for (Consumer<ViolationInfo> listener : listeners) {
            try {
                listener.accept(info);
            } catch (RuntimeException exception) {
                if (logger != null) {
                    logger.warn("violation listener failed: " + exception);
                }
            }
        }

        if (player.setbackEnabled() && checkConfig.setbacksEnabled()
                && state.level().value() >= checkConfig.effectiveSetbackThreshold()) {
            SetbackHandler handler = setbackHandler;
            if (handler != null) {
                handler.onSetback(player, state, record, detail);
            }
            state.level().set(Math.max(0.0, state.level().value() - checkConfig.setbackThreshold()));
        }

        EnforcementChannel channel = enforcementChannel;
        if (channel != null) {
            try {
                channel.onViolation(player, checkConfig, checkKey, detail, bufferAtFlag, level);
            } catch (RuntimeException exception) {
                if (logger != null) {
                    logger.warn("enforcement routing failed for " + player.name() + ": " + exception);
                }
            }
        }

        if (logger != null && checkConfig.logsEnabled()) {
            logger.violation(checkKey + " VL=" + level + " " + player.name() + " " + detail);
        }
    }

    public void enforcementChannel(EnforcementChannel channel) {
        this.enforcementChannel = channel;
    }

    public void historyStore(ViolationHistoryStore store) {
        this.historyStore = store;
    }

    public ViolationHistoryStore historyStore() {
        return historyStore;
    }

    public List<ViolationInfo> recent(UUID playerId, int limit) {
        ViolationHistoryStore store = historyStore;
        if (store == null) {
            return List.of();
        }
        List<ViolationInfo> stored = store.history(playerId);
        if (stored.isEmpty()) {
            return List.of();
        }
        int size = Math.min(limit, stored.size());
        return new ArrayList<>(stored.subList(stored.size() - size, stored.size()));
    }

    public void remember(ViolationInfo info) {
        ViolationHistoryStore store = historyStore;
        if (store != null) {
            store.record(info);
        }
    }

    public void forget(UUID playerId) {
        ViolationHistoryStore store = historyStore;
        if (store != null) {
            store.flush(playerId);
        }
    }

    public interface SetbackHandler {

        void onSetback(PlayerData player, CheckState state, ViolationRecord record, String detail);
    }

    public interface EnforcementChannel {

        void onViolation(PlayerData player, CheckConfig checkConfig, String checkKey,
                String detail, double buffer, double level);
    }
}
