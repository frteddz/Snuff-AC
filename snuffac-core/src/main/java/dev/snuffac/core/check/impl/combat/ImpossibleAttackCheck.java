package dev.snuffac.core.check.impl.combat;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.core.check.Check;
import dev.snuffac.core.check.CheckContext;
import dev.snuffac.core.packet.AttackPacket;
import dev.snuffac.core.packet.PacketType;
import dev.snuffac.core.packet.SnuffPacket;
import java.util.Map;
import java.util.Set;

public final class ImpossibleAttackCheck implements Check {

    private static final int MAX_ATTACKS_PER_SECOND = 22;
    private static final int MIN_ATTACKS_PER_TICK = 1;
    private static final long WINDOW_MILLIS = 1000L;

    @Override
    public Set<PacketType> packetInterests() {
        return Set.of(PacketType.ATTACK);
    }

    @Override
    public String key() {
        return "impossibleattack";
    }

    @Override
    public String name() {
        return "ImpossibleAttack";
    }

    @Override
    public CheckCategory category() {
        return CheckCategory.COMBAT;
    }

    @Override
    public String description() {
        return "Rejects attack packets that exceed the client attack rate or repeat the same tick illegally.";
    }

    @Override
    public Object createState() {
        return new AttackState();
    }

    @Override
    public void onPacket(CheckContext context, SnuffPacket packet) {
        if (!(packet instanceof AttackPacket)) {
            return;
        }
        var state = (AttackState) state(context.player());
        if (state == null) {
            return;
        }

        long now = System.currentTimeMillis();
        if (state.lastAttackMillis > 0L && now - state.lastAttackMillis < 1L) {
            state.duplicateTicks++;
        } else {
            state.duplicateTicks = 0;
        }
        state.lastAttackMillis = now;

        int perSecond = context.player().combat().attacksInWindow(now, WINDOW_MILLIS);
        if (perSecond > MAX_ATTACKS_PER_SECOND) {
            Map<String, Object> evidence = context.newEvidence();
            evidence.put("attacksPerSecond", perSecond);
            evidence.put("maximum", MAX_ATTACKS_PER_SECOND);
            evidence.put("duplicateTicks", state.duplicateTicks);
            context.flag("attack rate of " + perSecond + " per second", evidence, 8.0);
        }
        if (state.duplicateTicks > MIN_ATTACKS_PER_TICK) {
            Map<String, Object> evidence = context.newEvidence();
            evidence.put("duplicateTicks", state.duplicateTicks);
            evidence.put("ping", round(context.ping()));
            context.flag("multiple attack packets in a single millisecond", evidence, 6.0);
            state.duplicateTicks = 0;
        }
    }

    @Override
    public void onTick(CheckContext context) {
        var state = (AttackState) state(context.player());
        if (state != null) {
            state.duplicateTicks = 0;
        }
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    static final class AttackState {

        private long lastAttackMillis;
        private int duplicateTicks;
    }
}
