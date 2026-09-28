package dev.snuffac.core.check.impl.combat;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.api.Vec3d;
import dev.snuffac.core.check.Check;
import dev.snuffac.core.check.CheckContext;
import dev.snuffac.core.packet.AttackPacket;
import dev.snuffac.core.packet.PacketType;
import dev.snuffac.core.packet.SnuffPacket;
import dev.snuffac.core.physics.MovementConstants;
import java.util.Map;
import java.util.Set;

public final class ReachCheck implements Check {

    private static final double MIN_EXCESS = 0.02;

    @Override
    public Set<PacketType> packetInterests() {
        return Set.of(PacketType.ATTACK);
    }

    @Override
    public String key() {
        return "reach";
    }

    @Override
    public String name() {
        return "Reach";
    }

    @Override
    public CheckCategory category() {
        return CheckCategory.COMBAT;
    }

    @Override
    public String description() {
        return "Measures eye to hitbox distance with ping aware tolerance and rejects attacks outside the limit.";
    }

    @Override
    public Object createState() {
        return new ReachState();
    }

    @Override
    public void onPacket(CheckContext context, SnuffPacket packet) {
        if (!(packet instanceof AttackPacket attack)) {
            return;
        }
        var state = (ReachState) state(context.player());
        if (state == null) {
            return;
        }

        var player = context.player();
        var movement = player.movement();

        if (movement.ticksSinceTeleport() <= 1) {
            return;
        }

        Vec3d eye = eyePosition(player.position(), movement.pitch());
        double reach = eye.distanceTo(attack.cursorPosition());

        player.combat().recordReach(reach);

        double ping = context.ping();
        boolean inVehicle = movement.inVehicle();
        double tolerance = context.config().reachToleranceFor(ping, inVehicle);
        double maximum = context.config().reachMaximum() + tolerance;

        if (reach <= maximum) {
            state.excessTicks = 0;
            return;
        }

        double excess = reach - maximum;
        if (excess < MIN_EXCESS) {
            return;
        }

        state.excessTicks++;
        if (state.excessTicks < 2) {
            return;
        }

        Map<String, Object> evidence = context.newEvidence();
        evidence.put("reach", round(reach));
        evidence.put("maximum", round(maximum));
        evidence.put("excess", round(excess));
        evidence.put("tolerance", round(tolerance));
        evidence.put("ping", round(ping));
        evidence.put("attackDistance", round(player.position().distanceTo(attack.cursorPosition())));
        evidence.put("average", round(player.combat().reachAverage()));
        evidence.put("deviation", round(player.combat().reachDeviation()));
        evidence.put("momentum", round(player.combat().lastReachMomentum()));
        evidence.put("inVehicle", inVehicle);
        evidence.put("sprinting", movement.sprinting());

        context.flag("reach of " + round(reach) + " exceeds " + round(maximum), evidence,
                Math.min(excess * 14.0, 12.0));
        state.excessTicks = 0;
    }

    static Vec3d eyePosition(Vec3d position, float pitch) {
        double height = MovementConstants.PLAYER_EYE_HEIGHT;
        return new Vec3d(position.x(), position.y() + height, position.z());
    }

    private static double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    static final class ReachState {

        private int excessTicks;
    }
}
