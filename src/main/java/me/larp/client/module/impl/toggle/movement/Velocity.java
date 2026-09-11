package me.larp.client.module.impl.toggle.movement;

import me.larp.api.event.Listener;
import me.larp.client.events.PacketEvent;
import me.larp.client.module.ModuleType;
import me.larp.client.module.ToggleableModule;
import me.larp.client.properties.NumberProperty;
import me.larp.client.properties.Property;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.phys.Vec3;

public class Velocity extends ToggleableModule {

  private final NumberProperty<Double> horizontal =
      new NumberProperty<Double>(0.0, 0.0, 100.0, "Horizontal");
  private final NumberProperty<Double> vertical =
      new NumberProperty<Double>(0.0, 0.0, 100.0, "Vertical");
  private final Property<Boolean> explosions =
      new Property<Boolean>(true, "Explosions");
  private final Property<Boolean> jumpReset =
      new Property<Boolean>(false, "Jump Reset");

  private final Listener<PacketEvent> packetListener =
      new Listener<PacketEvent>("velocity_packet") {
        @Override
        public void call(PacketEvent event) {
          if (minecraft.player == null || minecraft.level == null) return;

          if (event.getPacket() instanceof ClientboundSetEntityMotionPacket packet) {
            if (packet.id() != minecraft.player.getId()) return;
            double h = horizontal.getValue() / 100.0;
            double v = vertical.getValue() / 100.0;
            if (h <= 0.0 && v <= 0.0) {
              event.setCanceled(true);
              return;
            }
            if (h >= 1.0 && v >= 1.0) return;
            // Partial take: cancel the server velocity, apply our scaled share
            // on the client thread (inbound packets arrive on netty).
            event.setCanceled(true);
            Vec3 motion = packet.movement();
            boolean jump = jumpReset.getValue() && minecraft.player.onGround();
            minecraft.execute(
                () -> {
                  if (minecraft.player == null) return;
                  double x = motion.x * h;
                  double y = motion.y * v;
                  double z = motion.z * h;
                  minecraft.player.setDeltaMovement(
                      minecraft.player.getDeltaMovement().add(x, y, z));
                  if (jump) {
                    minecraft.player.jumpFromGround();
                  }
                });
          }

          if (event.getPacket() instanceof ClientboundExplodePacket && explosions.getValue()) {
            event.setCanceled(true);
          }
        }
      };

  public Velocity() {
    super("Velocity", new String[] {"velocity", "velocity-cancel"}, 0xFF0000, ModuleType.MOVEMENT);
    setDescription("Scales knockback from attacks and explosions.");
    offerProperties(horizontal, vertical, explosions, jumpReset);
    this.listeners.add(packetListener);
  }
}
