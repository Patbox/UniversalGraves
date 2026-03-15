package eu.pb4.graves.model;

import eu.pb4.graves.config.data.WrappedText;
import eu.pb4.graves.grave.GraveManager;
import eu.pb4.graves.mixin.MannequinEntityAccessor;
import eu.pb4.graves.mixin.PlayerLikeEntityAccessor;
import eu.pb4.graves.model.parts.EntityModelPart;
import eu.pb4.graves.model.parts.ItemDisplayModelPart;
import eu.pb4.graves.model.parts.ModelPart;
import eu.pb4.graves.model.parts.TextDisplayModelPart;
import eu.pb4.graves.registry.AbstractGraveBlock;
import eu.pb4.graves.registry.GravesRegistry;
import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.api.node.TextNode;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.*;
import java.util.*;
import java.util.function.Function;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RotationSegment;

public class GraveModelHandler extends ElementHolder {
    private final List<TextsWithPlaceholders> textsWithPlaceholders = new ArrayList<>();
    //private final List<ItemDisplayElement> playerHeadDisplays = new ArrayList<>();
    private final List<Tuple<ItemDisplayElement, ItemDisplayModelPart>> itemDisplays = new ArrayList<>();
    private final ServerLevel world;
    private BlockState blockState;
    private float yaw;
    private Set<Identifier> ignoredFlags;
    private final List<Tuple<VirtualElement, ModelPart<?, ?>>> rotatingElements = new ArrayList<>();
    private ModelDataProvider dataPrivider;
    private final List<Tuple<LivingEntity, Set<Identifier>>> entityWithEquipment = new ArrayList<>();
    private int tickTime = 20;

    public GraveModelHandler(BlockState state, ServerLevel world) {
        this.blockState = state;
        this.world = world;
        this.updateYaw();
    }

    private void updateYaw() {
        this.setYaw(RotationSegment.convertToDegrees(blockState.getValue(AbstractGraveBlock.ROTATION)));
    }

    public void setGrave(ModelDataProvider modelDataProvider) {
        this.dataPrivider = modelDataProvider;
        this.updateModel();
    }

    public void updateModel() {
        if (!this.getElements().isEmpty()) {
            this.textsWithPlaceholders.clear();
            //this.playerHeadDisplays.clear();
            this.itemDisplays.clear();
            this.entityWithEquipment.clear();
            this.rotatingElements.clear();
            for (var element : new ArrayList<>(this.getElements())) {
                this.removeElement(element);
            }
        }

        var model = this.dataPrivider.getGraveModelId();

        if (model != null) {
            var isProtected = this.dataPrivider.isGraveProtected();
            var flags = new HashSet<Identifier>();

            flags.add(isProtected ? ModelTags.IF_UNPROTECTED : ModelTags.IF_PROTECTED);
            flags.add(this.dataPrivider.isGravePlayerMade() ? ModelTags.IF_NOT_PLAYER_MADE : ModelTags.IF_PLAYER_MADE);
            flags.add(this.dataPrivider.isGravePaymentRequired() ? ModelTags.IF_NOT_REQUIRE_PAYMENT : ModelTags.IF_REQUIRE_PAYMENT);
            flags.add(this.blockState.is(GravesRegistry.GRAVE_BLOCK) ? ModelTags.IF_VISUAL : ModelTags.IF_NOT_VISUAL);


            if (!isProtected || GraveManager.INSTANCE.getProtectionTime() <= 0) {
                flags.add(ModelTags.HAS_PROTECTION_TIMER);
            }

            if (this.dataPrivider.isGraveBroken() || GraveManager.INSTANCE.getBreakingTime() <= 0) {
                flags.add(ModelTags.HAS_BREAKING_TIMER);
            }

            this.ignoredFlags = flags;
            var modelDefinition = GraveModel.setup(model, flags, this::addPart);
            this.tickTime = modelDefinition.tickTime;
        }
    }

