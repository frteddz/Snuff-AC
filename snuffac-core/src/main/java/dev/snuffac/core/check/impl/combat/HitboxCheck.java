package dev.snuffac.core.check.impl.combat;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.api.Vec3d;
import dev.snuffac.core.check.Check;
import dev.snuffac.core.check.CheckContext;
import dev.snuffac.core.combat.HitboxVerifier;
import dev.snuffac.core.combat.ReachResolver;
import dev.snuffac.core.packet.AttackPacket;
import dev.snuffac.core.packet.MovementPacket;
import dev.snuffac.core.packet.PacketType;
import dev.snuffac.core.packet.SnuffPacket;
import dev.snuffac.core.util.AxisAlignedBox;
import dev.snuffac.core.util.BlockPos;
import java.util.Map;
import java.util.Set;

public final class HitboxCheck implements Check {

    public static final double PLAYER_BOX = 0.6;
    public static final double EXPANSION_STEP = 0.05;
    public static final double MIN_EXPANSION = 0.15;
    public static final double MIN_ANGLE_MARGIN = 2.0;
    public static final int REQUIRED_HITS = 4;

    @Override
    public Set<PacketType> packetInterests() {
        return Set.of(PacketType.ATTACK, PacketType.MOVEMENT);
    }

    @Override
    public String key() {
        return "hitbox";
    }

    @Override
    public String name() {
        return "Hitbox";
    }

    @Override
    public CheckCategory category() {
        return CheckCategory.COMBAT;
    }

    @Override
    public String description() {
        return "Raytraces every attack against a vanilla sized hitbox and reports a run of hits "
                + "that only landed because the client used a larger box.";
    }

    @Override
    public Object createState() {
        return new HitboxState();
    }

    @Override
    public void onPacket(CheckContext context, SnuffPacket packet) {
        var state = (HitboxState) state(context.player());
        if (state == null) {
            return;
        }

        if (packet instanceof MovementPacket) {
            return;
        }
        if (!(packet instanceof AttackPacket attack)) {
            return;
        }

        var player = context.player();
        var movement = player.movement();
        if (movement.ticksSinceTeleport() <= 1 || movement.inVehicle()) {
            state.reset();
            return;
        }

        var environment = player.combatEnvironment();
        var target = environment.byId(attack.targetId());
        if (target == null || !environment.known(attack.targetId())) {
            state.reset();
            return;
        }

        Vec3d eye = ReachResolver.eyePosition(player.position(), movement.sneaking());
        Vec3d direction = facingDirection(attack.yaw(), attack.pitch());
        AxisAlignedBox real = vanillaBox(target, movement.sneaking());
        if (!HitboxVerifier.intersects(real, eye, direction)) {
            state.misses++;
            if (state.misses >= REQUIRED_HITS) {
                state.reset();
            }
            return;
        }
        state.misses = 0;

        double expansion = smallestExpansion(real, eye, direction);
        if (expansion < MIN_EXPANSION) {
            state.expansions.clear();
            state.hits = 0;
            return;
        }

        double angle = HitboxVerifier.angleToCenterDegrees(eye, attack.yaw(), attack.pitch(),
                target.center());
        if (angle < MIN_ANGLE_MARGIN) {
            state.expansions.clear();
            state.hits = 0;
            return;
        }

        state.expansions.add(expansion);
        state.hits++;
        if (state.hits < REQUIRED_HITS) {
            return;
        }

        double average = mean(state.expansions);
        double furthest = state.expansions.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        double targetAngle = angle;

        Map<String, Object> evidence = context.newEvidence();
        evidence.put("mode", "expanded hitbox");
        evidence.put("averageExpansion", round(average));
        evidence.put("furthestExpansion", round(furthest));
        evidence.put("minExpansion", MIN_EXPANSION);
        evidence.put("hits", state.hits);
        evidence.put("targetId", attack.targetId());
        evidence.put("targetDistance", round(player.position().distanceTo(target.position())));
        evidence.put("angleToCentre", round(targetAngle));
        evidence.put("boxWidth", PLAYER_BOX);
        evidence.put("sneaking", movement.sneaking());

        context.preventAttack("hit landed outside the vanilla hitbox by " + round(average));
        context.flag("hits landed only on an expanded hitbox, average " + round(average)
                + " blocks outside the real box", evidence, 8.0);
        state.reset();
    }

    public static AxisAlignedBox vanillaBox(
            dev.snuffac.core.combat.EntitySnapshot target, boolean sneaking) {
        double height = sneaking ? HitboxVerifier.SNEAK_HEIGHT : HitboxVerifier.PLAYER_HEIGHT;
        return new AxisAlignedBox(
                target.position().x() - PLAYER_BOX / 2.0,
                target.position().y(),
                target.position().z() - PLAYER_BOX / 2.0,
                target.position().x() + PLAYER_BOX / 2.0,
                target.position().y() + height,
                target.position().z() + PLAYER_BOX / 2.0);
    }

    public static Vec3d facingDirection(float yaw, float pitch) {
        double yawRadians = Math.toRadians(yaw);
        double pitchRadians = Math.toRadians(pitch);
        return new Vec3d(
                -Math.sin(yawRadians) * Math.cos(pitchRadians),
                -Math.sin(pitchRadians),
                Math.cos(yawRadians) * Math.cos(pitchRadians));
    }

    public static double smallestExpansion(AxisAlignedBox real, Vec3d eye, Vec3d direction) {
        for (double step = 0.0; step <= 1.0; step += EXPANSION_STEP) {
            if (HitboxVerifier.intersects(real.expanded(step), eye, direction)) {
                return step;
            }
        }
        return 1.0;
    }

    private static double mean(java.util.List<Double> values) {
        double total = 0.0;
        for (double value : values) {
            total += value;
        }
        return values.isEmpty() ? 0.0 : total / values.size();
    }

    private static double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    static final class HitboxState {

        private final java.util.List<Double> expansions = new java.util.ArrayList<>(REQUIRED_HITS);
        private int hits;
        private int misses;

        private void reset() {
            expansions.clear();
            hits = 0;
            misses = 0;
        }
    }
}
