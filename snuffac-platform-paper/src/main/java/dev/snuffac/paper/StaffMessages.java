package dev.snuffac.paper;

import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class StaffMessages {

    public static final String PREFIX = LegacyColour.PREFIX;

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private StaffMessages() {
    }

    public static Component render(String body) {
        String joined = LegacyColour.joined(body);
        try {
            return MINI.deserialize(joined);
        } catch (RuntimeException exception) {
            return Component.text(stripped(joined));
        }
    }

    public static void send(CommandSender sender, String body) {
        sender.sendMessage(render(body));
    }

    public static void send(CommandSender sender, String key, Map<String, String> placeholders) {
        send(sender, Messages.render(key, placeholders));
    }

    public static void sendToAll(java.util.Collection<? extends Player> players, String body) {
        Component component = render(body);
        for (Player player : players) {
            player.sendMessage(component);
        }
    }

    private static String stripped(String input) {
        StringBuilder builder = new StringBuilder(input.length());
        int index = 0;
        while (index < input.length()) {
            char c = input.charAt(index);
            if (c == '<') {
                int close = input.indexOf('>', index);
                if (close > index) {
                    index = close + 1;
                    continue;
                }
            }
            builder.append(c);
            index++;
        }
        return builder.toString().trim();
    }
}
