package dev.snuffac.core.check;

import dev.snuffac.core.config.CheckConfig;
import dev.snuffac.core.config.SnuffConfig;
import dev.snuffac.core.log.SnuffLogger;
import dev.snuffac.core.packet.SnuffPacket;
import dev.snuffac.core.player.PlayerData;
import dev.snuffac.core.server.ServerHealth;
import dev.snuffac.core.violation.CheckState;
import dev.snuffac.core.violation.ViolationHandler;
import java.util.List;

public final class CheckDispatcher {

    private final CheckRegistry registry;
    private final SnuffConfig config;
    private final ServerHealth server;
    private final ViolationHandler violations;
    private final SnuffLogger logger;
    private volatile dev.snuffac.core.enforcement.EnforcementService enforcement;

    public void enforcement(dev.snuffac.core.enforcement.EnforcementService value) {
        this.enforcement = value;
    }

    public CheckDispatcher(
            CheckRegistry registry,
            SnuffConfig config,
            ServerHealth server,
            ViolationHandler violations,
            SnuffLogger logger) {
        this.registry = registry;
        this.config = config;
        this.server = server;
        this.violations = violations;
        this.logger = logger;
    }

    public CheckRegistry registry() {
        return registry;
    }

    public CheckContextImpl context(PlayerData player, Check check) {
        CheckConfig checkConfig = registry.config(check);
        CheckState state = player.checkState(check.key());
        if (checkConfig == null || state == null) {
            return null;
        }
        return new CheckContextImpl(player, check, checkConfig, config, server, violations, state, this::debug);
    }

    public void dispatchPacket(PlayerData player, SnuffPacket packet) {
        if (!shouldProcess(player)) {
            return;
        }
        var service = this.enforcement;
        boolean prevention = config.preventionEnabled()
                && service != null
                && service.preventionEnabled();
        updateSharedState(player, packet);
        List<Check> targets = registry.dispatchFor(packet.type());
        for (int i = 0; i < targets.size(); i++) {
            Check check = targets.get(i);
            CheckConfig checkConfig = registry.config(check);
            if (checkConfig == null || !checkConfig.enabled()) {
                continue;
            }
            CheckContextImpl context = context(player, check);
            if (context == null) {
                continue;
            }
            context.preventionAllowed(prevention);
            try {
                check.onPacket(context, packet);
            } catch (RuntimeException exception) {
                logger.debug("check error " + check.key() + " for " + player.name() + ": " + exception);
            } finally {
                context.clearEvidence();
            }
        }
    }

    private void updateSharedState(PlayerData player, SnuffPacket packet) {
        var movement = player.movement();
        if (packet instanceof dev.snuffac.core.packet.MovementPacket move) {
            movement.observeMovementPacket(player.network().tickCounter());
            player.combat().observeMovement(move.arrivalNanos());
            movement.observePositionDelta(move);
        } else if (packet instanceof dev.snuffac.core.packet.BlockPlacePacket) {
            movement.markPlaced();
        } else if (packet instanceof dev.snuffac.core.packet.BlockBreakPacket) {
            movement.markBroke();
        }
    }

    public void dispatchTick(PlayerData player) {
        if (!shouldProcess(player)) {
            return;
        }
        List<Check> checks = registry.all();
        for (int i = 0; i < checks.size(); i++) {
            Check check = checks.get(i);
            CheckConfig checkConfig = registry.config(check);
            if (checkConfig == null || !checkConfig.enabled()) {
                continue;
            }
            CheckContextImpl context = context(player, check);
            if (context == null) {
                continue;
            }
            try {
                check.onTick(context);
            } catch (RuntimeException exception) {
                logger.debug("check tick error " + check.key() + " for " + player.name() + ": " + exception);
            } finally {
                context.clearEvidence();
            }
        }
    }

    public void dispatchJoin(PlayerData player) {
        for (Check check : registry.all()) {
            CheckContextImpl context = context(player, check);
            if (context == null) {
                continue;
            }
            try {
                check.onPlayerJoin(context);
            } catch (RuntimeException exception) {
                logger.debug("join error " + check.key() + ": " + exception);
            } finally {
                context.clearEvidence();
            }
        }
    }

    public void dispatchQuit(PlayerData player) {
        for (Check check : registry.all()) {
            CheckContextImpl context = context(player, check);
            if (context == null) {
                continue;
            }
            try {
                check.onPlayerQuit(context);
            } catch (RuntimeException exception) {
                logger.debug("quit error " + check.key() + ": " + exception);
            } finally {
                context.clearEvidence();
            }
        }
    }

    private boolean shouldProcess(PlayerData player) {
        return config.enabled() && player.alive() && player.joined() && !player.exempt();
    }

    public static boolean globalGate(
            dev.snuffac.core.player.PlayerData player, double tps, double pingMillis) {
        if (dev.snuffac.core.prediction.PredictionGraces.serverLagged(tps)) {
            return false;
        }
        if (dev.snuffac.core.prediction.PredictionGraces.pingUnreliable(pingMillis)) {
            return false;
        }
        return !dev.snuffac.core.prediction.PredictionGraces.joinGraceActive(
                player.joinMillis(), System.currentTimeMillis());
    }

    private void debug(PlayerData player, String checkKey, String message) {
        logger.debug("[" + player.name() + "] " + checkKey + " " + message);
    }
}
