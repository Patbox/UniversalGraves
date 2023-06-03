package eu.pb4.graves.other;

import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.api.node.TextNode;
import net.minecraft.text.Text;

import java.util.Map;
import java.util.function.Function;

public record DynamicNode(String key, Text text) implements TextNode {
    public static DynamicNode of(String key) {
        return new DynamicNode(key, Text.literal("${" + key + "}"));
    }

    public static final ParserContext.Key<Function<String, Text>> NODES = new ParserContext.Key<>("graves:dynamic", null);

    @Override
    public Text toText(ParserContext context, boolean removeBackslashes) {
        var x = context.get(NODES);
        if (x != null) {
            var value = x.apply(key);
            return value != null ? value : text;
        }
        return text;
    }

    @Override
    public boolean isDynamic() {
        return true;
    }
}
