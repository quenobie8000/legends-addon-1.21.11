package legends.ultra.cool.addons.hud;

import legends.ultra.cool.addons.data.WidgetConfigManager;
import legends.ultra.cool.addons.hud.widget.settings.ColorPicker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.MouseInput;
import net.minecraft.text.Text;

import javax.naming.Context;
import java.util.List;
import java.util.Optional;

public class HudEditorScreen extends Screen {
    private static final int PANEL_WIDTH = 120;
    private static final int HEADER_HEIGHT = 16;
    private static final int ROW_HEIGHT = 14;
    private static final int MODAL_W = 220;
    private static final int MODAL_H = 180;
    private static final int MODAL_PAD = 8;

    // Settings rows
    private static final int SETTINGS_ROW_H = 16;
    private static final int SETTINGS_ROW_GAP = 6;

    // Reset button
    private static final int RESET_W = 12;
    private static final int RESET_H = 12;

    private HudWidget dragging;
    private double lastMouseX, lastMouseY;
    private boolean panelExpanded = true;

    private HudWidget settingsWidget = null; // null = closed

    // Color picker
    private ColorPicker colorPicker = null;
    private String openColorKey = null; // which setting key opened it

    // Slider drag state
    private String draggingSliderKey = null;

    public HudEditorScreen() {
        super(Text.literal("HUD Editor"));
    }

    private boolean isSettingsOpen() {
        return settingsWidget != null;
    }

    private int panelX() {
        return this.width - PANEL_WIDTH;
    }

    private int modalX() {
        int base = (this.width - MODAL_W) / 2;

        if (colorPicker != null) {
            int pickerW = colorPicker.getWidth();
            int gap = 8;
            return base - (pickerW + gap) / 2;
        }

        return base;
    }

    private int modalY() {
        return (this.height - MODAL_H) / 2;
    }

