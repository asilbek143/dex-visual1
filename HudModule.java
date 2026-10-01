package com.dexvisual.module;

import com.dexvisual.config.DexConfig;
import com.dexvisual.config.ModuleSettings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** Base class of every DEX VISUAL module. Content is drawn at local (0,0); the renderer adds panel, scale & animation. */
public abstract class HudModule {
    public final String id, name, category, description;
    public final float defX, defY;
    /** true = draggable HUD box, false = overlay / settings-only module */
    public final boolean box;
    public final boolean defaultOn;
    public String optionLabel = null;
    /** draw the glass panel (background/border) around the content */
    public boolean container = true;

    protected HudModule(String id, String name, String category, String description, float x, float y, boolean box, boolean defaultOn) {
        this.id = id; this.name = name; this.category = category; this.description = description;
        this.defX = x; this.defY = y; this.box = box; this.defaultOn = defaultOn;
    }

    public HudModule option(String label) { this.optionLabel = label; return this; }
    public HudModule noContainer() { this.container = false; return this; }

    public ModuleSettings cfg() { return DexConfig.get().module(this); }

    public ModuleSettings defaults() {
        ModuleSettings s = new ModuleSettings();
        s.enabled = defaultOn;
        s.x = defX;
        s.y = defY;
        return s;
    }

    /** Content size. {@code preview} = HUD editor / menu: show sample data if live data is empty. */
    public int width(MinecraftClient mc, boolean preview) { return 0; }
    public int height(MinecraftClient mc, boolean preview) { return 0; }

    /** @param color text color with opacity already applied (ARGB) */
    public void render(DrawContext ctx, MinecraftClient mc, int color, boolean preview) {}

    /** Used by modules without a box (damage numbers, crosshair...). */
    public void renderOverlay(DrawContext ctx, MinecraftClient mc) {}

    public void tick(MinecraftClient mc) {}

    public boolean isOn() { return cfg().enabled; }
}
