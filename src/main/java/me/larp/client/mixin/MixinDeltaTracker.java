package me.larp.client.mixin;

import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.client.DeltaTracker$Timer")
public interface MixinDeltaTracker {

  @Mutable
  @Accessor("msPerTick")
  void setMsPerTick(float msPerTick);

  @Accessor("msPerTick")
  float getMsPerTick();

  @Accessor("deltaTicks")
  float getDeltaTicks();

  static MixinDeltaTracker of(DeltaTracker timer) {
    return (MixinDeltaTracker) timer;
  }
}
