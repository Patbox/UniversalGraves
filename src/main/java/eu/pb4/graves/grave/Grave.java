package eu.pb4.graves.grave;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.DataFixer;
import eu.pb4.graves.config.Config;
import eu.pb4.graves.config.ConfigManager;
import eu.pb4.graves.config.data.WrappedText;
import eu.pb4.graves.mixin.PlayerEntityAccessor;
import eu.pb4.graves.other.*;
import eu.pb4.graves.registry.GraveBlock;
import eu.pb4.graves.registry.GraveBlockEntity;
import eu.pb4.graves.registry.GravesRegistry;
import eu.pb4.graves.ui.GraveGui;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.registry.RegistryKey;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@SuppressWarnings({"unused"})
public final class Grave {
    public static final Text DEFAULT_DEATH_CAUSE = Text.literal("Unknown cause");
    public static final GameProfile DEFAULT_GAME_PROFILE = new GameProfile(UUID.fromString("9586e5ab-157a-4658-ad80-b07552a9ca63"), "Herobrine");
    public static final Grave SOMETHING_BROKE_AGAIN = new Grave();

    @Nullable
    private GameProfile gameProfile;
    private int xp;
    private long creationTime;
    private long gameCreationTime;
    private int itemCount;
    private Text deathCause;
    private final Set<UUID> allowedUUIDs;
    private final DefaultedList<PositionedItemStack> items;
    private Location location;
    private GraveType type;
    private boolean isRemoved;
    private long id = -1;
    private Arm mainArm = Arm.RIGHT;
    private byte skinModelParts = (byte) 0xFF;

    private boolean requirePayment;

    private boolean utilProtectionChangeMessage;
    private boolean isProtectionEnabled;
    private VisualGraveData visualData;
    private int minecraftDay;
    private final Map<Identifier, List<PositionedItemStack>> taggedStacks = new HashMap<>();
    private boolean delayPlayerModel = false;

    public Grave() {
        this.requirePayment = !ConfigManager.getConfig().interactions.cost.isFree();
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
        this.minecraftDay = -1;
    }

    public Grave(long id, @Nullable GameProfile profile, byte visibleLayers, Arm arm, BlockPos position, Identifier world, GraveType type, long creationTime, long gameCreationTime, int xp, Text deathCause, Collection<UUID> allowedUUIDs, Collection<PositionedItemStack> itemStacks, boolean isProtectionEnabled, int minecraftDay) {
        this.requirePayment = !ConfigManager.getConfig().interactions.cost.isFree();
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
        this.id = id;
        this.minecraftDay = minecraftDay;
        this.skinModelParts = visibleLayers;
        this.mainArm = arm;
        for (var item : this.items) {
            this.addTaggedItem(item);
        }

        this.updateDisplay();
    }

    public boolean delayPlayerModel() {
        return this.delayPlayerModel;
    }

    private void addTaggedItem(PositionedItemStack item) {
        for (var tag : item.tags()) {
            this.taggedStacks.computeIfAbsent(tag, Grave::createList).add(item);
        }
    }

    private void removeTaggedItem(PositionedItemStack item) {
        for (var tag : item.tags()) {
            var stacks = this.taggedStacks.get(tag);

            if (stacks != null) {
                stacks.remove(item);
                if (stacks.isEmpty()) {
                    this.taggedStacks.remove(tag);
                }
            }
        }
    }

    private static List<PositionedItemStack> createList(Identifier identifier) {
        return new ArrayList<>();
    }

