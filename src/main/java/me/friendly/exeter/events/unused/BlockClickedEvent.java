package me.friendly.exeter.events.unused;

import me.friendly.api.event.Event;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

public class BlockClickedEvent
extends Event {
    private BlockPos blockPos;
    private Direction enumFacing;

    public BlockClickedEvent(BlockPos blockPos, Direction enumFacing) {
        this.blockPos = blockPos;
        this.enumFacing = enumFacing;
    }

    public BlockPos getBlockPos() {
        return this.blockPos;
    }

    public void setBlockPos(BlockPos blockPos) {
        this.blockPos = blockPos;
    }

    public Direction getEnumFacing() {
        return this.enumFacing;
    }

    public void setEnumFacing(Direction enumFacing) {
        this.enumFacing = enumFacing;
    }
}

