package com.dexvisual.gui;

import com.dexvisual.config.DexConfig;
import com.dexvisual.config.ModuleSettings;
import com.dexvisual.config.Profile;
import com.dexvisual.gui.Widgets.*;
import com.dexvisual.hud.HudRenderer;
import com.dexvisual.module.HudModule;
import com.dexvisual.module.Modules;
import com.dexvisual.util.Gfx;
import com.dexvisual.util.Notifications;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Fully interactive HUD editor: drag, resize, restyle, presets. */
public class HudEditorScreen extends Screen {
    private HudModule selected;
    private WidgetList inspector, modules, presets;
    private boolean showPresets, showModules = true;
    private HudModule dragMod;
    private boolean resizing;
    private int offX, offY;
    private int guideX = -1, guideY = -1;
    private final long opened = System.currentTimeMillis();
    private Input presetName;
    private final Screen parent;

    private static final int TOP = 24;

    public HudEditorScreen(Screen parent) {
        super(Text.literal("DEX VISUAL HUD Editor"));
        this.parent = parent;
    }

    @Override public boolean shouldPause() { return false; }

    @Override protected void init() {
        buildModules();
        buildInspector();
        buildPresets();
    }

    private void buildModules() {
        modules = new WidgetList(6, TOP + 18, 118, height - TOP - 24);
        for (HudModule m : Modules.ALL) {
            if (!m.box) continue;
            modules.add(new Toggle(m.name, () -> m.cfg().enabled, v -> Modules.set(m, v)));
        }
    }

    private void buildInspector() {
        int w = 150;
        inspector = new WidgetList(width - w - 6, TOP + 20, w, height - TOP - 26);
        if (selected != null) ModulePanel.build(inspector, selected, this::buildInspector);
    }

    private void buildPresets() {
        presets = new WidgetList(width / 2 - 80, TOP + 4, 160, Math.min(height - TOP - 12, 150));
        presetName = presets.add(new Input("Preset name", null));
        presets.add(new Button("Save current HUD as preset", 1, () -> {
            String n = presetName.text();
            Profile p = DexConfig.get().createProfile(n.isEmpty() ? "HUD Preset" : n);
            Notifications.push("Preset saved", p.name, 0x46FFB4);
            presetName.sb.setLength(0);
            buildPresets();
        }));
        presets.gap(2);
        for (Profile p : new ArrayList<>(DexConfig.get().profiles)) {
            boolean active = p.name.equals(DexConfig.get().activeProfile);
            presets.addRow(
                    new Button((active ? "\u25cf " : "") + p.name, active ? 1 : 0, () -> {
                        DexConfig.get().applyProfile(p.name);
                        Notifications.push("Preset loaded", p.name, 0xA06CFF);
                        buildPresets();
                    }),
                    new Button("X", 2, () -> {
                        if (DexConfig.get().deleteProfile(p)) buildPresets();
                    }));
        }
    }

    private void select(HudModule m) {
        if (selected != m) {
            selected = m;
            buildInspector();
        }
    }

    // ---------------------------------------------------------------- render
    @Override public void renderBackground(DrawContext c, int mx, int my, float d) {}

