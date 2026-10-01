package com.dexvisual.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

/** Small 2D drawing toolkit: rounded rectangles, borders, glow, text with a global fade multiplier. */
public final class Gfx {
    private Gfx() {}

    /** Global alpha multiplier (used for menu fade-in). */
    public static float alpha = 1f;

    public static int argb(int rgb, float a) {
        int al = Math.round(MathHelper.clamp(a, 0f, 1f) * 255f);
        return (al << 24) | (rgb & 0xFFFFFF);
    }

    public static int ga(int argb) {
        if (alpha >= 0.999f) return argb;
        int a = Math.round(((argb >>> 24) & 0xFF) * alpha);
        return (a << 24) | (argb & 0xFFFFFF);
    }

    public static int lerp(int c1, int c2, float t) {
        int a = (int) MathHelper.lerp(t, (c1 >>> 24) & 0xFF, (c2 >>> 24) & 0xFF);
        int r = (int) MathHelper.lerp(t, (c1 >> 16) & 0xFF, (c2 >> 16) & 0xFF);
        int g = (int) MathHelper.lerp(t, (c1 >> 8) & 0xFF, (c2 >> 8) & 0xFF);
        int b = (int) MathHelper.lerp(t, c1 & 0xFF, c2 & 0xFF);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int inset(int r, int i) {
        double d = r - i - 0.5;
        return (int) Math.round(r - Math.sqrt(Math.max(0, r * r - d * d)));
    }

    public static void rrect(DrawContext c, int x, int y, int w, int h, int r, int color) {
        rrect(c, x, y, w, h, r, color, color);
    }

    /** Rounded rectangle with a vertical gradient (top -> bottom). */
    public static void rrect(DrawContext c, int x, int y, int w, int h, int r, int top, int bottom) {
        if (w <= 0 || h <= 0) return;
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        top = ga(top);
        bottom = ga(bottom);
        if (top == bottom) {
            for (int i = 0; i < r; i++) {
                int in = inset(r, i);
                c.fill(x + in, y + i, x + w - in, y + i + 1, top);
                c.fill(x + in, y + h - 1 - i, x + w - in, y + h - i, top);
            }
            if (h - 2 * r > 0) c.fill(x, y + r, x + w, y + h - r, top);
        } else {
            for (int i = 0; i < h; i++) {
                int in = i < r ? inset(r, i) : (i >= h - r ? inset(r, h - 1 - i) : 0);
                c.fill(x + in, y + i, x + w - in, y + i + 1, lerp(top, bottom, i / (float) Math.max(1, h - 1)));
            }
        }
    }

    public static void rborder(DrawContext c, int x, int y, int w, int h, int r, int thick, int color) {
        for (int k = 0; k < Math.max(1, thick); k++) rborder1(c, x + k, y + k, w - 2 * k, h - 2 * k, Math.max(0, r - k), color);
    }

    private static void rborder1(DrawContext c, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0) return;
        color = ga(color);
        if (r <= 0) { c.drawBorder(x, y, w, h, color); return; }
        r = Math.min(r, Math.min(w, h) / 2);
        int i0 = inset(r, 0);
        c.fill(x + i0, y, x + w - i0, y + 1, color);
        c.fill(x + i0, y + h - 1, x + w - i0, y + h, color);
        for (int i = 1; i < r; i++) {
            int a = inset(r, i), b = inset(r, i - 1), end = Math.max(a + 1, b);
            c.fill(x + a, y + i, x + end, y + i + 1, color);
            c.fill(x + w - end, y + i, x + w - a, y + i + 1, color);
            c.fill(x + a, y + h - 1 - i, x + end, y + h - i, color);
            c.fill(x + w - end, y + h - 1 - i, x + w - a, y + h - i, color);
        }
        if (h - 2 * r > 0) {
            c.fill(x, y + r, x + 1, y + h - r, color);
            c.fill(x + w - 1, y + r, x + w, y + h - r, color);
        }
    }

    /** Soft glow behind a rounded rect (layered translucent expansions). */
    public static void glow(DrawContext c, int x, int y, int w, int h, int r, int rgb, float strength) {
        for (int k = 5; k >= 1; k--) {
            rrect(c, x - k, y - k, w + 2 * k, h + 2 * k, r + k, argb(rgb, 0.035f * strength * (6 - k) / 2f));
        }
    }

    public static void text(DrawContext c, String s, int x, int y, int color, boolean shadow) {
        color = ga(color);
        if (((color >>> 24) & 0xFF) < 5) return;
        c.drawText(MinecraftClient.getInstance().textRenderer, s, x, y, color, shadow);
    }

    public static void textC(DrawContext c, String s, int cx, int y, int color, boolean shadow) {
        text(c, s, cx - tw(s) / 2, y, color, shadow);
    }

    public static void textR(DrawContext c, String s, int rx, int y, int color, boolean shadow) {
        text(c, s, rx - tw(s), y, color, shadow);
    }

    public static int tw(String s) {
        return MinecraftClient.getInstance().textRenderer.getWidth(s);
    }

    public static boolean in(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    public static float ease(float t) {
        t = MathHelper.clamp(t, 0f, 1f);
        return 1f - (1f - t) * (1f - t) * (1f - t);
    }
}
