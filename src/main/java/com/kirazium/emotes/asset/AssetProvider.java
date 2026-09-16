package com.kirazium.emotes.asset;

import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public interface AssetProvider {
    String id();

    boolean isAvailable();

    Optional<ItemStack> item(String itemId);
}
