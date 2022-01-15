package eu.pb4.graves.client;


import eu.pb4.graves.GraveNetworking;
import eu.pb4.graves.registry.AbstractGraveBlockEntity;
import eu.pb4.graves.registry.VisualGraveBlockEntity;
import eu.pb4.graves.registry.GraveBlockEntity;
import eu.pb4.graves.other.GravesLookType;
import eu.pb4.polymer.api.client.PolymerClientUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.util.function.Consumer;

public class GravesModClient implements ClientModInitializer {
    public static final Identifier UI_TEXTURE = new Identifier("universal_graves", "textures/gui/default_ui.png");
    public static ClientModel model = ClientModel.NONE;
    public static GravesLookType serverSideModel = GravesLookType.PLAYER_HEAD;
    public static GraveNetworking.NetworkingConfig config = new GraveNetworking.NetworkingConfig(false, "", false);

    @Override
    public void onInitializeClient() {
        BlockEntityRendererRegistry.register(VisualGraveBlockEntity.BLOCK_ENTITY_TYPE,
                (ctx) -> (BlockEntityRenderer<VisualGraveBlockEntity>) (Object) new GraveRenderer(ctx));
        BlockEntityRendererRegistry.register(GraveBlockEntity.BLOCK_ENTITY_TYPE,
                (ctx) -> (BlockEntityRenderer<GraveBlockEntity>) (Object) new GraveRenderer(ctx));


        PolymerClientUtils.registerPacketHandler(GraveNetworking.SERVER_HELLO, this::handleHelloPacket, 0);
        PolymerClientUtils.registerPacketHandler(GraveNetworking.SERVER_GRAVE, this::handleGravePacket, 0);
        PolymerClientUtils.registerPacketHandler(GraveNetworking.SERVER_UI, this::handleUIPacket, 0);

        ClientPlayConnectionEvents.JOIN.register(this::onConnect);

        ModelLoadingRegistry.INSTANCE.registerModelProvider((ResourceManager manager, Consumer<Identifier> out) -> {
            out.accept(GraveRenderer.GENERIC_GRAVE);
            out.accept(GraveRenderer.GENERIC_UNLOCKED_GRAVE);
        });
    }

    private void onConnect(ClientPlayNetworkHandler handler, PacketSender packetSender, MinecraftClient client) {
        model = ClientModel.NONE;
        config = new GraveNetworking.NetworkingConfig(false, "", false);
    }

    private void handleHelloPacket(ClientPlayNetworkHandler handler, int i, PacketByteBuf buf) {
        var decoded = GraveNetworking.readConfig(i, buf);

        if (decoded.enabled()) {
            for (var possible : ClientModel.values()) {
                if (possible.networkName.equals(decoded.style())) {
                    model = possible;
                    break;
                }
            }
        } else {
            model = ClientModel.NONE;
        }

        config = decoded;

        serverSideModel = GravesLookType.byName(decoded.style());
    }

    private void handleGravePacket(ClientPlayNetworkHandler handler, int i, PacketByteBuf buf) {
        var decoded = GraveNetworking.readGrave(i, buf);

        MinecraftClient.getInstance().execute(() -> {
            var blockEntity = handler.getWorld().getBlockEntity(decoded.pos());

            if (blockEntity instanceof AbstractGraveBlockEntity grave) {
                grave.setFromPacket(decoded);
            }
        });
    }

    private void handleUIPacket(ClientPlayNetworkHandler handler, int i, PacketByteBuf buf) {
        MinecraftClient.getInstance().execute(() -> {
            var screen = MinecraftClient.getInstance().currentScreen;

            if (screen instanceof ClientGraveUi clientGraveUi) {
                clientGraveUi.grave_set();
            }
        });
    }
    public enum ClientModel {
        NONE(""),
        HEAD(GravesLookType.PLAYER_HEAD.networkName),
        GENERIC_GRAVE(GravesLookType.CLIENT_MODEL.networkName);

        private final String networkName;

        ClientModel(String networkName) {
            this.networkName = networkName;
        }
    }
}
