package dev.snuffac.core.check;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.snuffac.api.CheckCategory;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CheckRegistryTest {

    private CheckRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new CheckRegistry();
    }

    private static Check stub(String key, CheckCategory category, Set<dev.snuffac.core.packet.PacketType> interests) {
        return new Check() {

            @Override
            public String key() {
                return key;
            }

            @Override
            public CheckCategory category() {
                return category;
            }

            @Override
            public Set<dev.snuffac.core.packet.PacketType> packetInterests() {
                return interests;
            }
        };
    }

    @Test
    @DisplayName("checks are registered under a lower case key")
    void registersLowerCase() {
        registry.register(stub("FastBreak", CheckCategory.WORLD, Set.of()));
        assertNotNull(registry.check("fastbreak"));
        assertNotNull(registry.check("FASTBREAK"));
    }

    @Test
    @DisplayName("duplicate keys are rejected so a config typo cannot shadow a check")
    void rejectsDuplicates() {
        registry.register(stub("fly", CheckCategory.MOVEMENT, Set.of()));
        assertThrows(IllegalStateException.class,
                () -> registry.register(stub("FLY", CheckCategory.MOVEMENT, Set.of())));
    }

    @Test
    @DisplayName("registration is closed once the registry is frozen")
    void rejectsRegistrationAfterFreeze() {
        registry.register(stub("fly", CheckCategory.MOVEMENT, Set.of()));
        registry.freeze();
        assertTrue(registry.isFrozen());
        assertThrows(IllegalStateException.class,
                () -> registry.register(stub("speed", CheckCategory.MOVEMENT, Set.of())));
    }

    @Test
    @DisplayName("every registered check gets a default configuration")
    void providesDefaultConfig() {
        registry.register(stub("fly", CheckCategory.MOVEMENT, Set.of()));
        assertNotNull(registry.config("fly"));
        assertTrue(registry.config("fly").enabled());
        assertEquals(CheckCategory.MOVEMENT, registry.config("fly").category());
    }

    @Test
    @DisplayName("dispatch tables contain only checks that declared the packet type")
    void buildsDispatchTables() {
        Set<dev.snuffac.core.packet.PacketType> movement = Set.of(dev.snuffac.core.packet.PacketType.MOVEMENT);
        registry.register(stub("fly", CheckCategory.MOVEMENT, movement));
        registry.register(stub("reach", CheckCategory.COMBAT, Set.of(dev.snuffac.core.packet.PacketType.ATTACK)));
        registry.register(stub("idler", CheckCategory.MOVEMENT, Set.of()));
        registry.freeze();

        assertEquals(1, registry.dispatchFor(dev.snuffac.core.packet.PacketType.MOVEMENT).size());
        assertEquals(1, registry.dispatchFor(dev.snuffac.core.packet.PacketType.ATTACK).size());
        assertTrue(registry.dispatchFor(dev.snuffac.core.packet.PacketType.BLOCK_BREAK).isEmpty());
        assertEquals(3, registry.all().size());
    }

    @Test
    @DisplayName("a check without packet interests is still ticked every tick")
    void allChecksAreTicked() {
        registry.register(stub("idle", CheckCategory.PACKET, Set.of()));
        registry.freeze();
        assertEquals(1, registry.tickDispatch().size());
    }

    @Test
    @DisplayName("categories group checks for the command listing")
    void groupsByCategory() {
        registry.register(stub("fly", CheckCategory.MOVEMENT, Set.of()));
        registry.register(stub("speed", CheckCategory.MOVEMENT, Set.of()));
        registry.register(stub("reach", CheckCategory.COMBAT, Set.of()));
        registry.freeze();
        assertEquals(2, registry.byCategory(CheckCategory.MOVEMENT).size());
        assertEquals(1, registry.byCategory(CheckCategory.COMBAT).size());
        assertTrue(registry.byCategory(CheckCategory.WORLD).isEmpty());
    }

    @Test
    @DisplayName("configurations load from a nested config source")
    void loadsConfigurations() {
        registry.register(stub("fly", CheckCategory.MOVEMENT, Set.of()));
        registry.freeze();

        java.util.Map<String, Object> fly = new java.util.LinkedHashMap<>();
        fly.put("enabled", "false");
        fly.put("buffer-threshold", 33.0);
        java.util.Map<String, Object> movement = new java.util.LinkedHashMap<>();
        movement.put("fly", fly);
        java.util.Map<String, Object> checks = new java.util.LinkedHashMap<>();
        checks.put("movement", movement);
        java.util.Map<String, Object> root = new java.util.LinkedHashMap<>();
        root.put("checks", checks);

        registry.loadConfigurations(dev.snuffac.core.config.ConfigSource.ofMap(root));
        assertFalse(registry.config("fly").enabled());
        assertEquals(33.0, registry.config("fly").bufferThreshold(), 1.0E-9);
    }

    @Test
    @DisplayName("the built in check set has unique keys and complete metadata")
    void builtInChecksAreConsistent() {
        var core = new dev.snuffac.core.SnuffCore(
                dev.snuffac.api.SnuffPlatform.PAPER, new dev.snuffac.core.log.RecordingLogger());
        List<String> keys = core.checkKeys();
        assertFalse(keys.isEmpty(), "there must be registered checks");
        assertEquals(new HashSet<>(keys).size(), keys.size(), "check keys must be unique");
        for (String key : keys) {
            Check check = core.registry().check(key);
            assertNotNull(check, "check should be resolvable by key: " + key);
            assertNotNull(check.category(), "check should declare a category: " + key);
            assertNotNull(core.registry().config(key), "check should have configuration: " + key);
        }
        for (CheckCategory category : CheckCategory.values()) {
            assertFalse(core.registry().byCategory(category).isEmpty(),
                    "every category should have at least one check: " + category);
        }
    }
}
