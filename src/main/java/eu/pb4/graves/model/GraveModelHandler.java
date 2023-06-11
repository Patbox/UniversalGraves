package eu.pb4.graves.model;

import com.mojang.authlib.GameProfile;
import eu.pb4.graves.config.Config;
import eu.pb4.graves.other.DynamicNode;
import eu.pb4.graves.registry.AbstractGraveBlock;
import eu.pb4.graves.registry.GraveBlock;
import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.api.node.TextNode;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.GenericEntityElement;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import eu.pb4.polymer.virtualentity.api.elements.TextDisplayElement;
import net.minecraft.block.BlockState;
import net.minecraft.item.SkullItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.text.Text;
import net.minecraft.util.math.RotationPropertyHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

public class GraveModelHandler extends ElementHolder {
    private final List<TextsWithPlaceholders> textsWithPlaceholders = new ArrayList<>();
    private final List<ItemDisplayElement> playerHeadDisplays = new ArrayList<>();
    private BlockState blockState;
    private float yaw;
    private GameProfile gameProfile;
    private Supplier<Map<String, Text>> placeholderSupplier;
    private String model;
    private boolean isProtected;
    private boolean isPlayerMade;
    private boolean isPaymentRequired;

    public GraveModelHandler(BlockState state) {
        this.blockState = state;
        this.updateYaw();
    }

    private void updateYaw() {
        this.setYaw(RotationPropertyHelper.toDegrees(blockState.get(AbstractGraveBlock.ROTATION)));
    }

    public void setGrave(String model, boolean isProtected, boolean isPlayerMade, boolean isPaymentRequired, GameProfile profile, Supplier<Map<String, Text>> placeholderSupplier) {
        this.gameProfile = profile;
        this.placeholderSupplier = placeholderSupplier;
        this.setModel(model, isProtected, isPlayerMade, isPaymentRequired);
    }

    public void setModel(String model, boolean isProtected, boolean isPlayerMade, boolean isPaymentRequired) {
        this.model = model;
        this.isPlayerMade = isPlayerMade;
        this.isProtected = isProtected;
        this.isPaymentRequired = isPaymentRequired;
        this.updateModel();
    }

    public void updateModel() {
        if (!this.getElements().isEmpty()) {
            this.textsWithPlaceholders.clear();
            this.playerHeadDisplays.clear();
            for (var element : new ArrayList<>(this.getElements())) {
                this.removeElement(element);
            }
        }

        if (this.model != null) {
            var flags = Set.of(
                    this.isProtected ? ModelPart.Tags.IF_UNPROTECTED : ModelPart.Tags.IF_PROTECTED,
                    this.isPlayerMade ? ModelPart.Tags.IF_NOT_PLAYER_MADE : ModelPart.Tags.IF_PLAYER_MADE,
                    this.isPaymentRequired ? ModelPart.Tags.IF_NOT_REQUIRE_PAYMENT : ModelPart.Tags.IF_REQUIRE_PAYMENT,
                    this.blockState.isOf(GraveBlock.INSTANCE) ? ModelPart.Tags.IF_VISUAL : ModelPart.Tags.IF_NOT_VISUAL
            );

            GraveModel.setup(this.model, flags, this::addPart);
        }
    }

    private void addPart(ModelPart part) {
        var element = part.construct();
        if (element instanceof TextDisplayElement textDisplayElement) {
            this.textsWithPlaceholders.add(new TextsWithPlaceholders(part.text.textNode(), textDisplayElement));

            if (placeholderSupplier != null) {
                textDisplayElement.setText(part.text.textNode().toText(ParserContext.of(DynamicNode.NODES, placeholderSupplier.get()::get)));
            }
        }

        if (element instanceof ItemDisplayElement itemDisplayElement && part.tags.contains(ModelPart.Tags.PLAYER_HEAD)) {
            this.playerHeadDisplays.add(itemDisplayElement);

            if (gameProfile != null) {
                itemDisplayElement.getItem().getOrCreateNbt().put(SkullItem.SKULL_OWNER_KEY, NbtHelper.writeGameProfile(new NbtCompound(), gameProfile));
            }
        }

        element.setYaw(this.yaw);
        this.addElement(element);
    }

    public void setYaw(float value) {
        this.yaw = value;
        for (var element : this.getElements()) {
            if (element instanceof GenericEntityElement genericEntityElement) {
                genericEntityElement.setYaw(value);
            }
        }
    }

    @Override
    protected void onTick() {
        var placeholders = (Function<String, Text>) placeholderSupplier.get()::get;
        for (var text : textsWithPlaceholders) {
            text.displayElement.setText(text.node().toText(ParserContext.of(DynamicNode.NODES, placeholders)));
        }
    }

    @Override
    public void notifyUpdate(HolderAttachment.UpdateType updateType) {
        if (BlockBoundAttachment.BLOCK_STATE_UPDATE == updateType) {
            var state = BlockBoundAttachment.get(this).getBlockState();
            if (state != this.blockState) {
                this.updateYaw();
            }
            this.blockState = state;
        }
    }

    private record TextsWithPlaceholders(TextNode node, TextDisplayElement displayElement) {}
}
