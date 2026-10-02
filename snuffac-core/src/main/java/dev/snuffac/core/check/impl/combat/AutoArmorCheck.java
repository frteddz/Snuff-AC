package dev.snuffac.core.check.impl.combat;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.core.check.Check;
import dev.snuffac.core.check.CheckContext;
import dev.snuffac.core.packet.PacketType;
import dev.snuffac.core.packet.SnuffPacket;
import dev.snuffac.core.packet.WindowClickPacket;
import java.util.Map;
import java.util.Set;

public final class AutoArmorCheck implements Check {

    public static final int FIRST_ARMOR_SLOT = 5;
    public static final int LAST_ARMOR_SLOT = 8;
    public static final long MAX_REACTION_MILLIS = 200L;
    public static final long MIN_REACTION_MILLIS = 1L;
    public static final int REQUIRED = 4;
    public static final double JITTER_RATIO = 0.3;

    @Override
    public Set<PacketType> packetInterests() {
        return Set.of(PacketType.INVENTORY_CLICK);
    }

    @Override
    public String key() {
        return "autoarmor";
    }

    @Override
    public String name() {
        return "AutoArmor";
    }

    @Override
    public CheckCategory category() {
        return CheckCategory.COMBAT;
    }

    @Override
    public String description() {
        return "Detects armour being equipped a fixed short time after the player takes damage, "
                + "which is the AutoArmor cheat.";
    }

    @Override
    public Object createState() {
        return new ArmorState();
    }

    @Override
    public void onPacket(CheckContext context, SnuffPacket packet) {
        var state = (ArmorState) state(context.player());
        if (state == null) {
            return;
        }

        if (!(packet instanceof WindowClickPacket click)) {
            return;
        }
        if (click.windowId() != 0 || !isArmorSlot(click.slot())) {
            return;
        }

        long damaged = context.player().combat().lastDamageMillis();
        if (damaged <= 0L) {
            return;
        }

        long reaction = Math.abs(packet.arrivalNanos() / 1_000_000L - damaged);
        if (reaction < MIN_REACTION_MILLIS || reaction > MAX_REACTION_MILLIS) {
            return;
        }

        state.reactions.add(reaction);
        if (state.reactions.size() > 8) {
            state.reactions.remove(0);
        }
        if (state.reactions.size() < REQUIRED) {
            return;
        }

        long mean = mean(state.reactions);
        if (mean <= 0L) {
            return;
        }
        long jitter = jitter(state.reactions, mean);
        if ((double) jitter / (double) mean > JITTER_RATIO) {
            return;
        }

        Map<String, Object> evidence = context.newEvidence();
        evidence.put("meanReactionMs", mean);
        evidence.put("jitterMs", jitter);
        evidence.put("samples", state.reactions.size());
        evidence.put("reactions", state.reactions);
        evidence.put("maxReactionMs", MAX_REACTION_MILLIS);
        evidence.put("lastSlot", click.slot());
        evidence.put("damageEvents", context.player().combat().damageEvents());

        context.flag("equipped armour " + mean + "ms after being hit, with only "
                + jitter + "ms of spread across " + state.reactions.size() + " hits", evidence, 7.0);
        state.reactions.clear();
    }

    public static boolean isArmorSlot(int slot) {
        return slot >= FIRST_ARMOR_SLOT && slot <= LAST_ARMOR_SLOT;
    }

    private static long mean(java.util.List<Long> values) {
        long total = 0L;
        for (long value : values) {
            total += value;
        }
        return values.isEmpty() ? 0L : total / values.size();
    }

    private static long jitter(java.util.List<Long> values, long mean) {
        long total = 0L;
        for (long value : values) {
            total += Math.abs(value - mean);
        }
        return values.isEmpty() ? 0L : total / values.size();
    }

    static final class ArmorState {

        private final java.util.ArrayList<Long> reactions = new java.util.ArrayList<>(8);
    }
}
