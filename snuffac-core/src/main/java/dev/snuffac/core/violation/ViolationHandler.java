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
    private final Map<UUID, List<ViolationInfo>> recentByPlayer = new java.util.concurrent.ConcurrentHashMap<>();
    private final int recentLimit = 256;

    private volatile SetbackHandler setbackHandler;
    private volatile CommandExecutor commandExecutor;

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

    public void commandExecutor(CommandExecutor executor) {
        this.commandExecutor = executor;
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
                platform);

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

        if (player.setbackEnabled() && checkConfig.setbacksEnabled() && state.level().value() >= checkConfig.setbackThreshold()) {
            SetbackHandler handler = setbackHandler;
            if (handler != null) {
                handler.onSetback(player, state, record, detail);
            }
            state.level().set(Math.max(0.0, state.level().value() - checkConfig.setbackThreshold()));
        }

        CommandExecutor executor = commandExecutor;
        if (executor != null && state.commandReady(now, checkKey)) {
            executor.execute(player, checkConfig, record, detail);
        }

        if (logger != null && checkConfig.logsEnabled()) {
            logger.violation(checkKey + " VL=" + level + " " + player.name() + " " + detail);
        }
    }

    public List<ViolationInfo> recent(UUID playerId, int limit) {
        List<ViolationInfo> stored = recentByPlayer.get(playerId);
        if (stored == null || stored.isEmpty()) {
            return List.of();
        }
        int size = Math.min(limit, stored.size());
        return new ArrayList<>(stored.subList(stored.size() - size, stored.size()));
    }

    public void remember(ViolationInfo info) {
        recentByPlayer.compute(info.playerId(), (id, list) -> {
            List<ViolationInfo> target = list == null ? new ArrayList<>() : list;
            target.add(info);
            while (target.size() > recentLimit) {
                target.remove(0);
            }
            return target;
        });
    }

    public void forget(UUID playerId) {
        recentByPlayer.remove(playerId);
    }

    public interface SetbackHandler {

        void onSetback(PlayerData player, CheckState state, ViolationRecord record, String detail);
    }

    public interface CommandExecutor {

        void execute(PlayerData player, CheckConfig checkConfig, ViolationRecord record, String detail);
    }
}
