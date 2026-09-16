package com.kirazium.emotes.ui;

import com.kirazium.emotes.api.PlayResult;
import com.kirazium.emotes.core.EmoteDefinition;
import com.kirazium.emotes.core.EmoteManager;
import com.kirazium.emotes.core.EmoteRegistry;
import com.kirazium.emotes.util.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Server-side emote selector. This deliberately uses a Bukkit inventory so
 * KiraziumEmotes never requires a client mod or custom key binding.
 */
public final class EmoteMenu implements Listener, AutoCloseable {
    private static final Component TITLE = Component.text("Kirazium Emotes", NamedTextColor.LIGHT_PURPLE);
    private static final int SIZE = 54;
    private static final int MAX_EMOTES_PER_PAGE = 45;

    private final EmoteManager manager;
    private final EmoteRegistry registry;
    private final NamespacedKey emoteIdKey;
    private final NamespacedKey actionKey;
    private UltimateEmoteMenu ultimate;

    public EmoteMenu(JavaPlugin plugin, EmoteManager manager, EmoteRegistry registry) {
        this.manager = manager;
        this.registry = registry;
        this.emoteIdKey = new NamespacedKey(plugin, "emote_id");
        this.actionKey = new NamespacedKey(plugin, "menu_action");
        var dependency = plugin.getServer().getPluginManager().getPlugin("UltimateUI");
        if (dependency != null && dependency.isEnabled() && plugin.getConfig().getBoolean("ui.ultimateui", true)) {
            try {
                ultimate = new UltimateEmoteMenu(plugin, dependency, manager, registry);
                plugin.getLogger().info("UltimateUI eight-slot emote menu enabled (left: play, right: assign).");
            } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE,
                        "UltimateUI API is incompatible; using inventory menu.", exception);
            }
        }
    }

    public void open(Player player) {
        if (ultimate != null) { ultimate.open(player); return; }
        MenuHolder holder = new MenuHolder();
        Inventory inventory = Bukkit.createInventory(holder, SIZE, TITLE);
        holder.inventory = inventory;

        int slot = 0;
        for (EmoteDefinition definition : registry.all()) {
            if (slot >= MAX_EMOTES_PER_PAGE) break;
            inventory.setItem(slot++, emoteItem(definition));
        }

        if (slot == 0) {
            inventory.setItem(22, infoItem());
        }

        inventory.setItem(49, stopItem());
        player.openInventory(inventory);
    }

    @Override public void close() { if (ultimate != null) ultimate.close(); }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof MenuHolder)) return;

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() != top) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        if ("stop".equals(action)) {
            player.closeInventory();
            if (manager.stop(player, EmoteManager.StopReason.USER)) {
                Messages.success(player, "Emote durduruldu.");
            } else {
                Messages.info(player, "Şu anda oynattığın bir emote yok.");
            }
            return;
        }

        String emoteId = meta.getPersistentDataContainer().get(emoteIdKey, PersistentDataType.STRING);
        if (emoteId == null) return;

        player.closeInventory();
        PlayResult result = manager.play(player, emoteId);
        switch (result) {
            case SUCCESS -> Messages.success(player, "Emote başlatıldı.");
            case NOT_FOUND -> Messages.error(player, "Bu emote artık mevcut değil.");
            case ALREADY_PLAYING -> Messages.error(player, "Önce mevcut emoteyi durdurmalısın.");
            case RENDERER_UNAVAILABLE -> Messages.error(player, "Bu emote için gerekli animasyon sistemi aktif değil.");
            case FAILED -> Messages.error(player, "Emote başlatılamadı. Konsol logunu kontrol et.");
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof MenuHolder) {
            event.setCancelled(true);
        }
    }

    private ItemStack emoteItem(EmoteDefinition definition) {
        ItemStack item = new ItemStack(Material.ARMOR_STAND);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(definition.displayName(), NamedTextColor.LIGHT_PURPLE));
        meta.lore(List.of(
                Component.text("Oynatmak için tıkla", NamedTextColor.GRAY)
        ));
        meta.getPersistentDataContainer().set(emoteIdKey, PersistentDataType.STRING, definition.id());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack stopItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Emoteyi Durdur", NamedTextColor.RED));
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "stop");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack infoItem() {
        ItemStack item = new ItemStack(Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Henüz emote eklenmedi", NamedTextColor.GRAY));
        meta.lore(List.of(
                Component.text("Emoteler emotes.yml üzerinden tanımlanır.", NamedTextColor.DARK_GRAY)
        ));
        item.setItemMeta(meta);
        return item;
    }

    private static final class MenuHolder implements InventoryHolder {
        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
