package eu.pb4.graves.config.data;

import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.api.PlaceholderContext;
import eu.pb4.placeholders.api.node.TextNode;
import eu.pb4.placeholders.api.parsers.*;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.network.chat.Component;

public record WrappedText(String input, TextNode textNode, Component text) {
    public static final ParserContext.Key<Function<String, Component>> DYNAMIC_NODES = ParserContext.Key.of("graves:dynamic");

    public static final NodeParser PARSER = NodeParser.builder()
            .simplifiedTextFormat()
            .quickText()
            .placeholders(TagLikeParser.PLACEHOLDER_USER, DYNAMIC_NODES)
            .staticPreParsing()
            .build();

    public static final WrappedText EMPTY = new WrappedText("", TextNode.empty(), Component.empty());

    public static WrappedText of(String input) {
        if (input.isEmpty()) {
            return EMPTY;
        }

        return new WrappedText(input, PARSER.parseNode(input), PARSER.parseNode(input).toComponent());
    }

    public Component with(Map<String, Component> textMap) {
        return with(textMap::get);
    }
    public Component with(Function<String, Component> textMap) {
        return this.textNode.toComponent(ParserContext.of(DYNAMIC_NODES, textMap));
    }
    public Component with(PlaceholderContext context, Map<String, Component> textMap) {
        return with(context, textMap::get);
    }
    public Component with(PlaceholderContext context, Function<String, Component> textMap) {
        return this.textNode.toComponent(ParserContext.of(DYNAMIC_NODES, textMap).with(PlaceholderContext.COMMON_KEY, context));
    }

    public boolean isEmpty() {
        return this.input.isEmpty();
    }
}
