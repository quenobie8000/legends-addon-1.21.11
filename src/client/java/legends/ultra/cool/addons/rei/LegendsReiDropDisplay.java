package legends.ultra.cool.addons.rei;

import legends.ultra.cool.addons.LegendsAddon;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.LoadedEntityProcessor;
import net.minecraft.entity.SpawnReason;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class LegendsReiDropDisplay extends BasicDisplay {
    public static final CategoryIdentifier<LegendsReiDropDisplay> CATEGORY =
            CategoryIdentifier.of(LegendsAddon.MOD_ID, "drops");

    private final String title;
    private final Identifier entityId;
    private final EntityType<?> entityType;
    private final List<DropEntry> drops;
    private final NbtCompound entityNbt;
    private LivingEntity cachedEntity;
    private static int maxDropCount = 0;

    private LegendsReiDropDisplay(
            List<EntryIngredient> outputs,
            String title,
            Identifier entityId,
            EntityType<?> entityType,
            List<DropEntry> drops,
            @Nullable NbtCompound entityNbt
    ) {
        super(List.of(), outputs);
        this.title = title;
        this.entityId = entityId;
        this.entityType = entityType;
        this.drops = drops;
        this.entityNbt = entityNbt;
        maxDropCount = Math.max(maxDropCount, drops.size());
    }

    public static LegendsReiDropDisplay create(
            Identifier entityId,
            EntityType<?> entityType,
            String title,
            List<DropEntry> drops,
            @Nullable NbtCompound entityNbt
    ) {
        List<EntryIngredient> outputs = new ArrayList<>(drops.size());
        for (DropEntry drop : drops) {
            outputs.add(EntryIngredients.of(drop.stack()));
        }
        return new LegendsReiDropDisplay(outputs, title, entityId, entityType, List.copyOf(drops), entityNbt);
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return CATEGORY;
    }

    @Override
    public @Nullable DisplaySerializer<? extends Display> getSerializer() {
        return null;
    }

    public String getTitle() {
        return title;
    }

    public Identifier getEntityId() {
        return entityId;
    }

    public EntityType<?> getEntityType() {
        return entityType;
    }

    public List<DropEntry> getDrops() {
        return drops;
    }

    public static int getMaxDropCount() {
        return Math.max(1, maxDropCount);
    }

    @Nullable
    public LivingEntity getOrCreateEntity(MinecraftClient client) {
        if (client == null || client.world == null) {
            return null;
        }
        if (cachedEntity != null && cachedEntity.getType() == entityType && cachedEntity.getEntityWorld() == client.world) {
            return cachedEntity;
        }
        Entity entity = createEntityFromNbt(client);
        if (entity == null) {
            entity = entityType.create(client.world, SpawnReason.COMMAND);
        }
        if (entity instanceof LivingEntity living) {
            cachedEntity = living;
            return living;
        }
        return null;
    }

    @Nullable
    private Entity createEntityFromNbt(MinecraftClient client) {
        if (entityNbt == null) {
            return null;
        }
        try {
            NbtCompound copy = entityNbt.copy();
            return EntityType.loadEntityWithPassengers(
                    entityType,
                    copy,
                    client.world,
                    SpawnReason.COMMAND,
                    LoadedEntityProcessor.NOOP
            );
        } catch (Exception e) {
            LegendsAddon.LOGGER.warn("[LegendsAddon] Failed to load entity NBT for {}", entityId, e);
            return null;
        }
    }

    public record DropEntry(ItemStack stack, double chance) {
    }
}
