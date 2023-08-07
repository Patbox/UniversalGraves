package eu.pb4.graves.model;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.graves.config.data.WrappedText;
import eu.pb4.placeholders.api.node.TextNode;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.*;

public record TaggedText(List<Line> entry, Text direct) {
    public static final TaggedText EMPTY = new TaggedText(List.of(), Text.empty());
    private static final Codec<WrappedText> BASE_TEXT_NODE = Codec.STRING.xmap(WrappedText::of, WrappedText::input);

    private static final Codec<Line> LINE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    BASE_TEXT_NODE.fieldOf("text").forGetter(Line::node),
                    Codec.list(Identifier.CODEC).xmap(HashSet::new, ArrayList::new).optionalFieldOf("tags", new HashSet<>()).forGetter(Line::requiredTags)
            ).apply(instance, Line::new)
    );

    private static final Codec<List<Line>> LINES = Codec.list(Codec.either(BASE_TEXT_NODE, LINE_CODEC)
            .flatXmap((either) -> DataResult.success(either.right().orElseGet(either.left().map(Line::of)::get)),
                    (x) -> DataResult.success(x.requiredTags.isEmpty() ? Either.left(x.node) : Either.right(x))));


    public static final Codec<TaggedText> CODEC = Codec.either(LINES, BASE_TEXT_NODE)
            .xmap(either -> either.left().orElseGet(() -> List.of(Line.of(either.right().get()))), Either::left).xmap(TaggedText::of, TaggedText::entry);


    public static TaggedText of(Line... strings) {
        return TaggedText.of(List.of(strings));
    }

    public static TaggedText of(String... strings) {
        return TaggedText.of(Arrays.stream(strings).map(x -> Line.of(WrappedText.of(x))).toList());
    }

    private static TaggedText of(List<Line> list) {
        var t = Text.empty();
        var iter = list.iterator();
        var nl = Text.literal("\n");
        while (iter.hasNext()) {
            var l = iter.next();
            t.append(l.node.text());
            if (iter.hasNext()) {
               t.append(nl);
            }
        }

        return new TaggedText(list, t);
    }

    public record Line(WrappedText node, HashSet<Identifier> requiredTags) {
        public static Line of(WrappedText wrappedText) {
            return new Line(wrappedText, new HashSet<>());
        }

        public static Line of(String line, Identifier... tags) {
            return new Line(WrappedText.of(line), new HashSet<>(List.of(tags)));
        }

        public boolean containsTags(Set<Identifier> tags) {
            for (var tag : tags) {
                if (this.requiredTags.contains(tag)) {
                    return true;
                }
            }
            return false;
        }
    }
}
