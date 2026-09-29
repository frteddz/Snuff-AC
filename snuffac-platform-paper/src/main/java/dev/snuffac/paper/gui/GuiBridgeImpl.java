package dev.snuffac.paper.gui;

import dev.snuffac.api.violation.ViolationInfo;
import dev.snuffac.core.player.PlayerData;
import dev.snuffac.paper.SnuffPaperPlugin;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class GuiBridgeImpl implements MainMenu.GuiBridge, FlagsMenu.FlagsBridge {

    private final SnuffPaperPlugin plugin;

    public GuiBridgeImpl(SnuffPaperPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public PlayerData dataOf(UUID id) {
        return plugin.dataOf(id);
    }

    @Override
    public void openSuspicious(Player player, int page) {
        var menu = new FlagsMenu(plugin, this, page);
        menu.setParent(mainMenu());
        menu.build();
        menu.open(player);
    }

    public void openCase(Player player, String uuid) {
        try {
            showCase(player, java.util.UUID.fromString(uuid));
        } catch (IllegalArgumentException malformed) {
            player.sendMessage(Component.text("[Snuff] Bad player id."));
        }
    }

    @Override
    public void openSettings(Player player) {
        plugin.openSettings(player);
    }

    @Override
    public void openWarned(Player player) {
        plugin.openWarned(player);
    }

    @Override
    public void alertsToggled(Player player, boolean enabled) {
        player.sendMessage(Component.text(
                "[Snuff] Your alerts are now " + (enabled ? "on" : "off") + "."));
    }

    @Override
    public void page(Player player, SnuffMenu current, int page) {
        if (current instanceof FlagsMenu menu) {
            var next = new FlagsMenu(plugin, this, page);
            next.setParent(menu.parent());
            next.build();
            next.open(player);
        }
    }

    @Override
    public List<FlagsMenu.Entry> entries() {
        List<FlagsMenu.Entry> rows = new ArrayList<>();
        var store = plugin.core().historyStore();
        if (store == null) {
            return rows;
        }
        for (UUID id : plugin.knownPlayerIds()) {
            List<ViolationInfo> records = store.history(id);
            if (records.isEmpty()) {
                continue;
            }
            String name = records.get(records.size() - 1).playerName();
            FlagsMenu.Summary summary = FlagsMenu.summarise(records);
            rows.add(new FlagsMenu.Entry(
                    id,
                    name,
                    Bukkit.getPlayer(id) != null,
                    records.size(),
                    summary.topChecks(),
                    summary.lastFlagMillis()));
        }
        rows.sort(Comparator.comparingLong(FlagsMenu.Entry::lastFlagMillis).reversed());
        return rows;
    }

    @Override
    public void showCase(Player player, UUID id) {
        var store = plugin.core().historyStore();
        if (store == null) {
            return;
        }
        List<ViolationInfo> records = store.history(id);
        if (records.isEmpty()) {
            player.sendMessage(Component.text("[Snuff] No history for that player."));
            return;
        }
        String name = records.get(records.size() - 1).playerName();
        player.sendMessage(Component.text("[Snuff] " + name + " has " + records.size()
                + " recorded flag(s). Most recent:"));
        int shown = 0;
        for (int i = records.size() - 1; i >= 0 && shown < 10; i--, shown++) {
            ViolationInfo info = records.get(i);
            player.sendMessage(Component.text("  " + info.checkName()
                    + " VL " + info.violationLevel()
                    + " ping " + info.pingMillis()
                    + "ms at " + info.timestampMillis()));
        }
    }

    private MainMenu mainMenu() {
        var menu = new MainMenu(plugin, this);
        menu.build();
        return menu;
    }
}
