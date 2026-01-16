package legends.ultra.cool.addons.events;

import legends.ultra.cool.addons.hud.HudManager;
import legends.ultra.cool.addons.hud.widget.CounterWidget;
import legends.ultra.cool.addons.hud.widget.TimerWidget;
import legends.ultra.cool.addons.util.ChatLookup;
import legends.ultra.cool.addons.util.ScoreboardLookup;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class ClientTickHandler {
    private static int tick;
    private static int resultTicksLeft = 0;


    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            tick++;

            boolean start = ScoreboardLookup.sidebarContains("Corrupt Rift");
            boolean stop  = ChatLookup.consumeExact("stop");
            boolean reset = ChatLookup.consumeExact("reset");
            boolean completed = ChatLookup.consumeExact("completed");
            boolean failed = ChatLookup.consumeExact("corrupt rift failed!you have failed to complete the corrupt riftyou may retry at any time.");



            HudManager.getWidgets().forEach(widget -> {
                if (widget instanceof CounterWidget counter && counter.enabled) {
                    counter.tick();
                }

                if (widget instanceof TimerWidget timer && timer.enabled) {
                    if (reset) timer.reset();
                    timer.tick(start, stop || completed || failed);

                    if (completed) {
                        ChatLookup.setResult("completed");
                        resultTicksLeft = 20 * 5; // 5 seconds
                    } else if (failed) {
                        ChatLookup.setResult("failed");
                        resultTicksLeft = 20 * 5;
                    }

                    if (resultTicksLeft > 0) {
                        resultTicksLeft--;
                        if (resultTicksLeft == 0) {
                            timer.reset();
                        }
                    }

                }
            });
        });
    }

}

