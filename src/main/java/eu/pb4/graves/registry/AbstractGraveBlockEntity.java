package eu.pb4.graves.registry;

import eu.pb4.graves.other.VisualGraveData;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;


public abstract class AbstractGraveBlockEntity extends BlockEntity {
    private String model = "default";

    public AbstractGraveBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putString("GraveModel", this.model);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.model = nbt.getString("GraveModel");
    }

    public String getModelId() {
        return model;
    }

    public void setModelId(String model) {
        this.model = model;
    }

    public abstract VisualGraveData getClientData();
}
