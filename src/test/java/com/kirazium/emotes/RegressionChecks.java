package com.kirazium.emotes;

import com.kirazium.emotes.api.PlayResult;
import com.kirazium.emotes.core.*;
import com.kirazium.emotes.render.*;
import com.kirazium.emotes.ui.QuickSlots;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Logger;

/** Deterministic server-independent regression harness; not a server/client visual test. */
public final class RegressionChecks {
    private static int checks;
    public static void main(String[] args) throws Exception {
        List<Runnable> timers = new ArrayList<>();
        List<Boolean> cancelled = new ArrayList<>();
        BukkitScheduler scheduler = proxy(BukkitScheduler.class, (o, m, a) -> {
            if (!m.getName().equals("runTaskLater")) return defaultValue(m);
            int index = timers.size(); timers.add((Runnable)a[1]); cancelled.add(false);
            return proxy(BukkitTask.class, (task, method, values) -> {
                if (method.getName().equals("cancel")) cancelled.set(index, true);
                return defaultValue(method);
            });
        });
        Logger logger = Logger.getLogger("RegressionChecks");
        // Paper's setServer banner requires a real ServerBuildInfo service. This isolated
        // process uses a stub only; do not invoke production bootstrap just to print a banner.
        Field serverField = Bukkit.class.getDeclaredField("server");
        serverField.setAccessible(true);
        serverField.set(null, proxy(Server.class, (o, m, a) -> switch(m.getName()) {
            case "getScheduler" -> scheduler;
            case "getLogger" -> logger;
            case "getName", "getVersion", "getBukkitVersion" -> "regression-stub";
            default -> defaultValue(m);
        }));
        Plugin plugin = proxy(Plugin.class, (o, m, a) -> m.getName().equals("getLogger") ? logger : defaultValue(m));
        UUID uuid = UUID.randomUUID();
        Player player = proxy(Player.class, (o, m, a) -> switch(m.getName()) {
            case "getUniqueId" -> uuid;
            case "getName" -> "test-player";
            default -> defaultValue(m);
        });
        EmoteRegistry registry = new EmoteRegistry();
        registry.register(emote("one", "fake", 10)); registry.register(emote("two", "fake", 20));
        registry.register(emote("unavailable", "missing", 0)); registry.register(emote("reject", "fake", 0));
        RendererRegistry renderers = new RendererRegistry();
        FakeRenderer renderer = new FakeRenderer(); renderers.register(renderer);
        EmoteManager manager = new EmoteManager(plugin, registry, renderers);
        require(manager.play(player, "one") == PlayResult.SUCCESS, "initial play");
        require(manager.play(player, "unknown") == PlayResult.NOT_FOUND && renderer.stops == 0, "unknown preserves current");
        require(manager.play(player, "unavailable") == PlayResult.RENDERER_UNAVAILABLE && renderer.stops == 0, "unavailable preserves current");
        require(manager.play(player, "two") == PlayResult.SUCCESS && renderer.plays == 1 && renderer.switches == 1, "reuse renderer on switch");
        require(cancelled.get(0), "cancel old timeout");
        timers.get(0).run();
        require(manager.active(uuid).orElseThrow().id().equals("two"), "stale timeout cannot stop replacement");
        require(manager.play(player, "reject") == PlayResult.FAILED && manager.active(uuid).orElseThrow().id().equals("two"), "failed switch preserves current session");
        timers.get(1).run();
        require(!manager.isPlaying(uuid) && renderer.stops == 1, "new timeout stops replacement once");
        manager.play(player, "one"); renderer.reuse = false;
        require(manager.play(player, "two") == PlayResult.SUCCESS && renderer.plays == 3 && renderer.stops == 2, "fallback stops before starting another renderer");
        manager.stopAll(); require(renderer.stops == 3, "disable cleanup");
        Path dir = Files.createTempDirectory("kirazium-quickslots-test-");
        try {
            QuickSlots slots = new QuickSlots(dir);
            List<String> ids = java.util.stream.IntStream.range(0, 17).mapToObj(i -> "emote" + i).toList();
            require(slots.load(uuid, ids).equals(ids.subList(0,8)), "eight defaults");
            slots.assign(uuid, 0, "emote16", ids);
            require(new QuickSlots(dir).load(uuid, ids).get(0).equals("emote16"), "17th choice persists");
            slots.assign(uuid, 1, "emote16", ids);
            var assigned = slots.load(uuid, ids);
            require(assigned.get(1).equals("emote16") && assigned.get(0).equals("emote1"), "swap existing assignment");
            require(slots.load(UUID.randomUUID(), ids).get(0).equals("emote0"), "player isolation");
            boolean rejected = false;
            try { slots.assign(uuid, 8, "emote1", ids); } catch (IllegalArgumentException e) { rejected = true; }
            require(rejected, "invalid slot rejected");
        } finally {
            try(var files = Files.list(dir)) { for(Path file : files.toList()) Files.delete(file); }
            Files.delete(dir);
        }
        System.out.println("PASS: " + checks + " regression checks (no Minecraft client/server started)");
    }
    private static EmoteDefinition emote(String id, String renderer, long duration) {
        return new EmoteDefinition(id,id,renderer,"test","anim_"+id,duration,0,0,1,true,true,true);
    }
    private static void require(boolean ok, String label) { if(!ok) throw new AssertionError(label); checks++; }
    @SuppressWarnings("unchecked") private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T)Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }
    private static Object defaultValue(Method m) {
        Class<?> t = m.getReturnType();
        if(!t.isPrimitive() || t == void.class) return null;
        if(t == boolean.class) return false;
        if(t == long.class) return 0L;
        if(t == double.class) return 0d;
        if(t == float.class) return 0f;
        if(t == byte.class) return (byte)0;
        if(t == short.class) return (short)0;
        if(t == char.class) return (char)0;
        return 0;
    }
    private static final class FakeRenderer implements EmoteRenderer {
        int plays, switches, stops; boolean reuse = true;
        public String id() { return "fake"; }
        public boolean isAvailable() { return true; }
        public RenderHandle play(Player p, EmoteDefinition d) {
            plays++;
            return new RenderHandle() {
                public void stop() { stops++; }
                public boolean switchTo(EmoteDefinition next) {
                    if(next.id().equals("reject")) throw new IllegalArgumentException("expected regression test rejection");
                    if(!reuse) return false;
                    switches++; return true;
                }
            };
        }
    }
}
