package com.dexvisual.gui;

import com.dexvisual.config.CrosshairSettings;
import com.dexvisual.config.DexConfig;
import com.dexvisual.config.Profile;
import com.dexvisual.config.Waypoint;
import com.dexvisual.gui.Widgets.*;
import com.dexvisual.hud.CrosshairRenderer;
import com.dexvisual.module.HudModule;
import com.dexvisual.module.Modules;
import com.dexvisual.util.Gfx;
import com.dexvisual.util.Notifications;
import com.dexvisual.util.Tracker;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

/** The main DEX VISUAL mod menu. */
public class MenuScreen extends Screen {
    static final String[] NAV = {"Dashboard", "Modules", "HUD Editor", "Crosshair", "Animations", "Performance", "Waypoints", "Profiles", "Settings", "About"};
    static final String[] CATS = {"Info", "Combat", "Gear", "Utility", "Overlay"};
    static final String[] STYLES = {"CROSS", "PLUS", "T", "X", "DOT", "CIRCLE"};
    static String page = "Dashboard";

    private HudModule detail;
    private WidgetList list;
    private int px, py, pw, ph;
    private static final int SB = 108;
    private final long opened = System.currentTimeMillis();
    private final float[] hov = new float[NAV.length];
    private final Map<String, Float> rowAnim = new HashMap<>();
    private Input nameInput;
    private String status = "";

    public MenuScreen() { super(Text.literal("DEX VISUAL")); }

    @Override public boolean shouldPause() { return false; }

    @Override protected void init() {
        pw = Math.min(width - 12, 470);
        ph = Math.min(height - 12, 310);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        build();
    }

    static CrosshairSettings cs() { return DexConfig.get().crosshair; }

    // =================================================================== render
    @Override public void renderBackground(DrawContext c, int mx, int my, float d) {}

    @Override public void render(DrawContext c, int mx, int my, float delta) {
        DexConfig cfg = DexConfig.get();
        float fade = cfg.menuAnim ? Gfx.ease((System.currentTimeMillis() - opened) / (250f / Math.max(0.3f, cfg.animSpeed))) : 1f;
        Gfx.alpha = fade;
        try {
            c.fill(0, 0, width, height, Gfx.ga(Gfx.argb(0x03040A, 0.62f)));
            Gfx.glow(c, px, py, pw, ph, 10, Theme.rgb(Theme.a1()), 1.1f);
            Gfx.rrect(c, px, py, pw, ph, 10, Gfx.argb(0x0D0F18, cfg.panelOpacity), Gfx.argb(0x140F28, cfg.panelOpacity));
            Gfx.rborder(c, px, py, pw, ph, 10, 1, 0x34FFFFFF);
            // sidebar
            Gfx.rrect(c, px + 1, py + 1, SB, ph - 2, 9, Gfx.argb(0x000000, 0.28f));
            Gfx.rrect(c, px + 8, py + 8, 24, 24, 7, Theme.a1(), Theme.a2());
            Gfx.textC(c, "DEX", px + 20, py + 16, 0xFFFFFFFF, false);
            Gfx.text(c, "DEX VISUAL", px + 37, py + 11, 0xFFFFFFFF, false);
            Gfx.text(c, "1.21.4  Fabric", px + 37, py + 22, Theme.MUTED, false);
            for (int i = 0; i < NAV.length; i++) {
                int iy = py + 42 + i * 17, ix = px + 6, iw = SB - 10;
                boolean on = NAV[i].equals(page), hv = Gfx.in(mx, my, ix, iy, iw, 16);
                hov[i] += ((on || hv ? 1f : 0f) - hov[i]) * 0.3f;
                if (hov[i] > 0.02f) {
                    int top = Gfx.lerp(0x00000000, on ? Gfx.argb(Theme.rgb(Theme.a1()), 0.35f) : 0x1EFFFFFF, hov[i]);
                    int bot = Gfx.lerp(0x00000000, on ? Gfx.argb(Theme.rgb(Theme.a2()), 0.18f) : 0x12FFFFFF, hov[i]);
                    Gfx.rrect(c, ix, iy, iw, 16, 5, top, bot);
                }
                if (on) Gfx.rrect(c, ix, iy + 3, 2, 10, 1, Theme.a1());
                Gfx.text(c, NAV[i], ix + 8 + Math.round(hov[i] * 2), iy + 4, on ? 0xFFFFFFFF : Gfx.lerp(Theme.MUTED, 0xFFFFFFFF, hov[i]), false);
            }
            Gfx.text(c, "Right Shift", px + 10, py + ph - 14, Gfx.argb(0xFFFFFF, 0.3f), false);
            // header
            int cx = px + SB + 8;
            Gfx.text(c, detail != null && page.equals("Modules") ? "Modules  /  " + detail.name : page, cx, py + 12, 0xFFFFFFFF, false);
            String prof = "\u25cf " + cfg.activeProfile;
            Gfx.rrect(c, px + pw - Gfx.tw(prof) - 34, py + 8, Gfx.tw(prof) + 12, 14, 7, Gfx.argb(Theme.rgb(Theme.a1()), 0.22f));
            Gfx.text(c, prof, px + pw - Gfx.tw(prof) - 28, py + 11, Theme.TEXT, false);
            boolean xh = Gfx.in(mx, my, px + pw - 20, py + 8, 14, 14);
            Gfx.rrect(c, px + pw - 20, py + 8, 14, 14, 5, xh ? 0xCCB8324A : 0x22FFFFFF);
            Gfx.textC(c, "x", px + pw - 13, py + 11, 0xFFFFFFFF, false);
            Gfx.rrect(c, cx, py + 26, pw - SB - 16, 1, 0, Theme.LINE);
            if (!status.isEmpty()) Gfx.textR(c, status, px + pw - 12, py + ph - 12, Theme.a1(), false);
            list.render(c, mx, my);
        } finally {
            Gfx.alpha = 1f;
        }
    }

