package com.dexvisual.gui;

import com.dexvisual.config.DexConfig;

public final class Theme {
    private Theme() {}

    private static final int[] A1 = {0xFF6C8CFF, 0xFFA06CFF, 0xFF3FD8FF, 0xFFFF6CB4};
    private static final int[] A2 = {0xFFA06CFF, 0xFF6C8CFF, 0xFF6C8CFF, 0xFFA06CFF};
    public static final String[] ACCENT_NAMES = {"Blue", "Purple", "Cyan", "Pink"};

    public static final int TEXT = 0xFFE8EBF5;
    public static final int MUTED = 0xFF8A90A6;
    public static final int HOVER = 0x1EFFFFFF;
    public static final int LINE = 0x22FFFFFF;
    public static final int TRACK = 0xFF2A2E3D;

    public static int a1() { return A1[Math.floorMod(DexConfig.get().accent, A1.length)]; }
    public static int a2() { return A2[Math.floorMod(DexConfig.get().accent, A2.length)]; }
    public static int rgb(int argb) { return argb & 0xFFFFFF; }
}
