package com.dexvisual.config;

/** Per-module visual settings. Positions are stored as fractions of the scaled screen size. */
public class ModuleSettings {
    public boolean enabled = false;
    public float x = 0.02f, y = 0.02f;
    public float scale = 1f;
    public float opacity = 1f;
    public boolean background = true;
    public int bgColor = 0x0B0D14;
    public float bgOpacity = 0.6f;
    public boolean border = true;
    public int borderColor = 0x6C8CFF;
    public int borderWidth = 1;
    public int radius = 4;
    public int textColor = 0xFFFFFF;
    public boolean shadow = true;
    /** NONE, FADE or SLIDE */
    public String animation = "FADE";
    /** Module specific on/off option (label supplied by the module). */
    public boolean option = false;
    public float offX = 0f, offY = 0f;
    /** GLFW key code, -1 = unbound */
    public int key = -1;

    public ModuleSettings copy() {
        return DexConfig.GSON.fromJson(DexConfig.GSON.toJson(this), ModuleSettings.class);
    }
}
