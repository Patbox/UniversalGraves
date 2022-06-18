package eu.pb4.graves.ui;

import com.mojang.authlib.GameProfile;
import eu.pb4.graves.GraveNetworking;
import eu.pb4.graves.config.Config;
import eu.pb4.graves.config.ConfigManager;
import eu.pb4.graves.config.data.ConfigData;
import eu.pb4.graves.grave.Grave;
import eu.pb4.graves.grave.GraveManager;
import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GraveListGui extends PagedGui {
    private final UUID targetUUID;
    private final boolean canModify;
    private int ticker = 0;
    private List<Grave> graves;

    public GraveListGui(ServerPlayerEntity player, GameProfile profile, boolean canModify) {
        super(player);
        this.targetUUID = profile.getId();

        if (player.getUuid().equals(this.targetUUID)) {
            this.setTitle(ConfigManager.getConfig().guiTitle);
        } else {
            this.setTitle(Placeholders.parseText(
                    ConfigManager.getConfig().graveTitle,
                    Placeholders.PREDEFINED_PLACEHOLDER_PATTERN,
                    Map.of("player", Text.literal(profile.getName()))
            ));
        }
        this.graves = new ArrayList<>(GraveManager.INSTANCE.getByUuid(this.targetUUID));
        this.canModify = canModify;
        this.updateDisplay();
    }

    @Override
    protected int getPageAmount() {
        return this.graves.size() / PAGE_SIZE + 1;
    }

    @Override
    protected DisplayElement getElement(int id) {
        if (id < this.graves.size()) {
            var config = ConfigManager.getConfig();

            var grave = this.graves.get(id);

            var placeholders = grave.getPlaceholders(this.player.getServer());

            List<Text> parsed = new ArrayList<>();
            for (var text : grave.isProtected() ? ConfigManager.getConfig().guiProtectedText : ConfigManager.getConfig().guiText) {
                MutableText out = (MutableText) Placeholders.parseText(text, Placeholders.PREDEFINED_PLACEHOLDER_PATTERN, placeholders);
                if (out.getStyle().getColor() == null) {
                    out.setStyle(out.getStyle().withColor(Formatting.WHITE));
                }
                parsed.add(out);
            }

            var list = grave.isProtected() ? config.guiProtectedItem : config.guiItem;
            var element = GuiElementBuilder.from(list[Math.abs(grave.hashCode() % list.length)])
                    .setName(parsed.remove(0))
                    .setLore(parsed)
                    .setCallback((index, type, action) -> {
                        if (type.isRight) {
                            if (Permissions.check(this.player, "universal_graves.teleport", 3)) {
                                this.close();

                                var pos = grave.getLocation();

                                MinecraftServer server = Objects.requireNonNull(this.player.getServer(), "server; running on client?");
                                ServerWorld world = server.getWorld(RegistryKey.of(Registry.WORLD_KEY, pos.world()));
                                if (world != null) {
                                    final ScheduledExecutorService teleportThread = Executors.newScheduledThreadPool(1);
                                    server.execute(() -> this.player.sendMessage(Text.translatable(config.teleportTimerText), config.configData.teleportTime )));
                                    teleportThread.schedule(() -> {
                                        server.execute(() -> this.player.sendMessage(Text.translatable(config.teleportLocationText), pos.x() + ", " + (pos.y()+config.configData.teleportHeight) + ", " + pos.z())));
                                        this.player.teleport(world, pos.x() + 0.5, pos.y() + 1 + config.configData.teleportHeight, pos.z() + 0.5, this.player.getYaw(), this.player.getPitch());
                                        server.execute(() -> this.player.playSound(SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.MASTER, 1f, 1f));
                                        server.execute(() -> this.player.setInvulnerable(true));
                                        teleportThread.schedule(() -> {
                                            Objects.requireNonNull(this.player.getServer()).execute(() -> player.setInvulnerable(false));
                                        }, config.configData.invincibleTime, TimeUnit.SECONDS);
                                    }, config.configData.teleportTime, TimeUnit.SECONDS);
                                }
                                }
                        } else {
                            this.close();
                            grave.openUi(player, this.canModify);
                        }
                    });

            return DisplayElement.of(element);
        }

        return DisplayElement.empty();
    }

    @Override
    protected DisplayElement getNavElement(int id) {
        return switch (id) {
            case 2 -> DisplayElement.previousPage(this);
            case 6 -> DisplayElement.nextPage(this);
            default -> GraveNetworking.canReceiveGui(this.player.networkHandler) ? DisplayElement.empty() : DisplayElement.filler();
        };
    }

    @Override
    public void onTick() {
        this.ticker++;
        if (this.ticker % 20 == 0) {
            this.graves = new ArrayList<>(GraveManager.INSTANCE.getByUuid(this.targetUUID));
            this.updateDisplay();
        }
        super.onTick();
    }
}
