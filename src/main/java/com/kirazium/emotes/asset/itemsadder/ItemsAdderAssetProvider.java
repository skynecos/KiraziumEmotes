package com.kirazium.emotes.asset.itemsadder;

import com.kirazium.emotes.asset.AssetProvider;
import com.kirazium.emotes.integration.IntegrationRegistry;
import com.kirazium.emotes.integration.IntegrationType;
import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public final class ItemsAdderAssetProvider implements AssetProvider {
    private final IntegrationRegistry integrations;

    public ItemsAdderAssetProvider(IntegrationRegistry integrations) {
        this.integrations = integrations;
    }

    @Override
    public String id() {
        return "itemsadder";
    }

    @Override
    public boolean isAvailable() {
        return integrations.available(IntegrationType.ITEMS_ADDER);
    }

    @Override
    public Optional<ItemStack> item(String itemId) {
        if (!isAvailable() || itemId == null || itemId.isBlank()) return Optional.empty();
        CustomStack customStack = CustomStack.getInstance(itemId);
        if (customStack == null) return Optional.empty();
        return Optional.of(customStack.getItemStack().clone());
    }
}