    // ===================================================================== input
    @Override public boolean mouseClicked(double dx, double dy, int btn) {
        int mx = (int) dx, my = (int) dy;
        if (Gfx.in(mx, my, px + pw - 20, py + 8, 14, 14)) { close(); return true; }
        for (int i = 0; i < NAV.length; i++) {
            if (Gfx.in(mx, my, px + 6, py + 42 + i * 17, SB - 10, 16)) {
                if (NAV[i].equals("HUD Editor")) { client.setScreen(new HudEditorScreen(this)); return true; }
                page = NAV[i];
                detail = null;
                status = "";
                build();
                return true;
            }
        }
        list.click(mx, my, btn);
        return true;
    }

    @Override public boolean mouseDragged(double dx, double dy, int btn, double ddx, double ddy) { return list.drag((int) dx, (int) dy); }

    @Override public boolean mouseReleased(double dx, double dy, int btn) { list.release(); return true; }

    @Override public boolean mouseScrolled(double mx, double my, double h, double v) { return list.scroll(mx, my, v); }

    @Override public boolean keyPressed(int key, int sc, int mods) {
        if (Widgets.KeyBind.listening != null) {
            Widgets.KeyBind.listening.accept(key == GLFW.GLFW_KEY_ESCAPE ? -1 : key);
            return true;
        }
        if (Widgets.Input.focused != null) { Widgets.Input.key(key, mods); return true; }
        if (key == GLFW.GLFW_KEY_RIGHT_SHIFT) { close(); return true; }
        return super.keyPressed(key, sc, mods);
    }

    @Override public boolean charTyped(char ch, int mods) { return Widgets.Input.type(ch) || super.charTyped(ch, mods); }

    @Override public void removed() {
        Widgets.Input.focused = null;
        Widgets.KeyBind.listening = null;
        Gfx.alpha = 1f;
        DexConfig.get().save();
    }

    // ================================================================ page build
    private void build() {
        list = new WidgetList(px + SB + 8, py + 31, pw - SB - 16, ph - 38);
        Widgets.Input.focused = null;
        switch (page) {
            case "Dashboard" -> dashboard();
            case "Modules" -> { if (detail != null) moduleDetail(); else modules(); }
            case "Crosshair" -> crosshair();
            case "Animations" -> animations();
            case "Performance" -> performance();
            case "Waypoints" -> waypoints();
            case "Profiles" -> profiles();
            case "Settings" -> settings();
            default -> about();
        }
    }

