package dev.snuffac.core.check.impl.combat;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.core.check.Check;
import dev.snuffac.core.check.CheckContext;
import dev.snuffac.core.packet.AttackPacket;
import dev.snuffac.core.packet.MovementPacket;
import dev.snuffac.core.packet.PacketType;
import dev.snuffac.core.packet.SnuffPacket;
import dev.snuffac.core.util.AxisAlignedBox;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Set;

public final class TriggerBotCheck implements Check {

    public static final long REACTION_NANOS = 60_000_000L;
    public static final long MIN_REACTION_NANOS = 12_000_000L;
    public static final int SAMPLE_TARGETS = 8;
    public static final int REQUIRED_CONSISTENT = 5;
    public static final double JITTER_NANOS = 0.0000090;

    @Override
    public Set<PacketType> packetInterests() {
        return Set.of(PacketType.ATTACK, PacketType.MOVEMENT);
    }

    @Override
    public String key() {
        return "triggerbot";
    }

    @Override
    public String name() {
        return "TriggerBot";
    }

    @Override
    public CheckCategory category() {
        return CheckCategory.COMBAT;
    }

    @Override
    public String description() {
        return "Detects attacks landing an identical short time after the crosshair reaches a "
                + "target, with no aim movement in between, which is the TriggerBot cheat.";
    }

    @Override
    public Object createState() {
        return new TriggerState();
    }

    @Override
    public void onPacket(CheckContext context, SnuffPacket packet) {
        var state = (TriggerState) state(context.player());
        if (state == null) {
            return;
        }

        if (packet instanceof MovementPacket movement) {
            if (movement.rotationChanged()) {
                state.rotatedSinceAttack = true;
                state.lastRotationNanos = packet.arrivalNanos();
                state.lastYaw = movement.yaw();
                state.lastPitch = movement.pitch();
            }
            return;
        }
        if (!(packet instanceof AttackPacket attack)) {
            return;
        }

        var player = context.player();
        if (player.movement().ticksSinceTeleport() <= 2) {
            return;
        }
        if (state.lastRotationNanos == 0L) {
            return;
        }

        long reaction = attack.arrivalNanos() - state.lastRotationNanos;
        if (reaction < MIN_REACTION_NANOS || reaction > REACTION_NANOS) {
            return;
        }
        if (state.rotatedSinceAttack) {
            return;
        }

        double drift = Math.abs(dev.snuffac.core.util.MathUtil.deltaDegrees(state.lastYaw, attack.yaw()))
                + Math.abs(dev.snuffac.core.util.MathUtil.deltaDegrees(state.lastPitch, attack.pitch()));
        if (drift > 0.01) {
            return;
        }

        state.reactions.add(reaction);
        if (state.reactions.size() < SAMPLE_TARGETS) {
            return;
        }
        state.reactions.removeFirst();

        if (state.consistent < REQUIRED_CONSISTENT) {
            state.consistent++;
            return;
        }
        state.consistent = 0;

        double mean = mean(state.reactions);
        double jitter = meanAbsoluteDeviation(state.reactions, mean);

        Map<String, Object> evidence = context.newEvidence();
        evidence.put("meanReactionMs", round(mean / 1_000_000.0));
        evidence.put("jitterMs", round(jitter / 1_000_000.0));
        evidence.put("samples", state.reactions.size());
        evidence.put("yaw", state.lastYaw);
        evidence.put("pitch", state.lastPitch);
        evidence.put("attackedEveryTick", state.turns);
        evidence.put("crosshairMoves", state.rotationMoves);

        context.flag("attacked " + round(mean / 1_000_000.0) + "ms after the crosshair settled, "
                + "with no aim movement in between", evidence, 7.0);
        state.reactions.clear();
    }

    private static double mean(ArrayDeque<Long> values) {
        double total = 0.0;
        for (long value : values) {
            total += value;
        }
        return values.isEmpty() ? 0.0 : total / values.size();
    }

    private static double meanAbsoluteDeviation(ArrayDeque<Long> values, double mean) {
        if (values.isEmpty()) {
            return 0.0;
        }
        double total = 0.0;
        for (long value : values) {
            total += Math.abs(value - mean);
        }
        return total / values.size();
    }

    private static double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    static final class TriggerState {

        private final ArrayDeque<Long> reactions = new ArrayDeque<>();
        private long lastRotationNanos;
        private float lastYaw;
        private float lastPitch;
        private boolean rotatedSinceAttack;
        private int consistent;
        private int turns;
        private int rotationMoves;
    }

}
