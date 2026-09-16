package com.kirazium.emotes.ui;

import com.kirazium.emotes.api.PlayResult;
import com.kirazium.emotes.core.*;
import com.kirazium.emotes.util.Messages;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.*;
import java.util.*;
import java.util.logging.Level;

/** Optional public-API-only adapter. No UltimateUI classes or licensed assets are redistributed. */
public final class UltimateEmoteMenu implements Listener, AutoCloseable {
    private static final String PREFIX = "kirazium_emotes_";
    private static final List<String> ICON_IDS = List.of("meditacion", "shakie", "throwhead", "kiss",
            "backflip", "idk", "bless", "bye", "poll", "booya", "booya_2", "naruto",
            "dance", "dance_2", "dance_3", "relax", "sinav");
    private static final int[][] RING = {{0,0},{1,0},{2,0},{2,1},{2,2},{1,2},{0,2},{0,1}};
    private final JavaPlugin plugin;
    private final EmoteManager manager;
    private final EmoteRegistry registry;
    private final QuickSlots slots;
    private final Object api;
    private final Method create, addRaw, screenSize, open, close, currentPage;
    private final Method clickPlayer, clickPage, clickBlock, clickType;
    private final Map<UUID, Page> pages = new HashMap<>();
    private boolean closed;

    @SuppressWarnings("unchecked")
    public UltimateEmoteMenu(JavaPlugin plugin, Plugin ultimate, EmoteManager manager, EmoteRegistry registry)
            throws ReflectiveOperationException {
        this.plugin = plugin; this.manager = manager; this.registry = registry;
        slots = new QuickSlots(plugin.getDataFolder().toPath().resolve("wheels"));
        ClassLoader loader = ultimate.getClass().getClassLoader();
        Class<?> apiClass = Class.forName("dev.xqedii.ultimateUI.api.UltimateUIAPI", true, loader);
        if (!Boolean.TRUE.equals(apiClass.getMethod("isAvailable").invoke(null)))
            throw new IllegalStateException("UltimateUI API is not available");
        api = apiClass.getMethod("get").invoke(null);
        create = apiClass.getMethod("createGui", String.class);
        Class<?> builder = create.getReturnType();
        screenSize = builder.getMethod("screenSize", double.class, double.class);
        addRaw = builder.getMethod("addRaw", Map.class);
        open = apiClass.getMethod("openGui", Player.class, builder);
        close = apiClass.getMethod("closeGui", Player.class, String.class);
        currentPage = apiClass.getMethod("getOpenGuiName", Player.class);
        Class<? extends Event> event = (Class<? extends Event>) Class.forName(
                "dev.xqedii.ultimateUI.api.event.UltimateUIBlockClickEvent", true, loader).asSubclass(Event.class);
        clickPlayer = event.getMethod("getPlayer"); clickPage = event.getMethod("getPageName");
        clickBlock = event.getMethod("getBlockId"); clickType = event.getMethod("getClickType");
        plugin.getServer().getPluginManager().registerEvent(event, this, EventPriority.NORMAL,
                (listener, e) -> handleClick(e), plugin, true);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        show(player, -1);
    }

    private List<String> available() { return registry.all().stream().map(EmoteDefinition::id).toList(); }

    private void show(Player player, int selectingSlot) {
        try {
            String name = PREFIX + player.getUniqueId() + (selectingSlot < 0 ? "_wheel" : "_choices");
            Object builder = create.invoke(api, name);
            screenSize.invoke(builder, 1920.0, 1080.0);
            List<String> ids = selectingSlot < 0 ? slots.load(player.getUniqueId(), available()) : available();
            Map<String, Action> actions = new HashMap<>();
            String active = manager.active(player.getUniqueId()).map(EmoteDefinition::id).orElse("");
            for (int i = 0; i < ids.size(); i++) {
                String id = ids.get(i);
                if (id.isEmpty()) continue;
                int col = selectingSlot < 0 ? RING[i][0] : i % 6;
                int row = selectingSlot < 0 ? RING[i][1] : i / 6;
                double x = (selectingSlot < 0 ? 714 : 468) + col * 164;
                double y = 270 + row * 164;
                String key = "emote_" + i;
                Action action = new Action(i, id);
                actions.put(key, action); actions.put(key + "_icon", action); actions.put(key + "_label", action);
                block(builder, key, "block", "", x, y, 156, 156,
                        active.equals(id) ? "638F43" : "4A4A4A", 195, 10);
                int icon = ICON_IDS.indexOf(id);
                if (icon >= 0) {
                    String glyph = "<font:kiraziumemotes:icons>" + (char)(0xE100 + icon) + "</font>";
                    block(builder, key + "_icon", "block", glyph, x + 22, y + 8, 112, 112, "FFFFFF", 255, 20);
                }
                String label = registry.find(id).map(EmoteDefinition::displayName).orElse(id);
                block(builder, key + "_label", "text", label, x + 6, y + 130, 144, 18, "EEEEEE", 255, 30);
            }
            block(builder, "hint", "text", selectingSlot < 0 ? "Sol tık: oynat   ·   Sağ tık: değiştir" : "Kutunun yeni animasyonunu seç",
                    660, 790, 600, 22, "DDDDDD", 255, 40);
            block(builder, "stop", "text", selectingSlot < 0 ? "Durdur" : "Geri", 810, 840, 140, 24, "DDDDDD", 255, 40);
            block(builder, "close", "text", "Kapat", 990, 840, 140, 24, "DDDDDD", 255, 40);
            // openGui(builder) saves this player's two reusable pages using UltimateUI's public API.
            if (!Boolean.TRUE.equals(open.invoke(api, player, builder))) throw new IllegalStateException("UltimateUI refused to open " + name);
            pages.put(player.getUniqueId(), new Page(name, selectingSlot, actions));
        } catch (Exception e) { fail(player, "Menü açılamadı.", e); }
    }