    private void card(DrawContext c, int x, int y, int w, int h) {
        Gfx.rrect(c, x, y, w, h, 6, 0x16FFFFFF, 0x0CFFFFFF);
        Gfx.rborder(c, x, y, w, h, 6, 1, 0x1CFFFFFF);
    }

    private void stat(DrawContext c, int x, int y, int w, int h, String label, String value) {
        card(c, x, y, w, h);
        Gfx.text(c, label, x + 7, y + 6, Theme.MUTED, false);
        Gfx.text(c, value, x + 7, y + 20, 0xFFFFFFFF, false);
        Gfx.rrect(c, x + 7, y + h - 6, Math.max(8, w / 3), 2, 1, Theme.a1(), Theme.a2());
    }

    private void statRow(DrawContext c, int x, int y, int w, int h, String[][] s) {
        int n = s.length, cw = (w - (n - 1) * 4) / n;
        for (int i = 0; i < n; i++) stat(c, x + i * (cw + 4), y, cw, h, s[i][0], s[i][1]);
    }

    private void bar(DrawContext c, int x, int y, int w, float pct, String label) {
        Gfx.text(c, label, x, y, Theme.MUTED, false);
        Gfx.rrect(c, x, y + 10, w, 5, 2, Theme.TRACK);
        Gfx.rrect(c, x, y + 10, Math.max(3, (int) (w * Math.min(1f, pct))), 5, 2, Theme.a1(), Theme.a2());
    }

    private void graph(DrawContext c, int x, int y, int w, int h) {
        card(c, x, y, w, h);
        float[] fr = Tracker.recentFrames(120);
        int n = Math.min(fr.length, w - 8);
        for (int i = 0; i < n; i++) {
            float fps = 1000f / Math.max(1f, fr[fr.length - n + i]);
            int bh = Math.max(1, Math.min(h - 6, Math.round(fps / 300f * (h - 6))));
            int col = Gfx.lerp(Theme.a1(), Theme.a2(), i / (float) Math.max(1, n));
            c.fill(x + 4 + i, y + h - 3 - bh, x + 5 + i, y + h - 3, Gfx.ga(col));
        }
        Gfx.text(c, "FPS graph", x + 6, y + 4, Gfx.argb(0xFFFFFF, 0.5f), false);
        if (n < 2) Gfx.textC(c, "Join a world to record frames", x + w / 2, y + h / 2 - 4, Theme.MUTED, false);
    }

