package eu.pb4.graves;

import eu.pb4.graves.compat.TrinketsCompat;
import eu.pb4.graves.grave.GraveBlock;
import eu.pb4.graves.grave.GraveBlockEntity;
import eu.pb4.graves.config.ConfigManager;
import eu.pb4.graves.grave.GraveManager;
import eu.pb4.graves.other.Commands;
import eu.pb4.polymer.PolymerMod;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GravesMod implements ModInitializer {
	public static final Logger LOGGER = LogManager.getLogger("Universal Graves");
	public static String VERSION = FabricLoader.getInstance().getModContainer("universal-graves").get().getMetadata().getVersion().getFriendlyString();

	@Override
	public void onInitialize() {
		Registry.register(Registry.BLOCK, new Identifier("universal_graves", "grave"), GraveBlock.INSTANCE);
		GraveBlockEntity.BLOCK_ENTITY_TYPE = Registry.register(Registry.BLOCK_ENTITY_TYPE, "universal_graves:grave", FabricBlockEntityTypeBuilder.create(GraveBlockEntity::new, GraveBlock.INSTANCE).build(null));
		Commands.register();
		PolymerMod.registerVirtualBlockEntity(new Identifier("universal_graves", "grave"));
		FabricLoader loader = FabricLoader.getInstance();

		if (loader.isModLoaded("trinkets")) {
			TrinketsCompat.register();
		}

		ServerLifecycleEvents.SERVER_STARTING.register((server) -> ConfigManager.loadConfig());
		ServerLifecycleEvents.SERVER_STARTED.register((server ->
				GraveManager.INSTANCE = (GraveManager) server.getOverworld().getPersistentStateManager().getOrCreate((nbtCompound) -> GraveManager.fromNbt(nbtCompound),
				() -> new GraveManager(),
				"universal-graves"))
		);
	}


}
