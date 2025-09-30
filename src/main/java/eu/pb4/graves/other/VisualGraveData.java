package eu.pb4.graves.other;

import eu.pb4.graves.config.Config;
import eu.pb4.graves.config.ConfigManager;
import eu.pb4.graves.grave.Grave;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public record VisualGraveData(ProfileComponent profile, byte visualSkinModelLayers, Arm mainArm, Text deathCause, long creationTime, Location location, int minecraftDay) {
    public static final VisualGraveData DEFAULT = new VisualGraveData(Grave.DEFAULT_PROFILE_COMPONENT, (byte) 0xFF, Arm.RIGHT, Grave.DEFAULT_DEATH_CAUSE, 0, new Location(Identifier.of("the_void"), BlockPos.ORIGIN), -1);

    public Map<String, Text> getPlaceholders(MinecraftServer server) {
        Config config = ConfigManager.getConfig();

        Map<String, Text> values = new HashMap<>();
        values.put("player", Text.literal(this.profile != null ? this.profile.getName().orElse("<No player!>") : "<No player!>"));
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

    public void writeData(WriteView view) {
        view.put("GameProfile", ProfileComponent.CODEC, this.profile);
        view.put("DeathCause", TextCodecs.CODEC, this.deathCause);
        view.putLong("CreationTime", this.creationTime);
        view.putInt("MinecraftDay", this.minecraftDay);
        view.putByte("SkinModelParts", this.visualSkinModelLayers);
        view.putByte("MainArm", (byte) this.mainArm.getId());
        this.location.writeData(view);
    }


    public static VisualGraveData readData(ReadView view) {
        return new VisualGraveData(
                LegacyNbtHelper.readProfileComponentOrLegacyGameProfile(view.getReadView("GameProfile")).orElse(Grave.DEFAULT_PROFILE_COMPONENT),
                view.getByte("SkinModelParts", (byte) 0xFF),
                view.getByte("MainArm", (byte) 0) == Arm.LEFT.getId() ? Arm.LEFT : Arm.RIGHT,
                view.read("DeathCause", TextCodecs.CODEC).orElse(Text.empty()),
                view.getLong("CreationTime", 0),
                Location.readData(view),
                view.getInt("MinecraftDay", 0)
        );
    }
}