    // ---------------------------------------------------------------- dashboard
    private void dashboard() {
        list.add(new Heading("Overview"));
        list.add(new Block(38, (c, x, y, w, h, mx, my) -> statRow(c, x, y, w, h, new String[][]{
                {"FPS", String.valueOf(client.getCurrentFps())}, {"AVG / 1% LOW", Tracker.avgFps + " / " + Tracker.lowFps},
                {"PING", pingText()}, {"CPS", Tracker.cpsLeft() + " | " + Tracker.cpsRight()}})));
        list.add(new Heading("Player & Server"));
        list.add(new Block(66, (c, x, y, w, h, mx, my) -> {
            int half = (w - 4) / 2;
            card(c, x, y, half, h);
            card(c, x + half + 4, y, half, h);
            boolean g = client.player != null && client.world != null;
            String[] l = {"Player", g ? client.player.getName().getString() : client.getSession().getUsername(),
                    g ? String.format("XYZ %.0f %.0f %.0f", client.player.getX(), client.player.getY(), client.player.getZ()) : "XYZ  -",
                    g ? "Biome " + Modules.biome(client) : "Biome  -",
                    g ? "Facing " + Modules.facing(client.player.getYaw()) : "Facing  -"};
            for (int i = 0; i < l.length; i++) Gfx.text(c, Widgets.fit(l[i], half - 12), x + 7, y + 6 + i * 11, i == 0 ? Theme.a1() : Theme.TEXT, false);
            String addr = client.getCurrentServerEntry() != null ? client.getCurrentServerEntry().address : (client.isInSingleplayer() ? "Singleplayer" : "Not connected");
            int online = client.getNetworkHandler() == null ? 0 : client.getNetworkHandler().getPlayerList().size();
            String[] r = {"Server", addr, "Online players: " + online, "Version 1.21.4 (Fabric)", g ? client.world.getRegistryKey().getValue().getPath() : "Dimension  -"};
            for (int i = 0; i < r.length; i++) Gfx.text(c, Widgets.fit(r[i], half - 12), x + half + 11, y + 6 + i * 11, i == 0 ? Theme.a1() : Theme.TEXT, false);
        }));
        list.add(new Heading("System"));
        list.add(new Block(24, (c, x, y, w, h, mx, my) -> {
            int half = (w - 8) / 2;
            bar(c, x + 2, y, half, Tracker.ramPercent(), "RAM  " + Tracker.usedRam());
            bar(c, x + half + 8, y, half, Tracker.cpu / 100f, "CPU  " + String.format("%.0f%%", Tracker.cpu));
        }));
        list.add(new Block(60, (c, x, y, w, h, mx, my) -> graph(c, x, y, w, h)));
        list.add(new Heading("Quick toggles"));
        HudModule[] q = {Modules.FPS, Modules.CPS, Modules.PING, Modules.COORDS, Modules.KEYSTROKES, Modules.ARMOR, Modules.CROSSHAIR, Modules.TARGET};
        for (int i = 0; i < q.length; i += 2) {
            HudModule a = q[i], b = q[i + 1];
            list.addRow(new Toggle(a.name, () -> a.cfg().enabled, v -> Modules.set(a, v)), new Toggle(b.name, () -> b.cfg().enabled, v -> Modules.set(b, v)));
        }
    }

    private String pingText() {
        if (client.getNetworkHandler() == null || client.player == null) return "--";
        var e = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
        return e == null ? "--" : e.getLatency() + " ms";
    }

    // ------------------------------------------------------------------ modules
    private class ModuleRow extends W {
        final HudModule m;
        ModuleRow(HudModule m) { this.m = m; h = 26; }

        @Override public void render(DrawContext c, int mx, int my) {
            boolean on = m.cfg().enabled, hv = hover(mx, my);
            float a = rowAnim.getOrDefault(m.id, on ? 1f : 0f);
            a += ((on ? 1f : 0f) - a) * 0.35f;
            rowAnim.put(m.id, a);
            Gfx.rrect(c, x, y, w, h, 6, hv ? 0x26FFFFFF : 0x14FFFFFF);
            if (on) Gfx.rrect(c, x, y + 4, 2, h - 8, 1, Theme.a1());
            Gfx.text(c, Widgets.fit(m.name, w - 56), x + 8, y + 4, 0xFFFFFFFF, false);
            Gfx.text(c, Widgets.fit(m.description, w - 56), x + 8, y + 15, Theme.MUTED, false);
            int sw = 26, sh = 12, rx = x + w - sw - 8, ry = y + (h - sh) / 2;
            if (on) Gfx.glow(c, rx, ry, sw, sh, 6, Theme.rgb(Theme.a1()), 0.5f);
            Gfx.rrect(c, rx, ry, sw, sh, 6, Gfx.lerp(Theme.TRACK, Theme.a1(), a));
            Gfx.rrect(c, rx + 2 + Math.round(a * (sw - sh)), ry + 2, sh - 4, sh - 4, 4, 0xFFFFFFFF);
        }

        @Override public boolean click(int mx, int my, int btn) {
            if (mx > x + w - 40 || btn == 1) Modules.toggle(m);
            else { detail = m; build(); }
            return true;
        }
    }

    private void modules() {
        list.add(new Label("Click a module to customize it, or use the switch to toggle.", Theme.MUTED));
        for (String cat : CATS) {
            list.add(new Heading(cat));
            for (HudModule m : Modules.ALL) if (m.category.equals(cat)) list.add(new ModuleRow(m));
        }
    }

