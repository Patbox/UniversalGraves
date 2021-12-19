package eu.pb4.graves;

import net.fabricmc.loader.api.ModContainer;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;

import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class GenericModInfo {
    private static Text[] icon = new Text[0];
    private static Text[] about = new Text[0];
    private static Text[] consoleAbout = new Text[0];

    public static void build(ModContainer container) {
        {
            final String chr = "█";
            var icon = new ArrayList<MutableText>();
            try {
                var source = ImageIO.read(Files.newInputStream(container.getPath("assets/logo_mini.png")));

                for (int y = 0; y < source.getHeight(); y++) {
                    var base = new LiteralText("");
                    int line = 0;
                    int color = source.getRGB(0, y) & 0xFFFFFF;
                    for (int x = 0; x < source.getWidth(); x++) {
                        int colorPixel = source.getRGB(x, y) & 0xFFFFFF;

                        if (color == colorPixel) {
                            line++;
                        } else {
                            base.append(new LiteralText(chr.repeat(line)).setStyle(Style.EMPTY.withColor(color)));
                            color = colorPixel;
                            line = 1;
                        }
                    }

                    base.append(new LiteralText(chr.repeat(line)).setStyle(Style.EMPTY.withColor(color)));
                    icon.add(base);
                }
            } catch (Exception e) {
                e.printStackTrace();
                while (icon.size() < 16) {
                    icon.add(new LiteralText("/!\\ [ Invalid icon file ] /!\\").setStyle(Style.EMPTY.withColor(0xFF0000).withItalic(true)));
                }
            }

            GenericModInfo.icon = icon.toArray(new Text[0]);
        }

        {
            var about = new ArrayList<Text>();
            var aboutBasic = new ArrayList<Text>();
            var output = new ArrayList<Text>();

            try {
                about.add(new LiteralText(container.getMetadata().getName()).setStyle(Style.EMPTY.withColor(0x00e6b4).withBold(true).withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL,container.getMetadata().getContact().get("github").orElse("")))));
                about.add(new TranslatableText("text.graves.about.version").setStyle(Style.EMPTY.withColor(0xf7e1a7))
                        .append(new LiteralText(container.getMetadata().getVersion().getFriendlyString()).setStyle(Style.EMPTY.withColor(Formatting.WHITE))));

                aboutBasic.addAll(about);
                aboutBasic.add(LiteralText.EMPTY);
                aboutBasic.add(Text.of(container.getMetadata().getDescription()));

                var contributors = new ArrayList<String>();
                contributors.addAll(container.getMetadata().getAuthors().stream().map((p) -> p.getName()).collect(Collectors.toList()));
                contributors.addAll(container.getMetadata().getContributors().stream().map((p) -> p.getName()).collect(Collectors.toList()));

                about.add(new LiteralText("")
                        .append(new TranslatableText("text.graves.about.contributors")
                                .setStyle(Style.EMPTY.withColor(Formatting.AQUA)
                                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                                new LiteralText(String.join(", ", contributors)
                                        ))
                                )))
                        .append("")
                        .setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY)));
                about.add(LiteralText.EMPTY);

                var desc = new ArrayList<>(List.of(container.getMetadata().getDescription().split(" ")));

                if (desc.size() > 0) {
                    StringBuilder descPart = new StringBuilder();
                    while (!desc.isEmpty()) {
                        (descPart.isEmpty() ? descPart : descPart.append(" ")).append(desc.remove(0));

                        if (descPart.length() > 16) {
                            about.add(new LiteralText(descPart.toString()).setStyle(Style.EMPTY.withColor(Formatting.GRAY)));
                            descPart = new StringBuilder();
                        }
                    }

                    if (descPart.length() > 0) {
                        about.add(new LiteralText(descPart.toString()).setStyle(Style.EMPTY.withColor(Formatting.GRAY)));
                    }
                }

                if (icon.length > about.size() + 2) {
                    int a = 0;
                    for (int i = 0; i < icon.length; i++) {
                        if (i == (icon.length - about.size() - 1) / 2 + a && a < about.size()) {
                            output.add(icon[i].shallowCopy().append(new LiteralText("  ").setStyle(Style.EMPTY.withItalic(false)).append(about.get(a++))));
                        } else {
                            output.add(icon[i]);
                        }
                    }
                } else {
                    Collections.addAll(output, icon);
                    output.addAll(about);
                }
            } catch (Exception e) {
                e.printStackTrace();
                var invalid = new LiteralText("/!\\ [ Invalid about mod info ] /!\\").setStyle(Style.EMPTY.withColor(0xFF0000).withItalic(true));

                output.add(invalid);
                about.add(invalid);
            }

            GenericModInfo.about = output.toArray(new Text[0]);
            GenericModInfo.consoleAbout = aboutBasic.toArray(new Text[0]);
        }
    }

    public static Text[] getIcon() {
        return icon;
    }

    public static Text[] getAboutFull() {
        return about;
    }

    public static Text[] getAboutConsole() {
        return consoleAbout;
    }
}
