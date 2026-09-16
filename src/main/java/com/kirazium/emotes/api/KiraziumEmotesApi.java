package com.kirazium.emotes.api;

import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Optional;

public interface KiraziumEmotesApi {
    PlayResult play(Player player, String emoteId);

    boolean stop(Player player);

    boolean isPlaying(Player player);

    Optional<String> activeEmote(Player player);

    Collection<String> emoteIds();
}
