package legends.ultra.cool.addons.mixin.client;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(HandledScreen.class)
public interface HandledScreenAccessor {
    @Accessor("x")
    int legends$getX();

    @Accessor("y")
    int legends$getY();

    @Accessor("backgroundWidth")
    int legends$getBackgroundWidth();

    @Accessor("backgroundHeight")
    int legends$getBackgroundHeight();

    @Accessor("focusedSlot")
    Slot legends$getFocusedSlot();
}
