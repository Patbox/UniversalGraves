package eu.pb4.graves.grave;

import com.mojang.authlib.GameProfile;
import eu.pb4.graves.config.Config;
import eu.pb4.graves.config.ConfigManager;
import eu.pb4.graves.other.GraveUtils;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public final class GraveInfo {
    public static final GraveInfo DEFAULT = new GraveInfo();
    private static final Text DEFAULT_DEATH_CAUSE = new LiteralText("Unknown cause");
    private static final GameProfile DEFAULT_GAME_PROFILE = new GameProfile(UUID.fromString("9586e5ab-157a-4658-ad80-b07552a9ca63"), "Herobrine");

    protected GameProfile gameProfile;
    protected int xp;
    protected long creationTime;
    protected int itemCount;
    protected Text deathCause;
    protected BlockPos position;
    protected Identifier world;
    protected Set<UUID> allowedUUIDs;

    public GraveInfo() {
        this.gameProfile = DEFAULT_GAME_PROFILE;
        this.creationTime = Long.MAX_VALUE;
        this.xp = 0;
        this.itemCount = 0;
        this.deathCause = DEFAULT_DEATH_CAUSE;
        this.position = BlockPos.ORIGIN;
        this.world = ServerWorld.OVERWORLD.getValue();
        this.allowedUUIDs = new HashSet<>();
    }

    public GraveInfo(GameProfile profile, BlockPos position, Identifier world, long creationTime, int xp, int itemCount, Text deathCause, Collection<UUID> allowedUUIDs) {
        this.gameProfile = profile;
        this.creationTime = creationTime;
        this.xp = xp;
        this.itemCount = itemCount;
        this.deathCause = deathCause;
        this.position = position;
        this.world = world;
        this.allowedUUIDs = new HashSet<>(allowedUUIDs);
    }

    public NbtCompound writeNbt(NbtCompound nbt) {
        if (this.gameProfile != null) {
            nbt.put("GameProfile", NbtHelper.writeGameProfile(new NbtCompound(), this.gameProfile));
        }
        nbt.putInt("XP", this.xp);
        nbt.putLong("CreationTime", this.creationTime);
        nbt.putInt("ItemCount", this.itemCount);
        nbt.putString("DeathCause", Text.Serializer.toJson(this.deathCause));
        nbt.putIntArray("Position", new int[] { position.getX(), position.getY(), position.getZ() });
        nbt.putString("World", this.world.toString());

        var list = new NbtList();
        for (var uuid : this.allowedUUIDs) {
            list.add(NbtHelper.fromUuid(uuid));
        }

        nbt.put("AllowedUUIDs", list);
        return nbt;
    }

    public void readNbt(NbtCompound nbt) {
        try {
            this.gameProfile = NbtHelper.toGameProfile(nbt.getCompound("GameProfile"));
            this.xp = nbt.getInt("XP");
            this.creationTime = nbt.getLong("CreationTime");
            this.itemCount = nbt.getInt("ItemCount");
            this.deathCause = Text.Serializer.fromLenientJson(nbt.getString("DeathCause"));
            int[] pos = nbt.getIntArray("Position");
            this.position = new BlockPos(pos[0], pos[1], pos[2]);
            this.world = Identifier.tryParse(nbt.getString("World"));
            this.allowedUUIDs.clear();

            for (var nbtUUID : nbt.getList("AllowedUUIDs", NbtElement.INT_ARRAY_TYPE)) {
                this.allowedUUIDs.add(NbtHelper.toUuid(nbtUUID));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Map<String, Text> getPlaceholders(MinecraftServer server) {
        Config config = ConfigManager.getConfig();

        long currentTime = System.currentTimeMillis() / 1000;

        long protectionTime = config.configData.protectionTime > -1 ? config.configData.protectionTime - currentTime + this.creationTime : Long.MAX_VALUE;
        long breakTime = config.configData.breakingTime > -1 ? config.configData.breakingTime - currentTime + this.creationTime : Long.MAX_VALUE;

        Map<String, Text> values = new HashMap<>();
        values.put("player", new LiteralText(this.gameProfile != null ? this.gameProfile.getName() : "<No player!>"));
        values.put("protection_time", new LiteralText("" + (config.configData.protectionTime > -1 ? config.getFormattedTime(protectionTime) : config.configData.infinityText)));
        values.put("break_time", new LiteralText("" + (config.configData.breakingTime > -1 ? config.getFormattedTime(breakTime) : config.configData.infinityText)));
        values.put("xp", new LiteralText("" + this.xp));
        values.put("item_count", new LiteralText("" + this.itemCount));
        values.put("position", new LiteralText("" + this.position.toShortString()));
        values.put("world", new LiteralText(GraveUtils.toWorldName(this.world)));
        values.put("death_cause", this.deathCause);
        return values;
    }

    public boolean shouldBreak() {
        Config config = ConfigManager.getConfig();

        if (config.configData.breakingTime > -1) {
            long currentTime = System.currentTimeMillis() / 1000;
            long breakTime = config.configData.breakingTime - currentTime + this.creationTime;

            return breakTime <= 0;
        } else {
            return false;
        }
    }

    public boolean isProtected() {
        Config config = ConfigManager.getConfig();

        if (config.configData.protectionTime > -1 && config.configData.isProtected) {
            long currentTime = System.currentTimeMillis() / 1000;
            long protectionTime = config.configData.protectionTime - currentTime + this.creationTime;

            return protectionTime > 0;
        } else {
            return config.configData.isProtected;
        }
    }

    public boolean canTakeFrom(PlayerEntity entity) {
        return !this.isProtected() || this.gameProfile.getId().equals(entity.getUuid()) || this.allowedUUIDs.contains(entity.getUuid()) || Permissions.check(entity, "graves.can_open_others", 3);
    }

    public GameProfile getProfile() {
        return this.gameProfile;
    }

    public int getXp() {
        return xp;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public int getItemCount() {
        return itemCount;
    }

    public Text getDeathCause() {
        return deathCause;
    }

    public BlockPos getPosition() {
        return position;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraveInfo graveInfo = (GraveInfo) o;
        return Objects.equals(position, graveInfo.position);
    }

    @Override
    public int hashCode() {
        return position.hashCode();
    }

    public Identifier getWorld() {
        return this.world;
    }
}
