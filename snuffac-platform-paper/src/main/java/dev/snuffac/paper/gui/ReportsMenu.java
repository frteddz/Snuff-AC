package dev.snuffac.paper.gui;

import dev.snuffac.core.report.ReportCategory;
import dev.snuffac.paper.SnuffPaperPlugin;
import dev.snuffac.paper.StaffMessages;
import dev.snuffac.core.report.ReportStore;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

public final class ReportsMenu extends SnuffMenu {

    public static final String ACTION_OPEN_PICKER = "open_reports";
    public static final String ACTION_OPEN_ADMIN = "open_reports_admin";
    public static final String ACTION_BACK = "report_back";
    public static final String ACTION_SUBMIT = "report_submit";
    public static final String ACTION_CLAIM = "report_claim";
    public static final String ACTION_RELEASE = "report_unclaim";
    public static final String ACTION_RESOLVE = "report_dismiss";

    private final SnuffPaperPlugin plugin;
    private final ReportStore store;
    private final Player target;
    private final boolean adminView;
    private final int page;
    private final String filterStatus;
    private final String filterCategory;
    private String selectedCategory;

    public ReportsMenu(
            SnuffPaperPlugin plugin,
            ReportStore store,
            Player target,
            boolean adminView,
            int page,
            String filterStatus,
            String filterCategory) {
        super(plugin, 54, adminView ? "Reports" : "Report a player",
                GuiLayout.load(plugin, adminView
                        ? dev.snuffac.paper.GuiDefaults.REPORTS_ADMIN
                        : dev.snuffac.paper.GuiDefaults.REPORTS));
        this.plugin = plugin;
        this.store = store;
        this.target = target;
        this.adminView = adminView;
        this.page = page;
        this.filterStatus = filterStatus;
        this.filterCategory = filterCategory;
    }

    public void select(String category) {
        this.selectedCategory = category;
    }

    public void forViewer(Player viewer) {
        this.renderPlayer = viewer;
    }

    public int page() {
        return page;
    }

    public String filterStatus() {
        return filterStatus;
    }

    public String filterCategory() {
        return filterCategory;
    }

    public ReportCategory.Entry firstVisible() {
        List<ReportCategory.Entry> entries = visible();
        return entries.isEmpty() ? null : entries.get(0);
    }

    public List<ReportCategory.Entry> visible() {
        List<ReportCategory.Entry> all = store.all();
        if (!adminView) {
            return all;
        }
        return ReportCategory.filter(all, filterStatus, filterCategory);
    }

    @Override
    protected void render() {
        renderPlayer = Bukkit.getPlayer(target == null ? null : target.getUniqueId());
        if (adminView) {
            renderAdmin();
            return;
        }
        renderPicker();
    }

    private void renderPicker() {
        if (layout() != null && !layout().buttons().isEmpty()) {
            fillFromLayout();
            return;
        }
        int slot = 10;
        for (ReportCategory.Definition definition : ReportCategory.all()) {
            if (slot >= 16) {
                slot = 28;
            }
            if (slot >= 34) {
                break;
            }
            set(slot, iconFor(definition.id()), "<white>" + definition.label(),
                    List.of("<gray>" + definition.description()), "report_" + definition.id());
            slot++;
        }
        set(49, Material.PAPER, "<white>Submit report",
                List.of("<gray>Selected: <white>" + label(selectedCategory),
                        "<gray>Note: <white>" + note),
                ACTION_SUBMIT);
        set(45, Material.ARROW, "<white>Back", List.of(), ACTION_BACK);
    }

    private void renderAdmin() {
        List<ReportCategory.Entry> entries = visible();
        int slot = 10;
        int shown = 0;
        for (ReportCategory.Entry entry : entries) {
            if (shown >= pageSize()) {
                break;
            }
            ItemStack head = head(entry);
            set(slot, "<white>" + entry.targetName(), loreFor(entry), "none", head);
            slot++;
            if (slot == 16) {
                slot = 28;
            }
            if (slot >= 43) {
                break;
            }
            shown++;
        }
        if (shown == 0) {
            set(22, Material.PAPER, "<white>No reports", List.of(), "none");
        }
        set(45, Material.ARROW, "<white>Back", List.of(), ACTION_BACK);
        set(48, Material.LIME_CONCRETE, "<green>Claim",
                List.of("<gray>Take ownership of the top report"), ACTION_CLAIM);
        set(50, Material.RED_CONCRETE, "<red>Resolve",
                List.of("<gray>Close the top report as handled"), ACTION_RESOLVE);
    }

    private int pageSize() {
        return 28;
    }

    private void set(int slot, String name, List<String> lore, String action, ItemStack head) {
        if (slot < 0 || slot >= getInventory().getSize()) {
            return;
        }
        getInventory().setItem(slot, decorate(head, name, lore, action));
    }

