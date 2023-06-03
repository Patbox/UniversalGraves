package eu.pb4.graves.config.data;

import eu.pb4.graves.registry.IconItem;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record IconData(ItemStack baseStack, List<WrappedText> text) {

    public GuiElementBuilder builder(Map<String, Text> textMap) {
        var list = new ArrayList<Text>();
        for (var x : text) {
            list.add(x.with(textMap));
        }

        return GuiElementBuilder.from(baseStack).setName(list.remove(0)).setLore(list).hideFlags();
    }

    public GuiElementBuilder builder() {
        return builder(Map.of());
    }

    public static IconData of(Item icon, String... text) {
        return of(icon, List.of(text));
    }

    public static IconData of(Item icon, List<String> texts) {
        return new IconData(icon.getDefaultStack(), parseTexts(texts));
    }

    public static IconData of(IconItem.Texture texture, String... text) {
        return of(texture, List.of(text));
    }

    public static IconData of(IconItem.Texture texture, List<String> texts) {
        return new IconData(IconItem.of(texture), parseTexts(texts));
    }

    public static IconData of(ItemStack stack, List<String> texts) {
        return new IconData(stack, parseTexts(texts));
    }

    private static List<WrappedText> parseTexts(List<String> texts) {
        List<WrappedText> list = new ArrayList<>();
        for (String s : texts) {
            WrappedText of = WrappedText.of(s);
            list.add(of);
        }
        if (list.isEmpty()) {
            list.add(WrappedText.EMPTY);
        }

        return list;
    }
}
