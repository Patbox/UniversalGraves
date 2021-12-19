package eu.pb4.graves.ui;

import eu.pb4.graves.config.ConfigManager;
import eu.pb4.graves.grave.Grave;
import eu.pb4.graves.other.OutputSlot;
import eu.pb4.placeholders.PlaceholderAPI;
import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;

public class GraveGui extends SimpleGui {
    private final Grave grave;

    public GraveGui(ServerPlayerEntity player, Grave grave, boolean canTake) {
        super(getScreenHandlerType(grave.getItems().size()), player, false);
        this.grave = grave;
        GuiElementBuilder emptyPane = new GuiElementBuilder(Items.GRAY_STAINED_GLASS_PANE).hideFlags().setName(new LiteralText(""));
        this.setTitle(PlaceholderAPI.parsePredefinedText(ConfigManager.getConfig().graveTitle, PlaceholderAPI.PREDEFINED_PLACEHOLDER_PATTERN, grave.getPlaceholders(player.getWorld().getServer())));
        int x = 0;

        var inventory = this.grave.asInventory();

        for (; x < this.grave.getItems().size(); x++) {
            if (this.getFirstEmptySlot() == -1) {
                return;
            }

            this.addSlotRedirect(new OutputSlot(inventory, x, 0, 0, canTake));
        }

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
        this.grave.updateDisplay();
        this.grave.updateSelf(this.player.getServer());
        super.onClose();
    }
}
