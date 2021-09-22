package eu.pb4.graves.config;


import com.mojang.brigadier.StringReader;
import eu.pb4.graves.grave.GraveBlock;
import eu.pb4.graves.grave.GravesLookType;
import eu.pb4.graves.config.data.ConfigData;
import eu.pb4.graves.grave.GravesXPCalculation;
import eu.pb4.placeholders.TextParser;
import net.minecraft.block.*;
import net.minecraft.block.entity.*;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.command.argument.BlockStateArgument;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class Config {
    public final ConfigData configData;
    public final GravesLookType style;

    public final List<Text> hologramProtectedText;
    public final List<Text> hologramText;

    public final Text graveTitle;

    public final Text guiTitle;
    public final List<Text> guiProtectedText;
    public final List<Text> guiText;

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

    public final List<BlockStyleEntry> customBlockStateStylesLocked;
    public final List<BlockStyleEntry> customBlockStateStylesUnlocked;


    public Config(ConfigData data) {
        this.configData = data;
        this.style = GravesLookType.byName(configData.graveStyle);
        this.xpCalc = GravesXPCalculation.byName(configData.xpStorageType);
        this.hologramProtectedText = parse(data.hologramProtectedText);
        this.hologramText = parse(data.hologramText);

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
    }

    public static List<BlockStyleEntry> parseBlockStyles(List<String> stringList) {
        var blockStates = new ArrayList<BlockStyleEntry>();

        for (String stateName : stringList) {
            try {
                var stateData = new BlockArgumentParser(new StringReader(stateName), true).parse(true);
                if (stateData.getBlockState().getBlock() != GraveBlock.INSTANCE) {
                    if (stateData.getBlockState().hasBlockEntity()) {
                        var blockEntity = ((BlockEntityProvider) stateData.getBlockState().getBlock()).createBlockEntity(BlockPos.ORIGIN, stateData.getBlockState());
                        int i = -1;
                        if (blockEntity instanceof SkullBlockEntity) {
                            i = BlockEntityUpdateS2CPacket.SKULL;
                        } else if (blockEntity instanceof BannerBlockEntity) {
                            i = BlockEntityUpdateS2CPacket.BANNER;
                        } else if (blockEntity instanceof CampfireBlockEntity) {
                            i = BlockEntityUpdateS2CPacket.CAMPFIRE;
                        } else if (blockEntity instanceof ConduitBlockEntity) {
                            i = BlockEntityUpdateS2CPacket.CONDUIT;
                        } else if (blockEntity instanceof SignBlockEntity) {
                            i = BlockEntityUpdateS2CPacket.SIGN;
                        } else if (blockEntity instanceof MobSpawnerBlockEntity) {
                            i = BlockEntityUpdateS2CPacket.MOB_SPAWNER;
                        }

                        var nbt = stateData.getNbtData().copy();

                        nbt.putString("id", Registry.BLOCK_ENTITY_TYPE.getId(blockEntity.getType()).toString());

                        blockStates.add(new BlockStyleEntry(stateData.getBlockState(), i, stateData.getNbtData()));
                    } else {
                        blockStates.add(new BlockStyleEntry(stateData.getBlockState(), -1, null));
                    }
                } else {
                    blockStates.add(new BlockStyleEntry(Blocks.POTATOES.getDefaultState().with(CropBlock.AGE, 7), -1, null));
                }
            } catch (Exception e) {
                e.printStackTrace();
                blockStates.add(new BlockStyleEntry(Blocks.SKELETON_SKULL.getDefaultState(), -1, null));
            }
        }

        if (blockStates.size() == 0) {
            blockStates.add(new BlockStyleEntry(Blocks.SKELETON_SKULL.getDefaultState(), -1, null));
        }

        return blockStates;
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
                builder.append(years + configData.yearsText);
            }
            if (days > 0) {
                builder.append(days + configData.daysText);
            }
            if (hours > 0) {
                builder.append(hours + configData.hoursText);
            }
            if (minutes > 0) {
                builder.append(minutes + configData.minutesText);
            }
            if (seconds >= 0) {
                builder.append(seconds + configData.secondsText);
            } else {
                builder.append(time + configData.secondsText);
            }
            return builder.toString();
        } else {
            return configData.neverExpires;
        }
    }

    public static List<Text> parse(List<String> strings) {
        List<Text> texts = new ArrayList<>();

        for (String line : strings) {
            if (line.isEmpty()) {
                texts.add(LiteralText.EMPTY);
            } else {
                texts.add(TextParser.parse(line));
            }
        }
        return texts;
    }

    public static record BlockStyleEntry(BlockState state, int blockEntityId, NbtCompound blockEntityNbt) {};
}
