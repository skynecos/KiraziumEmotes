package com.kirazium.emotes.core;

import java.util.Locale;
import java.util.Objects;

public record EmoteDefinition(
        String id,
        String displayName,
        String renderer,
        String modelId,
        String animation,
        long durationTicks,
        double lerpIn,
        double lerpOut,
        double speed,
        boolean cancelOnMove,
        boolean cancelOnDamage,
        boolean cancelOnTeleport
) {
    public EmoteDefinition {
        id = normalizeRequired(id, "id");
        displayName = Objects.requireNonNullElse(displayName, id);
        renderer = normalizeRequired(renderer, "renderer");
        modelId = Objects.requireNonNullElse(modelId, "").trim();
        animation = Objects.requireNonNullElse(animation, "").trim();
        if (durationTicks < 0) throw new IllegalArgumentException("durationTicks cannot be negative");
        if (lerpIn < 0 || lerpOut < 0) throw new IllegalArgumentException("lerp values cannot be negative");
        if (speed <= 0) throw new IllegalArgumentException("speed must be greater than zero");
    }

    private static String normalizeRequired(String value, String field) {
        Objects.requireNonNull(value, field);
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) throw new IllegalArgumentException(field + " cannot be empty");
        return normalized;
    }
}