    private void moduleDetail() {
        list.add(new Button("< Back to modules", 0, () -> { detail = null; build(); }));
        list.add(new Heading(detail.name));
        list.add(new Label(detail.description, Theme.MUTED));
        ModulePanel.build(list, detail, this::build);
        if (detail == Modules.CROSSHAIR) list.add(new Button("Open Crosshair Editor", 1, () -> { page = "Crosshair"; detail = null; build(); }));
    }

    // ---------------------------------------------------------------- crosshair
    private void crosshair() {
        list.add(new Heading("Preview"));
        list.add(new Block(84, (c, x, y, w, h, mx, my) -> {
            for (int i = 0; i < w; i += 14) for (int j = 0; j < h; j += 14)
                c.fill(x + i, y + j, Math.min(x + i + 14, x + w), Math.min(y + j + 14, y + h), Gfx.ga(((i / 14 + j / 14) % 2 == 0) ? 0xFF4B8530 : 0xFF5D9C3A));
            Gfx.rborder(c, x, y, w, h, 0, 1, 0x40FFFFFF);
            long t = System.currentTimeMillis();
            boolean hit = cs().hitFlash && (t / 800) % 3 == 0;
            float extra = cs().dynamic ? (float) Math.abs(Math.sin(t / 600.0)) * 4f : 0f;
            CrosshairRenderer.draw(c, x + w / 2, y + h / 2, cs(), hit, extra);
            Gfx.text(c, hit ? "hit!" : "idle", x + 5, y + 4, 0xAAFFFFFF, true);
        }));
        list.add(new Heading("Presets"));
        list.addRow(new Button("Classic", 0, () -> preset("CROSS", 6, 1, 3, false)), new Button("Dot", 0, () -> preset("DOT", 4, 3, 0, true)),
                new Button("Tight", 0, () -> preset("PLUS", 5, 1, 0, false)), new Button("Circle", 0, () -> preset("CIRCLE", 8, 1, 0, true)));
        list.add(new Heading("Style"));
        list.add(new Select("Crosshair style", STYLES, () -> { for (int i = 0; i < STYLES.length; i++) if (STYLES[i].equals(cs().style)) return i; return 0; }, i -> cs().style = STYLES[i]));
        list.add(new Slider("Size", 1, 20, () -> (float) cs().size, v -> cs().size = Math.round(v), "%.0f", true));
        list.add(new Slider("Thickness", 1, 6, () -> (float) cs().thickness, v -> cs().thickness = Math.round(v), "%.0f", true));
        list.add(new Slider("Gap", 0, 12, () -> (float) cs().gap, v -> cs().gap = Math.round(v), "%.0f", true));
        list.add(new Toggle("Outline", () -> cs().outline, v -> cs().outline = v));
        list.add(new Toggle("Center dot", () -> cs().dot, v -> cs().dot = v));
        list.add(new Toggle("Dynamic (expands while moving)", () -> cs().dynamic, v -> cs().dynamic = v));
        list.add(new Heading("Color"));
        list.add(new ColorPick("Crosshair color", () -> cs().color, v -> cs().color = v));
        list.add(new Slider("Opacity", 0.1f, 1f, () -> cs().opacity, v -> cs().opacity = v, "%.2f", false));
        list.add(new Toggle("Hit color flash", () -> cs().hitFlash, v -> cs().hitFlash = v));
        list.add(new ColorPick("Hit color", () -> cs().hitColor, v -> cs().hitColor = v));
        list.add(new Heading("Custom presets"));
        list.add(new Button("Save current crosshair as preset", 1, () -> {
            CrosshairSettings s = cs().copy();
            s.name = "Custom " + (DexConfig.get().crosshairPresets.size() + 1);
            DexConfig.get().crosshairPresets.add(s);
            DexConfig.markDirty();
            build();
        }));
        for (CrosshairSettings p : new java.util.ArrayList<>(DexConfig.get().crosshairPresets)) {
            list.addRow(new Button(p.name == null ? "Preset" : p.name, 0, () -> { DexConfig.get().crosshair = p.copy(); DexConfig.markDirty(); build(); }),
                    new Button("Delete", 2, () -> { DexConfig.get().crosshairPresets.remove(p); DexConfig.markDirty(); build(); }));
        }
        if (!Modules.CROSSHAIR.cfg().enabled) list.add(new Button("Enable custom crosshair (currently off)", 1, () -> { Modules.set(Modules.CROSSHAIR, true); build(); }));
    }