    private int modalW() {
        return MODAL_W;
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static List<HudWidget.HudSetting> safeSettings(HudWidget w) {
        return Optional.ofNullable(w.getSettings()).orElse(List.of());
    }

    // -----------------------------------
    // Mouse input
    // -----------------------------------

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        int x = panelX();

        // If settings modal is open, handle it first
        if (isSettingsOpen()) {
            if (colorPicker != null && colorPicker.mouseClicked(mouseX, mouseY, button)) return true;
            return handleSettingsClick(mouseX, mouseY, button);
        }

        // Header click (collapse/expand)
        int headerLeft = panelExpanded ? x : x + PANEL_WIDTH - HEADER_HEIGHT;
        int headerRight = x + PANEL_WIDTH;

        if (mouseX >= headerLeft && mouseX <= headerRight
                && mouseY >= 0 && mouseY <= HEADER_HEIGHT) {
            panelExpanded = !panelExpanded;
            return true;
        }

        // If panel is collapsed, only allow dragging widgets on canvas
        if (!panelExpanded) {
            for (HudWidget widget : HudManager.getWidgets()) {
                if (!widget.isEnabled()) continue;
                if (widget.isMouseOver(mouseX, mouseY)) {
                    dragging = widget;
                    lastMouseX = mouseX;
                    lastMouseY = mouseY;
                    return true;
                }
            }
            return super.mouseClicked(new Click(mouseX, mouseY, new MouseInput(button,0)), false);
        }

        // Panel toggle clicks
        int y = HEADER_HEIGHT + 4;
        for (HudWidget widget : HudManager.getWidgets()) {
            int x1 = x + 5;
            int y1 = y;
            int x2 = x + PANEL_WIDTH - ROW_HEIGHT - 5;
            int y2 = y + ROW_HEIGHT;

            // toggle enabled
            if (mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2 && widget != null) {
                widget.toggle();
                WidgetConfigManager.updateWidget(widget);
                return true;
            }

            // settings button
            int sx1 = x + PANEL_WIDTH - ROW_HEIGHT - 5;
            int sy1 = y;
            int sx2 = x + PANEL_WIDTH - 5;
            int sy2 = y + ROW_HEIGHT;

            if (mouseX >= sx1 && mouseX <= sx2 && mouseY >= sy1 && mouseY <= sy2 && widget != null) {
                settingsWidget = widget;
                return true;
            }

            y += ROW_HEIGHT + 2;
        }

        // Dragging selection on canvas (after panel clicks)
        for (HudWidget widget : HudManager.getWidgets()) {
            if (!widget.isEnabled()) continue;
            if (widget.isMouseOver(mouseX, mouseY)) {
                dragging = widget;
                lastMouseX = mouseX;
                lastMouseY = mouseY;
                return true;
            }
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        // Settings modal drag (slider / picker)
        if (isSettingsOpen()) {
            if (colorPicker != null && colorPicker.mouseDragged(mouseX, mouseY, button, offsetX, offsetY)) return true;

            // Slider dragging: only if we started drag on a slider bar
            if (draggingSliderKey != null) {
                SettingsLayout l = beginSettingsLayout();

                for (HudWidget.HudSetting s : safeSettings(settingsWidget)) {
                    int rowY = nextRowY(l);

                    if (s.type() != HudWidget.HudSetting.Type.SLIDER) continue;
                    if (!s.key().equals(draggingSliderKey)) continue;
                    if (!s.enabled().getAsBoolean()) continue;

                    int barX = l.sliderBarX;
                    int barW = l.sliderBarW;
                    int barY = rowY - 1;
                    int barH = 12;

                    float t = (float) ((mouseX - barX) / (double) barW);
                    t = Math.max(0f, Math.min(1f, t));
                    float raw = s.min() + t * (s.max() - s.min());

                    float snapped = (s.step() > 0f)
                            ? (Math.round(raw / s.step()) * s.step())
                            : raw;

                    snapped = Math.max(s.min(), Math.min(s.max(), snapped));
                    s.setFloat().accept(snapped);
                    WidgetConfigManager.updateWidget(settingsWidget);
                    return true;
                }
            }

            return false;
        }

        // Normal canvas dragging
        if (dragging != null) {
            dragging.x += offsetX;
            dragging.y += offsetY;

            dragging.x = Math.max(0, Math.min(dragging.x, this.width - dragging.getWidth()));
            dragging.y = Math.max(0, Math.min(dragging.y, this.height - dragging.getHeight()));
            return true;
        }

        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        if (isSettingsOpen()) {
            draggingSliderKey = null;

            if (colorPicker != null && colorPicker.mouseReleased(mouseX, mouseY, button)) {
                WidgetConfigManager.updateWidget(settingsWidget);
                return true;
            }
            return true;
        }

        if (dragging != null) {
            WidgetConfigManager.updateWidget(dragging);
            dragging = null;
            return true;
        }

        return super.mouseReleased(click);
    }

    // -----------------------------------
    // Settings modal: input logic
    // -----------------------------------

    private void openColorPicker(HudWidget.HudSetting s, int px, int py, int pw) {
        if (openColorKey != null && openColorKey.equals(s.key())) {
            openColorKey = null;
            colorPicker = null;
            return;
        }

        openColorKey = s.key();
        colorPicker = new ColorPicker(
                px, py, pw,
                () -> s.getColor().getAsInt(),
                c -> {
                    s.setColor().accept(c);
                    WidgetConfigManager.updateWidget(settingsWidget);
                }
        );
    }

    private boolean handleSettingsClick(double mouseX, double mouseY, int button) {
        int x = modalX();
        int y = modalY();
        int w = modalW();

        // Close "✕"
        String close = "✕";
        int closeX = x + w - 14;
        int closeY = y + 6;
        int cw = textRenderer.getWidth(close);
        int ch = textRenderer.fontHeight;

        if (inside(mouseX, mouseY, closeX, closeY, cw, ch)) {
            WidgetConfigManager.updateWidget(settingsWidget);
            settingsWidget = null;
            colorPicker = null;
            openColorKey = null;
            draggingSliderKey = null;
            return true;
        }

        SettingsLayout l = beginSettingsLayout();

        for (HudWidget.HudSetting s : safeSettings(settingsWidget)) {
            int rowY = nextRowY(l);

            // Reset click
            if (inside(mouseX, mouseY, l.resetX, rowY - 1, RESET_W, RESET_H)) {
                WidgetConfigManager.clearSetting(settingsWidget.getName(), s.key(), true);

                // apply default back into the widget fields by invoking getter+setter once
                switch (s.type()) {
                    case TOGGLE -> s.setBool().accept(s.getBool().getAsBoolean());
                    case COLOR -> s.setColor().accept(s.getColor().getAsInt());
                    case SLIDER -> s.setFloat().accept((float) s.getFloat().getAsDouble());
                }

                if (openColorKey != null && openColorKey.equals(s.key())) {
                    openColorKey = null;
                    colorPicker = null;
                }
                if (draggingSliderKey != null && draggingSliderKey.equals(s.key())) {
                    draggingSliderKey = null;
                }

                WidgetConfigManager.updateWidget(settingsWidget);
                return true;
            }

            switch (s.type()) {
                case TOGGLE -> {
                    int pillY = rowY - 2;
                    if (s.enabled().getAsBoolean() && inside(mouseX, mouseY, l.toggleX, pillY, l.toggleW, l.btnH)) {
                        boolean newVal = !s.getBool().getAsBoolean();
                        s.setBool().accept(newVal);
                        WidgetConfigManager.updateWidget(settingsWidget);

                        // If we turned off the setting that owns the open color picker, close it.
                        if (!newVal && openColorKey != null) {
                            if (openColorKey.equals("bgColor") || openColorKey.equals("brdColor") || openColorKey.equals("textColor")) {
                                // we don't know which toggle controls which color globally,
                                // but closing is harmless + avoids stuck pickers.
                                openColorKey = null;
                                colorPicker = null;
                            }
                        }
                        return true;
                    }
                }

                case COLOR -> {
                    if (!s.enabled().getAsBoolean()) break;

                    int btnY = rowY - 2;
                    if (inside(mouseX, mouseY, l.btnX, btnY, l.btnW, l.btnH)) {
                        // open picker to the right of modal by default
                        int gap = 6;
                        int pickerW = 180;

                        int px = modalX() + modalW() + gap;
                        int py = modalY();

                        if (px + pickerW > this.width) {
                            px = modalX() - pickerW - gap;
                        }

                        openColorPicker(s, px, py, pickerW);
                        return true;
                    }
                }

                case SLIDER -> {
                    if (!s.enabled().getAsBoolean()) break;

                    int barX = l.sliderBarX;
                    int barY = rowY - 1;
                    int barW = l.sliderBarW;
                    int barH = 12;

                    if (inside(mouseX, mouseY, barX, barY, barW, barH)) {
                        draggingSliderKey = s.key();

                        float t = (float) ((mouseX - barX) / (double) barW);
                        t = Math.max(0f, Math.min(1f, t));
                        float raw = s.min() + t * (s.max() - s.min());

                        float snapped = (s.step() > 0f)
                                ? (Math.round(raw / s.step()) * s.step())
                                : raw;

                        snapped = Math.max(s.min(), Math.min(s.max(), snapped));
                        s.setFloat().accept(snapped);
                        WidgetConfigManager.updateWidget(settingsWidget);
                        return true;
                    }
                }
            }
        }

        return true;
    }

    // -----------------------------------
    // Rendering
    // -----------------------------------

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        for (HudWidget widget : HudManager.getWidgets()) {
            if (widget.isEnabled()) widget.render(context);
        }

        renderWidgetList(context);
        renderPanelHeader(context);
        renderSettingsModal(context, mouseX, mouseY);
    }

