package dev.snuffac.paper;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SnuffSounds {

    private static final Set<UUID> MUTED = ConcurrentHashMap.newKeySet();
    private static volatile boolean enabled = true;

    private SnuffSounds() {
    }

    public static void enabled(boolean value) {
        enabled = value;
    }

    public static boolean enabled() {
        return enabled;
    }

    public static boolean isMuted(UUID playerId) {
        return MUTED.contains(playerId);
    }

    public static boolean toggle(UUID playerId) {
        if (!MUTED.remove(playerId)) {
            MUTED.add(playerId);
            return false;
        }
        return true;
    }

    public static void clear() {
        MUTED.clear();
    }
}
