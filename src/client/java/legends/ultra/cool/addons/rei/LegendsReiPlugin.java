package legends.ultra.cool.addons.rei;

import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.entry.EntryRegistry;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

import java.util.List;

public class LegendsReiPlugin implements REIClientPlugin {

    public EntryStack<?> addFake(Item Type, String customName, float customItemModel) {
        ItemStack item = new ItemStack(Type);
        item.set(DataComponentTypes.ITEM_NAME, Text.literal(customName));
        item.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(List.of(customItemModel),null, null, null));

        return EntryStacks.of(item);
    }

    @Override
    public void registerEntries(EntryRegistry registry) {
        registry.removeEntryIf(entry ->
            entry.getType().equals(VanillaEntryTypes.ITEM) || entry.getType().equals(VanillaEntryTypes.FLUID)
        );

        EntryStack<?>[] itemList = {
                addFake(Items.IRON_SWORD, "Vampiric Dagger", 3)
        };

        for (EntryStack<?> entryStack : itemList) {
            registry.addEntries(entryStack);
        }
    }

}
