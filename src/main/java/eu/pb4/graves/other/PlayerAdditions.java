package eu.pb4.graves.other;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public interface PlayerAdditions {
    @Nullable
    Component graves$lastDeathCause();

    @Nullable
    long graves$lastGrave();

    void graves$setLastGrave(long graveId);

    void graves$setInvulnerable(boolean value);

    void graves$tryGiveCompass();
}
