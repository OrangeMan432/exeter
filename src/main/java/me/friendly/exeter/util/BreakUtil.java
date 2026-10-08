package me.friendly.exeter.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Vanilla block-breaking time estimate for a player's best hotbar tool. */
public final class BreakUtil {
  private BreakUtil() {}

  /** Milliseconds to break with the held item, or -1 if unbreakable. */
  public static long estimateBreakMs(Level level, Player player, BlockPos pos) {
    if (level == null || player == null) return -1;
    BlockState state = level.getBlockState(pos);
    float hardness = state.getDestroySpeed(level, pos);
    if (hardness < 0.0f) return -1;
    // Held item, not best hotbar tool: the estimate must match what's actually mining.
    float speed = digSpeed(player, state, player.getMainHandItem());
    if (speed <= 1.0f) return (long) Math.ceil(1.0f / (1.0f / hardness / 30.0f)) * 50L;
    float relative = speed / hardness / 30.0f;
    return (long) Math.ceil(1.0f / relative) * 50L;
  }

  private static float digSpeed(Player player, BlockState state, ItemStack stack) {
    float str = stack.getDestroySpeed(state);
    int eff = efficiencyLevel(stack);
    float speed = (float) Math.max(str + (str > 1.0f ? eff * eff + 1.0f : 0.0f), 0.0f);
    MobEffectInstance haste = player.getEffect(MobEffects.HASTE);
    if (haste != null) {
      speed *= 1.0f + (haste.getAmplifier() + 1) * 0.2f;
    }
    MobEffectInstance fatigue = player.getEffect(MobEffects.MINING_FATIGUE);
    if (fatigue != null) {
      float scale;
      switch (fatigue.getAmplifier()) {
        case 0 -> scale = 0.3f;
        case 1 -> scale = 0.09f;
        case 2 -> scale = 0.0027f;
        default -> scale = 8.1e-4f;
      }
      speed *= scale;
    }
    if (player.isInWater() && !player.onGround()) {
      speed /= 5.0f;
    }
    return speed;
  }

  private static int efficiencyLevel(ItemStack stack) {
    ItemEnchantments enchantments = stack.getEnchantments();
    for (var entry : enchantments.entrySet()) {
      if (entry.getKey().is(Enchantments.EFFICIENCY)) return entry.getIntValue();
    }
    return 0;
  }
}
