package com.dexvisual.config;

import com.dexvisual.module.HudModule;
import com.dexvisual.module.Modules;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class DexConfig {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static DexConfig inst;
    public static volatile boolean dirty = false;

    public Map<String, ModuleSettings> modules = new LinkedHashMap<>();
    public CrosshairSettings crosshair = new CrosshairSettings();
    public List<CrosshairSettings> crosshairPresets = new ArrayList<>();
    public List<Profile> profiles = new ArrayList<>();
    public List<Waypoint> waypoints = new ArrayList<>();
    public String activeProfile = "PvP";

    public boolean hudVisible = true;
    public boolean snap = true;
    public boolean guides = true;
    public int snapDist = 5;
    public boolean menuAnim = true;
    public boolean hudAnim = true;
    public float animSpeed = 1f;
    public int accent = 0;
    public float panelOpacity = 0.85f;
    public int notifSeconds = 4;

    public static DexConfig get() {
        if (inst == null) load();
        return inst;
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("dexvisual.json");
    }

    public static void load() {
        try {
            Path p = path();
            if (Files.exists(p)) inst = GSON.fromJson(Files.readString(p), DexConfig.class);
        } catch (Exception e) {
            System.err.println("[DEX VISUAL] Could not read config, using defaults: " + e);
        }
        if (inst == null) inst = new DexConfig();
        inst.init();
    }

    private void init() {
        if (modules == null) modules = new LinkedHashMap<>();
        if (crosshair == null) crosshair = new CrosshairSettings();
        if (crosshairPresets == null) crosshairPresets = new ArrayList<>();
        if (profiles == null) profiles = new ArrayList<>();
        if (waypoints == null) waypoints = new ArrayList<>();
        if (profiles.isEmpty()) {
            createDefaultProfiles();
            applyProfile("PvP");
        }
    }

    public void save() {
        try {
            syncActive();
            Files.writeString(path(), GSON.toJson(this));
            dirty = false;
        } catch (Exception e) {
            System.err.println("[DEX VISUAL] Could not save config: " + e);
        }
    }

    public static void markDirty() { dirty = true; }

    public ModuleSettings module(HudModule m) {
        return modules.computeIfAbsent(m.id, k -> m.defaults());
    }

    // ---------------------------------------------------------------- profiles
    private void createDefaultProfiles() {
        String[][] sets = {
            {"PvP", "fps,cps,ping,keystrokes,armor,potion,target,combo,reach,damage,hitcolor,crosshair,notifications,togglesprint"},
            {"Survival", "fps,coords,direction,clock,armor,potion,durability,itemcounter,waypoints,memory,notifications,togglesprint,togglesneak"},
            {"BedWars", "fps,cps,ping,armor,potion,kills,combo,itemcounter,target,damage,clock,session,notifications"},
            {"SkyWars", "fps,cps,ping,keystrokes,armor,target,combo,kills,potion,damage,hitcolor,crosshair,notifications"},
            {"Custom", "fps,cps,ping,coords,notifications"}
        };
        for (String[] set : sets) {
            Profile p = new Profile(set[0]);
            List<String> on = Arrays.asList(set[1].split(","));
            for (HudModule m : Modules.ALL) {
                ModuleSettings s = m.defaults();
                s.enabled = on.contains(m.id);
                p.modules.put(m.id, s);
            }
            profiles.add(p);
        }
    }

    public Profile profile(String name) {
        for (Profile p : profiles) if (p.name.equalsIgnoreCase(name)) return p;
        return null;
    }

    public void syncActive() {
        Profile p = profile(activeProfile);
        if (p == null) return;
        p.modules = new LinkedHashMap<>();
        for (Map.Entry<String, ModuleSettings> e : modules.entrySet()) p.modules.put(e.getKey(), e.getValue().copy());
        p.crosshair = crosshair.copy();
    }

    public boolean applyProfile(String name) {
        Profile p = profile(name);
        if (p == null) return false;
        syncActive();
        modules = new LinkedHashMap<>();
        for (Map.Entry<String, ModuleSettings> e : p.modules.entrySet()) modules.put(e.getKey(), e.getValue().copy());
        crosshair = p.crosshair == null ? new CrosshairSettings() : p.crosshair.copy();
        activeProfile = p.name;
        dirty = true;
        return true;
    }

    public Profile createProfile(String name) {
        syncActive();
        Profile p = new Profile(uniqueName(name));
        for (Map.Entry<String, ModuleSettings> e : modules.entrySet()) p.modules.put(e.getKey(), e.getValue().copy());
        p.crosshair = crosshair.copy();
        profiles.add(p);
        dirty = true;
        return p;
    }

    public Profile duplicateProfile(Profile src) {
        if (src.name.equals(activeProfile)) syncActive();
        Profile p = new Profile(uniqueName(src.name + " Copy"));
        for (Map.Entry<String, ModuleSettings> e : src.modules.entrySet()) p.modules.put(e.getKey(), e.getValue().copy());
        p.crosshair = src.crosshair.copy();
        profiles.add(p);
        dirty = true;
        return p;
    }

    public void renameProfile(Profile p, String name) {
        name = name.trim();
        if (name.isEmpty() || (profile(name) != null && profile(name) != p)) return;
        if (p.name.equals(activeProfile)) activeProfile = name;
        p.name = name;
        dirty = true;
    }

    public boolean deleteProfile(Profile p) {
        if (profiles.size() <= 1) return false;
        profiles.remove(p);
        if (p.name.equals(activeProfile)) applyProfile(profiles.get(0).name);
        dirty = true;
        return true;
    }

    public String exportProfile(Profile p) {
        if (p.name.equals(activeProfile)) syncActive();
        return GSON.toJson(p);
    }

    public Profile importProfile(String json) {
        try {
            Profile p = GSON.fromJson(json, Profile.class);
            if (p == null || p.modules == null || p.modules.isEmpty()) return null;
            if (p.name == null || p.name.isBlank()) p.name = "Imported";
            p.name = uniqueName(p.name);
            if (p.crosshair == null) p.crosshair = new CrosshairSettings();
            profiles.add(p);
            dirty = true;
            return p;
        } catch (Exception e) {
            return null;
        }
    }

    public String uniqueName(String base) {
        base = base == null || base.isBlank() ? "Profile" : base.trim();
        String n = base;
        int i = 2;
        while (profile(n) != null) n = base + " " + (i++);
        return n;
    }

    /** Resets every module's look & position to defaults. */
    public void resetModules(boolean keepEnabled) {
        for (HudModule m : Modules.ALL) {
            ModuleSettings old = modules.get(m.id);
            ModuleSettings d = m.defaults();
            d.enabled = keepEnabled && old != null ? old.enabled : m.defaultOn;
            d.key = old != null ? old.key : -1;
            modules.put(m.id, d);
        }
        dirty = true;
    }

    public void resetAll() {
        resetModules(false);
        crosshair = new CrosshairSettings();
        crosshairPresets.clear();
        hudVisible = true; snap = true; guides = true; snapDist = 5; menuAnim = true; hudAnim = true;
        animSpeed = 1f; accent = 0; panelOpacity = 0.85f; notifSeconds = 4;
        applyDefaultEnabled();
        dirty = true;
    }

    private void applyDefaultEnabled() {
        List<String> on = Arrays.asList("fps,cps,ping,keystrokes,armor,potion,target,combo,reach,damage,hitcolor,crosshair,notifications,togglesprint".split(","));
        for (HudModule m : Modules.ALL) module(m).enabled = on.contains(m.id);
    }

    // --------------------------------------------------------------- waypoints
    public Waypoint addWaypoint(String name, double x, double y, double z, String dim) {
        Waypoint w = new Waypoint(name, x, y, z, dim);
        int[] cols = {0x6C8CFF, 0xA06CFF, 0x3FD8FF, 0xFF6CB4, 0x46FFB4, 0xFFC857};
        w.color = cols[waypoints.size() % cols.length];
        waypoints.add(w);
        dirty = true;
        return w;
    }
}
