package legends.ultra.cool.addons.rei;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import legends.ultra.cool.addons.LegendsAddon;
import net.minecraft.client.MinecraftClient;

import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class TradeDefinitionLoader {
    private static final Gson GSON = new GsonBuilder().create();
    private static final Type LIST_TYPE = new TypeToken<List<TradeDefinition>>() {}.getType();
    private static final String FILE_NAME = "legendsaddon_trades.json";

    private TradeDefinitionLoader() {
    }

    public static List<TradeDefinition> loadDefinitions() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return List.of();
        }

        Path path = client.runDirectory.toPath().resolve("config/" + FILE_NAME);
        if (!Files.exists(path)) {
            return List.of();
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            List<TradeDefinition> definitions = GSON.fromJson(reader, LIST_TYPE);
            return definitions != null ? definitions : List.of();
        } catch (Exception e) {
            LegendsAddon.LOGGER.warn("[LegendsAddon] Failed to read trade definitions.", e);
            return List.of();
        }
    }

    public static final class TradeDefinition {
        public String id;
        public String type;
        public String title;
        public String entity;
        public String entityNbt;
        public String entity_nbt;
        public String shape;
        public Boolean shaped;
        public Integer width;
        public Integer height;
        public List<String> pattern;
        public Map<String, StackRef> key;
        public List<StackRef> grid;
        public StackRef result;
        public List<StackRef> inputs;
        public List<DropRef> drops;
    }

    public static final class StackRef {
        public String item;
        public int count = 1;
    }

    public static final class DropRef {
        public String item;
        public int count = 1;
        public double chance = 1.0;
    }
}
