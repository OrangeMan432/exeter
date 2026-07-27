package me.friendly.exeter.mixin;

import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.logging.Logger;
import me.friendly.exeter.module.Module;
import me.friendly.exeter.module.impl.active.render.NoRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


/**
 * This class is not present in the original
 * Exeter 1.8 client. It was added as part
 * of the Fabric 1.21.11 port
 *
 * @author OrangeMan432
 */
@Mixin(InGameHud.class)
public abstract class MixinGuiIngame {
    private static final Module norender = new NoRender();

    @Inject(
        method = "renderPumpkinOverlay",
        at = @At("HEAD"),
        cancellable = true)
    protected void renderPumpkinOverlayHook(ScaledResolution scaledResolution, CallbackInfo info) {
        if ((Boolean)norender.getPropertyByAlias("NoPumpkin").getValue()) {
            info.cancel(); // this doesnt work
        }
    }

}
