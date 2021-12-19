package eu.pb4.graves.other;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record Location(Identifier world, BlockPos blockPos) {
    public int x() { return this.blockPos.getX(); }

    public int y() {
        return this.blockPos.getY();
    }

    public int z() {
        return this.blockPos.getZ();
    }

    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putIntArray("Position", new int[]{this.x(), this.y(), this.z()});
        nbt.putString("World", this.world().toString());
        return nbt;
    }

    public static Location fromNbt(NbtCompound nbt) {
        int[] pos = nbt.getIntArray("Position");
        return new Location(Identifier.tryParse(nbt.getString("World")), new BlockPos(pos[0], pos[1], pos[2]));
    }
}
