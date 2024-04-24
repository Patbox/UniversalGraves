package eu.pb4.graves.ui;

import eu.pb4.graves.GraveTextures;
import eu.pb4.graves.config.ConfigManager;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementBuilderInterface;
import eu.pb4.sgui.api.elements.GuiElementInterface;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

public record GuiSlot(@Nullable GuiElementInterface element, @Nullable Slot slot) {
    private static final GuiSlot EMPTY = GuiSlot.of(new GuiElement(ItemStack.EMPTY, GuiElementInterface.EMPTY_CALLBACK));

    public static GuiSlot of(GuiElementInterface element) {
        return new GuiSlot(element, null);
    }

    public static GuiSlot of(GuiElementBuilderInterface<?> element) {
        return new GuiSlot(element.build(), null);
    }

    public static GuiSlot of(Slot slot) {
        return new GuiSlot(null, slot);
    }

    public static GuiSlot nextPage(PagedGui gui) {
        var config = ConfigManager.getConfig();
        if (gui.canNextPage()) {
            return GuiSlot.of(
                    config.ui.nextButton.get(true).builder()
                            .noDefaults()
                            .hideDefaultTooltip()
                            .setCallback((x, y, z) -> {
                                PagedGui.playClickSound(gui.getPlayer());
                                gui.nextPage();
                            })
            );
        } else {
            return GuiSlot.of(config.ui.nextButton.get(false).builder()
                    .noDefaults()
                    .hideDefaultTooltip());
        }
    }

    public static GuiSlot previousPage(PagedGui gui) {
        var config = ConfigManager.getConfig();

        if (gui.canPreviousPage()) {
            return GuiSlot.of(
                    config.ui.previousButton.get(true).builder()
                            .noDefaults()
                            .hideDefaultTooltip()
                            .setCallback((x, y, z) -> {
                                PagedGui.playClickSound(gui.getPlayer());
                                gui.previousPage();
                            })
            );
        } else {
            return GuiSlot.of(config.ui.previousButton.get(false).builder()
                    .noDefaults()
                    .hideDefaultTooltip()
            );
        }
    }

    public static GuiSlot filler() {
        return GuiSlot.of(
                ConfigManager.getConfig().ui.barButton.builder().hideTooltip()
        );
    }

    public static GuiSlot empty() {
        return EMPTY;
    }

    public static GuiSlot lowerBar(ServerPlayerEntity player) {
        return GraveTextures.hasGuiTexture(player) ? GuiSlot.empty() : GuiSlot.filler();
    }

    public static GuiSlot back(Runnable back) {
        var config = ConfigManager.getConfig();
        return GuiSlot.of(
                config.ui.backButton.builder()
                        .noDefaults()
                        .hideDefaultTooltip()
                        .setCallback((x, y, z, d) -> {
                            PagedGui.playClickSound(d.getPlayer());
                            back.run();
                        })
        );
    }
}
