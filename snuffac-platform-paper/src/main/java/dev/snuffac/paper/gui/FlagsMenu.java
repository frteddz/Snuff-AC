package dev.snuffac.paper.gui;

import dev.snuffac.api.violation.ViolationInfo;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public final class FlagsMenu extends SnuffMenu {

    public static final String ACTION_BACK = "back";
    public static final String ACTION_REFRESH = "refresh";
    public static final String ACTION_NEXT = "page_next";
    public static final String ACTION_PREV = "page_prev";
    public static final String ACTION_CASE = "case";

    private static final int CONTENT = 45;
    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final FlagsBridge bridge;
    private final int page;

    public FlagsMenu(Plugin plugin, FlagsBridge bridge, int page) {
        super(plugin, 54, "Flagged Players");
        this.bridge = bridge;
        this.page = page;
    }

    @Override
    protected void render() {
        List<Entry> rows = bridge.entries();
        int pages = Math.max(1, (int) Math.ceil(rows.size() / (double) CONTENT));
        int start = page * CONTENT;
        for (int i = 0; i < CONTENT && start + i < rows.size(); i++) {
            renderEntry(start + i, rows.get(start + i));
        }
        set(45, Material.ARROW, "<white>Previous",
                List.of("<gray>Page " + (page + 1) + " of " + pages), ACTION_PREV);
        set(49, Material.KNOWLEDGE_BOOK, "<white>Refresh", List.of("<gray>Reload the list"), ACTION_REFRESH);
        set(53, Material.ARROW, "<white>Next",
                List.of("<gray>Page " + (page + 1) + " of " + pages), ACTION_NEXT);
        set(45 + 3, Material.OAK_DOOR, "<white>Back", List.of("<gray>Return to the menu"), ACTION_BACK);
    }

    private void renderEntry(int slot, Entry entry) {
        List<String> lore = new ArrayList<>();
        lore.add("<gray>Status: <white>" + (entry.online() ? "online" : "offline"));
        lore.add("<gray>Flags: <white>" + entry.total());
        if (!entry.topChecks().isEmpty()) {
            lore.add("<gray>Top: <white>" + String.join(", ", entry.topChecks()));
        }
        if (entry.lastFlagMillis() > 0L) {
            lore.add("<gray>Last flag: <white>" + STAMP.format(Instant.ofEpochMilli(entry.lastFlagMillis())));
        }
        lore.add("<gray>Left click to see their history");

        ItemStack stack = new ItemStack(Material.PLAYER_HEAD);
        var meta = (SkullMeta) stack.getItemMeta();
        if (meta != null) {
            meta.displayName(component("<white>" + entry.name()));
            List<Component> rendered = new ArrayList<>(lore.size());
            for (String line : lore) {
                rendered.add(component(line));
            }
            meta.lore(rendered);
            Player live = Bukkit.getPlayer(entry.uuid());
            if (live != null) {
                meta.setOwningPlayer(live);
            } else {
                OfflinePlayer offline = Bukkit.getOfflinePlayer(entry.uuid());
                if (offline.getName() != null) {
                    meta.setOwningPlayer(offline);
                }
            }
            meta.getPersistentDataContainer().set(
                    new org.bukkit.NamespacedKey(plugin, "action"),
                    PersistentDataType.STRING, ACTION_CASE);
            meta.getPersistentDataContainer().set(
                    new org.bukkit.NamespacedKey(plugin, "target"),
                    PersistentDataType.STRING, entry.uuid().toString());
            stack.setItemMeta(meta);
        }
        getInventory().setItem(slot, stack);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        String action = actionOf(event.getInventory(), event.getRawSlot());
        if (action == null) {
            return;
        }
        switch (action) {
            case ACTION_BACK -> {
                if (parent() != null) {
                    player.openInventory(parent().getInventory());
                } else {
                    close(player);
                }
            }
            case ACTION_REFRESH -> {
                build();
                player.updateInventory();
            }
            case ACTION_NEXT -> bridge.page(player, this, page + 1);
            case ACTION_PREV -> {
                if (page > 0) {
                    bridge.page(player, this, page - 1);
                }
            }
            case ACTION_CASE -> {
                String raw = targetOf(event.getInventory(), event.getRawSlot());
                if (raw == null) {
                    return;
                }
                try {
                    bridge.showCase(player, UUID.fromString(raw));
                } catch (IllegalArgumentException malformed) {
                    return;
                }
            }
            default -> {
            }
        }
    }

    public String targetOf(org.bukkit.inventory.Inventory inventory, int slot) {
        if (slot < 0 || slot >= inventory.getSize()) {
            return null;
        }
        ItemStack item = inventory.getItem(slot);
        if (item == null || item.getItemMeta() == null) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(
                new org.bukkit.NamespacedKey(plugin, "target"), PersistentDataType.STRING);
    }

    public record Entry(
            UUID uuid,
            String name,
            boolean online,
            int total,
            List<String> topChecks,
            long lastFlagMillis) {
    }

    public record Summary(List<String> topChecks, long lastFlagMillis) {
    }

    public static Summary summarise(List<ViolationInfo> records) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        long latest = 0L;
        for (ViolationInfo info : records) {
            counts.merge(info.checkName(), 1, Integer::sum);
            latest = Math.max(latest, info.timestampMillis());
        }
        List<Map.Entry<String, Integer>> ordered = new ArrayList<>(counts.entrySet());
        ordered.sort(Comparator.comparingInt((Map.Entry<String, Integer> e) -> -e.getValue()));
        List<String> names = new ArrayList<>();
        for (int i = 0; i < Math.min(3, ordered.size()); i++) {
            names.add(ordered.get(i).getKey() + " x" + ordered.get(i).getValue());
        }
        return new Summary(names, latest);
    }

    public interface FlagsBridge {

        List<Entry> entries();

        void showCase(Player player, UUID id);

        void page(Player player, SnuffMenu current, int page);
    }
}
