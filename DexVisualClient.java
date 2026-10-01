package com.dexvisual;

import com.dexvisual.config.DexConfig;
import com.dexvisual.config.Waypoint;
import com.dexvisual.gui.HudEditorScreen;
import com.dexvisual.gui.MenuScreen;
import com.dexvisual.hud.HudRenderer;
import com.dexvisual.module.HudModule;
import com.dexvisual.module.Modules;
import com.dexvisual.util.Notifications;
import com.dexvisual.util.Tracker;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

/** DEX VISUAL - client entrypoint. */
public class DexVisualClient implements ClientModInitializer {
    public static final String MOD_ID = "dexvisual";

    private static KeyBinding menuKey, editorKey, hudKey, sprintKey, sneakKey;
    private final Map<String, Boolean> keyDown = new HashMap<>();

    @Override
    public void onInitializeClient() {
        DexConfig.get();

        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.dexvisual.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "key.categories.dexvisual"));
        editorKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.dexvisual.editor", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_CONTROL, "key.categories.dexvisual"));
        hudKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.dexvisual.hud", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, "key.categories.dexvisual"));
        sprintKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.dexvisual.sprint", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "key.categories.dexvisual"));
        sneakKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.dexvisual.sneak", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "key.categories.dexvisual"));

        // HUD rendering
        HudRenderCallback.EVENT.register((ctx, tickCounter) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            Tracker.frame(mc);
            if (mc.options.hudHidden || mc.currentScreen instanceof HudEditorScreen) return;
            HudRenderer.render(ctx, mc, false);
        });

        // combat tracking
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient && entity instanceof LivingEntity le) Tracker.onAttack(le);
            return ActionResult.PASS;
        });

        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(c -> DexConfig.get().save());

        registerCommands();
    }

    private void tick(MinecraftClient mc) {
        Tracker.tick(mc);
        for (HudModule m : Modules.ALL) m.tick(mc);

        while (menuKey.wasPressed()) mc.setScreen(new MenuScreen());
        while (editorKey.wasPressed()) mc.setScreen(new HudEditorScreen(null));
        while (hudKey.wasPressed()) {
            DexConfig c = DexConfig.get();
            c.hudVisible = !c.hudVisible;
            DexConfig.markDirty();
            Notifications.push("HUD", c.hudVisible ? "Visible" : "Hidden", c.hudVisible ? 0x46FFB4 : 0xFF4D6D);
        }
        while (sprintKey.wasPressed()) Modules.toggle(Modules.TOGGLESPRINT);
        while (sneakKey.wasPressed()) Modules.toggle(Modules.TOGGLESNEAK);

        // per-module keybinds (only while no screen is open)
        if (mc.currentScreen == null && mc.player != null) {
            long handle = mc.getWindow().getHandle();
            for (HudModule m : Modules.ALL) {
                int key = m.cfg().key;
                if (key < 0) continue;
                boolean down = InputUtil.isKeyPressed(handle, key);
                boolean was = keyDown.getOrDefault(m.id, false);
                if (down && !was) Modules.toggle(m);
                keyDown.put(m.id, down);
            }
        }
    }

    private void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                ClientCommandManager.literal("dexwp")
                        .then(ClientCommandManager.literal("add")
                                .then(ClientCommandManager.argument("name", StringArgumentType.word()).executes(ctx -> {
                                    MinecraftClient mc = MinecraftClient.getInstance();
                                    if (mc.player == null || mc.world == null) return 0;
                                    String name = StringArgumentType.getString(ctx, "name");
                                    DexConfig.get().addWaypoint(name, mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.world.getRegistryKey().getValue().toString());
                                    Notifications.push("Waypoint added", name, 0x46FFB4);
                                    ctx.getSource().sendFeedback(Text.literal("[DEX VISUAL] Waypoint '" + name + "' added"));
                                    return 1;
                                })))
                        .then(ClientCommandManager.literal("remove")
                                .then(ClientCommandManager.argument("name", StringArgumentType.word()).executes(ctx -> {
                                    String name = StringArgumentType.getString(ctx, "name");
                                    boolean removed = DexConfig.get().waypoints.removeIf(w -> w.name.equalsIgnoreCase(name));
                                    DexConfig.markDirty();
                                    ctx.getSource().sendFeedback(Text.literal("[DEX VISUAL] " + (removed ? "Removed " + name : "No waypoint named " + name)));
                                    return removed ? 1 : 0;
                                })))
                        .then(ClientCommandManager.literal("list").executes(ctx -> {
                            if (DexConfig.get().waypoints.isEmpty()) ctx.getSource().sendFeedback(Text.literal("[DEX VISUAL] No waypoints"));
                            for (Waypoint w : DexConfig.get().waypoints)
                                ctx.getSource().sendFeedback(Text.literal("[DEX VISUAL] " + w.name + " " + Math.round(w.x) + " " + Math.round(w.y) + " " + Math.round(w.z)));
                            return 1;
                        }))));
    }
}
