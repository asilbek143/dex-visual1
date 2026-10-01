package com.dexvisual.hud;

import com.dexvisual.config.CrosshairSettings;
import com.dexvisual.config.DexConfig;
import com.dexvisual.module.Modules;
import com.dexvisual.util.Gfx;
import com.dexvisual.util.Tracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public final class CrosshairRenderer {
    private CrosshairRenderer() {}

    private static float dyn = 0f;

    public static void drawHud(DrawContext ctx, MinecraftClient mc) {
        if (mc.player == null || mc.options.hudHidden || mc.currentScreen != null || !mc.options.getPerspective().isFirstPerson()) return;
        CrosshairSettings s = DexConfig.get().crosshair;
        float target = 0f;
        if (s.dynamic) {
            Vec3d v = mc.player.getVelocity();
            target = (float) Math.min(6.0, Math.hypot(v.x, v.z) * 24.0);
            if (!mc.player.isOnGround()) target += 3f;
            if (mc.player.isSprinting()) target += 1.5f;
        }
        dyn += (target - dyn) * 0.25f;
        boolean hit = s.hitFlash && Modules.HITCOLOR.cfg().enabled && System.currentTimeMillis() < Tracker.hitFlashUntil;
        draw(ctx, mc.getWindow().getScaledWidth() / 2, mc.getWindow().getScaledHeight() / 2, s, hit, dyn);
    }

    /** Draws the crosshair centered at (cx, cy). */
    public static void draw(DrawContext c, int cx, int cy, CrosshairSettings s, boolean hit, float extra) {
        int col = Gfx.ga(Gfx.argb(hit ? s.hitColor : s.color, s.opacity));
        int out = Gfx.ga(Gfx.argb(0x000000, s.opacity * 0.9f));
        int g = Math.round(s.gap + extra), len = Math.max(1, s.size), t = Math.max(1, s.thickness), ho = t / 2;
        List<int[]> r = new ArrayList<>();
        String st = s.style == null ? "CROSS" : s.style;
        switch (st) {
            case "PLUS" -> {
                r.add(new int[]{cx - ho, cy - len, cx - ho + t, cy + len});
                r.add(new int[]{cx - len, cy - ho, cx + len, cy - ho + t});
            }
            case "X" -> {
                for (int i = 0; i < len; i++) {
                    int o = g + i;
                    r.add(new int[]{cx + o, cy + o, cx + o + t, cy + o + t});
                    r.add(new int[]{cx - o - t, cy + o, cx - o, cy + o + t});
                    r.add(new int[]{cx + o, cy - o - t, cx + o + t, cy - o});
                    r.add(new int[]{cx - o - t, cy - o - t, cx - o, cy - o});
                }
            }
            case "DOT" -> {
                int d = Math.max(2, t);
                r.add(new int[]{cx - d / 2, cy - d / 2, cx - d / 2 + d, cy - d / 2 + d});
            }
            case "CIRCLE" -> {
                int rad = len + g;
                for (int deg = 0; deg < 360; deg += 3) {
                    int x = cx + (int) Math.round(Math.cos(Math.toRadians(deg)) * rad);
                    int y = cy + (int) Math.round(Math.sin(Math.toRadians(deg)) * rad);
                    r.add(new int[]{x - ho, y - ho, x - ho + t, y - ho + t});
                }
            }
            default -> { // CROSS and T
                if (!st.equals("T")) r.add(new int[]{cx - ho, cy - g - len, cx - ho + t, cy - g});
                r.add(new int[]{cx - ho, cy + g, cx - ho + t, cy + g + len});
                r.add(new int[]{cx - g - len, cy - ho, cx - g, cy - ho + t});
                r.add(new int[]{cx + g, cy - ho, cx + g + len, cy - ho + t});
            }
        }
        if (s.dot && !st.equals("DOT")) {
            int d = Math.max(2, t);
            r.add(new int[]{cx - d / 2, cy - d / 2, cx - d / 2 + d, cy - d / 2 + d});
        }
        if (s.outline) for (int[] q : r) c.fill(q[0] - 1, q[1] - 1, q[2] + 1, q[3] + 1, out);
        for (int[] q : r) c.fill(q[0], q[1], q[2], q[3], col);
    }
}
