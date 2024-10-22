package eu.pb4.graves.registry;

import eu.pb4.polymer.core.api.block.PolymerBlockUtils;
import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import eu.pb4.polymer.core.api.other.PolymerComponent;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.function.Function;

public interface GravesRegistry {
    GraveBlock GRAVE_BLOCK = register("grave", GraveBlock::new);
    VisualGraveBlock VISUAL_GRAVE_BLOCK = register("visual_grave", AbstractBlock.Settings.copy(GRAVE_BLOCK).hardness(4)
            .dropsNothing(), VisualGraveBlock::new);
    ContainerGraveBlock CONTAINER_GRAVE_BLOCK = register("container_grave", AbstractBlock.Settings.create().nonOpaque().dynamicBounds().hardness(4),
            ContainerGraveBlock::new);
    TempBlock TEMP_BLOCK = register("temp_block", AbstractBlock.Settings.copy(Blocks.BEDROCK).noCollision(), TempBlock::new);

    GraveCompassItem GRAVE_COMPASS_ITEM = registerItem("grave_compass", GraveCompassItem::new);
    CointainerGraveBlockItem CONTAINER_GRAVE_ITEM = registerItem("visual_grave", (s) -> new CointainerGraveBlockItem(CONTAINER_GRAVE_BLOCK, s.useBlockPrefixedTranslationKey()));
    IconItem ICON_ITEM = registerItem("icon", IconItem::new);

    static <T extends Item> T registerItem(String path, Function<Item.Settings, T> function) {
        var id = Identifier.of("universal_graves", path);
        var key = RegistryKey.of(RegistryKeys.ITEM, id);
        var settings = new Item.Settings();
        settings.registryKey(key);
        var value = function.apply(settings);
        Registry.register(Registries.ITEM, id, value);
        return value;
    }

    static <T extends Block> T register(String path, Function<Block.Settings, T> function) {
        var id = Identifier.of("universal_graves", path);
        var key = RegistryKey.of(RegistryKeys.BLOCK, id);
        var settings = AbstractBlock.Settings.create();
        settings.registryKey(key);
        var value = function.apply(settings);
        Registry.register(Registries.BLOCK, id, value);
        return value;
    }

    static <T extends Block> T register(String path, AbstractBlock.Settings settings, Function<Block.Settings, T> function) {
        var id = Identifier.of("universal_graves", path);
        var key = RegistryKey.of(RegistryKeys.BLOCK, id);
        settings.registryKey(key);
        var value = function.apply(settings);
        Registry.register(Registries.BLOCK, id, value);
        return value;
    }

    static void register() {
        Registry.register(Registries.ENTITY_TYPE, Identifier.of("universal_graves", "xp"), SafeXPEntity.TYPE);
        Registry.register(Registries.DATA_COMPONENT_TYPE, Identifier.of("universal_graves", "compass"), GraveCompassComponent.TYPE);
        Registry.register(Registries.DATA_COMPONENT_TYPE, Identifier.of("universal_graves", "texture"), IconItem.TEXTURE);
        PolymerComponent.registerDataComponent(GraveCompassComponent.TYPE, IconItem.TEXTURE);
        PolymerEntityUtils.registerType(SafeXPEntity.TYPE);
        GraveBlockEntity.BLOCK_ENTITY_TYPE = Registry.register(Registries.BLOCK_ENTITY_TYPE, "universal_graves:grave", FabricBlockEntityTypeBuilder.create(GraveBlockEntity::new, GRAVE_BLOCK).build());
        VisualGraveBlockEntity.BLOCK_ENTITY_TYPE = Registry.register(Registries.BLOCK_ENTITY_TYPE, "universal_graves:visual_grave", FabricBlockEntityTypeBuilder.create(VisualGraveBlockEntity::new,VISUAL_GRAVE_BLOCK).build());
        ContainerGraveBlockEntity.BLOCK_ENTITY_TYPE = Registry.register(Registries.BLOCK_ENTITY_TYPE, "universal_graves:container_grave", FabricBlockEntityTypeBuilder.create(ContainerGraveBlockEntity::new, CONTAINER_GRAVE_BLOCK).build());
        PolymerBlockUtils.registerBlockEntity(GraveBlockEntity.BLOCK_ENTITY_TYPE, VisualGraveBlockEntity.BLOCK_ENTITY_TYPE, ContainerGraveBlockEntity.BLOCK_ENTITY_TYPE);

    }
}
