package com.kirazium.emotes.config;

import com.kirazium.emotes.core.EmoteDefinition;
import com.kirazium.emotes.core.EmoteRegistry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.logging.Level;

public final class EmoteConfigLoader {
    private final Plugin plugin;

    public EmoteConfigLoader(Plugin plugin) {
        this.plugin = plugin;
    }

    public int loadInto(EmoteRegistry registry) {
        registry.clear();
        File file = new File(plugin.getDataFolder(), "emotes.yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("emotes");
        if (root == null) return 0;

        int loaded = 0;
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) continue;

            try {
                EmoteDefinition definition = new EmoteDefinition(
                        id,
                        section.getString("display-name", id),
                        section.getString("renderer", "modelengine"),
                        section.getString("model", ""),
                        section.getString("animation", ""),
                        section.getLong("duration-ticks", 0L),
                        section.getDouble("lerp-in", 0.10D),
                        section.getDouble("lerp-out", 0.10D),
                        section.getDouble("speed", 1.0D),
                        section.getBoolean("cancel-on-move", true),
                        section.getBoolean("cancel-on-damage", true),
                        section.getBoolean("cancel-on-teleport", true)
                );

                if (definition.renderer().equals("modelengine")
                        && (definition.modelId().isBlank() || definition.animation().isBlank())) {
                    plugin.getLogger().warning("Skipping emote '" + id + "': ModelEngine renderer requires model and animation.");
                    continue;
                }

                registry.register(definition);
                loaded++;
            } catch (RuntimeException exception) {
                plugin.getLogger().log(Level.WARNING, "Skipping invalid emote definition '" + id + "'", exception);
            }
        }
        return loaded;
    }
}
