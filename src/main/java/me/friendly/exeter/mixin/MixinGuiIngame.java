package me.friendly.exeter.mixin;

import me.friendly.api.minecraft.render.RenderMethods;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.events.RenderGameOverlayEvent;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(Gui.class)
public abstract class MixinGuiIngame {

    @Inject(
        method = "render",
        at = @At("HEAD"))
    private void onRender(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        RenderMethods.guiGraphics = guiGraphics;
        Exeter.getInstance().getEventManager().dispatch(
            new RenderGameOverlayEvent(RenderGameOverlayEvent.Type.IN_GAME));
    }

}
