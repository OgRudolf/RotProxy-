package com.rotproxy.mixin;

import com.rotproxy.proxy.ProxyManager;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(net.minecraft.client.network.MultiplayerServerListPinger.class)
public abstract class MultiplayerServerListPingerMixin {
    @Inject(
            method = "add(Lnet/minecraft/client/network/ServerInfo;Ljava/lang/Runnable;Ljava/lang/Runnable;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void rotproxy$blockServerPing12110(ServerInfo serverInfo, Runnable saveCallback, Runnable pingCallback, CallbackInfo ci) {
        rotproxy$blockPing(serverInfo, ci);
    }

    @Inject(
            method = "add(Lnet/minecraft/client/network/ServerInfo;Ljava/lang/Runnable;Ljava/lang/Runnable;Lnet/minecraft/network/NetworkingBackend;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void rotproxy$blockServerPing12111(ServerInfo serverInfo, Runnable saveCallback, Runnable pingCallback, @Coerce Object backend, CallbackInfo ci) {
        rotproxy$blockPing(serverInfo, ci);
    }

    private void rotproxy$blockPing(ServerInfo serverInfo, CallbackInfo ci) {
        if (!ProxyManager.shouldBlockServerConnections()) {
            return;
        }

        serverInfo.ping = -1L;
        serverInfo.players = null;
        serverInfo.playerCountLabel = Text.empty();
        serverInfo.playerListSummary = List.of(Text.literal("RotProxy kill switch is blocking multiplayer traffic."));
        serverInfo.label = ProxyManager.getKillSwitchReasonText();
        serverInfo.setStatus(ServerInfo.Status.UNREACHABLE);
        ci.cancel();
    }
}
