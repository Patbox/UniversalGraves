package eu.pb4.graves.model;

import com.mojang.authlib.GameProfile;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;

public interface ModelDataProvider {
    String getGraveModelId();
    boolean isGraveProtected();
    boolean isGraveBroken();
    boolean isGravePlayerMade();
    boolean isGravePaymentRequired();
    Text getGravePlaceholder(String id);
    ProfileComponent getGraveGameProfile();
    ItemStack getGraveSlotItem(int i);
    ItemStack getGraveTaggedItem(Identifier identifier);
    Arm getGraveMainArm();
    byte getGraveSkinModelLayers();
}
