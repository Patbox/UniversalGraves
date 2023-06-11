package eu.pb4.graves;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import eu.pb4.polymer.networking.api.PolymerServerNetworking;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;


public final class GraveTextures {
    private static final Identifier IDENTIFIER = new Identifier("universal_graves", "has_rp");
    private static final Supplier<Text> DEV_TEXTURE = () -> Text.literal("-1.").setStyle(Style.EMPTY.withColor(Formatting.WHITE).withFont(new Identifier("universal_graves", "gui")));
    private static final Supplier<Text> TEXTURE = GravesMod.DEV ? DEV_TEXTURE : Suppliers.memoize(DEV_TEXTURE);

    public static Text get(ServerPlayerEntity player, Text text) {
        return hasGuiTexture(player) ? Text.empty().append(TEXTURE.get()).append(text) : text;
    }

    public static boolean hasGuiTexture(@Nullable ServerPlayerEntity player) {
        return PolymerResourcePackUtils.hasPack(player)
                || (player != null && player.networkHandler != null && PolymerServerNetworking.getSupportedVersion(player.networkHandler, IDENTIFIER) == 0);
    }

    public static void initialize() {
        PolymerServerNetworking.registerSendPacket(IDENTIFIER, 0);
    }
}
