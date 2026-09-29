package dev.snuffac.paper;

import dev.snuffac.core.punish.Durations;
import dev.snuffac.core.punish.PunishmentService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

final class PunishCommands {

    static final Set<String> PUNISH = Set.of(
            "ban", "timeout", "tempban", "ipban", "tempipban", "mute", "tempmute", "warn");
    static final Set<String> REVERSE = Set.of(
            "unban", "untimeout", "untempban", "unipban", "untempipban", "unmute", "untempmute", "unwarn");

    private final SnuffPaperPlugin plugin;
    private final Plugin owner;

    PunishCommands(SnuffPaperPlugin plugin) {
        this.plugin = plugin;
        this.owner = plugin;
    }

    static String permissionFor(String sub) {
        return switch (sub) {
            case "ban", "tempban" -> "snuffac.punish.ban";
            case "timeout" -> "snuffac.punish.kick";
            case "mute", "tempmute" -> "snuffac.punish.mute";
            case "warn" -> "snuffac.punish.warn";
            default -> "snuffac.punish.ipban";
        };
    }

    private static PunishmentService.Kind kindFor(String sub) {
        return switch (sub) {
            case "ban" -> PunishmentService.Kind.BAN;
            case "tempban" -> PunishmentService.Kind.TEMPBAN;
            case "timeout" -> PunishmentService.Kind.TIMEOUT;
            case "ipban" -> PunishmentService.Kind.IPBAN;
            case "tempipban" -> PunishmentService.Kind.TEMPIPBAN;
            case "mute" -> PunishmentService.Kind.MUTE;
            case "tempmute" -> PunishmentService.Kind.TEMPMUTE;
            case "warn" -> PunishmentService.Kind.WARN;
            default -> PunishmentService.Kind.WARN;
        };
    }

    private static String reverseKindFor(String sub) {
        return switch (sub) {
            case "unban" -> "BAN";
            case "untempban" -> "TEMPBAN";
            case "untimeout" -> "TIMEOUT";
            case "unipban" -> "IPBAN";
            case "untempipban" -> "TEMPIPBAN";
            case "unmute" -> "MUTE";
            case "untempmute" -> "TEMPMUTE";
            case "unwarn" -> "WARN";
            default -> "";
        };
    }

    private static boolean needsDuration(String sub) {
        return sub.equals("tempban") || sub.equals("timeout")
                || sub.equals("tempipban") || sub.equals("tempmute");
    }

    boolean execute(CommandSender sender, String sub, String[] args) {
        if (args.length < 2) {
            msg(sender, "punish-usage", "<use>", usageFor(sub));
            return true;
        }
        String targetName = args[1];
        long duration = 0L;
        int reasonIndex = 2;

        if (needsDuration(sub)) {
            if (args.length < 3) {
                msg(sender, "punish-usage", "<use>", usageFor(sub));
                return true;
            }
            try {
                duration = Durations.parseMillis(args[2]);
            } catch (IllegalArgumentException invalid) {
                msg(sender, "bad-duration", "<input>", args[2], "<why>", invalid.getMessage());
                return true;
            }
            reasonIndex = 3;
        }

        String reason = join(args, reasonIndex);
        if (reason.isBlank()) {
            msg(sender, "punish-no-reason", "<use>", usageFor(sub));
            return true;
        }

        OfflinePlayer target = resolve(targetName);
        if (target == null) {
            msg(sender, "punish-unknown", "<player>", targetName);
            return true;
        }
        UUID id = target.getUniqueId();
        String name = target.getName() == null ? targetName : target.getName();

        if (target instanceof Player online && online.hasPermission("snuffac.exempt.punish")) {
            msg(sender, "punish-protected", "<player>", name);
            return true;
        }

        if (duration > 0L && !withinStaffCap(sender, duration)) {
            msg(sender, "punish-too-long", "<duration>", Durations.describe(duration));
            return true;
        }

        PunishmentService.Kind kind = kindFor(sub);
        String ipHash = "";
        if (kind == PunishmentService.Kind.IPBAN || kind == PunishmentService.Kind.TEMPIPBAN) {
            Player online = Bukkit.getPlayer(id);
            if (online != null && online.getAddress() != null) {
                ipHash = PunishmentService.hashIp(online.getAddress().getAddress().getHostAddress());
            } else {
                ipHash = PunishmentService.hashIp(targetName);
            }
        }

        var record = plugin.punishments().punish(kind, id, name, ipHash, reason, sender.getName(), duration);
        applyOnline(target, kind, true);

        msg(sender, "punish-applied", Map.of(
                "type", kind.label(),
                "player", name,
                "duration", Durations.describe(duration),
                "reason", reason,
                "id", record.id().toString().substring(0, 8)));
        return true;
    }

