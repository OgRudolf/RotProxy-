package com.rotproxy.mixin;

import com.rotproxy.gui.RotProxyConfigScreen;
import com.rotproxy.gui.widget.RotProxyButtonWidget;
import com.rotproxy.proxy.ProxyManager;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(MultiplayerScreen.class)
public abstract class RotProxyMultiplayerScreenMixin extends Screen {
    @Shadow
    private ButtonWidget buttonJoin;

    @Shadow
    protected abstract void updateButtonActivationStates();

    @Unique
    private RotProxyButtonWidget rotproxy$button;
    @Unique
    private boolean rotproxy$lastBlockingState;
    @Unique
    private boolean rotproxy$toastShown;

    protected RotProxyMultiplayerScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void rotproxy$addButton(CallbackInfo ci) {
        MultiplayerScreen self = (MultiplayerScreen) (Object) this;
        rotproxy$button = new RotProxyButtonWidget(
                width - 154,
                8,
                146,
                20,
                this::rotproxy$buildLabel,
                button -> {
                    if (client != null) {
                        client.setScreen(new RotProxyConfigScreen(self));
                    }
                }
        );
        addDrawableChild(rotproxy$button);

        if (client != null && ProxyManager.shouldBlockServerConnections() && !rotproxy$toastShown) {
            rotproxy$toastShown = true;
            SystemToast.show(
                    client.getToastManager(),
                    SystemToast.Type.PERIODIC_NOTIFICATION,
                    Text.literal("RotProxy Kill Switch"),
                    Text.literal("Multiplayer traffic is blocked until your proxy becomes healthy.")
            );
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void rotproxy$syncKillSwitchState(CallbackInfo ci) {
        boolean blocking = ProxyManager.shouldBlockServerConnections();
        if (buttonJoin != null && blocking) {
            buttonJoin.active = false;
        }
        if (!blocking && rotproxy$lastBlockingState) {
            updateButtonActivationStates();
        }

        if (blocking == rotproxy$lastBlockingState) {
            return;
        }

        rotproxy$lastBlockingState = blocking;
        if (!blocking) {
            return;
        }

        MultiplayerScreen self = (MultiplayerScreen) (Object) this;
        self.getServerListPinger().cancel();

        ServerList serverList = self.getServerList();
        if (serverList == null) {
            return;
        }

        for (int index = 0; index < serverList.size(); index++) {
            ServerInfo server = serverList.get(index);
            server.ping = -1L;
            server.players = null;
            server.playerCountLabel = Text.empty();
            server.playerListSummary = List.of(Text.literal("RotProxy kill switch is blocking multiplayer traffic."));
            server.label = Text.literal("Blocked by RotProxy until a healthy proxy is active.");
            server.setStatus(ServerInfo.Status.UNREACHABLE);
        }
    }

    @Unique
    private Text rotproxy$buildLabel() {
        ProxyManager.Status status = ProxyManager.getStatus();
        return switch (status) {
            case CONNECTED -> Text.literal("RotProxy: Protected");
            case CONNECTING -> Text.literal("RotProxy: Blocking");
            case ERROR -> Text.literal("RotProxy: Blocked");
            case DISABLED -> Text.literal("RotProxy: Blocked");
        };
    }
}
