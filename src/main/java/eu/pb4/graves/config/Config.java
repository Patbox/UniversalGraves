package eu.pb4.graves.config;


import com.mojang.brigadier.StringReader;
import eu.pb4.graves.config.data.ConfigData;
import eu.pb4.graves.registry.GraveBlock;
import eu.pb4.graves.other.GravesLookType;
import eu.pb4.graves.other.GravesXPCalculation;
import eu.pb4.graves.registry.IconItem;
import eu.pb4.placeholders.TextParser;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.command.argument.ItemStringReader;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.*;

public final class Config {
    public final ConfigData configData;
    public final GravesLookType style;

    public final Text[] hologramProtectedText;
    public final Text[] hologramText;
    public final Text[] hologramVisualText;

    public final Text[] signProtectedText;
    public final Text[] signText;
    public final Text[] signVisualText;

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
    public final Text creationFailedVoidMessage;
    @Nullable
    public final Text creationFailedPvPMessage;
    @Nullable
    public final Text creationFailedClaimMessage;
    public final GravesXPCalculation xpCalc;

    public final BlockStyleEntry[] customBlockStateStylesLocked;
    public final BlockStyleEntry[] customBlockStateStylesUnlocked;

    public final Set<Identifier> skippedEnchantments;
    public final Map<Identifier, Text> worldNameOverrides;
    public final Set<Identifier> blacklistedWorlds;
    public final boolean canClientSide;

    public final Text guiPreviousPageText;
    public final Text guiPreviousPageBlockedText;

    public final Text guiNextPageText;
    public final Text guiNextPageBlockedText;

    public final Text guiRemoveProtectionText;
    public final Text guiBreakGraveText;

    public final Text guiQuickPickupText;
    public final Text guiCantReverseAction;
    public final Text guiClickToConfirm;

    public final ItemStack guiInfoIcon;

    public final ItemStack guiPreviousPageIcon;
    public final ItemStack guiPreviousPageBlockedIcon;

    public final ItemStack guiNextPageIcon;
    public final ItemStack guiNextPageBlockedIcon;

    public final ItemStack guiRemoveProtectionIcon;
    public final ItemStack guiBreakGraveIcon;

    public final ItemStack guiQuickPickupIcon;
    public final ItemStack guiBarItem;
    public final SimpleDateFormat fullDateFormat;
    public final HashMap<Identifier, List<Box>> blacklistedAreas;

    public Config(ConfigData data) {
        this.configData = data;
        this.style = GravesLookType.byName(configData.graveStyle);
        this.xpCalc = GravesXPCalculation.byName(configData.xpStorageType);
        this.hologramProtectedText = parse(data.hologramProtectedText);
        this.hologramText = parse(data.hologramText);
        this.hologramVisualText = parse(data.hologramVisualText);
        this.signProtectedText = parse(data.customStyleSignProtectedText);
        this.signText = parse(data.customStyleSignText);
        this.signVisualText = parse(data.customStyleSignVisualText);

        this.graveTitle = TextParser.parse(data.graveTitle);
        this.canClientSide = data.allowClientSideStyle && this.style.allowClient;

        this.guiTitle = TextParser.parse(data.guiTitle);
        this.guiProtectedText = parse(data.guiProtectedText);
        this.guiText = parse(data.guiText);

        this.noLongerProtectedMessage = parse(data.messageProtectionEnded, null);
        this.graveExpiredMessage = parse(data.messageGraveExpired, null);
        this.graveBrokenMessage = parse(data.messageGraveBroken, null);
        this.createdGraveMessage = parse(data.messageGraveCreated, null);
        this.creationFailedGraveMessage = parse(data.messageCreationFailed, null);
        this.creationFailedVoidMessage = parse(data.messageCreationFailedVoid, null);
        this.creationFailedPvPMessage = parse(data.messageCreationFailedPvP, null);
        this.creationFailedClaimMessage = parse(data.messageCreationFailedClaim, null);

        this.guiPreviousPageText = parse(data.guiPreviousPageText, LiteralText.EMPTY);
        this.guiPreviousPageBlockedText = parse(data.guiPreviousPageBlockedText, LiteralText.EMPTY);
        this.guiNextPageText = parse(data.guiNextPageText, LiteralText.EMPTY);
        this.guiNextPageBlockedText = parse(data.guiNextPageBlockedText, LiteralText.EMPTY);
        this.guiRemoveProtectionText = parse(data.guiRemoveProtectionText, LiteralText.EMPTY);
        this.guiBreakGraveText = parse(data.guiBreakGraveText, LiteralText.EMPTY);
        this.guiQuickPickupText = parse(data.guiQuickPickupText, LiteralText.EMPTY);
        this.guiCantReverseAction = parse(data.guiCantReverseAction, LiteralText.EMPTY);
        this.guiClickToConfirm = parse(data.guiClickToConfirm, LiteralText.EMPTY);

        this.guiInfoIcon = parseItem(data.guiInfoIcon);
        this.guiBarItem = parseItem(data.guiBarItem);
        this.guiPreviousPageIcon = parseItem(data.guiPreviousPageIcon);
        this.guiPreviousPageBlockedIcon = parseItem(data.guiPreviousPageBlockedIcon);
        this.guiNextPageIcon = parseItem(data.guiNextPageIcon);
        this.guiNextPageBlockedIcon = parseItem(data.guiNextPageBlockedIcon);
        this.guiRemoveProtectionIcon = parseItem(data.guiRemoveProtectionIcon);
        this.guiBreakGraveIcon = parseItem(data.guiBreakGraveIcon);
        this.guiQuickPickupIcon = parseItem(data.guiQuickPickupIcon);

        this.customBlockStateStylesLocked = parseBlockStyles(this.configData.customBlockStateLockedStyles);
        this.customBlockStateStylesUnlocked = parseBlockStyles(this.configData.customBlockStateUnlockedStyles);

        this.guiProtectedItem = parseItems(this.configData.guiProtectedItem);
        this.guiItem = parseItems(this.configData.guiItem);

        this.skippedEnchantments = parseIds(data.skippedEnchantments);
        this.blacklistedWorlds = parseIds(data.blacklistedWorlds);

        this.fullDateFormat = new SimpleDateFormat(configData.fullDateFormat);

        this.worldNameOverrides = new HashMap<>();

        for (var entry : data.worldNameOverrides.entrySet()) {
            var id = Identifier.tryParse(entry.getKey());

            if (id != null) {
                this.worldNameOverrides.put(id, parse(entry.getValue(), null));

            }
        }

        this.blacklistedAreas = new HashMap<>();

        for (var entry : data.blacklistedAreas.entrySet()) {
            var id = Identifier.tryParse(entry.getKey());

            if (id != null) {
                var list = new ArrayList<Box>();
                this.blacklistedAreas.put(id, list);

                for (var area : entry.getValue()) {
                    list.add(new Box(area.x1, area.y1, area.z1, area.x2, area.y2, area.z2));
                }
            }
        }
    }

