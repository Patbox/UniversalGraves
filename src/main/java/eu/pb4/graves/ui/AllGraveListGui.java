package eu.pb4.graves.ui;

import eu.pb4.graves.GraveTextures;
import eu.pb4.graves.config.ConfigManager;
import eu.pb4.graves.grave.Grave;
import eu.pb4.graves.grave.GraveManager;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;

public class AllGraveListGui extends PagedGui {
    private final boolean canModify;
    private int ticker = 0;
    private List<Grave> graves;
    private boolean canFetch;

    public AllGraveListGui(ServerPlayerEntity player, boolean canModify, boolean canFetch) {
        super(player);

        this.setTitle(ConfigManager.getConfig().ui.adminGraveListTitle.text());
        this.graves = new ArrayList<>(GraveManager.INSTANCE.getAll());
        this.canModify = canModify;
        this.canFetch = canFetch;
        this.updateDisplay();
    }

    @Override
    protected int getPageAmount() {
        return this.graves.size() / PAGE_SIZE + 1;
    }

    @Override
    protected GuiSlot getElement(int id) {
        if (id < this.graves.size()) {
            var config = ConfigManager.getConfig();

            var grave = this.graves.get(id);

            var placeholders = grave.getPlaceholders(this.player.getServer());

            var element = config.ui.listAllGraveIcon.get(grave.isProtected())
                    .builder(placeholders)
                    .setCallback((index, type, action) -> {
                        grave.openUi(player, this.canModify, this.canFetch);
                    });

            return GuiSlot.of(element);
        }

        return GuiSlot.empty();
    }

    @Override
    protected GuiSlot getNavElement(int id) {
        return switch (id) {
            case 2 -> GuiSlot.previousPage(this);
            case 6 -> GuiSlot.nextPage(this);
            default -> GraveTextures.hasGuiTexture(this.player) ? GuiSlot.empty() : GuiSlot.filler();
        };
    }

    @Override
    public void onTick() {
        this.ticker++;
        if (this.ticker % 20 == 0) {
            this.graves = new ArrayList<>(GraveManager.INSTANCE.getAll());
            this.updateDisplay();
        }
        super.onTick();
    }
}