    private void block(Object builder, String id, String type, String text, double x, double y,
                       double width, double height, String color, int opacity, int layer) throws ReflectiveOperationException {
        Map<String, Object> data = new LinkedHashMap<>();
        if (type.equals("text")) {
            // UltimateUI text height is 7/60 of its nominal box height, unlike 64px bitmap blocks.
            double scale = height * 60.0 / 7.0;
            x += (width - scale) / 2.0;
            width = scale; height = scale;
        }
        data.put("id", id); data.put("type", type); data.put("enabled", true); data.put("layer", layer);
        data.put("opacity", opacity); data.put("color", color);
        data.put("position", Map.of("x", x, "y", y)); data.put("size", Map.of("width", width, "height", height));
        if (type.equals("text")) { data.put("text", text); data.put("font", "default"); }
        else {
            data.put("unicode", text.isEmpty() ? "\uE67B" : text);
            if (text.isEmpty()) data.put("outline", Map.of("size", 1, "color", "898989"));
        }
        addRaw.invoke(builder, data);
    }

    private void handleClick(Event event) {
        try {
            Player player = (Player) clickPlayer.invoke(event);
            Page page = pages.get(player.getUniqueId());
            if (page == null || !page.name.equals(clickPage.invoke(event))) return;
            String type = clickType.invoke(event).toString();
            if (!type.equals("LEFT") && !type.equals("RIGHT")) return;
            String block = (String) clickBlock.invoke(event);
            if (event instanceof Cancellable cancellable) cancellable.setCancelled(true);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                try {
                    if (closed || !player.isOnline() || pages.get(player.getUniqueId()) != page
                            || !page.name.equals(currentPage.invoke(api, player))) return;
                    if (block.equals("close")) { closePage(player, page); return; }
                    if (block.equals("stop")) {
                        if (page.slot >= 0) show(player, -1);
                        else { closePage(player, page); manager.stop(player, EmoteManager.StopReason.USER); }
                        return;
                    }
                    Action action = page.actions.get(block);
                    if (action == null) return;
                    if (page.slot >= 0) {
                        slots.assign(player.getUniqueId(), page.slot, action.id, available());
                        show(player, -1);
                    } else if (type.equals("RIGHT")) show(player, action.slot);
                    else {
                        closePage(player, page);
                        // UltimateUI closes its camera and teleports back. Play AFTER that lifecycle event.
                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                            if (closed || !player.isOnline() || player.isDead() || pages.containsKey(player.getUniqueId())) return;
                            PlayResult result = manager.play(player, action.id);
                            if (result != PlayResult.SUCCESS) Messages.error(player, "Emote başlatılamadı: " + result);
                        }, 2L);
                    }
                } catch (Exception e) { fail(player, "Menü işlemi tamamlanamadı.", e); }
            });
        } catch (ReflectiveOperationException e) { plugin.getLogger().log(Level.WARNING, "UltimateUI click bridge failed", e); }
    }

    private void closePage(Player player, Page page) throws ReflectiveOperationException {
        close.invoke(api, player, page.name);
        pages.remove(player.getUniqueId(), page);
    }

    private void fail(Player player, String message, Exception e) {
        Messages.error(player, message + " Konsolu kontrol et.");
        plugin.getLogger().log(Level.WARNING, message, e);
    }

    @EventHandler public void onQuit(PlayerQuitEvent event) { pages.remove(event.getPlayer().getUniqueId()); }

    @Override public void close() {
        closed = true;
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            Page page = pages.get(player.getUniqueId());
            if (page != null) try { closePage(player, page); } catch (ReflectiveOperationException e) {
                plugin.getLogger().log(Level.WARNING, "Could not close emote UI", e);
            }
        }
        pages.clear(); HandlerList.unregisterAll(this);
    }

    private record Action(int slot, String id) {}
    private record Page(String name, int slot, Map<String, Action> actions) {}
}
