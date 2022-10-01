package eu.pb4.graves.ui;

import eu.pb4.graves.GraveNetworking;
import eu.pb4.graves.config.ConfigManager;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.elements.GuiElementBuilderInterface;
import eu.pb4.sgui.api.elements.GuiElementInterface;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public abstract class PagedGui extends SimpleGui {
    public static final int PAGE_SIZE = 9 * 4;
    protected int page = 0;

    public PagedGui(ServerPlayerEntity player) {
        super(ScreenHandlerType.GENERIC_9X5, player, false);
    }

    protected void nextPage() {
        this.page = Math.min(this.getPageAmount() - 1, this.page + 1);
        this.updateDisplay();
    }

    protected boolean canNextPage() {
        return this.getPageAmount() > this.page + 1;
    }

    protected void previousPage() {
        this.page = Math.max(0, this.page - 1);
        this.updateDisplay();
    }

    protected boolean canPreviousPage() {
        return this.page - 1 >= 0;
    }

    protected void updateDisplay() {
        var offset = this.page * PAGE_SIZE;

        for (int i = 0; i < PAGE_SIZE; i++) {
            var element = this.getElement(offset + i);

            if (element == null) {
                element = DisplayElement.empty();
            }

            if (element.element() != null) {
                this.setSlot(i, element.element());
            } else if (element.slot() != null) {
                this.setSlotRedirect(i, element.slot());
            }
        }

        for (int i = 0; i < 9; i++) {
            var navElement = this.getNavElement(i);

            if (navElement == null) {
                navElement = DisplayElement.EMPTY;
            }

            if (navElement.element != null) {
                this.setSlot(i + PAGE_SIZE, navElement.element);
            } else if (navElement.slot != null) {
                this.setSlotRedirect(i + PAGE_SIZE, navElement.slot);
            }
        }
    }

    protected int getPage() {
        return this.page;
    }

    protected abstract int getPageAmount();

    protected abstract DisplayElement getElement(int id);

    protected abstract DisplayElement getNavElement(int id);

    @Override
    protected boolean sendGui() {
        var value = super.sendGui();
        GraveNetworking.sendGraveUi(this.player.networkHandler);
        return value;
    }

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
                                    playClickSound(gui.player);
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
                                    playClickSound(gui.player);
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
            );        }

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
                                playClickSound(d.getPlayer());
                                back.run();
                            })
            );
        }
    }

    public static final void playClickSound(ServerPlayerEntity player) {
        player.playSound(SoundEvents.UI_BUTTON_CLICK, SoundCategory.MASTER, 1, 1);
    }

    public static final void playClickSound(ServerPlayerEntity player, SoundEvent soundEvent) {
        player.playSound(soundEvent, SoundCategory.MASTER, 1, 1);
    }
}
