package dev.snuffac.velocity;

import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.settings.PacketEventsSettings;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.snuffac.api.SnuffPlatform;
import dev.snuffac.core.SnuffCore;
import dev.snuffac.core.config.ConfigSource;
import dev.snuffac.core.log.SnuffLogger;
import dev.snuffac.core.packet.KeepAlivePacket;
import dev.snuffac.core.packet.MovementPacket;
import dev.snuffac.core.packet.ServerKeepAlivePacket;
import dev.snuffac.core.packet.SnuffPacket;
import dev.snuffac.core.player.PlayerData;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;

@Plugin(
        id = "snuffac",
        name = "SnuffAC",
        version = "1.0.2-dev",
        description = "Snuff AC, an independent movement and combat anticheat.",
        authors = {"Snuff"})
public final class SnuffVelocityPlugin {

    private final Logger logger;
    private final ProxyServer proxy;
    private final Path dataDirectory;

    private final Map<UUID, UUID> userToPlayer = new ConcurrentHashMap<>();
    private final Map<UUID, PendingKeepAlive> pendingKeepAlives = new ConcurrentHashMap<>();

    private SnuffCore core;
    private PacketEventsAPI<?> packetEvents;

    @Inject
    public SnuffVelocityPlugin(Logger logger, ProxyServer proxy, @DataDirectory Path dataDirectory) {
        this.logger = logger;
        this.proxy = proxy;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        bootstrap();
    }

    @Subscribe
    public void onServerConnect(ServerPostConnectEvent event) {
        if (core == null) {
            return;
        }
        Player player = event.getPlayer();
        if (core.player(player.getUniqueId()) != null) {
            return;
        }
        PlayerData data = core.addPlayer(player.getUniqueId(), player.getUsername(),
                System.currentTimeMillis());
        data.platformPlayer(player);
        data.alive(true);
        data.network().recordPing(Math.max(0L, player.getPing()));
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        Player player = event.getPlayer();
        if (core != null) {
            core.removePlayer(player.getUniqueId());
        }
        userToPlayer.values().removeIf(value -> value.equals(player.getUniqueId()));
        pendingKeepAlives.remove(player.getUniqueId());
    }

    private synchronized void bootstrap() {
        if (core != null) {
            return;
        }
        this.core = new SnuffCore(SnuffPlatform.VELOCITY, new VelocityLogger(logger));
        this.core.boot(
                new VelocityMessenger(proxy),
                new VelocityPermissions(),
                ConfigSource.ofMap(Map.of()),
                dataDirectory.resolve("logs"));
        logger.info("Snuff AC core started with {} checks.", core.checkKeys().size());

        try {
            var container = proxy.getPluginManager().fromInstance(this)
                    .orElse(null);
            if (container == null) {
                logger.warn("plugin container unavailable, packet layer disabled");
                return;
            }
            PacketEventsSettings settings = new PacketEventsSettings()
                    .checkForUpdates(false)
                    .bStats(false);
            this.packetEvents = io.github.retrooper.packetevents.velocity.factory.VelocityPacketEventsBuilder
                    .buildNoCache(proxy, container, logger, dataDirectory, settings);
            com.github.retrooper.packetevents.PacketEvents.setAPI(this.packetEvents);
            this.packetEvents.load();
            this.packetEvents.getEventManager().registerListener(
                    new SnuffVelocityPacketListener(new VelocityPacketTranslator(), this::onPacket));
            logger.info("Packet layer ready, packet based checks active.");
        } catch (RuntimeException | NoClassDefFoundError exception) {
            logger.warn("packet layer unavailable on this proxy, packet checks are disabled", exception);
        }
    }

