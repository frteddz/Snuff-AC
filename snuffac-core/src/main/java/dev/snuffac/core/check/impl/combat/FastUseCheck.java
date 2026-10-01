package dev.snuffac.core.check.impl.combat;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.core.check.Check;
import dev.snuffac.core.check.CheckContext;
import dev.snuffac.core.packet.BlockPlacePacket;
import dev.snuffac.core.packet.MovementPacket;
import dev.snuffac.core.packet.PacketType;
import dev.snuffac.core.packet.SnuffPacket;
import java.util.Map;
import java.util.Set;

public final class FastUseCheck implements Check {

    public static final int VANILLA_FOOD_TICKS = 32;
    public static final int MIN_TICKS = 2;
    public static final int REQUIRED_RESTARTS = 3;
    public static final int EXPIRY_TICKS = 45;

    @Override
    public Set<PacketType> packetInterests() {
        return Set.of(PacketType.BLOCK_PLACE, PacketType.MOVEMENT);
    }

    @Override
    public String key() {
        return "fastuse";
    }

    @Override
    public String name() {
        return "FastUse";
    }

    @Override
    public CheckCategory category() {
        return CheckCategory.COMBAT;
    }

    @Override
    public String description() {
        return "Enforces the vanilla use duration before a consumable takes effect, catching "
                + "food, drink and bow use that completes early.";
    }

    @Override
    public Object createState() {
        return new UseState();
    }

    @Override
    public void onPacket(CheckContext context, SnuffPacket packet) {
        var state = (UseState) state(context.player());
        if (state == null) {
            return;
        }

        if (packet instanceof MovementPacket) {
            if (state.expiry > 0) {
                state.expiry--;
            }
            return;
        }
        if (!(packet instanceof BlockPlacePacket)) {
            return;
        }

        int ticksUsed = context.player().movement().ticksUsingItem();
        if (ticksUsed < MIN_TICKS || ticksUsed >= VANILLA_FOOD_TICKS) {
            return;
        }

        state.early++;
        state.expiry = EXPIRY_TICKS;
        if (state.early < REQUIRED_RESTARTS) {
            return;
        }

        Map<String, Object> evidence = context.newEvidence();
        evidence.put("restarts", state.early);
        evidence.put("ticksUsed", ticksUsed);
        evidence.put("vanillaTicks", VANILLA_FOOD_TICKS);
        evidence.put("shortfall", VANILLA_FOOD_TICKS - ticksUsed);
        evidence.put("onGround", context.player().movement().onGround());
        evidence.put("sprinting", context.player().movement().sprinting());
        evidence.put("hand", ((BlockPlacePacket) packet).handOrdinal());

        context.preventInteraction("item used after only " + ticksUsed + " of "
                + VANILLA_FOOD_TICKS + " ticks");
        context.flag("restarted an item use after " + ticksUsed + " ticks, vanilla needs "
                + VANILLA_FOOD_TICKS, evidence, 8.0);
        state.early = 0;
    }

    static final class UseState {

        private int early;
        private int expiry;
    }
}
