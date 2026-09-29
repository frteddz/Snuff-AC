package dev.snuffac.paper.gui;

import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.Plugin;

public final class SettingsMenu extends SnuffMenu {

    public static final String ACTION_RETENTION_UP = "retention_up";
    public static final String ACTION_RETENTION_DOWN = "retention_down";
    public static final String ACTION_HISTORY_UP = "history_up";
    public static final String ACTION_HISTORY_DOWN = "history_down";
    public static final String ACTION_PREVENTION = "prevention";
    public static final String ACTION_ALERT_COOLDOWN_UP = "cooldown_up";
    public static final String ACTION_ALERT_COOLDOWN_DOWN = "cooldown_down";
    public static final String ACTION_RELOAD = "reload";
    public static final String ACTION_CLOSE = "close";

    private final dev.snuffac.paper.SnuffPaperPlugin plugin;

    public SettingsMenu(Plugin plugin) {
        super(plugin, 27, "Snuff Settings");
        this.plugin = (dev.snuffac.paper.SnuffPaperPlugin) plugin;
    }

    @Override
    protected void render() {
        var config = plugin.core().config();
        set(10, Material.CLOCK, "<white>Log Retention",
                List.of("<gray>Days before old log files are deleted",
                        "<white>Current: <aqua>" + config.logRetentionDays() + " days",
                        "",
                        "<gray>Left click to decrease",
                        "<gray>Right click to increase"), null);
        set(11, Material.PAPER, "<white>History Retention",
                List.of("<gray>Days before old flag history is purged",
                        "<white>Current: <aqua>" + config.historyRetentionDays() + " days",
                        "",
                        "<gray>Left click to decrease",
                        "<gray>Right click to increase"), null);
        set(12, Material.LEVER, "<white>Prevention",
                List.of("<gray>Setback and cancel actions",
                        "<white>Current: <aqua>" + (config.preventionEnabled() ? "enabled" : "disabled"),
                        "<gray>Click to toggle"), ACTION_PREVENTION);
        set(13, Material.BELL, "<white>Alert Cooldown",
                List.of("<gray>Milliseconds between repeat alerts",
                        "<white>Current: <aqua>" + config.alertCooldownMillis() + "ms",
                        "",
                        "<gray>Left click to decrease",
                        "<gray>Right click to increase"), null);
        set(15, Material.BARRIER, "<white>Close", List.of(), ACTION_CLOSE);
        set(22, Material.REDSTONE, "<white>Reload Config",
                List.of("<gray>Re-read config and checks from disk"), ACTION_RELOAD);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        int slot = event.getRawSlot();
        if (!plugin.canUseMenu(player)) {
            close(player);
            return;
        }
        boolean increase = event.isRightClick();
        var config = plugin.core().config();
        if (slot == 10) {
            config.logRetentionDays(clamp(config.logRetentionDays() + (increase ? 1 : -1), 1, 365));
        } else if (slot == 11) {
            config.historyRetentionDays(clamp(config.historyRetentionDays() + (increase ? 1 : -1), 1, 365));
        } else if (slot == 12) {
            config.preventionEnabled(!config.preventionEnabled());
        } else if (slot == 13) {
            config.alertCooldownMillis(clamp(config.alertCooldownMillis() + (increase ? 500 : -500), 0, 60_000));
        } else if (slot == 15) {
            close(player);
            return;
        } else if (slot == 22) {
            plugin.reloadEverything();
        } else {
            return;
        }
        build();
        player.updateInventory();
    }

    private static int clamp(int value, int low, int high) {
        return Math.max(low, Math.min(high, value));
    }
}
