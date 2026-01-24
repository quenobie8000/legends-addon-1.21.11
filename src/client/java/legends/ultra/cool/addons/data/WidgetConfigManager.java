package legends.ultra.cool.addons.data;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import legends.ultra.cool.addons.hud.HudWidget;
import net.minecraft.client.MinecraftClient;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class WidgetConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "legendsaddon_widgets.json";

    private static Map<String, WidgetData> widgetDataMap = new HashMap<>();

    private static Path getConfigPath() {
        return MinecraftClient.getInstance().runDirectory.toPath().resolve(FILE_NAME);
    }

    public static void load() {
        Path path = getConfigPath();
        if (!Files.exists(path)) return;

        try (Reader reader = Files.newBufferedReader(path)) {
            Type type = new TypeToken<Map<String, WidgetData>>() {}.getType();
            Map<String, WidgetData> loaded = GSON.fromJson(reader, type);
            if (loaded != null) widgetDataMap = loaded;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        Path path = getConfigPath();
        try (Writer writer = Files.newBufferedWriter(path)) {
            GSON.toJson(widgetDataMap, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Ensures we always have a WidgetData entry for this widget id/name. */
    private static WidgetData dataFor(String widgetId) {
        WidgetData d = widgetDataMap.get(widgetId);
        if (d == null) {
            d = new WidgetData();
            widgetDataMap.put(widgetId, d);
        }
        return d;
    }

    // ------------------------------------------------------------
    // Public API used by your HUD system
    // ------------------------------------------------------------

    public static void registerWidget(HudWidget widget) {
        if (widget == null) return;

        String id = widget.getName();
        WidgetData data = dataFor(id);

        data.x = widget.x;
        data.y = widget.y;
        data.enabled = widget.enabled;
    }

    /**
     * Keeps your old call sites working:
     * - saves position/enabled
     * - saves the standard WidgetStyle fields
     * - writes to disk
     */
    public static void updateWidget(HudWidget widget) {
        if (widget == null) return;

        String id = widget.getName();
        WidgetData data = dataFor(id);

        data.x = widget.x;
        data.y = widget.y;
        data.enabled = widget.isEnabled();

        save();
    }

    public static void resetAll() {
        widgetDataMap.clear();
        save();
    }

    public static void clearSetting(String widgetId, String key, boolean autosave) {
        WidgetData d = dataFor(widgetId);
        if (d.settings.remove(key) != null && autosave) save();
    }

    // ------------------------------------------------------------
    // Generic per-widget setting getters/setters
    // These are what your dynamic settings UI should use.
    // ------------------------------------------------------------

    // --- String widgetId overloads (useful in mixins) ---

    public static boolean getBool(String widgetId, String key, boolean def) {
        WidgetData d = dataFor(widgetId);
        JsonElement e = d.settings.get(key);
        if (e == null || !e.isJsonPrimitive()) return def;
        JsonPrimitive p = e.getAsJsonPrimitive();
        try {
            if (p.isBoolean()) return p.getAsBoolean();
            if (p.isNumber()) return p.getAsInt() != 0;
            if (p.isString()) return Boolean.parseBoolean(p.getAsString());
        } catch (Exception ignored) {}
        return def;
    }

    public static int getInt(String widgetId, String key, int def) {
        WidgetData d = dataFor(widgetId);
        JsonElement e = d.settings.get(key);
        if (e == null || !e.isJsonPrimitive()) return def;
        JsonPrimitive p = e.getAsJsonPrimitive();
        try {
            if (p.isNumber()) return p.getAsInt();
            if (p.isString()) return Integer.parseInt(p.getAsString());
            if (p.isBoolean()) return p.getAsBoolean() ? 1 : 0;
        } catch (Exception ignored) {}
        return def;
    }

    public static float getFloat(String widgetId, String key, float def) {
        WidgetData d = dataFor(widgetId);
        JsonElement e = d.settings.get(key);
        if (e == null || !e.isJsonPrimitive()) return def;
        JsonPrimitive p = e.getAsJsonPrimitive();
        try {
            if (p.isNumber()) return p.getAsFloat();
            if (p.isString()) return Float.parseFloat(p.getAsString());
            if (p.isBoolean()) return p.getAsBoolean() ? 1f : 0f;
        } catch (Exception ignored) {}
        return def;
    }

    public static String getString(String widgetId, String key, String def) {
        WidgetData d = dataFor(widgetId);
        JsonElement e = d.settings.get(key);
        if (e == null || !e.isJsonPrimitive()) return def;
        JsonPrimitive p = e.getAsJsonPrimitive();
        try {
            return p.getAsString();
        } catch (Exception ignored) {}
        return def;
    }

    public static void setBool(String widgetId, String key, boolean value, boolean autosave) {
        WidgetData d = dataFor(widgetId);
        d.settings.put(key, new JsonPrimitive(value));
        if (autosave) save();
    }

    public static void setInt(String widgetId, String key, int value, boolean autosave) {
        WidgetData d = dataFor(widgetId);
        d.settings.put(key, new JsonPrimitive(value));
        if (autosave) save();
    }

    public static void setFloat(String widgetId, String key, float value, boolean autosave) {
        WidgetData d = dataFor(widgetId);
        d.settings.put(key, new JsonPrimitive(value));
        if (autosave) save();
    }

    private static void setString(String widgetId, String key, String value, boolean autosave) {
        WidgetData d = dataFor(widgetId);
        d.settings.put(key, new JsonPrimitive(value));
        if (autosave) save();
    }

    // ------------------------------------------------------------
    // Data model
    // ------------------------------------------------------------

    public static class WidgetData {
        public double x;
        public double y;
        public boolean enabled = true;

        /** New: generic settings map */
        public Map<String, JsonElement> settings = new HashMap<>();
    }
}
