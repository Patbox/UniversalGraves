package eu.pb4.graves.grave;

import eu.pb4.graves.config.ConfigManager;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;

import java.util.*;

public class GraveManager extends PersistentState {
    public static GraveManager INSTANCE;

    private HashMap<UUID, List<GraveInfo>> byUuid = new HashMap<>();
    private HashMap<BlockPos, GraveInfo> byPos = new HashMap<>();
    private HashSet<GraveInfo> graves = new HashSet<>();
    private long ticker;


    public void add(GraveInfo info) {
        this.byUuid.computeIfAbsent(info.gameProfile.getId(), (v) -> new ArrayList<>()).add(0, info);
        this.byPos.put(info.position, info);
        this.graves.add(info);
        this.markDirty();
    }

    public void remove(GraveInfo info) {
        if (this.graves.remove(info)) {
            List<GraveInfo> graveInfoList = this.byUuid.get(info.gameProfile.getId());
            this.byPos.remove(info.position);
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

        for (GraveInfo grave : new ArrayList<>(this.graves)) {
            if (!grave.shouldBreak()) {
                list.add(grave.writeNbt(new NbtCompound()));
            }
        }
        nbt.put("Graves", list);
        nbt.putInt("Version", 1);

        return nbt;
    }

    public static PersistentState fromNbt(NbtCompound nbt) {
        GraveManager manager = new GraveManager();

        NbtList graves = nbt.getList("Graves", NbtElement.COMPOUND_TYPE);
        for (NbtElement graveNbt : graves) {
            GraveInfo graveInfo = new GraveInfo();
            graveInfo.readNbt((NbtCompound) graveNbt);

            if (!graveInfo.shouldBreak()) {
                manager.add(graveInfo);
            }
        }
        return manager;
    }

    public List<GraveInfo> getByUuid(UUID uuid) {
        List<GraveInfo> graveInfoList = this.byUuid.get(uuid);
        if (graveInfoList != null) {
            graveInfoList.removeIf((g) -> g.shouldBreak());
            return graveInfoList;
        }

        return Collections.emptyList();
    }

    public GraveInfo getByPos(BlockPos pos) {
        return this.byPos.get(pos);
    }

    public void tick() {
        this.ticker++;
    }
}
