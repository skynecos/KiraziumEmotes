package com.kirazium.emotes.core;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class EmoteRegistry {
    private final Map<String, EmoteDefinition> emotes = new LinkedHashMap<>();

    public void register(EmoteDefinition definition) {
        emotes.put(definition.id(), definition);
    }

    public Optional<EmoteDefinition> find(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(emotes.get(id.toLowerCase(Locale.ROOT)));
    }

    public Collection<EmoteDefinition> all() {
        return java.util.List.copyOf(emotes.values());
    }

    public void clear() {
        emotes.clear();
    }
}
