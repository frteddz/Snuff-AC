package dev.snuffac.paper;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.core.config.CheckConfig;
import dev.snuffac.core.player.PlayerData;
import dev.snuffac.core.violation.CheckState;
import dev.snuffac.core.violation.ViolationRecord;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class SnuffCommand implements CommandExecutor, TabCompleter {


    private final SnuffPaperPlugin plugin;
    private final PunishCommands punishCommands;

    public SnuffCommand(SnuffPaperPlugin plugin) {
        this.plugin = plugin;
        this.punishCommands = new PunishCommands(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player && player.hasPermission("snuffac.menu")) {
                plugin.openMainMenu(player);
                return true;
            }
            send(sender, "usage");
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (PunishCommands.REVERSE.contains(sub)) {
            if (!sender.hasPermission("snuffac.punish.unban")) {
                send(sender, "no-permission");
                return true;
            }
            return punishCommands.reverse(sender, sub, args);
        }
        if (PunishCommands.PUNISH.contains(sub)) {
            if (!sender.hasPermission(PunishCommands.permissionFor(sub))) {
                send(sender, "no-permission");
                return true;
            }
            return punishCommands.execute(sender, sub, args);
        }
        if (sub.equals("punishments")) {
            if (!sender.hasPermission("snuffac.punish.ban")) {
                send(sender, "no-permission");
                return true;
            }
            return punishCommands.view(sender, args);
        }
        if (sub.equals("warns")) {
            if (!sender.hasPermission("snuffac.punish.warn")) {
                send(sender, "no-permission");
                return true;
            }
            return punishCommands.warns(sender, args);
        }
        if (sub.equals("settings")) {
            if (!sender.hasPermission("snuffac.escalation.manage")) {
                send(sender, "no-permission");
                return true;
            }
            return punishCommands.settings(sender);
        }
        if (!sender.hasPermission("snuffac.admin")) {
            send(sender, "no-permission");
            return true;
        }
        switch (sub) {
            case "info" -> info(sender);
            case "version" -> send(sender, "version", Map.ofEntries(
                    Map.entry("version", plugin.api().version()),
                    Map.entry("platform", plugin.platform().name()),
                    Map.entry("checks", String.valueOf(plugin.core().checkKeys().size()))));
            case "reload" -> {
                plugin.reloadConfiguration();
                send(sender, "reloaded");
            }
            case "debug" -> debug(sender, args);
            case "alerts" -> alerts(sender, args);
            case "checks" -> checks(sender);
            case "violations" -> violations(sender, args);
            case "toggle" -> toggle(sender, args);
            case "stats" -> stats(sender);
            case "setback" -> setback(sender, args);
            case "profile" -> profile(sender, args);
            case "reports" -> reports(sender, args);
            case "report" -> report(sender, args);
            case "menu" -> openMenu(sender);
            case "bypass" -> bypass(sender, args);
            case "clearflags" -> clearFlags(sender, args);
            case "clearwarns" -> clearWarns(sender, args);
            case "clearpunishments" -> clearPunishments(sender, args);
            case "tp" -> teleportTo(sender, args);
            default -> send(sender, "unknown", Map.of("sub", sub));
        }
        return true;
    }

    private void info(CommandSender sender) {
        var core = plugin.core();
        send(sender, "info", Map.ofEntries(
                Map.entry("version", plugin.api().version()),
                Map.entry("platform", plugin.platform().name()),
                Map.entry("checks", String.valueOf(core.checkKeys().size())),
                Map.entry("enabled", String.valueOf(core.config().enabled())),
                Map.entry("tps", format(core.server().tps())),
                Map.entry("players", String.valueOf(core.players().size()))));
    }

    private void stats(CommandSender sender) {
        var core = plugin.core();
        double totalVl = core.players().stream()
                .mapToDouble(player -> player.totalViolationLevel())
                .sum();
        send(sender, "stats", Map.ofEntries(
                Map.entry("players", String.valueOf(core.players().size())),
                Map.entry("violations", format(totalVl)),
                Map.entry("tps", format(core.server().tps())),
                Map.entry("minTps", format(core.server().minTps()))));
    }

    private void violationsConsole(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            send(sender, "gui-needs-player");
            return;
        }
        if (!plugin.canUseMenu(player)) {
            send(sender, "no-permission");
            return;
        }
        plugin.openMainMenu(player);
    }

    private void checks(CommandSender sender) {
        StringBuilder builder = new StringBuilder();
        for (CheckCategory category : CheckCategory.values()) {
            builder.append("<gray>").append(category.name().toLowerCase(Locale.ROOT)).append(":</gray> ");
            List<String> names = plugin.core().registry().byCategory(category).stream()
                    .map(check -> {
                        CheckConfig config = plugin.core().registry().config(check);
                        String state = config.enabled() ? "<green>on</green>" : "<red>off</red>";
                        return "<yellow>" + check.key() + "</yellow>" + state;
                    })
                    .collect(Collectors.toList());
            builder.append(String.join("<gray>,</gray> ", names));
            builder.append("  ");
        }
        raw(sender, builder.toString());
    }

    private void toggle(CommandSender sender, String[] args) {
        if (args.length < 2) {
            send(sender, "usage");
            return;
        }
        String key = args[1].toLowerCase(Locale.ROOT);
        var check = plugin.core().registry().check(key);
        if (check == null) {
            send(sender, "unknown-check", Map.of("check", key));
            return;
        }
        boolean target = !plugin.core().registry().config(check).enabled();
        plugin.setCheckEnabled(check.key(), target);
        send(sender, "toggled", Map.of("check", check.key(), "state", String.valueOf(target)));
    }

    private void debug(CommandSender sender, String[] args) {
        Player target = args.length > 1
                ? Bukkit.getPlayerExact(args[1])
                : sender instanceof Player player ? player : null;
        if (target == null) {
            send(sender, "no-player");
            return;
        }
        PlayerData data = plugin.dataOf(target.getUniqueId());
        if (data == null) {
            send(sender, "no-player");
            return;
        }
        boolean enable = !data.debugEnabled();
        plugin.setDebug(data, enable);
        send(sender, "debug-toggled", Map.of("player", target.getName(), "state", String.valueOf(enable)));
        if (enable) {
            printDebug(target, data);
        }
    }

    void printDebug(Player player, PlayerData data) {
        var state = data.movement();
        var network = data.network();
        var config = plugin.core().registry().config("fly");
        StringBuilder builder = new StringBuilder();
        builder.append("<gray>--- Snuff debug </gray><white>").append(player.getName()).append("</white>\n");
        builder.append("<gray>pos</gray> <white>")
                .append(format(state.position().x())).append(", ")
                .append(format(state.position().y())).append(", ")
                .append(format(state.position().z())).append("</white>\n");
        builder.append("<gray>vel</gray> <white>")
                .append(format(state.velocity().x())).append(", ")
                .append(format(state.velocity().y())).append(", ")
                .append(format(state.velocity().z())).append("</white>\n");
        builder.append("<gray>onGround</gray> <white>").append(state.onGround())
                .append("</white> <gray>airTicks</gray> <white>").append(state.ticksSinceGround())
                .append("</white> <gray>ticks</gray> <white>").append(data.network().tickCounter())
                .append("</white>\n");
        builder.append("<gray>ping</gray> <white>").append(format(network.ping()))
                .append("ms</white> <gray>tps</gray> <white>").append(format(plugin.core().server().tps()))
                .append("</white>\n");
        builder.append("<gray>tolerance</gray> <white>").append(format(state.tolerance().total().length()))
                .append("</white> <gray>flyEnabled</gray> <white>").append(config != null && config.enabled())
                .append("</white>");
        raw(player, builder.toString());
    }

    private void alerts(CommandSender sender, String[] args) {
        if (args.length < 2) {
            send(sender, "usage");
            return;
        }
        String targetName = args[1];
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            send(sender, "no-player");
            return;
        }
        PlayerData data = plugin.dataOf(target.getUniqueId());
        if (data == null) {
            send(sender, "no-player");
            return;
        }
        boolean enable = !data.alertsEnabled();
        data.alertsEnabled(enable);
        send(sender, "alerts-toggled", Map.of("player", target.getName(), "state", String.valueOf(enable)));
    }

    private void violations(CommandSender sender, String[] args) {
        if (args.length >= 2 && sender instanceof Player player) {
            var target = Bukkit.getPlayerExact(args[1]);
            if (target != null) {
                plugin.openCase(player, target.getUniqueId().toString());
                return;
            }
            plugin.openSuspiciousByName(player, args[1]);
            return;
        }
        if (sender instanceof Player player) {
            plugin.openSuspicious(player);
            return;
        }
        violationsConsole(sender, args);
        Player target = args.length > 1
                ? Bukkit.getPlayerExact(args[1])
                : sender instanceof Player player ? player : null;
        if (target == null) {
            send(sender, "no-player");
            return;
        }
        PlayerData data = plugin.dataOf(target.getUniqueId());
        if (data == null) {
            send(sender, "no-player");
            return;
        }
        List<ViolationRecord> history = new ArrayList<>(data.history());
        if (history.isEmpty()) {
            send(sender, "no-violations", Map.of("player", target.getName()));
            return;
        }
        StringBuilder builder = new StringBuilder();
        builder.append("<gray>violations for</gray> <white>").append(target.getName())
                .append("</white> <gray>(total vl</gray> <white>")
                .append(format(data.totalViolationLevel())).append("</white><gray>)</gray>\n");
        int shown = 0;
        for (ViolationRecord record : history) {
            if (shown++ >= 8) {
                break;
            }
            builder.append("<gray>-</gray> <yellow>").append(record.checkKey())
                    .append("</yellow> <gray>vl</gray> <white>").append(format(record.violationLevel()))
                    .append("</white> <gray>").append(record.detail()).append("</gray>\n");
        }
        raw(sender, builder.toString());
    }

    private void setback(CommandSender sender, String[] args) {
        Player target = args.length > 1
                ? Bukkit.getPlayerExact(args[1])
                : sender instanceof Player player ? player : null;
        if (target == null) {
            send(sender, "no-player");
            return;
        }
        PlayerData data = plugin.dataOf(target.getUniqueId());
        if (data == null) {
            send(sender, "no-player");
            return;
        }
        plugin.performSetback(data, "manual");
        send(sender, "setback", Map.of("player", target.getName()));
    }

    private void profile(CommandSender sender, String[] args) {
        Player target = args.length > 1
                ? Bukkit.getPlayerExact(args[1])
                : sender instanceof Player player ? player : null;
        if (target == null) {
            send(sender, "no-player");
            return;
        }
        PlayerData data = plugin.dataOf(target.getUniqueId());
        if (data == null) {
            send(sender, "no-player");
            return;
        }
        var combat = data.combat();
        var network = data.network();
        StringBuilder builder = new StringBuilder();
        builder.append("<gray>profile</gray> <white>").append(target.getName()).append("</white>\n");
        builder.append("<gray>protocol</gray> <white>").append(data.protocolVersion())
                .append("</white> <gray>ping</gray> <white>").append(format(network.ping()))
                .append("</white> <gray>minPing</gray> <white>").append(format(network.minPing()))
                .append("</white>\n");
        builder.append("<gray>attacks</gray> <white>").append(combat.attacks())
                .append("</white> <gray>lastReach</gray> <white>").append(format(combat.lastReach()))
                .append("</white> <gray>avgReach</gray> <white>").append(format(combat.reachAverage()))
                .append("</white>\n");
        builder.append("<gray>badPackets</gray> <white>").append(network.invalidPackets())
                .append("</white> <gray>cancelled</gray> <white>").append(network.cancelledPackets())
                .append("</white> <gray>exempt</gray> <white>").append(data.exempt()).append("</white>");
        raw(sender, builder.toString());
    }

    private static final java.util.Map<String, String> PERMISSIONS = java.util.Map.ofEntries(
            java.util.Map.entry("menu", "snuffac.menu"),
            java.util.Map.entry("version", "snuffac.version"),
            java.util.Map.entry("info", "snuffac.use"),
            java.util.Map.entry("report", "snuffac.report"),
            java.util.Map.entry("reports", "snuffac.reports.manage"),
            java.util.Map.entry("violations", "snuffac.violations"),
            java.util.Map.entry("checks", "snuffac.violations"),
            java.util.Map.entry("toggle", "snuffac.checks"),
            java.util.Map.entry("reload", "snuffac.reload"),
            java.util.Map.entry("debug", "snuffac.debug"),
            java.util.Map.entry("alerts", "snuffac.alerts"),
            java.util.Map.entry("stats", "snuffac.stats"),
            java.util.Map.entry("setback", "snuffac.setback"),
            java.util.Map.entry("profile", "snuffac.profile"),
            java.util.Map.entry("bypass", "snuffac.bypass.give"),
            java.util.Map.entry("clearflags", "snuffac.clear.flags"),
            java.util.Map.entry("clearwarns", "snuffac.clear.warns"),
            java.util.Map.entry("clearpunishments", "snuffac.clear.punishments"),
            java.util.Map.entry("tp", "snuffac.teleport"),
            java.util.Map.entry("settings", "snuffac.escalation.manage"));

    static java.util.Set<String> declaredSubcommands() {
        return java.util.Collections.unmodifiableSet(PERMISSIONS.keySet());
    }

    static java.util.List<String> advertisedWithoutPermission() {
        java.util.List<String> missing = new java.util.ArrayList<>();
        for (String sub : SUB_COMMANDS) {
            if (PunishCommands.REVERSE.contains(sub)) {
                continue;
            }
            if (!PERMISSIONS.containsKey(sub)) {
                missing.add(sub);
            }
        }
        return missing;
    }

    static String permissionForSubcommand(String sub) {
        return PERMISSIONS.get(sub == null ? "" : sub.toLowerCase(Locale.ROOT));
    }

    private static boolean permissionFor(CommandSender sender, String sub) {
        if (!sender.hasPermission("snuffac.use")) {
            return false;
        }
        String node = permissionForSubcommand(sub);
        if (node == null) {
            return sender.hasPermission("snuffac.admin");
        }
        return sender.hasPermission(node);
    }

    private final PendingConfirm confirmations = new PendingConfirm();

    private void bypass(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            StaffMessages.send(sender, "Only a player can bypass another player.");
            return;
        }
        if (args.length < 2) {
            StaffMessages.send(player, "Usage: /snuff bypass <player> [on|off]");
            return;
        }
        UUID id = PendingConfirm.resolve(args[1]);
        if (id == null) {
            StaffMessages.send(player, "That player is unknown to this server.");
            return;
        }
        if (!player.hasPermission("snuffac.bypass.give")) {
            StaffMessages.send(player, "You do not have permission to grant a bypass.");
            return;
        }
        boolean grant = args.length < 3 || !args[2].equalsIgnoreCase("off");
        var target = plugin.permitManager().setBypass(id, grant, player.getName());
        if (target == null) {
            StaffMessages.send(player, "That player is not online, so the bypass could not be applied.");
            return;
        }
        StaffMessages.send(player, (grant ? "Bypass granted to " : "Bypass revoked for ") + target
                + ". They will not be flagged.");
        StaffMessages.sendToAll(Bukkit.getOnlinePlayers().stream()
                .filter(staff -> staff.hasPermission("snuffac.debug"))
                .toList(), player.getName() + " "
                + (grant ? "granted" : "revoked") + " an anticheat bypass for " + target + ".");
    }

    private void clearFlags(CommandSender sender, String[] args) {
        destructive(sender, args, "clearflags", "snuffac.clear.flags",
                "clear the recorded violation history");
    }

    private void clearWarns(CommandSender sender, String[] args) {
        destructive(sender, args, "clearwarns", "snuffac.clear.warns",
                "clear the automatic warning ladder count");
    }

    private void clearPunishments(CommandSender sender, String[] args) {
        destructive(sender, args, "clearpunishments", "snuffac.clear.punishments",
                "clear every active punishment");
    }

    private void destructive(CommandSender sender, String[] args, String action, String permission,
            String what) {
        if (!(sender instanceof Player player)) {
            StaffMessages.send(sender, "Only a player can do that.");
            return;
        }
        if (args.length < 2) {
            StaffMessages.send(player, "Usage: /snuff " + action + " <player> [confirm]");
            return;
        }
        if (!player.hasPermission(permission)) {
            StaffMessages.send(player, "You do not have permission to do that.");
            return;
        }
        UUID id = PendingConfirm.resolve(args[1]);
        String name = PendingConfirm.nameOf(id, args[1]);
        if (id == null) {
            StaffMessages.send(player, "That player is unknown to this server.");
            return;
        }

        if (args.length >= 3 && args[2].equalsIgnoreCase("confirm")) {
            executeDestructive(player, action, id, name);
            return;
        }

        var entry = confirmations.open(player.getUniqueId(), action, id, name,
                what + " for " + name);
        StaffMessages.send(player, "This will " + what + " for " + name
                + ". This cannot be undone.");
        StaffMessages.send(player, "Run /snuff " + action + " " + args[1]
                + " confirm to continue, or " + String.join(" or ", PendingConfirm.expiredWords())
                + " to abort. The prompt expires in 20 seconds.");
    }

    private void executeDestructive(Player staff, String action, UUID id, String name) {
        confirmations.clear(staff.getUniqueId());
        int count;
        String result;
        switch (action) {
            case "clearflags" -> {
                int removed = plugin.core().historyStore() == null
                        ? 0 : plugin.core().historyStore().clear(id);
                var data = plugin.dataOf(id);
                if (data != null) {
                    data.resetSessionState();
                    for (var key : plugin.core().checkKeys()) {
                        var state = data.checkState(key);
                        if (state != null) {
                            state.level().reset();
                            state.buffer().reset();
                        }
                    }
                }
                count = removed;
                result = removed + " flag(s) cleared for " + name;
            }
            case "clearwarns" -> {
                plugin.escalation().resetForStaffAction(id);
                count = 1;
                result = "warning ladder reset for " + name;
            }
            default -> {
                int removed = plugin.punishments().clearAll(id);
                count = removed;
                result = removed + " active punishment(s) cleared for " + name;
            }
        }
        StaffMessages.send(staff, result + ".");
        StaffMessages.sendToAll(Bukkit.getOnlinePlayers().stream()
                .filter(online -> online.hasPermission("snuffac.debug"))
                .toList(), staff.getName() + " cleared " + action.replace("clear", "")
                + " for " + name + " (" + count + ").");
    }

    private void teleportTo(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            StaffMessages.send(sender, "Only a player can teleport.");
            return;
        }
        if (!player.hasPermission("snuffac.teleport")) {
            StaffMessages.send(player, "You do not have permission to teleport to a flagged player.");
            return;
        }
        if (args.length < 2) {
            StaffMessages.send(player, "Usage: /snuff tp <player>");
            return;
        }
        UUID id = PendingConfirm.resolve(args[1]);
        if (id == null) {
            StaffMessages.send(player, "That player is unknown to this server.");
            return;
        }
        Player target = Bukkit.getPlayer(id);
        if (target != null && target.isOnline()) {
            player.teleport(target.getLocation());
            StaffMessages.send(player, "Teleported to " + target.getName() + ".");
            return;
        }
        var last = plugin.core().historyStore() == null
                ? java.util.List.<dev.snuffac.api.violation.ViolationInfo>of()
                : plugin.core().historyStore().history(id);
        if (last.isEmpty() || last.get(last.size() - 1).worldName().isBlank()) {
            StaffMessages.send(player, "That player is offline and has no recorded last position.");
            return;
        }
        var record = last.get(last.size() - 1);
        var world = Bukkit.getWorld(record.worldName());
        if (world == null) {
            StaffMessages.send(player, "World " + record.worldName() + " is not loaded.");
            return;
        }
        var location = new org.bukkit.Location(world, record.x(), record.y(), record.z());
        if (!world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
            StaffMessages.send(player, "That chunk is not loaded, refusing to teleport you there.");
            return;
        }
        player.teleport(location);
        StaffMessages.send(player, "Teleported to where " + record.playerName() + " was last seen.");
    }

    private void openMenu(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            StaffMessages.send(sender, "Only a player can open the menu.");
            return;
        }
        if (!plugin.canUseMenu(player)) {
            StaffMessages.send(player, "You do not have permission to open the staff menus.");
            return;
        }
        plugin.openMainMenu(player);
    }

    private void reports(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            StaffMessages.send(sender, "Only a player can browse reports.");
            return;
        }
        if (!player.hasPermission("snuffac.reports.manage")) {
            StaffMessages.send(player, "You do not have permission to view reports.");
            return;
        }
        plugin.openAdminReports(player, 0, "", "");
    }

    private void report(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            StaffMessages.send(sender, "Only a player can file a report.");
            return;
        }
        if (args.length < 2) {
            StaffMessages.send(player, "Usage: /snuff report <player>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            target = Bukkit.getPlayer(args[1]);
        }
        if (target == null) {
            StaffMessages.send(player, "That player is not online.");
            return;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            StaffMessages.send(player, "You cannot report yourself.");
            return;
        }
        if (plugin.reports().rateLimited(player.getUniqueId())) {
            StaffMessages.send(player, "You are filing reports too quickly. Try again later.");
            return;
        }
        if (target.hasPermission("snuffac.exempt.punish")) {
            StaffMessages.send(player, "That player is exempt from reports.");
            return;
        }
        dev.snuffac.paper.gui.ReportsMenu.resetNote();
        dev.snuffac.paper.gui.ReportsMenu menu = new dev.snuffac.paper.gui.ReportsMenu(
                plugin, plugin.reports(), target, false, 0, "", "");
        menu.setParent(new dev.snuffac.paper.gui.MainMenu(plugin, plugin.guiBridge()));
        menu.forViewer(player);
        menu.build();
        menu.open(player);
    }

    private static final List<String> SUB_COMMANDS = List.of(
            "info", "version", "reload", "debug", "alerts", "checks", "violations",
            "toggle", "stats", "setback", "profile", "report", "reports",
            "bypass", "clearflags", "clearwarns", "clearpunishments", "tp",
            "menu", "settings", "punishments", "warns", "sounds",
            "ban", "timeout", "tempban", "ipban", "tempipban",
            "mute", "tempmute", "warn",
            "unban", "untimeout", "untempban", "unipban", "untempipban",
            "unmute", "untempmute", "unwarn");

    private static final List<String> DURATION_SUGGESTIONS =
            List.of("1h", "6h", "1d", "3d", "7d", "14d", "30d");

    private static final Set<String> NEEDS_PLAYER = Set.of(
            "debug", "alerts", "violations", "setback", "profile",
            "punishments", "warns", "ban", "timeout", "tempban", "ipban",
            "tempipban", "mute", "tempmute", "warn",
            "unban", "untimeout", "untempban", "unipban", "untempipban",
            "unmute", "untempmute", "unwarn");

    private static final Set<String> NEEDS_DURATION = Set.of(
            "timeout", "tempban", "tempipban", "tempmute");

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("snuffac.admin")) {
            return List.of();
        }
        String sub = args.length >= 1 ? args[0].toLowerCase(Locale.ROOT) : "";

        if (args.length == 1) {
            return matching(SUB_COMMANDS, args[0]);
        }
        if (args.length == 2) {
            if (sub.equals("toggle")) {
                return matching(plugin.core().checkKeys(), args[1]);
            }
            if (NEEDS_PLAYER.contains(sub)) {
                return matching(onlineNames(), args[1]);
            }
            if (sub.equals("punishments") || sub.equals("warns") || sub.equals("violations")) {
                return matching(knownNames(), args[1]);
            }
            if (sub.equals("alerts")) {
                return matching(List.of("verbose"), args[1]);
            }
        }
        if (args.length == 3 && NEEDS_DURATION.contains(sub)) {
            return matching(DURATION_SUGGESTIONS, args[2]);
        }
        return List.of();
    }

    private static List<String> matching(Iterable<String> source, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String candidate : source) {
            if (candidate.toLowerCase(Locale.ROOT).startsWith(lower)) {
                result.add(candidate);
            }
        }
        return result;
    }

    private static List<String> onlineNames() {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
    }

    private List<String> knownNames() {
        List<String> names = new ArrayList<>(onlineNames());
        var store = plugin.core().historyStore();
        if (store != null) {
            for (var id : plugin.core().knownPlayerIds()) {
                for (var record : store.history(id)) {
                    String name = record.playerName();
                    if (name != null && !name.isBlank() && !names.contains(name)) {
                        names.add(name);
                    }
                }
            }
        }
        return names;
    }

    private void send(CommandSender sender, String key, Map<String, String> placeholders) {
        raw(sender, Messages.render(key, placeholders));
    }

    private void send(CommandSender sender, String key) {
        send(sender, key, Map.of());
    }

    private static void raw(CommandSender sender, String message) {
        StaffMessages.send(sender, message);
    }

    static String format(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "n/a";
        }
        return String.valueOf(Math.round(value * 100.0) / 100.0);
    }

    UUID uuidOf(Player player) {
        return player.getUniqueId();
    }

    CheckState stateOf(PlayerData data, String key) {
        return data.checkState(key);
    }

    Component component(String text) {
        return Component.text(text);
    }
}
