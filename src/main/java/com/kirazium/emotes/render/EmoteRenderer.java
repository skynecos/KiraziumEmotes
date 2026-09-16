package com.kirazium.emotes.render;

import com.kirazium.emotes.core.EmoteDefinition;
import org.bukkit.entity.Player;

public interface EmoteRenderer {
    String id();

    boolean isAvailable();

    RenderHandle play(Player player, EmoteDefinition definition) throws Exception;
}
