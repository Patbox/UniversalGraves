package eu.pb4.graves.registry;

import eu.pb4.graves.GravesMod;
import eu.pb4.graves.config.ConfigManager;
import eu.pb4.graves.model.ModelDataProvider;
import eu.pb4.graves.other.VisualGraveData;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;


public abstract class AbstractGraveBlockEntity extends BlockEntity implements ModelDataProvider {
    protected static final Text EMPTY_TEXT = Text.empty();

    private String model = ConfigManager.getConfig().model.defaultModelId;

    public AbstractGraveBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        nbt.putString("GraveModel", this.model);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        this.model = nbt.getString("GraveModel");
        this.onModelChanged(this.model);

    }

    @Override
    public String getGraveModelId() {
        return this.model;
    }

    public void setModelId(String model) {
        this.model = model;
        this.onModelChanged(model);
        this.markDirty();
    }



    public abstract void onModelChanged(String model);

    public abstract VisualGraveData getClientData();

    public abstract void updateModel();
}