    private void preset(String style, int size, int th, int gap, boolean dot) {
        CrosshairSettings s = cs();
        s.style = style; s.size = size; s.thickness = th; s.gap = gap; s.dot = dot;
        DexConfig.markDirty();
    }

    // --------------------------------------------------------------- animations
    private void animations() {
        DexConfig cfg = DexConfig.get();
        list.add(new Heading("Interface"));
        list.add(new Toggle("Menu fade animation", () -> DexConfig.get().menuAnim, v -> DexConfig.get().menuAnim = v));
        list.add(new Toggle("HUD fade / slide animations", () -> DexConfig.get().hudAnim, v -> DexConfig.get().hudAnim = v));
        list.add(new Slider("Animation speed", 0.3f, 2.5f, () -> DexConfig.get().animSpeed, v -> DexConfig.get().animSpeed = v, "%.2fx", false));
        list.add(new Select("Accent theme", Theme.ACCENT_NAMES, () -> DexConfig.get().accent, i -> DexConfig.get().accent = i));
        list.add(new Slider("Panel opacity", 0.4f, 1f, () -> DexConfig.get().panelOpacity, v -> DexConfig.get().panelOpacity = v, "%.2f", false));
        list.add(new Heading("Preview"));
        list.add(new Block(46, (c, x, y, w, h, mx, my) -> {
            card(c, x, y, w, h);
            float t = (System.currentTimeMillis() % 2400) / 2400f;
            float e = Gfx.ease(t < 0.5f ? t * 2f : (1f - t) * 2f);
            int bx = x + 8 + Math.round(e * (w - 70));
            Gfx.glow(c, bx, y + 10, 54, 26, 6, Theme.rgb(Theme.a1()), 0.5f + e);
            Gfx.rrect(c, bx, y + 10, 54, 26, 6, Theme.a1(), Theme.a2());
            Gfx.textC(c, "SLIDE", bx + 27, y + 19, 0xFFFFFFFF, false);
        }));
        list.add(new Label("Each HUD module has its own animation (NONE / FADE / SLIDE)", Theme.MUTED));
        list.add(new Label("in Modules > module > Animation or the HUD Editor.", Theme.MUTED));
        list.add(new Label("Only transforms and alpha are animated, so FPS stays unaffected.", Theme.MUTED));
    }

    // -------------------------------------------------------------- performance
    private void performance() {
        list.add(new Heading("Live statistics"));
        list.add(new Block(38, (c, x, y, w, h, mx, my) -> statRow(c, x, y, w, h, new String[][]{
                {"FPS", String.valueOf(client.getCurrentFps())}, {"AVERAGE", String.valueOf(Tracker.avgFps)},
                {"1% LOW", String.valueOf(Tracker.lowFps)}, {"PING", pingText()}})));
        list.add(new Block(38, (c, x, y, w, h, mx, my) -> {
            int ents = 0;
            if (client.world != null) for (var e : client.world.getEntities()) ents++;
            statRow(c, x, y, w, h, new String[][]{
                    {"RAM", Tracker.usedRam()}, {"CPU", String.format("%.0f%%", Tracker.cpu)},
                    {"RENDER DIST", client.options.getViewDistance().getValue() + " chunks"}, {"ENTITIES", String.valueOf(ents)}});
        }));
        list.add(new Block(24, (c, x, y, w, h, mx, my) -> {
            int half = (w - 8) / 2;
            bar(c, x + 2, y, half, Tracker.ramPercent(), "Memory  " + Math.round(Tracker.ramPercent() * 100) + "%");
            bar(c, x + half + 8, y, half, Tracker.cpu / 100f, "Frame time  " + String.format("%.1f ms", Tracker.frameMs));
        }));
        list.add(new Heading("Performance graph"));
        list.add(new Block(76, (c, x, y, w, h, mx, my) -> graph(c, x, y, w, h)));
        list.add(new Heading("HUD modules"));
        list.add(new Toggle(Modules.PERF.name, () -> Modules.PERF.cfg().enabled, v -> Modules.set(Modules.PERF, v)));
        list.add(new Toggle(Modules.MEMORY.name, () -> Modules.MEMORY.cfg().enabled, v -> Modules.set(Modules.MEMORY, v)));
        list.add(new Toggle(Modules.FPS.name, () -> Modules.FPS.cfg().enabled, v -> Modules.set(Modules.FPS, v)));
    }

