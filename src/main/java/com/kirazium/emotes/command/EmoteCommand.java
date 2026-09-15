package com.kirazium.emotes.command;

import com.kirazium.emotes.api.PlayResult;
import com.kirazium.emotes.core.EmoteDefinition;
import com.kirazium.emotes.core.EmoteManager;
import com.kirazium.emotes.core.EmoteRegistry;
import com.kirazium.emotes.ui.EmoteMenu;
import com.kirazium.emotes.util.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class EmoteCommand implements CommandExecutor, TabCompleter {
    private final EmoteManager manager;
    private final EmoteRegistry registry;
    private final EmoteMenu menu;

    public EmoteCommand(EmoteManager manager, EmoteRegistry registry, EmoteMenu menu) {
        this.manager = manager;
        this.registry = registry;
        this.menu = menu;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            Messages.error(sender, "Bu komut yalnızca oyuncular tarafından kullanılabilir.");
            return true;
        }

        // /emote and /emotes are both server-side entry points to the menu.
        if (args.length == 0) {
            menu.open(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            List<String> ids = registry.all().stream().map(EmoteDefinition::id).toList();
            if (ids.isEmpty()) {
                Messages.info(player, "Henüz yüklü bir emote yok.");
            } else {
                Messages.info(player, "Emoteler: " + String.join(", ", ids));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("stop")) {
            if (manager.stop(player, EmoteManager.StopReason.USER)) {
                Messages.success(player, "Emote durduruldu.");
            } else {
                Messages.info(player, "Şu anda oynattığın bir emote yok.");
            }
            return true;
        }

        PlayResult result = manager.play(player, args[0]);
        switch (result) {
            case SUCCESS -> Messages.success(player, "Emote başlatıldı.");
            case NOT_FOUND -> Messages.error(player, "Böyle bir emote bulunamadı.");
            case ALREADY_PLAYING -> Messages.error(player, "Önce mevcut emoteyi durdurmalısın.");
            case RENDERER_UNAVAILABLE -> Messages.error(player, "Bu emote için gerekli animasyon sistemi aktif değil.");
            case FAILED -> Messages.error(player, "Emote başlatılamadı. Konsol logunu kontrol et.");
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        if (args.length != 1) return List.of();

        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> options = new ArrayList<>();
        options.add("list");
        options.add("stop");
        registry.all().stream().map(EmoteDefinition::id).forEach(options::add);
        return options.stream().filter(value -> value.startsWith(prefix)).toList();
    }
}
