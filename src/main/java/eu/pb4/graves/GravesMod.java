package eu.pb4.graves;

import eu.pb4.common.protection.api.CommonProtection;
import eu.pb4.graves.compat.GomlCompat;
import eu.pb4.graves.compat.InventorioCompat;
import eu.pb4.graves.compat.SaveGearOnDeathCompat;
import eu.pb4.graves.compat.TrinketsCompat;
import eu.pb4.graves.config.ConfigManager;
import eu.pb4.graves.grave.GraveManager;
import eu.pb4.graves.other.Commands;
import eu.pb4.graves.other.GraveProtectionProvider;
import eu.pb4.graves.other.VanillaInventoryMask;
import eu.pb4.graves.registry.*;
import eu.pb4.polymer.core.api.block.PolymerBlockUtils;
import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
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
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
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

        Registry.register(Registries.ITEM, new Identifier("universal_graves", "grave_compass"), GraveCompassItem.INSTANCE);
        Registry.register(Registries.ITEM, new Identifier("universal_graves", "visual_grave"), CointainerGraveBlockItem.INSTANCE);
        Registry.register(Registries.ITEM, new Identifier("universal_graves", "icon"), IconItem.INSTANCE);
        Registry.register(Registries.BLOCK, new Identifier("universal_graves", "grave"), GraveBlock.INSTANCE);
        Registry.register(Registries.BLOCK, new Identifier("universal_graves", "visual_grave"), VisualGraveBlock.INSTANCE);
        Registry.register(Registries.BLOCK, new Identifier("universal_graves", "container_grave"), ContainerGraveBlock.INSTANCE);
        Registry.register(Registries.BLOCK, new Identifier("universal_graves", "temp_block"), TempBlock.INSTANCE);
        Registry.register(Registries.ENTITY_TYPE, new Identifier("universal_graves", "xp"), SafeXPEntity.TYPE);
        PolymerEntityUtils.registerType(SafeXPEntity.TYPE);
        GraveBlockEntity.BLOCK_ENTITY_TYPE = Registry.register(Registries.BLOCK_ENTITY_TYPE, "universal_graves:grave", FabricBlockEntityTypeBuilder.create(GraveBlockEntity::new, GraveBlock.INSTANCE).build(null));
        VisualGraveBlockEntity.BLOCK_ENTITY_TYPE = Registry.register(Registries.BLOCK_ENTITY_TYPE, "universal_graves:visual_grave", FabricBlockEntityTypeBuilder.create(VisualGraveBlockEntity::new, VisualGraveBlock.INSTANCE).build(null));
        ContainerGraveBlockEntity.BLOCK_ENTITY_TYPE = Registry.register(Registries.BLOCK_ENTITY_TYPE, "universal_graves:container_grave", FabricBlockEntityTypeBuilder.create(ContainerGraveBlockEntity::new, ContainerGraveBlock.INSTANCE).build(null));
        Commands.register();
        PolymerBlockUtils.registerBlockEntity(GraveBlockEntity.BLOCK_ENTITY_TYPE, VisualGraveBlockEntity.BLOCK_ENTITY_TYPE, ContainerGraveBlockEntity.BLOCK_ENTITY_TYPE);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register((e) -> {
            if (!Thread.currentThread().getName().contains("client")) {
                e.add(CointainerGraveBlockItem.INSTANCE);
            }
        });

        GraveTextures.initialize();
        new GraveGameRules();

        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) {
            PolymerResourcePackUtils.addModAssets("universal-graves");
        }

        CommonProtection.register(new Identifier("universal_graves", "graves"), GraveProtectionProvider.INSTANCE);

        GravesApi.registerInventoryMask(new Identifier("vanilla"), VanillaInventoryMask.INSTANCE);

        if (loader.isModLoaded("goml")) {
            GomlCompat.register();
        }
        if (loader.isModLoaded("inventorio")) {
            InventorioCompat.register();
        }
        if (loader.isModLoaded("trinkets")) {
            TrinketsCompat.register();
        }
        if (loader.isModLoaded("sgod")) {
            SaveGearOnDeathCompat.register();
        }

        ServerLifecycleEvents.SERVER_STARTING.register((server) -> ConfigManager.loadConfig());
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            CardboardWarning.checkAndAnnounce();
        });

        ServerWorldEvents.LOAD.register(((server, world) -> {
            if (world == server.getOverworld()) {
                GraveManager.INSTANCE = world.getPersistentStateManager().getOrCreate(GraveManager.TYPE, "universal-graves");
                GraveManager.INSTANCE.setServer(server);
            }
        }));


        ServerTickEvents.END_SERVER_TICK.register(s -> {
            GraveManager.INSTANCE.tick(s);

            var copied = new ArrayList<>(DO_ON_NEXT_TICK);
            DO_ON_NEXT_TICK.clear();
            for (var c : copied) {
                c.run();
            }
        });
    }


}
