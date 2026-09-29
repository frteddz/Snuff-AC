package dev.snuffac.paper;

import dev.snuffac.core.punish.Durations;
import dev.snuffac.core.punish.PunishmentService;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerKickEvent;

public final class PunishmentEnforcement implements Listener {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private final SnuffPaperPlugin plugin;

    public PunishmentEnforcement(SnuffPaperPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            return;
        }
        PunishmentService.Punishment ban = activeBan(event.getUniqueId());
        if (ban == null) {
            return;
        }
        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                component(screen(ban)));
    }

    private PunishmentService.Punishment activeBan(java.util.UUID id) {
        if (id == null) {
            return null;
        }
        long now = System.currentTimeMillis();
        List<PunishmentService.Punishment> active = plugin.punishments().active(id);
        for (PunishmentService.Punishment record : active) {
            if (!record.live(now)) {
                continue;
            }
            if (record.kind() == PunishmentService.Kind.BAN
                    || record.kind() == PunishmentService.Kind.TEMPBAN) {
                return record;
            }
        }
        return null;
    }

    public static String screen(PunishmentService.Punishment ban) {
        boolean permanent = ban.expiresMillis() <= 0L;
        StringBuilder message = new StringBuilder();
        message.append("<gradient:#EE6832:#FCE6DD><bold>");
        message.append(permanent ? "You are banned" : "You are temporarily banned");
        message.append("</bold></gradient>\n\n");
        message.append("<gray>Staff: </gray><white>")
                .append(escape(ban.staff()))
                .append("</white>\n");
        if (!permanent) {
            message.append("<gray>Expires: </gray><white>")
                    .append(STAMP.format(Instant.ofEpochMilli(ban.expiresMillis())))
                    .append("</white> <gray>(")
                    .append(Durations.describe(ban.expiresMillis() - System.currentTimeMillis()))
                    .append(" remaining)</gray>\n");
        }
        message.append("<gray>Reason: </gray><white>")
                .append(escape(ban.reason()))
                .append("</white>\n\n");
        message.append("<gray>If you believe this is a mistake, contact the admins.</gray>");
        return message.toString();
    }

    public static String kickScreen(String reason, String staff) {
        StringBuilder message = new StringBuilder();
        message.append("<gradient:#EE6832:#FCE6DD><bold>You were kicked</bold></gradient>\n\n");
        if (staff != null && !staff.isBlank()) {
            message.append("<gray>Staff: </gray><white>").append(escape(staff)).append("</white>\n");
        }
        if (reason != null && !reason.isBlank()) {
            message.append("<gray>Reason: </gray><white>").append(escape(reason)).append("</white>\n");
        }
        message.append("\n<gray>Contact the admins if you believe this is a mistake.</gray>");
        return message.toString();
    }

    public static String muteScreen(String reason, long expiresMillis) {
        StringBuilder message = new StringBuilder();
        message.append("<gradient:#EE6832:#FCE6DD><bold>You are muted</bold></gradient>\n\n");
        if (expiresMillis > 0L) {
            message.append("<gray>Until: </gray><white>")
                    .append(STAMP.format(Instant.ofEpochMilli(expiresMillis)))
                    .append("</white>\n");
        } else {
            message.append("<gray>Length: </gray><white>permanent</white>\n");
        }
        if (reason != null && !reason.isBlank()) {
            message.append("<gray>Reason: </gray><white>").append(escape(reason)).append("</white>");
        }
        return message.toString();
    }

    public static String escape(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '<' || c == '>' || c == '&' || c == '\n' || c == '\r') {
                continue;
            }
            builder.append(c);
        }
        return builder.toString();
    }

    private static Component component(String miniMessage) {
        try {
            return MINI.deserialize(miniMessage);
        } catch (RuntimeException exception) {
            return Component.text(miniMessage);
        }
    }
}
