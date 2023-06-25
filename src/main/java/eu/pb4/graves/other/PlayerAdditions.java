package eu.pb4.graves.other;

import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public interface PlayerAdditions {
    @Nullable
    Text graves$lastDeathCause();

    @Nullable
    long graves$lastGrave();

    void graves$setLastGrave(long graveId);

    void graves$setInvulnerable(boolean value);
}
