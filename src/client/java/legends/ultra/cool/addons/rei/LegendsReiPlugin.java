package legends.ultra.cool.addons.rei;

import dev.architectury.event.EventResult;
import legends.ultra.cool.addons.LegendsAddon;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.registry.entry.EntryRegistry;
import me.shedaniel.rei.api.client.view.ViewSearchBuilder;
import me.shedaniel.rei.api.client.view.Views;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.plugin.client.displays.ClientsidedCraftingDisplay;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public class LegendsReiPlugin implements REIClientPlugin {
    private ItemDumpLoader.ItemIndex itemIndex;
    private List<TradeDefinitionLoader.TradeDefinition> tradeDefinitions;
    private final Set<Display> customDisplays = new HashSet<>();

    private final List<ItemStack> tradeInputs = List.of(
            new ItemStack(Items.EMERALD, 12),
            new ItemStack(Items.IRON_INGOT, 4),
            new ItemStack(Items.DIAMOND, 1),
            new ItemStack(Items.STICK, 2)
    );

    public ItemStack addFake(Item Type, String customName, float customItemModel) {
        ItemStack item = new ItemStack(Type);
        item.set(DataComponentTypes.ITEM_NAME, Text.literal(customName));
        item.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(List.of(customItemModel),null, null, null));

        return item;
    }

    private ItemDumpLoader.ItemIndex getItemIndex() {
        if (itemIndex == null) {
            itemIndex = ItemDumpLoader.loadItemIndex();
            if (itemIndex.items().isEmpty()) {
                ItemDumpLoader.DumpedItem fallback = new ItemDumpLoader.DumpedItem(
                        "vampiric_dagger",
                        "Vampiric Dagger",
                        addFake(Items.IRON_SWORD, "Vampiric Dagger", 3)
                );
                itemIndex = ItemDumpLoader.ItemIndex.fromItems(List.of(fallback));
            }
        }
        return itemIndex;
    }

    private List<ItemDumpLoader.DumpedItem> getDumpedItems() {
        return getItemIndex().items();
    }

    private List<TradeDefinitionLoader.TradeDefinition> getTradeDefinitions() {
        if (tradeDefinitions == null) {
            tradeDefinitions = TradeDefinitionLoader.loadDefinitions();
        }
        return tradeDefinitions;
    }

    @Override
    public void registerEntries(EntryRegistry registry) {
        registry.removeEntryIf(entry ->
            entry.getType().equals(VanillaEntryTypes.ITEM) || entry.getType().equals(VanillaEntryTypes.FLUID)
        );

        for (ItemDumpLoader.DumpedItem dumped : getDumpedItems()) {
            registry.addEntries(EntryStacks.of(dumped.stack()));
        }
    }

    @Override
    public void registerCategories(CategoryRegistry registry) {
        ItemDumpLoader.DumpedItem iconItem = getDumpedItems().get(0);
        registry.add(new LegendsReiCategory(
                EntryStacks.of(iconItem.stack()), // icon
                Text.literal("Trade")
        ));
        registry.add(new LegendsReiDropCategory(
                EntryStacks.of(iconItem.stack()),
                Text.literal("Drops")
        ));
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        customDisplays.clear();
        registry.registerVisibilityPredicate((category, display) -> {
            if (customDisplays.contains(display)) {
                return EventResult.pass();
            }
            ViewSearchBuilder context = Views.getInstance().getContext();
            boolean isCustomCategory = category.getCategoryIdentifier().getIdentifier().getNamespace()
                    .equals(LegendsAddon.MOD_ID);
            if (context != null && isCustomSearch(context)) {
                return isCustomCategory ? EventResult.pass() : EventResult.interruptFalse();
            }
            if (isCustomCategory) {
                return EventResult.pass();
            }
            return hasCustomItem(display) ? EventResult.interruptFalse() : EventResult.pass();
        });

        List<TradeDefinitionLoader.TradeDefinition> definitions = getTradeDefinitions();
        if (definitions.isEmpty()) {
            for (ItemDumpLoader.DumpedItem dumped : getDumpedItems()) {
                String title = (dumped.name() != null && !dumped.name().isBlank())
                        ? dumped.name()
                        : dumped.stack().getName().getString();
                Display display = LegendsReiDisplay.trade(dumped.stack(), tradeInputs, title);
                customDisplays.add(display);
                registry.add(display);
            }
            return;
        }

        ItemDumpLoader.ItemIndex index = getItemIndex();
        for (TradeDefinitionLoader.TradeDefinition definition : definitions) {
            if (definition == null) {
                continue;
            }

            if (isDropType(definition.type)) {
                Display dropDisplay = createDropDisplay(definition, index);
                if (dropDisplay != null) {
                    customDisplays.add(dropDisplay);
                    registry.add(dropDisplay);
                }
                continue;
            }

            if (definition.result == null) {
                continue;
            }

            ItemStack output = resolveStack(definition.result, index);
            if (output.isEmpty()) {
                continue;
            }

            List<ItemStack> inputs = new ArrayList<>();
            if (definition.inputs != null) {
                for (TradeDefinitionLoader.StackRef ref : definition.inputs) {
                    ItemStack input = resolveStack(ref, index);
                    if (!input.isEmpty()) {
                        inputs.add(input);
                    }
                }
            }

            if (isCraftType(definition.type)) {
                Display display = createCraftDisplay(definition, output, inputs, index);
                customDisplays.add(display);
                registry.add(display);
            } else {
                String title = buildTitle(definition, output);
                Display display = LegendsReiDisplay.trade(output, inputs, title);
                customDisplays.add(display);
                registry.add(display);
            }
        }
    }

    private boolean isCustomSearch(ViewSearchBuilder context) {
        return containsCustomEntry(context.getRecipesFor()) || containsCustomEntry(context.getUsagesFor());
    }

    private boolean containsCustomEntry(List<EntryStack<?>> stacks) {
        for (EntryStack<?> entry : stacks) {
            if (isCustomItem(entry)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasCustomItem(Display display) {
        return ingredientHasCustomItem(display.getInputEntries()) || ingredientHasCustomItem(display.getOutputEntries());
    }

    private boolean ingredientHasCustomItem(List<EntryIngredient> ingredients) {
        for (EntryIngredient ingredient : ingredients) {
            for (EntryStack<?> entry : ingredient) {
                if (isCustomItem(entry)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int normalizeCount(int count) {
        return count > 0 ? count : 1;
    }

    private static boolean isDropType(String type) {
        if (type == null) {
            return false;
        }
        String normalized = type.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("drop") || normalized.equals("drops") || normalized.equals("mob_drop");
    }

    private static boolean isCraftType(String type) {
        if (type == null) {
            return false;
        }
        String normalized = type.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("craft") || normalized.equals("crafting");
    }

    private Display createCraftDisplay(
            TradeDefinitionLoader.TradeDefinition definition,
            ItemStack output,
            List<ItemStack> inputs,
            ItemDumpLoader.ItemIndex index
    ) {
        ShapedCraftData shaped = buildShapedCraft(definition, index);
        if (shaped != null) {
            List<EntryIngredient> outputEntries = List.of(EntryIngredients.of(output));
            return new ClientsidedCraftingDisplay.Shaped(
                    shaped.inputs(),
                    outputEntries,
                    Optional.empty(),
                    shaped.width(),
                    shaped.height()
            );
        }
        return createShapelessDisplay(output, inputs);
    }

    private Display createShapelessDisplay(ItemStack output, List<ItemStack> inputs) {
        List<EntryIngredient> inputEntries = new ArrayList<>(inputs.size());
        for (ItemStack input : inputs) {
            inputEntries.add(EntryIngredients.of(input));
        }
        List<EntryIngredient> outputEntries = List.of(EntryIngredients.of(output));
        return new ClientsidedCraftingDisplay.Shapeless(inputEntries, outputEntries, Optional.empty());
    }

    private ShapedCraftData buildShapedCraft(TradeDefinitionLoader.TradeDefinition definition, ItemDumpLoader.ItemIndex index) {
        if (!isShapedCraft(definition)) {
            return null;
        }
        ShapedCraftData fromPattern = buildShapedFromPattern(definition, index);
        if (fromPattern != null) {
            return fromPattern;
        }
        ShapedCraftData fromGrid = buildShapedFromGrid(definition, index);
        if (fromGrid != null) {
            return fromGrid;
        }
        if (definition.inputs != null && !definition.inputs.isEmpty()) {
            LegendsAddon.LOGGER.warn("[LegendsAddon] Shaped craft missing pattern/grid for {}", definition.id);
        }
        return null;
    }

    private boolean isShapedCraft(TradeDefinitionLoader.TradeDefinition definition) {
        if (definition.shaped != null) {
            return definition.shaped;
        }
        if (definition.shape != null && !definition.shape.isBlank()) {
            String normalized = definition.shape.trim().toLowerCase(Locale.ROOT);
            if (normalized.equals("shaped")) {
                return true;
            }
            if (normalized.equals("shapeless")) {
                return false;
            }
        }
        return definition.pattern != null || definition.grid != null;
    }

    private ShapedCraftData buildShapedFromPattern(
            TradeDefinitionLoader.TradeDefinition definition,
            ItemDumpLoader.ItemIndex index
    ) {
        if (definition.pattern == null || definition.pattern.isEmpty()) {
            return null;
        }
        int height = definition.pattern.size();
        int width = 0;
        for (String row : definition.pattern) {
            if (row != null) {
                width = Math.max(width, row.length());
            }
        }
        if (width <= 0 || height <= 0) {
            return null;
        }

        List<EntryIngredient> inputs = new ArrayList<>(width * height);
        for (int row = 0; row < height; row++) {
            String line = definition.pattern.get(row);
            for (int col = 0; col < width; col++) {
                char symbol = (line != null && col < line.length()) ? line.charAt(col) : ' ';
                if (symbol == ' ') {
                    inputs.add(EntryIngredient.empty());
                    continue;
                }
                TradeDefinitionLoader.StackRef ref = null;
                if (definition.key != null) {
                    ref = definition.key.get(String.valueOf(symbol));
                }
                if (ref == null) {
                    LegendsAddon.LOGGER.warn("[LegendsAddon] Missing key '{}' in shaped craft {}", symbol, definition.id);
                    inputs.add(EntryIngredient.empty());
                    continue;
                }
                ItemStack stack = resolveStack(ref, index);
                inputs.add(stack.isEmpty() ? EntryIngredient.empty() : EntryIngredients.of(stack));
            }
        }
        return new ShapedCraftData(width, height, inputs);
    }

    private ShapedCraftData buildShapedFromGrid(
            TradeDefinitionLoader.TradeDefinition definition,
            ItemDumpLoader.ItemIndex index
    ) {
        if (definition.grid == null || definition.grid.isEmpty()) {
            return null;
        }
        if (definition.width == null || definition.height == null) {
            LegendsAddon.LOGGER.warn("[LegendsAddon] Shaped craft grid missing width/height for {}", definition.id);
            return null;
        }
        int width = definition.width;
        int height = definition.height;
        if (width <= 0 || height <= 0) {
            return null;
        }

        int total = width * height;
        List<EntryIngredient> inputs = new ArrayList<>(total);
        for (int i = 0; i < total; i++) {
            TradeDefinitionLoader.StackRef ref = i < definition.grid.size() ? definition.grid.get(i) : null;
            if (ref == null || ref.item == null || ref.item.isBlank()) {
                inputs.add(EntryIngredient.empty());
                continue;
            }
            ItemStack stack = resolveStack(ref, index);
            inputs.add(stack.isEmpty() ? EntryIngredient.empty() : EntryIngredients.of(stack));
        }
        return new ShapedCraftData(width, height, inputs);
    }

    private Display createDropDisplay(TradeDefinitionLoader.TradeDefinition definition, ItemDumpLoader.ItemIndex index) {
        Identifier entityId = resolveEntityId(definition.entity);
        if (entityId == null) {
            LegendsAddon.LOGGER.warn("[LegendsAddon] Drop definition missing entity id: {}", definition.id);
            return null;
        }

        if (!Registries.ENTITY_TYPE.containsId(entityId)) {
            LegendsAddon.LOGGER.warn("[LegendsAddon] Unknown entity id: {}", entityId);
            return null;
        }
        EntityType<?> entityType = Registries.ENTITY_TYPE.get(entityId);

        NbtCompound entityNbt = resolveEntityNbt(definition, entityId);
        List<LegendsReiDropDisplay.DropEntry> drops = resolveDrops(definition, index);
        String title = buildDropTitle(definition, entityType);
        return LegendsReiDropDisplay.create(entityId, entityType, title, drops, entityNbt);
    }

    private List<LegendsReiDropDisplay.DropEntry> resolveDrops(
            TradeDefinitionLoader.TradeDefinition definition,
            ItemDumpLoader.ItemIndex index
    ) {
        List<LegendsReiDropDisplay.DropEntry> drops = new ArrayList<>();
        if (definition.drops != null) {
            for (TradeDefinitionLoader.DropRef ref : definition.drops) {
                ItemStack stack = resolveStack(ref, index);
                if (!stack.isEmpty()) {
                    drops.add(new LegendsReiDropDisplay.DropEntry(stack, normalizeChance(ref.chance)));
                }
            }
            return drops;
        }

        if (definition.inputs != null) {
            for (TradeDefinitionLoader.StackRef ref : definition.inputs) {
                ItemStack stack = resolveStack(ref, index);
                if (!stack.isEmpty()) {
                    drops.add(new LegendsReiDropDisplay.DropEntry(stack, 1.0));
                }
            }
        }
        return drops;
    }

    private static double normalizeChance(double chance) {
        double normalized = chance > 1.0 ? chance / 100.0 : chance;
        if (normalized < 0.0) {
            return 0.0;
        }
        if (normalized > 1.0) {
            return 1.0;
        }
        return normalized;
    }

    private static Identifier resolveEntityId(String entityId) {
        if (entityId == null || entityId.isBlank()) {
            return null;
        }
        String raw = entityId.trim();
        try {
            return raw.contains(":") ? Identifier.of(raw) : Identifier.of("minecraft", raw);
        } catch (Exception e) {
            LegendsAddon.LOGGER.warn("[LegendsAddon] Invalid entity id: {}", entityId);
            return null;
        }
    }

    private static String buildDropTitle(TradeDefinitionLoader.TradeDefinition definition, EntityType<?> entityType) {
        String title = (definition.title != null && !definition.title.isBlank())
                ? definition.title
                : definition.id;
        if (title == null || title.isBlank()) {
            title = entityType.getName().getString();
        }
        return title;
    }

    private static NbtCompound resolveEntityNbt(TradeDefinitionLoader.TradeDefinition definition, Identifier entityId) {
        String raw = resolveEntityNbtString(definition);
        if (raw == null) {
            return null;
        }
        try {
            NbtCompound nbt = StringNbtReader.readCompound(raw);
            if (!nbt.contains("id")) {
                nbt.putString("id", entityId.toString());
            }
            return nbt;
        } catch (Exception e) {
            LegendsAddon.LOGGER.warn("[LegendsAddon] Failed to parse entity NBT for {}", entityId, e);
            return null;
        }
    }

    private static String resolveEntityNbtString(TradeDefinitionLoader.TradeDefinition definition) {
        if (definition.entityNbt != null && !definition.entityNbt.isBlank()) {
            return definition.entityNbt;
        }
        if (definition.entity_nbt != null && !definition.entity_nbt.isBlank()) {
            return definition.entity_nbt;
        }
        return null;
    }

    private static String buildTitle(TradeDefinitionLoader.TradeDefinition definition, ItemStack output) {
        String title = (definition.title != null && !definition.title.isBlank())
                ? definition.title
                : definition.id;
        if (title == null || title.isBlank()) {
            title = output.getName().getString();
        }
        if (definition.type != null && !definition.type.isBlank() && !definition.type.equalsIgnoreCase("trade")) {
            title = title + " (" + definition.type + ")";
        }
        return title;
    }

    private ItemStack resolveStack(TradeDefinitionLoader.StackRef ref, ItemDumpLoader.ItemIndex index) {
        if (ref == null || ref.item == null || ref.item.isBlank()) {
            return ItemStack.EMPTY;
        }

        int count = normalizeCount(ref.count);
        String itemId = ref.item.trim();
        if (itemId.contains(":")) {
            try {
                Identifier id = Identifier.of(itemId);
                Item item = Registries.ITEM.get(id);
                ItemStack stack = new ItemStack(item);
                stack.setCount(count);
                return stack;
            } catch (Exception e) {
                LegendsAddon.LOGGER.warn("[LegendsAddon] Unknown item id: {}", itemId);
                return ItemStack.EMPTY;
            }
        }

        ItemDumpLoader.DumpedItem custom = index.find(itemId);
        if (custom == null) {
            LegendsAddon.LOGGER.warn("[LegendsAddon] Unknown custom item: {}", itemId);
            return ItemStack.EMPTY;
        }

        ItemStack stack = custom.stack().copy();
        stack.setCount(count);
        return stack;
    }

    private ItemStack resolveStack(TradeDefinitionLoader.DropRef ref, ItemDumpLoader.ItemIndex index) {
        if (ref == null || ref.item == null || ref.item.isBlank()) {
            return ItemStack.EMPTY;
        }
        TradeDefinitionLoader.StackRef stackRef = new TradeDefinitionLoader.StackRef();
        stackRef.item = ref.item;
        stackRef.count = ref.count;
        return resolveStack(stackRef, index);
    }

    private boolean isCustomItem(EntryStack<?> entry) {
        if (entry.getType() != VanillaEntryTypes.ITEM) {
            return false;
        }
        ItemStack stack = entry.castValue();
        if (stack.get(DataComponentTypes.CUSTOM_MODEL_DATA) != null) {
            return true;
        }
        return getItemIndex().isCustomStack(stack);
    }

    private record ShapedCraftData(int width, int height, List<EntryIngredient> inputs) {
    }
}
