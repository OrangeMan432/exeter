package me.larp.client.module.impl.toggle.movement;

import me.larp.api.event.Listener;
import me.larp.api.event.Stage;
import me.larp.client.events.TickEvent;
import me.larp.client.module.ModuleType;
import me.larp.client.module.ToggleableModule;
import me.larp.client.properties.NumberProperty;
import me.larp.client.properties.Property;
import me.larp.client.util.PlayerUtil;

/** Catches you over the void: hovers and drifts back toward ground. */
public class AntiVoid extends ToggleableModule {

  private final NumberProperty<Double> fallDistance =
      new NumberProperty<Double>(8.0, 2.0, 32.0, "Fall Distance");
  private final NumberProperty<Double> drift =
      new NumberProperty<Double>(0.3, 0.05, 1.0, "Drift");
  private final Property<Boolean> disableOnGround =
      new Property<Boolean>(true, "Disable On Ground");

  private double lastGroundX;
  private double lastGroundZ;
  private boolean hasGround;
  private boolean caught;

  public AntiVoid() {
    super("AntiVoid", new String[] {"antivoid", "anti-void"}, 0x00FF00, ModuleType.MOVEMENT);
    setDescription("Saves you from falling into the void.");
    offerProperties(fallDistance, drift, disableOnGround);
    this.listeners.add(
        new Listener<TickEvent>("antivoid_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            AntiVoid.this.onTick();
          }
        });
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    hasGround = false;
    caught = false;
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null) return;
    if (minecraft.player.isDeadOrDying()) return;
    if (minecraft.player.isFallFlying()) return;

    if (minecraft.player.onGround()) {
      lastGroundX = minecraft.player.getX();
      lastGroundZ = minecraft.player.getZ();
      hasGround = true;
      if (caught && disableOnGround.getValue()) {
        setRunning(false);
      }
      return;
    }

    if (minecraft.player.fallDistance < fallDistance.getValue()) return;
    // Only act over real void: ground must exist nowhere below.
    if (hasFloorBelow()) return;

    // Hover and drift back to last grounded spot.
    double mx = 0;
    double mz = 0;
    if (hasGround) {
      double dx = lastGroundX - minecraft.player.getX();
      double dz = lastGroundZ - minecraft.player.getZ();
      double len = Math.sqrt(dx * dx + dz * dz);
      if (len > 0.5) {
        mx = (dx / len) * drift.getValue();
        mz = (dz / len) * drift.getValue();
      }
    }
    minecraft.player.setDeltaMovement(mx, 0, mz);
    minecraft.player.fallDistance = 0;
    caught = true;
    setTag("AntiVoid [CATCH]");
  }

  private boolean hasFloorBelow() {
    net.minecraft.core.BlockPos pos = minecraft.player.blockPosition();
    for (int i = 1; i <= 12; i++) {
      if (PlayerUtil.isSolid(pos.below(i))) return true;
    }
    return false;
  }
}
