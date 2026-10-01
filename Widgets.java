package com.dexvisual.gui;

import com.dexvisual.config.DexConfig;
import com.dexvisual.util.Gfx;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/** Collection of premium-styled widgets. */
public final class Widgets {
    private Widgets() {}

    public interface Painter { void paint(DrawContext c, int x, int y, int w, int h, int mx, int my); }

    public static String fit(String s, int maxW) {
        if (Gfx.tw(s) <= maxW) return s;
        while (s.length() > 1 && Gfx.tw(s + "...") > maxW) s = s.substring(0, s.length() - 1);
        return s + "...";
    }

    public static class Label extends W {
        private final String t; private final int col;
        public Label(String t, int col) { this.t = t; this.col = col; h = 12; }
        @Override public void render(DrawContext c, int mx, int my) { Gfx.text(c, fit(t, w), x + 2, y + 2, col, false); }
    }

    public static class Heading extends W {
        private final String t;
        public Heading(String t) { this.t = t; h = 18; }
        @Override public void render(DrawContext c, int mx, int my) {
            String u = t.toUpperCase(Locale.ROOT);
            Gfx.text(c, u, x + 2, y + 6, Theme.a1(), false);
            int tw = Gfx.tw(u);
            Gfx.rrect(c, x + tw + 8, y + 10, Math.max(0, w - tw - 8), 1, 0, Theme.LINE);
        }
    }

    public static class Block extends W {
        private final Painter p;
        public Block(int h, Painter p) { this.h = h; this.p = p; }
        @Override public void render(DrawContext c, int mx, int my) { p.paint(c, x, y, w, h, mx, my); }
    }

    public static class Toggle extends W {
        private final String label; private final BooleanSupplier get; private final Consumer<Boolean> set; private float anim = -1f;

        public Toggle(String label, BooleanSupplier get, Consumer<Boolean> set) { this.label = label; this.get = get; this.set = set; h = 18; }

        @Override public void render(DrawContext c, int mx, int my) {
            boolean on = get.getAsBoolean();
            if (anim < 0) anim = on ? 1f : 0f;
            anim += ((on ? 1f : 0f) - anim) * 0.35f;
            if (hover(mx, my)) Gfx.rrect(c, x, y, w, h, 4, Theme.HOVER);
            Gfx.text(c, fit(label, w - 44), x + 6, y + 5, Theme.TEXT, false);
            int sw = 26, sh = 12, rx = x + w - sw - 6, ry = y + (h - sh) / 2;
            if (on) Gfx.glow(c, rx, ry, sw, sh, 6, Theme.rgb(Theme.a1()), 0.5f);
            Gfx.rrect(c, rx, ry, sw, sh, 6, Gfx.lerp(Theme.TRACK, Theme.a1(), anim));
            Gfx.rrect(c, rx + 2 + Math.round(anim * (sw - sh)), ry + 2, sh - 4, sh - 4, 4, 0xFFFFFFFF);
        }

        @Override public boolean click(int mx, int my, int b) {
            set.accept(!get.getAsBoolean());
            DexConfig.markDirty();
            return true;
        }
    }

    public static class Slider extends W {
        private final String label, fmt; private final float min, max; private final Supplier<Float> get; private final Consumer<Float> set;
        private final boolean integer; private boolean dragging;

        public Slider(String label, float min, float max, Supplier<Float> get, Consumer<Float> set, String fmt, boolean integer) {
            this.label = label; this.min = min; this.max = max; this.get = get; this.set = set; this.fmt = fmt; this.integer = integer; h = 24;
        }

        @Override public void render(DrawContext c, int mx, int my) {
            float v = get.get();
            String vs = integer ? String.valueOf(Math.round(v)) : String.format(Locale.ROOT, fmt, v);
            if (hover(mx, my) || dragging) Gfx.rrect(c, x, y, w, h, 4, Theme.HOVER);
            Gfx.text(c, fit(label, w - 60), x + 6, y + 3, Theme.TEXT, false);
            Gfx.textR(c, vs, x + w - 6, y + 3, Theme.MUTED, false);
            int tx = x + 6, tw = w - 12, ty = y + 16;
            float pct = MathHelper.clamp((v - min) / (max - min), 0f, 1f);
            int fw = (int) (tw * pct);
            Gfx.rrect(c, tx, ty, tw, 4, 2, Theme.TRACK);
            if (fw > 0) Gfx.rrect(c, tx, ty, Math.max(3, fw), 4, 2, Theme.a1(), Theme.a2());
            Gfx.rrect(c, tx + fw - 3, ty - 2, 7, 8, 3, 0xFFFFFFFF);
        }

        private void apply(int mx) {
            float pct = MathHelper.clamp((mx - (x + 6)) / (float) (w - 12), 0f, 1f);
            float v = min + pct * (max - min);
            if (integer) v = Math.round(v);
            set.accept(v);
            DexConfig.markDirty();
        }

