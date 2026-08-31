package eu.pb4.graves.grave;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.DataFixer;
import eu.pb4.graves.config.BaseGson;
import eu.pb4.graves.config.Config;
import eu.pb4.graves.config.ConfigManager;
import eu.pb4.graves.config.data.WrappedText;
import eu.pb4.graves.mixin.PlayerLikeEntityAccessor;
import eu.pb4.graves.other.*;
import eu.pb4.graves.registry.GraveBlockEntity;
import eu.pb4.graves.registry.GravesRegistry;
import eu.pb4.graves.ui.GraveGui;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.SectionPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static eu.pb4.graves.GravesMod.id;

@SuppressWarnings({"unused"})
public final class Grave {
    public static final Component DEFAULT_DEATH_CAUSE = Component.literal("Unknown cause");
    public static final GameProfile DEFAULT_GAME_PROFILE = new GameProfile(UUID.fromString("9586e5ab-157a-4658-ad80-b07552a9ca63"), "Herobrine");
    public static final ResolvableProfile DEFAULT_PROFILE_COMPONENT = ResolvableProfile.createResolved(DEFAULT_GAME_PROFILE);
    public static final Grave SOMETHING_BROKE_AGAIN = new Grave();

    @Nullable
    private GameProfile gameProfile;
    private int xp;
    private long creationTime;
    private long gameCreationTime;
    private int itemCount;
    private Component deathCause;
    private final Set<UUID> allowedUUIDs;
    private final Set<UUID> paidUnlockUUIDs;
    private final NonNullList<PositionedItemStack> items;
    private Location location;
    private GraveType type;
    private boolean isRemoved;
    private long id = -1;
    private HumanoidArm mainArm = HumanoidArm.RIGHT;
    private byte skinModelParts = (byte) 0xFF;

    private boolean requirePayment;

    private boolean utilProtectionChangeMessage;
    private boolean isProtectionEnabled;
    private VisualGraveData visualData;
    private int minecraftDay;
    private final Map<Identifier, List<PositionedItemStack>> taggedStacks = new HashMap<>();
    private boolean delayPlayerModel = false;

    public Grave() {
        this.requirePayment = ConfigManager.getConfig().interactions.cost.requiresPayment();
        this.gameProfile = DEFAULT_GAME_PROFILE;
        this.creationTime = Long.MAX_VALUE;
        this.xp = 0;
        this.itemCount = 0;
        this.deathCause = DEFAULT_DEATH_CAUSE;
        this.allowedUUIDs = new HashSet<>();
        this.paidUnlockUUIDs = new HashSet<>();
        this.items = NonNullList.create();
        this.type = GraveType.VIRTUAL;
        this.location = new Location(ServerLevel.OVERWORLD.identifier(), BlockPos.ZERO);
        this.utilProtectionChangeMessage = true;
        this.isProtectionEnabled = true;
        this.visualData = VisualGraveData.DEFAULT;
        this.minecraftDay = -1;
    }

