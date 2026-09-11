package me.larp.client.module.impl.toggle.misc;

import me.larp.api.event.Listener;
import me.larp.api.event.Stage;
import me.larp.client.events.TickEvent;
import me.larp.client.module.ModuleType;
import me.larp.client.module.ToggleableModule;
import me.larp.client.properties.NumberProperty;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * TrollHack-pattern HitboxDesync: one-shot client-side nudge into the block
 * corner you face, desyncing hitbox from server position for phasing setups.
 */
public class HitboxDesync extends ToggleableModule {

  private final NumberProperty<Double> offset =
      new NumberProperty<Double>(0.2, 0.05, 0.5, "Offset");

  public HitboxDesync() {
    super("HitboxDesync", new String[] {"hitboxdesync", "desync"}, 0x00FFFF,
        ModuleType.MISCELLANEOUS);
    setDescription("Nudges your client hitbox for phasing.");
    offerProperties(offset);
    this.listeners.add(
        new Listener<TickEvent>("hitboxdesync_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            HitboxDesync.this.onTick();
          }
        });
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null) return;
    Direction facing = minecraft.player.getDirection();
    Vec3 center = minecraft.player.getBoundingBox().getCenter();
    Vec3 unit = Vec3.atLowerCornerOf(new BlockPos((int) center.x, (int) center.y, (int) center.z));
    double ox = facing.getStepX() * offset.getValue();
    double oz = facing.getStepZ() * offset.getValue();
    double nx = merge(unit.x + 0.5 + ox, facing.getStepX());
    double nz = merge(unit.z + 0.5 + oz, facing.getStepZ());
    minecraft.player.setPos(
        nx == 0.0 ? minecraft.player.getX() : nx,
        minecraft.player.getY(),
        nz == 0.0 ? minecraft.player.getZ() : nz);
    setRunning(false);
  }

  private double merge(double a, int facingComponent) {
    return a * Math.abs(facingComponent);
  }
}
