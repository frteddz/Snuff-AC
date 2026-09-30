package dev.snuffac.core.check.impl.movement;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.core.check.Check;
import dev.snuffac.core.check.CheckContext;
import dev.snuffac.core.packet.MovementPacket;
import dev.snuffac.core.packet.PacketType;
import dev.snuffac.core.packet.SnuffPacket;
import dev.snuffac.core.util.BlockPos;
import java.util.Map;
import java.util.Set;

public final class PhaseCheck extends AbstractMovementCheck {

    public static final double MIN_DISTANCE = 0.35;

    private static final int REQUIRED_CONSECUTIVE = 2;
    private static final double MAX_DISTANCE_PER_TICK = 8.0;

    @Override
    public String key() {
        return "phase";
    }

    @Override
    public String name() {
        return "Phase";
    }

    @Override
    public CheckCategory category() {
        return CheckCategory.MOVEMENT;
    }

    @Override
    public String description() {
        return "Sweeps the path between two positions against the server block view and rejects "
                + "movement that passes through solid blocks.";
    }

    @Override
    public Set<PacketType> packetInterests() {
        return Set.of(PacketType.MOVEMENT);
    }

    @Override
    public Object createState() {
        return new PhaseState();
    }

    @Override
    public void onPacket(CheckContext context, SnuffPacket packet) {
        if (!isPositionUpdate(packet)) {
            return;
        }
        var state = (PhaseState) state(context.player());
        if (state == null) {
            return;
        }

        var player = context.player();
        var movement = player.movement();
        var cache = player.worldCache();

        if (!cache.chunkLoaded() || movement.ticksSinceTeleport() <= 2
                || movement.pendingSetback()) {
            state.strikes = 0;
            return;
        }
        if (movement.inVehicle() || movement.riding() || movement.inWaterOrLava()
                || movement.onClimbable() || movement.ticksSinceKnockback() < 12
                || movement.ticksSinceBlockChange() < 3 || movement.teleportedThisTick()) {
            state.strikes = 0;
            return;
        }

        var from = movement.lastPosition();
        var to = movement.position();
        double travelled = from.distanceTo(to);
        if (travelled < MIN_DISTANCE) {
            state.strikes = 0;
            return;
        }

        BlockPos wall = PhaseSweep.findSolid(from, to, cache::blocksMovement);
        if (wall == null) {
            if (travelled > MAX_DISTANCE_PER_TICK) {
                state.strikes++;
            } else {
                state.strikes = 0;
            }
            if (state.strikes < REQUIRED_CONSECUTIVE) {
                return;
            }
            Map<String, Object> teleport = context.newEvidence();
            teleport.put("mode", "distance");
            teleport.put("travelled", round(travelled));
            teleport.put("maximum", MAX_DISTANCE_PER_TICK);
            teleport.put("ticks", state.strikes);
            context.requestSetback("moved " + round(travelled) + " blocks in one tick");
            context.flag("moved " + round(travelled) + " blocks in a single tick", teleport, 10.0);
            state.strikes = 0;
            return;
        }

        state.strikes++;
        if (state.strikes < REQUIRED_CONSECUTIVE) {
            return;
        }

        Map<String, Object> evidence = context.newEvidence();
        evidence.put("mode", "through solid");
        evidence.put("wallX", wall.x());
        evidence.put("wallY", wall.y());
        evidence.put("wallZ", wall.z());
        evidence.put("material", cache.materialAt(wall));
        evidence.put("travelled", round(travelled));
        evidence.put("fromX", round(from.x()));
        evidence.put("fromY", round(from.y()));
        evidence.put("fromZ", round(from.z()));
        evidence.put("toX", round(to.x()));
        evidence.put("toY", round(to.y()));
        evidence.put("toZ", round(to.z()));
        evidence.put("sprinting", movement.sprinting());
        evidence.put("onGround", movement.onGround());
        evidence.put("airTicks", movement.ticksSinceGround());

        context.requestSetback("moved through " + wall.toString());
        context.flag("movement passed through a solid block at " + wall.toString(), evidence, 12.0);
        state.strikes = 0;
    }

    private static double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    static final class PhaseState {

        private int strikes;
    }
}
