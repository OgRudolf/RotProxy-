package com.rotproxy.mixin;

import com.rotproxy.gui.RotProxyConfigScreen;
import com.rotproxy.gui.widget.RotProxyButtonWidget;
import com.rotproxy.proxy.ProxyManager;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiplayerScreen.class)
public abstract class RotProxyMultiplayerScreenMixin extends Screen {
    @Unique
    private RotProxyButtonWidget rotproxy$button;

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
    }

    @Unique
    private Text rotproxy$buildLabel() {
        ProxyManager.Status status = ProxyManager.getStatus();
        return switch (status) {
            case CONNECTED -> Text.literal("RotProxy: Enabled");
            case CONNECTING -> Text.literal("RotProxy: Checking");
            case ERROR -> Text.literal("RotProxy: Error");
            case DISABLED -> Text.literal("RotProxy: Disabled");
        };
    }
}
