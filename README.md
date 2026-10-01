# DEX VISUAL

Premium visual & performance client mod for **Minecraft Java 1.21.4**, built for **Fabric** (Loader + Fabric API, Java 21, Yarn mappings 1.21.4+build.8).

## Build the jar

**GitHub (easiest):** push this folder to a repo -> *Actions* tab -> "Build DEX VISUAL" -> download the artifact `DEX-VISUAL-1.21.4` (contains `dexvisual-1.0.0.jar`; do NOT use the `-sources` jar).

**Local:** JDK 21 installed, then

    ./gradlew build        (Windows: gradlew.bat build)

Jar: `build/libs/dexvisual-1.0.0.jar` -> put in `.minecraft/mods` together with Fabric API.

## Controls
| Key | Action |
|---|---|
| Right Shift | DEX VISUAL menu |
| Right Ctrl | HUD editor |
| H | show / hide HUD |
| `/dexwp add <name>` | add waypoint (also `remove`, `list`) |

All keys are rebindable in Options > Controls > DEX VISUAL; every module also has its own toggle key in the menu.

## Features
Custom HUD, HUD editor (drag, resize, scale, colors, opacity, background, border, radius, animation, presets), FPS / CPS / Ping / Coordinates / Direction / Keystrokes / Armor / Potions / Toggle Sprint & Sneak / Crosshair editor / Target HUD / Damage indicator / Hit color / Combo / Kill counter / Reach / Item counter / Durability / Clock / Session stats / Waypoints / Notifications / Custom scoreboard, tablist & chat / Performance monitor / FPS+RAM info, animations, keybind system, profiles (PvP, Survival, BedWars, SkyWars, Custom: create, rename, copy, delete, export/import via clipboard).

Config: `.minecraft/config/dexvisual.json`

## Project layout
- `com.dexvisual.DexVisualClient` - entrypoint, keybinds, events, commands
- `config/` - config, profiles, per-module settings
- `module/` - all modules (`Modules.java` is the registry)
- `hud/` - HUD renderer, crosshair renderer
- `gui/` - menu, HUD editor, widget toolkit
- `mixin/` - crosshair / scoreboard / tablist / chat hooks (all `require = 0`, safe if a target changes)
