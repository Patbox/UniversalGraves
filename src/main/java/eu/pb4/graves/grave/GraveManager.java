package eu.pb4.graves.grave;

import eu.pb4.graves.other.Location;
import eu.pb4.graves.registry.GraveGameRules;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;
import org.jetbrains.annotations.ApiStatus;

import java.util.*;

public final class GraveManager extends PersistentState {
    public static GraveManager INSTANCE;

    private final HashMap<UUID, Set<Grave>> byUuid = new HashMap<>();
    private final HashMap<Location, Grave> byLocation = new HashMap<>();
    private final Long2ObjectMap<Grave> byId = new Long2ObjectOpenHashMap<>();
    private final HashSet<Grave> graves = new HashSet<>();
    private long ticker;
    private long currentGameTime;
    private long currentGraveId = 0;
    private int protectionTime;
    private int breakingTime;

    public void add(Grave grave) {
        if (grave.getId() == -1) {
            grave.setId(this.requestId());
        }

        this.byUuid.computeIfAbsent(grave.getProfile().getId(), (v) -> new HashSet<>()).add(grave);
        this.byLocation.put(grave.getLocation(), grave);
        this.byId.put(grave.getId(), grave);
        this.graves.add(grave);
        this.markDirty();
    }

    public void remove(Grave info) {
        if (this.graves.remove(info)) {
            var graveInfoList = this.byUuid.get(info.getProfile().getId());
            this.byLocation.remove(info.getLocation());
            this.byId.remove(info.getId());
            if (graveInfoList != null) {
                graveInfoList.remove(info);
                if (graveInfoList.isEmpty()) {
                    this.byUuid.remove(info.getProfile().getId());
                }
            }
            this.markDirty();
        }
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList list = new NbtList();

        for (Grave grave : new ArrayList<>(this.graves)) {
            if (!grave.shouldNaturallyBreak()) {
                list.add(grave.writeNbt(new NbtCompound()));
            }
        }
        nbt.put("Graves", list);
        nbt.putInt("Version", 2);
        nbt.putLong("CurrentGameTime", this.currentGameTime);
        nbt.putLong("CurrentGrave", this.currentGraveId);

        return nbt;
    }

    public static PersistentState fromNbt(NbtCompound nbt, MinecraftServer server) {
        GraveManager manager = new GraveManager();
        manager.protectionTime = GraveGameRules.getProtectionTime(server);
        manager.breakingTime = GraveGameRules.getBreakingTime(server);
        GraveManager.INSTANCE = manager;

        manager.currentGameTime = nbt.getLong("CurrentGameTime");
        manager.currentGraveId = nbt.getLong("CurrentGrave");

        NbtList graves = nbt.getList("Graves", NbtElement.COMPOUND_TYPE);
        for (NbtElement graveNbt : graves) {
            Grave graveInfo = new Grave();
            graveInfo.readNbt((NbtCompound) graveNbt);
            manager.add(graveInfo);
        }
        return manager;
    }

    public Collection<Grave> getByUuid(UUID uuid) {
        var graveInfoList = this.byUuid.get(uuid);
        if (graveInfoList != null) {
            graveInfoList.removeIf((g) -> g.shouldNaturallyBreak());
            return graveInfoList;
        }

        return Collections.emptyList();
    }

    public Grave getId(long id) {
        return this.byId.get(id);
    }

    public Grave getByLocation(Location location) {
        return this.byLocation.get(location);
    }

    @ApiStatus.Internal
    public void moveToLocation(Grave grave, Location location) {
        this.byLocation.remove(grave.getLocation());
        this.byLocation.put(location, grave);
    }

    public Grave getByLocation(Identifier world, BlockPos pos) {
        return this.getByLocation(new Location(world, pos.toImmutable()));
    }

    public Grave getByLocation(World world, BlockPos pos) {
        return this.getByLocation(new Location(world.getRegistryKey().getValue(), pos.toImmutable()));
    }

    public Collection<Grave> getAll() {
        return this.graves;
    }

    public void tick(MinecraftServer server) {
        this.ticker++;
        if (this.ticker >= 20) {
            this.ticker = 0;
            this.currentGameTime++;

            this.protectionTime = GraveGameRules.getProtectionTime(server);
            this.breakingTime = GraveGameRules.getBreakingTime(server);

            try {
                for (var grave : this.graves.toArray(new Grave[0])) {
                    grave.tick(server);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public long getCurrentGameTime() {
        return this.currentGameTime;
    }

    public long requestId() {
        return this.currentGraveId++;
    }

    public int getProtectionTime() {
        return this.protectionTime;
    }

    public boolean isProtectionEnabled() {
        return this.protectionTime != 0;
    }

    public int getBreakingTime() {
        return this.breakingTime;
    }

    public Collection<Grave> getByPlayer(ServerPlayerEntity player) {
        return this.getByUuid(player.getUuid());
    }
}