    public static Grave createBlock(ServerPlayerEntity player, Identifier world, BlockPos position, int xp, Text deathCause, Collection<UUID> allowedUUIDs, Collection<PositionedItemStack> itemStacks, int minecraftDay) {
        return new Grave(GraveManager.INSTANCE.requestId(), player.getGameProfile(), player.getDataTracker().get(PlayerEntityAccessor.getPLAYER_MODEL_PARTS()), player.getMainArm(), position, world, GraveType.BLOCK, System.currentTimeMillis() / 1000, GraveManager.INSTANCE.getCurrentGameTime(), xp, deathCause, allowedUUIDs, itemStacks, true, minecraftDay);
    }

    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        if (this.gameProfile != null) {
            nbt.put("GameProfile", LegacyNbtHelper.writeGameProfile(new NbtCompound(), this.gameProfile));
        }
        nbt.putLong("Id", this.id);
        nbt.putInt("XP", this.xp);
        nbt.putLong("CreationTime", this.creationTime);
        nbt.putInt("ItemCount", this.itemCount);
        nbt.putInt("MinecraftDay", this.minecraftDay);
        nbt.putString("DeathCause", Text.Serialization.toJsonString(this.deathCause, lookup));
        nbt.putString("Type", this.type.name());
        nbt.putBoolean("IsProtectionEnabled", this.isProtectionEnabled);
        nbt.putBoolean("RequirePayment", this.requirePayment);

        this.location.writeNbt(nbt);

        var allowedUUIDs = new NbtList();
        for (var uuid : this.allowedUUIDs) {
            allowedUUIDs.add(NbtHelper.fromUuid(uuid));
        }

        nbt.put("AllowedUUIDs", allowedUUIDs);

        var items = new NbtList();
        for (var item : this.items) {
            items.add(item.toNbt(lookup));
        }

