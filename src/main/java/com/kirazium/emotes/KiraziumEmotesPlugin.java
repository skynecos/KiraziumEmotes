package com.kirazium.emotes;

import com.kirazium.emotes.api.KiraziumEmotesApi;
import com.kirazium.emotes.api.PlayResult;
import com.kirazium.emotes.asset.AssetProviderRegistry;
import com.kirazium.emotes.asset.itemsadder.ItemsAdderAssetProvider;
import com.kirazium.emotes.asset.nexo.NexoAssetProvider;
import com.kirazium.emotes.asset.oraxen.OraxenAssetProvider;
import com.kirazium.emotes.command.EmoteCommand;
import com.kirazium.emotes.config.EmoteConfigLoader;
import com.kirazium.emotes.core.EmoteDefinition;
import com.kirazium.emotes.core.EmoteManager;
import com.kirazium.emotes.core.EmoteRegistry;
import com.kirazium.emotes.integration.IntegrationRegistry;
import com.kirazium.emotes.integration.IntegrationStatus;
import com.kirazium.emotes.integration.IntegrationType;
import com.kirazium.emotes.listener.EmoteLifecycleListener;
import com.kirazium.emotes.render.RendererRegistry;
import com.kirazium.emotes.render.modelengine.ModelEngineRenderer;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Collection;
import java.util.Optional;

public final class KiraziumEmotesPlugin extends JavaPlugin {
    private EmoteManager emoteManager;
    private EmoteRegistry emoteRegistry;
    private AssetProviderRegistry assetProviders;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        ensureEmotesFile();

        IntegrationRegistry integrations = new IntegrationRegistry(getServer().getPluginManager());
        integrations.scan();

        RendererRegistry renderers = new RendererRegistry();
        if (integrations.available(IntegrationType.MODEL_ENGINE)) {
            renderers.register(new ModelEngineRenderer(integrations));
        }

        assetProviders = new AssetProviderRegistry();
        registerAssetProviders(integrations);

        emoteRegistry = new EmoteRegistry();
        int loaded = new EmoteConfigLoader(this).loadInto(emoteRegistry);
        emoteManager = new EmoteManager(this, emoteRegistry, renderers);

        getServer().getPluginManager().registerEvents(new EmoteLifecycleListener(emoteManager), this);
        registerCommand();
        registerApi();
        logIntegrations(integrations);

        getLogger().info("KiraziumEmotes enabled. Loaded " + loaded + " emote(s).");
    }

    @Override
    public void onDisable() {
        if (emoteManager != null) {
            emoteManager.stopAll();
        }
        getServer().getServicesManager().unregisterAll(this);
    }

    private void registerAssetProviders(IntegrationRegistry integrations) {
        if (integrations.available(IntegrationType.NEXO)) {
            assetProviders.register(new NexoAssetProvider(integrations));
        }
        if (integrations.available(IntegrationType.ITEMS_ADDER)) {
            assetProviders.register(new ItemsAdderAssetProvider(integrations));
        }
        if (integrations.available(IntegrationType.ORAXEN)) {
            assetProviders.register(new OraxenAssetProvider(integrations));
        }
    }

    private void ensureEmotesFile() {
        File file = new File(getDataFolder(), "emotes.yml");
        if (!file.exists()) {
            saveResource("emotes.yml", false);
        }
    }

    private void registerCommand() {
        PluginCommand command = getCommand("emote");
        if (command == null) {
            throw new IllegalStateException("Command 'emote' is missing from plugin.yml");
        }
        EmoteCommand executor = new EmoteCommand(emoteManager, emoteRegistry);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    private void registerApi() {
        KiraziumEmotesApi api = new KiraziumEmotesApi() {
            @Override
            public PlayResult play(Player player, String emoteId) {
                return emoteManager.play(player, emoteId);
            }

            @Override
            public boolean stop(Player player) {
                return emoteManager.stop(player, EmoteManager.StopReason.USER);
            }

            @Override
            public boolean isPlaying(Player player) {
                return emoteManager.isPlaying(player.getUniqueId());
            }

            @Override
            public Optional<String> activeEmote(Player player) {
                return emoteManager.active(player.getUniqueId()).map(EmoteDefinition::id);
            }

            @Override
            public Collection<String> emoteIds() {
                return emoteRegistry.all().stream().map(EmoteDefinition::id).toList();
            }
        };

        getServer().getServicesManager().register(KiraziumEmotesApi.class, api, this, ServicePriority.Normal);
    }

    private void logIntegrations(IntegrationRegistry integrations) {
        for (IntegrationType type : IntegrationType.values()) {
            IntegrationStatus status = integrations.status(type);
            if (status.installed()) {
                getLogger().info(type.pluginName() + " detected (" + status.version() + ").");
            } else {
                getLogger().info(type.pluginName() + " not detected; integration disabled.");
            }
        }
    }
}