    private void renderPanelHeader(DrawContext context) {
        int x = panelX();
        int y = 0;

        context.fill(
                panelExpanded ? x : x + PANEL_WIDTH - HEADER_HEIGHT, y,
                x + PANEL_WIDTH, HEADER_HEIGHT,
                0xCC000000
        );

        String arrow = panelExpanded ? "▶" : "☰";

        context.drawText(
                MinecraftClient.getInstance().textRenderer,
                panelExpanded ? arrow + " Widgets" : arrow,
                panelExpanded ? x + 6 : x + PANEL_WIDTH - HEADER_HEIGHT + 6,
                y + 4,
                0xFFFFFF,
                false
        );
    }

    private void renderWidgetList(DrawContext context) {
        if (!panelExpanded) return;

        int x = panelX();
        int y = HEADER_HEIGHT + 4;

        context.fill(
                x,
                HEADER_HEIGHT,
                x + PANEL_WIDTH,
                this.height,
                0xAA000000
        );

        for (HudWidget widget : HudManager.getWidgets()) {
            int bgColor = widget.isEnabled() ? 0xFF2ECC71 : 0xFF7F8C8D;

            context.fill(
                    x + 5, y,
                    x + PANEL_WIDTH - 5, y + ROW_HEIGHT,
                    bgColor
            );

            context.drawText(
                    MinecraftClient.getInstance().textRenderer,
                    widget.getName(),
                    x + 8,
                    y + 3,
                    0x000000,
                    false
            );

            // settings button
            context.fill(
                    x + PANEL_WIDTH - ROW_HEIGHT - 5, y,
                    x + PANEL_WIDTH - 5, y + ROW_HEIGHT,
                    0xFF7F8C8D
            );

            context.drawText(
                    MinecraftClient.getInstance().textRenderer,
                    "⚙",
                    x + PANEL_WIDTH - ROW_HEIGHT - 1,
                    y + 3,
                    0xFFFFFF,
                    false
            );

            y += ROW_HEIGHT + 2;
        }
    }

