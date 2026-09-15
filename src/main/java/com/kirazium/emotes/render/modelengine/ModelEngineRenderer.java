package com.kirazium.emotes.render.modelengine;

import com.kirazium.emotes.core.EmoteDefinition;
import com.kirazium.emotes.integration.IntegrationRegistry;
import com.kirazium.emotes.integration.IntegrationType;
import com.kirazium.emotes.render.EmoteRenderer;
import com.kirazium.emotes.render.RenderHandle;
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import org.bukkit.entity.Player;

import java.util.concurrent.atomic.AtomicBoolean;

public final class ModelEngineRenderer implements EmoteRenderer {
    private final IntegrationRegistry integrations;

    public ModelEngineRenderer(IntegrationRegistry integrations) {
        this.integrations = integrations;
    }

    @Override
    public String id() {
        return "modelengine";
    }

    @Override
    public boolean isAvailable() {
        return integrations.available(IntegrationType.MODEL_ENGINE);
    }

    @Override
    public RenderHandle play(Player player, EmoteDefinition definition) {
        if (!isAvailable()) {
            throw new IllegalStateException("ModelEngine is not available");
        }
        if (definition.modelId().isBlank()) {
            throw new IllegalArgumentException("ModelEngine emote '" + definition.id() + "' has no model id");
        }
        if (definition.animation().isBlank()) {
            throw new IllegalArgumentException("ModelEngine emote '" + definition.id() + "' has no animation id");
        }

        ModeledEntity modeledEntity = ModelEngineAPI.getOrCreateModeledEntity(player);
        if (modeledEntity.getModel(definition.modelId()) != null) {
            throw new IllegalStateException("Model '" + definition.modelId() + "' is already attached to player " + player.getName());
        }

        boolean previousBaseVisibility = modeledEntity.isBaseEntityVisible();
        ActiveModel activeModel = ModelEngineAPI.createActiveModel(definition.modelId());

        try {
            modeledEntity.addModel(activeModel, true);
            modeledEntity.setBaseEntityVisible(false);
            activeModel.getAnimationHandler().playAnimation(
                    definition.animation(),
                    definition.lerpIn(),
                    definition.lerpOut(),
                    definition.speed(),
                    true
            );
        } catch (RuntimeException exception) {
            modeledEntity.removeModel(definition.modelId());
            modeledEntity.setBaseEntityVisible(previousBaseVisibility);
            throw exception;
        }

        AtomicBoolean stopped = new AtomicBoolean(false);
        return () -> {
            if (!stopped.compareAndSet(false, true)) return;
            try {
                activeModel.getAnimationHandler().stopAnimation(definition.animation());
            } finally {
                modeledEntity.removeModel(definition.modelId());
                modeledEntity.setBaseEntityVisible(previousBaseVisibility);
            }
        };
    }
}
