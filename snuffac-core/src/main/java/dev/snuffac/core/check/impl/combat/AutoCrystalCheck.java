package dev.snuffac.core.check.impl.combat;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.core.check.Check;
import dev.snuffac.core.check.CheckContext;
import dev.snuffac.core.combat.EntitySnapshot;
import dev.snuffac.core.packet.AttackPacket;
import dev.snuffac.core.packet.MovementPacket;
import dev.snuffac.core.packet.PacketType;
import dev.snuffac.core.packet.SnuffPacket;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Set;

public final class AutoCrystalCheck implements Check {

    public static final long MAX_REACTION_MILLIS = 220L;
    public static final long MIN_REACTION_MILLIS = 1L;
    public static final int REQUIRED = 5;
    private static final String[] TARGETS = {"END_CRYSTAL"};

    @Override
    public Set<PacketType> packetInterests() {
        return Set.of(PacketType.ATTACK, PacketType.MOVEMENT);
    }

    @Override
    public String key() {
        return "autocrystal";
    }

    @Override
    public String name() {
        return "AutoCrystal";
    }

    @Override
    public CheckCategory category() {
        return CheckCategory.COMBAT;
    }

    @Override
    public String description() {
        return "Detects attacking end crystals on a fixed reaction time, which is the AutoCrystal cheat.";
    }

    @Override
    public Object createState() {
        return new AuraState();
    }

    public static boolean isTarget(String typeName) {
        if (typeName == null) {
            return false;
        }
        for (String target : TARGETS) {
            if (typeName.equals(target)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onPacket(CheckContext context, SnuffPacket packet) {
        var state = (AuraState) state(context.player());
        if (state == null) {
            return;
        }

        if (packet instanceof MovementPacket movement) {
            if (movement.positionChanged()) {
                state.lastMoveNanos = packet.arrivalNanos();
            }
            return;
        }
        if (!(packet instanceof AttackPacket attack)) {
            return;
        }

        var player = context.player();
        if (player.movement().ticksSinceTeleport() <= 2 || player.movement().inVehicle()) {
            state.reset();
            return;
        }

        var environment = player.combatEnvironment();
        EntitySnapshot target = environment.byId(attack.targetId());
        if (target == null || !environment.known(attack.targetId()) || !isTarget(target.typeName())) {
            return;
        }

        long reaction = reactionNanos(state, attack);
        if (reaction < MIN_REACTION_MILLIS * 1_000_000L
                || reaction > MAX_REACTION_MILLIS * 1_000_000L) {
            return;
        }

        state.gaps.add(reaction);
        state.targets.add(attack.targetId());
        if (state.gaps.size() > ReactionTiming.WINDOW) {
            state.gaps.removeFirst();
            state.targets.removeFirst();
        }

        ReactionTiming.Verdict verdict = ReactionTiming.judge(
                state.gaps, REQUIRED, MAX_REACTION_MILLIS);
        if (!verdict.flagged()) {
            return;
        }

        Map<String, Object> evidence = context.newEvidence();
        evidence.put("meanReactionMs", Math.round(verdict.mean() / 10000.0) / 100.0);
        evidence.put("jitterMs", Math.round(verdict.jitter() / 10000.0) / 100.0);
        evidence.put("samples", verdict.samples());
        evidence.put("distinctTargets", state.targets.stream().mapToInt(Integer::intValue).distinct().count());
        evidence.put("targetType", target.typeName());
        evidence.put("lastTargetId", attack.targetId());
        evidence.put("distance", Math.round(player.position().distanceTo(target.position()) * 100.0) / 100.0);
        evidence.put("maxReactionMs", MAX_REACTION_MILLIS);

        context.flag("attacked " + target.typeName() + " targets " + verdict.mean()
                + "ms apart with only " + verdict.jitter() + "ms of spread across "
                + verdict.samples() + " swings", evidence, 7.0);
        state.gaps.clear();
        state.targets.clear();
    }

    private static long reactionNanos(AuraState state, AttackPacket attack) {
        if (state.lastAttackNanos == 0L) {
            state.lastAttackNanos = attack.arrivalNanos();
            return MAX_REACTION_MILLIS * 1_000_000L + 1L;
        }
        long gap = attack.arrivalNanos() - state.lastAttackNanos;
        state.lastAttackNanos = attack.arrivalNanos();
        if (state.lastMoveNanos > 0L && attack.arrivalNanos() - state.lastMoveNanos < gap) {
            gap = attack.arrivalNanos() - state.lastMoveNanos;
        }
        return gap / 1_000_000L;
    }

    static final class AuraState {

        private final ArrayDeque<Long> gaps = new ArrayDeque<>(ReactionTiming.WINDOW);
        private final ArrayDeque<Integer> targets = new ArrayDeque<>(ReactionTiming.WINDOW);
        private long lastAttackNanos;
        private long lastMoveNanos;

        private void reset() {
            gaps.clear();
            targets.clear();
            lastAttackNanos = 0L;
            lastMoveNanos = 0L;
        }
    }
}
