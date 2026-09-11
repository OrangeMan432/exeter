package me.larp.client.module.impl.toggle.movement;

import me.larp.api.event.Listener;
import me.larp.api.event.Stage;
import me.larp.client.events.TickEvent;
import me.larp.client.module.ModuleType;
import me.larp.client.module.ToggleableModule;
import me.larp.client.properties.NumberProperty;
import me.larp.client.properties.Property;
import me.larp.client.util.PlayerUtil;
import net.minecraft.core.BlockPos;

/** Anarchy staple: steers you into the nearest safe hole and centers you in it. */
public class HoleSnap extends ToggleableModule {

  private final NumberProperty<Double> range = new NumberProperty<Double>(5.0, 1.0, 10.0, "Range");
  private final NumberProperty<Double> snap = new NumberProperty<Double>(0.25, 0.05, 1.0, "Snap");
  private final Property<Boolean> autoDisable = new Property<Boolean>(true, "Auto Disable");

  public HoleSnap() {
    super("HoleSnap", new String[] {"holesnap", "hole-snap"}, 0x00FF00, ModuleType.MOVEMENT);
    setDescription("Pulls you into the nearest safe hole.");
    offerProperties(range, snap, autoDisable);
    this.listeners.add(
        new Listener<TickEvent>("holesnap_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            HoleSnap.this.onTick();
          }
        });
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null) return;
    if (minecraft.player.isDeadOrDying()) return;

    BlockPos hole = findHole();
    if (hole == null) return;

    double cx = hole.getX() + 0.5;
    double cz = hole.getZ() + 0.5;
    double dx = cx - minecraft.player.getX();
    double dz = cz - minecraft.player.getZ();
    double distSq = dx * dx + dz * dz;

    if (distSq < 0.01) {
      // Centered: kill drift so crystals can't push you out.
      minecraft.player.setDeltaMovement(0, minecraft.player.getDeltaMovement().y, 0);
      if (autoDisable.getValue()) {
        setRunning(false);
      }
      return;
    }

    double factor = Math.min(snap.getValue(), Math.sqrt(distSq));
    double len = Math.sqrt(distSq);
    minecraft.player.setDeltaMovement(
        (dx / len) * factor, minecraft.player.getDeltaMovement().y, (dz / len) * factor);
  }

  private BlockPos findHole() {
    BlockPos origin = minecraft.player.blockPosition();
    int r = (int) Math.ceil(range.getValue());
    BlockPos best = null;
    double bestDist = Double.MAX_VALUE;
    for (int x = -r; x <= r; x++) {
      for (int y = -2; y <= 1; y++) {
        for (int z = -r; z <= r; z++) {
          BlockPos pos = origin.offset(x, y, z);
          if (!isHole(pos)) continue;
          double d = pos.distSqr(origin);
          if (d < bestDist) {
            bestDist = d;
            best = pos;
          }
        }
      }
    }
    return best;
  }

  private boolean isHole(BlockPos pos) {
    if (!PlayerUtil.isAirOrReplaceable(pos)) return false;
    if (!PlayerUtil.isAirOrReplaceable(pos.above())) return false;
    if (!PlayerUtil.isSolid(pos.below())) return false;
    return PlayerUtil.isSolid(pos.north())
        && PlayerUtil.isSolid(pos.south())
        && PlayerUtil.isSolid(pos.east())
        && PlayerUtil.isSolid(pos.west());
  }
}
