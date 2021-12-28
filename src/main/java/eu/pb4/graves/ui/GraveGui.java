package eu.pb4.graves.ui;

import eu.pb4.graves.GraveNetworking;
import eu.pb4.graves.config.ConfigManager;
import eu.pb4.graves.grave.Grave;
import eu.pb4.graves.other.OutputSlot;
import eu.pb4.graves.registry.GraveCompassItem;
import eu.pb4.placeholders.PlaceholderAPI;
import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;

import java.util.ArrayList;
import java.util.List;

public class GraveGui extends PagedGui {
    private final Grave grave;
    private final Inventory inventory;
    private final boolean canTake;
    private int ticker = 0;
    private int actionTime = -1;

    public GraveGui(ServerPlayerEntity player, Grave grave, boolean canTake) {
        super(player);
        this.grave = grave;
        this.canTake = canTake;
        this.setTitle(PlaceholderAPI.parsePredefinedText(ConfigManager.getConfig().graveTitle, PlaceholderAPI.PREDEFINED_PLACEHOLDER_PATTERN, grave.getPlaceholders(player.getWorld().getServer())));
        this.inventory = this.grave.asInventory();
        this.updateDisplay();
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

        this.ticker++;
        if (this.actionTime <= this.ticker) {
            this.actionTime = -1;
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
        return switch (id) {
            case 1 -> {
                var placeholders = grave.getPlaceholders(this.player.getServer());

                List<Text> parsed = new ArrayList<>();
                for (Text text : grave.isProtected() ? ConfigManager.getConfig().guiProtectedText : ConfigManager.getConfig().guiText) {
                    MutableText out = (MutableText) PlaceholderAPI.parsePredefinedText(text, PlaceholderAPI.PREDEFINED_PLACEHOLDER_PATTERN, placeholders);
                    if (out.getStyle().getColor() == null) {
                        out.setStyle(out.getStyle().withColor(Formatting.WHITE));
                    }
                    parsed.add(out);
                }

                yield DisplayElement.of(new GuiElementBuilder(Items.OAK_SIGN)
                        .setName((MutableText) parsed.remove(0))
                        .setLore(parsed)
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
                    yield DisplayElement.of(new GuiElementBuilder(Items.PLAYER_HEAD)
                            .setName(new TranslatableText("text.graves.gui.quick_pickup").formatted(Formatting.YELLOW))
                            .setSkullOwner("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDVjNmRjMmJiZjUxYzM2Y2ZjNzcxNDU4NWE2YTU2ODNlZjJiMTRkNDdkOGZmNzE0NjU0YTg5M2Y1ZGE2MjIifX19")
                            .setCallback((x, y, z) -> {
                                playClickSound(this.player);
                                this.grave.quickEquip(this.player);
                            })
                    );
                } else {
                    yield DisplayElement.filler();
                }
            }
            case 5 -> DisplayElement.previousPage(this);
            case 7 -> DisplayElement.nextPage(this);
            default -> GraveNetworking.canReceiveGui(this.player.networkHandler) ? DisplayElement.empty() : DisplayElement.filler();
        };
    }

    private DisplayElement getRemoveProtection() {
        if (!this.grave.isProtected()) {
            if (this.actionTime != -1) {
                return DisplayElement.of(new GuiElementBuilder(Items.PLAYER_HEAD)
                        .setName(new TranslatableText("text.graves.gui.break_grave").formatted(Formatting.RED))
                        .addLoreLine(new TranslatableText("text.graves.gui.cant_reverse").setStyle(Style.EMPTY.withColor(Formatting.DARK_RED).withBold(true).withItalic(false)))
                        .addLoreLine(LiteralText.EMPTY)
                        .addLoreLine(new TranslatableText("text.graves.gui.click_to_confirm").setStyle(Style.EMPTY.withColor(Formatting.WHITE).withItalic(false)))
                        .setSkullOwner("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGQ3NGQyOGQwOTdlNTNkYjExMTJlMzkwYTdkNGZmODJkZjcxODg2NmFlYThmMTY1MGQ5NDY2NTdhOTM2OTY5OSJ9fX0=")
                        .hideFlags()
                        .setCallback((x, y, z) -> {
                            playClickSound(player);
                            this.grave.destroyGrave(this.player.getServer(), this.player);
                            this.actionTime = -1;
                            this.close();
                        })
                );
            } else {
                return DisplayElement.of(new GuiElementBuilder(Items.PLAYER_HEAD)
                        .setName(new TranslatableText("text.graves.gui.break_grave").formatted(Formatting.RED))
                        .setSkullOwner("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGQ3NGQyOGQwOTdlNTNkYjExMTJlMzkwYTdkNGZmODJkZjcxODg2NmFlYThmMTY1MGQ5NDY2NTdhOTM2OTY5OSJ9fX0=")
                        .hideFlags()
                        .setCallback((x, y, z) -> {
                            playClickSound(player);
                            this.actionTime = this.ticker + 20 * 5;
                            this.updateDisplay();
                        })
                );
            }
        } else {
            if (this.actionTime != -1) {
                return DisplayElement.of(new GuiElementBuilder(Items.PLAYER_HEAD)
                        .setName(new TranslatableText("text.graves.gui.remove_protection").formatted(Formatting.RED))
                        .addLoreLine(new TranslatableText("text.graves.gui.cant_reverse").setStyle(Style.EMPTY.withColor(Formatting.DARK_RED).withBold(true).withItalic(false)))
                        .addLoreLine(LiteralText.EMPTY)
                        .addLoreLine(new TranslatableText("text.graves.gui.click_to_confirm").setStyle(Style.EMPTY.withColor(Formatting.WHITE).withItalic(false)))
                        .setSkullOwner("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODE5OWI1ZWUzMjBlNzk5N2Q5MWJiNWY4NjY1ZjNkMzJhZTQ5MjBlMDNjNmIzZDliN2VlY2E2OTcxMTk5OTcifX19")
                        .hideFlags()
                        .setCallback((x, y, z) -> {
                            playClickSound(player);
                            this.grave.disableProtection();
                            this.actionTime = -1;
                            this.updateDisplay();
                        })
                );
            } else {
                return DisplayElement.of(new GuiElementBuilder(Items.PLAYER_HEAD)
                        .setName(new TranslatableText("text.graves.gui.remove_protection").formatted(Formatting.RED))
                        .setSkullOwner("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODE5OWI1ZWUzMjBlNzk5N2Q5MWJiNWY4NjY1ZjNkMzJhZTQ5MjBlMDNjNmIzZDliN2VlY2E2OTcxMTk5OTcifX19")
                        .hideFlags()
                        .setCallback((x, y, z) -> {
                            playClickSound(player);
                            this.actionTime = this.ticker + 20 * 5;
                            this.updateDisplay();
                        })
                );
            }
        }
    }
}
