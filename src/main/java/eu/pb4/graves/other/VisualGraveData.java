package eu.pb4.graves.other;

import com.mojang.authlib.GameProfile;
import eu.pb4.graves.config.Config;
import eu.pb4.graves.config.ConfigManager;
import eu.pb4.graves.grave.Grave;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public record VisualGraveData(GameProfile gameProfile, Text deathCause, long creationTime, Location location, int minecraftDay) {
    public static final VisualGraveData DEFAULT = new VisualGraveData(Grave.DEFAULT_GAME_PROFILE, Grave.DEFAULT_DEATH_CAUSE, 0, new Location(new Identifier("the_void"), BlockPos.ORIGIN), -1);

    public NbtCompound toNbt() {
        var nbt = new NbtCompound();
        nbt.put("GameProfile", NbtHelper.writeGameProfile(new NbtCompound(), this.gameProfile));
        nbt.putString("DeathCause", Text.Serializer.toJson(this.deathCause));
        nbt.putLong("CreationTime", this.creationTime);
        nbt.putInt("MinecraftDay", this.minecraftDay);
        this.location.writeNbt(nbt);
        return nbt;
    }

    public Map<String, Text> getPlaceholders(MinecraftServer server) {
        Config config = ConfigManager.getConfig();

        Map<String, Text> values = new HashMap<>();
        values.put("player", new LiteralText(this.gameProfile != null ? this.gameProfile.getName() : "<No player!>"));
        values.put("protection_time", new LiteralText("" + (config.configData.protectionTime > -1 ? config.getFormattedTime(0) : config.configData.infinityText)));
        values.put("break_time", new LiteralText("" + (config.configData.breakingTime > -1 ? config.getFormattedTime(0) : config.configData.infinityText)));
        values.put("xp", new LiteralText("0"));
        values.put("item_count", new LiteralText("0"));
        values.put("position", new LiteralText("" + this.location.blockPos().toShortString()));
        values.put("world", GraveUtils.toWorldName(this.location.world()));
        values.put("death_cause", this.deathCause);
        values.put("minecraft_day", new LiteralText("" + this.minecraftDay));
        values.put("creation_date", new LiteralText(config.fullDateFormat.format(new Date(this.creationTime * 1000))));
        values.put("since_creation", new LiteralText(config.getFormattedTime(System.currentTimeMillis() / 1000 - this.creationTime)));
        values.put("id", new LiteralText("<no id>"));
        return values;
    }

    public static VisualGraveData fromNbt(NbtCompound nbt) {
        return new VisualGraveData(
                NbtHelper.toGameProfile(nbt.getCompound("GameProfile")),
                Text.Serializer.fromJson(nbt.getString("DeathCause")),
                nbt.getLong("CreationTime"),
                Location.fromNbt(nbt),
                nbt.getInt("MinecraftDay")
        );
    }
}
