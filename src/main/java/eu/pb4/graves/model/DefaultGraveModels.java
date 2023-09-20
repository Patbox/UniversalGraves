package eu.pb4.graves.model;

import eu.pb4.graves.model.parts.EntityModelPart;
import eu.pb4.graves.model.parts.ItemDisplayModelPart;
import eu.pb4.graves.model.parts.ParticleModelPart;
import eu.pb4.graves.model.parts.TextDisplayModelPart;
import eu.pb4.graves.registry.IconItem;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.Brightness;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.AffineTransformation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class DefaultGraveModels {
    public static final GraveModel FALLBACK = playerHead();

    public static void forEach(BiConsumer<String, GraveModel> consumer) {
        consumer.accept("default", playerHead());
        consumer.accept("player_head", playerHead());
        consumer.accept("corpse_player", corpsePlayer());
        consumer.accept("corpse_zombie", corpseZombie());
        consumer.accept("soul", soul());
    }

    public static GraveModel debug() {
        return corpsePlayer();
    }

    public static GraveModel soul() {
        var model = new GraveModel();
        {
            var head = new ItemDisplayModelPart();
            head.transformation = new AffineTransformation(
                    new Matrix4f().scale(0.8f)
            );
            head.transformation.getTranslation();
            head.billboardMode = DisplayEntity.BillboardMode.CENTER;

            var skull = head.copy();
            head.tags.add(ModelTags.IF_PROTECTED);
            head.tags.add(ModelTags.PLAYER_HEAD);
            head.itemStack = Items.PLAYER_HEAD.getDefaultStack();

            skull.tags.add(ModelTags.IF_UNPROTECTED);
            skull.itemStack = Items.SKELETON_SKULL.getDefaultStack();

            model.elements.add(head);
            model.elements.add(skull);
        }

        {
            var particle = new ParticleModelPart();
            particle.particleEffect = ParticleTypes.SOUL_FIRE_FLAME;
            particle.delta = new Vector3f(0.2f);
            particle.speed = 0.01f;
            particle.count = 2;
            particle.waitDuration = 3;
            model.elements.add(particle);
        }

        {
            var lock = new ItemDisplayModelPart();
            lock.transformation = new AffineTransformation(
                    new Matrix4f().translate(0, -0.44f, 0.3f).scale(0.35f, 0.35f, 0.1f)
            );
            lock.transformation.getTranslation();
            lock.viewRange = 0.2f;
            lock.billboardMode = DisplayEntity.BillboardMode.CENTER;

            lock.tags.add(ModelTags.IF_REQUIRE_PAYMENT);
            lock.itemStack = IconItem.of(IconItem.Texture.REMOVE_PROTECTION);

            model.elements.add(lock);
        }

        {
            var lockText = new TextDisplayModelPart();
            lockText.transformation = new AffineTransformation(
                    new Matrix4f().translate(0, -0.31f, 0.3f).scale(0.35f)
            );
            lockText.textShadow = true;
            lockText.transformation.getTranslation();
            lockText.viewRange = 0.2f;
            lockText.billboardMode = DisplayEntity.BillboardMode.CENTER;

            lockText.tags.add(ModelTags.IF_REQUIRE_PAYMENT);
            lockText.text = TaggedText.of("<yellow>${cost}");

            model.elements.add(lockText);
        }

        addGenericText(model, customText -> {
            customText.transformation = new AffineTransformation(
                    new Matrix4f().translate(0, 0.25f, 0).scale(0.4f)
            );
            customText.textWidth = 9999;
            customText.textShadow = true;
            customText.brightness = new Brightness(15, 15);
            customText.billboardMode = DisplayEntity.BillboardMode.CENTER;
            customText.viewRange = 0.5f;
        });

        return model;
    }


    public static GraveModel corpseZombie() {
        var model = new GraveModel();

        {
            var entity = new EntityModelPart();
            entity.entityType = EntityType.SKELETON;
            entity.position = new Vec3d(0.9, -0.51, 0);
            entity.entityPose = EntityPose.SLEEPING;

            entity.tags.add(ModelTags.IF_UNPROTECTED);
            entity.tags.add(ModelTags.PLAYER_HEAD);
            entity.tags.add(ModelTags.EQUIPMENT_HELMET);
            entity.tags.add(ModelTags.EQUIPMENT_CHESTPLATE);
            entity.tags.add(ModelTags.EQUIPMENT_LEGGINGS);
            entity.tags.add(ModelTags.EQUIPMENT_BOOTS);
            entity.tags.add(ModelTags.EQUIPMENT_MAIN_HAND);
            entity.tags.add(ModelTags.EQUIPMENT_OFFHAND_HAND);

            model.elements.add(entity);
        }
        {
            var entity = new EntityModelPart();
            entity.entityType = EntityType.ZOMBIE;
            entity.position = new Vec3d(0.9, -0.51, 0);
            entity.entityPose = EntityPose.SLEEPING;

            entity.tags.add(ModelTags.IF_PROTECTED);
            entity.tags.add(ModelTags.PLAYER_HEAD);
            entity.tags.add(ModelTags.EQUIPMENT_HELMET);
            entity.tags.add(ModelTags.EQUIPMENT_CHESTPLATE);
            entity.tags.add(ModelTags.EQUIPMENT_LEGGINGS);
            entity.tags.add(ModelTags.EQUIPMENT_BOOTS);
            entity.tags.add(ModelTags.EQUIPMENT_MAIN_HAND);
            entity.tags.add(ModelTags.EQUIPMENT_OFFHAND_HAND);

            model.elements.add(entity);
        }


        {
            var head = new ItemDisplayModelPart();
            head.transformation = new AffineTransformation(
                    new Matrix4f().rotateY(MathHelper.HALF_PI).translate(0, -0.42f, 0).rotateX(-MathHelper.PI / 3).scale(0.35f)
            );
            head.transformation.getTranslation();

            head.tags.add(ModelTags.IF_REQUIRE_PAYMENT);
            head.itemStack = IconItem.of(IconItem.Texture.REMOVE_PROTECTION);

            model.elements.add(head);
        }

        {
            var head = new TextDisplayModelPart();
            head.transformation = new AffineTransformation(
                    new Matrix4f().rotateY(MathHelper.HALF_PI).translate(0, -0.29f, 0).rotateX(-MathHelper.PI / 3).scale(0.35f)
            );
            head.textShadow = true;
            head.transformation.getTranslation();

            head.tags.add(ModelTags.IF_REQUIRE_PAYMENT);
            head.text = TaggedText.of("<yellow>${cost}");

            model.elements.add(head);
        }


        addGenericText(model, customText -> {
            customText.transformation = new AffineTransformation(
                    new Matrix4f().translate(0, 0.15f, 0).scale(0.6f)
            );
            customText.textWidth = 9999;
            customText.textShadow = true;
            customText.brightness = new Brightness(15, 15);
            customText.billboardMode = DisplayEntity.BillboardMode.CENTER;
            customText.viewRange = 0.5f;
        });

        return model;
    }

    public static GraveModel corpsePlayer() {
        var model = new GraveModel();

        {
            var entity = new EntityModelPart();
            entity.entityType = EntityType.SKELETON;
            entity.position = new Vec3d(0.9, -0.51, 0);
            entity.entityPose = EntityPose.SLEEPING;

            entity.tags.add(ModelTags.IF_UNPROTECTED);
            entity.tags.add(ModelTags.PLAYER_HEAD);
            entity.tags.add(ModelTags.EQUIPMENT_HELMET);
            entity.tags.add(ModelTags.EQUIPMENT_CHESTPLATE);
            entity.tags.add(ModelTags.EQUIPMENT_LEGGINGS);
            entity.tags.add(ModelTags.EQUIPMENT_BOOTS);
            entity.tags.add(ModelTags.EQUIPMENT_MAIN_HAND);
            entity.tags.add(ModelTags.EQUIPMENT_OFFHAND_HAND);

            model.elements.add(entity);
        }
        {
            var entity = new EntityModelPart();
            entity.entityType = EntityType.PLAYER;
            entity.position = new Vec3d(0.9, -0.51, 0);
            entity.entityPose = EntityPose.SLEEPING;

            entity.tags.add(ModelTags.IF_PROTECTED);
            entity.tags.add(ModelTags.PLAYER_HEAD);
            entity.tags.add(ModelTags.EQUIPMENT_HELMET);
            entity.tags.add(ModelTags.EQUIPMENT_CHESTPLATE);
            entity.tags.add(ModelTags.EQUIPMENT_LEGGINGS);
            entity.tags.add(ModelTags.EQUIPMENT_BOOTS);
            entity.tags.add(ModelTags.EQUIPMENT_MAIN_HAND);
            entity.tags.add(ModelTags.EQUIPMENT_OFFHAND_HAND);

            model.elements.add(entity);
        }


        {
            var head = new ItemDisplayModelPart();
            head.transformation = new AffineTransformation(
                    new Matrix4f().rotateY(MathHelper.HALF_PI).translate(0, -0.42f, 0).rotateX(-MathHelper.PI / 3).scale(0.35f)
            );
            head.transformation.getTranslation();

            head.tags.add(ModelTags.IF_REQUIRE_PAYMENT);
            head.itemStack = IconItem.of(IconItem.Texture.REMOVE_PROTECTION);

            model.elements.add(head);
        }

        {
            var head = new TextDisplayModelPart();
            head.transformation = new AffineTransformation(
                    new Matrix4f().rotateY(MathHelper.HALF_PI).translate(0, -0.29f, 0).rotateX(-MathHelper.PI / 3).scale(0.35f)
            );
            head.textShadow = true;
            head.transformation.getTranslation();

            head.tags.add(ModelTags.IF_REQUIRE_PAYMENT);
            head.text = TaggedText.of("<yellow>${cost}");

            model.elements.add(head);
        }


        addGenericText(model, customText -> {
            customText.transformation = new AffineTransformation(
                    new Matrix4f().translate(0, 0.1f, 0).scale(0.6f)
            );
            customText.textWidth = 9999;
            customText.textShadow = true;
            customText.brightness = new Brightness(15, 15);
            customText.billboardMode = DisplayEntity.BillboardMode.CENTER;
            customText.viewRange = 0.5f;
        });

        return model;
    }

    public static GraveModel playerHead() {
        var model = new GraveModel();
        {
            var tool = new ItemDisplayModelPart();
            tool.transformation = new AffineTransformation(
                    new Matrix4f().translate(0.4f, -0.495f, -0.2f).rotateY(330 * MathHelper.RADIANS_PER_DEGREE).rotateZ(5 * MathHelper.RADIANS_PER_DEGREE).rotateX(MathHelper.HALF_PI).scale(0.5f)
            );
            tool.transformation.getTranslation();

            tool.tags.add(ModelTags.ITEM);

            model.elements.add(tool);
        }
        {
            var tool = new ItemDisplayModelPart();
            tool.transformation = new AffineTransformation(
                    new Matrix4f().translate(-0.35f, -0.43f, -0.05f).rotateY(80 * MathHelper.RADIANS_PER_DEGREE).rotateZ(-30 * MathHelper.RADIANS_PER_DEGREE).rotateX(-160 * MathHelper.RADIANS_PER_DEGREE).scale(0.5f)
            );
            tool.transformation.getTranslation();

            tool.tags.add(ModelTags.ITEM);

            model.elements.add(tool);
        }

        {
            var head = new ItemDisplayModelPart();
            head.transformation = new AffineTransformation(
                    new Matrix4f().translate(0, -0.35f, 0).rotateX(-MathHelper.PI / 12).rotateZ(-MathHelper.PI / 64)
            );
            head.transformation.getTranslation();

            var skull = head.copy();


            head.tags.add(ModelTags.IF_PROTECTED);
            head.tags.add(ModelTags.PLAYER_HEAD);
            head.itemStack = Items.PLAYER_HEAD.getDefaultStack();

            skull.tags.add(ModelTags.IF_UNPROTECTED);
            skull.itemStack = Items.SKELETON_SKULL.getDefaultStack();

            model.elements.add(head);
            model.elements.add(skull);
        }

        {
            var head = new ItemDisplayModelPart();
            head.transformation = new AffineTransformation(
                    new Matrix4f().translate(0, -0.44f, 0.42f).rotateX(-MathHelper.PI / 3).scale(0.35f)
            );
            head.transformation.getTranslation();

            head.tags.add(ModelTags.IF_REQUIRE_PAYMENT);
            head.itemStack = IconItem.of(IconItem.Texture.REMOVE_PROTECTION);

            model.elements.add(head);
        }

        {
            var head = new TextDisplayModelPart();
            head.transformation = new AffineTransformation(
                    new Matrix4f().translate(0, -0.31f, 0.42f).rotateX(-MathHelper.PI / 3).scale(0.35f)
            );
            head.textShadow = true;
            head.transformation.getTranslation();

            head.tags.add(ModelTags.IF_REQUIRE_PAYMENT);
            head.text = TaggedText.of("<yellow>${cost}");

            model.elements.add(head);
        }

        addGenericText(model, customText -> {
            customText.transformation = new AffineTransformation(
                    new Matrix4f().translate(0, 0.1f, 0).scale(0.6f)
            );
            customText.textWidth = 9999;
            customText.textShadow = true;
            customText.brightness = new Brightness(15, 15);
            customText.billboardMode = DisplayEntity.BillboardMode.CENTER;
            customText.viewRange = 0.5f;
        });

        return model;
    }

    private static void addGenericText(GraveModel model, Consumer<TextDisplayModelPart> baseModifier) {
        var customText = new TextDisplayModelPart();
        baseModifier.accept(customText);
        customText.transformation.getTranslation();

        var vis = customText.copy();
        var mainText = customText.copy();

        mainText.text = TaggedText.of(
                TaggedText.Line.of("<gold><lang:'text.graves.grave_of':'<yellow>${player}'>"),
                TaggedText.Line.of("<yellow>${death_cause}"),
                TaggedText.Line.of("<gray><lang:'text.graves.items_xp':'<white>${item_count}':'<white>${xp}'>"),
                TaggedText.Line.of("<blue><lang:'text.graves.protected_time':'<white>${protection_time}'><r>", ModelTags.IF_PROTECTED, ModelTags.HAS_PROTECTION_TIMER),
                TaggedText.Line.of("<red><lang:'text.graves.break_time':'<white>${break_time}'>", ModelTags.HAS_BREAKING_TIMER));

        vis.text = TaggedText.of(
                "<gold><lang:'text.graves.grave_of':'<yellow>${player}'>",
                "<yellow>${death_cause}"
        );
        mainText.tags.add(ModelTags.IF_NOT_VISUAL);

        customText.text = TaggedText.of("${text_1}", "${text_2}", "${text_3}", "${text_4}");
        customText.tags.add(ModelTags.IF_PLAYER_MADE);
        customText.tags.add(ModelTags.IF_VISUAL);

        vis.tags.add(ModelTags.IF_VISUAL);
        vis.tags.add(ModelTags.IF_NOT_PLAYER_MADE);

        model.elements.add(mainText);
        model.elements.add(customText);
        model.elements.add(vis);
    }
}
