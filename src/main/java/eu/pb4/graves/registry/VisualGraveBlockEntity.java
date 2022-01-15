package eu.pb4.graves.registry;

import eu.pb4.graves.GraveNetworking;
import eu.pb4.graves.config.Config;
import eu.pb4.graves.config.ConfigManager;
import eu.pb4.graves.other.VisualGraveData;
import eu.pb4.holograms.api.elements.SpacingHologramElement;
import eu.pb4.holograms.api.holograms.WorldHologram;
import eu.pb4.placeholders.PlaceholderAPI;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
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
            this.replacedBlockState = NbtHelper.toBlockState((NbtCompound) Objects.requireNonNull(nbt.get("BlockState")));
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

        Map<String, Text> placeholders = self.visualData.getPlaceholders(self.world.getServer());

        if (self.textOverrides != self.deltaTextOverride && !self.textOverrides.equals(self.deltaTextOverride)) {
            self.updateForAllPlayers();
            self.deltaTextOverride = self.textOverrides.clone();
        }

        if (config.configData.hologram) {
            if (self.hologram == null) {
                self.hologram = new WorldHologram((ServerWorld) world, new Vec3d(pos.getX(), pos.getY(), pos.getZ()).add(0.5, config.configData.hologramOffset, 0.5)){
                    @Override
                    public boolean canAddPlayer(ServerPlayerEntity player) {
                        return !config.configData.hologramDisplayIfOnClient ? !GraveNetworking.canReceive(player.networkHandler) : true;
                    }
                };
                self.hologram.show();
            }

            List<Text> texts = new ArrayList<>();

            if (self.textOverrides != null) {
                for (Text text : self.textOverrides) {
                    if (text != LiteralText.EMPTY) {
                        texts.add(PlaceholderAPI.parsePredefinedText(text, PlaceholderAPI.PREDEFINED_PLACEHOLDER_PATTERN, placeholders));
                    } else {
                        texts.add(LiteralText.EMPTY);
                    }
                }
            } else {
                for (Text text : config.hologramVisualText) {
                    if (!text.equals(LiteralText.EMPTY)) {
                        texts.add(PlaceholderAPI.parsePredefinedText(text, PlaceholderAPI.PREDEFINED_PLACEHOLDER_PATTERN, placeholders));
                    } else {
                        texts.add(LiteralText.EMPTY);
                    }
                }
            }


            if (texts.size() != self.hologram.getElements().size()) {
                self.hologram.clearElements();
            }

            int x = 0;
            for (Text text : texts) {
                if (text.equals(LiteralText.EMPTY)) {
                    self.hologram.setElement(x, new SpacingHologramElement(0.28));
                } else {
                    self.hologram.setText(x, text);
                }
                x++;
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

    public Text[] getSignText() {
        return this.textOverrides != null ? this.textOverrides : ConfigManager.getConfig().signVisualText;
    }

    public Text[] getTextOverrides() {
        return this.textOverrides;
    }
}
