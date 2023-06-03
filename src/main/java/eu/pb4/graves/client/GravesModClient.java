package eu.pb4.graves.client;


import eu.pb4.graves.GraveNetworking;
import eu.pb4.graves.registry.AbstractGraveBlockEntity;
import eu.pb4.polymer.networking.api.client.PolymerClientNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public class GravesModClient implements ClientModInitializer {
    public static final Identifier UI_TEXTURE = new Identifier("universal_graves", "textures/gui/default_ui.png");

    @Override
    public void onInitializeClient() {
        PolymerClientNetworking.registerPacketHandler(GraveNetworking.SERVER_UI, this::handleUIPacket, 1);
        ClientPlayConnectionEvents.JOIN.register(this::onConnect);
    }

    private void onConnect(ClientPlayNetworkHandler handler, PacketSender packetSender, MinecraftClient client) {
    }

    private void handleUIPacket(ClientPlayNetworkHandler handler, int i, PacketByteBuf buf) {
        MinecraftClient.getInstance().execute(() -> {
            var screen = MinecraftClient.getInstance().currentScreen;

            if (screen instanceof ClientGraveUi clientGraveUi) {
                clientGraveUi.grave_set();
            }
        });
    }
}
