package com.kirazium.emotes.render.modelengine;

import com.kirazium.emotes.bootstrap.BundledModelInstaller;
import com.kirazium.emotes.core.EmoteDefinition;
import com.kirazium.emotes.integration.IntegrationRegistry;
import com.kirazium.emotes.integration.IntegrationType;
import com.kirazium.emotes.render.EmoteRenderer;
import com.kirazium.emotes.render.RenderHandle;
import com.kirazium.emotes.render.equipment.EquipmentVisibilityController;
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.entity.data.BukkitEntityData;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import com.ticxo.modelengine.api.model.bone.BoneBehaviorTypes;
import com.ticxo.modelengine.api.model.bone.ModelBone;
import com.ticxo.modelengine.api.model.bone.type.PlayerLimb;
import com.ticxo.modelengine.api.model.bone.type.UserLimb;
import com.ticxo.modelengine.api.nms.entity.EntityHandler;
import com.ticxo.modelengine.api.nms.entity.wrapper.BodyRotationController;
import com.ticxo.modelengine.api.nms.entity.wrapper.TrackedEntity;
import org.bukkit.entity.Player;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ModelEngineRenderer implements EmoteRenderer {
    private final IntegrationRegistry integrations;
    private final EquipmentVisibilityController equipmentVisibility;

    public ModelEngineRenderer(
            IntegrationRegistry integrations,
            EquipmentVisibilityController equipmentVisibility
    ) {
        this.integrations = integrations;
        this.equipmentVisibility = equipmentVisibility;
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

        if (ModelEngineAPI.getBlueprint(definition.modelId()) == null) {
            throw new IllegalStateException("ModelEngine blueprint '" + definition.modelId() + "' is not registered");
        }
        if (ModelEngineAPI.getBlueprint(definition.modelId()).getAnimationOrRef(definition.animation()) == null) {
            throw new IllegalArgumentException("Animation is missing: " + definition.animation());
        }

        ModeledEntity modeledEntity = ModelEngineAPI.getOrCreateModeledEntity(player);
        if (modeledEntity.getModel(definition.modelId()).isPresent()) {
            throw new IllegalStateException("Model '" + definition.modelId() + "' is already attached to player " + player.getName());
        }

        boolean previousBaseVisibility = modeledEntity.isBaseEntityVisible();
        BodyRotationController rotationController = modeledEntity.getBase().getBodyRotationController();
        boolean previousPlayerMode = rotationController.isPlayerMode();
        EntityHandler entityHandler = ModelEngineAPI.getEntityHandler();
        boolean previousForcedInvisible = entityHandler.isForcedInvisible(player);

        if (!(modeledEntity.getBase().getData() instanceof BukkitEntityData entityData)) {
            throw new IllegalStateException("ModelEngine did not create Bukkit entity data for player " + player.getName());
        }

        UUID playerId = player.getUniqueId();
        TrackedEntity trackedEntity = entityData.getTracked();
        boolean playerWasTracked = trackedEntity.getTrackedPlayer().contains(playerId);
        boolean forcedSelfPairing = false;
        ActiveModel activeModel = ModelEngineAPI.createActiveModel(definition.modelId());
        activeModel.setAutoRendererInitialization(false);

        try {
            // ModelEngine's own disguise command performs these player-specific steps.
            // addModel() alone does not send the display model to the owning player's
            // client, so F5 self-view requires a forced tracking pair.
            rotationController.setPlayerMode(true);
            if (!playerWasTracked) {
                trackedEntity.addForcedPairing(playerId);
                forcedSelfPairing = true;
            }
            entityHandler.setForcedInvisible(player, true);
            modeledEntity.setBaseEntityVisible(false);

            // ModelEngine cannot despawn a player from their own client because that
            // entity owns the camera. Forced invisibility hides the skin, but vanilla
            // equipment is rendered separately and keeps the unanimated player pose.
            // Suppress that equipment for every viewer without changing the inventory.
            equipmentVisibility.hide(player);

            // Emotes must not replace the player's vanilla hitbox. The boolean is
            // overrideHitbox in ModelEngine R4.1.0/R4.1.1, not a render toggle.
            modeledEntity.addModel(activeModel, false);
            if (modeledEntity.getModel(definition.modelId()).orElse(null) != activeModel) {
                throw new IllegalStateException("ModelEngine cancelled or failed to attach model '"
                        + definition.modelId() + "'");
            }

            SkinBinding binding = bindPlayerSkin(activeModel, player);
            validateBundledPlayerModel(definition, binding);

            // Textures are bound before initialization so the first spawn packet sent
            // to the forced self-viewer already contains the correct player-head item.
            activeModel.initializeRenderer();
            activeModel.getAnimationHandler().playAnimation(
                    definition.animation(),
                    definition.lerpIn(),
                    definition.lerpOut(),
                    definition.speed(),
                    true
            );
            if (!activeModel.getAnimationHandler().isPlayingAnimation(definition.animation())) {
                throw new IllegalStateException("ModelEngine did not start animation '" + definition.animation() + "'");
            }
        } catch (RuntimeException exception) {
            cleanup(
                    player,
                    modeledEntity,
                    activeModel,
                    definition.animation(),
                    previousBaseVisibility,
                    rotationController,
                    previousPlayerMode,
                    entityHandler,
                    previousForcedInvisible,
                    trackedEntity,
                    forcedSelfPairing
            );
            throw exception;
        }

        boolean removeForcedSelfPairing = forcedSelfPairing;
        AtomicBoolean stopped = new AtomicBoolean(false);
        return new RenderHandle() {
            private EmoteDefinition current = definition;

            @Override
            public boolean switchTo(EmoteDefinition next) {
                if (stopped.get() || !id().equals(next.renderer()) || !current.modelId().equals(next.modelId())) {
                    return false;
                }
                if (activeModel.getBlueprint().getAnimationOrRef(next.animation()) == null) {
                    throw new IllegalArgumentException("Animation is missing: " + next.animation());
                }
                var handler = activeModel.getAnimationHandler();
                handler.forceStopAnimation(current.animation());
                try {
                    handler.playAnimation(next.animation(), next.lerpIn(), next.lerpOut(), next.speed(), true);
                    if (!handler.isPlayingAnimation(next.animation())) {
                        throw new IllegalStateException("ModelEngine did not start " + next.animation());
                    }
                    current = next;
                    return true;
                } catch (RuntimeException failure) {
                    handler.forceStopAnimation(next.animation());
                    handler.playAnimation(current.animation(), current.lerpIn(), current.lerpOut(), current.speed(), true);
                    throw failure;
                }
            }

            @Override
            public void stop() {
                if (!stopped.compareAndSet(false, true)) return;
                cleanup(
                    player,
                    modeledEntity,
                    activeModel,
                    current.animation(),
                    previousBaseVisibility,
                    rotationController,
                    previousPlayerMode,
                    entityHandler,
                    previousForcedInvisible,
                    trackedEntity,
                    removeForcedSelfPairing
                );
            }
        };
    }

    private static SkinBinding bindPlayerSkin(ActiveModel activeModel, Player player) {
        Set<PlayerLimb.Limb> playerLimbs = EnumSet.noneOf(PlayerLimb.Limb.class);
        Set<String> nestedPlayerLimbs = new LinkedHashSet<>();
        int userLimbs = 0;

        for (ModelBone bone : activeModel.getBones().values()) {
            var playerLimb = bone.getBoneBehavior(BoneBehaviorTypes.PLAYER_LIMB);
            if (playerLimb.isPresent()) {
                PlayerLimb limb = playerLimb.get();
                limb.setTexture(player);
                playerLimbs.add(limb.getLimbType());

                ModelBone parent = bone.getParent();
                while (parent != null) {
                    if (parent.hasBoneBehavior(BoneBehaviorTypes.PLAYER_LIMB)) {
                        nestedPlayerLimbs.add(bone.getBoneId());
                        break;
                    }
                    parent = parent.getParent();
                }
            }

            var userLimb = bone.getBoneBehavior(BoneBehaviorTypes.USER_LIMB);
            if (userLimb.isPresent()) {
                UserLimb limb = userLimb.get();
                limb.setTexture(player);
                userLimbs++;
            }
        }

        return new SkinBinding(playerLimbs, userLimbs, nestedPlayerLimbs);
    }

    private static void validateBundledPlayerModel(EmoteDefinition definition, SkinBinding binding) {
        if (!BundledModelInstaller.MODEL_ID.equals(definition.modelId())) return;

        Set<PlayerLimb.Limb> required = EnumSet.allOf(PlayerLimb.Limb.class);
        if (!binding.playerLimbs().equals(required)) {
            Set<PlayerLimb.Limb> missing = EnumSet.copyOf(required);
            missing.removeAll(binding.playerLimbs());
            throw new IllegalStateException("Bundled ModelEngine model is missing PLAYER_LIMB behaviors: " + missing);
        }
        if (!binding.nestedPlayerLimbs().isEmpty()) {
            throw new IllegalStateException("Bundled ModelEngine model nests PLAYER_LIMB bones under another "
                    + "PLAYER_LIMB, which corrupts shader part ids: " + binding.nestedPlayerLimbs());
        }
    }

    private void cleanup(
            Player player,
            ModeledEntity modeledEntity,
            ActiveModel activeModel,
            String animation,
            boolean previousBaseVisibility,
            BodyRotationController rotationController,
            boolean previousPlayerMode,
            EntityHandler entityHandler,
            boolean previousForcedInvisible,
            TrackedEntity trackedEntity,
            boolean removeForcedSelfPairing
    ) {
        try {
            if (!activeModel.isDestroyed()) {
                activeModel.getAnimationHandler().stopAnimation(animation);
            }
        } finally {
            try {
                if (modeledEntity.getModel(activeModel.getBlueprint().getName()).orElse(null) == activeModel) {
                    modeledEntity.removeModel(activeModel.getBlueprint().getName());
                }
                if (!activeModel.isDestroyed()) {
                    activeModel.destroy();
                }
                if (removeForcedSelfPairing) {
                    trackedEntity.removeForcedPairing(player.getUniqueId());
                }
                entityHandler.setForcedInvisible(player, previousForcedInvisible);
                modeledEntity.setBaseEntityVisible(previousBaseVisibility);
                rotationController.setPlayerMode(previousPlayerMode);
            } finally {
                // Always re-send the player's current equipment, including items that
                // changed while the emote was playing, even if model cleanup fails.
                equipmentVisibility.restore(player);
            }
        }
    }

    private record SkinBinding(
            Set<PlayerLimb.Limb> playerLimbs,
            int userLimbs,
            Set<String> nestedPlayerLimbs
    ) {
    }
}

