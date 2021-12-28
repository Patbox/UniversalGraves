package eu.pb4.graves.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import eu.pb4.graves.client.ClientGraveUi;
import eu.pb4.graves.client.GravesModClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GenericContainerScreen.class)
public abstract class GenericContainerScreenMixin extends HandledScreen<GenericContainerScreenHandler> implements ClientGraveUi {
    @Unique
    private boolean grave_enableTexture;

    public GenericContainerScreenMixin(GenericContainerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void graves_customUiTexture(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (this.grave_enableTexture) {
            this.renderBackground(matrices);

            super.render(matrices, mouseX, mouseY, delta);

            { // Mouse
                if (this.handler.getCursorStack().isEmpty() && this.focusedSlot != null && this.focusedSlot.hasStack()) {
                    if (this.focusedSlot.inventory == this.handler.getInventory() && this.focusedSlot.id > 35 && this.focusedSlot.id < 47) {
                        this.renderTooltip(matrices, this.focusedSlot.getStack().getTooltip(MinecraftClient.getInstance().player, TooltipContext.Default.NORMAL), mouseX, mouseY);
                    } else {
                        this.renderTooltip(matrices, this.focusedSlot.getStack(), mouseX, mouseY);
                    }
                }
            }

            ci.cancel();
        }
    }

    @Inject(method = "drawBackground", at = @At("HEAD"), cancellable = true)
    private void graves_customUiTexture(MatrixStack matrices, float delta, int mouseX, int mouseY, CallbackInfo ci) {
        if (this.grave_enableTexture) {
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShaderTexture(0, GravesModClient.UI_TEXTURE);
            int i = (this.width - this.backgroundWidth) / 2;
            int j = (this.height - this.backgroundHeight) / 2;
            this.drawTexture(matrices, i, j, 0, 0, this.backgroundWidth, this.backgroundHeight);

            ci.cancel();
        }
    }

    @Override
    public void grave_set() {
        this.grave_enableTexture = true;

        for (int i = 36; i < 47; i++) {
            var slot = this.handler.slots.get(i);

            if (slot.inventory == this.handler.getInventory() && slot.getStack().isEmpty()) {
                this.handler.slots.set(i, new Slot(slot.inventory, slot.id, slot.x, slot.y) {
                    @Override
                    public boolean isEnabled() {
                        return false;
                    }
                });
            }
        }
    }
}
