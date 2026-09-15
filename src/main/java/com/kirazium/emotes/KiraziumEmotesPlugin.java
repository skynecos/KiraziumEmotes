package com.kirazium.emotes;

import com.kirazium.emotes.api.KiraziumEmotesApi;
import com.kirazium.emotes.api.PlayResult;
import com.kirazium.emotes.asset.AssetProviderRegistry;
import com.kirazium.emotes.asset.itemsadder.ItemsAdderAssetProvider;
import com.kirazium.emotes.asset.nexo.NexoAssetProvider;
import com.kirazium.emotes.asset.oraxen.OraxenAssetProvider;
import com.kirazium.emotes.bootstrap.BundledEmoteDefaults;
import com.kirazium.emotes.bootstrap.BundledModelInstaller;
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
import com.kirazium.emotes.ui.EmoteMenu;
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
    private BundledModelInstaller.Result bundledModelResult = BundledModelInstaller.Result.NO_BUNDLED_MODEL;

    @Override
    public void onLoad() {
        // Paper guarantees every plugin's onLoad runs before any plugin's onEnable.
        // Installing here lets ModelEngine see a bundled blueprint during its normal startup import.
        bundledModelResult = new BundledModelInstaller(this).installDuringLoad();
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        File emotesFile = ensureEmotesFile();
        int bundledDefinitionsAdded = new BundledEmoteDefaults(this).mergeInto(emotesFile);

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

        EmoteMenu emoteMenu = new EmoteMenu(this, emoteManager, emoteRegistry);
        getServer().getPluginManager().registerEvents(new EmoteLifecycleListener(emoteManager), this);
        getServer().getPluginManager().registerEvents(emoteMenu, this);
        registerCommand(emoteMenu);
        registerApi();
        logIntegrations(integrations);
        logBundledModelResult();

        if (bundledDefinitionsAdded > 0) {
            getLogger().info("Added " + bundledDefinitionsAdded + " bundled emote definition(s) without overwriting existing entries.");
        }
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

    private File ensureEmotesFile() {
        File file = new File(getDataFolder(), "emotes.yml");
        if (!file.exists()) {
            saveResource("emotes.yml", false);
        }
        return file;
    }

    private void registerCommand(EmoteMenu emoteMenu) {
        PluginCommand command = getCommand("emote");
        if (command == null) {
            throw new IllegalStateException("Command 'emote' is missing from plugin.yml");
        }
        EmoteCommand executor = new EmoteCommand(emoteManager, emoteRegistry, emoteMenu);
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

    private void logBundledModelResult() {
        switch (bundledModelResult) {
            case INSTALLED -> getLogger().info("Installed bundled ModelEngine blueprint before plugin enable.");
            case UPDATED -> getLogger().info("Updated managed ModelEngine blueprint before plugin enable.");
            case UNCHANGED -> getLogger().info("Bundled ModelEngine blueprint is already current.");
            case MODEL_ENGINE_MISSING -> getLogger().warning("ModelEngine is not installed; bundled blueprint was not installed.");
            case CONFLICT -> getLogger().severe("Bundled ModelEngine blueprint conflicts with a modified/unmanaged file; it was not overwritten.");
            case FAILED -> getLogger().severe("Bundled ModelEngine blueprint installation failed. Check earlier log entries.");
            case NO_BUNDLED_MODEL -> getLogger().info("No private bundled ModelEngine blueprint is present in this build.");
        }
    }
}