    @Override public void render(DrawContext c, int mx, int my, float delta) {
        float fade = Gfx.ease((System.currentTimeMillis() - opened) / 220f);
        c.fill(0, 0, width, height, Gfx.argb(0x04050A, 0.55f * fade));
        // grid
        for (int x = 0; x < width; x += 20) c.fill(x, 0, x + 1, height, 0x0AFFFFFF);
        for (int y = 0; y < height; y += 20) c.fill(0, y, width, y + 1, 0x0AFFFFFF);
        c.fill(width / 2, 0, width / 2 + 1, height, 0x10FFFFFF);
        c.fill(0, height / 2, width, height / 2 + 1, 0x10FFFFFF);

        HudRenderer.render(c, client, true);

        // hover + selection outlines
        for (HudModule m : Modules.ALL) {
            if (!m.box) continue;
            int[] b = HudRenderer.box(m, client, true);
            if (b == null) continue;
            if (m == selected) {
                Gfx.glow(c, b[0], b[1], b[2], b[3], 4, Theme.rgb(Theme.a1()), 1f);
                Gfx.rborder(c, b[0] - 1, b[1] - 1, b[2] + 2, b[3] + 2, 4, 1, Theme.a1());
                Gfx.rrect(c, b[0] + b[2] - 3, b[1] + b[3] - 3, 7, 7, 2, 0xFFFFFFFF);
                Gfx.text(c, m.name, b[0], b[1] - 10, Theme.a1(), true);
            } else if (Gfx.in(mx, my, b[0], b[1], b[2], b[3]) && dragMod == null) {
                Gfx.rborder(c, b[0], b[1], b[2], b[3], 4, 1, 0x66FFFFFF);
            }
        }
        if (dragMod != null && DexConfig.get().guides) {
            if (guideX >= 0) c.fill(guideX, 0, guideX + 1, height, Gfx.argb(Theme.rgb(Theme.a2()), 0.8f));
            if (guideY >= 0) c.fill(0, guideY, width, guideY + 1, Gfx.argb(Theme.rgb(Theme.a2()), 0.8f));
        }

        // top bar
        Gfx.rrect(c, 0, 0, width, TOP, 0, Gfx.argb(0x0B0D14, 0.92f));
        c.fill(0, TOP, width, TOP + 1, Gfx.argb(Theme.rgb(Theme.a1()), 0.5f));
        Gfx.rrect(c, 6, 4, 26, 16, 5, Theme.a1(), Theme.a2());
        Gfx.textC(c, "DEX", 19, 8, 0xFFFFFFFF, false);
        Gfx.text(c, "HUD EDITOR", 38, 8, Theme.TEXT, false);
        Gfx.text(c, "Profile: " + DexConfig.get().activeProfile, 105, 8, Theme.MUTED, false);

        renderTopButtons(c, mx, my);

        // left module list
        if (showModules) {
            panel(c, 4, TOP + 4, 122, height - TOP - 8);
            Gfx.text(c, "MODULES", 12, TOP + 9, Theme.a1(), false);
            modules.render(c, mx, my);
        }
        // inspector
        panel(c, width - 156, TOP + 4, 152, height - TOP - 8);
        Gfx.text(c, selected == null ? "INSPECTOR" : selected.name.toUpperCase(), width - 148, TOP + 9, Theme.a1(), false);
        if (selected == null) Gfx.text(c, "Click a HUD element", width - 148, TOP + 26, Theme.MUTED, false);
        else inspector.render(c, mx, my);
        if (showPresets) {
            panel(c, width / 2 - 86, TOP + 2, 172, Math.min(height - TOP - 10, 158));
            presets.render(c, mx, my);
        }
        Gfx.text(c, "Drag = move  |  corner handle = resize  |  scroll = scale  |  arrows = nudge", width / 2 - 140, height - 10, Gfx.argb(0xFFFFFF, 0.35f), false);
    }

    private void panel(DrawContext c, int x, int y, int w, int h) {
        Gfx.rrect(c, x, y, w, h, 8, Gfx.argb(0x0B0D14, 0.88f), Gfx.argb(0x120F24, 0.88f));
        Gfx.rborder(c, x, y, w, h, 8, 1, 0x30FFFFFF);
    }

    private static final String[] TOPB = {"Modules", "Presets", "Reset HUD", "Menu", "Done"};

    private int[] topBtn(int i) {
        int w = 56, g = 4, total = TOPB.length * (w + g);
        return new int[]{width - total + i * (w + g) - 2 - (i == 3 ? 0 : 0), 3, w, 18};
    }

