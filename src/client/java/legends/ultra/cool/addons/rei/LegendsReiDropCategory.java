package legends.ultra.cool.addons.rei;

import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LegendsReiDropCategory implements DisplayCategory<LegendsReiDropDisplay> {
    private static final int MIN_DISPLAY_HEIGHT = 90;
    private static final int DISPLAY_WIDTH = 160;
    private static final int SLOT_SIZE = 18;
    private static final int PADDING = 6;
    private static final int LABEL_HEIGHT = 12;
    private static final int MOB_AREA_HEIGHT = 48;
    private static final int DROPS_TOP_GAP = 4;
    private static final int DROP_GAP = 4;
    private static final int LABEL_COLOR_LIGHT = 0xFF404040;
    private static final int LABEL_COLOR_DARK = 0xFFBBBBBB;
    private static final float ISO_YAW_DEGREES = 25.0f;
    private static final float ISO_PITCH_DEGREES = -18.0f;

    private final Renderer icon;
    private final Text title;

    public LegendsReiDropCategory(Renderer icon, Text title) {
        this.icon = icon;
        this.title = title;
    }

    @Override
    public CategoryIdentifier<? extends LegendsReiDropDisplay> getCategoryIdentifier() {
        return LegendsReiDropDisplay.CATEGORY;
    }

    @Override
    public Text getTitle() {
        return title;
    }

    @Override
    public Renderer getIcon() {
        return icon;
    }

    @Override
    public int getDisplayHeight() {
        int dropCount = LegendsReiDropDisplay.getMaxDropCount();
        int dropsHeight = dropCount * SLOT_SIZE + Math.max(0, dropCount - 1) * DROP_GAP;
        int height = PADDING * 2 + LABEL_HEIGHT + MOB_AREA_HEIGHT + DROPS_TOP_GAP + dropsHeight;
        return Math.max(height, MIN_DISPLAY_HEIGHT);
    }

    @Override
    public int getDisplayWidth(LegendsReiDropDisplay display) {
        return DISPLAY_WIDTH;
    }

    @Override
    public List<Widget> setupDisplay(LegendsReiDropDisplay display, Rectangle bounds) {
        List<Widget> widgets = new ArrayList<>();

        widgets.add(Widgets.createRecipeBase(bounds));

        String label = display.getTitle();
        if (label != null && !label.isBlank()) {
            widgets.add(
                    Widgets.createLabel(new Point(bounds.x + PADDING, bounds.y + PADDING), Text.literal(label))
                            .leftAligned()
                            .noShadow()
                            .color(LABEL_COLOR_LIGHT, LABEL_COLOR_DARK)
            );
        }

        int contentTop = bounds.y + PADDING + LABEL_HEIGHT;
        int mobCenterX = bounds.x + bounds.width / 2;
        int mobTopY = bounds.y - 36;
        int mobBottomY = contentTop + MOB_AREA_HEIGHT;
        int mobSize = (MOB_AREA_HEIGHT/2);
        int mobLeftX = mobCenterX - mobSize / 2;
        int mobRightX = mobCenterX + mobSize / 2;
        float centerX = (mobLeftX + mobRightX) / 2.0f;
        float centerY = (mobTopY + mobBottomY) / 2.0f;
        float yawRad = ISO_YAW_DEGREES / 20.0f;
        float pitchRad = ISO_PITCH_DEGREES / 20.0f;
//        float mouseX = centerX - (float) Math.tan(yawRad) * 40.0f;
//        float mouseY = centerY - (float) Math.tan(pitchRad) * 40.0f;

        LivingEntity entity = display.getOrCreateEntity(MinecraftClient.getInstance());
        if (entity != null) {
            widgets.add(Widgets.createDrawableWidget((context, mouseX, mouseY, delta) ->
                    InventoryScreen.drawEntity(
                            context,
                            mobLeftX -10,
                            mobTopY,
                            mobRightX + 10,
                            mobBottomY,
                            mobSize,
                            1.0F,
                            mouseX,
                            mouseY,
                            entity
                    )
            ));
        }

        List<LegendsReiDropDisplay.DropEntry> drops = display.getDrops();
        if (!drops.isEmpty()) {
            int dropX = bounds.x + PADDING;
            int dropY = contentTop + MOB_AREA_HEIGHT + DROPS_TOP_GAP;
            for (int i = 0; i < drops.size(); i++) {
                LegendsReiDropDisplay.DropEntry drop = drops.get(i);
                int rowY = dropY + i * (SLOT_SIZE + DROP_GAP);

                widgets.add(
                        Widgets.createSlot(new Point(dropX, rowY))
                                .entries(EntryIngredients.of(drop.stack()))
                                .markOutput()
                );

                String chanceLabel = formatChance(drop.chance());
                widgets.add(
                        Widgets.createLabel(new Point(dropX + SLOT_SIZE + 6, rowY + 5), Text.literal(chanceLabel))
                                .leftAligned()
                                .noShadow()
                                .color(LABEL_COLOR_LIGHT, LABEL_COLOR_DARK)
                );
            }
        } else {
            widgets.add(
                    Widgets.createLabel(new Point(bounds.x + PADDING, contentTop + MOB_AREA_HEIGHT + DROPS_TOP_GAP), Text.literal("No drops"))
                            .leftAligned()
                            .noShadow()
                            .color(LABEL_COLOR_LIGHT, LABEL_COLOR_DARK)
            );
        }

        return widgets;
    }

    private static String formatChance(double chance) {
        double percent = chance * 100.0;
        if (percent <= 0.0) {
            return "0%";
        }
        if (Math.abs(percent - Math.round(percent)) < 0.05) {
            return String.format(Locale.ROOT, "%.0f%%", percent);
        }
        return String.format(Locale.ROOT, "%.1f%%", percent);
    }
}
