package eu.pb4.graves.ui;

import eu.pb4.graves.GraveTextures;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

public abstract class PagedGui extends SimpleGui {
    public static final int PAGE_SIZE = 9 * 4;
    protected int page = 0;

    public PagedGui(ServerPlayerEntity player) {
        super(ScreenHandlerType.GENERIC_9X5, player, false);
    }

    @Override
    public void setTitle(Text title) {
        super.setTitle(GraveTextures.get(this.getPlayer(), title));
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
                navElement = DisplayElement.empty();
            }

            if (navElement.element() != null) {
                this.setSlot(i + PAGE_SIZE, navElement.element());
            } else if (navElement.slot() != null) {
                this.setSlotRedirect(i + PAGE_SIZE, navElement.slot());
            }
        }
    }

    protected int getPage() {
        return this.page;
    }

    protected abstract int getPageAmount();

    protected abstract DisplayElement getElement(int id);

    protected abstract DisplayElement getNavElement(int id);

    public static final void playClickSound(ServerPlayerEntity player) {
        player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), SoundCategory.MASTER, 1, 1);
    }

    public static final void playClickSound(ServerPlayerEntity player, SoundEvent soundEvent) {
        player.playSound(soundEvent, SoundCategory.MASTER, 1, 1);
    }
}