    private void renderSettingsModal(DrawContext ctx, int mouseX, int mouseY) {
        if (settingsWidget == null) return;

        int x = modalX();
        int y = modalY();
        int w = modalW();
        int gap = 6;

        // dim background
        ctx.fill(0, 0, this.width, this.height, 0x88000000);

        // modal
        ctx.fill(x, y, x + w, y + MODAL_H, 0xFF111111);
        drawBorder(ctx, x, y, w, MODAL_H, 0xFFFFFFF);

        // title
        ctx.drawText(textRenderer,
                settingsWidget.getName() + " Settings",
                x + 8, y + 8,
                0xFFFFFF, false
        );

        // close button
        String close = "✕";
        int closeX = x + w - 14;
        int closeY = y + 6;
        ctx.drawText(textRenderer, close, closeX, closeY, 0xFFFFFF, false);

        SettingsLayout l = beginSettingsLayout();

        for (HudWidget.HudSetting s : safeSettings(settingsWidget)) {
            int rowY = nextRowY(l);

            // reset button per row
            drawResetButton(ctx, l.resetX, rowY - 1, mouseX, mouseY);

            switch (s.type()) {
                case TOGGLE -> {
                    boolean enabled = s.enabled().getAsBoolean();
                    boolean val = s.getBool().getAsBoolean();

                    ctx.drawText(textRenderer, s.label(), l.startX, rowY, enabled ? 0xFFFFFF : 0x777777, false);
                    drawTogglePill(ctx, l.toggleX, rowY - 2, l.toggleW, l.btnH, val, enabled);
                }

                case COLOR -> {
                    boolean enabled = s.enabled().getAsBoolean();
                    int argb = s.getColor().getAsInt();

                    ctx.drawText(textRenderer, s.label(), l.startX, rowY, enabled ? 0xFFFFFF : 0x777777, false);

                    if (enabled) {
                        drawPickButton(ctx, l.btnX, rowY - 2, l.btnW, l.btnH, mouseX, mouseY);
                        drawSwatch(ctx, l.btnX - 18, rowY - 1, argb);
                    } else {
                        ctx.drawText(textRenderer, "-", l.btnX + 26, rowY, 0x777777, false);
                    }
                }

                case SLIDER -> {
                    boolean enabled = s.enabled().getAsBoolean();
                    float val = (float) s.getFloat().getAsDouble();

                    ctx.drawText(textRenderer, s.label(), l.startX, rowY, enabled ? 0xFFFFFF : 0x777777, false);
                    drawSliderRow(ctx, l, rowY, mouseX, mouseY, val, s.min(), s.max(), s.step(), enabled);
                }
            }
        }

        // Render picker (if open)
        if (colorPicker != null) {
            int pickerW = colorPicker.getWidth();

            int px = modalX() + w + gap;
            int py = modalY();

            if (px + pickerW > this.width) {
                px = modalX() - pickerW - gap;
            }

            colorPicker.setPos(px, py);
            colorPicker.render(ctx, mouseX, mouseY);
        }
    }

    // -----------------------------------
    // Drawing helpers
    // -----------------------------------

    private void drawPickButton(DrawContext ctx, int x, int y, int w, int h, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        int bg = hovered ? 0xFF444444 : 0xFF333333;
        ctx.fill(x, y, x + w, y + h, bg);
        drawBorder(ctx, x, y, w, h, 0xFF000000);
        ctx.drawText(textRenderer, "Pick", x + 18, y + 3, 0xFFFFFF, false);
    }

    private void drawSwatch(DrawContext ctx, int x, int y, int color) {
        ctx.fill(x, y, x + 12, y + 12, color);
        drawBorder(ctx, x, y, 12, 12, 0xFF000000);

    }

