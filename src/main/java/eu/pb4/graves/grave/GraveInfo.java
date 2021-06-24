package eu.pb4.graves.grave;

import com.mojang.authlib.GameProfile;
import eu.pb4.graves.config.Config;
import eu.pb4.graves.config.ConfigManager;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public class GraveInfo {
    private static final Text DEFAULT_DEATH_CAUSE = new LiteralText("Unknown cause");
    private static final GameProfile DEFAULT_GAME_PROFILE = new GameProfile(UUID.fromString("9586e5ab-157a-4658-ad80-b07552a9ca63"), "Herobrine");

    public GameProfile gameProfile;
    public int xp;
    public long creationTime;
    public int itemCount;
    public Text deathCause;
    public BlockPos position;
    public Identifier world;

    public GraveInfo() {
        this.gameProfile = DEFAULT_GAME_PROFILE;
        this.creationTime = Long.MAX_VALUE;
        this.xp = 0;
        this.itemCount = 0;
        this.deathCause = DEFAULT_DEATH_CAUSE;
        this.position = BlockPos.ORIGIN;
        this.world = ServerWorld.OVERWORLD.getValue();
    }

    public GraveInfo(GameProfile profile, BlockPos position, Identifier world, long creationTime, int xp, int itemCount, Text deathCause) {
        this.gameProfile = profile;
        this.creationTime = creationTime;
        this.xp = xp;
        this.itemCount = itemCount;
        this.deathCause = deathCause;
        this.position = position;
        this.world = world;
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
        return nbt;
    }

    public void readNbt(NbtCompound nbt) {
        this.gameProfile = NbtHelper.toGameProfile(nbt.getCompound("GameProfile"));
        this.xp = nbt.getInt("XP");
        this.creationTime = nbt.getLong("CreationTime");
        this.itemCount = nbt.getInt("ItemCount");
        this.deathCause = Text.Serializer.fromLenientJson(nbt.getString("DeathCause"));
        int[] pos = nbt.getIntArray("Position");
        this.position = new BlockPos(pos[0], pos[1], pos[2]);
        this.world = Identifier.tryParse(nbt.getString("World"));
    }

    public Map<String, Text> getPlaceholders() {
        Config config = ConfigManager.getConfig();

        long currentTime = System.currentTimeMillis() / 1000;

        long protectionTime = config.configData.isProtected ? config.configData.protectionTime - currentTime + this.creationTime : Long.MAX_VALUE;
        long breakTime = config.configData.shouldBreak ? config.configData.breakAfter - currentTime + this.creationTime : Long.MAX_VALUE;

        Map<String, Text> values = new HashMap<>();
        values.put("player", new LiteralText(this.gameProfile != null ? this.gameProfile.getName() : "<No player!>"));
        values.put("protection_time", new LiteralText("" + config.getFormattedTime(protectionTime)));
        values.put("break_time", new LiteralText("" + config.getFormattedTime(breakTime)));
        values.put("xp", new LiteralText("" + this.xp));
        values.put("item_count", new LiteralText("" + this.itemCount));
        values.put("position", new LiteralText("" + this.position.toShortString()));

        List<String> parts = new ArrayList<>();
        {
            String[] words = this.world.getPath().split("_");
            for (String word : words) {
                String[] s = word.split("", 2);
                s[0] = s[0].toUpperCase(Locale.ROOT);
                parts.add(String.join("", s));
            }
        }
        values.put("world", new LiteralText(String.join(" ", parts)));
        values.put("death_cause", this.deathCause);
        return values;
    }

    public boolean shouldBreak() {
        Config config = ConfigManager.getConfig();

        long currentTime = System.currentTimeMillis() / 1000;
        long breakTime = config.configData.shouldBreak ? config.configData.breakAfter - currentTime + this.creationTime : Long.MAX_VALUE;

        return breakTime <= 0;
    }

    public boolean isProtected() {
        Config config = ConfigManager.getConfig();

        long currentTime = System.currentTimeMillis() / 1000;
        long protectionTime = config.configData.isProtected ? config.configData.protectionTime - currentTime + this.creationTime : Long.MAX_VALUE;

        return protectionTime > 0;
    }

    public boolean canTakeFrom(PlayerEntity entity) {
        return !this.isProtected() || this.gameProfile.getId().equals(entity.getUuid()) || Permissions.check(entity, "graves.can_open_others", 3);
    }
}
