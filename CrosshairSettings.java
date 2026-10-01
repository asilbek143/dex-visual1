package com.dexvisual.config;

public class CrosshairSettings {
    public String name = null;
    /** CROSS, PLUS, T, X, DOT, CIRCLE */
    public String style = "CROSS";
    public int size = 6;
    public int thickness = 1;
    public int gap = 3;
    public boolean outline = true;
    public boolean dot = false;
    public boolean dynamic = false;
    public int color = 0x46FFB4;
    public float opacity = 1f;
    public int hitColor = 0xFF4D6D;
    public boolean hitFlash = true;

    public CrosshairSettings copy() {
        return DexConfig.GSON.fromJson(DexConfig.GSON.toJson(this), CrosshairSettings.class);
    }
}
