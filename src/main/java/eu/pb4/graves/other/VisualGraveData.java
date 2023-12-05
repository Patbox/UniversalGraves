package eu.pb4.graves.other;

import com.mojang.authlib.GameProfile;
import eu.pb4.graves.config.Config;
import eu.pb4.graves.config.ConfigManager;
import eu.pb4.graves.grave.Grave;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public record VisualGraveData(GameProfile gameProfile, byte visualSkinModelLayers, Arm mainArm, Text deathCause, long creationTime, Location location, int minecraftDay) {
    public static final VisualGraveData DEFAULT = new VisualGraveData(Grave.DEFAULT_GAME_PROFILE, (byte) 0xFF, Arm.RIGHT, Grave.DEFAULT_DEATH_CAUSE, 0, new Location(new Identifier("the_void"), BlockPos.ORIGIN), -1);

    public NbtCompound toNbt() {
        var nbt = new NbtCompound();
        nbt.put("GameProfile", NbtHelper.writeGameProfile(new NbtCompound(), this.gameProfile));
        nbt.putString("DeathCause", Text.Serialization.toJsonString(this.deathCause));
        nbt.putLong("CreationTime", this.creationTime);
        nbt.putInt("MinecraftDay", this.minecraftDay);
        nbt.putByte("SkinModelParts", this.visualSkinModelLayers);
        nbt.putByte("MainArm", (byte) this.mainArm.getId());
        this.location.writeNbt(nbt);
        return nbt;
    }

    public Map<String, Text> getPlaceholders(MinecraftServer server) {
        Config config = ConfigManager.getConfig();

        Map<String, Text> values = new HashMap<>();
        values.put("player", Text.literal(this.gameProfile != null && this.gameProfile.getName() != null ? this.gameProfile.getName() : "<No player!>"));
        values.put("protection_time", Text.literal("" + (config.protection.protectionTime > -1 ? config.getFormattedTime(0) : config.texts.infinityText)));
        values.put("break_time", Text.literal("" + (config.protection.breakingTime > -1 ? config.getFormattedTime(0) : config.texts.infinityText)));
        values.put("xp", Text.literal("0"));
        values.put("item_count", Text.literal("0"));
        values.put("position", Text.literal("" + this.location.blockPos().toShortString()));
        values.put("world", GraveUtils.toWorldName(this.location.world()));
        values.put("death_cause", this.deathCause);
        values.put("minecraft_day", Text.literal("" + this.minecraftDay));
        values.put("creation_date", Text.literal(config.texts.fullDateFormat.format().format(new Date(this.creationTime * 1000))));
        values.put("since_creation", Text.literal(config.getFormattedTime(System.currentTimeMillis() / 1000 - this.creationTime)));
        values.put("id", Text.literal("<no id>"));
        return values;
    }

    public static VisualGraveData fromNbt(NbtCompound nbt) {
        return new VisualGraveData(
                NbtHelper.toGameProfile(nbt.getCompound("GameProfile")),
                nbt.contains("SkinModelParts", NbtElement.BYTE_TYPE) ? nbt.getByte("SkinModelParts") : (byte) 0xFF,
                nbt.contains("MainArm", NbtElement.BYTE_TYPE) ? (nbt.getByte("MainArm") == Arm.LEFT.getId() ? Arm.LEFT : Arm.RIGHT) : Arm.RIGHT,
                Text.Serialization.fromJson(nbt.getString("DeathCause")),
                nbt.getLong("CreationTime"),
                Location.fromNbt(nbt),
                nbt.getInt("MinecraftDay")
        );
    }
}