    private void onPacket(User user, SnuffPacket packet) {
        if (core == null) {
            return;
        }
        UUID playerId = resolvePlayerId(user);
        if (playerId == null) {
            return;
        }
        PlayerData data = core.player(playerId);
        if (data == null) {
            return;
        }

        if (packet instanceof MovementPacket movement) {
            var state = data.movement();
            if (movement.positionChanged()) {
                state.advanceTo(movement.position(), movement.arrivalNanos());
                state.advanceGround(movement.onGround());
                state.horizontalCollision(movement.horizontalCollision());
            }
            if (movement.rotationChanged()) {
                state.advanceRotation(movement.yaw(), movement.pitch(), movement.arrivalNanos());
            }
        } else if (packet instanceof ServerKeepAlivePacket keepAlive) {
            pendingKeepAlives.put(playerId,
                    new PendingKeepAlive(keepAlive.payload(), System.currentTimeMillis()));
        } else if (packet instanceof KeepAlivePacket response) {
            PendingKeepAlive pending = pendingKeepAlives.remove(playerId);
            if (pending != null) {
                data.network().receivedKeepAlive(pending.id(), System.currentTimeMillis());
            }
        }

        data.network().packetReceived(packet.arrivalNanos());
        core.enqueue(playerId, packet);
    }

    private UUID resolvePlayerId(User user) {
        UUID cached = userToPlayer.get(user.getUUID());
        if (cached != null) {
            return cached;
        }
        String name = user.getName();
        if (name == null) {
            return null;
        }
        for (Player player : proxy.getAllPlayers()) {
            if (player.getUsername().equalsIgnoreCase(name)) {
                userToPlayer.put(user.getUUID(), player.getUniqueId());
                return player.getUniqueId();
            }
        }
        return null;
    }

    public SnuffCore core() {
        return core;
    }

    static final class PendingKeepAlive {

        private final long id;
        private final long sentMillis;

        PendingKeepAlive(long id, long sentMillis) {
            this.id = id;
            this.sentMillis = sentMillis;
        }

        long id() {
            return id;
        }

        long sentMillis() {
            return sentMillis;
        }
    }

    private static final class VelocityLogger implements SnuffLogger {

        private final Logger delegate;

        VelocityLogger(Logger delegate) {
            this.delegate = delegate;
        }

        @Override
        public void info(String message) {
            delegate.info("[Snuff] {}", message);
        }

        @Override
        public void warn(String message) {
            delegate.warn("[Snuff] {}", message);
        }

        @Override
        public void severe(String message) {
            delegate.error("[Snuff] {}", message);
        }

        @Override
        public void debug(String message) {
            delegate.debug("[Snuff] {}", message);
        }

        @Override
        public void violation(String message) {
            delegate.info("[Snuff] [violation] {}", message);
        }
    }

    private static final class VelocityMessenger implements dev.snuffac.core.platform.SnuffMessenger {

        private final ProxyServer proxy;
        private final org.slf4j.Logger log;

        VelocityMessenger(ProxyServer proxy) {
            this.proxy = proxy;
            this.log = org.slf4j.LoggerFactory.getLogger("snuffac");
        }

        @Override
        public void sendMessage(Object handle, String message) {
            if (handle instanceof Player player) {
                player.sendMessage(net.kyori.adventure.text.Component.text(message));
            }
        }

        @Override
        public void broadcast(String message, String permission) {
            Component component = net.kyori.adventure.text.Component.text(message);
            proxy.getAllPlayers().forEach(player -> {
                try {
                    if (permission == null || permission.isEmpty() || player.hasPermission(permission)) {
                        player.sendMessage(component);
                    }
                } catch (RuntimeException exception) {
                    log.warn("alert delivery failed: " + exception);
                }
            });
        }

        @Override
        public void sendConsole(String message) {
            log.info(message);
        }
    }

    private static final class VelocityPermissions implements dev.snuffac.core.platform.SnuffPermissionChecker {

        @Override
        public boolean hasPermission(Object handle, String permission) {
            return handle instanceof Player player && player.hasPermission(permission);
        }

        @Override
        public List<String> playersWithPermission(String permission) {
            return List.of();
        }
    }
}
