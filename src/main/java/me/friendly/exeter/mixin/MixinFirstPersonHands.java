package me.friendly.exeter.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Old style for the Swing module: drops the swing's vertical bob so the item never shifts downward
 * mid-swing. First-person only, so no entity check needed.
 */
@Mixin(value = FirstPersonHandsAndItemsRenderer.class)
public class MixinFirstPersonHands {

  @Redirect(
      method =
          "swingArm(FLcom/mojang/blaze3d/vertex/PoseStack;ILnet/minecraft/world/entity/HumanoidArm;)V",
      at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"))
  private void flattenSwingY(PoseStack pose, float x, float y, float z) {
    // Flat Swing forces X and Z off inside translateX/translateZ already.
    pose.translate(
        me.friendly.exeter.module.impl.toggle.misc.Swing.translateX() ? x : 0.0f,
        me.friendly.exeter.module.impl.toggle.misc.Swing.translateY() ? y : 0.0f,
        me.friendly.exeter.module.impl.toggle.misc.Swing.translateZ() ? z : 0.0f);
  }

  /**
   * Flat style continued: the attack rotations, reimplemented from the vanilla math so each axis
   * gates individually. Untouched axes compute identically; when all three are on, vanilla runs
   * instead (see below).
   */
  @Inject(
      method =
          "applyItemArmAttackTransform(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/HumanoidArm;F)V",
      at = @At("HEAD"),
      cancellable = true)
  private void gateAttackTransform(
      PoseStack pose, HumanoidArm arm, float progress, CallbackInfo ci) {
    boolean x = me.friendly.exeter.module.impl.toggle.misc.Swing.rotateX();
    boolean y = me.friendly.exeter.module.impl.toggle.misc.Swing.rotateY();
    boolean z = me.friendly.exeter.module.impl.toggle.misc.Swing.rotateZ();
    if (x && y && z) return;
    int sign = arm == HumanoidArm.RIGHT ? 1 : -1;
    float wave = Mth.sin(progress * progress * 3.1415927f);
    float sway = Mth.sin(Mth.sqrt(progress) * 3.1415927f);
    if (y) {
      pose.rotateDegrees(com.mojang.math.Axis.YP, sign * (45.0f + wave * -20.0f));
      pose.rotateDegrees(com.mojang.math.Axis.YP, sign * -45.0f);
    }
    if (z) {
      pose.rotateDegrees(com.mojang.math.Axis.ZP, sign * sway * -20.0f);
    }
    if (x) {
      pose.rotateDegrees(com.mojang.math.Axis.XP, sway * -80.0f);
    }
    ci.cancel();
  }
}
