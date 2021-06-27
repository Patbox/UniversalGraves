package eu.pb4.graves.config;


import eu.pb4.graves.grave.GravesLookType;
import eu.pb4.graves.config.data.ConfigData;
import eu.pb4.placeholders.TextParser;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public final class Config {
    public final ConfigData configData;
    public final GravesLookType style;

    public final List<Text> hologramProtectedText;
    public final List<Text> hologramText;

    public final Text graveTitle;

    public final Text guiTitle;
    public final List<Text> guiProtectedText;
    public final List<Text> guiText;

    public final Text noLongerProtectedMessage;
    public final Text graveExpiredMessage;
    public final Text graveBrokenMessage;
    public final Text createdGraveMessage;
    public final Text creationFailedGraveMessage;
    public final Text creationFailedPvPGraveMessage;


    public Config(ConfigData data) {
        this.configData = data;
        this.style = GravesLookType.byName(configData.graveType);
        this.hologramProtectedText = parse(data.hologramProtectedText);
        this.hologramText = parse(data.hologramText);

        this.graveTitle = TextParser.parse(data.graveTitle);

        this.guiTitle = TextParser.parse(data.guiTitle);
        this.guiProtectedText = parse(data.guiProtectedText);
        this.guiText = parse(data.guiText);

        this.noLongerProtectedMessage = TextParser.parse(data.noLongerProtectedMessage);
        this.graveExpiredMessage = TextParser.parse(data.graveExpiredMessage);
        this.graveBrokenMessage = TextParser.parse(data.graveBrokenMessage);
        this.createdGraveMessage = TextParser.parse(data.createdGraveMessage);
        this.creationFailedGraveMessage = TextParser.parse(data.creationFailedGraveMessage);
        this.creationFailedPvPGraveMessage = TextParser.parse(data.creationFailedPvPGraveMessage);
    }


    public String getFormattedTime(long time) {
        if (time != Long.MAX_VALUE) {

            long seconds = time % 60;
            long minutes = (time / 60) % 60;
            long hours = (time / (60 * 60)) % 24;
            long days = time / (60 * 60 * 24) % 365;
            long years = time / (60 * 60 * 24 * 365);

            StringBuilder builder = new StringBuilder();

            if (years > 0) {
                builder.append(years + configData.yearsText);
            }
            if (days > 0) {
                builder.append(days + configData.daysText);
            }
            if (hours > 0) {
                builder.append(hours + configData.hoursText);
            }
            if (minutes > 0) {
                builder.append(minutes + configData.minutesText);
            }
            if (seconds >= 0) {
                builder.append(seconds + configData.secondsText);
            } else {
                builder.append(time + configData.secondsText);
            }
            return builder.toString();
        } else {
            return configData.neverExpires;
        }
    }

    public static List<Text> parse(List<String> strings) {
        List<Text> texts = new ArrayList<>();

        for (String line : strings) {
            texts.add(TextParser.parse(line));
        }
        return texts;
    }
}