    public Grave(long id, @Nullable GameProfile profile, byte visibleLayers, HumanoidArm arm, BlockPos position, Identifier world, GraveType type, long creationTime, long gameCreationTime, int xp, Component deathCause, Collection<UUID> allowedUUIDs, Collection<PositionedItemStack> itemStacks, boolean isProtectionEnabled, int minecraftDay) {
        this.requirePayment = ConfigManager.getConfig().interactions.cost.requiresPayment();
        this.gameProfile = profile;
        this.creationTime = creationTime;
        this.gameCreationTime = gameCreationTime;
        this.type = type;
        this.xp = xp;
        this.deathCause = deathCause;
        this.allowedUUIDs = new HashSet<>(allowedUUIDs);
        this.paidUnlockUUIDs = new HashSet<>();
        this.location = new Location(world, position);
        this.items = NonNullList.of(PositionedItemStack.EMPTY, itemStacks.toArray(new PositionedItemStack[0]));
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

    public static Grave createBlock(ServerPlayer player, Identifier world, BlockPos position, int xp, Component deathCause, Collection<UUID> allowedUUIDs, Collection<PositionedItemStack> itemStacks, int minecraftDay) {
        return new Grave(GraveManager.INSTANCE.requestId(), player.getGameProfile(), player.getEntityData().get(PlayerLikeEntityAccessor.getDATA_PLAYER_MODE_CUSTOMISATION()), player.getMainArm(), position, world, GraveType.BLOCK, System.currentTimeMillis() / 1000, GraveManager.INSTANCE.getCurrentGameTime(), xp, deathCause, allowedUUIDs, itemStacks, true, minecraftDay);
    }

    public CompoundTag writeNbt(CompoundTag nbt, HolderLookup.Provider lookup) {
        if (this.gameProfile != null) {
            nbt.put("GameProfile", LegacyNbtHelper.writeGameProfile(new CompoundTag(), this.gameProfile));
        }
        nbt.putLong("Id", this.id);
        nbt.putInt("XP", this.xp);
        nbt.putLong("CreationTime", this.creationTime);
        nbt.putInt("ItemCount", this.itemCount);
        nbt.putInt("MinecraftDay", this.minecraftDay);
        nbt.putString("DeathCause", BaseGson.text(lookup).toJsonString(this.deathCause));
        nbt.putString("Type", this.type.name());
        nbt.putBoolean("IsProtectionEnabled", this.isProtectionEnabled);
        nbt.putBoolean("RequirePayment", this.requirePayment);

        this.location.writeData(nbt);

        var allowedUUIDs = new ListTag();
        for (var uuid : this.allowedUUIDs) {
            allowedUUIDs.add(UUIDUtil.LENIENT_CODEC.encodeStart(NbtOps.INSTANCE, uuid).getOrThrow());
        }

        nbt.put("AllowedUUIDs", allowedUUIDs);

        var paidUnlockUUIDs = new ListTag();
        for (var uuid : this.paidUnlockUUIDs) {
            paidUnlockUUIDs.add(UUIDUtil.LENIENT_CODEC.encodeStart(NbtOps.INSTANCE, uuid).getOrThrow());
        }
        nbt.put("PaidUnlockUUIDs", paidUnlockUUIDs);

        var items = new ListTag();
        for (var item : this.items) {
            items.add(item.toNbt(lookup));
        }

        nbt.put("Items", items);
        nbt.putByte("SkinModelParts", this.skinModelParts);
        nbt.putByte("MainArm", (byte) this.mainArm.ordinal());
        return nbt;
    }

    public void readNbt(CompoundTag nbt, HolderLookup.Provider lookup, DataFixer dataFixer, int dataVersion, int currentDataVersion) {
        try {
            if (nbt.contains("Id")) {
                this.id = nbt.getLongOr("Id", -1);
            } else {
                this.id = GraveManager.INSTANCE.requestId();
            }

            this.gameProfile = LegacyNbtHelper.toGameProfile(nbt.getCompoundOrEmpty("GameProfile"));
            this.xp = nbt.getIntOr("XP", 0);
            this.creationTime = nbt.getLongOr("CreationTime", 0);
            this.itemCount = nbt.getIntOr("ItemCount", 0);
            this.minecraftDay = nbt.getIntOr("MinecraftDay", 0);
            this.deathCause = BaseGson.text(lookup).fromJson(nbt.getStringOr("DeathCause", ""));
            this.location = Location.readData(nbt);
            this.allowedUUIDs.clear();
            this.paidUnlockUUIDs.clear();
            this.requirePayment = nbt.getBooleanOr("RequirePayment", false);

            if (nbt.contains("Type")) {
                this.type = GraveType.byName(nbt.getStringOr("Type", ""));
            } else {
                this.type = GraveType.BLOCK;
            }

            if (nbt.contains("TickCreationTime")) {
                this.gameCreationTime = nbt.getLongOr("TickCreationTime", 0);
            } else {
                this.gameCreationTime = GraveManager.INSTANCE.getCurrentGameTime();
            }

            if (nbt.contains("IsProtectionEnabled")) {
                this.isProtectionEnabled = nbt.getBooleanOr("IsProtectionEnabled", false);
            } else {
                this.isProtectionEnabled = true;
            }

            for (var nbtUUID : nbt.getListOrEmpty("AllowedUUIDs")) {
                this.allowedUUIDs.add(UUIDUtil.AUTHLIB_CODEC.decode(NbtOps.INSTANCE, nbtUUID).getOrThrow().getFirst());
            }
            for (var nbtUUID : nbt.getListOrEmpty("PaidUnlockUUIDs")) {
                this.paidUnlockUUIDs.add(UUIDUtil.AUTHLIB_CODEC.decode(NbtOps.INSTANCE, nbtUUID).getOrThrow().getFirst());
            }

            for (var item : nbt.getListOrEmpty("Items")) {
                var stack = PositionedItemStack.fromNbt((CompoundTag) item, lookup, dataFixer, dataVersion, currentDataVersion);
                this.items.add(stack);
                this.addTaggedItem(stack);
            }
            if (nbt.contains("SkinModelParts")) {
                this.skinModelParts = nbt.getByteOr("SkinModelParts", (byte) 0);
            }

            if (nbt.contains("MainArm")) {
                this.mainArm = nbt.getByteOr("MainArm", (byte) 0) == HumanoidArm.LEFT.ordinal() ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
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

    public Map<String, Component> getPlaceholders(MinecraftServer server) {
        Config config = ConfigManager.getConfig();

        long protectionTime = GraveManager.INSTANCE.getProtectionTime() > -1 ? getTimeLeft(GraveManager.INSTANCE.getProtectionTime(), config.protection.useRealTime) : Long.MAX_VALUE;
        long breakTime = GraveManager.INSTANCE.getBreakingTime() > -1 ? getTimeLeft(GraveManager.INSTANCE.getBreakingTime(), config.protection.useRealTime) : Long.MAX_VALUE;

        Map<String, Component> values = new HashMap<>();
        values.put("player", Component.literal(this.gameProfile != null && this.gameProfile.name() != null ? this.gameProfile.name() : "<No player!>"));
        values.put("protection_time", Component.literal("" + (GraveManager.INSTANCE.getProtectionTime() > -1 ? config.getFormattedTime(protectionTime) : config.texts.infinityText)));
        values.put("break_time", Component.literal("" + (GraveManager.INSTANCE.getBreakingTime() > -1 ? config.getFormattedTime(breakTime) : config.texts.infinityText)));
        values.put("xp", Component.literal("" + this.xp));
        values.put("item_count", Component.literal("" + this.itemCount));
        values.put("position", Component.literal("" + this.location.blockPos().toShortString()));
        values.put("world", GraveUtils.toWorldName(this.location.world()));
        values.put("death_cause", this.deathCause);
        values.put("minecraft_day", Component.literal("" + this.minecraftDay));
        values.put("creation_date", Component.literal(config.texts.fullDateFormat.format().format(new Date(this.creationTime * 1000))));
        values.put("since_creation", Component.literal(config.getFormattedTime(System.currentTimeMillis() / 1000 - this.creationTime)));
        values.put("id", Component.literal("" + this.id));
        values.put("cost", config.interactions.cost.baseQuote(this).toText());
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
        return !this.isPaymentRequired(profile.id()) && this.hasAccess(profile);
    }

    public boolean canTakeFrom(NameAndId profile) {
        return !this.isPaymentRequired(profile.id()) && this.hasAccess(profile);
    }

    public boolean canTakeFrom(Player entity) {
        return this.canTakeFrom(entity.getGameProfile()) || (entity.isCreative() && FabricPermissionBridge.checkPermission(entity.createCommandSourceStackForNameResolution((ServerLevel) entity.level()), id("can_open_others"), PermissionLevel.ADMINS));
    }

    public boolean hasAccess(Player entity) {
        return hasAccess(entity.getGameProfile()) || (entity.isCreative() && FabricPermissionBridge.checkPermission(entity.createCommandSourceStackForNameResolution((ServerLevel) entity.level()), id("can_open_others"), PermissionLevel.ADMINS));
    }

    public boolean hasAccess(GameProfile profile) {
        return this.hasProtectionAccess(profile) || this.paidUnlockUUIDs.contains(profile.id());
    }

    public boolean hasAccess(NameAndId profile) {
        return this.hasProtectionAccess(profile) || this.paidUnlockUUIDs.contains(profile.id());
    }

    public boolean hasProtectionAccess(Player entity) {
        return this.hasProtectionAccess(entity.getGameProfile()) || (entity.isCreative() && FabricPermissionBridge.checkPermission(entity.createCommandSourceStackForNameResolution((ServerLevel) entity.level()), id("can_open_others"), PermissionLevel.ADMINS));
    }

    public boolean hasProtectionAccess(GameProfile profile) {
        return !this.isProtected() || (this.gameProfile != null && this.gameProfile.id().equals(profile.id())) || this.allowedUUIDs.contains(profile.id());
    }

    public boolean hasProtectionAccess(NameAndId profile) {
        return !this.isProtected() || (this.gameProfile != null && this.gameProfile.id().equals(profile.id())) || this.allowedUUIDs.contains(profile.id());
    }

    public boolean canPayForUnlock(ServerPlayer player) {
        if (!this.isPaymentRequired(player)) {
            return false;
        }

        var interactions = ConfigManager.getConfig().interactions;
        return this.hasProtectionAccess(player) || interactions.allowNonOwnerPaidUnlock;
    }

    public GenericCost<?> getUnlockCost(ServerPlayer player) {
        return ConfigManager.getConfig().interactions.cost.quote(this, player);
    }

    public boolean payForUnlock(ServerPlayer player) {
        var cfg = ConfigManager.getConfig();
        if (!this.canPayForUnlock(player)) {
            if (!cfg.texts.cantPayForThisGrave.isEmpty()) {
                player.sendSystemMessage(cfg.texts.cantPayForThisGrave.text());
            }
            return false;
        }

        var cost = cfg.interactions.cost.quote(this, player);
        if (cost.takeCost(player)) {
            if (cfg.interactions.allowNonOwnerPaidUnlock) {
                this.paidUnlockUUIDs.add(player.getUUID());
            } else {
                this.requirePayment = false;
            }
            GraveManager.INSTANCE.setDirty();
            if (!cfg.texts.graveUnlocked.isEmpty()) {
                player.sendSystemMessage(cfg.texts.graveUnlocked.with(cost.getPlaceholders()));
            }
            var world = player.level().getServer().getLevel(ResourceKey.create(Registries.DIMENSION, this.location.world()));
            if (world != null && world.getBlockEntity(location.blockPos()) instanceof GraveBlockEntity entity) {
                entity.setModelId(entity.getGraveModelId());
            }

            return true;
        }
        player.sendSystemMessage(cfg.texts.graveNotEnoughCost.with(cost.getPlaceholders()));

        return false;
    }

    public GameProfile getProfile() {
        return this.gameProfile != null ? this.gameProfile : DEFAULT_GAME_PROFILE;
    }

    public ResolvableProfile getProfileComponent() {
        return this.gameProfile != null ? ResolvableProfile.createResolved(this.gameProfile) : DEFAULT_PROFILE_COMPONENT;
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

    public Component getDeathCause() {
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
        var world = server.getLevel(ResourceKey.create(Registries.DIMENSION, location.world()));

        if (world != null) {
            var state = world.getBlockState(location.blockPos());

            if (GraveUtils.canReplaceState(state, ConfigManager.getConfig().placement.replaceAnyBlock)
                    && world.getWorldBorder().isWithinBounds(location.blockPos()) && location.y() >= world.getMinY() && location.y() <= world.getMaxY()) {

                var old = this.location;
                this.setLocation(location);

                {
                    var oldWorld = server.getLevel(ResourceKey.create(Registries.DIMENSION, old.world()));

                    if (oldWorld != null) {
                        var oldChunk = oldWorld.getChunk(SectionPos.blockToSectionCoord(old.x()), SectionPos.blockToSectionCoord(old.z()));

                        if (oldChunk.getBlockEntity(old.blockPos()) instanceof GraveBlockEntity grave) {
                            grave.setGrave(null);
                            grave.breakBlock(false);
                        }
                    }
                }

                world.setBlockAndUpdate(location.blockPos(), GravesRegistry.GRAVE_BLOCK.defaultBlockState());

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

    public void openUi(ServerPlayer player, boolean canModify, boolean canFetch) {
        new GraveGui(player, this, canModify, canFetch).open();
    }

    public Container asInventory() {
        return ImplementedInventory.of(new NonNullList<>(List.of(), null) {
            @NotNull
            public ItemStack get(int index) {
                return Grave.this.items.get(index).stack();
            }

            public ItemStack set(int index, ItemStack element) {
                Validate.notNull(element);
                var old = Grave.this.items.set(index, new PositionedItemStack(element, -1, VanillaInventoryMask.INSTANCE, null, Set.of()));
                if (old != null) {
                    Grave.this.removeTaggedItem(old);
                    GraveManager.INSTANCE.setDirty();
                    return old.stack();
                }
                return ItemStack.EMPTY;
            }

            public void add(int value, ItemStack element) {
                Validate.notNull(element);
                Grave.this.items.add(value, new PositionedItemStack(element, -1, VanillaInventoryMask.INSTANCE, null, Set.of()));
                GraveManager.INSTANCE.setDirty();
            }

            public ItemStack remove(int index) {
                var x = Grave.this.items.remove(index);
                Grave.this.removeTaggedItem(x);
                GraveManager.INSTANCE.setDirty();
                return x.stack();
            }

            public int size() {
                return Grave.this.items.size();
            }

            public void clear() {
                Grave.this.items.clear();
                GraveManager.INSTANCE.setDirty();
            }
        }, GraveManager.INSTANCE::setDirty);
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

        this.visualData = new VisualGraveData(this.getProfileComponent(), this.skinModelParts, this.mainArm, this.deathCause, this.creationTime, this.location, this.minecraftDay);
    }

    public boolean isRemoved() {
        return this.isRemoved;
    }

    public boolean isPaymentRequired() {
        return this.requirePayment;
    }

    public boolean isPaymentRequired(ServerPlayer player) {
        return this.isPaymentRequired(player.getUUID());
    }

    private boolean isPaymentRequired(UUID uuid) {
        return this.requirePayment && !this.paidUnlockUUIDs.contains(uuid);
    }

    public byte visibleSkinModelParts() {
        return skinModelParts;
    }

    public HumanoidArm mainArm() {
        return mainArm;
    }

    public void destroyGrave(MinecraftServer server, @Nullable Player breaker) {
        if (this.isRemoved) {
            return;
        }

        var config = ConfigManager.getConfig();
        var owner = this.gameProfile != null ? server.getPlayerList().getPlayer(this.gameProfile.id()) : null;

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
                owner.sendSystemMessage(text.with(this.getPlaceholders(server)));
            }
        }

        var world = server.getLevel(ResourceKey.create(Registries.DIMENSION, this.getLocation().world()));

        if (world != null) {
            var chunk = world.getChunk(SectionPos.blockToSectionCoord(this.location.x()), SectionPos.blockToSectionCoord(this.location.z()));

            if (config.protection.dropItemsAfterExpiring || !shouldBreak) {
                Containers.dropContents(world, this.location.blockPos(), this.asInventory());
                GraveUtils.spawnExp(world, Vec3.atCenterOf(this.location.blockPos()), this.xp);
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
            var world = server.getLevel(ResourceKey.create(Registries.DIMENSION, this.getLocation().world()));
            if (world != null && !world.getWorldBorder().isWithinBounds(this.location.blockPos())) {
                var newPos = GraveUtils.findGravePosition(this.gameProfile != null ? new NameAndId(this.gameProfile) : null, null, world, this.location.blockPos(), config.placement.maxPlacementDistance, config.placement.replaceAnyBlock);
                if (newPos.result().canCreate()) {
                    this.moveTo(server, this.location.withPos(newPos.pos()));
                }
            }
        }

        if (!this.utilProtectionChangeMessage && !this.isProtected()) {
            this.utilProtectionChangeMessage = true;
            if (!config.texts.messageProtectionEnded.isEmpty()) {
                ServerPlayer player = this.gameProfile != null ? server.getPlayerList().getPlayer(this.gameProfile.id()) : null;
                if (player != null) {
                    player.sendSystemMessage(config.texts.messageProtectionEnded.with(this.getPlaceholders(server)));
                }
            }
        }
    }

    public boolean tryBreak(MinecraftServer server, @Nullable Player player) {
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

    public void quickEquip(ServerPlayer player) {
        try {
            if (player.isAlive() && this.hasAccess(player)) {
                if (this.isPaymentRequired(player) && !this.payForUnlock(player)) {
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
                this.tryBreak(player.level().getServer(), player);
                this.updateSelf(player.level().getServer());
                GraveManager.INSTANCE.setDirty();
            }
        } catch (Exception e) {
            e.printStackTrace();
            GraveManager.INSTANCE.setDirty();
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

    public boolean isOwner(ServerPlayer player) {
        return this.gameProfile != null && player.getUUID().equals(this.gameProfile.id());
    }
}
