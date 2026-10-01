package dev.snuffac.core.check.impl.combat;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.core.check.Check;
import dev.snuffac.core.check.CheckContext;
import dev.snuffac.core.packet.HeldItemChangePacket;
import dev.snuffac.core.packet.PacketType;
import dev.snuffac.core.packet.SnuffPacket;
import java.util.Map;
import java.util.Set;

public final class AutoTotemCheck implements Check {

    public static final long REACTION_MILLIS = 250L;
    public static final long MIN_REACTION_MILLIS = 1L;
    public static final int REQUIRED_REACTIONS = 5;
    public static final int WINDOW_SIZE = 12;
    public static final double JITTER_RATIO = 0.35;

    @Override
    public Set<PacketType> packetInterests() {
        return Set.of(PacketType.HELD_ITEM_CHANGE, PacketType.MOVEMENT);
    }

    @Override
    public String key() {
        return "autototem";
    }

    @Override
    public String name() {
        return "AutoTotem";
    }

    @Override
    public CheckCategory category() {
        return CheckCategory.COMBAT;
    }

    @Override
    public String description() {
        return "Detects hotbar slot changes landing an identical short time after the player "
                + "takes damage, which is the AutoTotem cheat.";
    }

    @Override
    public Object createState() {
        return new TotemState();
    }

    @Override
    public void onPacket(CheckContext context, SnuffPacket packet) {
        var state = (TotemState) state(context.player());
        if (state == null) {
            return;
        }

        if (packet instanceof dev.snuffac.core.packet.MovementPacket) {
            return;
        }
        if (!(packet instanceof HeldItemChangePacket change)) {
            return;
        }

        var combat = context.player().combat();
        long damaged = combat.lastDamageMillis();
        if (damaged <= 0L) {
            return;
        }

        long reaction = Math.abs(change.arrivalNanos() / 1_000_000L - damaged);
        if (reaction < MIN_REACTION_MILLIS || reaction > REACTION_MILLIS) {
            return;
        }

        state.reactions.add(reaction);
        state.slots.add(change.slot());
        if (state.reactions.size() > WINDOW_SIZE) {
            state.reactions.remove(0);
            state.slots.remove(0);
        }
        if (state.reactions.size() < REQUIRED_REACTIONS) {
            return;
        }

        double mean = mean(state.reactions);
        double jitter = meanAbsoluteDeviation(state.reactions, mean);
        if (mean <= 0.0 || jitter / mean > JITTER_RATIO) {
            return;
        }

        Map<String, Object> evidence = context.newEvidence();
        evidence.put("meanReactionMs", round(mean));
        evidence.put("jitterMs", round(jitter));
        evidence.put("samples", state.reactions.size());
        evidence.put("slots", state.slots);
        evidence.put("offhand", change.offhand());
        evidence.put("lastSlot", change.slot());

        context.flag("changed slot " + round(mean) + "ms after taking damage, "
                + "with consistent timing across " + state.reactions.size() + " hits", evidence, 7.0);
        state.reactions.clear();
        state.slots.clear();
    }

    private static double mean(java.util.List<Long> values) {
        double total = 0.0;
        for (long value : values) {
            total += value;
        }
        return values.isEmpty() ? 0.0 : total / values.size();
    }

    private static double meanAbsoluteDeviation(java.util.List<Long> values, double mean) {
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
        return Math.round(value * 100.0) / 100.0;
    }

    static final class TotemState {

        private final java.util.ArrayList<Long> reactions = new java.util.ArrayList<>(WINDOW_SIZE);
        private final java.util.ArrayList<Integer> slots = new java.util.ArrayList<>(WINDOW_SIZE);
    }
}
