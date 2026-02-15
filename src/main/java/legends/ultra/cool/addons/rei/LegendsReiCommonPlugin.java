package legends.ultra.cool.addons.rei;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import legends.ultra.cool.addons.LegendsAddon;
import me.shedaniel.rei.api.common.entry.comparison.EntryComparator;
import me.shedaniel.rei.api.common.entry.comparison.ItemComparatorRegistry;
import me.shedaniel.rei.api.common.plugins.REICommonPlugin;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class LegendsReiCommonPlugin implements REICommonPlugin {
    private static final Gson GSON = new GsonBuilder().create();
    private static final Type LIST_TYPE = new TypeToken<List<DumpEntry>>() {}.getType();
    private static final String ITEM_DUMP_FILE = "legendsaddon_item_dump.json";
    private static final Item[] FALLBACK_ITEMS = {
            Items.IRON_SWORD
    };

    private static final EntryComparator<ItemStack> COMPONENTS_COMPARATOR = EntryComparator.itemComponents();
    private static final EntryComparator<ItemStack> CUSTOM_COMPARATOR = (context, stack) -> {
        long hash = 1L;
        CustomModelDataComponent customModelData = stack.get(DataComponentTypes.CUSTOM_MODEL_DATA);
        if (customModelData != null) {
            hash = 31L * hash + customModelData.hashCode();
        }
        Text customName = stack.get(DataComponentTypes.ITEM_NAME);
        if (customName != null) {
            hash = 31L * hash + customName.getString().hashCode();
        }
        if (hash != 1L) {
            return hash;
        }
        return context.isExact() ? COMPONENTS_COMPARATOR.hash(context, stack) : 1L;
    };

    @Override
    public void registerItemComparators(ItemComparatorRegistry registry) {
        for (Item item : loadCustomBaseItems()) {
            registry.register(CUSTOM_COMPARATOR, item);
        }
    }

    private static List<Item> loadCustomBaseItems() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(ITEM_DUMP_FILE);
        if (!Files.exists(path)) {
            return List.of(FALLBACK_ITEMS);
        }

        List<DumpEntry> entries;
        try (Reader reader = Files.newBufferedReader(path)) {
            entries = GSON.fromJson(reader, LIST_TYPE);
        } catch (Exception e) {
            LegendsAddon.LOGGER.warn("[LegendsAddon] Failed to read item dump for comparators.", e);
            return List.of(FALLBACK_ITEMS);
        }

        if (entries == null || entries.isEmpty()) {
            return List.of(FALLBACK_ITEMS);
        }

        Set<Item> items = new LinkedHashSet<>();
        for (DumpEntry entry : entries) {
            if (entry == null || entry.nbt == null || entry.nbt.isBlank()) {
                continue;
            }
            try {
                NbtCompound nbt = StringNbtReader.readCompound(entry.nbt);
                if (!nbt.contains("id")) {
                    continue;
                }
                String rawId = String.valueOf(nbt.getString("id"));
                if (rawId == null || rawId.isBlank()) {
                    continue;
                }
                Identifier id = rawId.contains(":") ? Identifier.of(rawId) : Identifier.of("minecraft", rawId);
                Item item = Registries.ITEM.get(id);
                if (item != Items.AIR) {
                    items.add(item);
                }
            } catch (Exception e) {
                LegendsAddon.LOGGER.warn("[LegendsAddon] Failed to read item id from dump entry.", e);
            }
        }

        if (items.isEmpty()) {
            return List.of(FALLBACK_ITEMS);
        }
        return new ArrayList<>(items);
    }

    private static final class DumpEntry {
        public String nbt;
    }
}
