package eu.pb4.graves;

import eu.pb4.common.protection.api.CommonProtection;
import eu.pb4.graves.compat.*;
import eu.pb4.graves.config.ConfigManager;
import eu.pb4.graves.grave.GraveManager;
import eu.pb4.graves.other.Commands;
import eu.pb4.graves.other.GraveProtectionProvider;
import eu.pb4.graves.other.VanillaInventoryMask;
import eu.pb4.graves.registry.*;
import eu.pb4.polymer.core.api.block.PolymerBlockUtils;
import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import eu.pb4.polymer.core.api.other.PolymerComponent;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class GravesMod implements ModInitializer {
    public static final Logger LOGGER = LogManager.getLogger("Universal Graves");
    public static final boolean DEV = FabricLoader.getInstance().isDevelopmentEnvironment();
    public static final boolean IS_CLIENT = FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    public static ModContainer CONTAINER = FabricLoader.getInstance().getModContainer("universal-graves").get();

    public static final List<Runnable> DO_ON_NEXT_TICK = new ArrayList<>();

    @Override
    public void onInitialize() {
        CardboardWarning.checkAndAnnounce();
        FabricLoader loader = FabricLoader.getInstance();
        GenericModInfo.build(CONTAINER);


        GravesRegistry.register();
        Commands.register();

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register((e) -> {
            e.add(GravesRegistry.CONTAINER_GRAVE_ITEM);
        });

        GraveTextures.initialize();
        GraveGameRules.register();

        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) {
            PolymerResourcePackUtils.addModAssets("universal-graves");
        }

        CommonProtection.register(Identifier.of("universal_graves", "graves"), GraveProtectionProvider.INSTANCE);

        GravesApi.registerInventoryMask(Identifier.of("vanilla"), VanillaInventoryMask.INSTANCE);

        if (loader.isModLoaded("goml")) {
            GomlCompat.register();
        }
        if (loader.isModLoaded("inventorio")) {
            InventorioCompat.register();
        }

        if (loader.isModLoaded("accessories")) {
            AccessoriesCompat.register();
        }

        if (loader.isModLoaded("trinkets")) {
            TrinketsCompat.register(loader.isModLoaded("tclayer"));
        }
        if (loader.isModLoaded("sgod")) {
            SaveGearOnDeathCompat.register();
        }

        ServerLifecycleEvents.SERVER_STARTING.register((server) -> ConfigManager.loadConfig(server.getRegistryManager()));
        ServerLifecycleEvents.SERVER_STOPPED.register((server) -> {
            GraveManager.INSTANCE = null;
            ConfigManager.clearConfig();
        });
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            CardboardWarning.checkAndAnnounce();
        });

        ServerWorldEvents.LOAD.register(((server, world) -> {
            if (world == server.getOverworld()) {
                GraveManager.INSTANCE = world.getPersistentStateManager().getOrCreate(GraveManager.getType(server), "universal-graves");
                GraveManager.INSTANCE.setServer(server);
            }
        }));


        ServerTickEvents.END_SERVER_TICK.register(s -> {
            GraveManager.INSTANCE.tick(s);

            var copied = new ArrayList<>(DO_ON_NEXT_TICK);
            DO_ON_NEXT_TICK.clear();
            for (var c : copied) {
                try {
                    c.run();
                } catch (Throwable e) {
                    GravesMod.LOGGER.error("Error occurred while executing delayed task!", e);
                }
            }
        });
    }


}
