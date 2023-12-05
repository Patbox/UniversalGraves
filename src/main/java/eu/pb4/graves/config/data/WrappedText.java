package eu.pb4.graves.config.data;

import eu.pb4.graves.other.DynamicNode;
import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.api.PlaceholderContext;
import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.placeholders.api.node.TextNode;
import eu.pb4.placeholders.api.parsers.NodeParser;
import eu.pb4.placeholders.api.parsers.PatternPlaceholderParser;
import eu.pb4.placeholders.api.parsers.StaticPreParser;
import eu.pb4.placeholders.api.parsers.TextParserV1;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;

import java.util.Map;
import java.util.function.Function;

public record WrappedText(String input, TextNode textNode, Text text) {
    public static final NodeParser CONTEXTLESS = NodeParser.merge(
            TextParserV1.DEFAULT, Placeholders.DEFAULT_PLACEHOLDER_PARSER,
            new PatternPlaceholderParser(PatternPlaceholderParser.PREDEFINED_PLACEHOLDER_PATTERN, DynamicNode::of),
            StaticPreParser.INSTANCE
    );

    public static final NodeParser PARSER = NodeParser.merge(
            TextParserV1.DEFAULT, Placeholders.DEFAULT_PLACEHOLDER_PARSER,
            Placeholders.DEFAULT_PLACEHOLDER_PARSER,
            new PatternPlaceholderParser(PatternPlaceholderParser.PREDEFINED_PLACEHOLDER_PATTERN, DynamicNode::of),
            StaticPreParser.INSTANCE
    );

    public static final WrappedText EMPTY = new WrappedText("", TextNode.empty(), Text.empty());

    public static WrappedText of(String input) {
        if (input.isEmpty()) {
            return EMPTY;
        }

        return new WrappedText(input, CONTEXTLESS.parseNode(input), CONTEXTLESS.parseNode(input).toText());
    }

    public Text with(Map<String, Text> textMap) {
        return with(textMap::get);
    }
    public Text with(Function<String, Text> textMap) {
        return this.textNode.toText(ParserContext.of(DynamicNode.NODES, textMap));
    }
    public Text with(PlaceholderContext context, Map<String, Text> textMap) {
        return with(context, textMap::get);
    }
    public Text with(PlaceholderContext context, Function<String, Text> textMap) {
        return this.textNode.toText(ParserContext.of(DynamicNode.NODES, textMap).with(PlaceholderContext.KEY, context));
    }

    public boolean isEmpty() {
        return this.input.isEmpty();
    }
}
