package eu.pb4.graves.config;


import com.mojang.brigadier.StringReader;
import eu.pb4.graves.config.data.ConfigData;
import eu.pb4.graves.grave.GraveBlock;
import eu.pb4.graves.grave.GravesLookType;
import eu.pb4.graves.grave.GravesXPCalculation;
import eu.pb4.placeholders.TextParser;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.command.argument.ItemStringReader;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class Config {
    public final ConfigData configData;
    public final GravesLookType style;

    public final Text[] hologramProtectedText;
    public final Text[] hologramText;

    public final Text[] signProtectedText;
    public final Text[] signText;

    public final Text graveTitle;

    public final Text guiTitle;
    public final Text[] guiProtectedText;
    public final Text[] guiText;

    public final ItemStack[] guiProtectedItem;
    public final ItemStack[] guiItem;

    @Nullable
    public final Text noLongerProtectedMessage;
    @Nullable
    public final Text graveExpiredMessage;
    @Nullable
    public final Text graveBrokenMessage;
    @Nullable
    public final Text createdGraveMessage;
    @Nullable
    public final Text creationFailedGraveMessage;
    @Nullable
    public final Text creationFailedPvPGraveMessage;
    @Nullable
    public final Text creationFailedClaimGraveMessage;
    public final GravesXPCalculation xpCalc;

    public final BlockStyleEntry[] customBlockStateStylesLocked;
    public final BlockStyleEntry[] customBlockStateStylesUnlocked;


    public Config(ConfigData data) {
        this.configData = data;
        this.style = GravesLookType.byName(configData.graveStyle);
        this.xpCalc = GravesXPCalculation.byName(configData.xpStorageType);
        this.hologramProtectedText = parse(data.hologramProtectedText);
        this.hologramText = parse(data.hologramText);
        this.signProtectedText = parse(data.customStyleSignProtectedText);
        this.signText = parse(data.customStyleSignText);

        this.graveTitle = TextParser.parse(data.graveTitle);

        this.guiTitle = TextParser.parse(data.guiTitle);
        this.guiProtectedText = parse(data.guiProtectedText);
        this.guiText = parse(data.guiText);

        this.noLongerProtectedMessage = !data.messageProtectionEnded.isEmpty() ? TextParser.parse(data.messageProtectionEnded) : null;
        this.graveExpiredMessage = !data.messageGraveExpired.isEmpty() ? TextParser.parse(data.messageGraveExpired) : null;
        this.graveBrokenMessage = !data.messageGraveBroken.isEmpty() ? TextParser.parse(data.messageGraveBroken) : null;
        this.createdGraveMessage = !data.messageGraveCreated.isEmpty() ? TextParser.parse(data.messageGraveCreated) : null;
        this.creationFailedGraveMessage = !data.messageCreationFailed.isEmpty() ? TextParser.parse(data.messageCreationFailed) : null;
        this.creationFailedPvPGraveMessage = !data.messageCreationFailedPvP.isEmpty() ? TextParser.parse(data.messageCreationFailedPvP) : null;
        this.creationFailedClaimGraveMessage = !data.messageCreationFailedClaim.isEmpty() ? TextParser.parse(data.messageCreationFailedClaim) : null;

        this.customBlockStateStylesLocked = parseBlockStyles(this.configData.customBlockStateLockedStyles);
        this.customBlockStateStylesUnlocked = parseBlockStyles(this.configData.customBlockStateUnlockedStyles);

        this.guiProtectedItem = parseItems(this.configData.guiProtectedItem);
        this.guiItem = parseItems(this.configData.guiItem);
    }

    public static ItemStack[] parseItems(List<String> stringList) {
        var items = new ArrayList<ItemStack>();

        for (var itemDef : stringList) {
            try {
                var item = new ItemStringReader(new StringReader(itemDef), true).consume();
                var itemStack = item.getItem().getDefaultStack();

                if (item.getNbt() != null) {
                    itemStack.setNbt(item.getNbt());
                }
                items.add(itemStack);
            } catch (Exception e) {
                // noop
            }
        }
        if (items.isEmpty()) {
            items.add(Items.CHEST.getDefaultStack());
        }

        return items.toArray(new ItemStack[0]);
    }

    public static BlockStyleEntry[] parseBlockStyles(List<String> stringList) {
        var blockStates = new ArrayList<BlockStyleEntry>();

        for (String stateName : stringList) {
            try {
                var stateData = new BlockArgumentParser(new StringReader(stateName), true).parse(true);
                if (stateData.getBlockState().getBlock() != GraveBlock.INSTANCE && stateData.getBlockState() != null) {
                    if (stateData.getBlockState().hasBlockEntity()) {
                        var blockEntity = ((BlockEntityProvider) stateData.getBlockState().getBlock()).createBlockEntity(BlockPos.ORIGIN, stateData.getBlockState());
                        BlockEntityType<?> i = null;

                        var packet = blockEntity.toUpdatePacket();
                        if (packet != null && packet instanceof BlockEntityUpdateS2CPacket) {
                            i = ((BlockEntityUpdateS2CPacket) packet).getBlockEntityType();
                        }

                        if (stateData.getNbtData() != null) {
                            blockEntity.readNbt(stateData.getNbtData());
                        }

                        blockStates.add(new BlockStyleEntry(stateData.getBlockState(), i, blockEntity.toInitialChunkDataNbt()));
                    } else {
                        blockStates.add(new BlockStyleEntry(stateData.getBlockState(), null, null));
                    }
                } else {
                    blockStates.add(new BlockStyleEntry(Blocks.POTATOES.getDefaultState().with(CropBlock.AGE, 7), null, null));
                }
            } catch (Exception e) {
                e.printStackTrace();
                blockStates.add(new BlockStyleEntry(Blocks.SKELETON_SKULL.getDefaultState(), null, null));
            }
        }

        if (blockStates.size() == 0) {
            blockStates.add(new BlockStyleEntry(Blocks.SKELETON_SKULL.getDefaultState(), null, null));
        }

        return blockStates.toArray(new BlockStyleEntry[0]);
    }


    public String getFormattedTime(long time) {
        if (time != Long.MAX_VALUE) {

            long seconds = time % 60;
            long minutes = (time / 60) % 60;
            long hours = (time / (60 * 60)) % 24;
            long days = time / (60 * 60 * 24) % 365;
            long years = time / (60 * 60 * 24 * 365);

            StringBuilder builder = new StringBuilder();

            if (years > 0) {
                builder.append(years).append(configData.yearsText);
            }
            if (days > 0) {
                builder.append(days).append(configData.daysText);
            }
            if (hours > 0) {
                builder.append(hours).append(configData.hoursText);
            }
            if (minutes > 0) {
                builder.append(minutes).append(configData.minutesText);
            }
            if (seconds >= 0) {
                builder.append(seconds).append(configData.secondsText);
            } else {
                builder.append(time).append(configData.secondsText);
            }
            return builder.toString();
        } else {
            return configData.neverExpires;
        }
    }

    public static Text[] parse(List<String> strings) {
        List<Text> texts = new ArrayList<>();

        for (String line : strings) {
            if (line.isEmpty()) {
                texts.add(LiteralText.EMPTY);
            } else {
                texts.add(TextParser.parse(line));
            }
        }
        return texts.toArray(new Text[0]);
    }

    public static record BlockStyleEntry(BlockState state, BlockEntityType<?> blockEntityType, NbtCompound blockEntityNbt) {
    }

}
