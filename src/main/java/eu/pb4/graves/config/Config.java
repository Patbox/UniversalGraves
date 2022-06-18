package eu.pb4.graves.config;


import com.mojang.brigadier.StringReader;
import eu.pb4.graves.config.data.ConfigData;
import eu.pb4.graves.registry.GraveBlock;
import eu.pb4.graves.other.GravesLookType;
import eu.pb4.graves.other.GravesXPCalculation;
import eu.pb4.graves.registry.IconItem;
import eu.pb4.placeholders.api.TextParserUtils;
import eu.pb4.placeholders.api.node.EmptyNode;
import eu.pb4.placeholders.api.node.TextNode;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.command.CommandRegistryWrapper;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.command.argument.ItemStringReader;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.registry.Registry;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.*;

public final class Config {
    public final ConfigData configData;
    public final GravesLookType style;

    public final TextNode[] hologramProtectedText;
    public final TextNode[] hologramText;
    public final TextNode[] hologramVisualText;

    public final TextNode[] signProtectedText;
    public final TextNode[] signText;
    public final TextNode[] signVisualText;

    public final TextNode graveTitle;

    public final Text guiTitle;
    public final TextNode[] guiProtectedText;
    public final TextNode[] guiText;

    public final ItemStack[] guiProtectedItem;
    public final ItemStack[] guiItem;

    @Nullable
    public final TextNode noLongerProtectedMessage;
    @Nullable
    public final TextNode graveExpiredMessage;
    @Nullable
    public final TextNode graveBrokenMessage;
    @Nullable
    public final TextNode createdGraveMessage;
    @Nullable
    public final TextNode creationFailedGraveMessage;
    @Nullable
    public final TextNode creationFailedVoidMessage;
    @Nullable
    public final TextNode creationFailedPvPMessage;
    @Nullable
    public final TextNode creationFailedClaimMessage;
    @Nullable
    public final TextNode teleportTimerText;
    @Nullable
    public final TextNode teleportLocationText;
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

        this.graveTitle = TextParserUtils.formatNodes(data.graveTitle);
        this.canClientSide = data.allowClientSideStyle && this.style.allowClient;

        this.guiTitle = TextParserUtils.formatText(data.guiTitle);
        this.guiProtectedText = parse(data.guiProtectedText);
        this.guiText = parse(data.guiText);

        this.noLongerProtectedMessage = parse(data.messageProtectionEnded);
        this.graveExpiredMessage = parse(data.messageGraveExpired);
        this.graveBrokenMessage = parse(data.messageGraveBroken);
        this.createdGraveMessage = parse(data.messageGraveCreated);
        this.creationFailedGraveMessage = parse(data.messageCreationFailed);
        this.creationFailedVoidMessage = parse(data.messageCreationFailedVoid);
        this.creationFailedPvPMessage = parse(data.messageCreationFailedPvP);
        this.creationFailedClaimMessage = parse(data.messageCreationFailedClaim);

        this.guiPreviousPageText = parseText(data.guiPreviousPageText);
        this.guiPreviousPageBlockedText = parseText(data.guiPreviousPageBlockedText);
        this.guiNextPageText = parseText(data.guiNextPageText);
        this.guiNextPageBlockedText = parseText(data.guiNextPageBlockedText);
        this.guiRemoveProtectionText = parseText(data.guiRemoveProtectionText);
        this.guiBreakGraveText = parseText(data.guiBreakGraveText);
        this.guiQuickPickupText = parseText(data.guiQuickPickupText);
        this.guiCantReverseAction = parseText(data.guiCantReverseAction);
        this.guiClickToConfirm = parseText(data.guiClickToConfirm);

        this.teleportLocationText = parse(data.teleportLocationText);
        this.teleportTimerText = parse(data.teleportTimerText);

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
                this.worldNameOverrides.put(id, parseText(entry.getValue()));

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

    private static TextNode parse(String string) {
        return !string.isEmpty() ? TextParserUtils.formatNodes(string) : null;
    }

    private static Text parseText(String string) {
        return !string.isEmpty() ? TextParserUtils.formatText(string) : Text.empty();
    }

    private static ItemStack parseItem(String itemDef) {
        try {
            var item = ItemStringReader.item(CommandRegistryWrapper.of(Registry.ITEM), new StringReader(itemDef));
            var itemStack = item.item().value().getDefaultStack();

            if (item.nbt() != null) {
                itemStack.setNbt(item.nbt());
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
                var stateData = BlockArgumentParser.block(Registry.BLOCK, new StringReader(stateName), true);
                if (stateData.blockState().getBlock() != GraveBlock.INSTANCE && stateData.blockState() != null) {
                    if (stateData.blockState().hasBlockEntity()) {
                        var blockEntity = ((BlockEntityProvider) stateData.blockState().getBlock()).createBlockEntity(BlockPos.ORIGIN, stateData.blockState());
                        BlockEntityType<?> i = null;

                        var packet = blockEntity.toUpdatePacket();
                        if (packet instanceof BlockEntityUpdateS2CPacket bePacket) {
                            i = bePacket.getBlockEntityType();
                        }

                        if (stateData.nbt() != null) {
                            blockEntity.readNbt(stateData.nbt());
                        }

                        blockStates.add(new BlockStyleEntry(stateData.blockState(), i, blockEntity.toInitialChunkDataNbt()));
                    } else {
                        blockStates.add(new BlockStyleEntry(stateData.blockState(), null, null));
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

    private static TextNode[] parse(List<String> strings) {
        List<TextNode> texts = new ArrayList<>();

        for (String line : strings) {
            if (line == null) {
                continue;
            } else if (line.isEmpty()) {
                texts.add(EmptyNode.INSTANCE);
            } else {
                texts.add(TextParserUtils.formatNodes(line));
            }
        }
        return texts.toArray(new TextNode[0]);
    }

    public static record BlockStyleEntry(BlockState state, BlockEntityType<?> blockEntityType,
                                         NbtCompound blockEntityNbt) {
    }

}
