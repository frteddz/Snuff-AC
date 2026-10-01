package dev.snuffac.core.check.impl.movement;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.api.Vec3d;
import dev.snuffac.core.check.CheckContext;
import dev.snuffac.core.packet.MovementPacket;
import dev.snuffac.core.packet.PacketType;
import dev.snuffac.core.packet.SnuffPacket;
import dev.snuffac.core.util.BlockPos;
import java.util.Map;
import java.util.Set;

public final class SafewalkCheck extends AbstractMovementCheck {

    public static final double STOP_LIMIT = 0.02;
    public static final double EDGE_DISTANCE = 0.7;
    public static final int REQUIRED_STOPS = 6;
    public static final int SEPARATION_TICKS = 4;

    @Override
    public Set<PacketType> packetInterests() {
        return Set.of(PacketType.MOVEMENT);
    }

    @Override
    public String key() {
        return "safewalk";
    }

    @Override
    public String name() {
        return "Safewalk";
    }

    @Override
    public CheckCategory category() {
        return CheckCategory.MOVEMENT;
    }

    @Override
    public String description() {
        return "Detects a player repeatedly stopping dead at the lip of a block edge without "
                + "sneaking, which is the signature of safewalk.";
    }

    @Override
    public Object createState() {
        return new SafewalkState();
    }

    @Override
    public void onPacket(CheckContext context, SnuffPacket packet) {
        if (!isPositionUpdate(packet)) {
            return;
        }
        var state = (SafewalkState) state(context.player());
        if (state == null) {
            return;
        }

        var player = context.player();
        var movement = player.movement();
        var cache = player.worldCache();

        if (!dev.snuffac.core.check.CheckDispatcher.globalGate(
                player, context.tps(), context.ping())) {
            return;
        }
        if (!cache.chunkLoaded() || movement.ticksSinceTeleport() <= 3
                || movement.pendingSetback()) {
            state.reset();
            return;
        }
        if (movement.sneaking() || !movement.onGround() || movement.inVehicle()
                || movement.inWaterOrLava() || movement.onClimbable()) {
            state.reset();
            return;
        }

        double horizontal = movement.delta().horizontalLength();
        if (horizontal >= STOP_LIMIT) {
            state.walkingTicks++;
            state.sinceStop = 0;
            if (state.walkingTicks >= SEPARATION_TICKS) {
                state.stops = 0;
            }
            return;
        }
        state.walkingTicks = 0;
        state.sinceStop++;

        if (!atEdge(cache, movement.position())) {
            state.sinceStop = 0;
            state.stops = 0;
            return;
        }
        if (state.sinceStop < SEPARATION_TICKS) {
            return;
        }

        state.stops++;
        if (state.stops < REQUIRED_STOPS) {
            return;
        }

        Map<String, Object> evidence = context.newEvidence();
        evidence.put("stops", state.stops);
        evidence.put("stillTicks", state.sinceStop);
        evidence.put("stopLimit", STOP_LIMIT);
        evidence.put("edgeDistance", EDGE_DISTANCE);
        evidence.put("sneaking", false);
        evidence.put("positionX", round(movement.position().x()));
        evidence.put("positionZ", round(movement.position().z()));
        evidence.put("positionY", round(movement.position().y()));
        evidence.put("yaw", round(movement.yaw()));

        context.flag("stopped at a block edge " + state.stops + " times without sneaking", evidence, 7.0);
        state.reset();
    }

    public static boolean atEdge(
            dev.snuffac.core.player.PlayerWorldCache cache, Vec3d position) {
        BlockPos feet = BlockPos.of(position);
        if (!cache.blocksMovement(feet.offset(0, -1, 0))) {
            return false;
        }
        return !supportedBeside(cache, feet, (int) Math.floor(position.x() + EDGE_DISTANCE), feet.z())
                || !supportedBeside(cache, feet, (int) Math.floor(position.x() - EDGE_DISTANCE), feet.z())
                || !supportedBeside(cache, feet, feet.x(), (int) Math.floor(position.z() + EDGE_DISTANCE))
                || !supportedBeside(cache, feet, feet.x(), (int) Math.floor(position.z() - EDGE_DISTANCE));
    }

    private static boolean supportedBeside(
            dev.snuffac.core.player.PlayerWorldCache cache, BlockPos feet, int x, int z) {
        if (x == feet.x() && z == feet.z()) {
            return true;
        }
        return cache.blocksMovement(new BlockPos(x, feet.y(), z).offset(0, -1, 0));
    }

    private static double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    static final class SafewalkState {

        private int stops;
        private int sinceStop;
        private int walkingTicks;

        private void reset() {
            stops = 0;
            sinceStop = 0;
            walkingTicks = 0;
        }
    }
}
