package eu.pb4.graves.client;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import eu.pb4.graves.other.VisualGraveData;
import eu.pb4.graves.registry.AbstractGraveBlockEntity;
import net.fabricmc.fabric.api.client.model.BakedModelManagerHelper;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.SkullEntityModel;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Map;

import static eu.pb4.graves.registry.AbstractGraveBlock.IS_LOCKED;

public class GraveRenderer implements BlockEntityRenderer<AbstractGraveBlockEntity> {
    public static final Identifier GENERIC_GRAVE = new Identifier("universal_graves", "grave/generic");
    public static final Identifier GENERIC_UNLOCKED_GRAVE = new Identifier("universal_graves", "grave/generic_unlocked");

    private final TextRenderer textRenderer;
    private final SkullEntityModel playerHeadModel;
    private final SkullEntityModel skullModel;

    private final BakedModel graveStoneModel;
    private final BakedModel graveStoneUnlockedModel;

    public static final Identifier SKULL_TEXTURE = new Identifier("textures/entity/skeleton/skeleton.png");

    public GraveRenderer(BlockEntityRendererFactory.Context context) {
        var client = MinecraftClient.getInstance();
        this.textRenderer = context.getTextRenderer();
        this.playerHeadModel = new SkullEntityModel(context.getLayerRenderDispatcher().getModelPart(EntityModelLayers.PLAYER_HEAD));
        this.skullModel = new SkullEntityModel(context.getLayerRenderDispatcher().getModelPart(EntityModelLayers.SKELETON_SKULL));
        this.graveStoneModel = BakedModelManagerHelper.getModel(client.getBakedModelManager(), GENERIC_GRAVE);
        this.graveStoneUnlockedModel = BakedModelManagerHelper.getModel(client.getBakedModelManager(), GENERIC_UNLOCKED_GRAVE);
    }
    
    @Override
    public void render(AbstractGraveBlockEntity entity, float tickDelta, MatrixStack matrixStack, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        if (entity.getClientData() == VisualGraveData.DEFAULT) {
            return;
        }
        switch (GravesModClient.model) {
            case HEAD -> renderHead(entity, tickDelta, matrixStack, vertexConsumers, light, overlay);
            case GENERIC_GRAVE -> renderGrave(entity, tickDelta, matrixStack, vertexConsumers, light, overlay);
        }
    }

    public void renderHead(AbstractGraveBlockEntity entity, float tickDelta, MatrixStack matrixStack, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        var world = entity.getWorld();
        var lower = world != null ? entity.getWorld().getBlockState(entity.getPos().down()) : Blocks.AIR.getDefaultState();
        boolean blockModelEnabled = lower.isFullCube(entity.getWorld(), entity.getPos());
        if (blockModelEnabled) {
            matrixStack.push();
            var client = MinecraftClient.getInstance();
            matrixStack.translate(0.1D, -0.65D, 0.1D);
            matrixStack.scale(0.8F, 0.8F, 0.8F);
            client.getBlockRenderManager().renderBlock(lower, entity.getPos(), entity.getWorld(), matrixStack, vertexConsumers.getBuffer(RenderLayers.getBlockLayer(lower)), true, Random.create());
            matrixStack.pop();
        }
        matrixStack.push();
        matrixStack.translate(0.5D, blockModelEnabled ? 0.1D : 0, 0.5D);
        matrixStack.scale(-1.0F, -1.0F, 1.0F);
        var rotation = 22.5F * entity.getCachedState().get(Properties.ROTATION);
        var useSkull = GravesModClient.config.playerHeadsTurnSkull() && !entity.getCachedState().get(IS_LOCKED);

        var model = useSkull ? this.skullModel : this.playerHeadModel;

        model.setHeadRotation(0, rotation, blockModelEnabled ? -10f : 0);
        model.render(matrixStack, vertexConsumers.getBuffer(getRenderLayer(useSkull ? null : entity.getClientData().gameProfile())), light, overlay, 1, 1, 1, 1f);
        matrixStack.pop();
    }

