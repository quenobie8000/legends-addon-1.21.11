package legends.ultra.cool.addons.rei;

import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class LegendsReiCategory implements DisplayCategory<LegendsReiDisplay> {
    private static final int MIN_DISPLAY_HEIGHT = 60;
    private static final int DISPLAY_WIDTH = 160;
    private static final int SLOT_SIZE = 18;
    private static final int PADDING = 6;
    private static final int LABEL_HEIGHT = 12;
    private static final int MIN_INPUT_GAP = 4;
    private static final int MAX_INPUT_GAP = 8;
    private static final int LINE_COLOR = 0xFF707070;
    private static final int LABEL_COLOR_LIGHT = 0xFF404040;
    private static final int LABEL_COLOR_DARK = 0xFFBBBBBB;

    private final Renderer icon;
    private final Text title;

    public LegendsReiCategory(Renderer icon, Text title) {
        this.icon = icon;
        this.title = title;
    }

    @Override
    public CategoryIdentifier<? extends LegendsReiDisplay> getCategoryIdentifier() {
        return LegendsReiDisplay.CATEGORY;
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
        int inputCount = LegendsReiDisplay.getMaxInputCount();
        int contentHeight = inputCount * SLOT_SIZE + Math.max(0, inputCount - 1) * MIN_INPUT_GAP;
        int height = contentHeight + LABEL_HEIGHT + (PADDING * 2);
        return Math.max(height, MIN_DISPLAY_HEIGHT);
    }

    @Override
    public int getDisplayWidth(LegendsReiDisplay display) {
        return DISPLAY_WIDTH;
    }

    @Override
    public List<Widget> setupDisplay(LegendsReiDisplay display, Rectangle bounds) {
        List<Widget> widgets = new ArrayList<>();

        widgets.add(Widgets.createRecipeBase(bounds));

        String npcName = display.getNpcName();
        if (npcName != null && !npcName.isBlank()) {
            widgets.add(
                    Widgets.createLabel(new Point(bounds.x + PADDING, bounds.y + PADDING), Text.literal(npcName))
                            .leftAligned()
                            .noShadow()
                            .color(LABEL_COLOR_LIGHT, LABEL_COLOR_DARK)
            );
        }

        List<EntryIngredient> inputs = display.getInputEntries();
        int inputCount = inputs.size();

        int contentTop = bounds.y + PADDING + LABEL_HEIGHT;
        int contentHeight = Math.max(SLOT_SIZE, bounds.height - LABEL_HEIGHT - (PADDING * 2));

        int outputX = bounds.x + PADDING;
        int outputY = contentTop + (contentHeight - SLOT_SIZE) / 2;
        int outputRightX = outputX + SLOT_SIZE;
        int outputCenterY = outputY + SLOT_SIZE / 2;

        widgets.add(
                Widgets.createSlot(new Point(outputX, outputY))
                        .entries(display.getOutputEntries().get(0))
                        .markOutput()
        );

        if (inputCount > 0) {
            int inputGap = 0;
            if (inputCount > 1) {
                int maxGap = (contentHeight - inputCount * SLOT_SIZE) / (inputCount - 1);
                inputGap = Math.max(MIN_INPUT_GAP, Math.min(MAX_INPUT_GAP, maxGap));
            }

            int inputX = bounds.x + bounds.width - PADDING - SLOT_SIZE;
            int totalInputsHeight = inputCount * SLOT_SIZE + (inputCount - 1) * inputGap;
            int inputTop = contentTop + (contentHeight - totalInputsHeight) / 2;
            int branchX = inputX - 8;

            int topY = inputTop + SLOT_SIZE / 2;
            int bottomY = inputTop + (inputCount - 1) * (SLOT_SIZE + inputGap) + SLOT_SIZE / 2;

            drawHorizontal(widgets, outputRightX, branchX, outputCenterY);
            drawVertical(widgets, branchX, topY, bottomY);

            for (int i = 0; i < inputCount; i++) {
                int inputY = inputTop + i * (SLOT_SIZE + inputGap);
                int inputCenterY = inputY + SLOT_SIZE / 2;

                drawHorizontal(widgets, branchX, inputX + SLOT_SIZE / 2, inputCenterY);

                widgets.add(
                        Widgets.createSlot(new Point(inputX, inputY))
                                .entries(inputs.get(i))
                                .markInput()
                );
            }
        }

        return widgets;
    }

    private static void drawHorizontal(List<Widget> widgets, int x1, int x2, int y) {
        int left = Math.min(x1, x2);
        int width = Math.abs(x2 - x1) + 1;
        widgets.add(Widgets.createFilledRectangle(new Rectangle(left, y, width, 1), LINE_COLOR));
    }

    private static void drawVertical(List<Widget> widgets, int x, int y1, int y2) {
        int top = Math.min(y1, y2);
        int height = Math.abs(y2 - y1) + 1;
        widgets.add(Widgets.createFilledRectangle(new Rectangle(x, top, 1, height), LINE_COLOR));
    }
}
