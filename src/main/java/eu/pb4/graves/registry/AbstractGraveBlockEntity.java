package eu.pb4.graves.registry;

import eu.pb4.graves.GraveNetworking;
import eu.pb4.graves.other.VisualGraveData;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.Collections;
import java.util.List;

public abstract class AbstractGraveBlockEntity extends BlockEntity {
    protected List<Text> clientText = Collections.emptyList();

    public AbstractGraveBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public abstract void setFromPacket(GraveNetworking.NetworkingGrave decoded);

    public List<Text> getClientText() {
        return this.clientText;
    }

    public abstract VisualGraveData getClientData();
}
