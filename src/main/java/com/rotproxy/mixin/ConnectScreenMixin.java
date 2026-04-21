package com.rotproxy.mixin;

import com.rotproxy.proxy.ProxyManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.CookieStorage;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ConnectScreen.class)
public abstract class ConnectScreenMixin {
    @Inject(
            method = "connect(Lnet/minecraft/client/gui/screen/Screen;Lnet/minecraft/client/MinecraftClient;Lnet/minecraft/client/network/ServerAddress;Lnet/minecraft/client/network/ServerInfo;ZLnet/minecraft/client/network/CookieStorage;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void rotproxy$blockUnsafeConnect(Screen screen, MinecraftClient client, ServerAddress address, ServerInfo info, boolean quickPlay, @Nullable CookieStorage cookieStorage, CallbackInfo ci) {
        if (!ProxyManager.shouldBlockServerConnections()) {
            return;
        }

        Text reason = ProxyManager.getKillSwitchReasonText();
        SystemToast.show(client.getToastManager(), SystemToast.Type.PERIODIC_NOTIFICATION, Text.literal("RotProxy Kill Switch"), reason);
        client.setScreen(new DisconnectedScreen(screen, Text.literal("RotProxy Kill Switch"), reason));
        ci.cancel();
    }
}
