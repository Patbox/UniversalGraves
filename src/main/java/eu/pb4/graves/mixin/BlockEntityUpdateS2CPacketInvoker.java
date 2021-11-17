package eu.pb4.graves.mixin;

import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BlockEntityUpdateS2CPacket.class)
public interface BlockEntityUpdateS2CPacketInvoker {
    @Invoker("<init>")
    public static BlockEntityUpdateS2CPacket create(BlockPos pos, BlockEntityType<?> type, NbtCompound nbt) {
        throw new AssertionError();
    }
}