    private void renderTopButtons(DrawContext c, int mx, int my) {
        for (int i = 0; i < TOPB.length; i++) {
            int[] b = topBtn(i);
            boolean hv = Gfx.in(mx, my, b[0], b[1], b[2], b[3]);
            boolean act = (i == 0 && showModules) || (i == 1 && showPresets);
            if (i == 4) Gfx.rrect(c, b[0], b[1], b[2], b[3], 5, Theme.a1(), Theme.a2());
            else if (i == 2) Gfx.rrect(c, b[0], b[1], b[2], b[3], 5, hv ? 0xCCB8324A : 0x66B8324A);
            else Gfx.rrect(c, b[0], b[1], b[2], b[3], 5, act ? Gfx.argb(Theme.rgb(Theme.a1()), 0.4f) : (hv ? 0x30FFFFFF : 0x18FFFFFF));
            Gfx.textC(c, TOPB[i], b[0] + b[2] / 2, b[1] + 5, 0xFFFFFFFF, false);
        }
    }

    // ----------------------------------------------------------------- input
    @Override public boolean mouseClicked(double dx, double dy, int btn) {
        int mx = (int) dx, my = (int) dy;
        for (int i = 0; i < TOPB.length; i++) {
            int[] b = topBtn(i);
            if (Gfx.in(mx, my, b[0], b[1], b[2], b[3])) {
                switch (i) {
                    case 0 -> showModules = !showModules;
                    case 1 -> { showPresets = !showPresets; buildPresets(); }
                    case 2 -> { DexConfig.get().resetModules(true); Notifications.push("HUD reset", "Layout restored", 0xFF4D6D); buildInspector(); }
                    case 3 -> { DexConfig.get().save(); client.setScreen(new MenuScreen()); }
                    default -> close();
                }
                return true;
            }
        }
        if (showPresets && Gfx.in(mx, my, width / 2 - 86, TOP + 2, 172, 158)) { presets.click(mx, my, btn); return true; }
        if (showModules && modules.click(mx, my, btn)) return true;
        if (selected != null && Gfx.in(mx, my, width - 156, TOP + 4, 152, height - TOP - 8)) { inspector.click(mx, my, btn); return true; }

        // HUD elements (top-most first)
        for (int i = Modules.ALL.size() - 1; i >= 0; i--) {
            HudModule m = Modules.ALL.get(i);
            if (!m.box) continue;
            int[] b = HudRenderer.box(m, client, true);
            if (b == null || !Gfx.in(mx, my, b[0] - 2, b[1] - 2, b[2] + 6, b[3] + 6)) continue;
            select(m);
            if (btn == 1) { Modules.toggle(m); return true; }
            dragMod = m;
            resizing = m == selected && Gfx.in(mx, my, b[0] + b[2] - 6, b[1] + b[3] - 6, 10, 10);
            offX = mx - b[0];
            offY = my - b[1];
            return true;
        }
        selected = null;
        buildInspector();
        return true;
    }

