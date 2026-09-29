package dev.snuffac.paper;

import com.github.retrooper.packetevents.PacketEventsAPI;
import dev.snuffac.core.config.CheckConfig;
import java.util.function.Consumer;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.settings.PacketEventsSettings;
import dev.snuffac.api.SnuffAc;
import dev.snuffac.api.SnuffPlatform;
import dev.snuffac.core.util.BlockPos;
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

    private static int messengerFailures;
    private dev.snuffac.core.punish.PunishmentService punishments;
    private static final long RETENTION_PROMPT_MILLIS = 60_000L;
    private dev.snuffac.core.punish.EscalationService escalation;

    private void applyMuteOnDisable() {
    }

    public boolean canUseMenu(Player player) {
        return player != null && player.hasPermission("snuffac.menu") && core != null;
    }

    public void openMainMenu(Player player) {
        StaffSounds.play(player, StaffSounds.MENU_OPEN);
        var bridge = new dev.snuffac.paper.gui.GuiBridgeImpl(this);
        var menu = new dev.snuffac.paper.gui.MainMenu(this, bridge, alertsEnabledFor(player));
        menu.build();
        menu.open(player);
    }

    private void onViolationForEscalation(dev.snuffac.api.violation.ViolationInfo info) {
        if (escalation == null || !escalation.enabled()) {
            return;
        }
        try {
            var outcome = escalation.onFlag(
                    info.playerId(),
                    info.playerName(),
                    info.checkKey(),
                    info.checkName(),
                    info.confidence(),
                    "Snuff AC");
            if (!outcome.actionable()) {
                return;
            }
            announceEscalation(info, outcome);
        } catch (RuntimeException failure) {
            getLogger().warning("escalation failed: " + failure);
        }
    }

    private void announceEscalation(dev.snuffac.api.violation.ViolationInfo info,
            dev.snuffac.core.punish.EscalationService.Outcome outcome) {
        String name = info.playerName();
        String check = outcome.checkLabel();
        if (outcome.kind() == dev.snuffac.core.punish.EscalationService.OutcomeKind.WARNED) {
            Player online = Bukkit.getPlayer(info.playerId());
            if (online != null) {
                StaffMessages.send(online, "You were warned for " + check
                        + " (" + outcome.warnings() + " of " + outcome.maxWarnings() + ").");
            }
            return;
        }
        String reason = "Warned " + outcome.maxWarnings() + " times for " + check
                + ". If you believe this is a mistake, contact the admins after the ban expires.";
        kickForEscalation(info.playerId(), reason);
    }

    private void kickForEscalation(UUID id, String reason) {
        Player online = Bukkit.getPlayer(id);
        if (online == null) {
            return;
        }
        online.kick(net.kyori.adventure.text.Component.text("You are temporarily banned.\n\nReason: "
                + reason + "\n\nDuration: "
                + dev.snuffac.core.punish.Durations.describe(escalation.banMillis())
                + "\n\nIf you believe this is a mistake, contact the admins."));
    }

    public dev.snuffac.core.punish.EscalationService escalation() {
        return escalation;
    }

    private void applyEscalationConfig() {
        var config = core.config();
        escalation.enabled(config.escalationEnabled());
        escalation.maxWarnings(config.escalationMaxWarnings());
        escalation.banMillis(config.escalationBanMillis());
        escalation.minConfidence(config.escalationMinConfidence());
        escalation.warnOnly(config.escalationWarnOnly());
    }

    public void applyEscalationConfigFromSettings() {
        var config = core.config();
        getConfig().set("escalation.enabled", escalation.enabled());
        getConfig().set("escalation.max-warnings", escalation.maxWarnings());
        getConfig().set("escalation.ban-duration-millis", escalation.banMillis());
        getConfig().set("escalation.min-confidence", escalation.minConfidence());
        getConfig().set("escalation.warn-only", escalation.warnOnly());
        saveConfig();
        config.escalationEnabled(escalation.enabled());
        config.escalationMaxWarnings(escalation.maxWarnings());
        config.escalationBanMillis(escalation.banMillis());
        config.escalationMinConfidence(escalation.minConfidence());
        config.escalationWarnOnly(escalation.warnOnly());
    }

    private final java.util.Map<UUID, RetentionPrompt> retentionPrompts = new java.util.concurrent.ConcurrentHashMap<>();

    public void promptForRetention(Player player, dev.snuffac.paper.gui.SettingsMenu.RetentionKind kind) {
        retentionPrompts.put(player.getUniqueId(), new RetentionPrompt(
                kind, System.currentTimeMillis() + RETENTION_PROMPT_MILLIS));
        StaffMessages.send(player, "Type the number of days, for example "
                + "<white>30</white> or <white>5d</white>, or type <white>cancel</white>.");
    }

    public void persistRetention() {
        var config = core.config();
        getConfig().set("general.log-retention-days", config.logRetentionDays());
        getConfig().set("general.history-retention-days", config.historyRetentionDays());
        getConfig().set("general.alert-cooldown-ms", config.alertCooldownMillis());
        saveConfig();
    }

    public void persistPrevention() {
        getConfig().set("prevention.enabled", core.config().preventionEnabled());
        saveConfig();
        core.enforcement().preventionEnabled(core.config().preventionEnabled());
    }

    public boolean handleRetentionChat(Player player, String message) {
        RetentionPrompt prompt = retentionPrompts.get(player.getUniqueId());
        if (prompt == null) {
            return false;
        }
        String text = sanitizeChatInput(message);
        if (text.equalsIgnoreCase("cancel")) {
            retentionPrompts.remove(player.getUniqueId());
            StaffMessages.send(player, "Cancelled, nothing changed.");
            return true;
        }
        if (text.isEmpty()) {
            return false;
        }
        if (!Character.isDigit(text.charAt(0))) {
            return false;
        }
        int days = parseRetentionDays(text);
        if (days < 0) {
            StaffMessages.send(player, "That is not a number of days. "
                    + "Try <white>30</white> or <white>5d</white>.");
            return true;
        }
        if (days < 1 || days > 3650) {
            StaffMessages.send(player, "Enter a number between 1 and 3650. Nothing was changed.");
            return true;
        }
        if (!canUseMenu(player)) {
            StaffMessages.send(player, "You no longer have permission to do that.");
            return true;
        }
        var config = core.config();
        if (prompt.kind() == dev.snuffac.paper.gui.SettingsMenu.RetentionKind.LOG) {
            config.logRetentionDays(days);
        } else {
            config.historyRetentionDays(days);
        }
        persistRetention();
        StaffSounds.play(player, StaffSounds.SUCCESS);
        StaffMessages.send(player, "Retention set to <white>" + days + "</white> days.");
        return true;
    }

    private static int parseRetentionDays(String text) {
        try {
            int plain = Integer.parseInt(text);
            return plain > 0 ? plain : -1;
        } catch (NumberFormatException notPlain) {
            try {
                long millis = dev.snuffac.core.punish.Durations.parseMillis(text);
                long days = millis / 86_400_000L;
                if (millis % 86_400_000L != 0L) {
                    days = Math.max(1L, Math.round((double) millis / 86_400_000.0));
                }
                return days > 0 ? (int) days : -1;
            } catch (IllegalArgumentException notDuration) {
                return -1;
            }
        }
    }

    private static String sanitizeChatInput(String message) {
        if (message == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder(message.length());
        for (int i = 0; i < message.length() && i < 32; i++) {
            char c = message.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                builder.append(Character.toLowerCase(c));
            }
        }
        return builder.toString();
    }

    public void purgeExpiredRetentionPrompts() {
        long now = System.currentTimeMillis();
        retentionPrompts.entrySet().removeIf(entry -> entry.getValue().expiresMillis() < now);
    }

    private record RetentionPrompt(
            dev.snuffac.paper.gui.SettingsMenu.RetentionKind kind, long expiresMillis) {
    }

    public dev.snuffac.core.punish.PunishmentService punishments() {
        return punishments;
    }

    public void reloadEverything() {
        try {
            reloadConfiguration();
            core.enforcement().preventionEnabled(core.config().preventionEnabled());
            core.enforcement().minConfidenceForPrevention(core.config().minConfidenceForPrevention());
            core.alerts().clearCooldowns();
            SnuffSounds.enabled(core.config().staffSounds());
            getLogger().info("configuration reloaded");
        } catch (RuntimeException failure) {
            getLogger().warning("reload failed: " + failure);
        }
    }

    public void openSuspicious(Player player) {
        new dev.snuffac.paper.gui.GuiBridgeImpl(this).openSuspicious(player, 0);
    }

    public void openSuspiciousByName(Player player, String name) {
        new dev.snuffac.paper.gui.GuiBridgeImpl(this).openSuspicious(player, 0);
        StaffMessages.send(player, "Showing flagged players. Looking for " + name + ".");
    }

    public void openCase(Player player, String uuid) {
        new dev.snuffac.paper.gui.GuiBridgeImpl(this).openCase(player, uuid);
    }

    public void openWarned(Player player) {
        var menu = new dev.snuffac.paper.gui.WarnedMenu(this);
        menu.build();
        menu.open(player);
    }

    public void openSettings(Player player) {
        var menu = new dev.snuffac.paper.gui.SettingsMenu(this);
        menu.build();
        menu.open(player);
    }

    public SnuffCore core() {
        return core;
    }

    public java.util.Set<UUID> knownPlayerIds() {
        return core.knownPlayerIds();
    }

    private boolean alertsEnabledFor(Player player) {
        dev.snuffac.core.player.PlayerData data = core.player(player.getUniqueId());
        return data == null || data.alertsEnabled();
    }

    static void reportMessengerFailure(RuntimeException exception) {
        messengerFailures++;
        if (messengerFailures <= 10) {
            Bukkit.getLogger().warning("alert delivery failed: " + exception);
        } else if (messengerFailures == 11) {
            Bukkit.getLogger().warning("further alert delivery failures will not be logged");
        }
    }

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
                new BukkitPlatformAdapters.Messenger(this),
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
        dev.snuffac.paper.gui.SnuffMenu.closeAll();
        applyMuteOnDisable();
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
        this.punishments = new dev.snuffac.core.punish.PunishmentService(
                new java.io.File(getDataFolder(), "punishments").toPath(), null);
        this.punishments.load();
        SnuffSounds.enabled(core.config().staffSounds());
        this.escalation = new dev.snuffac.core.punish.EscalationService(
                punishments, core.config().escalationMinConfidence());
        applyEscalationConfig();
        core.violations().addListener(this::onViolationForEscalation);
        applyAntiXray();
        getServer().getPluginManager().registerEvents(new dev.snuffac.paper.gui.MenuListener(this), this);
        getServer().getPluginManager().registerEvents(new MechanicsListener(this), this);
    }

    void registerPlayer(Player player) {
        UUID id = player.getUniqueId();
        PlayerData data = core.addPlayer(id, player.getName(), System.currentTimeMillis());
        data.platformPlayer(player);
        data.entityId(player.getEntityId());
        data.protocolVersion(protocolOf(player));
        data.alive(true);
        core.loadHistory(id, player.getName());
        int priorFlags = core.historyStore() == null ? 0 : core.historyStore().total(id);
        if (priorFlags > 0 && core.config().alertOnRejoinWithHistory()) {
            String name = player.getName();
            Bukkit.getScheduler().runTaskLater(this, () -> {
                for (Player staff : Bukkit.getOnlinePlayers()) {
                    if (staff.hasPermission(core.config().alertPermission())) {
                        StaffMessages.send(staff, name + " rejoined carrying "
                                + priorFlags + " recorded flag(s) from previous sessions");
                    }
                }
            }, 40L);
        }
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

    private void resolveMiningProbes(PlayerData data, Player player) {
        observeWindCharge(data, player);
        observeWeaponAttributes(data, player);
        observePing(data, player);
        var analyser = data.mining();
        var probes = analyser.drainProbes(16);
        if (probes.isEmpty()) {
            return;
        }
        java.util.List<String> materials = new java.util.ArrayList<>(probes.size());
        for (long packed : probes) {
            BlockPos pos = BlockPos.unpack(packed);
            String material = null;
            try {
                if (player.getWorld().isChunkLoaded(pos.x() >> 4, pos.z() >> 4)) {
                    material = player.getWorld().getBlockAt(pos.x(), pos.y(), pos.z()).getType().name();
                }
            } catch (RuntimeException ignored) {
                material = null;
            }
            materials.add(material);
        }
        analyser.publishProbeBatch(probes, materials);
    }

    private void applyAntiXray() {
        var policy = core.config().antiXrayMode();
        if (policy == null || policy == dev.snuffac.core.world.ObfuscationPolicy.OFF) {
            return;
        }
        int bandStart = core.config().antiXrayBandStart();
        int bandEnd = core.config().antiXrayBandEnd();
        try {
            Class<?> worldSettings = Class.forName(
                    "com.destroystokyo.paper.PaperWorldConfig");
            Class<?> antiXray = Class.forName(
                    "com.destroystokyo.paper.antixray.PaperAntiXrayConfig");
            for (org.bukkit.World world : Bukkit.getWorlds()) {
                Object config = worldSettings.getMethod("getInstance", org.bukkit.World.class)
                        .invoke(null, world);
                Object antiXrayConfig = antiXray.getMethod("getInstance",
                        worldSettings).invoke(null, config);
                antiXray.getMethod("setEngineMode", String.class)
                        .invoke(antiXrayConfig, engineModeFor(policy));
                antiXray.getMethod("setHiddenBlocks",
                        java.util.List.class).invoke(antiXrayConfig, hiddenBlocksFor(policy));
                antiXray.getMethod("setReplaceBlocks",
                        java.util.List.class).invoke(antiXrayConfig, hiddenBlocksFor(policy));
                antiXray.getMethod("setHeight", int.class)
                        .invoke(antiXrayConfig, Math.max(0, Math.min(320, bandEnd - bandStart)));
            }
            getLogger().info("anti-xray obfuscation active: " + policy);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            getLogger().warning("anti-xray obfuscation unavailable on this server build: "
                    + exception);
        }
    }

    private static String engineModeFor(dev.snuffac.core.world.ObfuscationPolicy policy) {
        return switch (policy) {
            case HIDDEN_ORES, HIDDEN_ORES_AND_DEEPSLATE -> "ALL_ORES";
            case OFF -> "NONE";
        };
    }

    private static java.util.List<String> hiddenBlocksFor(dev.snuffac.core.world.ObfuscationPolicy policy) {
        return switch (policy) {
            case HIDDEN_ORES_AND_DEEPSLATE -> java.util.List.of(
                    "ORE_COAL", "ORE_DEEPSLATE_COAL", "ORE_IRON", "ORE_DEEPSLATE_IRON",
                    "ORE_COPPER", "ORE_DEEPSLATE_COPPER", "ORE_GOLD", "ORE_DEEPSLATE_GOLD",
                    "ORE_REDSTONE", "ORE_DEEPSLATE_REDSTONE", "ORE_DIAMOND", "ORE_DEEPSLATE_DIAMOND",
                    "ORE_LAPIS", "ORE_DEEPSLATE_LAPIS", "ORE_EMERALD", "ORE_DEEPSLATE_EMERALD",
                    "DEEPSLATE_DIAMOND_ORE", "DEEPSLATE_EMERALD_ORE", "DEEPSLATE_REDSTONE_ORE",
                    "DEEPSLATE_LAPIS_ORE", "DEEPSLATE_GOLD_ORE", "DEEPSLATE_IRON_ORE",
                    "DEEPSLATE_COPPER_ORE", "DEEPSLATE_COAL_ORE");
            default -> java.util.List.of(
                    "ORE_DIAMOND", "ORE_DEEPSLATE_DIAMOND", "DEEPSLATE_DIAMOND_ORE",
                    "ORE_EMERALD", "ORE_DEEPSLATE_EMERALD", "DEEPSLATE_EMERALD_ORE",
                    "ORE_GOLD", "ORE_DEEPSLATE_GOLD", "DEEPSLATE_GOLD_ORE",
                    "ORE_REDSTONE", "ORE_DEEPSLATE_REDSTONE", "DEEPSLATE_REDSTONE_ORE");
        };
    }

    private void refreshWorldCaches() {
        purgeExpiredRetentionPrompts();
        for (PlayerData data : new ArrayList<>(core.players())) {
            Object handle = data.platformPlayer();
            if (!(handle instanceof Player player) || !player.isOnline()) {
                continue;
            }
            refreshPlayer(data, player);
        }
    }

    private void refreshPlayer(PlayerData data, Player player) {
        resolveMiningProbes(data, player);
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

    private static double observedReach(Player player) {
        try {
            var attribute = player.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE);
            if (attribute == null) {
                return 3.0;
            }
            return attribute.getValue();
        } catch (RuntimeException ignored) {
            return 3.0;
        }
    }

    private static double observedDamage(Player player) {
        try {
            var attribute = player.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE);
            return attribute == null ? 1.0 : attribute.getValue();
        } catch (RuntimeException ignored) {
            return 1.0;
        }
    }

    private static boolean isRiptiding(Player player) {
        try {
            for (var effect : player.getActivePotionEffects()) {
                String key = effect.getType().getKey().toString();
                if (key.contains("riptide") || key.contains("wind_charged")) {
                    return true;
                }
            }
        } catch (RuntimeException ignored) {
        }
        return false;
    }

    private void observePing(PlayerData data, Player player) {
        try {
            int ping = player.getPing();
            if (ping >= 0) {
                data.network().recordPing(ping);
            }
        } catch (RuntimeException ignored) {
        }
    }

    private void observeWindCharge(PlayerData data, Player player) {
        try {
            if (player.hasMetadata("snuffac.windcharge")) {
                data.movement().markWindCharge();
            }
        } catch (RuntimeException ignored) {
        }
    }

    private void observeWeaponAttributes(PlayerData data, Player player) {
        try {
            var held = player.getInventory().getItemInMainHand();
            String name = held == null ? "" : held.getType().name();
            boolean spearLike = name.contains("SPEAR")
                    || name.contains("MACE")
                    || name.contains("TRIDENT");
            data.equipment().weaponType(name);
            data.equipment().weaponAttributeActive(spearLike);
            data.equipment().observedAttackReach(observedReach(player));
            data.equipment().attackDamage(observedDamage(player));
        } catch (RuntimeException ignored) {
        }
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
        state.riptiding(isRiptiding(player));
        if (player.isGliding()) {
            state.markKnockback();
        }
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

    public void reloadConfiguration() {
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

    SnuffCore coreInternal() {
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

    public PlayerData dataOf(UUID id) {
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
