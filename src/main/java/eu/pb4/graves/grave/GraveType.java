package eu.pb4.graves.grave;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum GraveType {
    BLOCK,
    VIRTUAL;

    private static final Map<String, GraveType> BY_NAME = Arrays.stream(values()).collect(Collectors.toMap((f) -> f.name(), (f) -> f));

    public static GraveType byName(String name) {
        return BY_NAME.get(name);
    }
}
