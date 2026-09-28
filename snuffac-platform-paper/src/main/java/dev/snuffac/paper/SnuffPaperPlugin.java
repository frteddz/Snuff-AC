package dev.snuffac.paper;

import com.github.retrooper.packetevents.PacketEventsAPI;
import dev.snuffac.core.config.CheckConfig;
import java.util.function.Consumer;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.settings.PacketEventsSettings;
import dev.snuffac.api.SnuffAc;
import dev.snuffac.api.SnuffPlatform;
import dev.snuffac.api.Vec3d;
import com.github.retrooper.packetevents.PacketEvents;
import dev.snuffac.core.SnuffCore;
import dev.snuffac.core.config.ConfigSource;
import dev.snuffac.core.config.SnuffConfig;
import dev.snuffac.core.log.SnuffLogger;
import dev.snuffac.core.packet.MovementPacket;
import dev.snuffac.core.packet.SnuffPacket;
import dev.snuffac.core.packet.ServerKeepAlivePacket;
import dev.snuffac.core.packet.ServerTeleportPacket;
import dev.snuffac.core.physics.MovementAttributes;
import dev.snuffac.core.player.EnvironmentMapper;
import dev.snuffac.core.player.MovementState;
import dev.snuffac.core.combat.CombatEnvironment;
import dev.snuffac.core.enforcement.EnforcementType;
import dev.snuffac.core.combat.EntitySnapshot;
import dev.snuffac.core.combat.ReachResolver;
import dev.snuffac.core.util.AxisAlignedBox;
import dev.snuffac.core.player.PlayerData;
import dev.snuffac.core.player.PlayerWorldCache;
import dev.snuffac.core.util.BlockKind;
import dev.snuffac.core.util.BlockPos;
import dev.snuffac.core.violation.CheckState;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.util.Locale;
import org.bukkit.scheduler.BukkitTask;

public final class SnuffPaperPlugin extends JavaPlugin implements SnuffLogger {

    private static final int WORLD_CACHE_RADIUS = 3;
    private static final int WORLD_CACHE_HEIGHT = 3;
    private static final long LOG_RETENTION_DAYS = 30L;

    private final Map<UUID, UUID> packetUserToPlayer = new ConcurrentHashMap<>();
    private final Map<UUID, PendingKeepAlive> pendingKeepAlives = new ConcurrentHashMap<>();

    private SnuffCore core;
    private SnuffPaperApi api;
    private PacketEventsAPI<?> packetEvents;
    private BukkitTask tickTask;
    private BukkitTask cacheTask;
    private SnuffPlatform platform = SnuffPlatform.PAPER;
    private ConfigSource configSource;
    private YamlConfigSource checkSource;

    @Override
    public void onEnable() {
        this.platform = resolvePlatform();
        saveDefaultConfig();
        saveResourceIfMissing("checks.yml");

        this.configSource = new YamlConfigSource(getConfig());
        this.checkSource = new YamlConfigSource(
                YamlConfigurationLoader.load(this, "checks.yml"));

        this.core = new SnuffCore(platform, this);
        this.core.boot(
                new BukkitPlatformAdapters.Messenger(),
                new BukkitPlatformAdapters.Permissions(),
                configSource,
                logDirectory());
        this.core.config().load(checkSource);
        this.core.registry().loadConfigurations(checkSource);
        this.core.config().load(configSource);

        this.api = new SnuffPaperApi(this);

        initPacketEvents();
        registerListeners();
        registerCommands();
        startTasks();

        SnuffAc.Holder.set(api);
        getLogger().info("Snuff AC " + api.version() + " enabled on " + platform.name()
                + " with " + core.checkKeys().size() + " checks.");
    }

    @Override
    public void onDisable() {
        if (tickTask != null) {
            tickTask.cancel();
        }
        if (cacheTask != null) {
            cacheTask.cancel();
        }
        if (packetEvents != null) {
            try {
                packetEvents.terminate();
            } catch (RuntimeException exception) {
                getLogger().log(Level.WARNING, "failed to terminate packet layer", exception);
            }
        }
        if (core != null) {
            core.shutdown();
        }
        SnuffAc.Holder.clear();
        getLogger().info("Snuff AC disabled.");
    }

