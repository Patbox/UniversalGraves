package eu.pb4.graves.model;

import com.google.gson.annotations.SerializedName;
import eu.pb4.graves.config.ConfigManager;
import eu.pb4.graves.model.parts.ModelPart;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class GraveModel {
    @SerializedName("format")
    public int format = 0;

    @SerializedName("tick_time")
    public int tickTime = 20;

    @SerializedName("elements")
    public List<ModelPart> elements = new ArrayList<>();

    public static GraveModel setup(String model, Set<Identifier> ignoredTags, Consumer<ModelPart<?, ?>> builder) {
        var graveModel = ConfigManager.getModel(model);
        graveModel.setup(ignoredTags, builder);
        return graveModel;
    }

    public void setup(Set<Identifier> ignoredTags, Consumer<ModelPart<?, ?>> builder) {
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
}