    private ItemStack head(ReportCategory.Entry entry) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        if (item.getItemMeta() instanceof SkullMeta meta) {
            Player online = Bukkit.getPlayer(entry.target());
            if (online != null) {
                meta.setOwningPlayer(online);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private List<String> loreFor(ReportCategory.Entry entry) {
        ReportCategory.Definition definition = ReportCategory.get(entry.category());
        List<String> lore = new java.util.ArrayList<>();
        lore.add("<gray>Category: <white>"
                + (definition == null ? entry.category() : definition.label()));
        lore.add("<gray>Status: <white>" + entry.status());
        lore.add("<gray>Reporter: <white>" + entry.reporterName());
        lore.add("<gray>Filed: <white>" + relative(entry.createdMillis()));
        if (entry.claimed()) {
            lore.add("<gray>Held by: <white>" + entry.handledByName());
        }
        if (entry.note() != null && !entry.note().isBlank()) {
            lore.add("<gray>Note: <white>" + entry.note());
        }
        if (adminView && entry.category().equals(ReportCategory.CHEATING)) {
            lore.addAll(evidenceLines());
        }
        return lore;
    }

    private List<String> evidenceLines() {
        if (target == null) {
            return List.of();
        }
        var store = pluginHistory();
        if (store == null) {
            return List.of();
        }
        var records = store.history(target.getUniqueId());
        if (records.isEmpty()) {
            return List.of("<gray>Snuff evidence: <white>no recorded flags");
        }
        List<String> lines = new java.util.ArrayList<>();
        lines.add("<gray>Snuff evidence: <white>" + records.size() + " flag(s)");
        String latest = records.get(records.size() - 1).checkKey();
        lines.add("<gray>Most recent check: <white>" + latest);
        return lines;
    }

    private dev.snuffac.core.violation.ViolationHistoryStore pluginHistory() {
        return history;
    }

    private dev.snuffac.core.violation.ViolationHistoryStore history;

    public void history(dev.snuffac.core.violation.ViolationHistoryStore value) {
        this.history = value;
    }

    private ItemStack decorate(ItemStack item, String name, List<String> lore, String action) {
        if (item == null) {
            return null;
        }
        var meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.displayName(component(name));
        if (lore != null && !lore.isEmpty()) {
            meta.lore(lore.stream().map(SnuffMenu::component).toList());
        }
        if (action != null && !action.isEmpty()) {
            meta.getPersistentDataContainer().set(actionKey(), PersistentDataType.STRING, action);
        }
        item.setItemMeta(meta);
        return item;
    }

    private String relative(long millis) {
        long seconds = Math.max(0L, (System.currentTimeMillis() - millis) / 1000L);
        if (seconds < 60L) {
            return seconds + "s ago";
        }
        if (seconds < 3600L) {
            return (seconds / 60L) + "m ago";
        }
        if (seconds < 86400L) {
            return (seconds / 3600L) + "h ago";
        }
        return (seconds / 86400L) + "d ago";
    }

    private String label(String category) {
        if (category == null) {
            return "nothing yet";
        }
        ReportCategory.Definition definition = ReportCategory.get(category);
        return definition == null ? category : definition.label();
    }

    private static String note = "";

    public void note(String value) {
        note = value == null ? "" : value;
    }

    public String note() {
        return note;
    }

    public static void resetNote() {
        note = "";
    }

    private static Material iconFor(String category) {
        return switch (category) {
            case "cheating" -> Material.DIAMOND_SWORD;
            case "exploiting" -> Material.TNT;
            case "language" -> Material.PAPER;
            case "offensive" -> Material.REDSTONE;
            case "griefing" -> Material.IRON_PICKAXE;
            case "name" -> Material.NAME_TAG;
            case "impersonation" -> Material.PLAYER_HEAD;
            default -> Material.PAPER;
        };
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        String action = actionOf(event.getInventory(), event.getRawSlot());
        if (action == null || action.isEmpty() || "none".equals(action)) {
            return;
        }
        if (!plugin.canUseMenu(player)) {
            player.closeInventory();
            return;
        }
        if (ADMIN_ACTIONS.contains(action) && !player.hasPermission("snuffac.admin")) {
            StaffMessages.send(player, "You do not have permission to handle reports.");
            return;
        }
        switch (action) {
            case ACTION_BACK -> openParent(player);
            case ACTION_SUBMIT -> submit(player);
            default -> {
                if (action.startsWith("report_") && !ADMIN_ACTIONS.contains(action)) {
                    selectedCategory = action.substring("report_".length());
                    renderPicker();
                    refresh(player);
                }
            }
        }
    }

    private static final java.util.Set<String> ADMIN_ACTIONS = java.util.Set.of(
            ACTION_CLAIM, ACTION_RELEASE, ACTION_RESOLVE, ACTION_OPEN_ADMIN);

    private void submit(Player player) {
        if (selectedCategory == null) {
            StaffMessages.send(player, "Pick a category first.");
            return;
        }
        var entry = store.file(
                player.getUniqueId(),
                player.getName(),
                target.getUniqueId(),
                target.getName(),
                selectedCategory,
                note);
        if (entry == null) {
            StaffMessages.send(player, "That report could not be filed.");
            return;
        }
        store.save();
        StaffMessages.send(player, "Report filed against " + target.getName() + ".");
        close(player);
    }

    private void refresh(Player player) {
        player.updateInventory();
    }
}
