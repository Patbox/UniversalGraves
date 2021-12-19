package eu.pb4.graves.ui;

import com.mojang.authlib.GameProfile;
import eu.pb4.graves.config.ConfigManager;
import eu.pb4.graves.grave.Grave;
import eu.pb4.graves.grave.GraveManager;
import eu.pb4.placeholders.PlaceholderAPI;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GraveListGui extends SimpleGui {
    private final UUID targetUUID;
    private int ticker = 0;

    public GraveListGui(ServerPlayerEntity player, GameProfile profile) {
        super(ScreenHandlerType.GENERIC_9X3, player, false);
        this.targetUUID = profile.getId();

        if (player.getUuid().equals(this.targetUUID)) {
            this.setTitle(ConfigManager.getConfig().guiTitle);
        } else {
            this.setTitle(PlaceholderAPI.parsePredefinedText(
                    ConfigManager.getConfig().graveTitle,
                    PlaceholderAPI.PREDEFINED_PLACEHOLDER_PATTERN,
                    Map.of("player", new LiteralText(profile.getName()))
            ));
        }
        this.updateIcons();
    }

    private void updateIcons() {
        var config = ConfigManager.getConfig();
        for (int x = 0; x < this.size; x++) {
            this.clearSlot(x);
        }

        for (Grave graveInfo : GraveManager.INSTANCE.getByUuid(this.targetUUID)) {
            if (this.getFirstEmptySlot() == -1) {
                return;
            }

            Map<String, Text> placeholders = graveInfo.getPlaceholders(this.player.getServer());

            List<Text> parsed = new ArrayList<>();
            for (Text text : graveInfo.isProtected() ? ConfigManager.getConfig().guiProtectedText : ConfigManager.getConfig().guiText) {
                MutableText out = (MutableText) PlaceholderAPI.parsePredefinedText(text, PlaceholderAPI.PREDEFINED_PLACEHOLDER_PATTERN, placeholders);
                if (out.getStyle().getColor() == null) {
                    out.setStyle(out.getStyle().withColor(Formatting.WHITE));
                }
                parsed.add(out);
            }

            var list = graveInfo.isProtected() ? config.guiProtectedItem : config.guiItem;
            this.addSlot(GuiElementBuilder.from(list[Math.abs(graveInfo.hashCode() % list.length)])
                    .setName((MutableText) parsed.remove(0))
                    .setLore(parsed)
                    .setCallback((index, type, action) -> {
                        if (type.isRight) {
                            if (Permissions.check(this.player, "universal_graves.teleport", 3)) {
                                this.close();

                                var pos = graveInfo.getLocation();

                                ServerWorld world = this.player.getServer().getWorld(RegistryKey.of(Registry.WORLD_KEY, pos.world()));
                                if (world != null) {
                                    this.player.teleport(world, pos.x() + 0.5, pos.y() + 1, pos.z() + 0.5, this.player.getYaw(), this.player.getPitch());
                                }
                            }
                        } else {
                            this.close();
                            graveInfo.openUi(player, false);
                        }
                    })
            );
        }
    }

    @Override
    public void onTick() {
        this.ticker++;
        if (this.ticker % 20 == 0) {
            this.updateIcons();
        }
        super.onTick();
    }
}
