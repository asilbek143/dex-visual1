package com.dexvisual.module;

import com.dexvisual.config.CrosshairSettings;
import com.dexvisual.config.DexConfig;
import com.dexvisual.config.ModuleSettings;
import com.dexvisual.config.Waypoint;
import com.dexvisual.hud.CrosshairRenderer;
import com.dexvisual.util.Gfx;
import com.dexvisual.util.Notifications;
import com.dexvisual.util.Tracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Registry of every DEX VISUAL module. */
public final class Modules {
    private Modules() {}

    public static final List<HudModule> ALL = new ArrayList<>();

    public static HudModule FPS, CPS, PING, COORDS, DIRECTION, CLOCK, MEMORY, SESSION, PERF;
    public static HudModule KEYSTROKES, TARGET, COMBO, KILLS, REACH, DAMAGE, HITCOLOR, CROSSHAIR;
    public static HudModule ARMOR, POTION, ITEMCOUNTER, DURABILITY;
    public static HudModule TOGGLESPRINT, TOGGLESNEAK, WAYPOINTS, NOTIFICATIONS;
    public static HudModule SCOREBOARD, TABLIST, CHAT;

    private static <T extends HudModule> T reg(T m) { ALL.add(m); return m; }

    public static HudModule byId(String id) {
        for (HudModule m : ALL) if (m.id.equals(id)) return m;
        return null;
    }

    public static void set(HudModule m, boolean on) {
        ModuleSettings s = m.cfg();
        if (s.enabled == on) return;
        s.enabled = on;
        DexConfig.markDirty();
        Notifications.push(m.name, on ? "Enabled" : "Disabled", on ? 0x46FFB4 : 0xFF4D6D);
    }

    public static void toggle(HudModule m) { set(m, !m.cfg().enabled); }

    static String f(String fmt, Object... a) { return String.format(Locale.ROOT, fmt, a); }

    static int ping(MinecraftClient mc) {
        if (mc.getNetworkHandler() == null || mc.player == null) return -1;
        PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        return e == null ? -1 : e.getLatency();
    }

    static String pretty(String s) {
        String[] p = s.split("_");
        StringBuilder b = new StringBuilder();
        for (String w : p) if (!w.isEmpty()) b.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(' ');
        return b.toString().trim();
    }

    public static String biome(MinecraftClient mc) {
        return mc.world.getBiome(mc.player.getBlockPos()).getKey().map(k -> pretty(k.getValue().getPath())).orElse("Unknown");
    }

    static final String[] DIRS = {"South", "South-West", "West", "North-West", "North", "North-East", "East", "South-East"};

    public static String facing(float yaw) {
        float y = MathHelper.wrapDegrees(yaw);
        return DIRS[Math.floorMod(Math.round(y / 45f), 8)];
    }

    static int ratioColor(float r, int alphaSource) {
        int rgb = r > 0.5f ? 0x55FF55 : r > 0.25f ? 0xFFFF55 : 0xFF5555;
        return (alphaSource & 0xFF000000) | rgb;
    }

    static String roman(int n) {
        String[] r = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return n >= 0 && n < r.length ? r[n] : String.valueOf(n);
    }

