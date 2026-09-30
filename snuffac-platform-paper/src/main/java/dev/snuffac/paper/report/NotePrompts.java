package dev.snuffac.paper.report;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * A report with a category but no detail is close to useless to the staff
 * handling it, and the note field already exists on every stored report and
 * has never been reachable from a menu. This collects it in chat, with a
 * timeout so a player who walks away does not get a note typed into an
 * unexpected prompt later.
 */
public final class NotePrompts {

    public static final long MILLIS = 60_000L;
    public static final int MAX_LENGTH = 240;

    private record Prompt(UUID target, String targetName, String category, long expires) {
    }


    private final Map<UUID, Prompt> pending = new ConcurrentHashMap<>();

    public void open(UUID player, UUID target, String targetName, String category) {
        pending.put(player, new Prompt(target, targetName, category, System.currentTimeMillis() + MILLIS));
    }

    public boolean isWaiting(UUID player) {
        Prompt prompt = pending.get(player);
        if (prompt == null) {
            return false;
        }
        if (prompt.expires() < System.currentTimeMillis()) {
            pending.remove(player);
            return false;
        }
        return true;
    }

    public String targetName(UUID player) {
        Prompt prompt = pending.get(player);
        return prompt == null ? null : prompt.targetName();
    }

    public UUID target(UUID player) {
        Prompt prompt = pending.get(player);
        return prompt == null ? null : prompt.target();
    }

    public String category(UUID player) {
        Prompt prompt = pending.get(player);
        return prompt == null ? null : prompt.category();
    }

    public void cancel(UUID player) {
        pending.remove(player);
    }

    public boolean isExpiredWord(String message) {
        String text = (message == null ? "" : message).trim().toLowerCase(Locale.ROOT);
        return text.equals("cancel") || text.equals("abort") || text.equals("n");
    }

    public void sweep() {
        long now = System.currentTimeMillis();
        pending.entrySet().removeIf(entry -> entry.getValue().expires() < now);
    }

}
