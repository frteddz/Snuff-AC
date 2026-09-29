package dev.snuffac.paper.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class MenuRegistryTest {

    private static final UUID PLAYER = UUID.randomUUID();

    @Test
    void aStaleCloseEventDoesNotRemoveTheNewlyOpenedMenu() {
        MenuRegistry registry = new MenuRegistry();
        Object oldMenu = new Object();
        Object newMenu = new Object();
        Object oldInventory = new Object();
        Object newInventory = new Object();

        registry.register(PLAYER, newMenu, newInventory);
        registry.forgetOnClose(PLAYER, oldMenu, oldInventory);

        assertSame(newMenu, registry.menuOf(PLAYER),
                "a close event for the previous screen must not unregister the current one");
    }

    @Test
    void theMatchingCloseEventStillUnregisters() {
        MenuRegistry registry = new MenuRegistry();
        Object menu = new Object();
        Object inventory = new Object();
        registry.register(PLAYER, menu, inventory);
        registry.forgetOnClose(PLAYER, menu, inventory);
        assertNull(registry.menuOf(PLAYER), "closing the live menu must clear it");
    }

    @Test
    void registerThenStaleCloseLeavesTheMenuUsable() {
        MenuRegistry registry = new MenuRegistry();
        Object main = new Object();
        Object child = new Object();
        registry.register(PLAYER, main, new Object());
        registry.register(PLAYER, child, new Object());
        registry.forgetOnClose(PLAYER, main, new Object());
        assertSame(child, registry.menuOf(PLAYER));
        assertTrue(registry.isOpen(PLAYER));
    }

    @Test
    void aSecondCloseEventForTheSameScreenIsHarmless() {
        MenuRegistry registry = new MenuRegistry();
        Object menu = new Object();
        Object inventory = new Object();
        registry.register(PLAYER, menu, inventory);
        registry.forgetOnClose(PLAYER, menu, inventory);
        registry.forgetOnClose(PLAYER, menu, inventory);
        assertFalse(registry.isOpen(PLAYER));
    }

    @Test
    void registeringNullIsIgnored() {
        MenuRegistry registry = new MenuRegistry();
        registry.register(PLAYER, null, null);
        assertEquals(0, registry.size());
    }

    @Test
    void forgetRemovesRegardless() {
        MenuRegistry registry = new MenuRegistry();
        registry.register(PLAYER, new Object(), new Object());
        registry.forget(PLAYER);
        assertNull(registry.menuOf(PLAYER));
    }

    @Test
    void clearEmptiesEverything() {
        MenuRegistry registry = new MenuRegistry();
        registry.register(PLAYER, new Object(), new Object());
        registry.register(UUID.randomUUID(), new Object(), new Object());
        registry.clear();
        assertEquals(0, registry.size());
    }
}
