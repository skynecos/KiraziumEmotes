package com.kirazium.emotes.bootstrap;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public final class BundledModelInstaller {
    private static final String RESOURCE_PATH = "bundled/modelengine/player_floss.bbmodel";
    private static final String FILE_NAME = "player_floss.bbmodel";

    private final JavaPlugin plugin;

    public BundledModelInstaller(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Installs the verified first-party test blueprint into ModelEngine's own data folder.
     * Existing administrator files are never overwritten.
     *
     * @return true when a new file was installed and ModelEngine should reload/restart.
     */
    public boolean installIfNeeded() {
        Plugin modelEngine = plugin.getServer().getPluginManager().getPlugin("ModelEngine");
        if (modelEngine == null) {
            plugin.getLogger().warning("ModelEngine is not installed; bundled emote blueprint was not installed.");
            return false;
        }

        File blueprints = new File(modelEngine.getDataFolder(), "blueprints");
        File target = new File(blueprints, FILE_NAME);
        if (target.isFile()) {
            return false;
        }

        if (!blueprints.exists() && !blueprints.mkdirs()) {
            plugin.getLogger().warning("Could not create ModelEngine blueprints directory: " + blueprints.getAbsolutePath());
            return false;
        }

        try (InputStream input = plugin.getResource(RESOURCE_PATH)) {
            if (input == null) {
                plugin.getLogger().severe("Bundled resource is missing from KiraziumEmotes JAR: " + RESOURCE_PATH);
                return false;
            }
            Files.copy(input, target.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
            plugin.getLogger().info("Installed verified ModelEngine blueprint: " + target.getAbsolutePath());
            return true;
        } catch (IOException exception) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE,
                    "Could not install bundled ModelEngine blueprint: " + target.getAbsolutePath(), exception);
            return false;
        }
    }
}
