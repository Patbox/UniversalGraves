package eu.pb4.graves.ui;

import eu.pb4.graves.config.ConfigManager;
import eu.pb4.graves.grave.Grave;
import eu.pb4.graves.other.GenericCost;
import eu.pb4.graves.other.GraveUtils;
import eu.pb4.graves.other.Location;
import eu.pb4.graves.other.OutputSlot;
import eu.pb4.graves.registry.GraveCompassItem;
import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.SguiUtils;
import eu.pb4.sgui.api.gui.GuiLike;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.item.Items;

public class GraveGui extends PagedGui {
    private final Grave grave;
    private final Container inventory;
    private boolean canTake;
    private final boolean canFetch;
    private final GuiLike previousUi;
    private final boolean canModify;
    private final boolean canTeleport;
    private final boolean hasAccess;
    private int ticker = 0;
    private int actionTimeRemoveProtect = -1;
    private int actionTimeFetch = -1;
    private int currentGraveSize;

    public GraveGui(ServerPlayer player, Grave grave, boolean canModify, boolean canFetch) {
        super(player);
        this.grave = grave;
        this.canModify = canModify;
        this.canTeleport = ConfigManager.getConfig().teleportation.cost.type() != GenericCost.Type.CREATIVE || player.isCreative();
        this.hasAccess = grave.hasAccess(player);
        this.canTake = grave.canTakeFrom(player);
        this.canFetch = canFetch;
        this.setTitle(ConfigManager.getConfig().ui.graveTitle.with(grave.getPlaceholders(player.level().getServer())));
        this.inventory = this.grave.asInventory();
        this.currentGraveSize = this.inventory.getContainerSize();
        this.previousUi = SguiUtils.getCurrentGui(player);
        this.updateDisplay();
    }

    @Override
    public boolean onAnyClick(int index, ClickType type, net.minecraft.world.inventory.ContainerInput action) {
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

        if (this.currentGraveSize != this.inventory.getContainerSize()) {
            this.currentGraveSize = this.inventory.getContainerSize();
            this.updateDisplay();
        } else if (this.ticker % 20 == 0) {
            if (this.canTake) {
                this.grave.tryBreak(this.player.level().getServer(), this.player);
            }
            this.updateDisplay();
        }
        super.onTick();
    }

    @Override
    public void onClose() {
        this.grave.updateDisplay();
        this.grave.updateSelf(this.player.level().getServer());
        super.onClose();
    }

    @Override
    protected int getPageAmount() {
        return this.grave.getItems().size() / PAGE_SIZE + 1;
    }

    @Override
    protected GuiSlot getElement(int id) {
        if (id < this.inventory.getContainerSize()) {
            return GuiSlot.of(new OutputSlot(inventory, id, 0, 0, this.canModify && this.canTake));
        }
        return GuiSlot.empty();
    }

