package eu.pb4.graves.grave;

import eu.pb4.graves.other.Location;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

import java.util.*;

public class GraveManager extends PersistentState {
    public static GraveManager INSTANCE;

    private HashMap<UUID, Set<Grave>> byUuid = new HashMap<>();
    private HashMap<Location, Grave> byLocation = new HashMap<>();
    private HashSet<Grave> graves = new HashSet<>();
    private long ticker;
    private long currentGameTime;

    public void add(Grave grave) {
        this.byUuid.computeIfAbsent(grave.gameProfile.getId(), (v) -> new HashSet<>()).add(grave);
        this.byLocation.put(grave.location, grave);
        this.graves.add(grave);
        this.markDirty();
    }

    public void remove(Grave info) {
        if (this.graves.remove(info)) {
            var graveInfoList = this.byUuid.get(info.gameProfile.getId());
            this.byLocation.remove(info.location);
            if (graveInfoList != null) {
                graveInfoList.remove(info);
                if (graveInfoList.isEmpty()) {
                    this.byUuid.remove(info.gameProfile.getId());
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

        return nbt;
    }

    public static PersistentState fromNbt(NbtCompound nbt) {
        GraveManager manager = new GraveManager();
        GraveManager.INSTANCE = manager;

        manager.currentGameTime = nbt.getLong("CurrentGameTime");

        NbtList graves = nbt.getList("Graves", NbtElement.COMPOUND_TYPE);
        for (NbtElement graveNbt : graves) {
            Grave graveInfo = new Grave();
            graveInfo.readNbt((NbtCompound) graveNbt);

            if (!graveInfo.shouldNaturallyBreak()) {
                manager.add(graveInfo);
            }
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

    public Grave getByLocation(Location location) {
        return this.byLocation.get(location);
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
            for (var grave : this.graves) {
                grave.tick(server);
            }
        }
    }

    public long getCurrentGameTime() {
        return this.currentGameTime;
    }
}
