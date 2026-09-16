package com.kirazium.emotes.integration;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.util.EnumMap;
import java.util.Map;

public final class IntegrationRegistry {
    private final PluginManager pluginManager;
    private final Map<IntegrationType, IntegrationStatus> statuses = new EnumMap<>(IntegrationType.class);

    public IntegrationRegistry(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    public void scan() {
        for (IntegrationType type : IntegrationType.values()) {
            Plugin plugin = pluginManager.getPlugin(type.pluginName());
            boolean installed = plugin != null && plugin.isEnabled();
            String version = plugin == null ? "-" : plugin.getPluginMeta().getVersion();
            statuses.put(type, new IntegrationStatus(type, installed, version));
        }
    }

    public IntegrationStatus status(IntegrationType type) {
        return statuses.getOrDefault(type, new IntegrationStatus(type, false, "-"));
    }

    public boolean available(IntegrationType type) {
        return status(type).installed();
    }
}
