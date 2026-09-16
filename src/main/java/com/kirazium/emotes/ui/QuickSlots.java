package com.kirazium.emotes.ui;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Per-player assignments, independent of menu implementation. */
public final class QuickSlots {
    public static final int COUNT = 8;
    private final Path directory;

    public QuickSlots(Path directory) { this.directory = directory; }

    public List<String> load(UUID player, List<String> available) throws IOException {
        Properties saved = new Properties();
        Path file = directory.resolve(player + ".properties");
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file)) { saved.load(reader); }
        }
        List<String> result = new ArrayList<>();
        for (int i = 0; i < COUNT; i++) {
            String fallback = i < available.size() ? available.get(i) : "";
            String value = saved.getProperty(Integer.toString(i), fallback);
            result.add(available.contains(value) ? value : fallback);
        }
        return result;
    }

    public void assign(UUID player, int slot, String id, List<String> available) throws IOException {
        if (slot < 0 || slot >= COUNT || !available.contains(id)) throw new IllegalArgumentException("Invalid assignment");
        List<String> values = load(player, available);
        // Swapping keeps the other shortcut usable when selecting an already assigned emote.
        int previous = values.indexOf(id);
        if (previous >= 0 && previous != slot) values.set(previous, values.get(slot));
        values.set(slot, id);
        Properties saved = new Properties();
        for (int i = 0; i < COUNT; i++) saved.setProperty(Integer.toString(i), values.get(i));
        Files.createDirectories(directory);
        Path file = directory.resolve(player + ".properties");
        Path temp = Files.createTempFile(directory, player + "-", ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(temp)) { saved.store(writer, "Kirazium emote shortcuts"); }
            try { Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException ignored) { Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
    }
}
