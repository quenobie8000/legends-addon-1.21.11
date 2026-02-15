package legends.ultra.cool.addons.rei;

import legends.ultra.cool.addons.LegendsAddon;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class LegendsReiDisplay extends BasicDisplay {

    public static final CategoryIdentifier<LegendsReiDisplay> CATEGORY =
            CategoryIdentifier.of(LegendsAddon.MOD_ID, "acquire"); // namespace, path

    private static int maxInputCount = 0;
    private final String npcName;

    public LegendsReiDisplay(List<EntryIngredient> inputs, List<EntryIngredient> outputs, String npcName) {
        super(inputs, outputs);
        this.npcName = npcName;
        maxInputCount = Math.max(maxInputCount, inputs.size());
    }

    public static LegendsReiDisplay trade(ItemStack output, List<ItemStack> inputs, String npcName) {
        List<EntryIngredient> inputEntries = new ArrayList<>(inputs.size());
        for (ItemStack input : inputs) {
            inputEntries.add(EntryIngredients.of(input));
        }
        return new LegendsReiDisplay(
                inputEntries,
                List.of(EntryIngredients.of(output)),
                npcName
        );
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return CATEGORY;
    }

    @Override
    public @Nullable DisplaySerializer<? extends Display> getSerializer() {
        return null;
    }

    public String getNpcName() {
        return npcName;
    }

    public static int getMaxInputCount() {
        return Math.max(1, maxInputCount);
    }
}
