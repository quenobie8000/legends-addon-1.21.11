package legends.ultra.cool.addons.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.storage.NbtWriteView;
import net.minecraft.text.Text;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.Identifier;

import java.util.Optional;

public class EntityDebug {

    public static String dumpTargetFiltered(LivingEntity e) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return null;
        if (!(e instanceof LivingEntity mob)) {
            client.player.sendMessage(Text.literal("[LegendsAddon] Target is not a LivingEntity"),false);
            return null;
        }

        String mobName =  mob.getDisplayName().getString();
        double[] mobStats = getMobStats(mob);
        double maxHp = mobStats[1];
        double itemDef = mobStats[2];
        double itemDmg = mobStats[3];

        return "\nMob: " + mobName + "\nMaxHP: " + maxHp + "\ndef: " + itemDef + "\ndmg: " + itemDmg;
    }

    public static double[] getMobStats(LivingEntity e) {

        ItemStack main = e.getMainHandStack();
        String mobName;

        double maxHp = 0;
        double currentHp = 0;
        Optional<Object> itemDef = readCustomInt(main, "def");
        Optional<Object> itemDmg = readCustomInt(main, "dmg");

        mobName =  e.getDisplayName().getString();
        if (mobName.matches(".*?(\\d+).*\\/(\\d+).*")) currentHp = Double.parseDouble(mobName.replaceAll(".*?(\\d+(?:[.,]\\d+)?).*?\\/.*?(\\d+(?:[.,]\\d+)?).*", "$1"));
        if (mobName.matches(".*?(\\d+).*\\/(\\d+).*")) maxHp = Double.parseDouble(mobName.replaceAll(".*?(\\d+(?:[.,]\\d+)?).*?\\/.*?(\\d+(?:[.,]\\d+)?).*", "$2"));

        return new double[]{currentHp, maxHp, (int) itemDef.get(), (int) itemDmg.get()};
    }

    private static Optional<Object> readCustomInt(ItemStack stack, String key) {
        if (stack.isEmpty()) return Optional.of(0);

        NbtComponent custom = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (custom == null || custom.isEmpty()) return Optional.of(0);

        NbtCompound nbt = custom.copyNbt(); // copies the NBT payload :contentReference[oaicite:2]{index=2}
        return java.util.Optional.of(nbt.contains(key) ? nbt.getInt(key) : 0);
    }

    public static NbtCompound getEntityFullNbt(LivingEntity entity) {
        if (entity == null) {
            return new NbtCompound();
        }
        try {
            NbtWriteView writeView = NbtWriteView.create(ErrorReporter.EMPTY);
            entity.writeData(writeView);
            NbtCompound nbt = writeView.getNbt();
            if (!nbt.contains("id")) {
                Identifier id = Registries.ENTITY_TYPE.getId(entity.getType());
                if (id != null) {
                    nbt.putString("id", id.toString());
                }
            }
            return nbt;
        } catch (Exception e) {
            return new NbtCompound();
        }
    }

}