    boolean reverse(CommandSender sender, String sub, String[] args) {
        if (args.length < 2) {
            msg(sender, "punish-usage", "<use>", usageFor(sub));
            return true;
        }
        OfflinePlayer target = resolve(args[1]);
        if (target == null) {
            msg(sender, "punish-unknown", "<player>", args[1]);
            return true;
        }
        UUID id = target.getUniqueId();
        String raw = reverseKindFor(sub);
        boolean removeAll = raw.equals("BAN") || raw.equals("IPBAN") || raw.equals("MUTE");
        PunishmentService.Kind[] kinds = removeAll
                ? new PunishmentService.Kind[] {
                        PunishmentService.Kind.valueOf(raw),
                        PunishmentService.Kind.valueOf("TEMP" + raw) }
                : new PunishmentService.Kind[] { PunishmentService.Kind.valueOf(raw) };

        int removed = 0;
        for (PunishmentService.Kind kind : kinds) {
            removed += plugin.punishments().remove(id, kind, sender.getName()).size();
        }
        if (removed == 0) {
            msg(sender, "reverse-none", "<player>", target.getName() == null ? args[1] : target.getName());
            return true;
        }
        Player online = Bukkit.getPlayer(id);
        if (online != null) {
            boolean stillBanned = plugin.punishments().isBanned(id);
            applyOnline(online, PunishmentService.Kind.BAN, stillBanned);
        }
        msg(sender, "reverse-applied", Map.of(
                "type", sub.replace("un", ""),
                "player", target.getName() == null ? args[1] : target.getName(),
                "count", String.valueOf(removed)));
        return true;
    }

