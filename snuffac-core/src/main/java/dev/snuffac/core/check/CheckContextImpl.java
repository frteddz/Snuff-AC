package dev.snuffac.core.check;

import dev.snuffac.api.Vec3d;
import dev.snuffac.core.config.CheckConfig;
import dev.snuffac.core.config.SnuffConfig;
import dev.snuffac.core.player.PlayerData;
import dev.snuffac.core.server.ServerHealth;
import dev.snuffac.core.violation.CheckState;
import dev.snuffac.core.violation.ViolationHandler;
import dev.snuffac.core.violation.ViolationRecord;
import java.util.LinkedHashMap;
import java.util.Map;

final class CheckContextImpl implements CheckContext {

    private final PlayerData player;
    private final Check check;
    private final CheckConfig checkConfig;
    private final SnuffConfig config;
    private final ServerHealth server;
    private final ViolationHandler violations;
    private final CheckState state;
    private final DebugSink debugSink;

    private Map<String, Object> evidence;

    CheckContextImpl(
            PlayerData player,
            Check check,
            CheckConfig checkConfig,
            SnuffConfig config,
            ServerHealth server,
            ViolationHandler violations,
            CheckState state,
            DebugSink debugSink) {
        this.player = player;
        this.check = check;
        this.checkConfig = checkConfig;
        this.config = config;
        this.server = server;
        this.violations = violations;
        this.state = state;
        this.debugSink = debugSink;
    }

    @Override
    public PlayerData player() {
        return player;
    }

    @Override
    public SnuffConfig config() {
        return config;
    }

    @Override
    public Check check() {
        return check;
    }

    @Override
    public ServerHealth server() {
        return server;
    }

    CheckConfig checkConfig() {
        return checkConfig;
    }

    CheckState state() {
        return state;
    }

    @Override
    public void flag(String detail) {
        flag(detail, null, defaultBufferAmount());
    }

    @Override
    public void flag(String detail, Map<String, Object> extraEvidence) {
        flag(detail, extraEvidence, defaultBufferAmount());
    }

    @Override
    public void flag(String detail, Map<String, Object> extraEvidence, double bufferAmount) {
        violations.report(
                player, checkConfig, check.key(), check.name(), detail,
                merge(evidence, extraEvidence), bufferAmount, false);
    }

    @Override
    public void flagImmediately(String detail, Map<String, Object> extraEvidence) {
        violations.report(
                player, checkConfig, check.key(), check.name(), detail,
                merge(evidence, extraEvidence), checkConfig.effectiveBufferThreshold(), true);
    }

    private boolean preventionAllowed;

    public void preventionAllowed(boolean value) {
        this.preventionAllowed = value;
    }

    @Override
    public void preventAttack(String reason) {
        if (preventionAllowed) {
            player.prevention().cancelAttack(check.key(), reason);
        }
    }

    @Override
    public void preventPlacement(String reason) {
        if (preventionAllowed) {
            player.prevention().cancelPlacement(check.key(), reason);
        }
    }

    @Override
    public void preventInteraction(String reason) {
        if (preventionAllowed) {
            player.prevention().cancelInteraction(check.key(), reason);
        }
    }

    @Override
    public void requestSetback(String reason) {
        if (preventionAllowed) {
            player.prevention().requestSetback(check.key(), reason);
        }
    }

    private double defaultBufferAmount() {
        return checkConfig.effectiveBufferThreshold() * 0.5;
    }

    private static Map<String, Object> merge(Map<String, Object> first, Map<String, Object> second) {
        if (first == null || first.isEmpty()) {
            return second == null ? Map.of() : second;
        }
        if (second == null || second.isEmpty()) {
            return first;
        }
        Map<String, Object> merged = new LinkedHashMap<>(first);
        merged.putAll(second);
        return merged;
    }

    @Override
    public void debug(String message) {
        if (!player.debugEnabled() && !config.debug()) {
            return;
        }
        debugSink.debug(player, check.key(), message);
    }

    @Override
    public void debug(String key, Object value) {
        if (!player.debugEnabled() && !config.debug()) {
            return;
        }
        debugSink.debug(player, check.key(), key + "=" + value);
    }

    @Override
    public Map<String, Object> newEvidence() {
        Map<String, Object> created = new LinkedHashMap<>(8);
        if (evidence == null) {
            evidence = created;
        }
        return created;
    }

    @Override
    public double ping() {
        return player.network().ping();
    }

    @Override
    public double tps() {
        return server.tps();
    }

    @Override
    public Vec3d position() {
        return player.position();
    }

    @Override
    public ViolationRecord lastViolation() {
        return state == null ? null : state.lastViolation();
    }

    @Override
    public Map<String, Object> collectedEvidence() {
        return evidence == null ? Map.of() : evidence;
    }

    void clearEvidence() {
        this.evidence = null;
    }

    interface DebugSink {

        void debug(PlayerData player, String checkKey, String message);
    }
}
