package eu.pb4.graves.grave;

import eu.pb4.graves.config.ConfigManager;
import eu.pb4.placeholders.PlaceholderAPI;
import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;

public class GraveGui extends SimpleGui {

    private final GraveBlockEntity grave;

    public GraveGui(ServerPlayerEntity player, GraveBlockEntity grave) {
        super(getScreenHandlerType(grave.info.itemCount), player, false);
        this.grave = grave;
        GuiElementBuilder emptyPane = new GuiElementBuilder(Items.GRAY_STAINED_GLASS_PANE).hideFlags().setName(new LiteralText(""));
        this.setTitle(PlaceholderAPI.parsePredefinedText(ConfigManager.getConfig().graveTitle, PlaceholderAPI.PREDEFINED_PLACEHOLDER_PATTERN, grave.info.getPlaceholders()));
        int x = 0;
        int skipped = 0;
        for (; x < this.grave.size(); x++) {
            if (this.getFirstEmptySlot() == -1) {
                return;
            }

            if (!grave.getStack(x).isEmpty()) {
                this.addSlotRedirect(new OutputSlot(grave, x, 0, 0));
            } else {
                skipped++;
            }
        }
        x -= skipped;

        for (; x < this.getSize(); x++) {
            this.addSlot(emptyPane);
        }
    }

    public static ScreenHandlerType<?> getScreenHandlerType(int size) {
        return switch ((size - 1) / 9) {
            case 0 -> ScreenHandlerType.GENERIC_9X1;
            case 1 -> ScreenHandlerType.GENERIC_9X2;
            case 2 -> ScreenHandlerType.GENERIC_9X3;
            case 3 -> ScreenHandlerType.GENERIC_9X4;
            case 4 -> ScreenHandlerType.GENERIC_9X5;
            default -> ScreenHandlerType.GENERIC_9X6;
        };
    }

    @Override
    public boolean onAnyClick(int index, ClickType type, SlotActionType action) {
        return super.onAnyClick(index, type, action);
    }

    @Override
    public void onTick() {
        if (this.grave.isRemoved()) {
            this.close();
        }

        super.onTick();
    }

    @Override
    public void onClose() {
        this.grave.updateState();
        super.onClose();
    }
}
