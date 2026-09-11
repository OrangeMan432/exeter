package me.larp.client.module.impl.toggle.render;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import me.larp.api.event.Listener;
import me.larp.client.events.PacketEvent;
import me.larp.client.events.RenderWorldEvent;
import me.larp.client.module.ModuleType;
import me.larp.client.module.ToggleableModule;
import me.larp.client.properties.NumberProperty;
import me.larp.client.properties.Property;
import me.larp.client.util.Render3D;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Meteor-Rejects-pattern NewChunks: flowing fluid next to a source means
 * freshly generated terrain. Marks new chunks red, old ones green.
 */
public class NewChunks extends ToggleableModule {

  private final Property<Boolean> clearOnDisable =
      new Property<Boolean>(true, "Clear On Disable");
  private final NumberProperty<Double> lineWidth =
      new NumberProperty<Double>(2.0, 1.0, 5.0, "Line Width");
  private final NumberProperty<Integer> renderHeight =
      new NumberProperty<Integer>(0, -64, 319, "Render Height");

  private final Set<ChunkPos> newChunks = Collections.synchronizedSet(new HashSet<>());
  private final Set<ChunkPos> oldChunks = Collections.synchronizedSet(new HashSet<>());
  private static final Direction[] SEARCH = {
    Direction.EAST, Direction.NORTH, Direction.WEST, Direction.SOUTH, Direction.UP
  };

  public NewChunks() {
    super("NewChunks", new String[] {"newchunks", "new-chunks"}, 0xFF00FF, ModuleType.RENDER);
    setDescription("Highlights freshly generated chunks.");
    offerProperties(clearOnDisable, lineWidth, renderHeight);
    this.listeners.add(
        new Listener<PacketEvent>("newchunks_packet") {
          @Override
          public void call(PacketEvent event) {
            NewChunks.this.onPacket(event);
          }
        });
    this.listeners.add(
        new Listener<RenderWorldEvent>("newchunks_render") {
          @Override
          public void call(RenderWorldEvent event) {
            NewChunks.this.onRender(event);
          }
        });
  }

  @Override
  protected void onDisable() {
    super.onDisable();
    if (clearOnDisable.getValue()) {
      newChunks.clear();
      oldChunks.clear();
    }
  }

  private void onPacket(PacketEvent event) {
    if (minecraft.level == null) return;
    if (event.getPacket() instanceof ClientboundSectionBlocksUpdatePacket packet) {
      packet.runUpdates(
          (pos, state) -> {
            if (minecraft.level == null) return;
            if (!state.getFluidState().isEmpty() && !state.getFluidState().isSource()) {
              checkFlow(pos.immutable());
            }
          });
      return;
    }
    if (event.getPacket() instanceof ClientboundBlockUpdatePacket packet) {
      if (!packet.getBlockState().getFluidState().isEmpty()
          && !packet.getBlockState().getFluidState().isSource()) {
        checkFlow(packet.getPos());
      }
    }
  }

  private void checkFlow(BlockPos pos) {
    ChunkPos chunkPos = new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
    if (oldChunks.contains(chunkPos)) return;
    for (Direction dir : SEARCH) {
      if (minecraft.level.getBlockState(pos.relative(dir)).getFluidState().isSource()) {
        newChunks.add(chunkPos);
        return;
      }
    }
  }

  private void onRender(RenderWorldEvent event) {
    if (minecraft.player == null) return;
    BlockPos origin = minecraft.player.blockPosition();
    synchronized (newChunks) {
      for (ChunkPos chunk : newChunks) {
        if (!origin.closerThan(chunk.getWorldPosition(), 1024)) continue;
        drawChunk(event, chunk, 0xFFFF0000);
      }
    }
    synchronized (oldChunks) {
      for (ChunkPos chunk : oldChunks) {
        if (!origin.closerThan(chunk.getWorldPosition(), 1024)) continue;
        drawChunk(event, chunk, 0xFF00FF00);
      }
    }
  }

  private void drawChunk(RenderWorldEvent event, ChunkPos chunk, int color) {
    Vec3 min = Vec3.atLowerCornerOf(chunk.getWorldPosition());
    Vec3 max = Vec3.atLowerCornerOf(chunk.getWorldPosition().offset(16, renderHeight.getValue(), 16));
    Render3D.drawBoxOutline(
        event.getSubmitNodeStorage(),
        event.getCamera(),
        event.getMatrixStack(),
        new AABB(min, max),
        color,
        lineWidth.getValue().floatValue());
  }
}
