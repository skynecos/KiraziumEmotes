package com.kirazium.emotes.render;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class RendererRegistry {
    private final Map<String, EmoteRenderer> renderers = new LinkedHashMap<>();

    public void register(EmoteRenderer renderer) {
        renderers.put(renderer.id().toLowerCase(Locale.ROOT), renderer);
    }

    public Optional<EmoteRenderer> find(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(renderers.get(id.toLowerCase(Locale.ROOT)));
    }
}
