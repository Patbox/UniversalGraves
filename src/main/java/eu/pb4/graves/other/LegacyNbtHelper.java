package eu.pb4.graves.other;

import com.google.common.collect.ImmutableMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIntArray;
import net.minecraft.nbt.NbtList;
import net.minecraft.storage.ReadView;
import net.minecraft.util.Util;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * Helper methods for handling NBT.
 */
public final class LegacyNbtHelper {
    private LegacyNbtHelper() {
    }

    public static Optional<ProfileComponent> readProfileComponentOrLegacyGameProfile(ReadView view) {
        if (view.contains("Id") || view.contains("Name")) {
            return Optional.ofNullable(toGameProfile(view.read(MapCodec.assumeMapUnsafe(NbtCompound.CODEC)).orElse(new NbtCompound()))).map(ProfileComponent::ofStatic);
        }
        return view.read(MapCodec.assumeMapUnsafe(ProfileComponent.CODEC));
    }

    // Before 1.21.4 this class serialized using int array
    // In 1.21.5 this was changed to Uuids.CODEC (string without dashes)
    public static final Codec<UUID> UUID_CODEC = Codec.withAlternative(Uuids.INT_STREAM_CODEC, Uuids.CODEC);

    @Nullable
    public static GameProfile toGameProfile(NbtCompound nbt) {
        UUID uUID = nbt.contains("Id") ? nbt.get("Id", UUID_CODEC).orElse(Util.NIL_UUID) : Util.NIL_UUID;
        String string = nbt.getString("Name", "");

        try {
            var map = ImmutableMultimap.<String, com.mojang.authlib.properties.Property>builder();
            if (nbt.contains("Properties")) {
                NbtCompound nbtCompound = nbt.getCompoundOrEmpty("Properties");

                for (String string2 : nbtCompound.getKeys()) {
                    NbtList nbtList = nbtCompound.getListOrEmpty(string2);

                    for (int i = 0; i < nbtList.size(); ++i) {
                        NbtCompound nbtCompound2 = nbtList.getCompoundOrEmpty(i);
                        String string3 = nbtCompound2.getString("Value", "");
                        if (nbtCompound2.contains("Signature")) {
                            map.put(string2, new com.mojang.authlib.properties.Property(string2, string3, nbtCompound2.getString("Signature", null)));
                        } else {
                            map.put(string2, new com.mojang.authlib.properties.Property(string2, string3));
                        }
                    }
                }
            }
            return new GameProfile(uUID, string, new PropertyMap(map.build()));
        } catch (Throwable var11) {
            return null;
        }
    }

    public static NbtCompound writeGameProfile(NbtCompound nbt, GameProfile profile) {
        if (!profile.name().isEmpty()) {
            nbt.putString("Name", profile.name());
        }

        if (!profile.id().equals(Util.NIL_UUID)) {
            nbt.put("Id", Uuids.CODEC, profile.id());
        }

        if (!profile.properties().isEmpty()) {
            NbtCompound nbtCompound = new NbtCompound();

            for (String string : profile.properties().keySet()) {
                NbtList nbtList = new NbtList();

                for (com.mojang.authlib.properties.Property property : profile.properties().get(string)) {
                    NbtCompound nbtCompound2 = new NbtCompound();
                    nbtCompound2.putString("Value", property.value());
                    String string2 = property.signature();
                    if (string2 != null) {
                        nbtCompound2.putString("Signature", string2);
                    }

                    nbtList.add(nbtCompound2);
                }

                nbtCompound.put(string, nbtList);
            }

            nbt.put("Properties", nbtCompound);
        }

        return nbt;
    }

    public static NbtIntArray fromUuid(UUID uuid) {
        return new NbtIntArray(Uuids.toIntArray(uuid));
    }

    public static UUID toUuid(NbtElement element) {
        if (element.getNbtType() != NbtIntArray.TYPE) {
            throw new IllegalArgumentException(
                    "Expected UUID-Tag to be of type " + NbtIntArray.TYPE.getCrashReportName() + ", but found " + element.getNbtType().getCrashReportName() + "."
            );
        } else {
            int[] is = ((NbtIntArray) element).getIntArray();
            if (is.length != 4) {
                throw new IllegalArgumentException("Expected UUID-Array to be of length 4, but found " + is.length + ".");
            } else {
                return Uuids.toUuid(is);
            }
        }
    }

    public static BlockPos toBlockPos(NbtCompound nbt) {
        return new BlockPos(nbt.getInt("X", 0), nbt.getInt("Y", 0), nbt.getInt("Z", 0));
    }

    public static NbtCompound fromBlockPos(BlockPos pos) {
        NbtCompound nbtCompound = new NbtCompound();
        nbtCompound.putInt("X", pos.getX());
        nbtCompound.putInt("Y", pos.getY());
        nbtCompound.putInt("Z", pos.getZ());
        return nbtCompound;
    }
}