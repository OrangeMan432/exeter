package me.friendly.exeter.util;

import me.friendly.exeter.core.Exeter;
import net.minecraft.client.player.LocalPlayer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * PlaceholderAPI. Resolves {@code %placeholder%} tokens in any string — usable from the Hud,
 * ChatSpam messages, NameTags custom text, anywhere user-facing text is built.
 *
 * Built-in placeholders (all live values, no caching):
 *  - %player%        : your username
 *  - %health%        : health (1 decimal)
 *  - %maxhealth%     : max health
 *  - %hunger%        : food level
 *  - %fps%           : current FPS
 *  - %ping%          : latency in ms
 *  - %coords%        : "x, y, z" integers
 *  - %x% %y% %z%     : individual coordinates
 *  - %facing%        : cardinal direction (N/S/E/W…)
 *  - %server%        : server address or "Singleplayer"
 *  - %ping players%  : (use %players%) online players
 *  - %players%       : online player count
 *  - %time%          : client local time (HH:mm:ss)
 *  - %date%          : client local date (yyyy-MM-dd)
 *  - %totem%         : totems left in inventory
 *  - %crystals%      : end crystals in inventory
 *  - %pearls%        : ender pearls in inventory
 *  - %xpbottles%     : XP bottles in inventory
 *  - %dimension%     : current dimension id (overworld/the_nether/the_end)
 *  - %biome%         : biome at player position
 *  - %helditem%      : display name of held item
 *  - %durability%    : durability left on held item (or "-" if unbreakable)
 *
 * Register custom placeholders with {@link #register(String, Supplier)} — e.g. other modules
 * can expose their own state ({@code %autocrystal_target%} etc.).
 */
public final class PlaceholderAPI {
    private static final Map<String, Supplier<String>> PLACEHOLDERS = new LinkedHashMap<>();
    private static final java.time.format.DateTimeFormatter TIME_FMT =
            java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final java.time.format.DateTimeFormatter DATE_FMT =
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");

    static {
        register("player", () -> {
            LocalPlayer p = MinecraftHolder.mc().player;
            return p != null ? p.getName().getString() : "unknown";
        });
        register("health", () -> {
            LocalPlayer p = MinecraftHolder.mc().player;
            return p != null ? String.format("%.1f", p.getHealth()) : "0";
        });
        register("maxhealth", () -> {
            LocalPlayer p = MinecraftHolder.mc().player;
            return p != null ? String.format("%.1f", p.getMaxHealth()) : "0";
        });
        register("hunger", () -> {
            LocalPlayer p = MinecraftHolder.mc().player;
            return p != null ? String.valueOf(p.getFoodData().getFoodLevel()) : "0";
        });
        register("fps", () -> String.valueOf(MinecraftHolder.mc().getFps()));
        register("ping", () -> {
            LocalPlayer p = MinecraftHolder.mc().player;
            if (p == null || p.connection == null || p.connection.getPlayerInfo(p.getUUID()) == null) return "0";
            var info = p.connection.getPlayerInfo(p.getUUID());
            return String.valueOf(info.getLatency());
        });
        register("coords", () -> {
            LocalPlayer p = MinecraftHolder.mc().player;
            return p != null ? String.format("%d, %d, %d",
                    (int) p.getX(), (int) p.getY(), (int) p.getZ()) : "0, 0, 0";
        });
        register("x", () -> coord(0));
        register("y", () -> coord(1));
        register("z", () -> coord(2));
        register("facing", () -> {
            LocalPlayer p = MinecraftHolder.mc().player;
            if (p == null) return "?";
            return p.getDirection().getName().toUpperCase();
        });
        register("server", () -> {
            var conn = MinecraftHolder.mc().getConnection();
            var serverData = MinecraftHolder.mc().getCurrentServer();
            return serverData != null ? serverData.ip : "Singleplayer";
        });
        register("players", () -> {
            var conn = MinecraftHolder.mc().getConnection();
            if (conn == null || conn.getOnlinePlayers() == null) return "0";
            return String.valueOf(conn.getOnlinePlayers().size());
        });
        register("time", () -> java.time.LocalTime.now().format(TIME_FMT));
        register("date", () -> java.time.LocalDate.now().format(DATE_FMT));
        register("totem", () -> String.valueOf(countItem(net.minecraft.world.item.Items.TOTEM_OF_UNDYING)));
        register("crystals", () -> String.valueOf(countItem(net.minecraft.world.item.Items.END_CRYSTAL)));
        register("pearls", () -> String.valueOf(countItem(net.minecraft.world.item.Items.ENDER_PEARL)));
        register("xpbottles", () -> String.valueOf(countItem(net.minecraft.world.item.Items.EXPERIENCE_BOTTLE)));
        register("dimension", () -> {
            LocalPlayer p = MinecraftHolder.mc().player;
            if (p == null) return "?";
            String id = p.level().dimension().identifier().getPath();
            return id;
        });
        register("biome", () -> {
            LocalPlayer p = MinecraftHolder.mc().player;
            if (p == null || MinecraftHolder.mc().level == null) return "?";
            var holder = MinecraftHolder.mc().level.getBiome(p.blockPosition());
            return holder.unwrapKey().map(k -> k.identifier().getPath()).orElse("?");
        });
        register("helditem", () -> {
            LocalPlayer p = MinecraftHolder.mc().player;
            if (p == null || p.getMainHandItem().isEmpty()) return "-";
            var name = p.getMainHandItem().getHoverName();
            return name != null ? name.getString() : "-";
        });
        register("durability", () -> {
            LocalPlayer p = MinecraftHolder.mc().player;
            if (p == null || p.getMainHandItem().isEmpty()) return "-";
            var stack = p.getMainHandItem();
            if (stack.getMaxDamage() <= 0) return "-";
            return String.valueOf(stack.getMaxDamage() - stack.getDamageValue());
        });
    }

    private static String coord(int axis) {
        LocalPlayer p = MinecraftHolder.mc().player;
        if (p == null) return "0";
        double v = axis == 0 ? p.getX() : axis == 1 ? p.getY() : p.getZ();
        return String.valueOf((int) v);
    }

    private static int countItem(net.minecraft.world.item.Item item) {
        LocalPlayer p = MinecraftHolder.mc().player;
        if (p == null) return 0;
        int count = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            var stack = p.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() == item) count += stack.getCount();
        }
        // Include offhand.
        var off = p.getOffhandItem();
        if (!off.isEmpty() && off.getItem() == item) count += off.getCount();
        return count;
    }

    /** Register a custom placeholder (name without % signs). Overwrites existing. */
    public static void register(String name, Supplier<String> resolver) {
        PLACEHOLDERS.put(name.toLowerCase(), resolver);
    }

    /** Resolve all {@code %placeholder%} tokens in {@code text}. Unknown tokens are left as-is. */
    public static String apply(String text) {
        if (text == null || text.isEmpty() || !text.contains("%")) return text;
        StringBuilder out = new StringBuilder(text.length());
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '%') {
                int close = text.indexOf('%', i + 1);
                if (close > i + 1) {
                    String name = text.substring(i + 1, close).trim().toLowerCase();
                    Supplier<String> resolver = PLACEHOLDERS.get(name);
                    if (resolver != null) {
                        try {
                            out.append(resolver.get());
                            i = close + 1;
                            continue;
                        } catch (Exception ignored) {
                            // resolver failed — fall through and keep the raw token
                        }
                    }
                }
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }

    /** All registered placeholder names (for GUI pickers / help text). */
    public static java.util.Set<String> getRegisteredNames() {
        return java.util.Collections.unmodifiableSet(PLACEHOLDERS.keySet());
    }

    /** Lazy Minecraft handle — avoids classloading issues during static init. */
    private static final class MinecraftHolder {
        static net.minecraft.client.Minecraft mc() {
            return net.minecraft.client.Minecraft.getInstance();
        }
    }

    private PlaceholderAPI() {}
}
