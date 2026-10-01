package com.dexvisual.hud;

import com.dexvisual.config.DexConfig;
import com.dexvisual.config.ModuleSettings;
import com.dexvisual.module.HudModule;
import com.dexvisual.module.Modules;
import com.dexvisual.util.Gfx;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

import java.util.HashMap;
import java.util.Map;

/** Draws all enabled HUD modules (glass panel + content + animation). Also used by the HUD editor. */
public final class HudRenderer {
    private HudRenderer() {}

    private static final Map<String, Float> PROGRESS = new HashMap<>();
    private static long lastNs;

    /** Bounds of a module box in scaled screen px: {x, y, w, h, boxW, boxH(unscaled)} or null if empty. */
    public static int[] box(HudModule m, MinecraftClient mc, boolean preview) {
        int cw = m.width(mc, preview), ch = m.height(mc, preview);
        if (cw <= 0 || ch <= 0) return null;
        ModuleSettings s = m.cfg();
        int pad = m.container ? 4 : 0;
        int bw = cw + pad * 2, bh = ch + pad * 2;
        int sw = mc.getWindow().getScaledWidth(), sh = mc.getWindow().getScaledHeight();
        int w = (int) Math.ceil(bw * s.scale), h = (int) Math.ceil(bh * s.scale);
        int x = MathHelper.clamp(Math.round(s.x * sw), 0, Math.max(0, sw - w));
        int y = MathHelper.clamp(Math.round(s.y * sh), 0, Math.max(0, sh - h));
        return new int[]{x, y, w, h, bw, bh};
    }

    public static void render(DrawContext ctx, MinecraftClient mc, boolean editor) {
        DexConfig cfg = DexConfig.get();
        long now = System.nanoTime();
        float dt = lastNs == 0 ? 0.016f : Math.min(0.1f, (now - lastNs) / 1e9f);
        lastNs = now;

        for (HudModule m : Modules.ALL) {
            if (!m.box) continue;
            ModuleSettings s = m.cfg();
            boolean on = s.enabled && (editor || cfg.hudVisible);
            float p = PROGRESS.getOrDefault(m.id, on ? 1f : 0f);
            float ghost = 1f;
            if (editor) {
                p = 1f;
                ghost = s.enabled ? 1f : 0.28f;
            } else if (!cfg.hudAnim || "NONE".equals(s.animation)) {
                p = on ? 1f : 0f;
            } else {
                float target = on ? 1f : 0f;
                p += (target - p) * Math.min(1f, dt * 12f * cfg.animSpeed);
                if (Math.abs(target - p) < 0.02f) p = target;
            }
            PROGRESS.put(m.id, p);
            if (p <= 0.01f) continue;

            int[] b = box(m, mc, editor);
            if (b == null) continue;
            float slide = "SLIDE".equals(s.animation) && !editor ? (1f - Gfx.ease(p)) * 24f * (s.x < 0.5f ? -1f : 1f) : 0f;
            float a = s.opacity * ghost * ("NONE".equals(s.animation) ? 1f : Gfx.ease(p));
            int pad = m.container ? 4 : 0;

            MatrixStack ms = ctx.getMatrices();
            ms.push();
            ms.translate(b[0] + slide, (float) b[1], 0f);
            ms.scale(s.scale, s.scale, 1f);
            if (m.container) {
                if (s.background) Gfx.rrect(ctx, 0, 0, b[4], b[5], s.radius, Gfx.argb(s.bgColor, s.bgOpacity * a));
                if (s.border) Gfx.rborder(ctx, 0, 0, b[4], b[5], s.radius, s.borderWidth, Gfx.argb(s.borderColor, a));
            }
            ms.translate((float) pad, (float) pad, 0f);
            m.render(ctx, mc, Gfx.argb(s.textColor, a), editor);
            ms.pop();
        }

        if (!editor && cfg.hudVisible) {
            for (HudModule m : Modules.ALL) if (!m.box && m.cfg().enabled) m.renderOverlay(ctx, mc);
        }
    }
}