    // ---------------------------------------------------------------- waypoints
    private void waypoints() {
        list.add(new Heading("New waypoint"));
        nameInput = list.add(new Input("Waypoint name", null));
        list.add(new Button("Add waypoint at my position", 1, () -> {
            if (client.player == null || client.world == null) { status = "Join a world first"; return; }
            String n = nameInput.text().isEmpty() ? "Waypoint " + (DexConfig.get().waypoints.size() + 1) : nameInput.text();
            DexConfig.get().addWaypoint(n, client.player.getX(), client.player.getY(), client.player.getZ(), client.world.getRegistryKey().getValue().toString());
            Notifications.push("Waypoint added", n, 0x46FFB4);
            build();
        }));
        list.add(new Toggle("Show waypoints on HUD", () -> Modules.WAYPOINTS.cfg().enabled, v -> Modules.set(Modules.WAYPOINTS, v)));
        list.add(new Heading("Saved waypoints"));
        if (DexConfig.get().waypoints.isEmpty()) list.add(new Label("No waypoints yet. Commands: /dexwp add <name>, /dexwp remove <name>, /dexwp list", Theme.MUTED));
        for (Waypoint w : new java.util.ArrayList<>(DexConfig.get().waypoints)) {
            list.addRow(new Label(w.name + "  " + Math.round(w.x) + " " + Math.round(w.y) + " " + Math.round(w.z), 0xFFFFFFFF),
                    new Button("Delete", 2, () -> { DexConfig.get().waypoints.remove(w); DexConfig.markDirty(); build(); }));
        }
    }

    // ----------------------------------------------------------------- profiles
    private void profiles() {
        DexConfig cfg = DexConfig.get();
        list.add(new Heading("Create / rename"));
        nameInput = list.add(new Input("Profile name", null));
        list.addRow(new Button("Create profile", 1, () -> {
            Profile p = cfg.createProfile(nameInput.text().isEmpty() ? "Custom" : nameInput.text());
            status = "Created " + p.name;
            build();
        }), new Button("Import (clipboard)", 0, () -> {
            Profile p = cfg.importProfile(client.keyboard.getClipboard());
            status = p == null ? "Clipboard is not a valid profile" : "Imported " + p.name;
            build();
        }));
        list.add(new Label("Type a name above, then use Create or Rename on a profile.", Theme.MUTED));
        list.add(new Heading("Your profiles"));
        for (Profile p : new java.util.ArrayList<>(cfg.profiles)) {
            boolean active = p.name.equals(cfg.activeProfile);
            list.add(new Block(14, (c, x, y, w, h, mx, my) -> {
                Gfx.text(c, (active ? "\u25cf " : "  ") + p.name + (active ? "  (active)" : ""), x + 3, y + 3, active ? Theme.a1() : 0xFFFFFFFF, false);
            }));
            list.addRow(
                    new Button("Load", active ? 1 : 0, () -> { cfg.applyProfile(p.name); Notifications.push("Profile loaded", p.name, 0xA06CFF); status = "Loaded " + p.name; build(); }),
                    new Button("Rename", 0, () -> { if (!nameInput.text().isEmpty()) { cfg.renameProfile(p, nameInput.text()); build(); } else status = "Type a new name first"; }),
                    new Button("Copy", 0, () -> { cfg.duplicateProfile(p); build(); }),
                    new Button("Export", 0, () -> { client.keyboard.setClipboard(cfg.exportProfile(p)); status = "Copied " + p.name + " to clipboard"; }),
                    new Button("Delete", 2, () -> { if (!cfg.deleteProfile(p)) status = "Keep at least one profile"; build(); }));
            list.gap(3);
        }
    }

