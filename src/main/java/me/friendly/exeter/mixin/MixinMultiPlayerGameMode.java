package me.friendly.exeter.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Steady Hand for the Swing module: attacks reset the crosshair cooldown as usual, but not the hand
 * dip (resetOnlyAttackStrengthTicker skips exactly the itemSwapTicker reset). Item-switch dips are
 * untouched.
 */
@Mixin(value = MultiPlayerGameMode.class)
public class MixinMultiPlayerGameMode {

  @Redirect(
      method =
          "attack(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;)V",
      at =
          @At(
              value = "INVOKE",
              target = "Lnet/minecraft/world/entity/player/Player;resetAttackStrengthTicker()V"))
  private void keepHandUp(Player player) {
    if (me.friendly.exeter.module.impl.toggle.misc.Swing.steadyHand()) {
      player.resetOnlyAttackStrengthTicker();
      return;
    }
    player.resetAttackStrengthTicker();
  }
}
