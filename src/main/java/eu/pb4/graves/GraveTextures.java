package eu.pb4.graves;

import com.google.common.base.Suppliers;
import eu.pb4.polymer.networking.api.server.PolymerServerNetworking;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.minecraft.nbt.NbtByte;
import net.minecraft.nbt.NbtInt;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;


public final class GraveTextures {
    private static final Identifier IDENTIFIER = Identifier.of("universal_graves", "has_rp");
    private static final Supplier<Text> DEV_TEXTURE = () -> Text.literal("-1.").setStyle(Style.EMPTY.withColor(Formatting.WHITE).withFont(Identifier.of("universal_graves", "gui")));
    private static final Supplier<Text> TEXTURE = GravesMod.DEV ? DEV_TEXTURE : Suppliers.memoize(DEV_TEXTURE::get);

    public static Text get(ServerPlayerEntity player, Text text) {
        return hasGuiTexture(player) ? Text.empty().append(TEXTURE.get()).append(text) : text;
    }

    public static boolean hasGuiTexture(@Nullable ServerPlayerEntity player) {
        var mata = player != null ? PolymerServerNetworking.getMetadata(player.networkHandler, IDENTIFIER, NbtInt.TYPE) : null;
        return PolymerResourcePackUtils.hasMainPack(player)
                || (player != null && player.networkHandler != null && mata != null && mata.intValue() == 1);
    }

    public static void initialize() {
        PolymerServerNetworking.setServerMetadata(IDENTIFIER, NbtInt.of(1));
    }
}
