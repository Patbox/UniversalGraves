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

public record DisplayElement(@Nullable GuiElementInterface element, @Nullable Slot slot) {
    private static final DisplayElement EMPTY = DisplayElement.of(new GuiElement(ItemStack.EMPTY, GuiElementInterface.EMPTY_CALLBACK));

    public static DisplayElement of(GuiElementInterface element) {
        return new DisplayElement(element, null);
    }

    public static DisplayElement of(GuiElementBuilderInterface<?> element) {
        return new DisplayElement(element.build(), null);
    }

    public static DisplayElement of(Slot slot) {
        return new DisplayElement(null, slot);
    }

    public static DisplayElement nextPage(PagedGui gui) {
        var config = ConfigManager.getConfig();
        if (gui.canNextPage()) {
            return DisplayElement.of(
                    config.ui.nextButton.get(true).builder()
                            .setCallback((x, y, z) -> {
                                PagedGui.playClickSound(gui.getPlayer());
                                gui.nextPage();
                            })
            );
        } else {
            return DisplayElement.of(config.ui.nextButton.get(false).builder());
        }
    }

    public static DisplayElement previousPage(PagedGui gui) {
        var config = ConfigManager.getConfig();

        if (gui.canPreviousPage()) {
            return DisplayElement.of(
                    config.ui.previousButton.get(true).builder()
                            .setCallback((x, y, z) -> {
                                PagedGui.playClickSound(gui.getPlayer());
                                gui.previousPage();
                            })
            );
        } else {
            return DisplayElement.of(config.ui.previousButton.get(false).builder()
            );
        }
    }

    public static DisplayElement filler() {
        return DisplayElement.of(
                ConfigManager.getConfig().ui.barButton.builder()
        );
    }

    public static DisplayElement empty() {
        return EMPTY;
    }

    public static DisplayElement lowerBar(ServerPlayerEntity player) {
        return GraveTextures.hasGuiTexture(player) ? DisplayElement.empty() : DisplayElement.filler();
    }

    public static DisplayElement back(Runnable back) {
        var config = ConfigManager.getConfig();
        return DisplayElement.of(
                config.ui.backButton.builder()
                        .setCallback((x, y, z, d) -> {
                            PagedGui.playClickSound(d.getPlayer());
                            back.run();
                        })
        );
    }
}
