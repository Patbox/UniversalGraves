package eu.pb4.graves.grave;

import com.mojang.authlib.GameProfile;
import eu.pb4.graves.config.Config;
import eu.pb4.graves.config.ConfigManager;
import eu.pb4.graves.other.*;
import eu.pb4.graves.ui.GraveGui;
import eu.pb4.placeholders.PlaceholderAPI;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.network.MessageType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.Util;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class Grave {
    private static final Text DEFAULT_DEATH_CAUSE = new LiteralText("Unknown cause");
    private static final GameProfile DEFAULT_GAME_PROFILE = new GameProfile(UUID.fromString("9586e5ab-157a-4658-ad80-b07552a9ca63"), "Herobrine");

    protected GameProfile gameProfile;
    protected int xp;
    protected long creationTime;
    protected long gameCreationTime;
    protected int itemCount;
    protected Text deathCause;
    protected Set<UUID> allowedUUIDs;
    protected DefaultedList<PositionedItemStack> items;
    protected Location location;
    protected GraveType type;
    protected boolean isRemoved;

    protected boolean utilProtectionChangeMessage;
    protected boolean isProtectionEnabled;
    protected VisualGraveData visualData;

    public Grave() {
        this.gameProfile = DEFAULT_GAME_PROFILE;
        this.creationTime = Long.MAX_VALUE;
        this.xp = 0;
        this.itemCount = 0;
        this.deathCause = DEFAULT_DEATH_CAUSE;
        this.allowedUUIDs = new HashSet<>();
        this.items = DefaultedList.of();
        this.type = GraveType.VIRTUAL;
        this.location = new Location(ServerWorld.OVERWORLD.getValue(), BlockPos.ORIGIN);
        this.utilProtectionChangeMessage = true;
        this.isProtectionEnabled = true;
        this.visualData = VisualGraveData.DEFAULT;
    }

    public Grave(GameProfile profile, BlockPos position, Identifier world, GraveType type, long creationTime, long gameCreationTime, int xp, Text deathCause, Collection<UUID> allowedUUIDs, Collection<PositionedItemStack> itemStacks, boolean isProtectionEnabled) {
        this.gameProfile = profile;
        this.creationTime = creationTime;
        this.gameCreationTime = gameCreationTime;
        this.type = type;
        this.xp = xp;
        this.deathCause = deathCause;
        this.allowedUUIDs = new HashSet<>(allowedUUIDs);
        this.location = new Location(world, position);
        this.items = DefaultedList.copyOf(PositionedItemStack.EMPTY, itemStacks.toArray(new PositionedItemStack[0]));
        this.utilProtectionChangeMessage = !this.isProtected();
        this.isProtectionEnabled = isProtectionEnabled;
        this.updateDisplay();
    }

    public static final Grave createBlock(GameProfile profile, Identifier world, BlockPos position, int xp, Text deathCause, Collection<UUID> allowedUUIDs, Collection<PositionedItemStack> itemStacks) {
        return new Grave(profile, position, world, GraveType.BLOCK, System.currentTimeMillis() / 1000, GraveManager.INSTANCE.getCurrentGameTime(), xp, deathCause, allowedUUIDs, itemStacks, true);
    }

    public NbtCompound writeNbt(NbtCompound nbt) {
        if (this.gameProfile != null) {
            nbt.put("GameProfile", NbtHelper.writeGameProfile(new NbtCompound(), this.gameProfile));
        }
        nbt.putInt("XP", this.xp);
        nbt.putLong("CreationTime", this.creationTime);
        nbt.putInt("ItemCount", this.itemCount);
        nbt.putString("DeathCause", Text.Serializer.toJson(this.deathCause));
        nbt.putString("Type", this.type.name());
        nbt.putBoolean("IsProtectionEnabled", this.isProtectionEnabled);
        this.location.writeNbt(nbt);

        var allowedUUIDs = new NbtList();
        for (var uuid : this.allowedUUIDs) {
            allowedUUIDs.add(NbtHelper.fromUuid(uuid));
        }

        nbt.put("AllowedUUIDs", allowedUUIDs);

        var items = new NbtList();
        for (var item : this.items) {
            items.add(item.toNbt());
        }

        nbt.put("Items", items);
        return nbt;
    }

    public void readNbt(NbtCompound nbt) {
        try {
            this.gameProfile = NbtHelper.toGameProfile(nbt.getCompound("GameProfile"));
            this.xp = nbt.getInt("XP");
            this.creationTime = nbt.getLong("CreationTime");
            this.itemCount = nbt.getInt("ItemCount");
            this.deathCause = Text.Serializer.fromLenientJson(nbt.getString("DeathCause"));
            this.location = Location.fromNbt(nbt);
            this.allowedUUIDs.clear();

            if (nbt.contains("Type", NbtElement.STRING_TYPE)) {
                this.type = GraveType.byName(nbt.getString("Type"));
            } else {
                this.type = GraveType.BLOCK;
            }

            if (nbt.contains("TickCreationTime", NbtElement.LONG_TYPE)) {
                this.gameCreationTime = nbt.getLong("TickCreationTime");
            } else {
                this.gameCreationTime = GraveManager.INSTANCE.getCurrentGameTime();
            }

            if (nbt.contains("IsProtectionEnabled", NbtElement.BYTE_TYPE)) {
                this.isProtectionEnabled = nbt.getBoolean("IsProtectionEnabled");
            } else {
                this.isProtectionEnabled = true;
            }

            for (var nbtUUID : nbt.getList("AllowedUUIDs", NbtElement.INT_ARRAY_TYPE)) {
                this.allowedUUIDs.add(NbtHelper.toUuid(nbtUUID));
            }

            for (var item : nbt.getList("Items", NbtElement.COMPOUND_TYPE)) {
                this.items.add(PositionedItemStack.fromNbt((NbtCompound) item));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public VisualGraveData toVisualGraveData() {
        return this.visualData;
    }

    public Map<String, Text> getPlaceholders(MinecraftServer server) {
        Config config = ConfigManager.getConfig();

        long protectionTime = config.configData.protectionTime > -1 ? getTimeLeft(config.configData.protectionTime, config.configData.useRealTime) : Long.MAX_VALUE;
        long breakTime = config.configData.breakingTime > -1 ? getTimeLeft(config.configData.breakingTime, config.configData.useRealTime) : Long.MAX_VALUE;

        Map<String, Text> values = new HashMap<>();
        values.put("player", new LiteralText(this.gameProfile != null ? this.gameProfile.getName() : "<No player!>"));
        values.put("protection_time", new LiteralText("" + (config.configData.protectionTime > -1 ? config.getFormattedTime(protectionTime) : config.configData.infinityText)));
        values.put("break_time", new LiteralText("" + (config.configData.breakingTime > -1 ? config.getFormattedTime(breakTime) : config.configData.infinityText)));
        values.put("xp", new LiteralText("" + this.xp));
        values.put("item_count", new LiteralText("" + this.itemCount));
        values.put("position", new LiteralText("" + this.location.blockPos().toShortString()));
        values.put("world", GraveUtils.toWorldName(this.location.world()));
        values.put("death_cause", this.deathCause);
        return values;
    }

    public boolean shouldNaturallyBreak() {
        Config config = ConfigManager.getConfig();

        if (config.configData.breakingTime > -1) {
            long breakTime = getTimeLeft(config.configData.breakingTime, config.configData.useRealTime);

            return breakTime <= 0;
        } else {
            return false;
        }
    }

    public boolean isProtected() {
        return this.isTimeProtected() && this.isProtectionEnabled;
    }

    public boolean isTimeProtected() {
        Config config = ConfigManager.getConfig();

        if (config.configData.protectionTime > -1 && config.configData.isProtected) {
            long protectionTime = getTimeLeft(config.configData.protectionTime, config.configData.useRealTime);

            return protectionTime > 0;
        } else {
            return config.configData.isProtected;
        }
    }

    public long getTimeLeft(int duration, boolean useRealTime) {
        return useRealTime
                ? this.creationTime + duration - System.currentTimeMillis() / 1000
                : this.gameCreationTime + duration - GraveManager.INSTANCE.getCurrentGameTime();
    }

    public void disableProtection() {
        this.isProtectionEnabled = false;
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

    public Location getLocation() {
        return this.location;
    }

    public List<PositionedItemStack> getItems() {
        return this.items;
    }

    public void openUi(ServerPlayerEntity player, boolean canTake) {
        new GraveGui(player, this, canTake).open();
    }

    public Inventory asInventory() {
        return ImplementedInventory.of(new DefaultedList<>(List.of(), null) {
            public ItemStack get(int index) {
                return Grave.this.items.get(index).stack();
            }

            public ItemStack set(int index, ItemStack element) {
                Validate.notNull(element);
                Grave.this.items.set(index, new PositionedItemStack(element,-1, VanillaInventoryMask.INSTANCE,null));
                return element;
            }

            public void add(int value, ItemStack element) {
                Validate.notNull(element);
                Grave.this.items.add(value, new PositionedItemStack(element,-1, VanillaInventoryMask.INSTANCE,  null));
            }

            public ItemStack remove(int index) {
                return Grave.this.items.remove(index).stack();
            }

            public int size() {
                return Grave.this.items.size();
            }

            public void clear() {
                Grave.this.items.clear();
            }
        });
    }

    public void tick(MinecraftServer server) {
        updateSelf(server);
    }

    public void updateDisplay() {
        int i = 0;

        for (var entry : this.items) {
            if (!entry.isEmpty()) {
                i++;
            }
        }
        this.itemCount = i;

        this.visualData = new VisualGraveData(this.gameProfile, this.deathCause, this.creationTime, this.location);
    }

    public boolean isRemoved() {
        return this.isRemoved;
    }

    public void destroyGrave(MinecraftServer server, @Nullable PlayerEntity breaker) {
        if (this.isRemoved) {
            return;
        }

        var config = ConfigManager.getConfig();
        var owner = server.getPlayerManager().getPlayer(this.gameProfile.getId());

        GraveManager.INSTANCE.remove(this);
        this.isRemoved = true;

        boolean shouldBreak = this.shouldNaturallyBreak();

        if (owner != breaker && owner != null) {
            Text text = null;

            if (!shouldBreak) {
                text = config.graveBrokenMessage;
            } else {
                text = config.graveExpiredMessage;
            }

            if (text != null) {
                owner.sendMessage(PlaceholderAPI.parsePredefinedText(text, PlaceholderAPI.PREDEFINED_PLACEHOLDER_PATTERN, this.getPlaceholders(server)), MessageType.SYSTEM, Util.NIL_UUID);
            }
        }

        if (config.configData.dropItemsAfterExpiring || !shouldBreak) {
            this.dropContent(server);
        }
    }

    public void updateSelf(MinecraftServer server) {
        if (this.isRemoved()) {
            return;
        }

        var config = ConfigManager.getConfig();

        if (tryBreak(server, null)) {
            return;
        }

        if (!this.utilProtectionChangeMessage && !this.isProtected()) {
            this.utilProtectionChangeMessage = true;
            if (config.noLongerProtectedMessage != null) {
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(this.getProfile().getId());
                if (player != null) {
                    player.sendMessage(PlaceholderAPI.parsePredefinedText(config.noLongerProtectedMessage, PlaceholderAPI.PREDEFINED_PLACEHOLDER_PATTERN, this.getPlaceholders(server)), MessageType.SYSTEM, Util.NIL_UUID);
                }
            }
        }
    }

    public boolean tryBreak(MinecraftServer server, @Nullable PlayerEntity player) {
        if (this.shouldNaturallyBreak() || this.isEmpty()) {
            this.destroyGrave(server, player);
            return true;
        }
        return false;
    }

    public boolean isEmpty() {
        for (var stack : this.items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public void quickEquip(ServerPlayerEntity player) {
        if (this.canTakeFrom(player)) {
            for (var item : this.items) {
                if (!item.isEmpty() && item.inventoryMask() != null) {
                    item.inventoryMask().moveToPlayerExactly(player, item.stack(), item.slot(), item.optionalData());
                }
            }
            for (var item : this.items) {
                if (!item.isEmpty()) {
                    if (item.inventoryMask() != null) {
                        item.inventoryMask().moveToPlayerClosest(player, item.stack(), item.slot(), item.optionalData());
                    } else {
                        VanillaInventoryMask.INSTANCE.moveToPlayerClosest(player, item.stack(), -1, null);
                    }
                }
            }
            this.updateSelf(player.getServer());
        }
    }

    public void dropContent(MinecraftServer server) {
        var world = server.getWorld(RegistryKey.of(Registry.WORLD_KEY, this.getLocation().world()));
        var chunk = world.getChunk(ChunkSectionPos.getSectionCoord(this.location.x()), ChunkSectionPos.getSectionCoord(this.location.z()));

        ItemScatterer.spawn(world, this.location.blockPos(), this.asInventory());
        ExperienceOrbEntity.spawn(world, Vec3d.ofCenter(this.location.blockPos()), this.xp);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Grave graveInfo = (Grave) o;
        return Objects.equals(this.location, graveInfo.location);
    }

    @Override
    public int hashCode() {
        return this.location.hashCode();
    }
}
