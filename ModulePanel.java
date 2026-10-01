package com.dexvisual.gui;

import com.dexvisual.config.DexConfig;
import com.dexvisual.config.ModuleSettings;
import com.dexvisual.gui.Widgets.*;
import com.dexvisual.module.HudModule;
import com.dexvisual.module.Modules;

/** Builds the per-module settings widgets (used by the menu and the HUD editor inspector). */
public final class ModulePanel {
    private ModulePanel() {}

    static final String[] ANIMS = {"NONE", "FADE", "SLIDE"};

    public static int animIndex(String a) {
        for (int i = 0; i < ANIMS.length; i++) if (ANIMS[i].equals(a)) return i;
        return 1;
    }

    public static void build(WidgetList l, HudModule m, Runnable rebuild) {
        l.add(new Toggle("Enabled", () -> m.cfg().enabled, v -> Modules.set(m, v)));
        if (m.box) {
            l.add(new Slider("Scale", 0.5f, 3f, () -> m.cfg().scale, v -> m.cfg().scale = v, "%.2fx", false));
            l.add(new Slider("Opacity", 0.1f, 1f, () -> m.cfg().opacity, v -> m.cfg().opacity = v, "%.2f", false));
            if (m.container) {
                l.add(new Toggle("Background", () -> m.cfg().background, v -> m.cfg().background = v));
                l.add(new ColorPick("Background color", () -> m.cfg().bgColor, v -> m.cfg().bgColor = v));
                l.add(new Slider("Background opacity", 0f, 1f, () -> m.cfg().bgOpacity, v -> m.cfg().bgOpacity = v, "%.2f", false));
                l.add(new Toggle("Border", () -> m.cfg().border, v -> m.cfg().border = v));
                l.add(new ColorPick("Border color", () -> m.cfg().borderColor, v -> m.cfg().borderColor = v));
                l.add(new Slider("Border width", 1f, 3f, () -> (float) m.cfg().borderWidth, v -> m.cfg().borderWidth = Math.round(v), "%.0f", true));
                l.add(new Slider("Corner radius", 0f, 10f, () -> (float) m.cfg().radius, v -> m.cfg().radius = Math.round(v), "%.0f", true));
            }
            l.add(new ColorPick("Text color", () -> m.cfg().textColor, v -> m.cfg().textColor = v));
            l.add(new Toggle("Text shadow", () -> m.cfg().shadow, v -> m.cfg().shadow = v));
            l.add(new Select("Animation", ANIMS, () -> animIndex(m.cfg().animation), i -> m.cfg().animation = ANIMS[i]));
            l.add(new Slider("Position X", 0f, 1f, () -> m.cfg().x, v -> m.cfg().x = v, "%.3f", false));
            l.add(new Slider("Position Y", 0f, 1f, () -> m.cfg().y, v -> m.cfg().y = v, "%.3f", false));
        } else if (m.id.equals("damage")) {
            l.add(new ColorPick("Number color", () -> m.cfg().textColor, v -> m.cfg().textColor = v));
        } else if (m.id.equals("scoreboard") || m.id.equals("tablist") || m.id.equals("chat")) {
            l.add(new Slider("Scale", 0.5f, 2f, () -> m.cfg().scale, v -> m.cfg().scale = v, "%.2fx", false));
            l.add(new Slider("Offset X", -300f, 300f, () -> m.cfg().offX, v -> m.cfg().offX = (float) Math.round(v), "%.0f", true));
            l.add(new Slider("Offset Y", -300f, 300f, () -> m.cfg().offY, v -> m.cfg().offY = (float) Math.round(v), "%.0f", true));
        }
        if (m.optionLabel != null) l.add(new Toggle(m.optionLabel, () -> m.cfg().option, v -> m.cfg().option = v));
        l.gap(2);
        l.add(new KeyBind("Toggle key", () -> m.cfg().key, v -> m.cfg().key = v));
        l.add(new Button("Reset this module", 2, () -> {
            ModuleSettings old = m.cfg();
            ModuleSettings d = m.defaults();
            d.enabled = old.enabled;
            d.key = old.key;
            DexConfig.get().modules.put(m.id, d);
            DexConfig.markDirty();
            rebuild.run();
        }));
    }
}
