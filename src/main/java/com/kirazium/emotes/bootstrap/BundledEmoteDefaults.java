package com.kirazium.emotes.bootstrap;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

/**
 * Merges private-build emote definitions into plugins/KiraziumEmotes/emotes.yml
 * without overwriting administrator changes. Public builds may omit the bundled
 * resource entirely.
 */
public final class BundledEmoteDefaults {
    public static final String RESOURCE_PATH = "bundled/emotes.yml";

    private final JavaPlugin plugin;

    public BundledEmoteDefaults(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public int mergeInto(File targetFile) {
        try (InputStream input = plugin.getResource(RESOURCE_PATH)) {
            if (input == null) return 0;

            YamlConfiguration bundled = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(input, StandardCharsets.UTF_8)
            );
            YamlConfiguration current = YamlConfiguration.loadConfiguration(targetFile);
            ConfigurationSection bundledRoot = bundled.getConfigurationSection("emotes");
            if (bundledRoot == null) return 0;

            int added = 0;
            for (String emoteId : bundledRoot.getKeys(false)) {
                String base = "emotes." + emoteId;
                if (current.isConfigurationSection(base)) {
                    continue;
                }

                ConfigurationSection section = bundledRoot.getConfigurationSection(emoteId);
                if (section == null) continue;
                for (String key : section.getKeys(false)) {
                    current.set(base + "." + key, section.get(key));
                }
                added++;
            }

            if (added > 0) {
                current.save(targetFile);
            }
            return added;
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Could not merge bundled KiraziumEmotes definitions.", exception);
            return 0;
        }
    }
}
