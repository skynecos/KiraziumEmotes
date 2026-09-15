package com.kirazium.emotes.listener;

import com.kirazium.emotes.core.EmoteDefinition;
import com.kirazium.emotes.core.EmoteManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Optional;

public final class EmoteLifecycleListener implements Listener {
    private final EmoteManager manager;

    public EmoteLifecycleListener(EmoteManager manager) {
        this.manager = manager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Optional<EmoteDefinition> active = manager.active(event.getPlayer().getUniqueId());
        if (active.isEmpty() || !active.get().cancelOnMove()) return;
        if (positionChanged(event.getFrom(), event.getTo())) {
            manager.stop(event.getPlayer(), EmoteManager.StopReason.MOVE);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        Optional<EmoteDefinition> active = manager.active(event.getPlayer().getUniqueId());
        if (active.isPresent() && active.get().cancelOnTeleport()) {
            manager.stop(event.getPlayer(), EmoteManager.StopReason.TELEPORT);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Optional<EmoteDefinition> active = manager.active(player.getUniqueId());
        if (active.isPresent() && active.get().cancelOnDamage()) {
            manager.stop(player, EmoteManager.StopReason.DAMAGE);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        manager.stop(event.getPlayer(), EmoteManager.StopReason.QUIT);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        manager.stop(event.getPlayer(), EmoteManager.StopReason.DEATH);
    }

    private static boolean positionChanged(Location from, Location to) {
        if (to == null || from.getWorld() != to.getWorld()) return true;
        return Double.compare(from.getX(), to.getX()) != 0
                || Double.compare(from.getY(), to.getY()) != 0
                || Double.compare(from.getZ(), to.getZ()) != 0;
    }
}
