package dev.snuffac.core.player;

import dev.snuffac.core.physics.MovementEnvironment;

public final class EnvironmentMapper {

    private EnvironmentMapper() {
    }

    public static MovementEnvironment from(MovementState state, PlayerWorldCache cache) {
        double slipperiness = cache == null ? 0.6 : cache.slipperinessBelow();
        return new MovementEnvironment(
                cache != null && cache.onGroundBelow(),
                state.inWater(),
                state.inLava(),
                state.inWaterOrLava(),
                state.swimming(),
                state.onClimbable(),
                state.onHoney(),
                state.onIce(),
                state.onSlime(),
                state.onSoulSand(),
                state.gliding(),
                state.flying(),
                state.riding(),
                state.sneaking(),
                state.sprinting(),
                state.usingItem(),
                state.onGround(),
                state.horizontalCollision(),
                state.hasLevitation(),
                state.hasSlowFalling(),
                false,
                state.jumpBoostLevel(),
                state.speedLevel(),
                state.slownessLevel(),
                slipperiness,
                slipperiness,
                state.fallDistance(),
                0);
    }
}
