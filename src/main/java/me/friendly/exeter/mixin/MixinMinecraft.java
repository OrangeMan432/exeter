package me.friendly.exeter.mixin;

import me.friendly.exeter.core.Exeter;
import net.minecraft.client.Minecraft;
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
@Mixin(Minecraft.class)
public abstract class MixinMinecraft
{
    @Inject(
            method = "init",
            at = @At("HEAD"))
    private void initHook2(CallbackInfo ci)
    {
        new Exeter();
    }
}
