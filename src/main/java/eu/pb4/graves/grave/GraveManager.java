package eu.pb4.graves.grave;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.world.PersistentState;

import java.util.*;

public class GraveManager extends PersistentState {
    public static GraveManager INSTANCE;

    private HashMap<UUID, List<GraveInfo>> graves = new HashMap<>();


    public void add(GraveInfo info) {
        this.graves.computeIfAbsent(info.gameProfile.getId(), (v) -> new ArrayList<>()).add(0, info);
        this.markDirty();
    }

    public void remove(GraveInfo info) {
        List<GraveInfo> graveInfoList = this.graves.get(info.gameProfile.getId());
        if (graveInfoList != null) {
            graveInfoList.remove(info);
            if (graveInfoList.isEmpty()) {
                this.graves.remove(info.gameProfile.getId());
            }
        }
        this.markDirty();
    }


    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList list = new NbtList();
        List<GraveInfo> tmp = new ArrayList();

        for (Collection<GraveInfo> graveInfoList : this.graves.values()) {
            tmp.addAll(graveInfoList);
        }

        for (GraveInfo grave : tmp) {
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

    public List<GraveInfo> get(UUID uuid) {
        List<GraveInfo> graveInfoList = this.graves.get(uuid);
        if (graveInfoList != null) {
            graveInfoList.removeIf((g) -> g.shouldBreak());
            return graveInfoList;
        }


        return Collections.emptyList();
    }
}
