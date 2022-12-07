package eu.pb4.graves.registry;

import eu.pb4.graves.GraveNetworking;
import eu.pb4.graves.config.Config;
import eu.pb4.graves.config.ConfigManager;
import eu.pb4.graves.other.VisualGraveData;
import eu.pb4.holograms.api.elements.SpacingHologramElement;
import eu.pb4.holograms.api.holograms.WorldHologram;
import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.placeholders.api.node.EmptyNode;
import eu.pb4.sgui.api.gui.SignGui;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.*;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.*;

import static eu.pb4.graves.registry.AbstractGraveBlock.IS_LOCKED;

public class VisualGraveBlockEntity extends AbstractGraveBlockEntity {
    public static BlockEntityType<VisualGraveBlockEntity> BLOCK_ENTITY_TYPE;
    public WorldHologram hologram = null;
    public BlockState replacedBlockState = Blocks.AIR.getDefaultState();
    private VisualGraveData visualData = VisualGraveData.DEFAULT;
    protected boolean allowModification = true;
    protected Text[] textOverrides = null;
    private Text[] deltaTextOverride = null;

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

    protected void updateForAllPlayers() {
        assert this.world != null;

        var converter = ConfigManager.getConfig().style.converter;
        var rotation = this.getCachedState().get(Properties.ROTATION);
        var isProtected = this.getCachedState().get(IS_LOCKED);
        for (var player : ((ServerWorld) this.world).getChunkManager().threadedAnvilChunkStorage.getPlayersWatchingChunk(new ChunkPos(this.pos), false)) {
            if (!GraveNetworking.sendGrave(player.networkHandler, pos, isProtected, this.visualData, this.visualData.getPlaceholders(player.server), this.textOverrides)) {
                converter.sendNbt(player, this.getCachedState(), pos.toImmutable(), rotation, isProtected, this.visualData, null, this.textOverrides);
            }
        }
    }

    @Override
    public void markRemoved() {
        if (this.hologram != null) {
            this.hologram.hide();
        }
        this.hologram = null;
        super.markRemoved();
    }

    public static <T extends BlockEntity> void tick(World world, BlockPos pos, BlockState state, T t) {
        if (!(t instanceof VisualGraveBlockEntity self) || world.isClient() || world.getTime() % 10 != 0) {
            return;
        }

        Config config = ConfigManager.getConfig();

        if (config.configData.hologram) {
            if (self.hologram == null) {
                self.hologram = new WorldHologram((ServerWorld) world, new Vec3d(pos.getX(), pos.getY(), pos.getZ()).add(0.5, config.configData.hologramOffset, 0.5)) {
                    @Override
                    public boolean canAddPlayer(ServerPlayerEntity player) {
                        return config.configData.hologramDisplayIfOnClient || !GraveNetworking.canReceive(player.networkHandler);
                    }
                };
                self.hologram.show();
            }

            boolean dirty = false;
            List<Text> texts = new ArrayList<>();

            if (self.textOverrides != null) {
                if ((self.deltaTextOverride == null || !Arrays.equals(self.textOverrides, self.deltaTextOverride))) {
                    for (Text text : self.textOverrides) {
                        texts.add(text.copy());
                    }

                    self.updateForAllPlayers();
                    self.deltaTextOverride = new Text[self.textOverrides.length];
                    for (int i = 0; i < self.textOverrides.length; i++) {
                        self.deltaTextOverride[i] = self.textOverrides[i].copy();
                    }
                    dirty = true;
                }
            } else {
                Map<String, Text> placeholders = self.visualData.getPlaceholders(self.world.getServer());

                for (var text : config.hologramVisualText) {
                    if (!text.equals(EmptyNode.INSTANCE)) {
                        texts.add(Placeholders.parseText(text, Placeholders.PREDEFINED_PLACEHOLDER_PATTERN, placeholders));
                    } else {
                        texts.add(Text.empty());
                    }
                }
                dirty = true;
            }


            if (dirty) {
                if (texts.size() != self.hologram.getElements().size()) {
                    self.hologram.clearElements();
                }

                int x = 0;
                for (Text text : texts) {
                    if (text.getContent() == TextContent.EMPTY && text.getSiblings().size() == 00) {
                        self.hologram.setElement(x, new SpacingHologramElement(0.28));
                    } else {
                        self.hologram.setText(x, text);
                    }
                    x++;
                }
            }
        } else {
            if (self.hologram != null) {
                self.hologram.hide();
                self.hologram = null;
            }
        }
    }

    public VisualGraveData getGrave() {
        return this.visualData;
    }

    public void setFromPacket(GraveNetworking.NetworkingGrave decoded) {
        this.visualData = decoded.data();
        this.clientText = decoded.displayText();
    }

    @Override
    public VisualGraveData getClientData() {
        return this.visualData;
    }

    public Text[] getTextOverrides() {
        return this.textOverrides;
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
