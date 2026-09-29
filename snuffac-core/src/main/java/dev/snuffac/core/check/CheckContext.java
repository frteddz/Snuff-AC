package dev.snuffac.core.check;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.api.Vec3d;
import dev.snuffac.core.config.SnuffConfig;
import dev.snuffac.core.player.PlayerData;
import dev.snuffac.core.server.ServerHealth;
import dev.snuffac.core.violation.ViolationRecord;
import java.util.LinkedHashMap;
import java.util.Map;

public interface CheckContext {

    PlayerData player();

    SnuffConfig config();

    Check check();

    ServerHealth server();

    void flag(String detail);

    void flag(String detail, Map<String, Object> evidence);

    void flag(String detail, Map<String, Object> evidence, double bufferAmount);

    void flagImmediately(String detail, Map<String, Object> evidence);

    void preventAttack(String reason);

    void preventPlacement(String reason);

    void preventInteraction(String reason);

    void requestSetback(String reason);

    void debug(String message);

    void debug(String key, Object value);

    Map<String, Object> newEvidence();

    double ping();

    double tps();

    Vec3d position();

    ViolationRecord lastViolation();

    Map<String, Object> collectedEvidence();
}
