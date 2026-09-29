package dev.snuffac.paper.gui;

import dev.snuffac.paper.SnuffPaperPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

public final class MenuListener implements Listener {

    private final SnuffPaperPlugin plugin;

    public MenuListener(SnuffPaperPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        SnuffMenu menu = SnuffMenu.of(player);
        if (menu == null) {
            return;
        }
        event.setCancelled(true);
        if (event.getClick() == org.bukkit.event.inventory.ClickType.NUMBER_KEY
                || event.getClick() == org.bukkit.event.inventory.ClickType.SWAP_OFFHAND
                || event.getClick() == org.bukkit.event.inventory.ClickType.DOUBLE_CLICK
                || event.getClick() == org.bukkit.event.inventory.ClickType.MIDDLE
                || event.getAction() == org.bukkit.event.inventory.InventoryAction.COLLECT_TO_CURSOR) {
            return;
        }
        if (event.getClickedInventory() == null) {
            return;
        }
        if (!event.getClickedInventory().equals(menu.getInventory())) {
            return;
        }
        if (!plugin.canUseMenu(player)) {
            player.closeInventory();
            return;
        }
        try {
            menu.onClick(event);
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("menu click failed: " + exception);
            player.closeInventory();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player && SnuffMenu.of(player) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        SnuffMenu menu = SnuffMenu.of(player);
        if (menu == null) {
            return;
        }
        try {
            menu.onClose(event);
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("menu close failed: " + exception);
        } finally {
            SnuffMenu.forget(player.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        SnuffMenu.forget(event.getPlayer().getUniqueId());
    }

    public static void shutdown(Plugin plugin) {
        SnuffMenu.closeAll();
    }
}
