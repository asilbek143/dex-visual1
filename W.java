package com.dexvisual.gui;

import net.minecraft.client.gui.DrawContext;

/** Base widget of the DEX VISUAL GUI toolkit. Coordinates are local to the owning WidgetList. */
public abstract class W {
    public int x, y, w, h = 18;

    public abstract void render(DrawContext c, int mx, int my);

    public boolean click(int mx, int my, int btn) { return false; }
    public boolean drag(int mx, int my) { return false; }
    public void release() {}

    public boolean hover(int mx, int my) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
