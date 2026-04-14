package com.rotproxy.gui.widget;

import com.rotproxy.gui.RotProxyTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.function.Supplier;

public class RotProxyButtonWidget extends ButtonWidget {
    private final Supplier<Text> labelSupplier;

    public RotProxyButtonWidget(int x, int y, int width, int height, Text message, PressAction onPress) {
        this(x, y, width, height, () -> message, onPress);
    }

    public RotProxyButtonWidget(int x, int y, int width, int height, Supplier<Text> labelSupplier, PressAction onPress) {
        super(x, y, width, height, labelSupplier.get(), onPress, DEFAULT_NARRATION_SUPPLIER);
        this.labelSupplier = labelSupplier;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        setMessage(labelSupplier.get());

        boolean focused = isFocused() || isHovered();
        int background = active
                ? (focused ? 0xF0200D0D : RotProxyTheme.SURFACE_ALT)
                : 0xAA221313;
        int border = active
                ? (focused ? RotProxyTheme.BORDER_BRIGHT : RotProxyTheme.BORDER)
                : 0xFF583838;
        int textColor = active ? RotProxyTheme.TEXT : RotProxyTheme.TEXT_MUTED;

        context.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), background);
        RotProxyTheme.drawOutline(context, getX(), getY(), getWidth(), getHeight(), border);
        context.drawCenteredTextWithShadow(
                MinecraftClient.getInstance().textRenderer,
                getMessage(),
                getX() + getWidth() / 2,
                getY() + (getHeight() - 8) / 2,
                textColor
        );
    }

    @Override
    public void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
}
