package dev.snuffac.core.check.impl.net;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.core.check.Check;
import dev.snuffac.core.check.CheckContext;
import dev.snuffac.core.packet.MovementPacket;
import dev.snuffac.core.packet.PacketType;
import dev.snuffac.core.packet.SnuffPacket;
import dev.snuffac.core.util.BlockPos;
import java.util.Map;
import java.util.Set;

public final class BadPacketsCheck implements Check {

    public static final double WORLD_BORDER = 29_999_984.0;
    public static final int MIN_Y = -2_048;
    public static final int MAX_Y = 20_000;
    public static final int VANILLA_MAX_Y = 320;
    public static final int VANILLA_MIN_Y = -64;

    @Override
    public Set<PacketType> packetInterests() {
        return Set.of(PacketType.MOVEMENT, PacketType.ATTACK, PacketType.BLOCK_BREAK, PacketType.BLOCK_PLACE);
    }

    @Override
    public String key() {
        return "badpackets";
    }

    @Override
    public String name() {
        return "BadPackets";
    }

    @Override
    public CheckCategory category() {
        return CheckCategory.PACKET;
    }

    @Override
    public String description() {
        return "Rejects structurally invalid packets including impossible coordinates, rotations and block positions.";
    }

    @Override
    public void onPacket(CheckContext context, SnuffPacket packet) {
        if (packet instanceof MovementPacket movement) {
            validateMovement(context, movement);
        } else if (packet instanceof dev.snuffac.core.packet.AttackPacket attack) {
            validateAttack(context, attack);
        } else if (packet instanceof dev.snuffac.core.packet.BlockBreakPacket dig) {
            validateBlockPosition(context, dig.packedPosition(), "digging");
        } else if (packet instanceof dev.snuffac.core.packet.BlockPlacePacket place) {
            validateBlockPosition(context, place.packedPosition(), "placement");
        }
    }

    private static void validateMovement(CheckContext context, MovementPacket packet) {
        if (!packet.positionChanged()) {
            return;
        }
        var position = packet.position();
        if (!finite(position.x()) || !finite(position.y()) || !finite(position.z())) {
            flag(context, "non finite movement coordinates", Map.of(
                    "x", position.x(), "y", position.y(), "z", position.z()));
            return;
        }
        if (Math.abs(position.x()) > WORLD_BORDER || Math.abs(position.z()) > WORLD_BORDER) {
            flag(context, "movement outside world border", Map.of(
                    "x", position.x(), "z", position.z()));
            return;
        }
        if (position.y() < MIN_Y || position.y() > MAX_Y) {
            flag(context, "movement outside build limits", Map.of(
                    "y", position.y(), "min", MIN_Y, "max", MAX_Y));
            return;
        }
        if (!finite(packet.yaw()) || !finite(packet.pitch())) {
            flag(context, "non finite rotation", Map.of("yaw", packet.yaw(), "pitch", packet.pitch()));
        }
    }

    private static void validateAttack(CheckContext context, dev.snuffac.core.packet.AttackPacket packet) {
        var cursor = packet.cursorPosition();
        if (!finite(cursor.x()) || !finite(cursor.y()) || !finite(cursor.z())) {
            flag(context, "non finite attack cursor", Map.of(
                    "x", cursor.x(), "y", cursor.y(), "z", cursor.z()));
            return;
        }
        if (Math.abs(cursor.x()) > WORLD_BORDER || Math.abs(cursor.z()) > WORLD_BORDER) {
            flag(context, "attack cursor outside world border", Map.of(
                    "x", cursor.x(), "z", cursor.z()));
        }
    }

    private static void validateBlockPosition(CheckContext context, long packed, String action) {
        BlockPos position = BlockPos.unpack(packed);
        if (Math.abs((long) position.x()) > WORLD_BORDER || Math.abs((long) position.z()) > WORLD_BORDER) {
            flag(context, action + " position outside world border", Map.of(
                    "x", position.x(), "y", position.y(), "z", position.z()));
            return;
        }
        if (position.y() < MIN_Y || position.y() > MAX_Y) {
            flag(context, action + " position outside build limits", Map.of(
                    "x", position.x(), "y", position.y(), "z", position.z()));
        }
    }

    private static void flag(CheckContext context, String reason, Map<String, Object> extra) {
        var evidence = context.newEvidence();
        evidence.put("reason", reason);
        evidence.putAll(extra);
        if (structurallyImpossible(reason)) {
            context.flagImmediately(reason, evidence);
            return;
        }
        context.flag(reason, evidence, 9.0);
    }

    private static boolean structurallyImpossible(String reason) {
        return reason.contains("non finite");
    }

    private static boolean finite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }
}
