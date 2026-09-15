package com.kirazium.emotes.asset.oraxen;

import com.kirazium.emotes.asset.AssetProvider;
import com.kirazium.emotes.integration.IntegrationRegistry;
import com.kirazium.emotes.integration.IntegrationType;
import io.th0rgal.oraxen.api.OraxenItems;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public final class OraxenAssetProvider implements AssetProvider {
    private final IntegrationRegistry integrations;

    public OraxenAssetProvider(IntegrationRegistry integrations) {
        this.integrations = integrations;
    }

    @Override
    public String id() {
        return "oraxen";
    }

    @Override
    public boolean isAvailable() {
        return integrations.available(IntegrationType.ORAXEN);
    }

    @Override
    public Optional<ItemStack> item(String itemId) {
        if (!isAvailable() || itemId == null || itemId.isBlank()) return Optional.empty();
        var builder = OraxenItems.getItemById(itemId);
        if (builder == null) return Optional.empty();
        return Optional.ofNullable(builder.build());
    }
}
