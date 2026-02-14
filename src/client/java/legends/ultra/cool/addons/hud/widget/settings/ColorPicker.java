package legends.ultra.cool.addons.hud.widget.settings;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public class ColorPicker {

    // Layout
    private int x, y;
    private final int w;


    // Reads/writes the color (ARGB int)
    private final IntSupplier getColor;
    private final IntConsumer setColor;

    // Internal drag state
    private boolean dragging = false;
    private int draggingChannel = -1; // 0=R,1=G,2=B

    // Slider layout inside modal
    private int sliderX;
    private int sliderY;
    private final int sliderW;
    private final int sliderH = 10;
    private final int sliderGap = 22;

    public ColorPicker(int x, int y, int w, IntSupplier getColor, IntConsumer setColor) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.getColor = getColor;
        this.setColor = setColor;

        this.sliderX = x + 10;
        this.sliderY = y + 36;
        this.sliderW = w - 20;
    }

    public int getHeight() {
        // sliderY is y + 36
        int previewTop = sliderY + sliderGap * 3 + 4;
        int previewBottom = previewTop + 22;
        int hexY = previewBottom + 6 + 9; // roughly one line of text
        return (hexY + 10) - y; // bottom padding
    }

    public int getWidth() {
        return this.w;
    }

    public void setPos(int x, int y) {
        this.x = x;
        this.y = y;

        // recompute slider layout because it depends on x/y
        this.sliderX = x + 10;
        this.sliderY = y + 36;
        // sliderW stays the same (depends on w)
    }

    public void render(DrawContext ctx, int mouseX, int mouseY) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;

        int h = getHeight();

        // Panel background
        ctx.fill(x, y, x + w, y + h, 0xFF111111);
        drawBorder(ctx, x, y, w, h, 0xFFFFFFFF);

        // Title
        ctx.drawText(tr, "Color Picker", x + 8, y + 8, 0xFFFFFF, false);

        int c = getColor.getAsInt();
        int r = (c >> 16) & 0xFF;
        int g = (c >> 8) & 0xFF;
        int b = c & 0xFF;

        drawSlider(ctx, tr, sliderX, sliderY, sliderW, sliderH, r, 0xFFFF5555, "R");
        drawSlider(ctx, tr, sliderX, sliderY + sliderGap, sliderW, sliderH, g, 0xFF55FF55, "G");
        drawSlider(ctx, tr, sliderX, sliderY + sliderGap * 2, sliderW, sliderH, b, 0xFF5555FF, "B");

        // Preview box
        int previewTop = sliderY + sliderGap * 3 + 4;
        int previewBottom = previewTop + 22;
        ctx.fill(sliderX, previewTop, sliderX + sliderW, previewBottom, c);
        drawBorder(ctx, sliderX, previewTop, sliderW, previewBottom - previewTop, 0xFF000000);

        // Hex display (optional)
        String hex = String.format("#%08X", c);
        ctx.drawText(tr, hex, sliderX, previewBottom + 6, 0xCCCCCC, false);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        // Check the 3 slider hitboxes
        for (int i = 0; i < 3; i++) {
            int sy = sliderY + i * sliderGap;
            if (isInside(mouseX, mouseY, sliderX, sy, sliderW, sliderH)) {
                dragging = true;
                draggingChannel = i;
                updateFromMouse(mouseX);
                return true;
            }
        }

        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (!dragging) return false;
        updateFromMouse(mouseX);
        return true;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!dragging) return false;
        dragging = false;
        draggingChannel = -1;
        return true;
    }

    public boolean isDragging() {
        return dragging;
    }

    private void updateFromMouse(double mouseX) {
        int value = clamp((int) ((mouseX - sliderX) / (double) sliderW * 255.0));

        int c = getColor.getAsInt();
        int a = (c >> 24) & 0xFF;
        int r = (c >> 16) & 0xFF;
        int g = (c >> 8) & 0xFF;
        int b = c & 0xFF;

        if (draggingChannel == 0) r = value;
        if (draggingChannel == 1) g = value;
        if (draggingChannel == 2) b = value;

        int out = (a << 24) | (r << 16) | (g << 8) | b;
        setColor.accept(out);
    }

    private static void drawSlider(
            DrawContext ctx,
            TextRenderer tr,
            int x, int y, int w, int h,
            int value,
            int fillColor,
            String label
    ) {
        // Label
        ctx.drawText(tr, label + ": " + value, x, y - 9, 0xFFFFFF, false);

        // Background
        ctx.fill(x, y, x + w, y + h, 0xFF222222);

        // Fill
        int fillW = (int) (w * (value / 255.0));
        ctx.fill(x, y, x + fillW, y + h, fillColor);

        drawBorder(ctx, x, y, w, h, 0xFF000000);
    }

    private static boolean isInside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    private static void drawBorder(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.drawVerticalLine(x, y, y + h, color);
        ctx.drawVerticalLine(x + w, y, y + h, color);
        ctx.drawHorizontalLine(x, x + w, y, color);
        ctx.drawHorizontalLine(x, x + w, y + h, color);
    }
}

