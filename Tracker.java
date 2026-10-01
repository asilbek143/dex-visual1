package com.dexvisual.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import org.lwjgl.glfw.GLFW;

import java.lang.management.ManagementFactory;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Collects live gameplay / performance statistics used by HUD modules. */
public final class Tracker {
    private Tracker() {}

    public static final long SESSION_START = System.currentTimeMillis();

    public static final class Floating {
        public final String text;
        public final long start;
        public Floating(String text, long start) { this.text = text; this.start = start; }
    }

    // clicks
    private static final ArrayDeque<Long> LEFT = new ArrayDeque<>(), RIGHT = new ArrayDeque<>();
    private static boolean lDown, rDown;
    public static boolean leftHeld, rightHeld;

    // combat
    public static int combo, maxCombo, kills, hits, streak;
    public static LivingEntity target;
    public static long lastAttackMs, lastHitMs;
    public static float lastHealth;
    public static double lastReach;
    public static boolean counted;
    public static long hitFlashUntil;
    public static long previewHitUntil;
    public static final List<Floating> FLOATS = new ArrayList<>();
    private static int prevHurt;

    // movement
    public static double distance;
    private static double lx, lz;
    private static boolean hasLast;

    // performance
    private static final ArrayDeque<Float> FRAMES = new ArrayDeque<>();
    private static long lastFrameNs;
    public static int avgFps, lowFps;
    public static float frameMs;
    public static float cpu = 0f;
    private static int cpuTimer;

    // misc flags
    public static boolean openMenuNext, openEditorNext;
    private static int saveTimer;

    /** Called every rendered frame. */
    public static void frame(MinecraftClient mc) {
        long now = System.nanoTime();
        if (lastFrameNs != 0) {
            float ms = (now - lastFrameNs) / 1_000_000f;
            if (ms < 1000f) {
                FRAMES.addLast(ms);
                if (FRAMES.size() > 400) FRAMES.removeFirst();
                frameMs = frameMs * 0.9f + ms * 0.1f;
            }
        }
        lastFrameNs = now;

        if (mc.currentScreen == null && mc.player != null) {
            long h = mc.getWindow().getHandle();
            boolean l = GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
            boolean r = GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
            long t = System.currentTimeMillis();
            if (l && !lDown) LEFT.addLast(t);
            if (r && !rDown) RIGHT.addLast(t);
            lDown = l;
            rDown = r;
            leftHeld = l;
            rightHeld = r;
        } else {
            lDown = rDown = leftHeld = rightHeld = false;
        }
    }

    private static int count(ArrayDeque<Long> q) {
        long now = System.currentTimeMillis();
        while (!q.isEmpty() && now - q.peekFirst() > 1000) q.removeFirst();
        return q.size();
    }

    public static int cpsLeft() { return count(LEFT); }
    public static int cpsRight() { return count(RIGHT); }

    public static float[] recentFrames(int n) {
        Float[] all = FRAMES.toArray(new Float[0]);
        int from = Math.max(0, all.length - n);
        float[] out = new float[all.length - from];
        for (int i = from; i < all.length; i++) out[i - from] = all[i];
        return out;
    }

    public static void onAttack(LivingEntity e) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        if (target != e) {
            target = e;
            lastHealth = e.getHealth();
            counted = false;
        }
        lastAttackMs = System.currentTimeMillis();
        lastReach = mc.player.distanceTo(e);
    }

    /** Client tick (20 Hz). */
    public static void tick(MinecraftClient mc) {
        Notifications.tick();
        long now = System.currentTimeMillis();
        FLOATS.removeIf(f -> now - f.start > 1200);

        if (++saveTimer >= 100) {
            saveTimer = 0;
            if (com.dexvisual.config.DexConfig.dirty) com.dexvisual.config.DexConfig.get().save();
        }

        // FPS statistics
        if (mc.player != null && mc.world != null && FRAMES.size() > 20) {
            if (mc.player.age % 10 == 0) {
                float[] arr = new float[FRAMES.size()];
                int i = 0;
                float sum = 0;
                for (float f : FRAMES) { arr[i++] = f; sum += f; }
                avgFps = Math.round(1000f / Math.max(0.1f, sum / arr.length));
                Arrays.sort(arr);
                int worst = Math.max(1, arr.length / 100);
                float ws = 0;
                for (int k = 0; k < worst; k++) ws += arr[arr.length - 1 - k];
                lowFps = Math.round(1000f / Math.max(0.1f, ws / worst));
            }
        }
        if (++cpuTimer >= 20) { cpuTimer = 0; cpu = sampleCpu(); }

        if (mc.player == null || mc.world == null) { hasLast = false; return; }

        // distance
        double px = mc.player.getX(), pz = mc.player.getZ();
        if (hasLast) {
            double d = Math.hypot(px - lx, pz - lz);
            if (d < 20) distance += d;
        }
        lx = px; lz = pz; hasLast = true;

        // combat tracking
        LivingEntity t = target;
        if (t != null) {
            float hp = t.getHealth();
            if (!t.isDead() && hp > 0 && hp < lastHealth - 0.01f && now - lastAttackMs < 2500) {
                float d = lastHealth - hp;
                hits++;
                combo++;
                maxCombo = Math.max(maxCombo, combo);
                lastHitMs = now;
                hitFlashUntil = now + 250;
                FLOATS.add(new Floating("-" + String.format("%.1f", d), now));
            }
            lastHealth = hp;
            if (!counted && (t.isDead() || hp <= 0 || t.isRemoved()) && now - lastAttackMs < 5000) {
                counted = true;
                kills++;
                streak++;
                Notifications.push("Kill", "You eliminated " + t.getName().getString(), 0xFF4D6D);
            }
            if (now - lastAttackMs > 8000 && mc.targetedEntity != t) { target = null; }
        }
        if (combo > 0 && now - lastHitMs > 3000) combo = 0;
        if (mc.player.hurtTime > 0 && prevHurt == 0) combo = 0;
        prevHurt = mc.player.hurtTime;
        if (mc.player.isDead()) streak = 0;
    }

    private static float sampleCpu() {
        try {
            Object bean = ManagementFactory.getOperatingSystemMXBean();
            Class<?> cls = Class.forName("com.sun.management.OperatingSystemMXBean");
            if (cls.isInstance(bean)) {
                double v = (double) cls.getMethod("getProcessCpuLoad").invoke(bean);
                if (v >= 0) return (float) (v * 100.0);
            }
        } catch (Throwable ignored) {}
        return 0f;
    }

    public static String sessionTime() {
        long s = (System.currentTimeMillis() - SESSION_START) / 1000;
        return s >= 3600 ? String.format("%dh %02dm %02ds", s / 3600, (s / 60) % 60, s % 60)
                : String.format("%dm %02ds", s / 60, s % 60);
    }

    public static String usedRam() {
        Runtime r = Runtime.getRuntime();
        return String.format("%.1f/%.1f GB", (r.totalMemory() - r.freeMemory()) / 1073741824.0, r.maxMemory() / 1073741824.0);
    }

    public static float ramPercent() {
        Runtime r = Runtime.getRuntime();
        return (r.totalMemory() - r.freeMemory()) / (float) r.maxMemory();
    }
}
