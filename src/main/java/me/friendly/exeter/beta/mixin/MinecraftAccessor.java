package me.friendly.exeter.beta.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Beta Minecraft keeps its instance private with no getter; expose it. */
@Mixin(Minecraft.class)
public interface MinecraftAccessor {
  @Accessor("INSTANCE")
  static Minecraft getMinecraft() {
    throw new AssertionError();
  }
}
