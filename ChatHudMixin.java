package com.dexvisual.mixin;

import com.dexvisual.config.ModuleSettings;
import com.dexvisual.module.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Mixin(ChatHud.class)
public class ChatHudMixin {

    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;IIIZ)V", at = @At("HEAD"), require = 0)
    private void dexvisual$head(DrawContext ctx, int tick, int mx, int my, boolean focused, CallbackInfo ci) {
        ModuleSettings s = Modules.CHAT.cfg();
        if (!s.enabled) return;
        float ax = 0f, ay = MinecraftClient.getInstance().getWindow().getScaledHeight() - 35f;
        ctx.getMatrices().push();
        ctx.getMatrices().translate(ax + s.offX, ay + s.offY, 0f);
        ctx.getMatrices().scale(s.scale, s.scale, 1f);
        ctx.getMatrices().translate(-ax, -ay, 0f);
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;IIIZ)V", at = @At("RETURN"), require = 0)
    private void dexvisual$return(DrawContext ctx, int tick, int mx, int my, boolean focused, CallbackInfo ci) {
        if (Modules.CHAT.cfg().enabled) ctx.getMatrices().pop();
    }

    @ModifyVariable(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At("HEAD"), argsOnly = true, require = 0)
    private Text dexvisual$timestamp(Text message) {
        ModuleSettings s = Modules.CHAT.cfg();
        if (!s.enabled || !s.option) return message;
        return Text.literal("[" + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) + "] ").formatted(Formatting.GRAY).append(message);
    }
}
