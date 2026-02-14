package legends.ultra.cool.addons.input;

import legends.ultra.cool.addons.hud.HudEditorScreen;
import legends.ultra.cool.addons.hud.HudManager;
import legends.ultra.cool.addons.hud.widget.TimerWidget;
import legends.ultra.cool.addons.overlay.ContainerOverlay;
import legends.ultra.cool.addons.util.EntityDebug;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.LivingEntity;
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

            //INV DEBUG
            while (INV_DEBUG.wasPressed()) {
                ContainerOverlay.init();
            }

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

        EntityDebug.dumpTargetFiltered(mob);
    }
}

