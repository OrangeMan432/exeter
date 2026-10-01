package me.friendly.exeter.beta.mixin;

import me.friendly.exeter.beta.mixin.MinecraftAccessor;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.module.Module;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.module.impl.toggle.world.fakeplayer.FakePlayerModule;
import me.friendly.exeter.module.impl.toggle.world.fakeplayer.util.FakePlayerEntity;
import net.minecraft.MultiplayerInteractionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies melee damage to the client-side fake player on multiplayer, where the server
 * would otherwise ignore the unknown entity id. Fixed 2 damage approximated for a hit.
 */
@Mixin(MultiplayerInteractionManager.class)
public class MixinFakePlayerAttack {

  @Inject(
      method = "attackEntity(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/entity/Entity;)V",
      at = @At("HEAD"),
      cancellable = true)
  private void onAttack(PlayerEntity player, Entity target, CallbackInfo info) {
    if (Exeter.getInstance() == null) {
      return;
    }
    Module module = Exeter.getInstance().getModuleManager().getModuleByAlias("fakeplayer");
    if (!(module instanceof ToggleableModule) || !((ToggleableModule) module).isRunning()) {
      return;
    }
    if (!(target instanceof FakePlayerEntity)) {
      return;
    }
    FakePlayerModule fp = (FakePlayerModule) module;
    if (fp.getFakePlayer() == null || !fp.isDamageEnabled()) {
      return;
    }
    Minecraft mc = MinecraftAccessor.getMinecraft();
    if (mc == null) {
      return;
    }
    target.damage(player, 2);
    player.attack(target);
    info.cancel();
  }
}
