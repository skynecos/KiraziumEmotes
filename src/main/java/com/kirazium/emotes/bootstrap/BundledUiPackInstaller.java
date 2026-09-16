package com.kirazium.emotes.bootstrap;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.InputStream;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.logging.Level;

/** Installs only our private menu icon pack, never UltimateUI's own pack or settings. */
public final class BundledUiPackInstaller {
    private BundledUiPackInstaller() {}

    public static void install(JavaPlugin plugin) {
        try (InputStream input = plugin.getResource("bundled/ui/kirazium_emotes_ui.zip")) {
            if (input == null) return;
            Plugin nexo = plugin.getServer().getPluginManager().getPlugin("Nexo");
            if (nexo == null) { plugin.getLogger().warning("Nexo missing: menu icon pack not installed."); return; }
            byte[] bytes = input.readAllBytes();
            Path target = nexo.getDataFolder().toPath().resolve("pack/external_packs/kirazium_emotes_ui.zip");
            Path marker = plugin.getDataFolder().toPath().resolve(".managed/ui-pack.sha256");
            String hash = hash(bytes);
            if (Files.exists(target)) {
                String current = hash(Files.readAllBytes(target));
                if (current.equals(hash)) {
                    Files.createDirectories(marker.getParent()); Files.writeString(marker, hash); return;
                }
                if (!Files.isRegularFile(marker) || !Files.readString(marker).trim().equals(current))
                    throw new IllegalStateException("Refusing to overwrite modified/unmanaged icon pack: " + target);
            }
            Files.createDirectories(target.getParent()); Files.createDirectories(marker.getParent());
            Path temp = Files.createTempFile(target.getParent(), "kirazium-ui-", ".tmp");
            try {
                Files.write(temp, bytes);
                try { Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
                catch (AtomicMoveNotSupportedException ignored) { Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING); }
            } finally { Files.deleteIfExists(temp); }
            Files.writeString(marker, hash);
            plugin.getLogger().info("Installed menu icons into Nexo external_packs. Regenerate and accept the server resource pack.");
        } catch (Exception e) { plugin.getLogger().log(Level.SEVERE, "Menu icon pack installation failed", e); }
    }

    private static String hash(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}
