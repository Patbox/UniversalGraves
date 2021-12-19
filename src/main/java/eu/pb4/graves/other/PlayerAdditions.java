package eu.pb4.graves.other;

import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public interface PlayerAdditions {
    @Nullable
    Text graves_lastDeathCause();

    @Nullable
    Location graves_lastGrave();

    void graves_setLastGrave(@Nullable Location location);
}
