package com.rotproxy.mixin;

import com.rotproxy.proxy.ProxyManager;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.proxy.ProxyHandler;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.handler.PacketSizeLogger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.network.ClientConnection.class)
public abstract class ClientConnectionMixin {
    @Inject(
            method = "addHandlers(Lio/netty/channel/ChannelPipeline;Lnet/minecraft/network/NetworkSide;ZLnet/minecraft/network/handler/PacketSizeLogger;)V",
            at = @At("TAIL")
    )
    private static void rotproxy$injectProxyHandler(ChannelPipeline pipeline, NetworkSide side, boolean local, PacketSizeLogger packetSizeLogger, CallbackInfo ci) {
        if (local || side != NetworkSide.CLIENTBOUND || pipeline.get("rotproxy_proxy") != null) {
            return;
        }

        ProxyHandler handler = ProxyManager.createNettyProxyHandler();
        if (handler != null) {
            pipeline.addFirst("rotproxy_proxy", handler);
        }
    }
}