    @Override
    protected GuiSlot getNavElement(int id) {
        var config = ConfigManager.getConfig();

        return switch (id) {
            case 0 -> {
                var placeholders = grave.getPlaceholders(this.player.level().getServer());

                yield GuiSlot.of(ConfigManager.getConfig().ui.graveInfoIcon.get(this.grave.isProtected())
                        .builder(placeholders)
                        .setCallback((x, y, z) -> {
                            var cursor = this.player.containerMenu.getCarried();
                            if (!cursor.isEmpty() && cursor.is(Items.COMPASS)) {
                                cursor.shrink(1);

                                player.getInventory().placeItemBackInInventory(GraveCompassItem.create(this.grave.getId(), true));
                            }
                        })
                );
            }
            case 1 -> getUnlockGrave();
            case 2 -> getRemoveProtection();
            case 3 -> {
                if (this.canTake && this.canModify) {
                    yield GuiSlot.of(ConfigManager.getConfig().ui.quickPickupButton.builder()
                            .setCallback((x, y, z) -> {
                                playClickSound(this.player);
                                this.grave.quickEquip(this.player);
                            })
                    );
                } else if (this.canTeleport) {
                    yield GuiSlot.of(ConfigManager.getConfig().ui.teleportButton.get(config.teleportation.cost.checkCost(player))
                            .builder(ConfigManager.getConfig().teleportation.cost.getPlaceholders())
                            .setCallback((x, y, z) -> {
                                if (config.teleportation.cost.takeCost(player)) {
                                    playClickSound(this.player);
                                    this.close();
                                    GraveUtils.teleportToGrave(this.player, grave, (b) -> {
                                        if (!b) {
                                            config.teleportation.cost.returnCost(player);
                                        }
                                    });
                                } else {
                                    playClickSound(this.player, SoundEvents.VILLAGER_NO);
                                }
                            })
                    );
                } else {
                    yield GuiSlot.lowerBar(player);
                }
            }
            case 4 -> this.canFetch ?
                    GuiSlot.of(this.actionTimeFetch != -1 ? ConfigManager.getConfig().ui.fetchButton.get(false).builder()
                            .setCallback((x, y, z) -> {
                                playClickSound(player);
                                this.actionTimeFetch = -1;
                                if (!this.grave.moveTo(player.level().getServer(), Location.fromEntity(player))) {
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
                    ) : GuiSlot.lowerBar(player);
            case 5 -> GuiSlot.previousPage(this);
            case 6 -> this.previousUi != null ? GuiSlot.nextPage(this) : GuiSlot.lowerBar(player);
            case 7 -> this.previousUi == null ? GuiSlot.nextPage(this) : GuiSlot.lowerBar(player);
            case 8 -> this.previousUi != null ? GuiSlot.back(this.previousUi::open) : GuiSlot.lowerBar(player);
            default -> GuiSlot.lowerBar(player);
        };
    }

    private GuiSlot getUnlockGrave() {
        var config = ConfigManager.getConfig();
        if (this.grave.isPaymentRequired() && (config.interactions.allowRemoteGraveUnlocking || Permissions.check(player.createCommandSourceStack(), "graves.can_unlock_remotely", 3))) {
            return GuiSlot.of(ConfigManager.getConfig().ui.unlockButton.get(config.interactions.cost.checkCost(player))
                    .builder(ConfigManager.getConfig().interactions.cost.getPlaceholders())
                    .setCallback((x, y, z) -> {
                        if (this.grave.payForUnlock(player)) {
                            this.canTake = this.grave.canTakeFrom(player);
                            playClickSound(this.player);
                            this.updateDisplay();
                        } else {
                            playClickSound(this.player, SoundEvents.VILLAGER_NO);
                        }
                    }));
        }

        return GuiSlot.lowerBar(player);
    }

    private GuiSlot getRemoveProtection() {
        var config = ConfigManager.getConfig();
        if (this.grave.isProtected() && (this.hasAccess && (config.interactions.allowRemoteProtectionRemoval || Permissions.check(player.createCommandSourceStack(), "graves.can_remove_protection_remotely", 3)))) {
            if (this.actionTimeRemoveProtect != -1) {
                return GuiSlot.of(config.ui.removeProtectionButton.get(false).builder()
                        .setCallback((x, y, z) -> {
                            playClickSound(player);
                            this.grave.disableProtection();
                            this.actionTimeRemoveProtect = -1;
                            this.updateDisplay();
                        })
                );
            } else {
                return GuiSlot.of(config.ui.removeProtectionButton.get(true).builder()
                        .setCallback((x, y, z) -> {
                            playClickSound(player);
                            this.actionTimeRemoveProtect = this.ticker + 20 * 5;
                            this.updateDisplay();
                        })
                );
            }
        }

        if (this.canModify || (this.canTake && (config.interactions.allowRemoteGraveBreaking || Permissions.check(player.createCommandSourceStack(), "graves.can_break_remotely", 3)))) {
            if (this.actionTimeRemoveProtect != -1) {
                return GuiSlot.of(config.ui.breakGraveButton.get(false).builder()
                        .setCallback((x, y, z) -> {
                            playClickSound(player);
                            this.grave.destroyGrave(this.player.level().getServer(), this.player);
                            this.actionTimeRemoveProtect = -1;
                            this.close();
                        })
                );
            } else {
                return GuiSlot.of(config.ui.breakGraveButton.get(true).builder()
                        .setCallback((x, y, z) -> {
                            playClickSound(player);
                            this.actionTimeRemoveProtect = this.ticker + 20 * 5;
                            this.updateDisplay();
                        })
                );
            }
        }
        return GuiSlot.lowerBar(player);
    }
}
