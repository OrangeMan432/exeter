package me.friendly.exeter.beta.mixin;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes protected fallDistance (field_1636, verified via NBT FallDistance tag) for NoFall. */
@Mixin(Entity.class)
public interface EntityAccessor {
  @Accessor("field_1636")
  float getFallDistance();

  @Accessor("field_1636")
  void setFallDistance(float distance);
}
