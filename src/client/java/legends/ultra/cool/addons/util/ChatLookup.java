package legends.ultra.cool.addons.util;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ChatLookup {
    private ChatLookup() {
    }

    private static boolean initialized = false;
    public static String result = "";


    public static String getResult() {
        return result;
    }

    public static void setResult(String value) {
        result = value;
    }


    private static final java.util.Set<String> triggers = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static final java.util.Set<String> fired = java.util.concurrent.ConcurrentHashMap.newKeySet();


    /**
     * Call this before init() to add what you want to detect.
     */
    public static void watchExact(String message) {
        triggers.add(message.toLowerCase());
    }

    public static void init() {
        if (initialized) return;
        initialized = true;

        // Normal chat (what you THINK you're listening to)
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, ts) -> {
            String plain = normalize(message.getString());
            System.out.println("[LEGENDS][CHAT] " + plain);
            matchTriggers(plain);
        });

        // Game messages / overlay (some mods reroute to here)
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            String plain = normalize(message.getString());
            System.out.println("[LEGENDS][GAME overlay=" + overlay + "] " + plain);
            matchTriggers(plain);
        });
    }

    private static void matchTriggers(String plain) {
        for (String t : triggers) {
            if (plain.contains(t)) {
                fired.add(t);
            }
        }
    }

    private static String normalize(String s) {
        // lower + strip weird invisible chars that some chat mods insert
        return s.toLowerCase().replaceAll("\\p{C}", "");
    }


    public static boolean consumeExact(String message) {
        if (message == null) return false;
        return fired.remove(message.toLowerCase());
    }

}
