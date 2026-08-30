package me.friendly.api.minecraft.helper;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;

public final class WorldHelper {
  private static Minecraft minecraft = Minecraft.getInstance();

  public static BlockPos getSpawnPoint() {
    return WorldHelper.minecraft.level.getRespawnData().pos();
  }

  public static Block getBlock(double x, double y, double z) {
    return WorldHelper.minecraft.level.getBlockState(BlockPos.containing(x, y, z)).getBlock();
  }
}
