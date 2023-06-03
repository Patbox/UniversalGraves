package eu.pb4.graves.config.data;

import com.google.gson.annotations.SerializedName;

public class Variant<T> {
    @SerializedName("base")
    public T a;
    @SerializedName("alt")
    public T b;

    public static <T> Variant<T> of(T protectedValue, T unprotectedValue) {
        var x = new Variant<T>();
        x.a = protectedValue;
        x.b = unprotectedValue;
        return x;
    }

    public static <T> Variant<T> of(T value) {
        var x = new Variant<T>();
        x.a = value;
        x.b = value;
        return x;
    }

    public T get(boolean active) {
        return active ? a : b;
    }
}
