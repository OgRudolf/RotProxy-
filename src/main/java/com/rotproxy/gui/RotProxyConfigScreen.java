package com.rotproxy.gui;

import com.rotproxy.config.ProxyConfig;
import com.rotproxy.config.ProxyProfile;
import com.rotproxy.gui.widget.RotProxyButtonWidget;
import com.rotproxy.gui.widget.RotProxyTextFieldWidget;
import com.rotproxy.proxy.ProxyManager;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.List;

public class RotProxyConfigScreen extends Screen {
    private static final int LEFT_PANEL_WIDTH = 356;
    private static final int RIGHT_PANEL_WIDTH = 190;
    private static final int PANEL_HEIGHT = 404;
    private static final int PANEL_GAP = 20;
    private static final int SECTION_GAP = 22;
    private static final int FIELD_HEIGHT = 20;
    private static final int BUTTON_HEIGHT = 20;

    private final Screen parent;

    private int leftX;
    private int topY;
    private int rightX;

    private int selectedIndex;
    private ProxyProfile editing;

    private RotProxyTextFieldWidget nameField;
    private RotProxyTextFieldWidget hostField;
    private RotProxyTextFieldWidget portField;
    private RotProxyTextFieldWidget userField;
    private RotProxyTextFieldWidget passField;
    private int typeButtonY;

