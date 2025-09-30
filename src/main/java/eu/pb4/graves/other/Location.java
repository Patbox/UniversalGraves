package eu.pb4.graves.other;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;

public record Location(Identifier world, BlockPos blockPos) {
    public int x() { return this.blockPos.getX(); }

    public int y() {
        return this.blockPos.getY();
    }

    public int z() {
        return this.blockPos.getZ();
    }

    public void writeData(WriteView view) {
        view.putIntArray("Position", new int[]{this.x(), this.y(), this.z()});
        view.putString("World", this.world().toString());
    }

    public void writeData(NbtCompound view) {
        view.putIntArray("Position", new int[]{this.x(), this.y(), this.z()});
        view.putString("World", this.world().toString());
    }

    public static Location readData(ReadView view) {
        int[] pos = view.getOptionalIntArray("Position").orElse(new int[0]);
        return new Location(Identifier.tryParse(view.getString("World", "")), new BlockPos(pos[0], pos[1], pos[2]));
    }

    public static Location readData(NbtCompound view) {
        int[] pos = view.getIntArray("Position").orElse(new int[0]);
        return new Location(Identifier.tryParse(view.getString("World", "")), new BlockPos(pos[0], pos[1], pos[2]));
    }

    public static Location fromEntity(ServerPlayerEntity player) {
        return new Location(player.getEntityWorld().getRegistryKey().getValue(), player.getBlockPos());
    }

    public GlobalPos asGlobalPos() {
        return GlobalPos.create(RegistryKey.of(RegistryKeys.WORLD, this.world), this.blockPos);
    }

    public Location withPos(BlockPos pos) {
        return new Location(world, pos);
    }
}