    private void drawTogglePill(DrawContext ctx, int x, int y, int w, int h, boolean on, boolean enabled) {
        int bg = on ? 0xFF2ECC71 : 0xFF7F8C8D;
        if (!enabled) bg = 0xFF444444;
        ctx.fill(x, y, x + w, y + h, bg);
        drawBorder(ctx, x, y, w, h, 0xFF000000);
        ctx.drawText(textRenderer, on ? "ON" : "OFF", x + 10, y + 3, 0xFF000000, false);
    }

    private void drawResetButton(DrawContext ctx, int x, int y, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX <= x + RESET_W && mouseY >= y && mouseY <= y + RESET_H;
        int bg = hovered ? 0xFF555555 : 0xFF333333;
        ctx.fill(x, y, x + RESET_W, y + RESET_H, bg);
        drawBorder(ctx, x, y, RESET_W, RESET_H, 0xFF000000);
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(x + 3, y - 4);
        ctx.getMatrices().scale(2f, 2f);
        ctx.drawText(textRenderer, "↺", 0, 0, 0xFFFFFF, false);
        ctx.getMatrices().popMatrix();
    }

    private void drawSliderRow(DrawContext ctx, SettingsLayout l, int rowY, int mouseX, int mouseY,
                               float value, float min, float max, float step, boolean enabled) {
        int barX = l.sliderBarX;
        int barY = rowY - 1;
        int barW = l.sliderBarW;
        int barH = 12;

        int bg = enabled ? 0xFF2A2A2A : 0xFF1F1F1F;
        int border = 0xFF000000;

        ctx.fill(barX, barY, barX + barW, barY + barH, bg);
        drawBorder(ctx, barX, barY, barW, barH, border);

        float t = (max == min) ? 0f : (value - min) / (max - min);
        t = Math.max(0f, Math.min(1f, t));

        int knobX = barX + (int) (t * (barW - 4));
        ctx.fill(knobX, barY, knobX + 4, barY + barH, enabled ? 0xFF7F8C8D : 0xFF444444);

        String v = String.format("%.2f", value);
        ctx.drawText(textRenderer, v,
                barX + (barW / 2) - (textRenderer.getWidth(v) / 2),
                rowY + 1,
                enabled ? 0xAAAAAA : 0x777777,
                false
        );
    }

    // -----------------------------------
    // Settings layout (row positions)
    // -----------------------------------

    private int settingsRowIndex = 0;

    private static final class SettingsLayout {
        final int startX, startY;
        final int btnW, btnH, btnX;
        final int toggleW, toggleX;
        final int resetX;
        final int sliderBarX, sliderBarW;

        SettingsLayout(int startX, int startY,
                       int btnW, int btnH, int btnX,
                       int toggleW, int toggleX,
                       int resetX,
                       int sliderBarX, int sliderBarW) {
            this.startX = startX;
            this.startY = startY;
            this.btnW = btnW;
            this.btnH = btnH;
            this.btnX = btnX;
            this.toggleW = toggleW;
            this.toggleX = toggleX;
            this.resetX = resetX;
            this.sliderBarX = sliderBarX;
            this.sliderBarW = sliderBarW;
        }
    }

    private SettingsLayout beginSettingsLayout() {
        settingsRowIndex = 0;

        int x = modalX();
        int y = modalY();
        int w = modalW();

        int startX = x + MODAL_PAD;
        int startY = y + 28;

        int btnW = 60;
        int btnH = 14;

        int rightEdge = x + w - MODAL_PAD;

        int resetX = rightEdge - RESET_W;
        int controlsRight = resetX - 4;

        int btnX = controlsRight - btnW;

        int toggleW = 42;
        int toggleX = controlsRight - toggleW;

        // slider lives left of reset button too
        int sliderBarW = 120;
        int sliderBarX = controlsRight - sliderBarW;

        return new SettingsLayout(
                startX, startY,
                btnW, btnH, btnX,
                toggleW, toggleX,
                resetX,
                sliderBarX, sliderBarW
        );
    }

    private int nextRowY(SettingsLayout l) {
        int rowY = l.startY + settingsRowIndex * (SETTINGS_ROW_H + SETTINGS_ROW_GAP);
        settingsRowIndex++;
        return rowY;
    }

    private void drawBorder(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.drawVerticalLine(x, y, y + h, color);
        ctx.drawVerticalLine(x + w, y, y + h, color);
        ctx.drawHorizontalLine(x, x + w, y, color);
        ctx.drawHorizontalLine(x, x + w, y + h, color);
    }
}