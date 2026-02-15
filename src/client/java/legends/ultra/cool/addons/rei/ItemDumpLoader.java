package legends.ultra.cool.addons.rei;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import legends.ultra.cool.addons.LegendsAddon;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryWrapper;

import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ItemDumpLoader {
    private static final Gson GSON = new GsonBuilder().create();
    private static final Type LIST_TYPE = new TypeToken<List<DumpEntry>>() {}.getType();
    private static final String FILE_NAME = "legendsaddon_item_dump.json";
    private static final RegistryWrapper.WrapperLookup FALLBACK_LOOKUP = buildFallbackLookup();

    private ItemDumpLoader() {
    }

    public static List<DumpedItem> loadDumpedItems() {
        return loadItemIndex().items();
    }

    public static ItemIndex loadItemIndex() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return ItemIndex.empty();
        }

        Path path = client.runDirectory.toPath().resolve("config/" + FILE_NAME);
        if (!Files.exists(path)) {
            return ItemIndex.empty();
        }

        List<DumpEntry> entries;
        try (Reader reader = Files.newBufferedReader(path)) {
            entries = GSON.fromJson(reader, LIST_TYPE);
        } catch (Exception e) {
            LegendsAddon.LOGGER.warn("[LegendsAddon] Failed to read item dump file.", e);
            return ItemIndex.empty();
        }

        if (entries == null || entries.isEmpty()) {
            return ItemIndex.empty();
        }

        RegistryWrapper.WrapperLookup registries = resolveRegistries(client);
        DynamicOps<NbtElement> ops = registries.getOps(NbtOps.INSTANCE);
        List<DumpedItem> items = new ArrayList<>();

        for (DumpEntry entry : entries) {
            if (entry == null || entry.nbt == null || entry.nbt.isBlank()) {
                continue;
            }

            try {
                NbtCompound nbt = StringNbtReader.readCompound(entry.nbt);
                DataResult<ItemStack> result = ItemStack.CODEC.parse(ops, nbt);
                result.resultOrPartial(error -> LegendsAddon.LOGGER.warn("[LegendsAddon] Item NBT decode failed: {}", error))
                        .ifPresent(stack -> {
                            String resolvedKey = resolveKey(entry.key, entry.name, items.size());
                            items.add(new DumpedItem(resolvedKey, entry.name, stack));
                        });
            } catch (Exception e) {
                LegendsAddon.LOGGER.warn("[LegendsAddon] Item NBT decode failed.", e);
            }
        }

        return ItemIndex.fromItems(items);
    }

    private static RegistryWrapper.WrapperLookup resolveRegistries(MinecraftClient client) {
        if (client.world != null) {
            return client.world.getRegistryManager();
        }
        if (client.getNetworkHandler() != null) {
            return client.getNetworkHandler().getRegistryManager();
        }
        return FALLBACK_LOOKUP;
    }

    private static RegistryWrapper.WrapperLookup buildFallbackLookup() {
        List<RegistryWrapper.Impl<?>> wrappers = new ArrayList<>();
        for (Registry<?> registry : Registries.REGISTRIES) {
            wrappers.add((RegistryWrapper.Impl<?>) registry);
        }
        return RegistryWrapper.WrapperLookup.of(wrappers.stream());
    }

    private static String resolveKey(String key, String name, int index) {
        String resolved = (key != null && !key.isBlank()) ? key : slugify(name);
        if (resolved == null || resolved.isBlank()) {
            resolved = "item_" + index;
        }
        return resolved;
    }

    private static String slugify(String value) {
        if (value == null) {
            return "";
        }
        String lower = value.trim().toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder(lower.length());
        boolean underscore = false;
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                sb.append(c);
                underscore = false;
            } else if (!underscore && sb.length() > 0) {
                sb.append('_');
                underscore = true;
            }
        }
        int len = sb.length();
        if (len > 0 && sb.charAt(len - 1) == '_') {
            sb.setLength(len - 1);
        }
        return sb.toString();
    }

    private static String normalizeKey(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeName(String value) {
        return slugify(value);
    }

    public static final class ItemIndex {
        private final List<DumpedItem> items;
        private final Map<String, DumpedItem> byKey;
        private final Map<String, DumpedItem> byName;

        private ItemIndex(List<DumpedItem> items, Map<String, DumpedItem> byKey, Map<String, DumpedItem> byName) {
            this.items = items;
            this.byKey = byKey;
            this.byName = byName;
        }

        public static ItemIndex empty() {
            return new ItemIndex(List.of(), Map.of(), Map.of());
        }

        public static ItemIndex fromItems(List<DumpedItem> items) {
            Map<String, DumpedItem> byKey = new HashMap<>();
            Map<String, DumpedItem> byName = new HashMap<>();
            for (DumpedItem item : items) {
                if (item == null) {
                    continue;
                }
                String key = normalizeKey(item.key());
                if (!key.isBlank() && !byKey.containsKey(key)) {
                    byKey.put(key, item);
                }
                String name = normalizeName(item.name());
                if (!name.isBlank() && !byName.containsKey(name)) {
                    byName.put(name, item);
                }
            }
            return new ItemIndex(items, byKey, byName);
        }

        public List<DumpedItem> items() {
            return items;
        }

        public DumpedItem find(String query) {
            if (query == null) {
                return null;
            }
            String keyNormalized = normalizeKey(query);
            DumpedItem byKeyMatch = byKey.get(keyNormalized);
            if (byKeyMatch != null) {
                return byKeyMatch;
            }
            String nameNormalized = normalizeName(query);
            return byName.get(nameNormalized);
        }

        public boolean isCustomStack(ItemStack stack) {
            for (DumpedItem item : items) {
                if (ItemStack.areItemsAndComponentsEqual(item.stack(), stack)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static final class DumpedItem {
        private final String key;
        private final String name;
        private final ItemStack stack;

        public DumpedItem(String key, String name, ItemStack stack) {
            this.key = key;
            this.name = name;
            this.stack = stack;
        }

        public String key() {
            return key;
        }

        public String name() {
            return name;
        }

        public ItemStack stack() {
            return stack;
        }
    }

    private static final class DumpEntry {
        public String key;
        public String name;
        public String nbt;
    }
}
