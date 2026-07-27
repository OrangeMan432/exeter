package me.friendly.exeter.mixin;

import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.events.RenderGameOverlayEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiSubtitleOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * This class is not present in the original
 * Exeter 1.8 client. It was added as part
 * of the Fabric 1.21.11 port
 *
 * @author Gopro336
 */
@Mixin(GuiSubtitleOverlay.class)
public abstract class MixinGuiSubtitleOverlay {

    @Inject(
        method = "renderSubtitles",
        at = @At(value = "HEAD"))
    private void renderSubtitlesHook(CallbackInfo info) {

        RenderGameOverlayEvent overlayEvent
                = new RenderGameOverlayEvent(null);

        Exeter.getInstance().getEventManager()
                .dispatch(overlayEvent);
    }

}