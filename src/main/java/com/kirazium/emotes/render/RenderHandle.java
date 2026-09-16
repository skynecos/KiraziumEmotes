package com.kirazium.emotes.render;

import com.kirazium.emotes.core.EmoteDefinition;

@FunctionalInterface
public interface RenderHandle {
    void stop();

    /** Return false when a different backend/model requires a fresh handle. */
    default boolean switchTo(EmoteDefinition definition) {
        return false;
    }
}
