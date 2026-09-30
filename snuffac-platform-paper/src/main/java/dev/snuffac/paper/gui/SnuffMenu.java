package dev.snuffac.paper.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public abstract class SnuffMenu implements InventoryHolder {


    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final MenuRegistry REGISTRY = new MenuRegistry();

    protected final Plugin plugin;
    private final org.bukkit.NamespacedKey actionKey;

    public org.bukkit.NamespacedKey actionKey() {
        return actionKey;
    }
    private final Inventory inventory;
    private String title;
    private SnuffMenu parent;

    protected GuiLayout.Layout layout;

    protected SnuffMenu(Plugin plugin, int size, String title) {
        this(plugin, size, title, null);
    }

    protected SnuffMenu(Plugin plugin, int size, String title, GuiLayout.Layout layout) {
        this.layout = layout;
        this.plugin = plugin;
        this.actionKey = new org.bukkit.NamespacedKey(plugin, "action");
        this.title = title;
        this.inventory = Bukkit.createInventory(this, size, component(title));
    }

    public void build() {
        render();
    }

    protected void fill(Material material) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) != null) {
                continue;
            }
            set(slot, material, " ", null, null);
        }
    }

    protected void fillFromLayout() {
        GuiLayout.Layout layout = layout();
        if (layout == null) {
            return;
        }
        for (GuiLayout.Button button : layout.buttons()) {
            applyButton(button);
        }
        if (layout.fillerEnabled()) {
            fill(layout.filler());
        }
    }

    public GuiLayout.Layout layout() {
        return layout;
    }

    private void applyButton(GuiLayout.Button button) {
        if (!button.enabled()) {
            return;
        }
        if (button.permission() != null && !button.permission().isBlank()
                && renderPlayer != null
            && !renderPlayer.hasPermission(button.permission())) {
            return;
        }
        set(button.slot(), button.material(), button.name(), button.lore(),
                button.action().isEmpty() ? null : button.action(),
                button.amount(), button.glow());
    }

    protected org.bukkit.entity.Player renderPlayer;

    protected abstract void render();

    public void open(Player player) {
        REGISTRY.register(player.getUniqueId(), this, inventory);
        player.openInventory(inventory);
    }

    public void openParent(Player player) {
        SnuffMenu target = parent();
        if (target == null) {
            close(player);
            return;
        }
        // the parent was handed to us by whoever opened us, and may never have
        // been built, which showed staff an empty window
        target.build();
        REGISTRY.register(player.getUniqueId(), target, target.getInventory());
        target.open(player);
    }

    public void close(Player player) {
        REGISTRY.forget(player.getUniqueId());
        player.closeInventory();
    }

    public void setParent(SnuffMenu parent) {
        this.parent = parent;
    }

    public SnuffMenu parent() {
        return parent;
    }

    public String title() {
        return title;
    }

    public void title(String value) {
        this.title = value;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public static SnuffMenu of(Player player) {
        Object current = REGISTRY.menuOf(player.getUniqueId());
        return current instanceof SnuffMenu menu ? menu : null;
    }

    public static void forget(UUID playerId) {
        REGISTRY.forget(playerId);
    }

    public static void forget(UUID playerId, SnuffMenu closing, org.bukkit.inventory.Inventory inventory) {
        REGISTRY.forgetOnClose(playerId, closing, inventory);
    }

    public static void closeAll() {
        REGISTRY.clear();
    }

    public void set(int slot, Material material, String name, List<String> lore, String action) {
        set(slot, material, name, lore, action, 1, false);
    }

    public void set(int slot, Material material, String name, List<String> lore, String action,
            int amount, boolean glow) {
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }
        if (material == null) {
            return;
        }
        ItemStack item = new ItemStack(material, clampAmount(amount));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (name != null && !name.isEmpty()) {
                meta.displayName(component(name));
            }
            if (lore != null && !lore.isEmpty()) {
                List<Component> rendered = new ArrayList<>(lore.size());
                for (String line : lore) {
                    rendered.add(component(line));
                }
                meta.lore(rendered);
            }
            if (action != null && !action.isEmpty()) {
                meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
            }
            if (glow) {
                meta.setEnchantmentGlintOverride(true);
            }
            item.setItemMeta(meta);
        }
        inventory.setItem(slot, item);
    }

    public String actionOf(Inventory inventory, int slot) {
        if (slot < 0 || slot >= inventory.getSize()) {
            return null;
        }
        ItemStack item = inventory.getItem(slot);
        if (item == null) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        return meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
    }

    public void onClick(InventoryClickEvent event) {
    }

    public void onDrag(InventoryDragEvent event) {
    }

    public void onClose(InventoryCloseEvent event) {
    }

    private static int clampAmount(int amount) {
        if (amount < 1) {
            return 1;
        }
        return Math.min(amount, 64);
    }

    protected static Component component(String text) {
        try {
            return MINI.deserialize(text == null ? "" : text);
        } catch (RuntimeException exception) {
            return Component.text(text == null ? "" : text);
        }
    }

}
