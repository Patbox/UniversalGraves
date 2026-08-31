package eu.pb4.graves.other;

import eu.pb4.graves.grave.Grave;
import net.minecraft.server.level.ServerPlayer;

public interface GraveUnlockCost {
    GenericCost<?> quote(Grave grave, boolean owner);

    default GenericCost<?> quote(Grave grave, ServerPlayer player) {
        return this.quote(grave, grave.isOwner(player));
    }

    default GenericCost<?> baseQuote(Grave grave) {
        return this.quote(grave, true);
    }

    boolean requiresPayment();

    record Static(GenericCost<?> cost) implements GraveUnlockCost {
        @Override
        public GenericCost<?> quote(Grave grave, boolean owner) {
            return this.cost;
        }

        @Override
        public GenericCost<?> baseQuote(Grave grave) {
            return this.cost;
        }

        @Override
        public boolean requiresPayment() {
            return !this.cost.isFree();
        }
    }
}
