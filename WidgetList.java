package com.dexvisual.gui;

import com.dexvisual.util.Gfx;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

/** Vertically stacked, scrollable list of widgets. */
public class WidgetList {
    public int x, y, w, h;
    public float scroll = 0f;
    public final List<W> items = new ArrayList<>();
    private int cy = 0;
    private W active;
    private static final int PAD = 4;

    public WidgetList(int x, int y, int w, int h) { this.x = x; this.y = y; this.w = w; this.h = h; }

    public <T extends W> T add(T wd) {
        wd.x = PAD; wd.y = cy; wd.w = w - PAD * 2 - 4;
        cy += wd.h + 3;
        items.add(wd);
        return wd;
    }

    public void addRow(W a, W b) {
        int half = (w - PAD * 2 - 4 - 4) / 2;
        a.x = PAD; a.y = cy; a.w = half;
        b.x = PAD + half + 4; b.y = cy; b.w = half;
        cy += Math.max(a.h, b.h) + 3;
        items.add(a);
        items.add(b);
    }

    /** Adds n widgets in one row. */
    public void addRow(W... ws) {
        int n = ws.length, cw = (w - PAD * 2 - 4 - (n - 1) * 4) / n, hh = 0;
        for (int i = 0; i < n; i++) {
            ws[i].x = PAD + i * (cw + 4); ws[i].y = cy; ws[i].w = cw;
            hh = Math.max(hh, ws[i].h);
            items.add(ws[i]);
        }
        cy += hh + 3;
    }

    public void gap(int g) { cy += g; }

    public boolean inside(int mx, int my) { return Gfx.in(mx, my, x, y, w, h); }

    private float minScroll() { return -Math.max(0, cy - h); }

    public void render(DrawContext c, int mx, int my) {
        scroll = MathHelper.clamp(scroll, minScroll(), 0f);
        c.enableScissor(x, y, x + w, y + h);
        MatrixStack ms = c.getMatrices();
        ms.push();
        ms.translate((float) x, (float) (y + (int) scroll), 0f);
        boolean in = inside(mx, my);
        int lx = in ? mx - x : -9999, ly = in ? my - y - (int) scroll : -9999;
        for (W wd : items) {
            if (wd.y + wd.h < -scroll - 2 || wd.y > -scroll + h + 2) continue;
            wd.render(c, lx, ly);
        }
        ms.pop();
        c.disableScissor();
        if (cy > h) {
            float vis = h / (float) cy;
            int bh = Math.max(14, (int) (h * vis));
            int by = y + (int) ((-scroll / (cy - h)) * (h - bh));
            Gfx.rrect(c, x + w - 3, by, 2, bh, 1, 0x44FFFFFF);
        }
    }

    public boolean click(int mx, int my, int btn) {
        if (!inside(mx, my)) return false;
        Widgets.Input.focused = null;
        int lx = mx - x, ly = my - y - (int) scroll;
        for (int i = items.size() - 1; i >= 0; i--) {
            W wd = items.get(i);
            if (wd.hover(lx, ly) && wd.click(lx, ly, btn)) { active = wd; return true; }
        }
        return true;
    }

    public boolean drag(int mx, int my) {
        if (active == null) return false;
        active.drag(mx - x, my - y - (int) scroll);
        return true;
    }

    public void release() {
        if (active != null) active.release();
        active = null;
    }

    public boolean scroll(double mx, double my, double amount) {
        if (!inside((int) mx, (int) my)) return false;
        scroll = MathHelper.clamp(scroll + (float) amount * 16f, minScroll(), 0f);
        return true;
    }
}
