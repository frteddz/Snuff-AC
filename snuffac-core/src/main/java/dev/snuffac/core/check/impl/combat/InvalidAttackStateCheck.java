package dev.snuffac.core.check.impl.combat;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.core.check.Check;
import dev.snuffac.core.check.CheckContext;
import dev.snuffac.core.packet.AttackPacket;
import dev.snuffac.core.packet.PacketType;
import dev.snuffac.core.packet.SnuffPacket;
import java.util.Map;
import java.util.Set;

public final class InvalidAttackStateCheck implements Check {

    private static final double MAX_ATTACK_DISTANCE = 8.0;
    private static final double MIN_CURSOR_COORDINATE = -8.0E6;
    private static final double MAX_CURSOR_COORDINATE = 8.0E6;

    @Override
    public Set<PacketType> packetInterests() {
        return Set.of(PacketType.ATTACK);
    }

    @Override
    public String key() {
        return "invalidattackstate";
    }

    @Override
    public String name() {
        return "InvalidAttackState";
    }

    @Override
    public CheckCategory category() {
        return CheckCategory.COMBAT;
    }

    @Override
    public String description() {
        return "Rejects attacks with impossible cursor coordinates, entity identifiers or world distance.";
    }

    @Override
    public void onPacket(CheckContext context, SnuffPacket packet) {
        if (!(packet instanceof AttackPacket attack)) {
            return;
        }

        var cursor = attack.cursorPosition();

        if (attack.targetId() <= 0) {
            flag(context, "invalid entity id", Map.of("targetId", attack.targetId()));
            return;
        }
        if (!valid(cursor.x()) || !valid(cursor.y()) || !valid(cursor.z())) {
            flag(context, "non finite cursor position", Map.of(
                    "x", cursor.x(), "y", cursor.y(), "z", cursor.z()));
            return;
        }
        if (Math.abs(cursor.x()) > MAX_CURSOR_COORDINATE
                || Math.abs(cursor.y()) > MAX_CURSOR_COORDINATE
                || Math.abs(cursor.z()) > MAX_CURSOR_COORDINATE) {
            flag(context, "cursor position outside world", Map.of(
                    "x", cursor.x(), "y", cursor.y(), "z", cursor.z()));
            return;
        }
        if (!valid(attack.yaw()) || !valid(attack.pitch())) {
            flag(context, "invalid attack rotation", Map.of(
                    "yaw", attack.yaw(), "pitch", attack.pitch()));
            return;
        }

        double distance = context.player().position().distanceTo(cursor);
        if (distance > MAX_ATTACK_DISTANCE) {
            flag(context, "attack cursor " + Math.round(distance * 100.0) / 100.0 + " blocks away", Map.of(
                    "distance", distance,
                    "maximum", MAX_ATTACK_DISTANCE));
        }
    }

    private static void flag(CheckContext context, String reason, Map<String, Object> extra) {
        var evidence = context.newEvidence();
        evidence.put("reason", reason);
        evidence.putAll(extra);
        context.flagImmediately(reason, evidence);
    }

    private static boolean valid(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }
}
