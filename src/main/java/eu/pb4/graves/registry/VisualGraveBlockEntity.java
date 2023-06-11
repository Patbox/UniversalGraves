package eu.pb4.graves.registry;

import eu.pb4.graves.model.GraveModelHandler;
import eu.pb4.graves.other.VisualGraveData;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.sgui.api.gui.SignGui;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.*;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;

import static eu.pb4.graves.registry.AbstractGraveBlock.IS_LOCKED;

public class VisualGraveBlockEntity extends AbstractGraveBlockEntity {
    public static BlockEntityType<VisualGraveBlockEntity> BLOCK_ENTITY_TYPE;
    public BlockState replacedBlockState = Blocks.AIR.getDefaultState();
    private VisualGraveData visualData = VisualGraveData.DEFAULT;
    protected boolean allowModification = true;
    protected Text[] textOverrides = null;
    private GraveModelHandler model;

    public VisualGraveBlockEntity(BlockPos pos, BlockState state) {
        super(BLOCK_ENTITY_TYPE, pos, state);
    }

    public void setVisualData(VisualGraveData data, BlockState oldBlockState, boolean allowModification) {
        this.replacedBlockState = oldBlockState;
        this.visualData = data;
        this.allowModification = allowModification;
        this.markDirty();
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.put("BlockState", NbtHelper.fromBlockState(this.replacedBlockState));
        nbt.put("VisualData", this.visualData.toNbt());
        nbt.putBoolean("AllowModification", this.allowModification);

        if (this.textOverrides != null) {
            var list = new NbtList();

            for (var text : this.textOverrides) {
                list.add(NbtString.of(Text.Serializer.toJson(text)));
            }

            nbt.put("TextOverride", list);
        }
    }


    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        try {
            this.visualData = VisualGraveData.fromNbt(nbt.getCompound("VisualData"));
            this.replacedBlockState = NbtHelper.toBlockState(Registries.BLOCK.getReadOnlyWrapper(), (NbtCompound) Objects.requireNonNull(nbt.get("BlockState")));
            this.allowModification = nbt.getBoolean("AllowModification");

            if (nbt.contains("TextOverride", NbtElement.LIST_TYPE)) {
                var textOverrides = new ArrayList<>();
                for (var text : nbt.getList("TextOverride", NbtElement.STRING_TYPE)) {
                    textOverrides.add(Text.Serializer.fromLenientJson(text.asString()));
                }
                this.textOverrides = textOverrides.toArray(new Text[0]);
            }
        } catch (Exception e) {
            if (this.visualData == null) {
                this.visualData = VisualGraveData.DEFAULT;
            }
        }
    }

    public static <T extends BlockEntity> void tick(World world, BlockPos pos, BlockState state, T t) {
        if (!(t instanceof VisualGraveBlockEntity self) || world.isClient() || world.getTime() % 10 != 0) {
            return;
        }

        if (self.model == null) {
            self.model = (GraveModelHandler) BlockBoundAttachment.get(world, pos).holder();
            self.model.setGrave(self.getModelId(), state.get(IS_LOCKED), self.allowModification, false, self.getGrave().gameProfile(), self::createPlaceholders);
        }

        if (world.getTime() % 20 == 0) {
            self.model.tick();
        }
    }

    private Map<String, Text> createPlaceholders() {
        var placeholder = this.getGrave().getPlaceholders(this.world.getServer());

        if (this.textOverrides != null) {
            placeholder.put("text_1", this.textOverrides[0]);
            placeholder.put("text_2", this.textOverrides[1]);
            placeholder.put("text_3", this.textOverrides[2]);
            placeholder.put("text_4", this.textOverrides[3]);
        } else {
            placeholder.put("text_1", Text.empty());
            placeholder.put("text_2", Text.empty());
            placeholder.put("text_3", Text.empty());
            placeholder.put("text_4", Text.empty());
        }
        return placeholder;
    }

    public VisualGraveData getGrave() {
        return this.visualData;
    }

    @Override
    public VisualGraveData getClientData() {
        return this.visualData;
    }

    @Override
    public void setModelId(String model) {
        if (!this.getModelId().equals(model)) {
            super.setModelId(model);
            if (this.model != null) {
                this.model.setModel(model, this.getCachedState().get(IS_LOCKED), this.allowModification, false);
            }
        }
    }

    public void openEditScreen(ServerPlayerEntity player) {
        var sign = new SignGui(player) {
            @Override
            public void onClose() {
                VisualGraveBlockEntity.this.textOverrides = new Text[]{
                        this.getLine(0),
                        this.getLine(1),
                        this.getLine(2),
                        this.getLine(3)
                };
                VisualGraveBlockEntity.this.markDirty();
            }
        };
        sign.setSignType(Blocks.BIRCH_SIGN);

        if (this.textOverrides != null) {
            int i = 0;

            for (var text : this.textOverrides) {
                sign.setLine(i, text.copy());
                i++;

                if (i == 4) {
                    break;
                }
            }
        }

        sign.open();
    }
}
