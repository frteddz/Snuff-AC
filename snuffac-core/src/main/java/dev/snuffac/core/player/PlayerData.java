package dev.snuffac.core.player;

import dev.snuffac.api.Vec3d;
import dev.snuffac.core.tolerance.ToleranceModel;
import dev.snuffac.core.violation.CheckState;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public final class PlayerData {

    private static final int VIOLATION_HISTORY_LIMIT = 32;

    private final UUID id;
    private final String name;
    private final long joinMillis;
    private final MovementState movement;
    private final CombatState combat = new CombatState();
    private final WorldState world = new WorldState();
    private final NetworkState network = new NetworkState();
    private final Map<String, CheckState> checkStates = new LinkedHashMap<>();
    private final Map<String, Object> checkData = new java.util.concurrent.ConcurrentHashMap<>();
    private final Deque<dev.snuffac.core.violation.ViolationRecord> history = new ArrayDeque<>(VIOLATION_HISTORY_LIMIT);
    private final List<dev.snuffac.core.violation.ViolationRecord> recent = new CopyOnWriteArrayList<>();

    private Object platformPlayer;
    private int entityId = -1;
    private int protocolVersion = -1;
    private String clientBrand = "unknown";

    private boolean alive = true;
    private boolean exempt;
    private boolean debugEnabled;
    private boolean alertsEnabled = true;
    private boolean verboseAlerts = false;
    private int warningsView;
    private boolean setbackEnabled = true;
    private boolean aliveLastTick = true;
    private boolean joined;
    private boolean dirty;

    private final EquipmentState equipment = new EquipmentState();
    private final dev.snuffac.core.confidence.ConfidenceModel confidence =
            new dev.snuffac.core.confidence.ConfidenceModel();
    private final dev.snuffac.core.mining.MiningAnalyser mining = new dev.snuffac.core.mining.MiningAnalyser(2);
    private volatile dev.snuffac.core.combat.CombatEnvironment combatEnvironment =
            dev.snuffac.core.combat.CombatEnvironment.empty();
    private volatile PlayerWorldCache worldCache = PlayerWorldCache.empty(Vec3d.ZERO);
    private volatile List<String> debugLines = List.of();

    private long lastViolationMillis;
    private long violationsThisSecondWindow;
    private long violationWindowStart;
    private int violationsThisSecond;
    private double totalViolationLevel;

    public PlayerData(UUID id, String name, long joinMillis, ToleranceModel tolerance) {
        this.id = id;
        this.name = name;
        this.joinMillis = joinMillis;
        this.movement = new MovementState(tolerance);
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public long joinMillis() {
        return joinMillis;
    }

    public MovementState movement() {
        return movement;
    }

    public CombatState combat() {
        return combat;
    }

    public WorldState world() {
        return world;
    }

    public NetworkState network() {
        return network;
    }

    public Vec3d position() {
        return movement.position();
    }

    public EquipmentState equipment() {
        return equipment;
    }

    public dev.snuffac.core.confidence.ConfidenceModel confidence() {
        return confidence;
    }

    public dev.snuffac.core.mining.MiningAnalyser mining() {
        return mining;
    }

    public dev.snuffac.core.combat.CombatEnvironment combatEnvironment() {
        return combatEnvironment;
    }

    public void combatEnvironment(dev.snuffac.core.combat.CombatEnvironment value) {
        this.combatEnvironment = value == null
                ? dev.snuffac.core.combat.CombatEnvironment.empty()
                : value;
    }

    public PlayerWorldCache worldCache() {
        return worldCache;
    }

    public void worldCache(PlayerWorldCache cache) {
        this.worldCache = cache;
    }

    public void debugLine(String line) {
        if (!debugEnabled) {
            return;
        }
        synchronized (debugLines) {
            java.util.ArrayList<String> next = new java.util.ArrayList<>(debugLines);
            next.add(line);
            while (next.size() > 40) {
                next.remove(0);
            }
            this.debugLines = List.copyOf(next);
        }
    }

    public List<String> debugLines() {
        return debugLines;
    }

    public Object platformPlayer() {
        return platformPlayer;
    }

    public void platformPlayer(Object value) {
        this.platformPlayer = value;
    }

    public int entityId() {
        return entityId;
    }

    public void entityId(int value) {
        this.entityId = value;
    }

    public int protocolVersion() {
        return protocolVersion;
    }

    public void protocolVersion(int value) {
        this.protocolVersion = value;
    }

    public String clientBrand() {
        return clientBrand;
    }

    public void clientBrand(String value) {
        this.clientBrand = value;
    }

    public boolean alive() {
        return alive;
    }

    public void alive(boolean value) {
        this.alive = value;
    }

    public boolean aliveLastTick() {
        return aliveLastTick;
    }

    public void aliveLastTick(boolean value) {
        this.aliveLastTick = value;
    }

    public boolean exempt() {
        return exempt;
    }

    public void exempt(boolean value) {
        this.exempt = value;
    }

    public boolean debugEnabled() {
        return debugEnabled;
    }

    public void debugEnabled(boolean value) {
        this.debugEnabled = value;
    }

    public boolean alertsEnabled() {
        return alertsEnabled;
    }

    public void alertsEnabled(boolean value) {
        this.alertsEnabled = value;
    }

    public int warningsView() {
        return warningsView;
    }

    public void warningsView(int value) {
        this.warningsView = value;
    }

    public boolean verboseAlerts() {
        return verboseAlerts;
    }

    public void verboseAlerts(boolean value) {
        this.verboseAlerts = value;
    }

    public boolean setbackEnabled() {
        return setbackEnabled;
    }

    public void setbackEnabled(boolean value) {
        this.setbackEnabled = value;
    }

    private final dev.snuffac.core.enforcement.PreventionSignal prevention =
            new dev.snuffac.core.enforcement.PreventionSignal();

    public dev.snuffac.core.enforcement.PreventionSignal prevention() {
        return prevention;
    }

    public boolean joined() {
        return joined;
    }

    public void joined(boolean value) {
        this.joined = value;
    }

    public boolean dirty() {
        return dirty;
    }

    public void dirty(boolean value) {
        this.dirty = value;
    }

    public long lastViolationMillis() {
        return lastViolationMillis;
    }

    public int violationsThisSecond() {
        return violationsThisSecond;
    }

    public long violationsThisSecondWindow() {
        return violationsThisSecondWindow;
    }

    public double totalViolationLevel() {
        return totalViolationLevel;
    }

    public void registerCheck(String key, CheckState state) {
        checkStates.put(key, state);
    }

    public CheckState checkState(String key) {
        return checkStates.get(key);
    }

    public Map<String, CheckState> checkStates() {
        return java.util.Collections.unmodifiableMap(checkStates);
    }

    public Object checkData(String checkKey) {
        return checkData.get(checkKey);
    }

    public void putCheckData(String checkKey, Object value) {
        if (value == null) {
            checkData.remove(checkKey);
        } else {
            checkData.put(checkKey, value);
        }
    }

    public void addHistory(dev.snuffac.core.violation.ViolationRecord record) {
        if (history.size() >= VIOLATION_HISTORY_LIMIT) {
            history.removeLast();
        }
        history.addFirst(record);
        recent.add(record);
        if (recent.size() > 256) {
            recent.remove(0);
        }
        lastViolationMillis = record.timestampMillis();
        totalViolationLevel += record.violationLevel();
    }

    public Deque<dev.snuffac.core.violation.ViolationRecord> history() {
        return history;
    }

    public List<dev.snuffac.core.violation.ViolationRecord> recent(int limit) {
        if (limit >= recent.size()) {
            return List.copyOf(recent);
        }
        return List.copyOf(recent.subList(recent.size() - limit, recent.size()));
    }

    public void tickCounters(long nowMillis) {
        if (violationWindowStart == 0L) {
            violationWindowStart = nowMillis;
        }
        if (nowMillis - violationWindowStart >= 1000L) {
            violationsThisSecond = 0;
            violationWindowStart = nowMillis;
        }
        movement.tickCounters();
        network.tick(20.0, nowMillis);
        confidence.tick(nowMillis);
        for (CheckState state : checkStates.values()) {
            state.tick();
        }
        totalViolationLevel = Math.max(totalViolationLevel - 0.05, 0.0);
        this.dirty = false;
    }

    public void incrementViolationCounter() {
        violationsThisSecond++;
    }

    public void reset() {
        worldCache = PlayerWorldCache.empty(movement.position());
        equipment.reset();
        confidence.reset();
        mining.clear();
        combatEnvironment(dev.snuffac.core.combat.CombatEnvironment.empty());
        checkData.clear();
        movement.reset();
        combat.reset();
        world.reset();
        network.reset();
    }
}