    // ----------------------------------------------------------------- settings
    private void settings() {
        list.add(new Heading("General"));
        list.add(new Toggle("HUD visible", () -> DexConfig.get().hudVisible, v -> DexConfig.get().hudVisible = v));
        list.add(new Slider("Notification time (s)", 1, 10, () -> (float) DexConfig.get().notifSeconds, v -> DexConfig.get().notifSeconds = Math.round(v), "%.0f", true));
        list.add(new Heading("HUD Editor"));
        list.add(new Toggle("Snap to edges & elements", () -> DexConfig.get().snap, v -> DexConfig.get().snap = v));
        list.add(new Toggle("Show alignment guides", () -> DexConfig.get().guides, v -> DexConfig.get().guides = v));
        list.add(new Slider("Snap distance", 1, 12, () -> (float) DexConfig.get().snapDist, v -> DexConfig.get().snapDist = Math.round(v), "%.0f", true));
        list.add(new Heading("Interface"));
        list.add(new Select("Accent theme", Theme.ACCENT_NAMES, () -> DexConfig.get().accent, i -> DexConfig.get().accent = i));
        list.add(new Slider("Panel opacity", 0.4f, 1f, () -> DexConfig.get().panelOpacity, v -> DexConfig.get().panelOpacity = v, "%.2f", false));
        list.add(new Toggle("Menu animation", () -> DexConfig.get().menuAnim, v -> DexConfig.get().menuAnim = v));
        list.add(new Heading("Keybinds (module toggle keys)"));
        list.add(new Label("Menu / editor keys are in Options > Controls > DEX VISUAL.", Theme.MUTED));
        for (HudModule m : Modules.ALL) list.add(new KeyBind(m.name, () -> m.cfg().key, v -> m.cfg().key = v));
        list.add(new Heading("Reset"));
        list.add(new Button("Reset HUD layout & styles", 2, () -> { DexConfig.get().resetModules(true); status = "HUD reset"; build(); }));
        list.add(new Button("Reset ALL settings", 2, () -> { DexConfig.get().resetAll(); status = "Everything reset"; build(); }));
    }

    // -------------------------------------------------------------------- about
    private void about() {
        list.add(new Block(54, (c, x, y, w, h, mx, my) -> {
            Gfx.glow(c, x + 4, y + 4, 38, 38, 10, Theme.rgb(Theme.a1()), 1f);
            Gfx.rrect(c, x + 4, y + 4, 38, 38, 10, Theme.a1(), Theme.a2());
            Gfx.textC(c, "DEX", x + 23, y + 20, 0xFFFFFFFF, false);
            Gfx.text(c, "DEX VISUAL", x + 52, y + 10, 0xFFFFFFFF, false);
            Gfx.text(c, "Premium visual client mod  -  v1.0.0", x + 52, y + 24, Theme.MUTED, false);
        }));
        list.add(new Heading("Info"));
        list.add(new Label("Minecraft 1.21.4  |  Fabric Loader  |  Fabric API  |  Java 21", Theme.TEXT));
        list.add(new Label(Modules.ALL.size() + " modules  |  profiles & HUD presets  |  crosshair editor", Theme.TEXT));
        list.add(new Heading("Controls"));
        list.add(new Label("Right Shift  -  open this menu", Theme.MUTED));
        list.add(new Label("Right Ctrl  -  open HUD editor", Theme.MUTED));
        list.add(new Label("H  -  show / hide the whole HUD", Theme.MUTED));
        list.add(new Label("/dexwp add <name>  -  create a waypoint", Theme.MUTED));
        list.add(new Heading("HUD Editor tips"));
        list.add(new Label("Drag boxes to move, drag the corner to resize,", Theme.MUTED));
        list.add(new Label("mouse wheel to scale, right-click to toggle,", Theme.MUTED));
        list.add(new Label("arrow keys nudge (Shift = 5px).", Theme.MUTED));
    }
}
