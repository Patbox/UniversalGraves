package eu.pb4.graves.ui;

import eu.pb4.graves.config.ConfigManager;
import eu.pb4.graves.grave.Grave;
import eu.pb4.graves.other.GraveUtils;
import eu.pb4.graves.other.Location;
import eu.pb4.graves.other.OutputSlot;
import eu.pb4.graves.registry.GraveCompassItem;
import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.GuiHelpers;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.GuiInterface;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

public class GraveGui extends PagedGui {
    private final Grave grave;
    private final Inventory inventory;
    private final boolean canTake;
    private final boolean canFetch;
    private final GuiInterface previousUi;
    private int ticker = 0;
    private int actionTimeRemoveProtect = -1;
    private int actionTimeFetch = -1;
    private final boolean canTeleport;

    public GraveGui(ServerPlayerEntity player, Grave grave, boolean canTake, boolean canTeleport, boolean canFetch) {
        super(player);
        this.grave = grave;
        this.canTake = canTake;
        this.canTeleport = canTeleport;
        this.canFetch = canFetch;
        this.setTitle(ConfigManager.getConfig().ui.graveTitle.with(grave.getPlaceholders(player.getWorld().getServer())));
        this.inventory = this.grave.asInventory();
        this.updateDisplay();
        this.previousUi = GuiHelpers.getCurrentGui(player);
    }

    @Override
    public boolean onAnyClick(int index, ClickType type, SlotActionType action) {
        return super.onAnyClick(index, type, action);
    }

    @Override
    public void onTick() {
        if (this.grave.isRemoved()) {
            if (this.previousUi != null) {
                this.previousUi.open();
            } else {
                this.close();
            }
        }

        this.ticker++;
        if (this.actionTimeRemoveProtect <= this.ticker) {
            this.actionTimeRemoveProtect = -1;
        }

        if (this.ticker % 20 == 0) {
            if (this.canTake) {
                this.grave.tryBreak(this.player.getServer(), this.player);
            }
            this.updateDisplay();
        }
        super.onTick();
    }

    @Override
    public void onClose() {
        this.grave.updateDisplay();
        this.grave.updateSelf(this.player.getServer());
        super.onClose();
    }

    @Override
    protected int getPageAmount() {
        return this.grave.getItems().size() / PAGE_SIZE + 1;
    }

    @Override
    protected DisplayElement getElement(int id) {
        if (id < this.inventory.size()) {
            return DisplayElement.of(new OutputSlot(inventory, id, 0, 0, canTake));
        }
        return DisplayElement.empty();
    }

    @Override
    protected DisplayElement getNavElement(int id) {
        var config = ConfigManager.getConfig();

        return switch (id) {
            case 1 -> {
                var placeholders = grave.getPlaceholders(this.player.getServer());

                yield DisplayElement.of(ConfigManager.getConfig().ui.graveInfoIcon.get(this.grave.isProtected())
                        .builder(placeholders)
                        .setCallback((x, y, z) -> {
                            var cursor = this.player.currentScreenHandler.getCursorStack();
                            if (!cursor.isEmpty() && cursor.isOf(Items.COMPASS)) {
                                cursor.decrement(1);

                                player.getInventory().offerOrDrop(GraveCompassItem.create(this.grave.getId(), true));
                            }
                        })
                );
            }
            case 2 -> getRemoveProtection();
            case 3 -> {
                if (this.canTake) {
                    yield DisplayElement.of(ConfigManager.getConfig().ui.quickPickupButton.builder()
                            .setCallback((x, y, z) -> {
                                playClickSound(this.player);
                                this.grave.quickEquip(this.player);
                            })
                    );
                } else if (this.canTeleport) {
                    yield DisplayElement.of(ConfigManager.getConfig().ui.teleportButton.get(config.teleportation.teleportationCostType.checkCost(player))
                            .builder(ConfigManager.getConfig().teleportation.teleportationCostType.getPlaceholders(player))
                            .setCallback((x, y, z) -> {
                                if (config.teleportation.teleportationCostType.takeCost(player)) {
                                    playClickSound(this.player);
                                    this.close();
                                    GraveUtils.teleportToGrave(this.player, grave, (b) -> {
                                        if (!b) {
                                            config.teleportation.teleportationCostType.returnCost(player);
                                        }
                                    });
                                } else {
                                    playClickSound(this.player, SoundEvents.ENTITY_VILLAGER_NO);
                                }
                            })
                    );
                } else {
                    yield DisplayElement.lowerBar(player);
                }
            }
            case 4 -> this.canFetch ?
                    DisplayElement.of(this.actionTimeFetch != -1 ? ConfigManager.getConfig().ui.fetchButton.get(false).builder()
                            .setCallback((x, y, z) -> {
                                playClickSound(player);
                                this.actionTimeFetch = -1;
                                if (!this.grave.moveTo(player.server, Location.fromEntity(player))) {
                                    //player.sendMessage(config.texts);
                                    return;
                                }

                                this.close();
                            }) : ConfigManager.getConfig().ui.fetchButton.get(true).builder()
                            .setCallback((x, y, z) -> {
                                playClickSound(player);
                                this.actionTimeFetch = this.ticker + 20 * 5;
                                this.updateDisplay();
                            })
                    ) : DisplayElement.lowerBar(player);
            case 5 -> DisplayElement.previousPage(this);
            case 6 -> this.previousUi != null ? DisplayElement.nextPage(this) : DisplayElement.lowerBar(player);
            case 7 -> this.previousUi == null ? DisplayElement.nextPage(this) : DisplayElement.lowerBar(player);
            case 8 -> this.previousUi != null ? DisplayElement.back(this.previousUi::open) : DisplayElement.lowerBar(player);
            default -> DisplayElement.lowerBar(player);
        };
    }

    private DisplayElement getRemoveProtection() {
        var config = ConfigManager.getConfig();
        if (this.grave.isProtected() && (this.canTake || config.interactions.allowRemoteProtectionRemoval || Permissions.check(player, "graves.can_remove_protection_remotely", 3))) {
            if (this.actionTimeRemoveProtect != -1) {
                return DisplayElement.of(config.ui.removeProtectionButton.get(false).builder()
                        .setCallback((x, y, z) -> {
                            playClickSound(player);
                            this.grave.disableProtection();
                            this.actionTimeRemoveProtect = -1;
                            this.updateDisplay();
                        })
                );
            } else {
                return DisplayElement.of(config.ui.removeProtectionButton.get(true).builder()
                        .setCallback((x, y, z) -> {
                            playClickSound(player);
                            this.actionTimeRemoveProtect = this.ticker + 20 * 5;
                            this.updateDisplay();
                        })
                );
            }
        }

        if (this.canTake || config.interactions.allowRemoteGraveBreaking || Permissions.check(player, "graves.can_break_remotely", 3)) {
            if (this.actionTimeRemoveProtect != -1) {
                return DisplayElement.of(config.ui.breakGraveButton.get(false).builder()
                        .setCallback((x, y, z) -> {
                            playClickSound(player);
                            this.grave.destroyGrave(this.player.getServer(), this.player);
                            this.actionTimeRemoveProtect = -1;
                            this.close();
                        })
                );
            } else {
                return DisplayElement.of(config.ui.breakGraveButton.get(true).builder()
                        .setCallback((x, y, z) -> {
                            playClickSound(player);
                            this.actionTimeRemoveProtect = this.ticker + 20 * 5;
                            this.updateDisplay();
                        })
                );
            }
        }
        return DisplayElement.lowerBar(player);
    }
}