        nbt.put("Items", items);
        nbt.putByte("SkinModelParts", this.skinModelParts);
        nbt.putByte("MainArm", (byte) this.mainArm.getId());
        return nbt;
    }

    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup, DataFixer dataFixer, int dataVersion, int currentDataVersion) {
        try {
            if (nbt.contains("Id", NbtElement.LONG_TYPE)) {
                this.id = nbt.getLong("Id");
            } else {
                this.id = GraveManager.INSTANCE.requestId();
            }

            this.gameProfile = LegacyNbtHelper.toGameProfile(nbt.getCompound("GameProfile"));
            this.xp = nbt.getInt("XP");
            this.creationTime = nbt.getLong("CreationTime");
            this.itemCount = nbt.getInt("ItemCount");
            this.minecraftDay = nbt.getInt("MinecraftDay");
            this.deathCause = Text.Serialization.fromLenientJson(nbt.getString("DeathCause"), lookup);
            this.location = Location.fromNbt(nbt);
            this.allowedUUIDs.clear();
            this.requirePayment = nbt.getBoolean("RequirePayment");

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
                var stack = PositionedItemStack.fromNbt((NbtCompound) item, lookup, dataFixer, dataVersion, currentDataVersion);
                this.items.add(stack);
                this.addTaggedItem(stack);
            }
            if (nbt.contains("SkinModelParts", NbtElement.BYTE_TYPE)) {
                this.skinModelParts = nbt.getByte("SkinModelParts");
            }

            if (nbt.contains("MainArm", NbtElement.BYTE_TYPE)) {
                this.mainArm = nbt.getByte("MainArm") == Arm.LEFT.getId() ? Arm.LEFT : Arm.RIGHT;
            }

            this.updateDisplay();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public VisualGraveData toVisualGraveData() {
        if (this.visualData == null) {
            this.updateDisplay();
        }
        return this.visualData;
    }

    public Map<String, Text> getPlaceholders(MinecraftServer server) {
        Config config = ConfigManager.getConfig();

        long protectionTime = GraveManager.INSTANCE.getProtectionTime() > -1 ? getTimeLeft(GraveManager.INSTANCE.getProtectionTime(), config.protection.useRealTime) : Long.MAX_VALUE;
        long breakTime = GraveManager.INSTANCE.getBreakingTime() > -1 ? getTimeLeft(GraveManager.INSTANCE.getBreakingTime(), config.protection.useRealTime) : Long.MAX_VALUE;

        Map<String, Text> values = new HashMap<>();
        values.put("player", Text.literal(this.gameProfile != null && this.gameProfile.getName() != null ? this.gameProfile.getName() : "<No player!>"));
        values.put("protection_time", Text.literal("" + (GraveManager.INSTANCE.getProtectionTime() > -1 ? config.getFormattedTime(protectionTime) : config.texts.infinityText)));
        values.put("break_time", Text.literal("" + (GraveManager.INSTANCE.getBreakingTime() > -1 ? config.getFormattedTime(breakTime) : config.texts.infinityText)));
        values.put("xp", Text.literal("" + this.xp));
        values.put("item_count", Text.literal("" + this.itemCount));
        values.put("position", Text.literal("" + this.location.blockPos().toShortString()));
        values.put("world", GraveUtils.toWorldName(this.location.world()));
        values.put("death_cause", this.deathCause);
        values.put("minecraft_day", Text.literal("" + this.minecraftDay));
        values.put("creation_date", Text.literal(config.texts.fullDateFormat.format().format(new Date(this.creationTime * 1000))));
        values.put("since_creation", Text.literal(config.getFormattedTime(System.currentTimeMillis() / 1000 - this.creationTime)));
        values.put("id", Text.literal("" + this.id));
        values.put("cost", config.interactions.cost.toText());
        return values;
    }

    public boolean shouldNaturallyBreak() {
        var time = GraveManager.INSTANCE.getBreakingTime();

        if (time > -1) {
            long breakTime = getTimeLeft(time, ConfigManager.getConfig().protection.useRealTime);

            return breakTime <= 0;
        } else {
            return false;
        }
    }

    public boolean isProtected() {
        return this.isTimeProtected() && this.isProtectionEnabled;
    }

    public boolean isTimeProtected() {
        var time = GraveManager.INSTANCE.getProtectionTime();

        if (time > -1 && GraveManager.INSTANCE.isProtectionEnabled()) {
            long protectionTime = getTimeLeft(time, ConfigManager.getConfig().protection.useRealTime);

            return protectionTime > 0;
        } else {
            return GraveManager.INSTANCE.isProtectionEnabled();
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

    public boolean canTakeFrom(GameProfile profile) {
        return !this.requirePayment && this.hasAccess(profile);
    }

    public boolean canTakeFrom(PlayerEntity entity) {
        return this.canTakeFrom(entity.getGameProfile()) || (entity.isCreative() && Permissions.check(entity.getCommandSource((ServerWorld) entity.getWorld()), "graves.can_open_others", 3));
    }

    public boolean hasAccess(PlayerEntity entity) {
        return hasAccess(entity.getGameProfile()) || (entity.isCreative() && Permissions.check(entity.getCommandSource((ServerWorld) entity.getWorld()), "graves.can_open_others", 3));
    }

    public boolean hasAccess(GameProfile profile) {
        return !this.isProtected() || (this.gameProfile != null && this.gameProfile.getId().equals(profile.getId())) || this.allowedUUIDs.contains(profile.getId());
    }

    public boolean payForUnlock(ServerPlayerEntity player) {
        var cfg = ConfigManager.getConfig();
        if (!hasAccess(player.getGameProfile())) {
            if (!cfg.texts.cantPayForThisGrave.isEmpty()) {
                player.sendMessage(cfg.texts.cantPayForThisGrave.text());
            }
            return false;
        }

        if (cfg.interactions.cost.checkCost(player)) {
            cfg.interactions.cost.takeCost(player);
            this.requirePayment = false;
            if (!cfg.texts.graveUnlocked.isEmpty()) {
                player.sendMessage(cfg.texts.graveUnlocked.with(cfg.interactions.cost.getPlaceholders()));
            }
            if (player.server.getWorld(RegistryKey.of(RegistryKeys.WORLD, this.location.world())).getBlockEntity(location.blockPos()) instanceof GraveBlockEntity entity) {
                entity.setModelId(entity.getGraveModelId());
            }

            return true;
        }
        player.sendMessage(cfg.texts.graveNotEnoughCost.with(cfg.interactions.cost.getPlaceholders()));

        return false;
    }

    public GameProfile getProfile() {
        return this.gameProfile != null ? this.gameProfile : DEFAULT_GAME_PROFILE;
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

    public void setLocation(Location location) {
        GraveManager.INSTANCE.moveToLocation(this, location);
        this.location = location;
    }

    public boolean moveTo(MinecraftServer server, Location location) {
        var world = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, location.world()));

        if (world != null) {
            var state = world.getBlockState(location.blockPos());

            if (GraveUtils.canReplaceState(state, ConfigManager.getConfig().placement.replaceAnyBlock)
                    && world.getWorldBorder().contains(location.blockPos()) && location.y() >= world.getBottomY() && location.y() <= world.getTopYInclusive()) {

                var old = this.location;
                this.setLocation(location);

                {
                    var oldWorld = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, old.world()));

                    if (oldWorld != null) {
                        var oldChunk = oldWorld.getChunk(ChunkSectionPos.getSectionCoord(old.x()), ChunkSectionPos.getSectionCoord(old.z()));

                        if (oldChunk.getBlockEntity(old.blockPos()) instanceof GraveBlockEntity grave) {
                            grave.setGrave(null);
                            grave.breakBlock(false);
                        }
                    }
                }

                world.setBlockState(location.blockPos(), GravesRegistry.GRAVE_BLOCK.getDefaultState());

                if (world.getBlockEntity(location.blockPos()) instanceof GraveBlockEntity entity) {
                    entity.setGrave(this, state);
                }
                return true;
            }
        }
        return false;
    }

    public void setLocation(Identifier identifier, BlockPos pos) {
        setLocation(new Location(identifier, pos));
    }

    public List<PositionedItemStack> getItems() {
        return this.items;
    }

    public void openUi(ServerPlayerEntity player, boolean canModify, boolean canFetch) {
        new GraveGui(player, this, canModify, canFetch).open();
    }

    public Inventory asInventory() {
        return ImplementedInventory.of(new DefaultedList<>(List.of(), null) {
            @NotNull
            public ItemStack get(int index) {
                return Grave.this.items.get(index).stack();
            }

            public ItemStack set(int index, ItemStack element) {
                Validate.notNull(element);
                var old = Grave.this.items.set(index, new PositionedItemStack(element, -1, VanillaInventoryMask.INSTANCE, null, Set.of()));
                if (old != null) {
                    Grave.this.removeTaggedItem(old);
                    GraveManager.INSTANCE.markDirty();
                    return old.stack();
                }
                return ItemStack.EMPTY;
            }

            public void add(int value, ItemStack element) {
                Validate.notNull(element);
                Grave.this.items.add(value, new PositionedItemStack(element, -1, VanillaInventoryMask.INSTANCE, null, Set.of()));
                GraveManager.INSTANCE.markDirty();
            }

            public ItemStack remove(int index) {
                var x = Grave.this.items.remove(index);
                Grave.this.removeTaggedItem(x);
                GraveManager.INSTANCE.markDirty();
                return x.stack();
            }

            public int size() {
                return Grave.this.items.size();
            }

            public void clear() {
                Grave.this.items.clear();
                GraveManager.INSTANCE.markDirty();
            }
        }, GraveManager.INSTANCE::markDirty);
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

        this.visualData = new VisualGraveData(this.getProfile(), this.skinModelParts, this.mainArm, this.deathCause, this.creationTime, this.location, this.minecraftDay);
    }

    public boolean isRemoved() {
        return this.isRemoved;
    }

    public boolean isPaymentRequired() {
        return this.requirePayment;
    }

    public byte visibleSkinModelParts() {
        return skinModelParts;
    }

    public Arm mainArm() {
        return mainArm;
    }

    public void destroyGrave(MinecraftServer server, @Nullable PlayerEntity breaker) {
        if (this.isRemoved) {
            return;
        }

        var config = ConfigManager.getConfig();
        var owner = this.gameProfile != null ? server.getPlayerManager().getPlayer(this.gameProfile.getId()) : null;

        GraveManager.INSTANCE.remove(this);
        this.isRemoved = true;

        boolean shouldBreak = this.shouldNaturallyBreak();

        if (owner != breaker && owner != null) {
            WrappedText text;

            if (!shouldBreak) {
                text = config.texts.messageGraveBroken;
            } else {
                text = config.texts.messageGraveExpired;
            }

            if (!text.isEmpty()) {
                owner.sendMessage(text.with(this.getPlaceholders(server)));
            }
        }

        var world = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, this.getLocation().world()));

        if (world != null) {
            var chunk = world.getChunk(ChunkSectionPos.getSectionCoord(this.location.x()), ChunkSectionPos.getSectionCoord(this.location.z()));

            if (config.protection.dropItemsAfterExpiring || !shouldBreak) {
                ItemScatterer.spawn(world, this.location.blockPos(), this.asInventory());
                GraveUtils.spawnExp(world, Vec3d.ofCenter(this.location.blockPos()), this.xp);
            }

            if (chunk.getBlockEntity(this.location.blockPos()) instanceof GraveBlockEntity grave) {
                grave.breakBlock();
            }
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

        if (config.placement.activelyMoveInsideBorder && config.placement.moveInsideBorder) {
            var world = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, this.getLocation().world()));
            if (world != null && !world.getWorldBorder().contains(this.location.blockPos())) {
                var newPos = GraveUtils.findGravePosition(this.gameProfile, null, world, this.location.blockPos(), config.placement.maxPlacementDistance, config.placement.replaceAnyBlock);
                if (newPos.result().canCreate()) {
                    this.moveTo(server, this.location.withPos(newPos.pos()));
                }
            }
        }

        if (!this.utilProtectionChangeMessage && !this.isProtected()) {
            this.utilProtectionChangeMessage = true;
            if (!config.texts.messageProtectionEnded.isEmpty()) {
                ServerPlayerEntity player = this.gameProfile != null ? server.getPlayerManager().getPlayer(this.gameProfile.getId()) : null;
                if (player != null) {
                    player.sendMessage(config.texts.messageProtectionEnded.with(this.getPlaceholders(server)));
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
        if (ConfigManager.getConfig().storage.canStoreOnlyXp && this.xp != 0) {
            return false;
        }

        for (var stack : this.items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public void quickEquip(ServerPlayerEntity player) {
        try {
            if (player.isAlive() && this.hasAccess(player)) {
                if (this.requirePayment && !this.payForUnlock(player)) {
                    return;
                }

                for (var item : this.items) {
                    if (!item.isEmpty() && item.inventoryMask() != null) {
                        item.inventoryMask().moveToPlayerExactly(player, item.stack(), item.slot(), item.optionalData());
                    }
                }
                for (var item : this.items) {
                    if (!item.isEmpty()) {
                        if (item.inventoryMask() == null || !item.inventoryMask().moveToPlayerClosest(player, item.stack(), item.slot(), item.optionalData())) {
                            VanillaInventoryMask.INSTANCE.moveToPlayerClosest(player, item.stack(), -1, null);
                        }
                    }
                }
                GraveUtils.grandExperience(player, this.xp);
                this.xp = 0;
                this.tryBreak(player.getServer(), player);
                this.updateSelf(player.getServer());
                GraveManager.INSTANCE.markDirty();
            }
        } catch (Exception e) {
            e.printStackTrace();
            GraveManager.INSTANCE.markDirty();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Grave graveInfo = (Grave) o;
        return this.id == graveInfo.id;
    }

    @Override
    public int hashCode() {
        return (int) (31 * this.id);
    }

    public long getId() {
        return this.id;
    }

    void setId(long requestId) {
        this.id = requestId;
    }

    public ItemStack getTaggedItem(Identifier identifier) {
        var list = this.taggedStacks.get(identifier);
        if (list != null && !list.isEmpty()) {
            return list.getFirst().stack();
        }
        return ItemStack.EMPTY;
    }

    public boolean isOwner(ServerPlayerEntity player) {
        return this.gameProfile != null && player.getUuid().equals(this.gameProfile.getId());
    }
}
