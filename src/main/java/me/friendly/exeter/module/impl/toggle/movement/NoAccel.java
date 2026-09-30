package me.friendly.exeter.module.impl.toggle.movement;

import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import net.minecraft.block.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import org.lwjgl.input.Keyboard;

public class NoAccel extends ToggleableModule {

  // Beta walk speed is 4.317 m/s with no sprint; cap at walk pace.
  private final NumberProperty<Double> speed =
      new NumberProperty<Double>(0.2158, 0.1, 0.22, "Speed", "speed");

  private double lastX;
  private double lastZ;
  private boolean haveLast;
  private boolean active;

  private final Listener<TickEvent> tickListener =
      new Listener<TickEvent>("noaccel_tick") {
        @Override
        public void call(TickEvent event) {
          if (event.getStage() == Stage.PRE) {
            NoAccel.this.onPre();
          } else if (event.getStage() == Stage.POST) {
            NoAccel.this.onPost();
          }
        }
      };

  public NoAccel() {
    super("NoAccel", new String[] {"noaccel"}, 0x00FF00, ModuleType.MOVEMENT);
    setDescription("Removes movement acceleration.");
    offerProperties(speed);
    this.listeners.add(tickListener);
  }

  @Override
  protected void onDisable() {
    super.onDisable();
    haveLast = false;
    active = false;
  }

  private double[] wishDir(Minecraft mc) {
    double forward = 0.0;
    double strafe = 0.0;
    if (Keyboard.isKeyDown(mc.options.forwardKey.code)) forward += 1.0;
    if (Keyboard.isKeyDown(mc.options.backKey.code)) forward -= 1.0;
    if (Keyboard.isKeyDown(mc.options.leftKey.code)) strafe += 1.0;
    if (Keyboard.isKeyDown(mc.options.rightKey.code)) strafe -= 1.0;
    if (forward == 0.0 && strafe == 0.0) {
      return null;
    }
    if (forward != 0.0 && strafe != 0.0) {
      forward *= Math.sin(Math.PI / 4);
      strafe *= Math.cos(Math.PI / 4);
    }
    double rad = Math.toRadians(mc.player.yaw);
    double sin = Math.sin(rad);
    double cos = Math.cos(rad);
    return new double[] {
      forward * -sin + strafe * cos, forward * cos + strafe * sin
    };
  }

  private boolean usable(Minecraft mc) {
    if (mc == null || mc.player == null || mc.options == null) {
      return false;
    }
    PlayerEntity player = mc.player;
    if (!player.field_1623) {
      return false;
    }
    if (player.isInFluid(Material.WATER) || player.isInFluid(Material.LAVA)) {
      return false;
    }
    return true;
  }

  private void onPre() {
    Minecraft mc = minecraft();
    if (!usable(mc)) {
      active = false;
      return;
    }
    double[] wish = wishDir(mc);
    if (wish == null) {
      active = false;
      return;
    }
    active = true;
    // Instant full speed: beta still adds its own acceleration on top during
    // its tick, so the POST pass clamps the resulting displacement.
    double target = speed.getValue().doubleValue();
    mc.player.velocityX = wish[0] * target;
    mc.player.velocityZ = wish[1] * target;
  }

  private void onPost() {
    Minecraft mc = minecraft();
    if (!usable(mc) || !active) {
      haveLast = false;
      return;
    }
    PlayerEntity player = mc.player;
    if (!haveLast) {
      lastX = player.x;
      lastZ = player.z;
      haveLast = true;
      return;
    }
    double dx = player.x - lastX;
    double dz = player.z - lastZ;
    double dist = Math.sqrt(dx * dx + dz * dz);
    double target = speed.getValue().doubleValue();
    if (dist > target && dist > 0.000001) {
      double scale = target / dist;
      player.x = lastX + dx * scale;
      player.z = lastZ + dz * scale;
    }
    lastX = player.x;
    lastZ = player.z;
  }
}
