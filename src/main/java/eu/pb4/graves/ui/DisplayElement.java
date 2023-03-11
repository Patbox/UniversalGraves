package eu.pb4.graves.ui;

import eu.pb4.graves.GraveNetworking;
import eu.pb4.graves.config.ConfigManager;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.elements.GuiElementBuilderInterface;
import eu.pb4.sgui.api.elements.GuiElementInterface;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
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
                    GuiElementBuilder.from(config.guiNextPageIcon)
                            .setName(config.guiNextPageText)
                            .hideFlags()
                            .setCallback((x, y, z) -> {
                                PagedGui.playClickSound(gui.getPlayer());
                                gui.nextPage();
                            })
            );
        } else {
            return DisplayElement.of(
                    GuiElementBuilder.from(config.guiNextPageBlockedIcon)
                            .setName(config.guiNextPageBlockedText)
                            .hideFlags()
            );
        }
    }

    public static DisplayElement previousPage(PagedGui gui) {
        var config = ConfigManager.getConfig();

        if (gui.canPreviousPage()) {
            return DisplayElement.of(
                    GuiElementBuilder.from(config.guiPreviousPageIcon)
                            .setName(config.guiPreviousPageText)
                            .hideFlags()
                            .setCallback((x, y, z) -> {
                                PagedGui.playClickSound(gui.getPlayer());
                                gui.previousPage();
                            })
            );
        } else {
            return DisplayElement.of(
                    GuiElementBuilder.from(config.guiPreviousPageBlockedIcon)
                            .setName(config.guiPreviousPageBlockedText)
                            .hideFlags()
            );
        }
    }

    public static DisplayElement filler() {
        return DisplayElement.of(
                GuiElementBuilder.from(ConfigManager.getConfig().guiBarItem)
                        .setName(Text.empty())
                        .hideFlags()
        );
    }

    public static DisplayElement empty() {
        return EMPTY;
    }

    public static DisplayElement lowerBar(ServerPlayerEntity player) {
        return GraveNetworking.canReceiveGui(player.networkHandler) ? DisplayElement.empty() : DisplayElement.filler();
    }

    public static DisplayElement back(Runnable back) {
        var config = ConfigManager.getConfig();
        return DisplayElement.of(
                GuiElementBuilder.from(config.guiBackIcon)
                        .setName(config.guiBackText)
                        .hideFlags()
                        .setCallback((x, y, z, d) -> {
                            PagedGui.playClickSound(d.getPlayer());
                            back.run();
                        })
        );
    }
}