    static {
        // ------------------------------------------------------------ info
        FPS = reg(new TextModule("fps", "FPS Counter", "Info", "Frames per second", 0.01f, 0.02f, "FPS: 240",
                mc -> "FPS: " + mc.getCurrentFps()));
        CPS = reg(new TextModule("cps", "CPS Counter", "Info", "Clicks per second (left | right)", 0.01f, 0.09f, "CPS: 8 | 3",
                mc -> "CPS: " + Tracker.cpsLeft() + " | " + Tracker.cpsRight()));
        PING = reg(new TextModule("ping", "Ping", "Info", "Server latency", 0.01f, 0.16f, "Ping: 24 ms",
                mc -> { int p = ping(mc); return p < 0 ? "Ping: --" : "Ping: " + p + " ms"; }));
        COORDS = reg(new TextModule("coords", "Coordinates", "Info", "XYZ position and biome", 0.01f, 0.23f, "XYZ: 128.4 64.0 -312.7\nBiome: Dark Forest",
                mc -> f("XYZ: %.1f %.1f %.1f", mc.player.getX(), mc.player.getY(), mc.player.getZ()) + "\nBiome: " + biome(mc)));
        DIRECTION = reg(new TextModule("direction", "Direction", "Info", "Facing direction and yaw", 0.01f, 0.32f, "Facing: North (180)",
                mc -> "Facing: " + facing(mc.player.getYaw()) + " (" + Math.round(MathHelper.wrapDegrees(mc.player.getYaw())) + ")"));
        CLOCK = reg(new TextModule("clock", "Clock", "Info", "Real time clock", 0.01f, 0.39f, "12:00:00",
                mc -> LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))));
        MEMORY = reg(new TextModule("memory", "FPS / RAM Info", "Info", "FPS and memory usage", 0.01f, 0.46f, "FPS: 240 | RAM: 1.4/4.0 GB",
                mc -> "FPS: " + mc.getCurrentFps() + " | RAM: " + Tracker.usedRam()));
        SESSION = reg(new TextModule("session", "Session Statistics", "Info", "Playtime, kills, hits, distance", 0.01f, 0.53f,
                "Session: 12m 03s\nKills: 3\nHits: 27\nBest combo: 8\nDistance: 1.2 km",
                mc -> "Session: " + Tracker.sessionTime() + "\nKills: " + Tracker.kills + "\nHits: " + Tracker.hits
                        + "\nBest combo: " + Tracker.maxCombo + "\nDistance: " + f("%.2f km", Tracker.distance / 1000.0)));
        PERF = reg(new PerfModule());

        // ---------------------------------------------------------- combat
        KEYSTROKES = reg(new KeystrokesModule());
        TARGET = reg(new TargetModule());
        COMBO = reg(new TextModule("combo", "Combo Counter", "Combat", "Consecutive hits without being hit", 0.01f, 0.72f, "Combo: 7",
                mc -> "Combo: " + Tracker.combo));
        KILLS = reg(new TextModule("kills", "Kill Counter", "Combat", "Kills and current streak", 0.01f, 0.79f, "Kills: 3 | Streak: 2",
                mc -> "Kills: " + Tracker.kills + " | Streak: " + Tracker.streak));
        REACH = reg(new TextModule("reach", "Reach Display", "Combat", "Distance of your last attack", 0.01f, 0.86f, "Reach: 2.91",
                mc -> Tracker.lastReach <= 0 ? "Reach: --" : f("Reach: %.2f", Tracker.lastReach)));
        DAMAGE = reg(new OverlayModule("damage", "Damage Indicator", "Combat", "Floating damage numbers near the crosshair") {
            @Override public void renderOverlay(DrawContext ctx, MinecraftClient mc) {
                long now = System.currentTimeMillis();
                int cx = mc.getWindow().getScaledWidth() / 2, cy = mc.getWindow().getScaledHeight() / 2;
                int rgb = cfg().textColor == 0xFFFFFF ? 0xFF5577 : cfg().textColor;
                for (Tracker.Floating fl : Tracker.FLOATS) {
                    float age = (now - fl.start) / 1200f;
                    Gfx.text(ctx, fl.text, cx + 12, cy - 12 - (int) (age * 26), Gfx.argb(rgb, 1f - age), true);
                }
            }
        });
        HITCOLOR = reg(new OverlayModule("hitcolor", "Hit Color", "Combat", "Crosshair & target bar flash when you land a hit"));
        CROSSHAIR = reg(new OverlayModule("crosshair", "Crosshair Editor", "Combat", "Replace the vanilla crosshair with your custom one") {
            @Override public void renderOverlay(DrawContext ctx, MinecraftClient mc) { CrosshairRenderer.drawHud(ctx, mc); }
        });

        // ------------------------------------------------------------ gear
        ARMOR = reg(new ArmorModule());
        POTION = reg(new PotionModule());
        ITEMCOUNTER = reg(new ItemCounterModule());
        DURABILITY = reg(new DurabilityModule());

        // --------------------------------------------------------- utility
        TOGGLESPRINT = reg(new ToggleModule("togglesprint", "Toggle Sprint", "Press the sprint key once to keep sprinting", 0.13f, 0.02f, true));
        TOGGLESNEAK = reg(new ToggleModule("togglesneak", "Toggle Sneak", "Press the sneak key once to keep sneaking", 0.13f, 0.09f, false));
        WAYPOINTS = reg(new WaypointsModule());
        NOTIFICATIONS = reg(new NotificationsModule());

        // --------------------------------------------------------- overlays
        SCOREBOARD = reg(new OverlayModule("scoreboard", "Custom Scoreboard", "Overlay", "Scale, move or hide the sidebar scoreboard").option("Hide scoreboard"));
        TABLIST = reg(new OverlayModule("tablist", "Custom Tablist", "Overlay", "Scale and move the player list"));
        CHAT = reg(new OverlayModule("chat", "Custom Chat", "Overlay", "Scale, move and timestamp the chat").option("Chat timestamps"));
    }

    // ================================================================ classes

    public static class OverlayModule extends HudModule {
        public OverlayModule(String id, String name, String cat, String desc) { super(id, name, cat, desc, 0f, 0f, false, false); }
    }

    static class ToggleModule extends TextModule {
        private final boolean sprint;
        private boolean applied, prev;

        ToggleModule(String id, String name, String desc, float x, float y, boolean sprint) {
            super(id, name, "Utility", desc, x, y, sprint ? "[Sprint Toggle: ON]" : "[Sneak Toggle: ON]",
                    mc -> sprint ? (mc.player.isSprinting() ? "[Sprinting (Toggled)]" : "[Sprint Toggle: ON]")
                            : (mc.player.isSneaking() ? "[Sneaking (Toggled)]" : "[Sneak Toggle: ON]"));
            this.sprint = sprint;
        }

        @Override public void tick(MinecraftClient mc) {
            if (mc.options == null) return;
            SimpleOption<Boolean> opt = sprint ? mc.options.getSprintToggled() : mc.options.getSneakToggled();
            if (cfg().enabled) {
                if (!applied) { prev = opt.getValue(); applied = true; }
                if (!opt.getValue()) opt.setValue(true);
            } else if (applied) {
                opt.setValue(prev);
                applied = false;
            }
        }
    }

    static class PerfModule extends HudModule {
        PerfModule() { super("perf", "Performance Monitor", "Info", "FPS, 1% low, frame time, RAM, CPU and a frame graph", 0.13f, 0.30f, true, false); }

        String text(MinecraftClient mc) {
            return "FPS: " + mc.getCurrentFps() + "  (avg " + Tracker.avgFps + " | 1% " + Tracker.lowFps + ")"
                    + "\nFrame: " + f("%.1f ms", Tracker.frameMs)
                    + "\nRAM: " + Tracker.usedRam() + "\nCPU: " + f("%.0f%%", Tracker.cpu);
        }

        @Override public int width(MinecraftClient mc, boolean p) { int w = 100; for (String l : text(mc).split("\n")) w = Math.max(w, Gfx.tw(l)); return w; }
        @Override public int height(MinecraftClient mc, boolean p) { return 4 * 10 + 26; }

        @Override public void render(DrawContext ctx, MinecraftClient mc, int color, boolean preview) {
            String[] l = text(mc).split("\n");
            for (int i = 0; i < l.length; i++) Gfx.text(ctx, l[i], 0, i * 10, color, cfg().shadow);
            float[] fr = Tracker.recentFrames(50);
            int base = 40 + 24;
            for (int i = 0; i < 50; i++) {
                float ms = i < fr.length ? fr[i] : 8f;
                int h = Math.max(1, Math.min(22, Math.round(ms / 33f * 22f)));
                int col = Gfx.lerp((color & 0xFF000000) | 0x6C8CFF, (color & 0xFF000000) | 0xA06CFF, i / 49f);
                ctx.fill(i * 2, base - h, i * 2 + 2, base, Gfx.ga(col));
            }
        }
    }

    static class KeystrokesModule extends HudModule {
        KeystrokesModule() { super("keystrokes", "Keystrokes", "Combat", "WASD, mouse buttons (with CPS) and space bar", 0.13f, 0.58f, true, false); }

        @Override public int width(MinecraftClient mc, boolean p) { return 60; }
        @Override public int height(MinecraftClient mc, boolean p) { return 78; }

        void key(DrawContext c, int x, int y, int w, int h, String label, boolean down, int color, String sub) {
            int a = (color >>> 24) & 0xFF;
            int rgb = color & 0xFFFFFF;
            Gfx.rrect(c, x, y, w, h, 3, down ? Gfx.argb(rgb, a / 255f * 0.8f) : Gfx.argb(0x000000, a / 255f * 0.45f));
            Gfx.rborder(c, x, y, w, h, 3, 1, Gfx.argb(0xFFFFFF, a / 255f * 0.18f));
            int tc = down ? ((color & 0xFF000000) | 0x0B0D14) : color;
            if (sub == null) Gfx.textC(c, label, x + w / 2, y + (h - 8) / 2, tc, false);
            else { Gfx.textC(c, label, x + w / 2, y + 3, tc, false); Gfx.textC(c, sub, x + w / 2, y + 13, tc, false); }
        }

        @Override public void render(DrawContext c, MinecraftClient mc, int color, boolean preview) {
            var o = mc.options;
            key(c, 20, 0, 18, 18, "W", o.forwardKey.isPressed(), color, null);
            key(c, 0, 20, 18, 18, "A", o.leftKey.isPressed(), color, null);
            key(c, 20, 20, 18, 18, "S", o.backKey.isPressed(), color, null);
            key(c, 40, 20, 18, 18, "D", o.rightKey.isPressed(), color, null);
            key(c, 0, 40, 29, 24, "LMB", Tracker.leftHeld, color, String.valueOf(Tracker.cpsLeft()));
            key(c, 31, 40, 29, 24, "RMB", Tracker.rightHeld, color, String.valueOf(Tracker.cpsRight()));
            key(c, 0, 66, 60, 10, "", o.jumpKey.isPressed(), color, null);
            int al = (color >>> 24) & 0xFF;
            c.fill(22, 70, 38, 72, Gfx.ga(Gfx.argb(0xFFFFFF, al / 255f * 0.6f)));
        }
    }

    static class ArmorModule extends HudModule {
        static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

        ArmorModule() { super("armor", "Armor Status", "Gear", "Equipped armor with durability", 0.90f, 0.45f, true, false); }

        List<ItemStack> stacks(MinecraftClient mc, boolean preview) {
            List<ItemStack> l = new ArrayList<>();
            if (mc.player != null) for (EquipmentSlot s : SLOTS) { ItemStack st = mc.player.getEquippedStack(s); if (!st.isEmpty()) l.add(st); }
            if (l.isEmpty() && preview) {
                l.add(new ItemStack(Items.DIAMOND_HELMET)); l.add(new ItemStack(Items.DIAMOND_CHESTPLATE));
                l.add(new ItemStack(Items.DIAMOND_LEGGINGS)); l.add(new ItemStack(Items.DIAMOND_BOOTS));
                for (ItemStack s : l) s.setDamage(14);
            }
            return l;
        }

        @Override public int width(MinecraftClient mc, boolean p) { return stacks(mc, p).isEmpty() ? 0 : 20 + Gfx.tw("000/000"); }
        @Override public int height(MinecraftClient mc, boolean p) { int n = stacks(mc, p).size(); return n == 0 ? 0 : n * 18 - 2; }

        @Override public void render(DrawContext c, MinecraftClient mc, int color, boolean preview) {
            int i = 0;
            for (ItemStack s : stacks(mc, preview)) {
                c.drawItem(s, 0, i * 18);
                if (s.isDamageable()) {
                    int left = s.getMaxDamage() - s.getDamage();
                    Gfx.text(c, left + "/" + s.getMaxDamage(), 20, i * 18 + 4, ratioColor(left / (float) s.getMaxDamage(), color), cfg().shadow);
                }
                i++;
            }
        }
    }

    static class PotionModule extends HudModule {
        PotionModule() { super("potion", "Potion Effects", "Gear", "Active status effects with timers", 0.90f, 0.20f, true, false); }

        List<String> lines(MinecraftClient mc, boolean preview) {
            List<String> l = new ArrayList<>();
            if (mc.player != null) for (StatusEffectInstance e : mc.player.getStatusEffects()) {
                String n = e.getEffectType().value().getName().getString();
                int amp = e.getAmplifier();
                int sec = e.getDuration() / 20;
                String t = e.isInfinite() ? "**:**" : (sec / 60) + ":" + String.format("%02d", sec % 60);
                l.add(n + (amp > 0 ? " " + roman(amp + 1) : "") + " \u00a77" + t);
            }
            if (l.isEmpty() && preview) { l.add("Speed II \u00a771:32"); l.add("Strength I \u00a70:48"); l.add("Fire Resistance \u00a77**:**"); }
            return l;
        }

        @Override public int width(MinecraftClient mc, boolean p) { int w = 0; for (String s : lines(mc, p)) w = Math.max(w, Gfx.tw(s)); return w; }
        @Override public int height(MinecraftClient mc, boolean p) { int n = lines(mc, p).size(); return n == 0 ? 0 : n * 10 - 1; }

        @Override public void render(DrawContext c, MinecraftClient mc, int color, boolean preview) {
            int i = 0;
            for (String s : lines(mc, preview)) Gfx.text(c, s, 0, (i++) * 10, color, cfg().shadow);
        }
    }

    static class ItemCounterModule extends HudModule {
        ItemCounterModule() { super("itemcounter", "Item Counter", "Gear", "Total amount of the item in your hand", 0.90f, 0.72f, true, false); }

        ItemStack held(MinecraftClient mc, boolean preview) {
            ItemStack h = mc.player == null ? ItemStack.EMPTY : mc.player.getMainHandStack();
            return h.isEmpty() && preview ? new ItemStack(Items.GOLDEN_APPLE) : h;
        }

        int count(MinecraftClient mc, ItemStack held) {
            if (mc.player == null) return 12;
            PlayerInventory inv = mc.player.getInventory();
            int n = 0;
            for (int i = 0; i < inv.size(); i++) { ItemStack s = inv.getStack(i); if (s.isOf(held.getItem())) n += s.getCount(); }
            return Math.max(n, mc.player.getMainHandStack().isEmpty() ? 12 : n);
        }

        @Override public int width(MinecraftClient mc, boolean p) { ItemStack h = held(mc, p); return h.isEmpty() ? 0 : 20 + Gfx.tw("x" + count(mc, h)); }
        @Override public int height(MinecraftClient mc, boolean p) { return held(mc, p).isEmpty() ? 0 : 16; }

        @Override public void render(DrawContext c, MinecraftClient mc, int color, boolean preview) {
            ItemStack h = held(mc, preview);
            if (h.isEmpty()) return;
            c.drawItem(h, 0, 0);
            Gfx.text(c, "x" + count(mc, h), 20, 4, color, cfg().shadow);
        }
    }

    static class DurabilityModule extends HudModule {
        DurabilityModule() { super("durability", "Durability", "Gear", "Durability of the item in your hand", 0.90f, 0.82f, true, false); }

        ItemStack held(MinecraftClient mc, boolean preview) {
            ItemStack h = mc.player == null ? ItemStack.EMPTY : mc.player.getMainHandStack();
            if ((h.isEmpty() || !h.isDamageable()) && preview) { h = new ItemStack(Items.DIAMOND_SWORD); h.setDamage(300); }
            return h;
        }

        String text(ItemStack h) { int left = h.getMaxDamage() - h.getDamage(); return left + "/" + h.getMaxDamage() + " (" + Math.round(left * 100f / h.getMaxDamage()) + "%)"; }

        @Override public int width(MinecraftClient mc, boolean p) { ItemStack h = held(mc, p); return h.isEmpty() || !h.isDamageable() ? 0 : 20 + Gfx.tw(text(h)); }
        @Override public int height(MinecraftClient mc, boolean p) { ItemStack h = held(mc, p); return h.isEmpty() || !h.isDamageable() ? 0 : 16; }

        @Override public void render(DrawContext c, MinecraftClient mc, int color, boolean preview) {
            ItemStack h = held(mc, preview);
            if (h.isEmpty() || !h.isDamageable()) return;
            c.drawItem(h, 0, 0);
            float r = (h.getMaxDamage() - h.getDamage()) / (float) h.getMaxDamage();
            Gfx.text(c, text(h), 20, 4, ratioColor(r, color), cfg().shadow);
        }
    }

    static class TargetModule extends HudModule {
        TargetModule() { super("target", "Target HUD", "Combat", "Name, health and distance of your target", 0.38f, 0.64f, true, false); }

        LivingEntity current(MinecraftClient mc) {
            if (mc.targetedEntity instanceof LivingEntity le) return le;
            LivingEntity t = Tracker.target;
            if (t != null && !t.isRemoved() && System.currentTimeMillis() - Tracker.lastAttackMs < 5000) return t;
            return null;
        }

        @Override public int width(MinecraftClient mc, boolean p) { return current(mc) == null && !p ? 0 : 120; }
        @Override public int height(MinecraftClient mc, boolean p) { return current(mc) == null && !p ? 0 : 28; }

        @Override public void render(DrawContext c, MinecraftClient mc, int color, boolean preview) {
            LivingEntity t = current(mc);
            String name = t == null ? "Xx_Dex_xX" : t.getName().getString();
            float hp = t == null ? 14.5f : t.getHealth(), max = t == null ? 20f : Math.max(1f, t.getMaxHealth());
            float dist = t == null || mc.player == null ? 3.1f : mc.player.distanceTo(t);
            int a = color & 0xFF000000;
            Gfx.text(c, name, 0, 0, color, cfg().shadow);
            Gfx.rrect(c, 0, 11, 120, 6, 2, Gfx.argb(0x000000, ((color >>> 24) / 255f) * 0.5f));
            float pct = MathHelper.clamp(hp / max, 0f, 1f);
            int bar = Gfx.lerp(a | 0xFF4D4D, a | 0x46FFB4, pct);
            if (HITCOLOR.cfg().enabled && System.currentTimeMillis() < Tracker.hitFlashUntil) bar = a | DexConfig.get().crosshair.hitColor;
            Gfx.rrect(c, 0, 11, Math.max(2, Math.round(120 * pct)), 6, 2, bar);
            Gfx.text(c, f("%.1f/%.0f", hp, max) + "  " + f("%.1fm", dist), 0, 19, a | 0xAAB0C4, cfg().shadow);
        }
    }

    static class WaypointsModule extends HudModule {
        static final String[] ARROWS = {"\u2191", "\u2197", "\u2192", "\u2198", "\u2193", "\u2199", "\u2190", "\u2196"};

        WaypointsModule() { super("waypoints", "Waypoints", "Utility", "Nearest waypoints with distance & direction (/dexwp add <name>)", 0.13f, 0.17f, true, false); }

        List<String> lines(MinecraftClient mc, boolean preview) {
            List<String> l = new ArrayList<>();
            if (mc.player != null && mc.world != null) {
                String dim = mc.world.getRegistryKey().getValue().toString();
                List<Waypoint> wps = new ArrayList<>();
                for (Waypoint w : DexConfig.get().waypoints) if (dim.equals(w.dim)) wps.add(w);
                wps.sort((a, b) -> Double.compare(d(mc, a), d(mc, b)));
                for (int i = 0; i < Math.min(5, wps.size()); i++) {
                    Waypoint w = wps.get(i);
                    double dx = w.x - mc.player.getX(), dz = w.z - mc.player.getZ();
                    float target = (float) (-Math.atan2(dx, dz) * 180.0 / Math.PI);
                    float rel = MathHelper.wrapDegrees(target - mc.player.getYaw());
                    String arrow = ARROWS[Math.floorMod(Math.round(rel / 45f), 8)];
                    l.add(w.name + " \u00a77" + Math.round(d(mc, w)) + "m " + arrow);
                }
            }
            if (l.isEmpty() && preview) { l.add("Home \u00a77120m \u2197"); l.add("Mine \u00a7745m \u2190"); }
            return l;
        }

        static double d(MinecraftClient mc, Waypoint w) {
            double dx = w.x - mc.player.getX(), dy = w.y - mc.player.getY(), dz = w.z - mc.player.getZ();
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }

        @Override public int width(MinecraftClient mc, boolean p) { int w = 0; for (String s : lines(mc, p)) w = Math.max(w, Gfx.tw(s) + 8); return w; }
        @Override public int height(MinecraftClient mc, boolean p) { int n = lines(mc, p).size(); return n == 0 ? 0 : n * 10 - 1; }

        @Override public void render(DrawContext c, MinecraftClient mc, int color, boolean preview) {
            List<String> l = lines(mc, preview);
            List<Waypoint> wps = DexConfig.get().waypoints;
            for (int i = 0; i < l.size(); i++) {
                int col = i < wps.size() ? wps.get(i).color : 0x6C8CFF;
                c.fill(0, i * 10 + 2, 3, i * 10 + 5, Gfx.ga((color & 0xFF000000) | col));
                Gfx.text(c, l.get(i), 7, i * 10, color, cfg().shadow);
            }
        }
    }

    static class NotificationsModule extends HudModule {
        NotificationsModule() { super("notifications", "Notifications", "Utility", "Toast notifications for toggles, kills and profiles", 0.74f, 0.02f, true, false); noContainer(); }

        List<Notifications.Note> notes(boolean preview) {
            List<Notifications.Note> l = new ArrayList<>(Notifications.NOTES);
            if (l.isEmpty() && preview) {
                long n = System.currentTimeMillis();
                l.add(new Notifications.Note("DEX VISUAL", "Profile loaded: PvP", n, 0xA06CFF));
                l.add(new Notifications.Note("Kill", "You eliminated Steve", n, 0xFF4D6D));
            }
            return l;
        }

        @Override public int width(MinecraftClient mc, boolean p) { return notes(p).isEmpty() ? 0 : 140; }
        @Override public int height(MinecraftClient mc, boolean p) { int n = notes(p).size(); return n == 0 ? 0 : n * 27 - 3; }

        @Override public void render(DrawContext c, MinecraftClient mc, int color, boolean preview) {
            long now = System.currentTimeMillis();
            long life = DexConfig.get().notifSeconds * 1000L;
            float base = ((color >>> 24) & 0xFF) / 255f;
            int i = 0;
            List<Notifications.Note> l = notes(preview);
            for (int k = l.size() - 1; k >= 0; k--) {
                Notifications.Note n = l.get(k);
                long age = now - n.time();
                float fade = preview ? 1f : MathHelper.clamp(Math.min(age / 250f, (life - age) / 400f), 0f, 1f);
                int y = i++ * 27;
                Gfx.glow(c, 0, y, 140, 24, 5, n.color(), base * fade * 0.6f);
                Gfx.rrect(c, 0, y, 140, 24, 5, Gfx.argb(0x0B0D14, 0.85f * base * fade));
                Gfx.rrect(c, 0, y + 2, 3, 20, 1, Gfx.argb(n.color(), base * fade));
                Gfx.text(c, n.title(), 9, y + 3, Gfx.argb(0xFFFFFF, base * fade), false);
                Gfx.text(c, n.message(), 9, y + 13, Gfx.argb(0x9AA0B8, base * fade), false);
            }
        }
    }
}
