package com.dexvisual.util;

import com.dexvisual.config.DexConfig;
import com.dexvisual.module.Modules;

import java.util.ArrayList;
import java.util.List;

public final class Notifications {
    private Notifications() {}

    public record Note(String title, String message, long time, int color) {}

    public static final List<Note> NOTES = new ArrayList<>();

    public static void push(String title, String message) {
        push(title, message, 0x6C8CFF);
    }

    public static void push(String title, String message, int color) {
        if (Modules.NOTIFICATIONS == null || !Modules.NOTIFICATIONS.cfg().enabled) return;
        NOTES.add(new Note(title, message, System.currentTimeMillis(), color));
        while (NOTES.size() > 5) NOTES.remove(0);
    }

    public static void tick() {
        long life = DexConfig.get().notifSeconds * 1000L;
        NOTES.removeIf(n -> System.currentTimeMillis() - n.time() > life);
    }
}