    private SnuffPlatform resolvePlatform() {
        try {
            Class.forName("org.purpurmc.purpur.PurpurConfig");
            return SnuffPlatform.PURPUR;
        } catch (ClassNotFoundException ignored) {
            return SnuffPlatform.PAPER;
        }
    }

    private void initPacketEvents() {
        PacketEventsSettings settings = new PacketEventsSettings()
                .checkForUpdates(false)
                .bStats(false);
        this.packetEvents = io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder
                .buildNoCache(this, settings);
        com.github.retrooper.packetevents.PacketEvents.setAPI(this.packetEvents);
        this.packetEvents.load();
        this.packetEvents.getEventManager().registerListener(
                new SnuffPacketListener(
                        new DefaultPacketTranslator(this::movementStateOf),
                        this::onUserPacket));
    }

    private void onUserPacket(User user, SnuffPacket packet) {
        UUID playerId = packetUserToPlayer.get(user.getUUID());
        if (playerId == null) {
            PlayerData existing = core.playerByName(user.getName());
            if (existing == null) {
                return;
            }
            playerId = existing.id();
            packetUserToPlayer.put(user.getUUID(), playerId);
        }
        onPacket(playerId, packet);
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new SnuffPlayerListener(this), this);
    }

    private void registerCommands() {
        SnuffCommand command = new SnuffCommand(this);
        var pluginCommand = getCommand("snuff");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }
    }

    private void startTasks() {
        this.tickTask = Bukkit.getScheduler().runTaskTimer(this, core::tick, 1L, 1L);
        this.cacheTask = Bukkit.getScheduler().runTaskTimer(this, this::refreshWorldCaches, 1L, 1L);
    }

    void registerPlayer(Player player) {
        UUID id = player.getUniqueId();
        PlayerData data = core.addPlayer(id, player.getName(), System.currentTimeMillis());
        data.platformPlayer(player);
        data.entityId(player.getEntityId());
        data.protocolVersion(protocolOf(player));
        data.alive(true);
        User user = userOf(player);
        if (user != null) {
            packetUserToPlayer.put(user.getUUID(), id);
            data.clientBrand(user.getName() == null ? "unknown" : user.getName());
            data.entityId(user.getEntityId());
        }
        data.movement().position(toVec(player.getLocation()));
        data.movement().lastPosition(toVec(player.getLocation()));
        data.movement().velocity(Vec3d.ZERO);
        refreshPlayer(data, player);
    }

    void unregisterPlayer(UUID id) {
        PlayerData data = core.player(id);
        if (data != null) {
            packetUserToPlayer.values().removeIf(value -> value.equals(id));
        }
        pendingKeepAlives.remove(id);
        core.removePlayer(id);
    }

    void onPacket(UUID playerId, SnuffPacket packet) {
        PlayerData data = core.player(playerId);
        if (data == null) {
            return;
        }
        if (packet instanceof MovementPacket movement) {
            applyMovement(data, movement);
        } else if (packet instanceof ServerKeepAlivePacket keepAlive) {
            pendingKeepAlives.put(playerId, new PendingKeepAlive(keepAlive.payload(), System.currentTimeMillis()));
        }
        data.network().packetReceived(packet.arrivalNanos());
        core.enqueue(playerId, packet);
    }

    private void applyMovement(PlayerData data, MovementPacket packet) {
        MovementState state = data.movement();
        if (packet.positionChanged()) {
            state.advanceTo(packet.position(), packet.arrivalNanos());
            state.advanceGround(packet.onGround());
            state.horizontalCollision(packet.horizontalCollision());
        }
        if (packet.rotationChanged()) {
            state.advanceRotation(packet.yaw(), packet.pitch(), packet.arrivalNanos());
        }
    }

    void noteTeleport(PlayerData data, ServerTeleportPacket.TeleportCause cause, Location target) {
        var state = data.movement();
        state.teleportedThisTick(true);
        state.serverTeleportPending(false);
        state.position(toVec(target));
        state.lastPosition(toVec(target));
        state.velocity(Vec3d.ZERO);
        state.clientVelocity(Vec3d.ZERO);
        state.predictedVelocity(Vec3d.ZERO);
        state.tolerance().reset();
    }

    void onKeepAliveResponse(UUID playerId, long payload) {
        PlayerData data = core.player(playerId);
        PendingKeepAlive pending = pendingKeepAlives.remove(playerId);
        if (data == null || pending == null) {
            return;
        }
        data.network().receivedKeepAlive(pending.id(), System.currentTimeMillis());
    }

    private MovementState movementStateOf(User user) {
        UUID playerId = packetUserToPlayer.get(user.getUUID());
        if (playerId == null) {
            return null;
        }
        PlayerData data = core.player(playerId);
        return data == null ? null : data.movement();
    }

    private void refreshWorldCaches() {
        for (PlayerData data : new ArrayList<>(core.players())) {
            Object handle = data.platformPlayer();
            if (!(handle instanceof Player player) || !player.isOnline()) {
                continue;
            }
            refreshPlayer(data, player);
        }
    }

    private void refreshPlayer(PlayerData data, Player player) {
        Location location = player.getLocation();
        Vec3d position = toVec(location);
        data.movement().position(position);

        BlockPos center = BlockPos.of(position);
        Map<Long, PlayerWorldCache.CachedBlock> blocks = new java.util.HashMap<>();
        int baseY = center.y();
        for (int y = baseY - WORLD_CACHE_HEIGHT; y <= baseY + WORLD_CACHE_HEIGHT; y++) {
            for (int x = center.x() - WORLD_CACHE_RADIUS; x <= center.x() + WORLD_CACHE_RADIUS; x++) {
                for (int z = center.z() - WORLD_CACHE_RADIUS; z <= center.z() + WORLD_CACHE_RADIUS; z++) {
                    BlockPos blockPos = BlockPos.of(x, y, z);
                    if (!player.getWorld().isChunkLoaded(blockPos.x() >> 4, blockPos.z() >> 4)) {
                        continue;
                    }
                    Block block = player.getWorld().getBlockAt(x, y, z);
                    BlockKind kind = BlockClassifier.classify(block);
                    blocks.put(blockPos.pack(), new PlayerWorldCache.CachedBlock(
                            kind, BlockClassifier.hardness(block), block.getType().name()));
                }
            }
        }

        BlockPos below = center.offset(0, -1, 0);
        Block belowBlock = player.getWorld().getBlockAt(below.x(), below.y(), below.z());
        double slipperiness = BlockClassifier.classify(belowBlock).slipperiness();
        boolean onGroundBelow = !BlockClassifier.classify(belowBlock).passable();
        boolean loaded = player.getWorld().isChunkLoaded(center.x() >> 4, center.z() >> 4);
        int light = belowBlock.getLightLevel();

        data.worldCache(PlayerWorldCache.of(position, blocks, slipperiness, light, loaded, onGroundBelow));

        applyEnvironment(data, player, onGroundBelow);
        applyEquipment(data, player);
        refreshCombat(data, player);
    }

    private void refreshCombat(PlayerData data, Player player) {
        var snapshots = new java.util.HashMap<Integer, EntitySnapshot>();
        Location eye = player.getEyeLocation();
        Vec3d eyePosition = new Vec3d(eye.getX(), eye.getY(), eye.getZ());
        int sentRadius = sentChunkRadius(player);
        var world = player.getWorld();
        for (Entity entity : world.getEntities()) {
            if (entity.getLocation().distanceSquared(player.getLocation()) > 2304.0) {
                continue;
            }
            if (entity.getUniqueId().equals(player.getUniqueId())) {
                continue;
            }
            Location location = entity.getLocation();
            Vec3d position = new Vec3d(location.getX(), location.getY(), location.getZ());
            AxisAlignedBox box = new AxisAlignedBox(
                    location.getX() - entity.getWidth() / 2.0,
                    location.getY(),
                    location.getZ() - entity.getWidth() / 2.0,
                    location.getX() + entity.getWidth() / 2.0,
                    location.getY() + entity.getHeight(),
                    location.getZ() + entity.getWidth() / 2.0);
            boolean living = entity instanceof LivingEntity;
            double eyeHeight = living ? ((LivingEntity) entity).getEyeHeight() : 0.0;
            org.bukkit.util.Vector velocity = entity.getVelocity();
            snapshots.put(entity.getEntityId(), new EntitySnapshot(
                    entity.getEntityId(),
                    entity.getType().name(),
                    position,
                    box,
                    living,
                    entity instanceof Player,
                    eyeHeight,
                    new Vec3d(velocity.getX(), velocity.getY(), velocity.getZ()),
                    -1.0));
        }
        data.combatEnvironment(CombatEnvironment.of(
                System.currentTimeMillis(), snapshots, sentRadius + 2, sentRadius));
        occlusion = new OcclusionProbe(data, eyePosition, world);
    }

    private static int sentChunkRadius(Player player) {
        int viewDistance = 10;
        try {
            return Math.max(2, player.getServer().getViewDistance());
        } catch (RuntimeException exception) {
            return viewDistance;
        }
    }

    private volatile ReachResolver.BlockOcclusion occlusion = position -> false;

    private final class OcclusionProbe implements ReachResolver.BlockOcclusion {

        private final PlayerData data;
        private final Vec3d eye;
        private final org.bukkit.World world;

        OcclusionProbe(PlayerData data, Vec3d eye, org.bukkit.World world) {
            this.data = data;
            this.eye = eye;
            this.world = world;
        }

        @Override
        public boolean opaqueAt(BlockPos position) {
            if (!world.isChunkLoaded(position.x() >> 4, position.z() >> 4)) {
                return false;
            }
            BlockKind kind = data.worldCache().kindAt(position);
            if (kind == BlockKind.AIR || kind == BlockKind.WATER || kind == BlockKind.LAVA
                    || kind == BlockKind.LADDER || kind == BlockKind.UNKNOWN) {
                return false;
            }
            return kind == BlockKind.SOLID || kind == BlockKind.BEDROCK || kind == BlockKind.BARRIER;
        }
    }

    public ReachResolver.BlockOcclusion occlusion() {
        return occlusion;
    }

    private void applyEnvironment(PlayerData data, Player player, boolean onGroundBelow) {
        MovementState state = data.movement();
        Location location = player.getLocation();
        BlockPos feet = BlockPos.of(state.position());

        state.sneaking(player.isSneaking());
        state.sprinting(player.isSprinting());
        state.flying(player.getAllowFlight() && player.isFlying());
        state.gliding(player.isGliding());
        state.riding(player.isInsideVehicle());
        state.inVehicle(player.isInsideVehicle());
        state.serverOnGround(onGroundBelow);
        state.fallDistance(player.getFallDistance());
        state.teleportedThisTick(false);

        data.worldCache().kindAt(feet);
        BlockPos head = feet.offset(0, 1, 0);
        state.inWater(data.worldCache().kindAt(feet) == BlockKind.WATER
                || data.worldCache().kindAt(head) == BlockKind.WATER);
        state.inLava(data.worldCache().kindAt(feet) == BlockKind.LAVA);
        state.onClimbable(data.worldCache().isClimbable(feet) || data.worldCache().isClimbable(head));
        state.onSlime(data.worldCache().isSlime(feet.offset(0, -1, 0)));
        state.onIce(data.worldCache().isIce(feet.offset(0, -1, 0)));
        state.onHoney(data.worldCache().kindAt(feet) == BlockKind.HONEY);
        state.onSoulSand(data.worldCache().kindAt(feet.offset(0, -1, 0)) == BlockKind.SOUL_SAND);
        state.hasSlowFalling(player.hasPotionEffect(PotionEffectType.SLOW_FALLING));
        state.levitation(player.hasPotionEffect(PotionEffectType.LEVITATION), amplifierOf(player, PotionEffectType.LEVITATION));
        state.swimming(player.isSwimming());
        state.effects(
                amplifierOf(player, PotionEffectType.JUMP_BOOST),
                amplifierOf(player, PotionEffectType.SPEED),
                amplifierOf(player, PotionEffectType.SLOWNESS));
        state.usingItem(player.isHandRaised());

        MovementAttributes attributes = MovementAttributes.DEFAULT;
        AttributeInstance gravity = attribute(player, Attribute.GRAVITY);
        if (gravity != null) {
            attributes = attributes.withGravity(gravity.getValue());
        }
        AttributeInstance speed = attribute(player, Attribute.MOVEMENT_SPEED);
        if (speed != null) {
            attributes = attributes.withMovementSpeed(speed.getValue());
        }
        AttributeInstance jump = attribute(player, Attribute.JUMP_STRENGTH);
        if (jump != null) {
            attributes = attributes.withJumpStrength(jump.getValue());
        }
        AttributeInstance step = attribute(player, Attribute.STEP_HEIGHT);
        if (step != null) {
            attributes = attributes.withStepHeight(step.getValue());
        }
        state.attributes(attributes);

        var environment = EnvironmentMapper.from(state, data.worldCache());
        state.environment(environment);

        GameMode mode = player.getGameMode();
        boolean bedrock = core.config().exemptBedrock() && ClientCompat.isBedrock(player);
        boolean legacy = core.config().exemptLegacyProtocol()
                && ClientCompat.isLegacyProtocol(data.protocolVersion());
        boolean belowVoidFloor = core.config().exemptVoidWorlds()
                && state.position().y() < core.config().voidWorldFloor();
        boolean exempt = mode == GameMode.SPECTATOR
                || player.hasPermission(core.config().bypassPermission())
                || location.getWorld() == null
                || belowVoidFloor
                || data.protocolVersion() > 0
                && !ClientCompat.isSupportedProtocol(data.protocolVersion())
                && !core.config().allowUnknownProtocols()
                || bedrock
                || legacy;
        data.exempt(exempt);
        if (bedrock || legacy) {
            data.debugLine("exempt: " + (bedrock ? "bedrock client" : "legacy protocol")
                    + " (protocol " + data.protocolVersion() + ")");
        }
    }

    private void applyEquipment(PlayerData data, Player player) {
        PlayerInventory inventory = player.getInventory();
        ItemStack held = inventory.getItemInMainHand();
        Material type = held == null ? Material.AIR : held.getType();
        var equipment = data.equipment();
        double speed = MiningSpeedResolver.speedFor(type);
        double blockSpeed = 1.0;
        equipment.update(speed, blockSpeed, MiningSpeedResolver.isTool(type), inventory.getHeldItemSlot(),
                MiningSpeedResolver.isPlaceable(type));
    }

    private static AttributeInstance attribute(Player player, Attribute attribute) {
        try {
            return player.getAttribute(attribute);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static int amplifierOf(Player player, PotionEffectType type) {
        PotionEffect effect = player.getPotionEffect(type);
        return effect == null ? 0 : effect.getAmplifier() + 1;
    }

    private static User userOf(Player player) {
        try {
            PacketEventsAPI<?> api = PacketEvents.getAPI();
            if (api == null || api.getPlayerManager() == null) {
                return null;
            }
            return api.getPlayerManager().getUser(player);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static int protocolOf(Player player) {
        User user = userOf(player);
        if (user == null || user.getClientVersion() == null) {
            return -1;
        }
        return user.getClientVersion().getProtocolVersion();
    }

    private static Vec3d toVec(Location location) {
        return new Vec3d(location.getX(), location.getY(), location.getZ());
    }

    private Path logDirectory() {
        return getDataFolder().toPath().resolve("logs");
    }

    void reloadConfiguration() {
        reloadConfig();
        saveResourceIfMissing("checks.yml");
        this.configSource = new YamlConfigSource(getConfig());
        this.checkSource = new YamlConfigSource(YamlConfigurationLoader.load(this, "checks.yml"));
        core.reload(configSource);
        core.config().load(checkSource);
        core.registry().loadConfigurations(checkSource);
        core.config().load(configSource);
        getLogger().info("Snuff AC configuration reloaded.");
    }

    void setCheckEnabled(String key, boolean enabled) {
        var check = core.registry().check(key);
        if (check == null) {
            return;
        }
        CheckConfig config = core.registry().config(check);
        config.enabled(enabled);
        String base = "checks." + check.category().name().toLowerCase(Locale.ROOT) + "." + check.key();
        checkSource.set(base + ".enabled", enabled);
        checkSource.save();
        YamlConfigurationLoader.store(this, "checks.yml", checkSource);
        for (PlayerData data : core.players()) {
            CheckState state = data.checkState(check.key());
            if (state != null) {
                state.reset();
            }
        }
    }

    boolean persistCheck(String key, Consumer<CheckConfig> mutator) {
        var check = core.registry().check(key);
        if (check == null) {
            return false;
        }
        CheckConfig config = core.registry().config(check);
        mutator.accept(config);
        core.registry().saveConfigurations(checkSource);
        YamlConfigurationLoader.store(this, "checks.yml", checkSource);
        return true;
    }

    private void enforce(dev.snuffac.core.enforcement.EnforcementRequest request) {
        if (!request.preventsAnything()) {
            return;
        }
        PlayerData data = core.player(request.playerId());
        if (data == null) {
            return;
        }
        switch (request.type()) {
            case SETBACK_POSITION, TELEPORT_SYNC -> Bukkit.getScheduler()
                    .runTask(this, () -> performSetback(data, request.reason()));
            case CANCEL_ATTACK, CANCEL_BLOCK_PLACE, CANCEL_BLOCK_BREAK, CANCEL_INTERACTION ->
                    data.packetModificationEnabled(false);
            default -> {
            }
        }
        data.debugLine("enforced " + request.type() + " for " + request.checkKey() + ": " + request.reason());
    }

    void scheduleSetback(PlayerData data, String detail) {
        if (!data.setbackEnabled()) {
            return;
        }
        if (!(data.platformPlayer() instanceof Player player) || !player.isOnline()) {
            return;
        }
        Bukkit.getScheduler().runTask(this, () -> performSetback(data, detail));
    }

    void performSetback(PlayerData data, String detail) {
        if (!(data.platformPlayer() instanceof Player player) || !player.isOnline()) {
            return;
        }
        Location target = player.getLocation();
        target.setYaw(data.movement().yaw());
        target.setPitch(data.movement().pitch());
        double horizontal = core.config().setbackHorizontal();
        double vertical = core.config().setbackVertical();
        if (horizontal > 0.0 || vertical > 0.0) {
            target.add(horizontal, vertical, 0.0);
        }
        player.teleport(target);
        var state = data.movement();
        state.teleportedThisTick(true);
        state.velocity(Vec3d.ZERO);
        state.predictedVelocity(Vec3d.ZERO);
        state.clientVelocity(Vec3d.ZERO);
        state.tolerance().reset();
        data.debugLine("setback: " + detail);
    }

    void setDebug(PlayerData data, boolean value) {
        data.debugEnabled(value);
    }

    SnuffCore core() {
        return core;
    }

    SnuffPaperApi api() {
        return api;
    }

    SnuffPlatform platform() {
        return platform;
    }

    SnuffConfig config() {
        return core.config();
    }

    @Override
    public void info(String message) {
        getLogger().info(message);
    }

    @Override
    public void warn(String message) {
        getLogger().warning(message);
    }

    @Override
    public void severe(String message) {
        getLogger().severe(message);
    }

    @Override
    public void debug(String message) {
        if (core != null && core.config().debug()) {
            getLogger().info("[debug] " + message);
        }
    }

    @Override
    public void violation(String message) {
        getLogger().info("[violation] " + message);
    }

    void debugLine(PlayerData data, String line) {
        if (data.debugEnabled()) {
            data.debugLine(line);
        }
        if (core.config().debug()) {
            debug("[" + data.name() + "] " + line);
        }
    }

    PlayerData dataOf(UUID id) {
        return core.player(id);
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

    private void saveResourceIfMissing(String name) {
        if (!new java.io.File(getDataFolder(), name).exists()) {
            try {
                saveResource(name, false);
            } catch (IllegalArgumentException exception) {
                getLogger().warning("missing bundled resource " + name);
            }
        }
    }
}
