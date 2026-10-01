package com.dexvisual.module;

import com.dexvisual.util.Gfx;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.function.Function;

/** Simple multi-line text module. */
public class TextModule extends HudModule {
    private final Function<MinecraftClient, String> live;
    private final String sample;

    public TextModule(String id, String name, String cat, String desc, float x, float y, String sample, Function<MinecraftClient, String> live) {
        super(id, name, cat, desc, x, y, true, false);
        this.live = live;
        this.sample = sample;
    }

    protected String text(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) return sample;
        try {
            String s = live.apply(mc);
            return s == null || s.isEmpty() ? sample : s;
        } catch (Exception e) {
            return sample;
        }
    }

    @Override public int width(MinecraftClient mc, boolean preview) {
        int w = 0;
        for (String l : text(mc).split("\n")) w = Math.max(w, Gfx.tw(l));
        return w;
    }

    @Override public int height(MinecraftClient mc, boolean preview) {
        return text(mc).split("\n").length * 10 - 1;
    }

    @Override public void render(DrawContext ctx, MinecraftClient mc, int color, boolean preview) {
        String[] lines = text(mc).split("\n");
        boolean sh = cfg().shadow;
        for (int i = 0; i < lines.length; i++) Gfx.text(ctx, lines[i], 0, i * 10, color, sh);
    }
}
