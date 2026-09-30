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

  private final NumberProperty<Double> speed =
      new NumberProperty<Double>(0.22, 0.1, 0.5, "Speed", "speed");

  private final Listener<TickEvent> tickListener =
      new Listener<TickEvent>("noaccel_tick") {
        @Override
        public void call(TickEvent event) {
          if (event.getStage() != Stage.PRE) return;
          NoAccel.this.onTick();
        }
      };

  public NoAccel() {
    super("NoAccel", new String[] {"noaccel"}, 0x00FF00, ModuleType.MOVEMENT);
    setDescription("Removes movement acceleration.");
    offerProperties(speed);
    this.listeners.add(tickListener);
  }

  private void onTick() {
    Minecraft mc = minecraft();
    if (mc == null || mc.player == null || mc.options == null) {
      return;
    }
    PlayerEntity player = mc.player;
    if (!player.field_1623) {
      // onGround only
      return;
    }
    if (player.isInFluid(Material.WATER) || player.isInFluid(Material.LAVA)) {
      return;
    }
    double forward = 0.0;
    double strafe = 0.0;
    if (Keyboard.isKeyDown(mc.options.forwardKey.code)) forward += 1.0;
    if (Keyboard.isKeyDown(mc.options.backKey.code)) forward -= 1.0;
    if (Keyboard.isKeyDown(mc.options.leftKey.code)) strafe += 1.0;
    if (Keyboard.isKeyDown(mc.options.rightKey.code)) strafe -= 1.0;
    if (forward == 0.0 && strafe == 0.0) {
      return;
    }
    if (forward != 0.0 && strafe != 0.0) {
      forward *= Math.sin(Math.PI / 4);
      strafe *= Math.cos(Math.PI / 4);
    }
    double rad = Math.toRadians(player.yaw);
    double sin = Math.sin(rad);
    double cos = Math.cos(rad);
    double target = speed.getValue().doubleValue();
    player.velocityX = forward * target * -sin + strafe * target * cos;
    player.velocityZ = forward * target * cos + strafe * target * sin;
  }
}
