package legends.ultra.cool.addons.mixin.client;

import legends.ultra.cool.addons.hud.widget.otherTypes.NpcChatWidget;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public abstract class ChatHudMixin {

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("HEAD"), cancellable = true)
    private void legends$npcChatOverlay(Text message, CallbackInfo ci) {
        if (!NpcChatWidget.isEnabledGlobal()) return;
        if (!NpcChatWidget.isNpcMessage(message)) return;
        if (NpcChatWidget.sendOverlay(message)) {
            ci.cancel();
        }
    }

    @Inject(
            method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void legends$npcChatOverlaySigned(Text message, MessageSignatureData signatureData, MessageIndicator indicator, CallbackInfo ci) {
        if (!NpcChatWidget.isEnabledGlobal()) return;
        if (!NpcChatWidget.isNpcMessage(message)) return;
        if (NpcChatWidget.sendOverlay(message)) {
            ci.cancel();
        }
    }
}
