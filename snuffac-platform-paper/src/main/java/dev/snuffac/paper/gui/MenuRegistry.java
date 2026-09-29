package dev.snuffac.paper.gui;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MenuRegistry {

    private final Map<UUID, Entry> open = new ConcurrentHashMap<>();

    public record Entry(Object menu, Object inventory) {
    }

    public void register(UUID playerId, Object menu, Object inventory) {
        if (menu != null) {
            open.put(playerId, new Entry(menu, inventory));
        }
    }

    public Object menuOf(UUID playerId) {
        Entry entry = open.get(playerId);
        return entry == null ? null : entry.menu();
    }

    public boolean isOpen(UUID playerId) {
        return open.containsKey(playerId);
    }

    public void forget(UUID playerId) {
        open.remove(playerId);
    }

    public void forgetOnClose(UUID playerId, Object closingMenu, Object closingInventory) {
        open.computeIfPresent(playerId, (key, entry) -> {
            if (entry.menu() != closingMenu) {
                return entry;
            }
            if (closingInventory != null && entry.inventory() != closingInventory) {
                return entry;
            }
            return null;
        });
    }

    public int size() {
        return open.size();
    }

    public void clear() {
        open.clear();
    }
}
