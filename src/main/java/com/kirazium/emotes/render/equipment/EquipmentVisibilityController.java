package com.kirazium.emotes.render.equipment;

import org.bukkit.entity.Player;

public interface EquipmentVisibilityController extends AutoCloseable {
    void hide(Player subject);

    void restore(Player subject);

    @Override
    default void close() {
    }
}
