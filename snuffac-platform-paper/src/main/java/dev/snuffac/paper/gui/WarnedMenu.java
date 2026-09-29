package dev.snuffac.paper.gui;

import dev.snuffac.core.punish.Durations;
import dev.snuffac.core.punish.PunishmentService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public final class WarnedMenu extends SnuffMenu {

    public static final String ACTION_BACK = "back";
    public static final String ACTION_CLOSE = "close";

    private final dev.snuffac.paper.SnuffPaperPlugin plugin;

    public WarnedMenu(Plugin plugin) {
        super(plugin, 54, "Warned Players");
        this.plugin = (dev.snuffac.paper.SnuffPaperPlugin) plugin;
    }

    @Override
    protected void render() {
        List<Row> rows = collect();
        int limit = Math.min(rows.size(), 45);
        for (int i = 0; i < limit; i++) {
            Row row = rows.get(i);
            List<String> lore = new ArrayList<>();
            lore.add("<gray>Warnings: <white>" + row.count());
            if (!row.latest().isBlank()) {
                lore.add("<gray>Latest: <white>" + row.latest());
            }
            lore.add("<gray>Left click to see the full list");
            var stack = new ItemStack(Material.PLAYER_HEAD);
            var meta = (SkullMeta) stack.getItemMeta();
            if (meta != null) {
                meta.displayName(component("<white>" + row.name()));
                var rendered = new ArrayList<net.kyori.adventure.text.Component>();
                for (String line : lore) {
                    rendered.add(component(line));
                }
                meta.lore(rendered);
                Player live = Bukkit.getPlayer(row.uuid());
                if (live != null) {
                    meta.setOwningPlayer(live);
                } else {
                    OfflinePlayer offline = Bukkit.getOfflinePlayer(row.uuid());
                    if (offline.getName() != null) {
                        meta.setOwningPlayer(offline);
                    }
                }
                meta.getPersistentDataContainer().set(
                        new org.bukkit.NamespacedKey(plugin, "action"),
                        PersistentDataType.STRING, MainMenu.ACTION_SUSPICIOUS);
                meta.getPersistentDataContainer().set(
                        new org.bukkit.NamespacedKey(plugin, "target"),
                        PersistentDataType.STRING, row.uuid().toString());
                stack.setItemMeta(meta);
            }
            getInventory().setItem(i, stack);
        }
        set(45, Material.OAK_DOOR, "<white>Back", List.of(), ACTION_BACK);
        set(49, Material.BARRIER, "<white>Close", List.of(), ACTION_CLOSE);
    }

    private List<Row> collect() {
        List<Row> rows = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            int count = countWarnings(online.getUniqueId());
            if (count > 0) {
                rows.add(new Row(online.getUniqueId(), online.getName(), count, latestWarning(online.getUniqueId())));
            }
        }
        rows.sort(Comparator.comparingInt(Row::count).reversed());
        return rows;
    }

    private int countWarnings(java.util.UUID id) {
        int count = 0;
        for (PunishmentService.Punishment record : plugin.punishments().history(id)) {
            if (record.kind() == PunishmentService.Kind.WARN) {
                count++;
            }
        }
        return count;
    }

    private String latestWarning(java.util.UUID id) {
        String latest = "";
        long newest = 0L;
        for (PunishmentService.Punishment record : plugin.punishments().history(id)) {
            if (record.kind() == PunishmentService.Kind.WARN && record.createdMillis() > newest) {
                newest = record.createdMillis();
                latest = record.reason();
            }
        }
        return latest;
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
        if (action.equals(ACTION_BACK)) {
            if (parent() != null) {
                player.openInventory(parent().getInventory());
            } else {
                close(player);
            }
            return;
        }
        if (action.equals(ACTION_CLOSE)) {
            close(player);
            return;
        }
        String target = targetOf(event.getInventory(), event.getRawSlot());
        if (target == null) {
            return;
        }
        try {
            java.util.UUID id = java.util.UUID.fromString(target);
            StringBuilder builder = new StringBuilder("[Snuff] Warnings for ")
                    .append(Bukkit.getOfflinePlayer(id).getName())
                    .append(':');
            player.sendMessage(net.kyori.adventure.text.Component.text(builder.toString()));
            for (PunishmentService.Punishment record : plugin.punishments().history(id)) {
                if (record.kind() == PunishmentService.Kind.WARN) {
                    player.sendMessage(net.kyori.adventure.text.Component.text("  " + record.reason()
                            + " | by " + record.staff()
                            + " | " + Durations.describe(System.currentTimeMillis() - record.createdMillis())
                            + " ago"));
                }
            }
        } catch (IllegalArgumentException malformed) {
            return;
        }
    }

    private String targetOf(org.bukkit.inventory.Inventory inventory, int slot) {
        if (slot < 0 || slot >= inventory.getSize()) {
            return null;
        }
        var item = inventory.getItem(slot);
        if (item == null || item.getItemMeta() == null) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(
                new org.bukkit.NamespacedKey(plugin, "target"), PersistentDataType.STRING);
    }

    private record Row(java.util.UUID uuid, String name, int count, String latest) {
    }
}