    @Override public boolean mouseDragged(double dx, double dy, int btn, double ddx, double ddy) {
        int mx = (int) dx, my = (int) dy;
        if (dragMod != null) {
            ModuleSettings s = dragMod.cfg();
            int[] b = HudRenderer.box(dragMod, client, true);
            if (b == null) return true;
            if (resizing) {
                s.scale = MathHelper.clamp((mx - b[0]) / (float) b[4], 0.5f, 3f);
            } else {
                int nx = mx - offX, ny = my - offY;
                guideX = guideY = -1;
                if (DexConfig.get().snap) {
                    int sd = DexConfig.get().snapDist;
                    int[] xs = {0, (width - b[2]) / 2, width - b[2]}, ys = {0, (height - b[3]) / 2, height - b[3]};
                    for (int v : xs) if (Math.abs(nx - v) <= sd) { nx = v; guideX = v == 0 ? 0 : (v == xs[1] ? width / 2 : width - 1); }
                    for (int v : ys) if (Math.abs(ny - v) <= sd) { ny = v; guideY = v == 0 ? 0 : (v == ys[1] ? height / 2 : height - 1); }
                    for (HudModule o : Modules.ALL) {
                        if (o == dragMod || !o.box) continue;
                        int[] ob = HudRenderer.box(o, client, true);
                        if (ob == null || !o.cfg().enabled) continue;
                        if (Math.abs(nx - ob[0]) <= sd) { nx = ob[0]; guideX = nx; }
                        else if (Math.abs(nx - (ob[0] + ob[2])) <= sd) { nx = ob[0] + ob[2]; guideX = nx; }
                        if (Math.abs(ny - ob[1]) <= sd) { ny = ob[1]; guideY = ny; }
                        else if (Math.abs(ny - (ob[1] + ob[3])) <= sd) { ny = ob[1] + ob[3]; guideY = ny; }
                    }
                }
                s.x = MathHelper.clamp(nx / (float) width, 0f, 1f);
                s.y = MathHelper.clamp(ny / (float) height, 0f, 1f);
            }
            DexConfig.markDirty();
            return true;
        }
        if (inspector.drag(mx, my) | modules.drag(mx, my) | presets.drag(mx, my)) return true;
        return false;
    }

    @Override public boolean mouseReleased(double dx, double dy, int btn) {
        if (dragMod != null) { dragMod = null; resizing = false; guideX = guideY = -1; buildInspector(); }
        inspector.release(); modules.release(); presets.release();
        return true;
    }

    @Override public boolean mouseScrolled(double mx, double my, double h, double v) {
        if (showPresets && presets.scroll(mx, my, v)) return true;
        if (showModules && modules.scroll(mx, my, v)) return true;
        if (inspector.scroll(mx, my, v)) return true;
        for (int i = Modules.ALL.size() - 1; i >= 0; i--) {
            HudModule m = Modules.ALL.get(i);
            if (!m.box) continue;
            int[] b = HudRenderer.box(m, client, true);
            if (b != null && Gfx.in((int) mx, (int) my, b[0], b[1], b[2], b[3])) {
                m.cfg().scale = MathHelper.clamp(m.cfg().scale + (float) v * 0.05f, 0.5f, 3f);
                DexConfig.markDirty();
                select(m);
                return true;
            }
        }
        return false;
    }

    @Override public boolean keyPressed(int key, int sc, int mods) {
        if (Widgets.KeyBind.listening != null) {
            Widgets.KeyBind.listening.accept(key == GLFW.GLFW_KEY_ESCAPE ? -1 : key);
            return true;
        }
        if (Widgets.Input.focused != null) { Widgets.Input.key(key, mods); return true; }
        if (selected != null && selected.box) {
            int step = (mods & GLFW.GLFW_MOD_SHIFT) != 0 ? 5 : 1;
            ModuleSettings s = selected.cfg();
            switch (key) {
                case GLFW.GLFW_KEY_LEFT -> s.x -= step / (float) width;
                case GLFW.GLFW_KEY_RIGHT -> s.x += step / (float) width;
                case GLFW.GLFW_KEY_UP -> s.y -= step / (float) height;
                case GLFW.GLFW_KEY_DOWN -> s.y += step / (float) height;
                default -> { return super.keyPressed(key, sc, mods); }
            }
            s.x = MathHelper.clamp(s.x, 0f, 1f);
            s.y = MathHelper.clamp(s.y, 0f, 1f);
            DexConfig.markDirty();
            return true;
        }
        return super.keyPressed(key, sc, mods);
    }

    @Override public boolean charTyped(char ch, int mods) {
        return Widgets.Input.type(ch) || super.charTyped(ch, mods);
    }

    @Override public void removed() {
        Widgets.Input.focused = null;
        Widgets.KeyBind.listening = null;
        DexConfig.get().save();
    }
}
