package com.rotproxy.gui;

import net.minecraft.client.gui.DrawContext;

public final class RotProxyTheme {
    public static final int BACKDROP = 0xD40A0606;
    public static final int SURFACE = 0xEE120909;
    public static final int SURFACE_ALT = 0xEE170C0C;
    public static final int FIELD = 0xEE0D0606;
    public static final int FIELD_ACTIVE = 0xEE1B0A0A;
    public static final int BORDER = 0xFF5E1414;
    public static final int BORDER_BRIGHT = 0xFFE33B3B;
    public static final int TEXT = 0xFFF6ECEC;
    public static final int TEXT_DIM = 0xFFC69797;
    public static final int TEXT_MUTED = 0xFF8E5B5B;
    public static final int SUCCESS = 0xFF35D167;
    public static final int WARNING = 0xFFFFC857;
    public static final int ERROR = 0xFFFF5B5B;

    private RotProxyTheme() {
    }

    public static void drawPanel(DrawContext context, int x, int y, int width, int height) {
        context.fill(x, y, x + width, y + height, SURFACE);
        drawOutline(context, x, y, width, height, BORDER);
    }

    public static void drawInset(DrawContext context, int x, int y, int width, int height, boolean focused) {
        context.fill(x, y, x + width, y + height, focused ? FIELD_ACTIVE : FIELD);
        drawOutline(context, x, y, width, height, focused ? BORDER_BRIGHT : BORDER);
    }

    public static void drawOutline(DrawContext context, int x, int y, int width, int height, int color) {
        context.drawHorizontalLine(x, x + width - 1, y, color);
        context.drawHorizontalLine(x, x + width - 1, y + height - 1, color);
        context.drawVerticalLine(x, y, y + height - 1, color);
        context.drawVerticalLine(x + width - 1, y, y + height - 1, color);
    }
}