    boolean view(CommandSender sender, String[] args) {
        if (args.length < 2) {
            List<Player> online = new java.util.ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!plugin.punishments().active(player.getUniqueId()).isEmpty()) {
                    online.add(player);
                }
            }
            if (online.isEmpty()) {
                msg(sender, "punish-none-online");
                return true;
            }
            msg(sender, "punish-online-header", Map.of("count", String.valueOf(online.size())));
            for (Player player : online) {
                sender.sendMessage(net.kyori.adventure.text.Component.text(
                        "  " + player.getName() + ": " + describeAll(player.getUniqueId())));
            }
            return true;
        }
        OfflinePlayer target = resolve(args[1]);
        if (target == null) {
            msg(sender, "punish-unknown", "<player>", args[1]);
            return true;
        }
        UUID id = target.getUniqueId();
        List<PunishmentService.Punishment> active = plugin.punishments().active(id);
        String name = target.getName() == null ? args[1] : target.getName();
        if (active.isEmpty()) {
            msg(sender, "punish-none", "<player>", name);
            return true;
        }
        msg(sender, "punish-list-header", Map.of("player", name, "count", String.valueOf(active.size())));
        for (PunishmentService.Punishment record : active) {
            sender.sendMessage(net.kyori.adventure.text.Component.text(
                    "  " + record.kind().label()
                            + " | " + (record.expiresMillis() == 0L
                                    ? "permanent"
                                    : Durations.describe(record.expiresMillis() - System.currentTimeMillis()))
                            + " left | " + record.reason()
                            + " | by " + record.staff()));
        }
        return true;
    }

    boolean warns(CommandSender sender, String[] args) {
        if (args.length < 2) {
            msg(sender, "punish-usage", "<use>", "/snuff warns <player>");
            return true;
        }
        OfflinePlayer target = resolve(args[1]);
        if (target == null) {
            msg(sender, "punish-unknown", "<player>", args[1]);
            return true;
        }
        List<PunishmentService.Punishment> warnings = new java.util.ArrayList<>();
        for (PunishmentService.Punishment record : plugin.punishments().history(target.getUniqueId())) {
            if (record.kind() == PunishmentService.Kind.WARN) {
                warnings.add(record);
            }
        }
        String name = target.getName() == null ? args[1] : target.getName();
        if (warnings.isEmpty()) {
            msg(sender, "warn-none", "<player>", name);
            return true;
        }
        msg(sender, "warn-header", Map.of("player", name, "count", String.valueOf(warnings.size())));
        for (PunishmentService.Punishment record : warnings) {
            sender.sendMessage(net.kyori.adventure.text.Component.text(
                    "  " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm")
                            .format(new java.util.Date(record.createdMillis()))
                            + " | " + record.reason()
                            + " | by " + record.staff()));
        }
        return true;
    }

    boolean settings(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            msg(sender, "gui-needs-player");
            return true;
        }
        plugin.openSettings(player);
        return true;
    }

    private void applyOnline(OfflinePlayer target, PunishmentService.Kind kind, boolean active) {
        Player online = target instanceof Player player ? player : Bukkit.getPlayer(target.getUniqueId());
        if (online == null) {
            return;
        }
        switch (kind) {
            case BAN, TEMPBAN, IPBAN, TEMPIPBAN -> {
                if (active) {
                    online.kick(net.kyori.adventure.text.Component.text(
                            "You are banned. Reason: see /snuff punishments"));
                }
            }
            default -> {
            }
        }
    }

    private String describeAll(UUID id) {
        StringBuilder builder = new StringBuilder();
        for (PunishmentService.Punishment record : plugin.punishments().active(id)) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(record.kind().label());
        }
        return builder.toString();
    }

    private OfflinePlayer resolve(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online;
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayerIfCached(name);
        if (offline != null) {
            return offline;
        }
        return null;
    }

    private static boolean withinStaffCap(CommandSender sender, long duration) {
        if (sender.hasPermission("snuffac.punish.maxduration.unlimited")) {
            return true;
        }
        if (duration <= 3_600_000L && sender.hasPermission("snuffac.punish.maxduration.1h")) {
            return true;
        }
        if (duration <= 86_400_000L && sender.hasPermission("snuffac.punish.maxduration.1d")) {
            return true;
        }
        if (duration <= 604_800_000L && sender.hasPermission("snuffac.punish.maxduration.7d")) {
            return true;
        }
        return duration <= 2_592_000_000L && sender.hasPermission("snuffac.punish.maxduration.30d");
    }

    static String usageFor(String sub) {
        return switch (sub) {
            case "warn" -> "/snuff warn <player> <reason>";
            case "ban", "mute" -> "/snuff " + sub + " <player> <reason>";
            default -> "/snuff " + sub + " <player> <duration m/h/d> <reason>";
        };
    }

    private static String join(String[] args, int from) {
        StringBuilder builder = new StringBuilder();
        for (int i = from; i < args.length; i++) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(args[i]);
        }
        return builder.toString().trim();
    }

    private void msg(CommandSender sender, String key, String... pairs) {
        Map<String, String> values = new java.util.HashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            values.put(pairs[i], pairs[i + 1]);
        }
        msg(sender, key, values);
    }

    private void msg(CommandSender sender, String key, Map<String, String> values) {
        sender.sendMessage(net.kyori.adventure.text.Component.text(
                "[Snuff] " + Messages.render(key, values)));
    }
}
