package me.friendly.exeter.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Allows the account manager to replace the active session. Modern Minecraft keeps its {@code User}
 * in a final field, so a mutable accessor is required.
 */
@Mixin(Minecraft.class)
public interface MixinMinecraft {
  @Mutable
  @Accessor("user")
  void exeter$setUser(User user);
}
