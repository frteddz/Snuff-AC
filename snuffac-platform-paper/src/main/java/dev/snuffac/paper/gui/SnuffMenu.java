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
    private static final Map<UUID, SnuffMenu> OPEN = new ConcurrentHashMap<>();

    protected final Plugin plugin;
    private final org.bukkit.NamespacedKey actionKey;
    private final Inventory inventory;
    private String title;
    private SnuffMenu parent;

    protected SnuffMenu(Plugin plugin, int size, String title) {
        this.plugin = plugin;
        this.actionKey = new org.bukkit.NamespacedKey(plugin, "action");
        this.title = title;
        this.inventory = Bukkit.createInventory(this, size, component(title));
    }

    public void build() {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            set(slot, Material.BLACK_STAINED_GLASS_PANE, " ", null, null);
        }
        render();
    }

    protected abstract void render();

    public void open(Player player) {
        OPEN.put(player.getUniqueId(), this);
        player.openInventory(inventory);
    }

    public void openParent(Player player) {
        SnuffMenu target = parent();
        if (target == null) {
            close(player);
            return;
        }
        target.open(player);
    }

    public void close(Player player) {
        OPEN.remove(player.getUniqueId());
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
        return OPEN.get(player.getUniqueId());
    }

    public static void forget(UUID playerId) {
        OPEN.remove(playerId);
    }

    public static void closeAll() {
        for (UUID id : new ArrayList<>(OPEN.keySet())) {
            OPEN.remove(id);
        }
    }

    public void set(int slot, Material material, String name, List<String> lore, String action) {
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }
        ItemStack item = new ItemStack(material);
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

    protected static Component component(String text) {
        try {
            return MINI.deserialize(text == null ? "" : text);
        } catch (RuntimeException exception) {
            return Component.text(text == null ? "" : text);
        }
    }

}