    public RotProxyConfigScreen(Screen parent) {
        super(Text.literal("RotProxy"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        leftX = (width - LEFT_PANEL_WIDTH - RIGHT_PANEL_WIDTH - PANEL_GAP) / 2;
        topY = (height - PANEL_HEIGHT) / 2;
        rightX = leftX + LEFT_PANEL_WIDTH + PANEL_GAP;

        if (ProxyConfig.getProfiles().isEmpty()) {
            ProxyConfig.addProfile("Profile 1");
        }

        selectedIndex = Math.min(ProxyConfig.getActiveProfileIndex(), ProxyConfig.getProfiles().size() - 1);
        loadSelectedProfile();

        int fieldX = leftX + 16;
        int fieldWidth = LEFT_PANEL_WIDTH - 32;
        int halfWidth = (fieldWidth - 8) / 2;

        int row = topY + 74;
        nameField = createField(fieldX, row, fieldWidth, false, "Profile name", editing.name);
        row += SECTION_GAP + FIELD_HEIGHT;
        hostField = createField(fieldX, row, fieldWidth, false, "Host / IP", editing.host);
        row += SECTION_GAP + FIELD_HEIGHT;
        portField = createField(fieldX, row, fieldWidth, false, "Port", editing.port > 0 ? String.valueOf(editing.port) : "");
        portField.setTextPredicate(value -> value.isEmpty() || value.matches("\\d{0,5}"));
        row += SECTION_GAP + FIELD_HEIGHT;
        userField = createField(fieldX, row, halfWidth, false, "Username (optional)", editing.username);
        passField = createField(fieldX + halfWidth + 8, row, halfWidth, true, "Password (optional)", editing.password);

        addDrawableChild(nameField);
        addDrawableChild(hostField);
        addDrawableChild(portField);
        addDrawableChild(userField);
        addDrawableChild(passField);

        typeButtonY = row + SECTION_GAP + FIELD_HEIGHT;
        addDrawableChild(new RotProxyButtonWidget(
                fieldX,
                typeButtonY,
                fieldWidth,
                BUTTON_HEIGHT,
                () -> Text.literal("TYPE: " + editing.type.name()),
                button -> cycleProxyType()
        ));

        int actionY = topY + PANEL_HEIGHT - 38;
        int actionWidth = (fieldWidth - 8) / 2;
        addDrawableChild(new RotProxyButtonWidget(fieldX, actionY - 26, fieldWidth, BUTTON_HEIGHT, Text.literal("APPLY"), button -> applyProxy()));
        addDrawableChild(new RotProxyButtonWidget(fieldX, actionY, actionWidth, BUTTON_HEIGHT, Text.literal("CLOSE"), button -> close()));
        addDrawableChild(new RotProxyButtonWidget(fieldX + actionWidth + 8, actionY, actionWidth, BUTTON_HEIGHT, Text.literal("DISABLE"), button -> disableProxy()));

        int sidebarButtonWidth = (RIGHT_PANEL_WIDTH - 28) / 2;
        int sidebarButtonY = topY + PANEL_HEIGHT - 38;
        addDrawableChild(new RotProxyButtonWidget(rightX + 10, sidebarButtonY, sidebarButtonWidth, BUTTON_HEIGHT, Text.literal("NEW"), button -> createProfile()));
        addDrawableChild(new RotProxyButtonWidget(rightX + 18 + sidebarButtonWidth, sidebarButtonY, sidebarButtonWidth, BUTTON_HEIGHT, Text.literal("DELETE"), button -> deleteProfile()));
    }

    private RotProxyTextFieldWidget createField(int x, int y, int width, boolean password, String placeholder, String value) {
        RotProxyTextFieldWidget field = new RotProxyTextFieldWidget(textRenderer, x, y, width, FIELD_HEIGHT, Text.empty());
        field.setMaxLength(256);
        field.setPlaceholder(Text.literal(placeholder));
        field.setText(value == null ? "" : value);
        if (password) {
            field.setPasswordMode(true);
        }
        return field;
    }

    private void loadSelectedProfile() {
        selectedIndex = Math.max(0, Math.min(selectedIndex, ProxyConfig.getProfiles().size() - 1));
        editing = ProxyConfig.getProfiles().get(selectedIndex);
    }

    private void saveFields() {
        if (editing == null) {
            return;
        }

        editing.name = blankToFallback(nameField.getText(), "Profile " + (selectedIndex + 1));
        editing.host = hostField.getText().trim();
        editing.username = userField.getText().trim();
        editing.password = passField.getText();

        String portValue = portField.getText().trim();
        editing.port = portValue.isEmpty() ? 0 : Integer.parseInt(portValue);
    }

    private void syncFieldsFromProfile() {
        nameField.setText(editing.name);
        hostField.setText(editing.host);
        portField.setText(editing.port > 0 ? String.valueOf(editing.port) : "");
        userField.setText(editing.username);
        passField.setText(editing.password);
    }

    private void cycleProxyType() {
        ProxyProfile.ProxyType[] values = ProxyProfile.ProxyType.values();
        editing.type = values[(editing.type.ordinal() + 1) % values.length];
    }

    private void applyProxy() {
        try {
            saveFields();
        } catch (NumberFormatException exception) {
            editing.port = 0;
        }

        ProxyConfig.setActiveProfileIndex(selectedIndex);
        ProxyConfig.save();
        ProxyManager.applyProxy(editing);
    }

    private void disableProxy() {
        try {
            saveFields();
        } catch (NumberFormatException exception) {
            editing.port = 0;
        }

        ProxyConfig.save();
        ProxyManager.clearProxy();
    }

    private void createProfile() {
        try {
            saveFields();
        } catch (NumberFormatException exception) {
            editing.port = 0;
        }

        ProxyConfig.addProfile("Profile " + (ProxyConfig.getProfiles().size() + 1));
        selectedIndex = ProxyConfig.getProfiles().size() - 1;
        loadSelectedProfile();
        syncFieldsFromProfile();
        ProxyConfig.save();
    }

    private void deleteProfile() {
        if (ProxyConfig.getProfiles().size() <= 1) {
            return;
        }

        ProxyConfig.removeProfile(selectedIndex);
        selectedIndex = Math.min(selectedIndex, ProxyConfig.getProfiles().size() - 1);
        ProxyConfig.setActiveProfileIndex(Math.min(ProxyConfig.getActiveProfileIndex(), ProxyConfig.getProfiles().size() - 1));
        loadSelectedProfile();
        syncFieldsFromProfile();
        ProxyConfig.save();
    }

    @Override
    public void close() {
        try {
            saveFields();
        } catch (NumberFormatException exception) {
            editing.port = 0;
        }

        ProxyConfig.save();
        if (client != null) {
            client.setScreen(parent);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        double mouseX = click.x();
        double mouseY = click.y();
        int listX = rightX + 10;
        int listY = topY + 52;
        int listWidth = RIGHT_PANEL_WIDTH - 20;

        List<ProxyProfile> profiles = ProxyConfig.getProfiles();
        for (int index = 0; index < profiles.size(); index++) {
            int entryY = listY + index * 26;
            if (mouseX >= listX && mouseX <= listX + listWidth && mouseY >= entryY && mouseY <= entryY + 22) {
                try {
                    saveFields();
                } catch (NumberFormatException exception) {
                    editing.port = 0;
                }

                selectedIndex = index;
                loadSelectedProfile();
                syncFieldsFromProfile();
                return true;
            }
        }

        return super.mouseClicked(click, doubleClick);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, RotProxyTheme.BACKDROP);

        RotProxyTheme.drawPanel(context, leftX, topY, LEFT_PANEL_WIDTH, PANEL_HEIGHT);
        RotProxyTheme.drawPanel(context, rightX, topY, RIGHT_PANEL_WIDTH, PANEL_HEIGHT);

        context.drawCenteredTextWithShadow(textRenderer, Text.literal("ROTPROXY"), leftX + LEFT_PANEL_WIDTH / 2, topY + 16, RotProxyTheme.TEXT);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("STORED PROXIES"), rightX + RIGHT_PANEL_WIDTH / 2, topY + 16, RotProxyTheme.TEXT);

        context.fill(leftX + 16, topY + 40, leftX + LEFT_PANEL_WIDTH - 16, topY + 41, RotProxyTheme.BORDER);
        context.fill(rightX + 10, topY + 40, rightX + RIGHT_PANEL_WIDTH - 10, topY + 41, RotProxyTheme.BORDER);

        drawSectionLabel(context, "PROFILE", nameField.getX(), nameField.getY() - 14);
        drawSectionLabel(context, "HOST", hostField.getX(), hostField.getY() - 14);
        drawSectionLabel(context, "PORT", portField.getX(), portField.getY() - 14);
        drawSectionLabel(context, "USERNAME / PASSWORD", userField.getX(), userField.getY() - 14);
        drawSectionLabel(context, "TYPE", userField.getX(), typeButtonY - 14);

        drawStatusBlock(context);
        drawSidebarEntries(context, mouseX, mouseY);

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawStatusBlock(DrawContext context) {
        ProxyManager.Status status = ProxyManager.getStatus();
        int color = switch (status) {
            case CONNECTED -> RotProxyTheme.SUCCESS;
            case CONNECTING -> RotProxyTheme.WARNING;
            case ERROR -> RotProxyTheme.ERROR;
            case DISABLED -> RotProxyTheme.TEXT_MUTED;
        };

        int statusY = typeButtonY + BUTTON_HEIGHT + 18;
        context.fill(leftX + 16, statusY - 8, leftX + LEFT_PANEL_WIDTH - 16, statusY - 7, RotProxyTheme.BORDER);
        context.drawTextWithShadow(textRenderer, Text.literal("STATUS: " + status.name()), leftX + 16, statusY, color);
        context.drawTextWithShadow(textRenderer, Text.literal("ACTIVE: " + ProxyManager.getActiveProfileName()), leftX + 16, statusY + 12, RotProxyTheme.TEXT_DIM);
        context.drawTextWithShadow(textRenderer, Text.literal("EXIT IP: " + ProxyManager.getCurrentIp()), leftX + 16, statusY + 24, RotProxyTheme.TEXT_DIM);

        long latency = ProxyManager.getLatencyMs();
        String latencyText = latency >= 0 ? latency + " ms" : "--";
        context.drawTextWithShadow(textRenderer, Text.literal("LATENCY: " + latencyText), leftX + 16, statusY + 36, RotProxyTheme.TEXT_DIM);

        String errorText = ProxyManager.getLastError();
        if (!errorText.isBlank()) {
            context.drawTextWithShadow(textRenderer, Text.literal(textRenderer.trimToWidth(errorText, LEFT_PANEL_WIDTH - 32)), leftX + 16, statusY + 48, RotProxyTheme.ERROR);
        }
    }

    private void drawSidebarEntries(DrawContext context, int mouseX, int mouseY) {
        List<ProxyProfile> profiles = ProxyConfig.getProfiles();
        int listX = rightX + 10;
        int listY = topY + 52;
        int listWidth = RIGHT_PANEL_WIDTH - 20;

        for (int index = 0; index < profiles.size(); index++) {
            ProxyProfile profile = profiles.get(index);
            int entryY = listY + index * 26;
            boolean selected = index == selectedIndex;
            boolean activeProfile = index == ProxyConfig.getActiveProfileIndex() && ProxyManager.isEnabled();
            boolean hovered = mouseX >= listX && mouseX <= listX + listWidth && mouseY >= entryY && mouseY <= entryY + 22;

            int background = selected ? 0xFF2B1010 : hovered ? 0xDD1D0D0D : 0xCC140808;
            int border = selected ? RotProxyTheme.BORDER_BRIGHT : RotProxyTheme.BORDER;
            context.fill(listX, entryY, listX + listWidth, entryY + 22, background);
            RotProxyTheme.drawOutline(context, listX, entryY, listWidth, 22, border);
            context.drawTextWithShadow(textRenderer, Text.literal(profile.name), listX + 8, entryY + 6, selected ? RotProxyTheme.TEXT : RotProxyTheme.TEXT_DIM);

            if (activeProfile) {
                context.fill(listX + listWidth - 12, entryY + 8, listX + listWidth - 6, entryY + 14, RotProxyTheme.SUCCESS);
            }
        }
    }

    private void drawSectionLabel(DrawContext context, String text, int x, int y) {
        context.drawTextWithShadow(textRenderer, Text.literal(text), x, y, RotProxyTheme.TEXT_MUTED);
    }

    private static String blankToFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
