package eu.pb4.graves;

import eu.pb4.graves.config.ConfigManager;
import eu.pb4.graves.other.VisualGraveData;
import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.polymer.core.api.utils.PolymerSyncUtils;
import eu.pb4.polymer.networking.api.PolymerServerNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.server.translations.api.Localization;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class GraveNetworking {
    public static final Identifier SERVER_UI = new Identifier("universal_graves", "set_ui");

    public static boolean canReceiveGui(@Nullable ServerPlayNetworkHandler handler) {
        return handler != null && PolymerServerNetworking.getSupportedVersion(handler, SERVER_UI) == 0;
    }

    public static void sendGraveUi(ServerPlayNetworkHandler handler) {
        var version = PolymerServerNetworking.getSupportedVersion(handler, SERVER_UI);
        if (version != -1) {
            PolymerServerNetworking.sendDirect(handler, SERVER_UI, PolymerServerNetworking.buf(version));
        }
    }

    public static void initialize() {
        PolymerServerNetworking.registerSendPacket(SERVER_UI, 1);
    }
}
