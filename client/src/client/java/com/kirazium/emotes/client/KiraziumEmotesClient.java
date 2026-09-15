package com.kirazium.emotes.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public final class KiraziumEmotesClient implements ClientModInitializer {
    public static final String MOD_ID = "kiraziumemotes_client";

    private final KeyMapping.Category category = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(MOD_ID, "emotes")
    );

    private final KeyMapping emoteWheelKey = KeyMappingHelper.registerKeyMapping(
            new KeyMapping(
                    "key.kiraziumemotes_client.open_wheel",
                    InputConstants.Type.KEYSYM,
                    InputConstants.KEY_G,
                    category
            )
    );

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (emoteWheelKey.consumeClick()) {
                if (client.player != null && client.screen == null) {
                    client.setScreen(new EmoteWheelScreen());
                }
            }
        });
    }
}
