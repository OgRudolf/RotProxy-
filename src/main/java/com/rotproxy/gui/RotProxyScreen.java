package com.rotproxy.gui;

import net.minecraft.client.gui.screen.Screen;

/**
 * Legacy compatibility wrapper retained so older references do not break.
 * The real in-game UI lives in RotProxyConfigScreen.
 */
@Deprecated
public class RotProxyScreen extends RotProxyConfigScreen {
    public RotProxyScreen(Screen parent) {
        super(parent);
    }
}
