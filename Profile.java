package com.dexvisual.config;

import java.util.LinkedHashMap;
import java.util.Map;

/** A named snapshot of every module + the crosshair. Used both as profile and HUD preset. */
public class Profile {
    public String name;
    public Map<String, ModuleSettings> modules = new LinkedHashMap<>();
    public CrosshairSettings crosshair = new CrosshairSettings();

    public Profile() {}

    public Profile(String name) { this.name = name; }
}
