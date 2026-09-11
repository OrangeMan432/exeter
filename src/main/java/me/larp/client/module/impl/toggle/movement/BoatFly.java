package me.larp.client.module.impl.toggle.movement;

import me.larp.api.event.Listener;
import me.larp.api.event.Stage;
import me.larp.client.events.TickEvent;
import me.larp.client.module.ModuleType;
import me.larp.client.module.ToggleableModule;
import me.larp.client.properties.NumberProperty;
import me.larp.client.properties.Property;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;

/** Direct control of boats: directional flight with vertical keys. */
public class BoatFly extends ToggleableModule {

  private final NumberProperty<Double> speed =
      new NumberProperty<Double>(1.0, 0.1, 5.0, "Speed");
  private final NumberProperty<Double> vertical =
      new NumberProperty<Double>(0.5, 0.05, 3.0, "Vertical");
  private final NumberProperty<Double> glide =
      new NumberProperty<Double>(0.0, 0.0, 1.0, "Glide");
  private final Property<Boolean> noFall =
      new Property<Boolean>(true, "No Fall");

  public BoatFly() {
    super("BoatFly", new String[] {"boatfly", "boat-fly", "entityfly"}, 0x00FF00,
        ModuleType.MOVEMENT);
    setDescription("Fly boats with full directional control.");
    offerProperties(speed, vertical, glide, noFall);
    this.listeners.add(
        new Listener<TickEvent>("boatfly_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            BoatFly.this.onTick();
          }
        });
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null) return;
    if (minecraft.player.isDeadOrDying()) return;
    Entity vehicle = minecraft.player.getVehicle();
    if (!(vehicle instanceof AbstractBoat boat)) return;

    double forward = minecraft.player.zza;
    double strafe = minecraft.player.xxa;
    double yawRad = Math.toRadians(minecraft.player.getYRot());

    double motionX = 0;
    double motionZ = 0;
    if (forward != 0 || strafe != 0) {
      double len = Math.sqrt(forward * forward + strafe * strafe);
      double f = forward / len;
      double s = strafe / len;
      double h = speed.getValue();
      motionX = (f * -Math.sin(yawRad) + s * Math.cos(yawRad)) * h;
      motionZ = (f * Math.cos(yawRad) + s * Math.sin(yawRad)) * h;
    }

    double motionY;
    if (minecraft.options.keyJump.isDown()) {
      motionY = vertical.getValue();
    } else if (minecraft.options.keyShift.isDown()) {
      motionY = -vertical.getValue();
    } else {
      motionY = -glide.getValue();
    }

    boat.setDeltaMovement(motionX, motionY, motionZ);
    boat.fallDistance = 0;
    if (noFall.getValue()) {
      minecraft.player.fallDistance = 0;
    }
    // Face travel direction like vanilla boat handling expects.
    if (forward != 0 || strafe != 0) {
      boat.setYRot(minecraft.player.getYRot());
    }
  }
}
