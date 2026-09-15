package com.kirazium.emotes.core;

import com.kirazium.emotes.api.PlayResult;
import com.kirazium.emotes.render.EmoteRenderer;
import com.kirazium.emotes.render.RenderHandle;
import com.kirazium.emotes.render.RendererRegistry;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

public final class EmoteManager {
    private final Plugin plugin;
    private final EmoteRegistry emotes;
    private final RendererRegistry renderers;
    private final Map<UUID, Session> sessions = new HashMap<>();

    public EmoteManager(Plugin plugin, EmoteRegistry emotes, RendererRegistry renderers) {
        this.plugin = plugin;
        this.emotes = emotes;
        this.renderers = renderers;
    }

    public PlayResult play(Player player, String emoteId) {
        UUID uuid = player.getUniqueId();
        if (sessions.containsKey(uuid)) return PlayResult.ALREADY_PLAYING;

        Optional<EmoteDefinition> optionalDefinition = emotes.find(emoteId);
        if (optionalDefinition.isEmpty()) return PlayResult.NOT_FOUND;

        EmoteDefinition definition = optionalDefinition.get();
        Optional<EmoteRenderer> optionalRenderer = renderers.find(definition.renderer());
        if (optionalRenderer.isEmpty() || !optionalRenderer.get().isAvailable()) {
            return PlayResult.RENDERER_UNAVAILABLE;
        }

        try {
            RenderHandle handle = optionalRenderer.get().play(player, definition);
            Session session = new Session(definition, handle);
            sessions.put(uuid, session);

            if (definition.durationTicks() > 0) {
                BukkitTask timeoutTask = Bukkit.getScheduler().runTaskLater(
                        plugin,
                        () -> stop(uuid, StopReason.TIMEOUT),
                        definition.durationTicks()
                );
                session.timeoutTask = timeoutTask;
            }
            return PlayResult.SUCCESS;
        } catch (Exception exception) {
            plugin.getLogger().log(Level.SEVERE,
                    "Failed to play emote '" + definition.id() + "' for " + player.getName(), exception);
            return PlayResult.FAILED;
        }
    }

    public boolean stop(Player player, StopReason reason) {
        return stop(player.getUniqueId(), reason);
    }

    public boolean stop(UUID playerId, StopReason reason) {
        Session session = sessions.remove(playerId);
        if (session == null) return false;

        if (session.timeoutTask != null) {
            session.timeoutTask.cancel();
        }

        try {
            session.handle.stop();
        } catch (Exception exception) {
            plugin.getLogger().log(Level.WARNING,
                    "Failed to stop emote '" + session.definition.id() + "' (reason=" + reason + ")", exception);
        }
        return true;
    }

    public boolean isPlaying(UUID playerId) {
        return sessions.containsKey(playerId);
    }

    public Optional<EmoteDefinition> active(UUID playerId) {
        Session session = sessions.get(playerId);
        return session == null ? Optional.empty() : Optional.of(session.definition);
    }

    public void stopAll() {
        for (UUID uuid : java.util.List.copyOf(sessions.keySet())) {
            stop(uuid, StopReason.PLUGIN_DISABLE);
        }
    }

    public enum StopReason {
        USER,
        MOVE,
        DAMAGE,
        TELEPORT,
        QUIT,
        DEATH,
        TIMEOUT,
        PLUGIN_DISABLE
    }

    private static final class Session {
        private final EmoteDefinition definition;
        private final RenderHandle handle;
        private BukkitTask timeoutTask;

        private Session(EmoteDefinition definition, RenderHandle handle) {
            this.definition = definition;
            this.handle = handle;
        }
    }
}
