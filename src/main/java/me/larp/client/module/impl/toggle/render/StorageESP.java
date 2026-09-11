package me.larp.client.module.impl.toggle.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import me.larp.api.event.Listener;
import me.larp.api.event.Stage;
import me.larp.client.events.RenderWorldEvent;
import me.larp.client.events.TickEvent;
import me.larp.client.module.ModuleType;
import me.larp.client.module.ToggleableModule;
import me.larp.client.properties.NumberProperty;
import me.larp.client.properties.Property;
import me.larp.client.util.Render3D;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.phys.AABB;

/** Boxes storage blocks: chests, shulkers, barrels, ender chests. */
public class StorageESP extends ToggleableModule {

  private final NumberProperty<Double> range =
      new NumberProperty<Double>(32.0, 8.0, 128.0, "Range");
  private final NumberProperty<Double> lineWidth =
      new NumberProperty<Double>(2.0, 1.0, 5.0, "Line Width");
  private final Property<Boolean> chests = new Property<Boolean>(true, "Chests");
  private final Property<Boolean> shulkers = new Property<Boolean>(true, "Shulkers");
  private final Property<Boolean> enderChests = new Property<Boolean>(true, "Ender Chests");
  private final Property<Boolean> others = new Property<Boolean>(false, "Others");

  private record BoxEntry(BlockPos pos, int color) {}

  // Chunk scans every frame stall the render thread: refresh on tick, draw cached.
  private volatile List<BoxEntry> cached = new ArrayList<>();
  private int tickCounter;

  public StorageESP() {
    super("StorageESP", new String[] {"storageesp", "chestesp"}, 0xFF00FF, ModuleType.RENDER);
    setDescription("Highlights storage containers.");
    offerProperties(range, lineWidth, chests, shulkers, enderChests, others);
    this.listeners.add(
        new Listener<RenderWorldEvent>("storageesp_render") {
          @Override
          public void call(RenderWorldEvent event) {
            StorageESP.this.onRender(event);
          }
        });
    this.listeners.add(
        new Listener<TickEvent>("storageesp_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            if (minecraft.level == null || minecraft.player == null) return;
            if (tickCounter++ < 20) return;
            tickCounter = 0;
            cached = scan();
          }
        });
  }

  private void onRender(RenderWorldEvent event) {
    if (minecraft.level == null || minecraft.player == null) return;
    for (BoxEntry box : cached) {
      Render3D.drawBoxOutline(
          event.getSubmitNodeStorage(),
          event.getCamera(),
          event.getMatrixStack(),
          new AABB(box.pos()),
          box.color(),
          lineWidth.getValue().floatValue());
    }
  }

  private List<BoxEntry> scan() {
    List<BoxEntry> out = new ArrayList<>();
    BlockPos origin = minecraft.player.blockPosition();
    int chunkR = (int) Math.ceil(range.getValue() / 16.0) + 1;
    int baseCx = origin.getX() >> 4;
    int baseCz = origin.getZ() >> 4;
    for (int cx = baseCx - chunkR; cx <= baseCx + chunkR; cx++) {
      for (int cz = baseCz - chunkR; cz <= baseCz + chunkR; cz++) {
        var chunk = minecraft.level.getChunk(cx, cz);
        for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
          BlockPos pos = entry.getKey();
          if (!inRange(pos)) continue;
          int color = colorFor(entry.getValue());
          if (color == 0) continue;
          out.add(new BoxEntry(pos.immutable(), color));
        }
      }
    }
    return out;
  }

  private int colorFor(BlockEntity be) {
    if (be instanceof ChestBlockEntity) return chests.getValue() ? 0xFFFFAA00 : 0;
    if (be instanceof ShulkerBoxBlockEntity) return shulkers.getValue() ? 0xFFFF55FF : 0;
    if (be instanceof EnderChestBlockEntity) return enderChests.getValue() ? 0xFFAA00FF : 0;
    if (be instanceof BarrelBlockEntity) return others.getValue() ? 0xFF885522 : 0;
    if (be instanceof BaseContainerBlockEntity) return others.getValue() ? 0xFF888888 : 0;
    return 0;
  }

  private boolean inRange(BlockPos pos) {
    double dx = pos.getX() + 0.5 - minecraft.player.getX();
    double dy = pos.getY() + 0.5 - minecraft.player.getY();
    double dz = pos.getZ() + 0.5 - minecraft.player.getZ();
    return dx * dx + dy * dy + dz * dz <= range.getValue() * range.getValue();
  }
}
