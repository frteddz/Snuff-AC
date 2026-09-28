package dev.snuffac.core;

import dev.snuffac.api.SnuffPlatform;
import dev.snuffac.api.violation.ViolationInfo;
import dev.snuffac.core.alert.AlertService;
import dev.snuffac.core.check.Check;
import dev.snuffac.core.check.CheckDispatcher;
import dev.snuffac.core.check.CheckRegistry;
import dev.snuffac.core.check.impl.combat.AutoClickerCheck;
import dev.snuffac.core.check.impl.combat.AimCheck;
import dev.snuffac.core.check.impl.combat.ImpossibleAttackCheck;
import dev.snuffac.core.check.impl.combat.InvalidAttackStateCheck;
import dev.snuffac.core.check.impl.combat.KillAuraCheck;
import dev.snuffac.core.check.impl.combat.ReachCheck;
import dev.snuffac.core.check.impl.movement.AirMovementCheck;
import dev.snuffac.core.check.impl.movement.FlyCheck;
import dev.snuffac.core.check.impl.movement.GroundSpoofCheck;
import dev.snuffac.core.check.impl.movement.HighJumpCheck;
import dev.snuffac.core.check.impl.movement.ImpossibleMovementCheck;
import dev.snuffac.core.check.impl.movement.LongJumpCheck;
import dev.snuffac.core.check.impl.movement.NoFallCheck;
import dev.snuffac.core.check.impl.movement.SpeedCheck;
import dev.snuffac.core.check.impl.movement.StepCheck;
import dev.snuffac.core.check.impl.movement.VelocityCheck;
import dev.snuffac.core.check.impl.net.BadPacketsCheck;
import dev.snuffac.core.check.impl.net.PacketSpamCheck;
import dev.snuffac.core.check.impl.net.TimerCheck;
import dev.snuffac.core.check.impl.world.FastBreakCheck;
import dev.snuffac.core.check.impl.world.FastPlaceCheck;
import dev.snuffac.core.check.impl.world.NukerCheck;
import dev.snuffac.core.check.impl.world.ScaffoldCheck;
import dev.snuffac.core.config.ConfigSource;
import dev.snuffac.core.config.SnuffConfig;
import dev.snuffac.core.log.FileViolationLogger;
import dev.snuffac.core.log.SnuffLogger;
import dev.snuffac.core.packet.SnuffPacket;
import dev.snuffac.core.platform.SnuffMessenger;
import dev.snuffac.core.platform.SnuffPermissionChecker;
import dev.snuffac.core.player.PlayerData;
import dev.snuffac.core.server.ServerHealth;
import dev.snuffac.core.tolerance.ToleranceModel;
import dev.snuffac.core.violation.CheckState;
import dev.snuffac.core.violation.ViolationHandler;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class SnuffCore {

    private static final int MAX_QUEUE_DRAIN = 512;

    private final SnuffPlatform platform;
    private final SnuffLogger logger;
    private final SnuffConfig config = new SnuffConfig();
    private final CheckRegistry registry = new CheckRegistry();
    private final ServerHealth server = new ServerHealth();
    private final Map<UUID, PlayerData> players = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<PacketTask> inbound = new ConcurrentLinkedQueue<>();
    private final List<Consumer<ViolationInfo>> apiListeners = new CopyOnWriteArrayList<>();

    private volatile CheckDispatcher dispatcher;
    private volatile ViolationHandler violations;
    private volatile AlertService alerts;
    private volatile FileViolationLogger fileLogger;
    private volatile boolean running;
    private volatile long tickCounter;

    private Thread checkThread;

    public SnuffCore(SnuffPlatform platform, SnuffLogger logger) {
        this.platform = platform;
        this.logger = logger;
        registerDefaults();
    }

    private void registerDefaults() {
        registry.register(new FlyCheck());
        registry.register(new SpeedCheck());
        registry.register(new NoFallCheck());
        registry.register(new AirMovementCheck());
        registry.register(new GroundSpoofCheck());
        registry.register(new StepCheck());
        registry.register(new HighJumpCheck());
        registry.register(new LongJumpCheck());
        registry.register(new ImpossibleMovementCheck());
        registry.register(new VelocityCheck());

        registry.register(new ReachCheck());
        registry.register(new AutoClickerCheck());
        registry.register(new AimCheck());
        registry.register(new KillAuraCheck());
        registry.register(new ImpossibleAttackCheck());
        registry.register(new InvalidAttackStateCheck());

        registry.register(new FastBreakCheck());
        registry.register(new FastPlaceCheck());
        registry.register(new ScaffoldCheck());
        registry.register(new NukerCheck());

        registry.register(new BadPacketsCheck());
        registry.register(new PacketSpamCheck());
        registry.register(new TimerCheck());

        registry.freeze();
    }

    public void boot(
            SnuffMessenger messenger,
            SnuffPermissionChecker permissions,
            ConfigSource source,
            Path logDirectory) {

        this.config.load(source);
        this.registry.loadConfigurations(source);
        this.fileLogger = config.logToFile() ? new FileViolationLogger(logDirectory, 30) : null;
        this.alerts = new AlertService(config, messenger, permissions, logger, fileLogger);
        this.violations = new ViolationHandler(config, server, alerts, logger, platform);
        this.violations.addListener(this::forwardToApi);
        this.dispatcher = new CheckDispatcher(registry, config, server, violations, logger);
        startCheckThread();
    }

    private void forwardToApi(ViolationInfo info) {
        violations.remember(info);
        for (Consumer<ViolationInfo> listener : apiListeners) {
            try {
                listener.accept(info);
            } catch (RuntimeException exception) {
                logger.warn("api listener failed: " + exception);
            }
        }
    }

    private void startCheckThread() {
        running = true;
        checkThread = new Thread(this::checkLoop, "SnuffAC-Check");
        checkThread.setDaemon(true);
        checkThread.setPriority(Thread.NORM_PRIORITY + 1);
        checkThread.start();
    }

    private void checkLoop() {
        while (running) {
            try {
                Thread.sleep(2L);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return;
            }
            processQueue();
        }
    }

    public int processQueue() {
        CheckDispatcher active = dispatcher;
        if (active == null) {
            inbound.clear();
            return 0;
        }
        int processed = 0;
        int budget = MAX_QUEUE_DRAIN;
        PacketTask task;
        while (budget-- > 0 && (task = inbound.poll()) != null) {
            processed++;
            PlayerData player = players.get(task.playerId());
            if (player == null || !player.alive()) {
                continue;
            }
            try {
                active.dispatchPacket(player, task.packet());
            } catch (RuntimeException exception) {
                logger.warn("packet processing failed for " + player.name() + ": " + exception);
            }
        }
        return processed;
    }

    public void enqueue(UUID playerId, SnuffPacket packet) {
        if (running) {
            inbound.offer(new PacketTask(playerId, packet));
        }
    }

    public void tick() {
        server.tick();
        long current = ++tickCounter;
        for (PlayerData player : new ArrayList<>(players.values())) {
            if (!player.alive()) {
                continue;
            }
            try {
                player.movement().tolerance().advanceTick(current);
                player.tickCounters(System.currentTimeMillis());
                CheckDispatcher active = dispatcher;
                if (active != null) {
                    active.dispatchTick(player);
                }
            } catch (RuntimeException exception) {
                logger.warn("tick failed for " + player.name() + ": " + exception);
            }
        }
    }

    public PlayerData addPlayer(UUID id, String name, long joinMillis) {
        PlayerData player = new PlayerData(id, name, joinMillis, newTolerance());
        for (Check check : registry.all()) {
            player.registerCheck(check.key(), new CheckState(check.key(), registry.config(check)));
            Object checkState = check.createState();
            if (checkState != null) {
                player.putCheckData(check.key(), checkState);
            }
        }
        player.joined(true);
        players.put(id, player);
        CheckDispatcher active = dispatcher;
        if (active != null) {
            active.dispatchJoin(player);
        }
        return player;
    }

    public void removePlayer(UUID id) {
        PlayerData player = players.remove(id);
        if (player == null) {
            return;
        }
        player.alive(false);
        CheckDispatcher active = dispatcher;
        if (active != null) {
            active.dispatchQuit(player);
        }
        if (violations != null) {
            violations.forget(id);
        }
    }

    private ToleranceModel newTolerance() {
        return new ToleranceModel(
                config.toleranceDecayPerTick(),
                config.toleranceMaximum(),
                config.toleranceCarryOverCap(),
                config.toleranceCarryOverRetention());
    }

    public PlayerData player(UUID id) {
        return players.get(id);
    }

    public PlayerData playerByName(String name) {
        if (name == null) {
            return null;
        }
        for (PlayerData player : players.values()) {
            if (name.equalsIgnoreCase(player.name())) {
                return player;
            }
        }
        return null;
    }

    public Collection<PlayerData> players() {
        return players.values();
    }

    public SnuffConfig config() {
        return config;
    }

    public CheckRegistry registry() {
        return registry;
    }

    public CheckDispatcher dispatcher() {
        return dispatcher;
    }

    public ServerHealth server() {
        return server;
    }

    public AlertService alerts() {
        return alerts;
    }

    public ViolationHandler violations() {
        return violations;
    }

    public SnuffPlatform platform() {
        return platform;
    }

    public SnuffLogger logger() {
        return logger;
    }

    public long tickCounter() {
        return tickCounter;
    }

    public void reload(ConfigSource source) {
        config.load(source);
        registry.loadConfigurations(source);
        if (alerts != null) {
            alerts.onReload();
            alerts.clearCooldowns();
        }
    }

    public void addListener(Consumer<ViolationInfo> listener) {
        apiListeners.add(listener);
    }

    public void removeListener(Consumer<ViolationInfo> listener) {
        apiListeners.remove(listener);
    }

    public List<String> checkKeys() {
        List<String> keys = new ArrayList<>();
        for (String key : registry.configurations().keySet()) {
            keys.add(key.toLowerCase(Locale.ROOT));
        }
        return keys;
    }

    public void shutdown() {
        running = false;
        if (checkThread != null) {
            checkThread.interrupt();
        }
        if (fileLogger != null) {
            fileLogger.close();
        }
    }

    private record PacketTask(UUID playerId, SnuffPacket packet) {
    }
}
