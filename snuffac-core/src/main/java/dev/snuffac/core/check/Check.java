package dev.snuffac.core.check;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.core.packet.PacketType;
import dev.snuffac.core.packet.SnuffPacket;
import dev.snuffac.core.player.PlayerData;
import java.util.Set;

public interface Check {

    String key();

    default String name() {
        return key();
    }

    CheckCategory category();

    default String description() {
        return "";
    }

    default Set<PacketType> packetInterests() {
        return Set.of();
    }

    default Object createState() {
        return null;
    }

    default boolean requiresMovementState() {
        return true;
    }

    default void onEnable(CheckRegistry registry) {
    }

    default void onPlayerJoin(CheckContext context) {
    }

    default Object state(PlayerData player) {
        return player.checkData(key());
    }

    default void onPlayerQuit(CheckContext context) {
    }

    default void onTick(CheckContext context) {
    }

    default void onPacket(CheckContext context, SnuffPacket packet) {
    }
}
