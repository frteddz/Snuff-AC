package dev.snuffac.core.check.impl.movement;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.core.check.Check;
import dev.snuffac.core.check.CheckContext;
import dev.snuffac.core.packet.MovementPacket;
import dev.snuffac.core.packet.PacketType;
import dev.snuffac.core.packet.SnuffPacket;
import dev.snuffac.core.util.BlockKind;
import dev.snuffac.core.player.PlayerWorldCache;
import dev.snuffac.core.util.BlockPos;
import java.util.Map;
import java.util.Set;

public final class JesusCheck implements Check {

    public static final double MIN_HORIZONTAL = 0.08;
    public static final int REQUIRED_TICKS = 6;
    public static final double SINK_LIMIT = 0.03;

    @Override
    public Set<PacketType> packetInterests() {
        return Set.of(PacketType.MOVEMENT);
    }

    @Override
    public String key() {
        return "jesus";
    }

    @Override
    public String name() {
        return "Jesus";
    }

    @Override
    public CheckCategory category() {
        return CheckCategory.MOVEMENT;
    }

    @Override
    public String description() {
        return "Detects standing on a liquid surface with no valid block beneath, which is the "
                + "WaterWalk and Jesus cheat.";
    }

    @Override
    public Object createState() {
        return new JesusState();
    }

    @Override
    public void onPacket(CheckContext context, SnuffPacket packet) {
        if (!(packet instanceof MovementPacket movement) || !movement.positionChanged()) {
            return;
        }
        var state = (JesusState) state(context.player());
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
                || movementState.gliding() || movementState.flying()
                || movementState.onClimbable() || movementState.onSoulSand()
                || movementState.ticksSinceKnockback() < 12
                || movementState.pendingSetback()) {
            state.reset();
            return;
        }

        BlockPos feet = BlockPos.of(movementState.position());
        if (!cache.isLiquid(feet)) {
            state.reset();
            return;
        }
        if (hasSupport(cache, feet)) {
            state.reset();
            return;
        }

        double deltaY = movementState.delta().y();
        if (deltaY > SINK_LIMIT) {
            state.reset();
            return;
        }

        double horizontal = movementState.delta().horizontalLength();
        if (horizontal < MIN_HORIZONTAL) {
            state.stillTicks++;
        } else {
            state.stillTicks = 0;
        }

        state.surfaceTicks++;
        if (state.surfaceTicks < REQUIRED_TICKS) {
            return;
        }

        Map<String, Object> evidence = context.newEvidence();
        evidence.put("surfaceTicks", state.surfaceTicks);
        evidence.put("stillTicks", state.stillTicks);
        evidence.put("deltaY", round(deltaY));
        evidence.put("horizontal", round(horizontal));
        evidence.put("velocityY", round(movementState.velocity().y()));
        evidence.put("fallDistance", round(movementState.fallDistance()));
        evidence.put("swimming", movementState.swimming());
        evidence.put("inWater", movementState.inWater());
        evidence.put("inLava", movementState.inLava());
        evidence.put("depth", depthBelow(cache, feet));

        context.requestSetback("standing on " + cache.kindAt(feet) + " with no block beneath");
        context.flag("walked on a liquid surface for " + state.surfaceTicks + " ticks with no block beneath",
                evidence, 9.0);
        state.reset();
    }

    public static boolean hasSupport(PlayerWorldCache cache, BlockPos feet) {
        if (cache.isIce(feet.offset(0, -1, 0))) {
            return true;
        }
        for (int dy = -1; dy >= -2; dy--) {
            BlockPos below = feet.offset(0, dy, 0);
            if (cache.blockAt(below) == null) {
                continue;
            }
            BlockKind kind = cache.kindAt(below);
            if (kind == BlockKind.AIR || kind == BlockKind.WATER || kind == BlockKind.LAVA
                    || kind == BlockKind.BARRIER) {
                continue;
            }
            if (cache.blocksMovement(below)) {
                return true;
            }
        }
        return false;
    }

    private static int depthBelow(PlayerWorldCache cache, BlockPos feet) {
        int depth = 0;
        for (int dy = -1; dy >= -4; dy--) {
            if (cache.isLiquid(feet.offset(0, dy, 0))) {
                depth++;
            }
        }
        return depth;
    }

    private static double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    static final class JesusState {

        private int surfaceTicks;
        private int stillTicks;

        private void reset() {
            surfaceTicks = 0;
            stillTicks = 0;
        }
    }
}
