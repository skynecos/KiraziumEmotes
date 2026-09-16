package com.kirazium.emotes.render.equipment;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class BukkitEquipmentVisibilityController implements EquipmentVisibilityController {
    protected static final ItemStack AIR = new ItemStack(Material.AIR);
    protected static final EquipmentSlot[] HIDDEN_EQUIPMENT = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET,
            EquipmentSlot.HAND,
            EquipmentSlot.OFF_HAND
    };

    @Override
    public void hide(Player subject) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            for (EquipmentSlot slot : HIDDEN_EQUIPMENT) {
                viewer.sendEquipmentChange(subject, slot, AIR);
            }
        }
    }

    @Override
    public void restore(Player subject) {
        if (!subject.isOnline()) return;

        EntityEquipment equipment = subject.getEquipment();
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            viewer.sendEquipmentChange(subject, EquipmentSlot.HEAD, equipment.getHelmet());
            viewer.sendEquipmentChange(subject, EquipmentSlot.CHEST, equipment.getChestplate());
            viewer.sendEquipmentChange(subject, EquipmentSlot.LEGS, equipment.getLeggings());
            viewer.sendEquipmentChange(subject, EquipmentSlot.FEET, equipment.getBoots());
            viewer.sendEquipmentChange(subject, EquipmentSlot.HAND, equipment.getItemInMainHand());
            viewer.sendEquipmentChange(subject, EquipmentSlot.OFF_HAND, equipment.getItemInOffHand());
        }
    }
}
