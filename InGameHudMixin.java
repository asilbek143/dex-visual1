package com.dexvisual.mixin;

import com.dexvisual.config.ModuleSettings;
import com.dexvisual.module.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {

    /** Hide the vanilla crosshair when the DEX VISUAL crosshair is on. */
    @Inject(method = "renderCrosshair(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void dexvisual$crosshair(DrawContext ctx, RenderTickCounter tick, CallbackInfo ci) {
        if (Modules.CROSSHAIR != null && Modules.CROSSHAIR.cfg().enabled) ci.cancel();
    }

    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void dexvisual$sbHead(DrawContext ctx, ScoreboardObjective obj, CallbackInfo ci) {
        ModuleSettings s = Modules.SCOREBOARD.cfg();
        if (!s.enabled) return;
        if (s.option) { ci.cancel(); return; }
        MinecraftClient mc = MinecraftClient.getInstance();
        float ax = mc.getWindow().getScaledWidth(), ay = mc.getWindow().getScaledHeight() / 2f;
        ctx.getMatrices().push();
        ctx.getMatrices().translate(ax + s.offX, ay + s.offY, 0f);
        ctx.getMatrices().scale(s.scale, s.scale, 1f);
        ctx.getMatrices().translate(-ax, -ay, 0f);
    }

    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
            at = @At("RETURN"), require = 0)
    private void dexvisual$sbReturn(DrawContext ctx, ScoreboardObjective obj, CallbackInfo ci) {
        ModuleSettings s = Modules.SCOREBOARD.cfg();
        if (s.enabled && !s.option) ctx.getMatrices().pop();
    }
}
