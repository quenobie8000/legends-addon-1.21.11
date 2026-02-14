package legends.ultra.cool.addons.overlay;

import legends.ultra.cool.addons.mixin.client.HandledScreenAccessor;
import legends.ultra.cool.addons.util.ContainerScan;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.util.Identifier;

public final class ContainerOverlay {

    private ContainerOverlay() {
    }

    private static Identifier texture =
            Identifier.of("legends-addon", "textures/gui/t1_inv.png");


    public static void init() {

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            // Register a per-screen render hook
            ScreenEvents.afterRender(screen).register((scr, ctx, mouseX, mouseY, delta) -> {
                if (!(scr instanceof HandledScreen<?> hs)) return;

                if (!shouldOverlay(hs)) return;

                drawOverlay(hs, ctx);
            });
        });

    }

    public static void setTexture(String texturePath) {
        texture = Identifier.of("legends-addon", texturePath);
    }

    /**
     * Decide which container(s) get the overlay
     */
    private static boolean shouldOverlay(HandledScreen<?> hs) {
        if (!isThatTitle) return false;
        return true;
    }

    static boolean isThatTitle = false;
    public static Boolean isThatTitle(String thatTitle) {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            ScreenEvents.afterRender(screen).register((scr, ctx, mouseX, mouseY, delta) -> {
                if (!(scr instanceof HandledScreen<?> hs)) return;

                isThatTitle = hs.getTitle().getString().toLowerCase().contains(thatTitle);
            });
        });
        return isThatTitle;
    }

    /**
     * Draw a texture on top of the container background area
     */
    private static void drawOverlay(HandledScreen<?> hs, DrawContext ctx) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen instanceof HandledScreen<?> screen) {
            HandledScreenAccessor acc = (HandledScreenAccessor) screen;

            int guiX = acc.legends$getX();
            int guiY = acc.legends$getY();

            // HandledScreen gives you the container's top-left & size
            int w = 160;
            int h = 106;
            int x = guiX + 8;
            int y = guiY + 18;


            // This draws the whole texture stretched to w/h.
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, 0, 0, w, h, w, h);
        }
    }

    public static void fTreeCheck() {
        if (isThatTitle("foraging tree")) {
            if (ContainerScan.containerHasNameAndLore("Tier Five", "100%"))
                setTexture("textures/gui/t5_inv.png");
            else if (ContainerScan.containerHasNameAndLore("Tier Four", "100%"))
                setTexture("textures/gui/t4_inv.png");
            else if (ContainerScan.containerHasNameAndLore("Tier Three", "100%"))
                setTexture("textures/gui/t3_inv.png");
            else if (ContainerScan.containerHasNameAndLore("Tier Two", "100%"))
                setTexture("textures/gui/t2_inv.png");
            else setTexture("textures/gui/t1_inv.png");
        }
    }
}