    public static RenderLayer getRenderLayer(@Nullable GameProfile profile) {
        if (profile != null) {
            MinecraftClient minecraftClient = MinecraftClient.getInstance();
            var map = minecraftClient.getSkinProvider().getTextures(profile);
            return map.containsKey(MinecraftProfileTexture.Type.SKIN)
                    ? RenderLayer.getEntityTranslucent(minecraftClient.getSkinProvider().loadSkin(map.get(MinecraftProfileTexture.Type.SKIN), MinecraftProfileTexture.Type.SKIN))
                    : RenderLayer.getEntityTranslucent(DefaultSkinHelper.getTexture(profile.getId()));
        } else {
            return RenderLayer.getEntityCutoutNoCullZOffset(SKULL_TEXTURE);
        }
    }


    public void renderGrave(AbstractGraveBlockEntity entity, float tickDelta, MatrixStack matrixStack, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        var renderer = MinecraftClient.getInstance().getBlockRenderManager().getModelRenderer();

        var world = entity.getWorld();
        matrixStack.push();
        //matrixStack.translate(0.5D, blockModelEnabled ? 0.1D : 0, 0.5D);
        //matrixStack.scale(-1.0F, -1.0F, 1.0F);

        var rotation = entity.getCachedState().get(Properties.ROTATION).intValue();

        switch (rotation / 4) {
            case 1 -> {
                matrixStack.multiply(RotationAxis.POSITIVE_Y.rotation(MathHelper.PI));
                matrixStack.translate(-1, 0, -1);
                break;
            }
            case 2 -> {
                matrixStack.multiply(RotationAxis.POSITIVE_Y.rotation(MathHelper.PI / 2));
                matrixStack.translate(-1, 0, 0);
                break;
            }
            case 3 -> {
                matrixStack.multiply(RotationAxis.POSITIVE_Y.rotation(MathHelper.PI / 2 * 3));
                matrixStack.translate(0, 0, -1);
                break;
            }
        }

        var model = entity.getCachedState().get(IS_LOCKED) ? this.graveStoneModel : this.graveStoneUnlockedModel;

        if (MinecraftClient.isAmbientOcclusionEnabled() && model.useAmbientOcclusion()) {
            renderer.renderSmooth(world, model, entity.getCachedState(), entity.getPos(), matrixStack, vertexConsumers.getBuffer(RenderLayers.getBlockLayer(entity.getCachedState())), false, Random.create(), 0, overlay);
        } else {
            renderer.renderFlat(world, model, entity.getCachedState(), entity.getPos(), matrixStack, vertexConsumers.getBuffer(RenderLayers.getBlockLayer(entity.getCachedState())), false, Random.create(), 0, overlay);
        }

        matrixStack.push();
        matrixStack.translate(0.5d, 0.75, 0.425d);
        matrixStack.scale(-0.01f, -0.01f, -0.01f);
        int i = 0;
        for (var text : entity.getClientText()) {
            var orderedText = text.asOrderedText();
            float offset = (float)(-this.textRenderer.getWidth(orderedText) / 2);
            this.textRenderer.draw(orderedText, offset, (float)(i * 10 - 20), 0xFFFFFF, false, matrixStack.peek().getPositionMatrix(), vertexConsumers, TextRenderer.TextLayerType.NORMAL, 0, light);
            i++;
        }
        matrixStack.pop();

        matrixStack.push();
        matrixStack.translate(0.5d, 0.15, 0.45d);
        matrixStack.scale(-0.8f, -0.8f, 0.07f);
        this.playerHeadModel.setHeadRotation(0, 0, 0);
        this.playerHeadModel.render(matrixStack, vertexConsumers.getBuffer(getRenderLayer(entity.getClientData().gameProfile())), light, overlay, 1, 1, 1, 1f);
        matrixStack.pop();

        //model.render(matrixStack, vertexConsumers.getBuffer(getRenderLayer(useSkull ? null : entity.getClientData().gameProfile())), light, overlay, 1, 1, 1, 1f);
        matrixStack.pop();
    }
}
