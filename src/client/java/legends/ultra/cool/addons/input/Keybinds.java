package legends.ultra.cool.addons.input;

import legends.ultra.cool.addons.hud.HudEditorScreen;
import legends.ultra.cool.addons.hud.HudManager;
import legends.ultra.cool.addons.hud.widget.TimerWidget;
import legends.ultra.cool.addons.overlay.ContainerOverlay;
import legends.ultra.cool.addons.util.EntityDebug;
import legends.ultra.cool.addons.util.ItemDebugDump;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import  net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class Keybinds {
    public static KeyBinding OPEN_EDITOR;
    public static final KeyBinding.Category MAIN_CATEGORY = KeyBinding.Category.create(Identifier.of("legends_addon"));

    public static void init() {
        KeyBinding openEditorKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                        "Toggle Editor",
                        InputUtil.Type.KEYSYM,
                        GLFW.GLFW_KEY_RIGHT_SHIFT,
                        MAIN_CATEGORY

                )
        );

        KeyBinding DUMP_MOB_KEY = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                        "Dump Mob",
                        GLFW.GLFW_KEY_K,
                        MAIN_CATEGORY
                ));

        KeyBinding TOGGLE_TIMER = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                        "Toggle Timer",
                        GLFW.GLFW_KEY_X,
                        MAIN_CATEGORY
                ));

        KeyBinding RESET_TIMER = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                        "Reset Timer",
                        GLFW.GLFW_KEY_C,
                        MAIN_CATEGORY
                ));

        KeyBinding INV_DEBUG = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                        "inv debug",
                        GLFW.GLFW_KEY_P,
                        MAIN_CATEGORY
                ));

        KeyBinding DUMP_HOVERED_ITEM = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                        "Dump Hovered Item",
                        GLFW.GLFW_KEY_O,
                        MAIN_CATEGORY
                ));

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            ScreenKeyboardEvents.afterKeyPress(screen).register((scr, key) -> {
                if (!(scr instanceof HandledScreen<?>)) {
                    return;
                }

                if (DUMP_HOVERED_ITEM.matchesKey(key)) {
                    ItemDebugDump.dumpHoveredItem(client);
                }
            });
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            //OPEN EDITOR
            while (openEditorKey.wasPressed()) {
                client.setScreen(new HudEditorScreen());
            }

            //MOB DEBUG
            while (DUMP_MOB_KEY.wasPressed()) {
                dumpLookedAtMob(client);
            }

            //TIMER
            HudManager.getWidgets().forEach(widget -> {
                if (widget instanceof TimerWidget timer && timer.enabled) {
                    while (TOGGLE_TIMER.wasPressed()) {
                        timer.toggleTick();
                    }

                    while (RESET_TIMER.wasPressed()) {
                        timer.reset();
                    }
                }
            });
        });
    }

    private static void dumpLookedAtMob(MinecraftClient client) {
        if (client == null || client.player == null || client.world == null) return;

        if (!(client.crosshairTarget instanceof EntityHitResult hit)) {
            return;
        }

        if (!(hit.getEntity() instanceof LivingEntity mob)) {
            return;
        }

        client.player.sendMessage(Text.literal(EntityDebug.getEntityFullNbt(mob).toString()),false);
    }
}

