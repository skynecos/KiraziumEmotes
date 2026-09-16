package com.kirazium.emotes.render.equipment;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ProtocolLibEquipmentVisibilityController extends BukkitEquipmentVisibilityController {
    private final ProtocolManager protocolManager;
    private final Set<Integer> hiddenEntityIds = ConcurrentHashMap.newKeySet();
    private final PacketListener listener;

    public ProtocolLibEquipmentVisibilityController(Plugin plugin) {
        protocolManager = ProtocolLibrary.getProtocolManager();
        if (protocolManager == null) {
            throw new IllegalStateException("ProtocolLib protocol manager is not initialized");
        }

        listener = new PacketAdapter(plugin, ListenerPriority.HIGHEST, PacketType.Play.Server.ENTITY_EQUIPMENT) {
            @Override
            public void onPacketSending(PacketEvent event) {
                Integer entityId = event.getPacket().getIntegers().readSafely(0);
                if (entityId == null || !hiddenEntityIds.contains(entityId)) return;

                List<Pair<EnumWrappers.ItemSlot, ItemStack>> equipment =
                        event.getPacket().getSlotStackPairLists().readSafely(0);
                if (equipment == null || equipment.isEmpty()) return;

                List<Pair<EnumWrappers.ItemSlot, ItemStack>> hidden = new ArrayList<>(equipment.size());
                for (Pair<EnumWrappers.ItemSlot, ItemStack> entry : equipment) {
                    hidden.add(new Pair<>(entry.getFirst(), AIR.clone()));
                }
                event.getPacket().getSlotStackPairLists().write(0, hidden);
            }
        };
        protocolManager.addPacketListener(listener);
    }

    @Override
    public void hide(Player subject) {
        hiddenEntityIds.add(subject.getEntityId());
        super.hide(subject);
    }

    @Override
    public void restore(Player subject) {
        hiddenEntityIds.remove(subject.getEntityId());
        super.restore(subject);
    }

    @Override
    public void close() {
        hiddenEntityIds.clear();
        protocolManager.removePacketListener(listener);
    }
}
