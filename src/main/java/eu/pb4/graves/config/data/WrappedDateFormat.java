package eu.pb4.graves.config.data;

import java.text.SimpleDateFormat;

public record WrappedDateFormat(String pattern, SimpleDateFormat format) {
    public static WrappedDateFormat of(String pattern) {
        return new WrappedDateFormat(pattern, new SimpleDateFormat(pattern));
    }
}
