package eu.pb4.graves.model;

import com.google.common.base.Suppliers;
import com.google.gson.annotations.SerializedName;
import eu.pb4.graves.GravesMod;
import eu.pb4.graves.config.BaseGson;
import eu.pb4.graves.config.ConfigManager;
import eu.pb4.graves.config.data.WrappedText;
import eu.pb4.graves.registry.IconItem;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.decoration.Brightness;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.AffineTransformation;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class GraveModel {
    public static final Supplier<GraveModel> DEFAULT_MODEL = GravesMod.DEV ? GraveModel::getDefaultModel : Suppliers.memoize(GraveModel::getDefaultModel);

    @SerializedName("format")
    public int format = 0;

    @SerializedName("elements")
    public List<ModelPart> elements = new ArrayList<>();

    public static void setup(String model, Set<Identifier> ignoredTags, Consumer<ModelPart> builder) {
        ConfigManager.getModel(model, DEFAULT_MODEL.get()).setup(ignoredTags, builder);
    }

    public void setup(Set<Identifier> ignoredTags, Consumer<ModelPart> builder) {
        main:
        for (var element : elements) {

            for (var flag : element.tags) {
                if (ignoredTags.contains(flag)) {
                    continue main;
                }
            }

            builder.accept(element);
        }
    }


    public static GraveModel getDefaultModel() {
        var model = new GraveModel();
        {
            var head = new ModelPart();
            head.type = ModelPart.Type.ITEM;
            head.transformation = new AffineTransformation(
                    new Matrix4f().translate(0, -0.35f, 0).rotateX(-MathHelper.PI / 12).rotateZ(-MathHelper.PI / 64)
            );
            head.transformation.getTranslation();

            var skull = head.copy();


            head.tags.add(ModelPart.Tags.IF_PROTECTED);
            head.tags.add(ModelPart.Tags.PLAYER_HEAD);
            head.itemStack = Items.PLAYER_HEAD.getDefaultStack();

            skull.tags.add(ModelPart.Tags.IF_UNPROTECTED);
            skull.itemStack = Items.SKELETON_SKULL.getDefaultStack();

            model.elements.add(head);
            model.elements.add(skull);
        }

        {
            var head = new ModelPart();
            head.type = ModelPart.Type.ITEM;
            head.transformation = new AffineTransformation(
                    new Matrix4f().translate(0, -0.48f, 0.42f).rotateX(-MathHelper.PI / 3).scale(0.35f)
            );
            head.transformation.getTranslation();

            head.tags.add(ModelPart.Tags.IF_REQUIRE_PAYMENT);
            head.itemStack = IconItem.of(IconItem.Texture.REMOVE_PROTECTION);

            model.elements.add(head);
        }

        {
            var head = new ModelPart();
            head.type = ModelPart.Type.TEXT;
            head.transformation = new AffineTransformation(
                    new Matrix4f().translate(0, -0.35f, 0.42f).rotateX(-MathHelper.PI / 3).scale(0.35f)
            );
            head.textShadow = true;
            head.transformation.getTranslation();

            head.tags.add(ModelPart.Tags.IF_REQUIRE_PAYMENT);
            head.text = WrappedText.of("<yellow>${cost}");

            model.elements.add(head);
        }

        {
            var text = new ModelPart();
            text.type = ModelPart.Type.TEXT;
            text.transformation = new AffineTransformation(
                    new Matrix4f().translate(0, 0.1f, 0).scale(0.6f)
            );
            text.textWidth = 9999;
            text.textShadow = true;
            text.brightness = new Brightness(15, 15);
            text.billboardMode = DisplayEntity.BillboardMode.CENTER;
            text.viewRange = 0.5f;
            text.transformation.getTranslation();

            var vis = text.copy();
            var prot = text.copy();
            var unProt = text.copy();

            prot.text = WrappedText.of("""
                    <gold><lang:'text.graves.grave_of':'<yellow>${player}'><r>
                    <yellow>${death_cause}<r>
                                       
                    <gray><lang:'text.graves.items_xp':'<white>${item_count}':'<white>${xp}'><r>
                    <blue><lang:'text.graves.protected_time':'<white>${protection_time}'><r>
                    <red><lang:'text.graves.break_time':'<white>${break_time}'><r>""");
            unProt.text = WrappedText.of("""
                    <gold><lang:'text.graves.grave_of':'<yellow>${player}'><r>
                    <yellow>${death_cause}<r>
                                       
                    <gray><lang:'text.graves.items_xp':'<white>${item_count}':'<white>${xp}'><r>
                    <red><lang:'text.graves.break_time':'<white>${break_time}'><r>""");

            vis.text = WrappedText.of("""
                    <gold><lang:'text.graves.grave_of':'<yellow>${player}'><r>
                    <yellow>${death_cause}""");

            prot.tags.add(ModelPart.Tags.IF_PROTECTED);
            prot.tags.add(ModelPart.Tags.IF_NOT_VISUAL);
            unProt.tags.add(ModelPart.Tags.IF_UNPROTECTED);
            unProt.tags.add(ModelPart.Tags.IF_NOT_VISUAL);

            text.text = WrappedText.of("${text_1}\n${text_2}\n${text_3}\n${text_4}");
            text.tags.add(ModelPart.Tags.IF_PLAYER_MADE);
            text.tags.add(ModelPart.Tags.IF_VISUAL);

            vis.tags.add(ModelPart.Tags.IF_VISUAL);
            vis.tags.add(ModelPart.Tags.IF_NOT_PLAYER_MADE);

            model.elements.add(prot);
            model.elements.add(unProt);
            model.elements.add(text);
            model.elements.add(vis);
        }

        return model;
    }
}