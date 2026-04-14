package com.rotproxy.gui.widget;

import com.rotproxy.gui.RotProxyTheme;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

public class RotProxyTextFieldWidget extends TextFieldWidget {
    private boolean passwordMode;

    public RotProxyTextFieldWidget(TextRenderer textRenderer, int x, int y, int width, int height, Text text) {
        super(textRenderer, x, y, width, height, text);
        setDrawsBackground(false);
        setEditableColor(RotProxyTheme.TEXT);
        setUneditableColor(RotProxyTheme.TEXT_MUTED);
    }

    public void setPasswordMode(boolean passwordMode) {
        if (passwordMode && !this.passwordMode) {
            addFormatter((text, firstCharacterIndex) -> OrderedText.styledForwardsVisitedString(mask(text, firstCharacterIndex), Style.EMPTY));
        }
        this.passwordMode = passwordMode;
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        RotProxyTheme.drawInset(context, getX(), getY(), getWidth(), getHeight(), isFocused());
        super.renderWidget(context, mouseX, mouseY, delta);
    }

    private static String mask(String text, int firstCharacterIndex) {
        int visibleLength = Math.max(0, text.length() - Math.max(0, firstCharacterIndex));
        return "*".repeat(visibleLength);
    }
}
