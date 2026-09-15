package com.kirazium.emotes.bootstrap;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.logging.Level;

/**
 * Installs a licensed/private bundled ModelEngine blueprint before plugins are enabled.
 * The actual asset is intentionally not stored in the public repository; private builds
 * can embed it at bundled/modelengine/kirazium_emotes.bbmodel.
 */
public final class BundledModelInstaller {
    public static final String RESOURCE_PATH = "bundled/modelengine/kirazium_emotes.bbmodel";
    public static final String MODEL_ID = "kirazium_emotes";

    private static final String TARGET_DIRECTORY = "kiraziumemotes";
    private static final String TARGET_FILE = MODEL_ID + ".bbmodel";

    // Never place installer metadata inside ModelEngine/blueprints: ModelEngine scans every
    // file there and reports unknown formats for non-blueprint files.
    private static final String MANAGED_DIRECTORY = ".managed";
    private static final String MARKER_FILE = "modelengine-" + MODEL_ID + ".sha256";
    private static final String LEGACY_MARKER_FILE = ".kirazium_emotes.sha256";

    private final JavaPlugin plugin;

    public BundledModelInstaller(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public Result installDuringLoad() {
        Plugin modelEngine = plugin.getServer().getPluginManager().getPlugin("ModelEngine");
        if (modelEngine == null) {
            return Result.MODEL_ENGINE_MISSING;
        }

        byte[] bundled;
        try (InputStream input = plugin.getResource(RESOURCE_PATH)) {
            if (input == null) {
                return Result.NO_BUNDLED_MODEL;
            }
            bundled = input.readAllBytes();
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Could not read bundled KiraziumEmotes model.", exception);
            return Result.FAILED;
        }

        Path blueprintDirectory = modelEngine.getDataFolder().toPath()
                .resolve("blueprints")
                .resolve(TARGET_DIRECTORY);
        Path target = blueprintDirectory.resolve(TARGET_FILE);

        Path managedDirectory = plugin.getDataFolder().toPath().resolve(MANAGED_DIRECTORY);
        Path marker = managedDirectory.resolve(MARKER_FILE);
        Path legacyMarker = blueprintDirectory.resolve(LEGACY_MARKER_FILE);

        try {
            Files.createDirectories(blueprintDirectory);
            Files.createDirectories(managedDirectory);

            String bundledHash = sha256(bundled);
            if (Files.isRegularFile(target)) {
                String currentHash = sha256(Files.readAllBytes(target));
                if (currentHash.equals(bundledHash)) {
                    writeMarker(marker, bundledHash);
                    Files.deleteIfExists(legacyMarker);
                    return Result.UNCHANGED;
                }

                Path ownershipMarker = Files.isRegularFile(marker)
                        ? marker
                        : (Files.isRegularFile(legacyMarker) ? legacyMarker : null);
                if (ownershipMarker == null) {
                    plugin.getLogger().severe("Refusing to overwrite unmanaged ModelEngine blueprint: " + target);
                    return Result.CONFLICT;
                }

                String managedHash = Files.readString(ownershipMarker, StandardCharsets.UTF_8).trim();
                if (!managedHash.equals(currentHash)) {
                    plugin.getLogger().severe("Refusing to overwrite modified KiraziumEmotes blueprint: " + target);
                    return Result.CONFLICT;
                }

                atomicWrite(target, bundled);
                writeMarker(marker, bundledHash);
                Files.deleteIfExists(legacyMarker);
                return Result.UPDATED;
            }

            atomicWrite(target, bundled);
            writeMarker(marker, bundledHash);
            Files.deleteIfExists(legacyMarker);
            return Result.INSTALLED;
        } catch (IOException | NoSuchAlgorithmException exception) {
            plugin.getLogger().log(Level.SEVERE, "Could not install KiraziumEmotes ModelEngine blueprint.", exception);
            return Result.FAILED;
        }
    }

    private static void atomicWrite(Path target, byte[] bytes) throws IOException {
        Path temp = target.resolveSibling(target.getFileName() + ".tmp");
        Files.deleteIfExists(temp);
        Files.write(temp, bytes);
        try {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void writeMarker(Path marker, String hash) throws IOException {
        Files.writeString(marker, hash + System.lineSeparator(), StandardCharsets.UTF_8);
    }

    private static String sha256(byte[] bytes) throws NoSuchAlgorithmException {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    public enum Result {
        INSTALLED,
        UPDATED,
        UNCHANGED,
        NO_BUNDLED_MODEL,
        MODEL_ENGINE_MISSING,
        CONFLICT,
        FAILED
    }
}
