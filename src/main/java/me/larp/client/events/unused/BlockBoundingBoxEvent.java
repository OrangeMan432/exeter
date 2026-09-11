package me.larp.client.events.unused;

import me.larp.api.event.Event;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class BlockBoundingBoxEvent extends Event {
  private BlockState state;
  private Block block;
  private AABB boundingBox;
  private BlockPos blockPos;

  public BlockBoundingBoxEvent(Block block, AABB boundingBox, BlockPos blockPos, BlockState state) {
    this.block = block;
    this.boundingBox = boundingBox;
    this.blockPos = blockPos;
    this.state = state;
  }

  public BlockBoundingBoxEvent(AABB var7, Block block, int x, int y, int z) {}

  public BlockPos getBlockPos() {
    return this.blockPos;
  }

  public void setBlockPos(BlockPos blockPos) {
    this.blockPos = blockPos;
  }

  public Block getBlock() {
    return this.block;
  }

  public void setBlock(Block block) {
    this.block = block;
  }

  public AABB getBoundingBox() {
    return this.boundingBox;
  }

  public void setBoundingBox(AABB boundingBox) {
    this.boundingBox = boundingBox;
  }

  public BlockState getState() {
    return this.state;
  }
}
