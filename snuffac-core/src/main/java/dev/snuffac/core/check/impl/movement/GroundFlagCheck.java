package dev.snuffac.core.check.impl.movement;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.core.check.Check;
import dev.snuffac.core.check.CheckContext;
import dev.snuffac.core.packet.MovementPacket;
import dev.snuffac.core.packet.PacketType;
import dev.snuffac.core.packet.SnuffPacket;
import java.util.Map;
import java.util.Set;

public final class GroundFlagCheck implements Check {

    public static final int MIN_AIR_TICKS = 3;
    public static final int REQUIRED = 4;

    @Override
    public Set<PacketType> packetInterests() {
        return Set.of(PacketType.MOVEMENT);
    }

    @Override
    public String key() {
        return "groundflag";
    }

    @Override
    public String name() {
        return "GroundFlag";
    }

    @Override
    public CheckCategory category() {
        return CheckCategory.MOVEMENT;
    }

    @Override
    public String description() {
        return "Detects sustained ground contact claims that contradict the server block view, the signature of air walk and no fall spoofing.";
    }

    @Override
    public Object createState() {
        return new GroundState();
    }

    @Override
    public void onPacket(CheckContext context, SnuffPacket packet) {
        if (!(packet instanceof MovementPacket movement) || !movement.positionChanged()) {
            return;
        }
        var state = (GroundState) state(context.player());
        if (state == null) {
            return;
        }
        if (!dev.snuffac.core.check.CheckDispatcher.globalGate(
                context.player(), context.tps(), context.ping())) {
            return;
        }

        var player = context.player();
        var cache = player.worldCache();
        var movementState = player.movement();

        if (!movement.onGround() || !cache.chunkLoaded() || cache.onGroundBelow()) {
            state.contradictions = 0;
            state.airTicks = 0;
            return;
        }
        if (movementState.ticksSinceTeleport() <= 2
                || movementState.flying()
                || movementState.gliding()
                || movementState.riding()
                || movementState.inVehicle()
                || movementState.inWaterOrLava()
                || movementState.onClimbable()) {
            state.contradictions = 0;
            state.airTicks = 0;
            return;
        }

        state.airTicks++;
        if (state.airTicks < MIN_AIR_TICKS) {
            return;
        }

        state.contradictions++;
        if (state.contradictions < REQUIRED) {
            return;
        }

        Map<String, Object> evidence = context.newEvidence();
        evidence.put("contradictions", state.contradictions);
        evidence.put("airTicks", state.airTicks);
        evidence.put("groundBelow", false);
        evidence.put("deltaY", Math.round(movementState.delta().y() * 10000.0) / 10000.0);
        evidence.put("position", movement.position().x() + ","
                + movement.position().y() + "," + movement.position().z());
        context.flag("ground claimed with no supporting block for " + state.airTicks + " ticks", evidence, 8.0);
        state.contradictions = 0;
    }

    @Override
    public void onTick(CheckContext context) {
        var state = (GroundState) state(context.player());
        if (state != null && context.player().movement().onGround()) {
            state.contradictions = 0;
        }
    }

    static final class GroundState {

        private int contradictions;
        private int airTicks;
    }
}
