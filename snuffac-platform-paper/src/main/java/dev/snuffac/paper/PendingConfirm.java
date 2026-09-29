package dev.snuffac.paper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public final class PendingConfirm {

    public static final long TIMEOUT_MILLIS = 20_000L;

    public record Entry(
            UUID requester,
            String action,
            UUID targetId,
            String targetName,
            String description,
            long createdMillis) {
    }

    private final java.util.Map<UUID, Entry> pending = new java.util.concurrent.ConcurrentHashMap<>();

    public Entry open(UUID requester, String action, UUID targetId, String targetName,
            String description) {
        purge();
        Entry entry = new Entry(requester, action, targetId, targetName, description,
                System.currentTimeMillis());
        pending.put(requester, entry);
        return entry;
    }

    public Entry pending(UUID requester) {
        purge();
        return pending.get(requester);
    }

    public void clear(UUID requester) {
        pending.remove(requester);
    }

    public int size() {
        purge();
        return pending.size();
    }

    private void purge() {
        long now = System.currentTimeMillis();
        for (var entry : new ArrayList<>(pending.values())) {
            if (now - entry.createdMillis() > TIMEOUT_MILLIS) {
                pending.remove(entry.requester());
            }
        }
    }

    public static UUID resolve(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online.getUniqueId();
        }
            OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        if (offline != null && (offline.hasPlayedBefore() || offline.isOnline())) {
            return offline.getUniqueId();
        }
        return null;
    }

    public static String nameOf(UUID id, String fallback) {
        Player online = id == null ? null : Bukkit.getPlayer(id);
        if (online != null) {
            return online.getName();
        }
        return fallback;
    }

    public static List<String> expiredWords() {
        return List.of("cancel", "no", "nevermind");
    }
}
