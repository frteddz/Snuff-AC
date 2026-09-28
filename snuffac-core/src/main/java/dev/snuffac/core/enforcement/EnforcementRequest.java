package dev.snuffac.core.enforcement;

import dev.snuffac.api.Vec3d;

public record EnforcementRequest(
        EnforcementType type,
        java.util.UUID playerId,
        String checkKey,
        String reason,
        Vec3d target,
        double confidence,
        long timestampMillis
) {

    public static EnforcementRequest flagOnly(java.util.UUID playerId, String checkKey, String reason) {
        return new EnforcementRequest(EnforcementType.FLAGGED_ONLY, playerId, checkKey, reason,
                Vec3d.ZERO, 0.0, System.currentTimeMillis());
    }

    public static EnforcementRequest setback(
            java.util.UUID playerId, String checkKey, String reason, Vec3d target, double confidence) {
        return new EnforcementRequest(EnforcementType.SETBACK_POSITION, playerId, checkKey, reason,
                target, confidence, System.currentTimeMillis());
    }

    public static EnforcementRequest cancel(
            EnforcementType type,
            java.util.UUID playerId,
            String checkKey,
            String reason,
            double confidence) {
        return new EnforcementRequest(type, playerId, checkKey, reason,
                Vec3d.ZERO, confidence, System.currentTimeMillis());
    }

    public boolean preventsAnything() {
        return type != EnforcementType.NONE && type != EnforcementType.FLAGGED_ONLY;
    }
}
