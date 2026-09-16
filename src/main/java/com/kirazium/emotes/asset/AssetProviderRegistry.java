package com.kirazium.emotes.asset;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class AssetProviderRegistry {
    private final Map<String, AssetProvider> providers = new LinkedHashMap<>();

    public void register(AssetProvider provider) {
        providers.put(provider.id().toLowerCase(Locale.ROOT), provider);
    }

    public Optional<AssetProvider> find(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(providers.get(id.toLowerCase(Locale.ROOT)));
    }
}
