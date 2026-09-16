package com.kirazium.emotes.asset.nexo;

import com.kirazium.emotes.asset.AssetProvider;
import com.kirazium.emotes.integration.IntegrationRegistry;
import com.kirazium.emotes.integration.IntegrationType;
import com.nexomc.nexo.api.NexoItems;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public final class NexoAssetProvider implements AssetProvider {
    private final IntegrationRegistry integrations;

    public NexoAssetProvider(IntegrationRegistry integrations) {
        this.integrations = integrations;
    }

    @Override
    public String id() {
        return "nexo";
    }

    @Override
    public boolean isAvailable() {
        return integrations.available(IntegrationType.NEXO);
    }

    @Override
    public Optional<ItemStack> item(String itemId) {
        if (!isAvailable() || itemId == null || itemId.isBlank()) return Optional.empty();
        var builder = NexoItems.itemFromId(itemId);
        if (builder == null) return Optional.empty();
        return Optional.ofNullable(builder.build());
    }
}
