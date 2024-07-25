package eu.pb4.graves.config.data;

import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.api.PlaceholderContext;
import eu.pb4.placeholders.api.node.TextNode;
import eu.pb4.placeholders.api.parsers.*;
import net.minecraft.text.Text;

import java.util.Map;
import java.util.function.Function;

public record WrappedText(String input, TextNode textNode, Text text) {
    public static final ParserContext.Key<Function<String, Text>> DYNAMIC_NODES = ParserContext.Key.of("graves:dynamic");

    public static final NodeParser PARSER = NodeParser.builder()
            .simplifiedTextFormat()
            .quickText()
            .placeholders(TagLikeParser.PLACEHOLDER_USER, DYNAMIC_NODES)
            .staticPreParsing()
            .build();

    public static final WrappedText EMPTY = new WrappedText("", TextNode.empty(), Text.empty());

    public static WrappedText of(String input) {
        if (input.isEmpty()) {
            return EMPTY;
        }

        return new WrappedText(input, PARSER.parseNode(input), PARSER.parseNode(input).toText());
    }

    public Text with(Map<String, Text> textMap) {
        return with(textMap::get);
    }
    public Text with(Function<String, Text> textMap) {
        return this.textNode.toText(ParserContext.of(DYNAMIC_NODES, textMap));
    }
    public Text with(PlaceholderContext context, Map<String, Text> textMap) {
        return with(context, textMap::get);
    }
    public Text with(PlaceholderContext context, Function<String, Text> textMap) {
        return this.textNode.toText(ParserContext.of(DYNAMIC_NODES, textMap).with(PlaceholderContext.KEY, context));
    }

    public boolean isEmpty() {
        return this.input.isEmpty();
    }
}
