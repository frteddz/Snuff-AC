package dev.snuffac.paper;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.core.config.CheckConfig;
import dev.snuffac.core.player.PlayerData;
import dev.snuffac.core.violation.CheckState;
import dev.snuffac.core.violation.ViolationRecord;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final String PREFIX = "<gray>[</gray><red>Snuff</red><gray>]</gray> ";

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
            if (!sender.hasPermission("snuffac.admin")) {
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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("snuffac.admin")) {
            return List.of();
        }
        List<String> subs = new ArrayList<>(Arrays.asList(
                "info", "version", "reload", "debug", "alerts", "checks", "violations",
                "toggle", "stats", "setback", "profile"));
        if (args.length == 1) {
            return subs.stream().filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT))).collect(Collectors.toList());
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("toggle")) {
                return plugin.core().checkKeys().stream()
                        .filter(k -> k.startsWith(args[1].toLowerCase(Locale.ROOT)))
                        .collect(Collectors.toList());
            }
            if (sub.equals("debug") || sub.equals("alerts") || sub.equals("violations")
                    || sub.equals("setback") || sub.equals("profile")) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                        .collect(Collectors.toList());
            }
        }
        return List.of();
    }

    private void send(CommandSender sender, String key, Map<String, String> placeholders) {
        raw(sender, Messages.render(key, placeholders));
    }

    private void send(CommandSender sender, String key) {
        send(sender, key, Map.of());
    }

    private static void raw(CommandSender sender, String message) {
        try {
            sender.sendMessage(MINI.deserialize(PREFIX + message));
        } catch (RuntimeException exception) {
            sender.sendMessage(PREFIX + message);
        }
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