        @Override public boolean click(int mx, int my, int b) { dragging = true; apply(mx); return true; }
        @Override public boolean drag(int mx, int my) { if (dragging) apply(mx); return dragging; }
        @Override public void release() { dragging = false; }
    }

    public static class Button extends W {
        private final String label; private final Runnable action; private final int style;

        /** @param style 0 normal, 1 primary (gradient), 2 danger */
        public Button(String label, int style, Runnable action) { this.label = label; this.style = style; this.action = action; h = 18; }

        @Override public void render(DrawContext c, int mx, int my) {
            boolean hv = hover(mx, my);
            if (style == 1) {
                if (hv) Gfx.glow(c, x, y, w, h, 5, Theme.rgb(Theme.a1()), 0.7f);
                Gfx.rrect(c, x, y, w, h, 5, Theme.a1(), Theme.a2());
            } else if (style == 2) {
                Gfx.rrect(c, x, y, w, h, 5, hv ? 0xCCB8324A : 0x66B8324A);
            } else {
                Gfx.rrect(c, x, y, w, h, 5, hv ? 0x30FFFFFF : 0x18FFFFFF);
                Gfx.rborder(c, x, y, w, h, 5, 1, hv ? Gfx.argb(Theme.rgb(Theme.a1()), 0.7f) : 0x22FFFFFF);
            }
            Gfx.textC(c, fit(label, w - 6), x + w / 2, y + (h - 8) / 2, 0xFFFFFFFF, false);
        }

        @Override public boolean click(int mx, int my, int b) { action.run(); return true; }
    }

    public static class Select extends W {
        private final String label; private final String[] opts; private final IntSupplier get; private final IntConsumer set;

        public Select(String label, String[] opts, IntSupplier get, IntConsumer set) { this.label = label; this.opts = opts; this.get = get; this.set = set; h = 18; }

        @Override public void render(DrawContext c, int mx, int my) {
            if (hover(mx, my)) Gfx.rrect(c, x, y, w, h, 4, Theme.HOVER);
            Gfx.text(c, fit(label, w / 2), x + 6, y + 5, Theme.TEXT, false);
            int i = Math.floorMod(get.getAsInt(), opts.length);
            Gfx.textR(c, "< " + opts[i] + " >", x + w - 6, y + 5, Theme.a1(), false);
        }

        @Override public boolean click(int mx, int my, int b) {
            int i = Math.floorMod(get.getAsInt() + (b == 1 ? -1 : 1), opts.length);
            set.accept(i);
            DexConfig.markDirty();
            return true;
        }
    }

    public static class ColorPick extends W {
        static final int[] PAL = {0xFFFFFF, 0xC8CCD8, 0x808794, 0x000000, 0xFF4D6D, 0xFF8A4D, 0xFFC857, 0xFFF04D,
                0x46FFB4, 0x4DFF6C, 0x3FD8FF, 0x6C8CFF, 0xA06CFF, 0xFF6CB4, 0xFF3FF0, 0x0B0D14};
        private final String label; private final IntSupplier get; private final IntConsumer set; private boolean hueDrag;

        public ColorPick(String label, IntSupplier get, IntConsumer set) { this.label = label; this.get = get; this.set = set; h = 42; }

        static int hsv(float h, float s, float v) {
            float r, g, b;
            int i = (int) Math.floor(h * 6);
            float f = h * 6 - i, p = v * (1 - s), q = v * (1 - f * s), t = v * (1 - (1 - f) * s);
            switch (Math.floorMod(i, 6)) {
                case 0 -> { r = v; g = t; b = p; }
                case 1 -> { r = q; g = v; b = p; }
                case 2 -> { r = p; g = v; b = t; }
                case 3 -> { r = p; g = q; b = v; }
                case 4 -> { r = t; g = p; b = v; }
                default -> { r = v; g = p; b = q; }
            }
            return ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
        }

        private int sz() { return Math.max(6, Math.min(11, (w - 12) / PAL.length - 1)); }

        @Override public void render(DrawContext c, int mx, int my) {
            int cur = get.getAsInt() & 0xFFFFFF;
            Gfx.text(c, fit(label, w - 40), x + 6, y + 3, Theme.TEXT, false);
            Gfx.rrect(c, x + w - 28, y + 2, 22, 9, 3, 0xFF000000 | cur);
            Gfx.rborder(c, x + w - 28, y + 2, 22, 9, 3, 1, 0x55FFFFFF);
            int s = sz();
            for (int i = 0; i < PAL.length; i++) {
                int sx = x + 6 + i * (s + 1);
                Gfx.rrect(c, sx, y + 15, s, s, 2, 0xFF000000 | PAL[i]);
                if (PAL[i] == cur) Gfx.rborder(c, sx - 1, y + 14, s + 2, s + 2, 2, 1, 0xFFFFFFFF);
            }
            int bw = w - 12;
            for (int i = 0; i < bw; i += 2) c.fill(x + 6 + i, y + 29, x + 8 + i, y + 37, Gfx.ga(0xFF000000 | hsv(i / (float) bw, 1f, 1f)));
            Gfx.rborder(c, x + 6, y + 29, bw, 8, 0, 1, 0x33FFFFFF);
        }