    private void addPart(ModelPart<?, ?> part) {
        if (this.dataPrivider == null) {
            return;
        }

        var element = part.construct(this.world);
        if (part instanceof TextDisplayModelPart textPart && element instanceof TextDisplayElement textDisplayElement) {
            List<TextNode> list = new ArrayList<>();
            for (TaggedText.Line l : textPart.text.entry()) {
                if (!l.containsTags(this.ignoredFlags)) {
                    WrappedText wrappedText = l.node();
                    TextNode textNode = wrappedText.textNode();
                    list.add(textNode);
                }
            }

            if (list.isEmpty()) {
                return;
            }
            List<TextNode> list2 = new ArrayList<>(list.size() * 2);

            var iter = list.iterator();

            while (iter.hasNext()) {
                list2.add(iter.next());
                if (iter.hasNext()) {
                    list2.add(TextNode.of("\n"));
                }
            }


            var node = TextNode.asSingle(list2.toArray(new TextNode[0]));

            this.textsWithPlaceholders.add(new TextsWithPlaceholders(node, textDisplayElement));

            textDisplayElement.setText(node.toComponent(ParserContext.of(WrappedText.DYNAMIC_NODES, this.dataPrivider::getGravePlaceholder)));
        }

        if (part instanceof ItemDisplayModelPart itemDisplayModelPart && element instanceof ItemDisplayElement itemDisplayElement) {
            boolean canContinue = true;
            if (part.tags.contains(ModelTags.PLAYER_HEAD)) {
                //this.playerHeadDisplays.add(itemDisplayElement);
                itemDisplayElement.getItem().set(DataComponents.PROFILE, this.dataPrivider.getGraveGameProfile());
                canContinue = false;
            } else {
                for (var tag : ModelTags.EQUIPMENT) {
                    if (part.tags.contains(tag)) {
                        var stack = this.dataPrivider.getGraveTaggedItem(tag);
                        if (!stack.isEmpty()) {
                            itemDisplayElement.setItem(stack);
                            this.itemDisplays.add(new Tuple<>(itemDisplayElement, itemDisplayModelPart));
                            canContinue = false;
                            break;
                        }
                    }
                }
            }

            if (canContinue && part.tags.contains(ModelTags.ITEM)) {
                var i = this.itemDisplays.size();
                itemDisplayElement.setItem(this.dataPrivider.getGraveSlotItem(i));
                this.itemDisplays.add(new Tuple<>(itemDisplayElement, itemDisplayModelPart));
            }
        }

        if (element instanceof EntityElement<?> entityElement && entityElement.entity() instanceof LivingEntity livingEntity) {
            boolean hasTag = false;
            for (var tag : ModelTags.EQUIPMENT_WITH_SLOT) {
                if (part.tags.contains(tag.getA())) {
                    hasTag = true;
                    var stack = this.dataPrivider.getGraveTaggedItem(tag.getA());
                    livingEntity.setItemSlot(tag.getB(), stack);
                }
            }
            if (hasTag) {
                this.entityWithEquipment.add(new Tuple<>(livingEntity, part.tags));
            }
        }

        if (this.updateYawFor(element, part)) {
            this.rotatingElements.add(new Tuple<>(element, part));
        }

        if (part instanceof EntityModelPart playerModelPart && element instanceof EntityElement<?> playerElement && playerElement.entity() instanceof Mannequin mannequin) {
            if (playerModelPart.tags.contains(ModelTags.PLAYER_HEAD)) {
                var m = (MannequinEntityAccessor) mannequin;
                m.callSetProfile(this.dataPrivider.getGraveGameProfile());
                mannequin.setMainArm(this.dataPrivider.getGraveMainArm());
                playerElement.entity().getEntityData().set(PlayerLikeEntityAccessor.getDATA_PLAYER_MODE_CUSTOMISATION(), this.dataPrivider.getGraveSkinModelLayers());
            }
        }

        this.addElement(element);
    }

    private boolean updateYawFor(VirtualElement element, ModelPart part) {
        if (part.tags.contains(ModelTags.ROUND_YAW_TO_90)) {
            yaw = Direction.fromYRot(yaw).toYRot();
        }
        boolean ret = false;

        if (part.rotateYaw) {
            if (element instanceof GenericEntityElement x) {
                x.setYaw(this.yaw);
                ret = true;
            } else if (element instanceof EntityElement<?> x) {
                x.entity().setYRot(this.yaw);
                x.entity().setYBodyRot(this.yaw);
                x.entity().setYHeadRot(this.yaw);
                ret = true;
            }
        }

        if (part.rotatePos) {
            element.setOffset(part.position.yRot(this.yaw * Mth.DEG_TO_RAD));
            ret = true;
        }
        return ret;
    }

    public void setYaw(float value) {
        this.yaw = value;
        for (var element : this.rotatingElements) {
            updateYawFor(element.getA(), element.getB());
        }
    }

    @Override
    protected void onTick() {
        if (this.dataPrivider != null) {
            if (this.world.getGameTime() % 20 == 0) {
                var placeholders = (Function<String, Component>) this.dataPrivider::getGravePlaceholder;
                for (var text : textsWithPlaceholders) {
                    text.displayElement.setText(text.node().toComponent(ParserContext.of(WrappedText.DYNAMIC_NODES, placeholders)));
                }

                for (int i = 0; i < this.itemDisplays.size(); i++) {
                    boolean canContinue = true;
                    var pair = this.itemDisplays.get(i);
                    for (var tag : ModelTags.EQUIPMENT) {
                        if (pair.getB().tags.contains(tag)) {
                            var stack = this.dataPrivider.getGraveTaggedItem(tag);
                            if (!stack.isEmpty()) {
                                pair.getA().setItem(stack);
                                canContinue = false;
                                break;
                            }
                        }
                    }
                    if (canContinue && pair.getB().tags.contains(ModelTags.ITEM)) {
                        pair.getA().setItem(this.dataPrivider.getGraveSlotItem(i));
                        continue;
                    }

                    pair.getA().setItem(ItemStack.EMPTY);
                }

                for (var pair : this.entityWithEquipment) {
                    for (var tag : ModelTags.EQUIPMENT_WITH_SLOT) {
                        if (pair.getB().contains(tag.getA())) {
                            var stack = this.dataPrivider.getGraveTaggedItem(tag.getA());
                            pair.getA().setItemSlot(tag.getB(), stack);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void notifyUpdate(HolderAttachment.UpdateType updateType) {
        if (BlockBoundAttachment.BLOCK_STATE_UPDATE == updateType) {
            var state = BlockBoundAttachment.get(this).getBlockState();
            if (state != this.blockState) {
                this.blockState = state;
                this.updateYaw();
                this.tick();
            }
        }
    }

    public void maybeTick(long time) {
        //if (time % this.tickTime == 0) {
            this.tick();
        //}
    }

    private record TextsWithPlaceholders(TextNode node, TextDisplayElement displayElement) {
    }
}
