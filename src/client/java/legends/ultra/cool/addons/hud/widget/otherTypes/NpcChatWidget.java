package legends.ultra.cool.addons.hud.widget.otherTypes;

import legends.ultra.cool.addons.hud.HudWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.List;

public class NpcChatWidget extends HudWidget {
    public static final String WIDGET_NAME = "Better Dialogue";
    private static final String PREFIX = "[NPC]";
    public static NpcChatWidget INSTANCE;

    public NpcChatWidget() {
        super(WIDGET_NAME, 0, 0);
        INSTANCE = this;
    }

    public static boolean isEnabledGlobal() {
        return INSTANCE != null && INSTANCE.isEnabled();
    }

    public static boolean isNpcMessage(Text message) {
        if (message == null) return false;
        String raw = message.getString();
        return raw != null && raw.startsWith(PREFIX);
    }

    public static boolean sendOverlay(Text message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return false;
        }
        System.out.println(message);
        client.player.sendMessage(message, true);
        return true;
    }

    @Override
    public void render(DrawContext context) {}

    @Override
    public int getWidth() {
        return 1;
    }

    @Override
    public int getHeight() {
        return 1;
    }

    @Override
    public List<HudSetting> getSettings() {
        return List.of();
    }
}