        private void hue(int mx) {
            float hh = MathHelper.clamp((mx - (x + 6)) / (float) (w - 12), 0f, 0.999f);
            set.accept(hsv(hh, 1f, 1f));
            DexConfig.markDirty();
        }

        @Override public boolean click(int mx, int my, int b) {
            if (my >= y + 28) { hueDrag = true; hue(mx); return true; }
            int s = sz();
            int i = (mx - (x + 6)) / (s + 1);
            if (my >= y + 14 && i >= 0 && i < PAL.length) { set.accept(PAL[i]); DexConfig.markDirty(); }
            return true;
        }

        @Override public boolean drag(int mx, int my) { if (hueDrag) hue(mx); return hueDrag; }
        @Override public void release() { hueDrag = false; }
    }

    public static class KeyBind extends W {
        public static KeyBind listening;
        private final String label; private final IntSupplier get; private final IntConsumer set;

        public KeyBind(String label, IntSupplier get, IntConsumer set) { this.label = label; this.get = get; this.set = set; h = 18; }

        public static String keyName(int k) {
            if (k < 0) return "None";
            try { return InputUtil.fromKeyCode(k, -1).getLocalizedText().getString(); } catch (Exception e) { return "Key " + k; }
        }

        @Override public void render(DrawContext c, int mx, int my) {
            if (hover(mx, my)) Gfx.rrect(c, x, y, w, h, 4, Theme.HOVER);
            Gfx.text(c, fit(label, w - 90), x + 6, y + 5, Theme.TEXT, false);
            boolean l = listening == this;
            int bw = 76, bx = x + w - bw - 4;
            Gfx.rrect(c, bx, y + 2, bw, h - 4, 4, l ? Gfx.argb(Theme.rgb(Theme.a1()), 0.35f) : 0x22FFFFFF);
            Gfx.rborder(c, bx, y + 2, bw, h - 4, 4, 1, l ? Theme.a1() : 0x22FFFFFF);
            Gfx.textC(c, l ? "Press key..." : fit(keyName(get.getAsInt()), bw - 6), bx + bw / 2, y + 5, 0xFFFFFFFF, false);
        }

        @Override public boolean click(int mx, int my, int b) { listening = this; return true; }

        public void accept(int key) {
            set.accept(key);
            DexConfig.markDirty();
            listening = null;
        }
    }

    public static class Input extends W {
        public static Input focused;
        public final StringBuilder sb = new StringBuilder();
        private final String hint; private final Runnable enter;
        public int max = 24;

        public Input(String hint, Runnable enter) { this.hint = hint; this.enter = enter; h = 18; }

        public String text() { return sb.toString().trim(); }

        @Override public void render(DrawContext c, int mx, int my) {
            boolean f = focused == this;
            Gfx.rrect(c, x, y, w, h, 5, 0x22FFFFFF);
            Gfx.rborder(c, x, y, w, h, 5, 1, f ? Theme.a1() : 0x22FFFFFF);
            String s = sb.toString();
            if (s.isEmpty() && !f) Gfx.text(c, hint, x + 6, y + 5, Theme.MUTED, false);
            else Gfx.text(c, fit(s, w - 16) + (f && (System.currentTimeMillis() / 500) % 2 == 0 ? "_" : ""), x + 6, y + 5, 0xFFFFFFFF, false);
        }

        @Override public boolean click(int mx, int my, int b) { focused = this; return true; }

        public static boolean type(char ch) {
            if (focused == null) return false;
            if (ch >= 32 && ch != 127 && focused.sb.length() < focused.max) focused.sb.append(ch);
            return true;
        }

        public static boolean key(int key, int mods) {
            Input f = focused;
            if (f == null) return false;
            if (key == GLFW.GLFW_KEY_BACKSPACE) { if (f.sb.length() > 0) f.sb.deleteCharAt(f.sb.length() - 1); }
            else if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) { if (f.enter != null) f.enter.run(); }
            else if (key == GLFW.GLFW_KEY_ESCAPE) focused = null;
            else if (key == GLFW.GLFW_KEY_V && (mods & GLFW.GLFW_MOD_CONTROL) != 0) {
                String cb = MinecraftClient.getInstance().keyboard.getClipboard();
                if (cb != null) for (char ch : cb.toCharArray()) if (ch >= 32 && f.sb.length() < f.max) f.sb.append(ch);
            }
            return true;
        }
    }
}
