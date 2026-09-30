package dev.snuffac.core.check.impl.movement;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.core.check.Check;
import dev.snuffac.core.check.CheckContext;
import dev.snuffac.core.packet.MovementPacket;
import dev.snuffac.core.packet.PacketType;
import dev.snuffac.core.packet.SnuffPacket;
import dev.snuffac.core.util.BlockKind;
import dev.snuffac.core.util.BlockPos;
import java.util.Map;
import java.util.Set;

public final class SpiderCheck extends AbstractMovementCheck {

    public static final double MIN_RISE = 0.12;
    public static final int REQUIRED_TICKS = 4;
    public static final double WALL_DISTANCE = 0.7;

    @Override
    public String key() {
        return "spider";
    }

    @Override
    public String name() {
        return "Spider";
    }

    @Override
    public CheckCategory category() {
        return CheckCategory.MOVEMENT;
    }

    @Override
    public String description() {
        return "Detects sustained upward movement pressed against a wall with no climbable block, "
                + "which is the Spider and WallClimb cheat.";
    }

    @Override
    public Set<PacketType> packetInterests() {
        return Set.of(PacketType.MOVEMENT);
    }

    @Override
    public Object createState() {
        return new SpiderState();
    }

    @Override
    public void onPacket(CheckContext context, SnuffPacket packet) {
        if (!(packet instanceof MovementPacket movement) || !movement.positionChanged()) {
            return;
        }
        var state = (SpiderState) state(context.player());
        if (state == null) {
            return;
        }

        var player = context.player();
        var movementState = player.movement();
        var cache = player.worldCache();

        if (!dev.snuffac.core.check.CheckDispatcher.globalGate(
                player, context.tps(), context.ping())) {
            return;
        }
        if (!cache.chunkLoaded() || movementState.ticksSinceTeleport() <= 2
                || movementState.inVehicle() || movementState.riding()
                || movementState.inWaterOrLava() || movementState.onClimbable()
                || movementState.gliding() || movementState.flying()
                || movementState.onSoulSand() || movementState.onHoney()
                || movementState.hasLevitation() || movementState.hasSlowFalling()
                || movementState.ticksSinceKnockback() < 12
                || movementState.pendingSetback()) {
            state.reset();
            return;
        }

        double rise = movementState.delta().y();
        if (rise < MIN_RISE) {
            state.reset();
            return;
        }
        if (!againstWall(cache, movementState.position())) {
            state.reset();
            return;
        }

        state.risingTicks++;
        if (state.risingTicks < REQUIRED_TICKS) {
            return;
        }

        Map<String, Object> evidence = context.newEvidence();
        evidence.put("risingTicks", state.risingTicks);
        evidence.put("rise", round(rise));
        evidence.put("wallDistance", WALL_DISTANCE);
        evidence.put("velocityY", round(movementState.velocity().y()));
        evidence.put("onGround", movementState.onGround());
        evidence.put("airTicks", movementState.ticksSinceGround());
        evidence.put("sprinting", movementState.sprinting());
        evidence.put("positionY", round(movementState.position().y()));

        context.requestSetback("climbing a wall with nothing to climb");
        context.flag("rose " + round(rise) + " blocks per tick against a wall for "
                + state.risingTicks + " ticks", evidence, 9.0);
        state.reset();
    }

    public static boolean againstWall(dev.snuffac.core.player.PlayerWorldCache cache,
            dev.snuffac.api.Vec3d position) {
        BlockPos feet = BlockPos.of(position);
        for (BlockKind kind : new BlockKind[] {BlockKind.SOLID, BlockKind.SLIME, BlockKind.ICE,
                BlockKind.HONEY, BlockKind.SOUL_SAND, BlockKind.PACKED_ICE, BlockKind.BLUE_ICE}) {
            if (isKind(cache, feet, kind)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isKind(
            dev.snuffac.core.player.PlayerWorldCache cache, BlockPos feet, BlockKind kind) {
        return cache.kindAt(feet) == kind;
    }

    private static double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    static final class SpiderState {

        private int risingTicks;

        private void reset() {
            risingTicks = 0;
        }
    }
}
