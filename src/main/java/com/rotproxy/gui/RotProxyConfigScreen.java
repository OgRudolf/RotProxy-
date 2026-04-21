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
    private static final int MAX_PANEL_HEIGHT = 452;
    private static final int PANEL_GAP = 20;
    private static final int SECTION_GAP = 22;
    private static final int FIELD_HEIGHT = 20;
    private static final int BUTTON_HEIGHT = 20;

    private final Screen parent;

    private int leftX;
    private int topY;
    private int rightX;
    private int panelHeight;
    private int actionY;
    private int sidebarButtonY;

    private int selectedIndex;
    private ProxyProfile editing;

    private RotProxyTextFieldWidget nameField;
    private RotProxyTextFieldWidget hostField;
    private RotProxyTextFieldWidget portField;
    private RotProxyTextFieldWidget userField;
    private RotProxyTextFieldWidget passField;
    private RotProxyButtonWidget testButton;
    private int typeButtonY;
    private String testFeedback = "Run a proxy test before joining a server.";
    private int testFeedbackColor = RotProxyTheme.TEXT_MUTED;
    private boolean testInProgress;

    public RotProxyConfigScreen(Screen parent) {
        super(Text.literal("RotProxy"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        panelHeight = Math.min(MAX_PANEL_HEIGHT, height - 24);
        leftX = (width - LEFT_PANEL_WIDTH - RIGHT_PANEL_WIDTH - PANEL_GAP) / 2;
        topY = Math.max(12, (height - panelHeight) / 2);
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

        actionY = topY + panelHeight - 64;
        int actionWidth = (fieldWidth - 8) / 2;
        addDrawableChild(new RotProxyButtonWidget(fieldX, actionY, actionWidth, BUTTON_HEIGHT, Text.literal("APPLY"), button -> applyProxy()));
        testButton = addDrawableChild(new RotProxyButtonWidget(fieldX + actionWidth + 8, actionY, actionWidth, BUTTON_HEIGHT, Text.literal("TEST"), button -> testProxy()));
        addDrawableChild(new RotProxyButtonWidget(fieldX, actionY + 26, actionWidth, BUTTON_HEIGHT, Text.literal("CLOSE"), button -> close()));
        addDrawableChild(new RotProxyButtonWidget(fieldX + actionWidth + 8, actionY + 26, actionWidth, BUTTON_HEIGHT, Text.literal("DISABLE"), button -> disableProxy()));

        int sidebarButtonWidth = (RIGHT_PANEL_WIDTH - 28) / 2;
        sidebarButtonY = topY + panelHeight - 64;
        addDrawableChild(new RotProxyButtonWidget(rightX + 10, sidebarButtonY, sidebarButtonWidth, BUTTON_HEIGHT, Text.literal("CLEAR"), button -> clearSelectedProfile()));
        addDrawableChild(new RotProxyButtonWidget(rightX + 18 + sidebarButtonWidth, sidebarButtonY, sidebarButtonWidth, BUTTON_HEIGHT, Text.literal("RESET"), button -> resetAllProfiles()));
        addDrawableChild(new RotProxyButtonWidget(rightX + 10, sidebarButtonY + 26, sidebarButtonWidth, BUTTON_HEIGHT, Text.literal("NEW"), button -> createProfile()));
        addDrawableChild(new RotProxyButtonWidget(rightX + 18 + sidebarButtonWidth, sidebarButtonY + 26, sidebarButtonWidth, BUTTON_HEIGHT, Text.literal("DELETE"), button -> deleteProfile()));
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

        ProxyProfile draft = buildDraftProfile();
        editing.name = draft.name;
        editing.host = draft.host;
        editing.port = draft.port;
        editing.username = draft.username;
        editing.password = draft.password;
        editing.type = draft.type;
    }

    private ProxyProfile buildDraftProfile() {
        ProxyProfile draft = editing == null ? new ProxyProfile("Profile " + (selectedIndex + 1)) : editing.copy();
        draft.name = blankToFallback(nameField.getText(), "Profile " + (selectedIndex + 1));
        draft.host = hostField.getText().trim();
        draft.username = userField.getText().trim();
        draft.password = passField.getText();

        String portValue = portField.getText().trim();
        draft.port = portValue.isEmpty() ? 0 : Integer.parseInt(portValue);
        return draft;
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
            setTestFeedback("Enter a valid numeric port before enabling the proxy.", RotProxyTheme.ERROR);
            return;
        }

        ProxyConfig.setActiveProfileIndex(selectedIndex);
        ProxyConfig.save();
        ProxyManager.applyProxy(editing);
        setTestFeedback("Proxy armed. All multiplayer stays blocked until health turns CONNECTED.", RotProxyTheme.WARNING);
    }

    private void disableProxy() {
        try {
            saveFields();
        } catch (NumberFormatException exception) {
            editing.port = 0;
        }

        ProxyConfig.save();
        ProxyManager.clearProxy();
        setTestFeedback("Proxy disabled. Kill switch is now blocking all multiplayer traffic.", RotProxyTheme.WARNING);
    }

    private void testProxy() {
        if (testInProgress) {
            return;
        }

        ProxyProfile draft;
        try {
            draft = buildDraftProfile();
        } catch (NumberFormatException exception) {
            setTestFeedback("Enter a valid numeric port before testing.", RotProxyTheme.ERROR);
            return;
        }

        if (!draft.isValid()) {
            setTestFeedback("Enter a valid host and port before testing.", RotProxyTheme.ERROR);
            return;
        }

        testInProgress = true;
        if (testButton != null) {
            testButton.active = false;
        }
        setTestFeedback("Testing proxy route...", RotProxyTheme.WARNING);

        ProxyManager.testProfile(draft).whenComplete((result, throwable) -> {
            if (client == null) {
                return;
            }

            client.execute(() -> {
                testInProgress = false;
                if (testButton != null) {
                    testButton.active = true;
                }

                if (throwable != null || result == null || !result.success()) {
                    String error = throwable != null ? throwable.getMessage() : result == null ? "Unknown proxy error" : result.errorMessage();
                    setTestFeedback("Test failed: " + error, RotProxyTheme.ERROR);
                    return;
                }

                String latencyText = result.latencyMs() >= 0 ? result.latencyMs() + " ms" : "--";
                setTestFeedback("OK | Exit IP " + result.ipAddress() + " | " + latencyText, RotProxyTheme.SUCCESS);
            });
        });
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
        setTestFeedback("New profile created. Fill it in and run TEST.", RotProxyTheme.TEXT_MUTED);
    }

    private void clearSelectedProfile() {
        ProxyConfig.clearProfile(selectedIndex);
        loadSelectedProfile();
        syncFieldsFromProfile();
        ProxyConfig.save();
        ProxyManager.clearProxy();
        setTestFeedback("Selected profile cleared. Host, port, username, and password are now blank.", RotProxyTheme.WARNING);
    }

    private void resetAllProfiles() {
        ProxyConfig.resetAllProfiles();
        ProxyManager.clearProxy();
        selectedIndex = 0;
        loadSelectedProfile();
        syncFieldsFromProfile();
        ProxyConfig.save();
        setTestFeedback("All stored profiles were reset to a blank default profile.", RotProxyTheme.WARNING);
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
        setTestFeedback("Profile deleted.", RotProxyTheme.TEXT_MUTED);
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
                setTestFeedback("Loaded profile " + editing.name + ".", RotProxyTheme.TEXT_MUTED);
                return true;
            }
        }

        return super.mouseClicked(click, doubleClick);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, RotProxyTheme.BACKDROP);

        RotProxyTheme.drawPanel(context, leftX, topY, LEFT_PANEL_WIDTH, panelHeight);
        RotProxyTheme.drawPanel(context, rightX, topY, RIGHT_PANEL_WIDTH, panelHeight);

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
            case DISABLED -> RotProxyTheme.WARNING;
        };

        int statusY = actionY - 96;
        context.fill(leftX + 16, statusY - 8, leftX + LEFT_PANEL_WIDTH - 16, statusY - 7, RotProxyTheme.BORDER);
        context.drawTextWithShadow(textRenderer, Text.literal("STATUS: " + status.name()), leftX + 16, statusY, color);
        context.drawTextWithShadow(textRenderer, Text.literal("ACTIVE: " + ProxyManager.getActiveProfileName()), leftX + 16, statusY + 12, RotProxyTheme.TEXT_DIM);
        context.drawTextWithShadow(textRenderer, Text.literal("EXIT IP: " + ProxyManager.getCurrentIp()), leftX + 16, statusY + 24, RotProxyTheme.TEXT_DIM);

        long latency = ProxyManager.getLatencyMs();
        String latencyText = latency >= 0 ? latency + " ms" : "--";
        context.drawTextWithShadow(textRenderer, Text.literal("LATENCY: " + latencyText), leftX + 16, statusY + 36, RotProxyTheme.TEXT_DIM);
        context.drawTextWithShadow(
                textRenderer,
                Text.literal("KILL SWITCH: " + ProxyManager.getKillSwitchStateLabel()),
                leftX + 16,
                statusY + 48,
                ProxyManager.shouldBlockServerConnections() ? RotProxyTheme.WARNING : RotProxyTheme.SUCCESS
        );

        context.drawTextWithShadow(
                textRenderer,
                Text.literal(textRenderer.trimToWidth("HEARTBEAT: " + ProxyManager.getHeartbeatSummary(), LEFT_PANEL_WIDTH - 32)),
                leftX + 16,
                statusY + 60,
                RotProxyTheme.TEXT_MUTED
        );

        String errorText = ProxyManager.getLastError();
        String diagnosticText = !errorText.isBlank() ? "ERROR: " + errorText : "TEST: " + testFeedback;
        int diagnosticColor = !errorText.isBlank() ? RotProxyTheme.ERROR : testFeedbackColor;
        context.drawTextWithShadow(
                textRenderer,
                Text.literal(textRenderer.trimToWidth(diagnosticText, LEFT_PANEL_WIDTH - 32)),
                leftX + 16,
                statusY + 72,
                diagnosticColor
        );
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

    private void setTestFeedback(String message, int color) {
        testFeedback = message;
        testFeedbackColor = color;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
