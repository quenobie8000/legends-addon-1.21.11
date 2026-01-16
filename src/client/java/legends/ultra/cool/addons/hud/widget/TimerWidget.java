package legends.ultra.cool.addons.hud.widget;

import legends.ultra.cool.addons.hud.HudWidget;
import legends.ultra.cool.addons.util.ChatLookup;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.text.DecimalFormat;
import java.util.List;

public class TimerWidget extends HudWidget{
    private int ticksElapsed = 0;
    private double value = 0;
    private int ticksPerIncrement = 1; // 1 second
    private boolean start = false;

    public TimerWidget(int x, int y) {
        super("Timer", x, y);
    }

    // Called from ClientTickHandler
    public void tick(boolean startCondition, boolean stopCondition) {

        if (stopCondition) {
            start = false;
            ticksElapsed = 0; // optional, but usually correct
            return;
        }

        if (startCondition) {
            start = true;
        }

        if (!start) return;

        ticksElapsed++;

        if (ticksElapsed >= ticksPerIncrement) {
            value += 0.05;
            ticksElapsed = 0;
        }
    }

    public void reset() {
        ticksElapsed = 0;
        value = 0;
        start = false;
        ChatLookup.setResult("");
    }

    public static int DungeonResultColor(String result) {
        int argb = 0xFFFFFFFF;

        if (ChatLookup.getResult().equalsIgnoreCase("completed")) argb = 0xFF23b000;
        if (ChatLookup.getResult().equalsIgnoreCase("failed")) argb = 0xFFb10000;

        return argb;
    }

    @Override
    public void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        DecimalFormat format = new DecimalFormat("0.00");

        String text = "Stopwatch: " +  String.format("%.2f", value);

        int width = client.textRenderer.getWidth(text);
        int height = client.textRenderer.fontHeight;

        // background
        if (style.drawBackground) {
            context.fill(
                    (int) (x - 3),
                    (int) (y - 3),
                    (int) (x + width + 2),
                    (int) (y + height + 2),
                    style.backgroundColor
            );
        }

        // border
        if (style.drawBorder) {
            context.drawBorder(
                    (int) (x - 3),
                    (int) (y - 3),
                    width + 5,
                    height + 5,
                    style.borderColor
            );
        }

        context.drawText(
                client.textRenderer,
                text,
                (int) x,
                (int) y,
                (ChatLookup.getResult().isEmpty()) ? style.textColor : DungeonResultColor(ChatLookup.getResult()),
                false
        );
    }

    @Override
    public int getWidth() {
        return MinecraftClient.getInstance()
                .textRenderer
                .getWidth("Stopwatch: " + String.format("%.2f", value));
    }

    @Override
    public int getHeight() {
        return MinecraftClient.getInstance()
                .textRenderer
                .fontHeight;
    }

    @Override
    public List<HudWidget.HudSetting> getSettings() {
        return List.of(
                HudSetting.toggle("drawBackground", "Background"),
                HudSetting.color("backgroundColor", "BG Color"),
                HudSetting.toggle("drawBorder", "Border"),
                HudSetting.color("borderColor", "Border Color"),
                HudSetting.color("textColor", "Text Color")
        );
    }
}