    private static Set<Identifier> parseIds(List<String> ids) {
        var set = new HashSet<Identifier>();
        for (var id : ids) {
            if (id != null) {
                var identifier = Identifier.tryParse(id);

                if (identifier != null) {
                    set.add(identifier);
                }
            }
        }

        return set;
    }

    private static Text parse(String string, Text defaultText) {
        return !string.isEmpty() ? TextParser.parse(string) : defaultText;
    }

    private static ItemStack parseItem(String itemDef) {
        try {
            var item = new ItemStringReader(new StringReader(itemDef), true).consume();
            var itemStack = item.getItem().getDefaultStack();

            if (item.getNbt() != null) {
                itemStack.setNbt(item.getNbt());
            }
            return itemStack;
        } catch (Exception e) {
            return IconItem.of(IconItem.Texture.INVALID);
        }
    }

    private static ItemStack[] parseItems(List<String> stringList) {
        var items = new ArrayList<ItemStack>();

        for (var itemDef : stringList) {
            items.add(parseItem(itemDef));
        }
        if (items.isEmpty()) {
            items.add(IconItem.of(IconItem.Texture.INVALID));
        }

        return items.toArray(new ItemStack[0]);
    }

    private static BlockStyleEntry[] parseBlockStyles(List<String> stringList) {
        var blockStates = new ArrayList<BlockStyleEntry>();

        for (String stateName : stringList) {
            try {
                var stateData = new BlockArgumentParser(new StringReader(stateName), true).parse(true);
                if (stateData.getBlockState().getBlock() != GraveBlock.INSTANCE && stateData.getBlockState() != null) {
                    if (stateData.getBlockState().hasBlockEntity()) {
                        var blockEntity = ((BlockEntityProvider) stateData.getBlockState().getBlock()).createBlockEntity(BlockPos.ORIGIN, stateData.getBlockState());
                        BlockEntityType<?> i = null;

                        var packet = blockEntity.toUpdatePacket();
                        if (packet instanceof BlockEntityUpdateS2CPacket bePacket) {
                            i = bePacket.getBlockEntityType();
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
        if (time < 0) {
            return "0" + configData.secondsText;
        }

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
        if (seconds > 0 || time <= 0) {
            builder.append(seconds).append(configData.secondsText);
        }
        return builder.toString();
    }

    private static Text[] parse(List<String> strings) {
        List<Text> texts = new ArrayList<>();

        for (String line : strings) {
            if (line == null) {
                continue;
            } else if (line.isEmpty()) {
                texts.add(LiteralText.EMPTY);
            } else {
                texts.add(TextParser.parse(line));
            }
        }
        return texts.toArray(new Text[0]);
    }

    public static record BlockStyleEntry(BlockState state, BlockEntityType<?> blockEntityType,
                                         NbtCompound blockEntityNbt) {
    }

}
