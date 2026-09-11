package me.larp.client.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net.minecraft.client.DeltaTracker$Timer")
public abstract class MixinDeltaTracker {

  @Mutable @Final @Shadow private float msPerTick;

  public void setMsPerTick(float msPerTick) {
    this.msPerTick = msPerTick;
  }

  public float getMsPerTick() {
    return this.msPerTick;
  }
}
