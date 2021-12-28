package eu.pb4.graves.ui;

import eu.pb4.graves.GraveNetworking;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.elements.GuiElementBuilderInterface;
import eu.pb4.sgui.api.elements.GuiElementInterface;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
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
        private static final DisplayElement FILLER = DisplayElement.of(
                new GuiElementBuilder(Items.WHITE_STAINED_GLASS_PANE)
                        .setName(new LiteralText(""))
                        .hideFlags()
        );

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
            if (gui.canNextPage()) {
                return DisplayElement.of(
                        new GuiElementBuilder(Items.PLAYER_HEAD)
                                .setName(new TranslatableText("text.graves.gui.next_page").formatted(Formatting.WHITE))
                                .hideFlags()
                                .setSkullOwner("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzg2MTg1YjFkNTE5YWRlNTg1ZjE4NGMzNGYzZjNlMjBiYjY0MWRlYjg3OWU4MTM3OGU0ZWFmMjA5Mjg3In19fQ")
                                .setCallback((x, y, z) -> {
                                    playClickSound(gui.player);
                                    gui.nextPage();
                                })
                );
            } else {
                return DisplayElement.of(
                        new GuiElementBuilder(Items.PLAYER_HEAD)
                                .setName(new TranslatableText("text.graves.gui.next_page").formatted(Formatting.DARK_GRAY))
                                .hideFlags()
                                .setSkullOwner("ewogICJ0aW1lc3RhbXAiIDogMTY0MDYxNjExMDQ4OCwKICAicHJvZmlsZUlkIiA6ICIxZjEyNTNhYTVkYTQ0ZjU5YWU1YWI1NmFhZjRlNTYxNyIsCiAgInByb2ZpbGVOYW1lIiA6ICJOb3RNaUt5IiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzdlNTc3MjBhNDg3OGM4YmNhYjBlOWM5YzQ3ZDllNTUxMjhjY2Q3N2JhMzQ0NWE1NGE5MWUzZTFlMWEyNzM1NmUiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ==")
                );
            }
        }

        public static DisplayElement previousPage(PagedGui gui) {
            if (gui.canPreviousPage()) {
                return DisplayElement.of(
                        new GuiElementBuilder(Items.PLAYER_HEAD)
                                .setName(new TranslatableText("text.graves.gui.previous_page").formatted(Formatting.WHITE))
                                .hideFlags()
                                .setSkullOwner("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzEwODI5OGZmMmIyNjk1MWQ2ODNlNWFkZTQ2YTQyZTkwYzJmN2M3ZGQ0MWJhYTkwOGJjNTg1MmY4YzMyZTU4MyJ9fX0")
                                .setCallback((x, y, z) -> {
                                    playClickSound(gui.player);
                                    gui.previousPage();
                                })
                );
            } else {
                return DisplayElement.of(
                        new GuiElementBuilder(Items.PLAYER_HEAD)
                                .setName(new TranslatableText("text.graves.gui.previous_page").formatted(Formatting.DARK_GRAY))
                                .hideFlags()
                                .setSkullOwner("ewogICJ0aW1lc3RhbXAiIDogMTY0MDYxNjE5MjE0MiwKICAicHJvZmlsZUlkIiA6ICJmMjc0YzRkNjI1MDQ0ZTQxOGVmYmYwNmM3NWIyMDIxMyIsCiAgInByb2ZpbGVOYW1lIiA6ICJIeXBpZ3NlbCIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS81MDgyMGY3NmUzZTA0MWM3NWY3NmQwZjMwMTIzMmJkZjQ4MzIxYjUzNGZlNmE4NTljY2I4NzNkMjk4MWE5NjIzIiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0=")
                );
            }
        }

        public static DisplayElement filler() {
            return FILLER;
        }

        public static DisplayElement empty() {
            return EMPTY;
        }
    }

    public static final void playClickSound(ServerPlayerEntity player) {
        player.playSound(SoundEvents.UI_BUTTON_CLICK, SoundCategory.MASTER, 1, 1);
    }
}
