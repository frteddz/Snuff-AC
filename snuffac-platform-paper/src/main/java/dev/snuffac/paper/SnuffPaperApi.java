package dev.snuffac.paper;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.settings.PacketEventsSettings;
import dev.snuffac.api.SnuffAc;
import dev.snuffac.api.SnuffPlatform;
import dev.snuffac.api.violation.CheckInfo;
import dev.snuffac.api.violation.ViolationInfo;
import dev.snuffac.api.violation.ViolationListener;
import dev.snuffac.core.SnuffCore;
import dev.snuffac.core.config.CheckConfig;
import dev.snuffac.core.config.SnuffConfig;
import dev.snuffac.core.log.SnuffLogger;
import dev.snuffac.core.packet.MovementPacket;
import dev.snuffac.core.packet.SnuffPacket;
import dev.snuffac.core.player.MovementState;
import dev.snuffac.core.player.PlayerData;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class SnuffPaperApi implements SnuffAc {

    private final SnuffPaperPlugin plugin;
    private final Map<ViolationListener, Consumer<ViolationInfo>> listeners = new ConcurrentHashMap<>();

    SnuffPaperApi(SnuffPaperPlugin plugin) {
        this.plugin = plugin;
    }

    public SnuffPaperPlugin plugin() {
        return plugin;
    }

    @Override
    public String version() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public SnuffPlatform platform() {
        return plugin.platform();
    }

    @Override
    public boolean isEnabled() {
        return plugin.core().config().enabled();
    }

    @Override
    public List<CheckInfo> checks() {
        List<CheckInfo> result = new ArrayList<>();
        plugin.core().registry().all().forEach(check -> {
            CheckConfig config = plugin.core().registry().config(check);
            result.add(new CheckInfo(
                    check.key(),
                    check.name(),
                    check.category(),
                    config.enabled(),
                    config.setbackThreshold(),
                    config.action(),
                    config.description()));
        });
        return result;
    }

    @Override
    public Optional<CheckInfo> check(String key) {
        SnuffConfig config = plugin.core().config();
        var check = plugin.core().registry().check(key);
        if (check == null) {
            return Optional.empty();
        }
        CheckConfig checkConfig = plugin.core().registry().config(check);
        return Optional.of(new CheckInfo(
                check.key(),
                check.name(),
                check.category(),
                checkConfig.enabled(),
                checkConfig.setbackThreshold(),
                checkConfig.action(),
                checkConfig.description()));
    }

    @Override
    public void setCheckEnabled(String key, boolean enabled) throws CheckNotFoundException {
        var check = plugin.core().registry().check(key);
        if (check == null) {
            throw new CheckNotFoundException(key);
        }
        plugin.setCheckEnabled(check.key(), enabled);
    }

    @Override
    public void addListener(ViolationListener listener) {
        Consumer<ViolationInfo> consumer = listener::onViolation;
        listeners.put(listener, consumer);
        plugin.core().addListener(consumer);
    }

    @Override
    public void removeListener(ViolationListener listener) {
        Consumer<ViolationInfo> consumer = listeners.remove(listener);
        if (consumer != null) {
            plugin.core().removeListener(consumer);
        }
    }

    @Override
    public void reload() {
        plugin.reloadConfiguration();
    }

    @Override
    public List<ViolationInfo> recentViolations(UUID playerId, int limit) {
        return plugin.core().violations().recent(playerId, limit);
    }

    static String normalise(String key) {
        return key == null ? "" : key.toLowerCase(Locale.ROOT);
    }
}
