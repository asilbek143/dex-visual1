package com.dexvisual.mixin;

import com.dexvisual.config.ModuleSettings;
import com.dexvisual.module.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerListHud.class)
public class PlayerListHudMixin {

    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;ILnet/minecraft/scoreboard/Scoreboard;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
            at = @At("HEAD"), require = 0)
    private void dexvisual$head(DrawContext ctx, int width, Scoreboard sb, ScoreboardObjective obj, CallbackInfo ci) {
        ModuleSettings s = Modules.TABLIST.cfg();
        if (!s.enabled) return;
        float ax = MinecraftClient.getInstance().getWindow().getScaledWidth() / 2f, ay = 10f;
        ctx.getMatrices().push();
        ctx.getMatrices().translate(ax + s.offX, ay + s.offY, 0f);
        ctx.getMatrices().scale(s.scale, s.scale, 1f);
        ctx.getMatrices().translate(-ax, -ay, 0f);
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;ILnet/minecraft/scoreboard/Scoreboard;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
            at = @At("RETURN"), require = 0)
    private void dexvisual$return(DrawContext ctx, int width, Scoreboard sb, ScoreboardObjective obj, CallbackInfo ci) {
        if (Modules.TABLIST.cfg().enabled) ctx.getMatrices().pop();
    }
}
