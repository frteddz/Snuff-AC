package dev.snuffac.paper;

import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSoundEffect;
import dev.snuffac.core.config.SnuffConfig;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.entity.Player;

public final class SoundConcealment {

    private final SnuffPaperPlugin plugin;
    private final SnuffConfig config;

    public SoundConcealment(SnuffPaperPlugin plugin, SnuffConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public boolean enabled() {
        return config.visualSoundFuzzing()
                && config.antiXrayMode() != null
                && config.antiXrayMode() != dev.snuffac.core.world.ObfuscationPolicy.OFF;
    }

    public static boolean carriesPosition(String soundName) {
        if (soundName == null) {
            return false;
        }
        String value = soundName.toLowerCase(Locale.ROOT);
        return value.contains("step")
                || value.contains("eat")
                || value.contains("drink")
                || value.contains("burp")
                || value.contains("bow")
                || value.contains("attack")
                || value.contains("breath")
                || value.contains("swoosh")
                || value.contains("splash")
                || value.contains("swim")
                || value.contains("armor");
    }

    public boolean rewrite(User user, WrapperPlayServerSoundEffect wrapper) {
        if (!enabled()) {
            return false;
        }
        Vector3d position = wrapper.getPosition();
        if (position == null) {
            return false;
        }
        String name = wrapper.getSound() == null ? "" : wrapper.getSound().getName().toString();
        if (!carriesPosition(name)) {
            return false;
        }
        Player listener = plugin.playerOf(user);
        if (listener == null || !listener.isOnline()) {
            return false;
        }
        var data = plugin.dataOf(listener.getUniqueId());
        if (data == null || data.exempt()) {
            return false;
        }
        double range = config.visualSoundJitter();
        if (range <= 0.0) {
            return false;
        }
        var origin = new org.bukkit.Location(
                listener.getWorld(), position.getX(), position.getY(), position.getZ());
        if (LineOfSight.clearAt(listener, origin)) {
            return false;
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        wrapper.setPosition(new Vector3d(
                origin.getX() + (random.nextDouble() * 2.0 - 1.0) * range,
                origin.getY() + (random.nextDouble() * 2.0 - 1.0) * range,
                origin.getZ() + (random.nextDouble() * 2.0 - 1.0) * range));
        return true;
    }
}
