package dev.snuffac.core.check.impl.combat;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.api.Vec3d;
import dev.snuffac.core.check.Check;
import dev.snuffac.core.check.CheckContext;
import dev.snuffac.core.combat.EntitySnapshot;
import dev.snuffac.core.combat.HitboxVerifier;
import dev.snuffac.core.combat.ReachResolver;
import dev.snuffac.core.packet.AttackPacket;
import dev.snuffac.core.packet.PacketType;
import dev.snuffac.core.packet.SnuffPacket;
import java.util.Map;
import java.util.Set;

public final class AttackAngleCheck implements Check {

    public static final int MIN_SAMPLES = 4;
    public static final double RAY_MISS_ANGLE = 12.0;
    public static final double RAY_MISS_STREAK = 3;

    @Override
    public Set<PacketType> packetInterests() {
        return Set.of(PacketType.ATTACK);
    }

    @Override
    public String key() {
        return "attackangle";
    }

    @Override
    public String name() {
        return "Attack Angle";
    }

    @Override
    public CheckCategory category() {
        return CheckCategory.COMBAT;
    }

    @Override
    public String description() {
        return "Casts the attack look vector against the true hitbox and rejects hits the crosshair was never on.";
    }

    @Override
    public Object createState() {
        return new AngleState();
    }

    @Override
    public void onPacket(CheckContext context, SnuffPacket packet) {
        if (!(packet instanceof AttackPacket attack)) {
            return;
        }
        var state = (AngleState) state(context.player());
        if (state == null) {
            return;
        }
        var player = context.player();
        var movement = player.movement();
        if (movement.ticksSinceTeleport() <= 1) {
            return;
        }

        var environment = player.combatEnvironment();
        if (!environment.known(attack.targetId())) {
            state.samples = 0;
            state.rayMisses = 0;
            return;
        }
        EntitySnapshot target = environment.byId(attack.targetId());
        if (target == null) {
            return;
        }

        var cache = player.worldCache();
        Vec3d eye = ReachResolver.eyePosition(player.position(), movement.sneaking());
        var box = HitboxVerifier.canonical(target).expanded(ReachResolver.verticalPadding());
        Vec3d aimPoint = box.closestPoint(eye);
        boolean clear = !ReachResolver.segmentBlocked(eye, aimPoint, cache::blocksMovement);

        double tolerance = context.config().reachToleranceFor(context.ping(), movement.inVehicle());
        var result = HitboxVerifier.verify(
                player.position(),
                movement.sneaking(),
                attack.yaw(),
                attack.pitch(),
                target,
                environment.creative(),
                movement.inVehicle(),
                tolerance,
                clear);

        if (!result.withinReach()) {
            return;
        }

        state.samples++;

        if (result.rayHits() && clear) {
            state.rayMisses = 0;
            return;
        }

        if (result.angleDegrees() < RAY_MISS_ANGLE && result.rayHits()) {
            return;
        }

        state.rayMisses++;

        if (state.rayMisses < RAY_MISS_STREAK) {
            return;
        }

        Map<String, Object> evidence = context.newEvidence();
        evidence.put("angleDegrees", HitboxVerifier.round(result.angleDegrees()));
        evidence.put("rayHitsHitbox", result.rayHits());
        evidence.put("lineOfSightClear", clear);
        evidence.put("reach", HitboxVerifier.round(result.reach()));
        evidence.put("maximum", HitboxVerifier.round(result.maximum()));
        evidence.put("rayMissStreak", state.rayMisses);
        evidence.put("samples", state.samples);
        evidence.put("ping", HitboxVerifier.round(context.ping()));
        evidence.put("yaw", attack.yaw());
        evidence.put("pitch", attack.pitch());
        evidence.put("targetType", target.typeName());

        context.preventAttack("attack angle: " + result.reason());
        context.flag("attack landed outside the hitbox the crosshair pointed at: "
                + result.reason(), evidence, 8.0);
        state.rayMisses = 0;
    }

    static final class AngleState {

        private int samples;
        private int rayMisses;
    }
}
