package me.friendly.exeter.module.impl.toggle.world.fakeplayer;

import me.friendly.api.event.Listener;
import me.friendly.exeter.events.PacketEvent;
import me.friendly.exeter.logging.DebugLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.world.phys.Vec3;

public class ListenerExplosion extends Listener<PacketEvent> {
  private static final Minecraft mc = Minecraft.getInstance();
  private final FakePlayerModule module;

  public ListenerExplosion(FakePlayerModule module) {
    super("fakeplayer_explosion");
    this.module = module;
  }

  @Override
  public void call(PacketEvent event) {
    if (!module.isDamageEnabled()) return;
    if (mc.level == null || module.getFakePlayer() == null) return;
    if (!module.isRunning()) return;
    if (!(event.getPacket() instanceof ClientboundExplodePacket packet)) return;

    mc.execute(
        () -> {
          if (mc.level == null || module.getFakePlayer() == null || !module.isRunning()) return;

          Vec3 center = packet.center();
          float strength = packet.radius();

          // Vanilla-faithful: sampled line-of-sight exposure, so cover blocks.
          float damage =
              me.friendly.exeter.util.ExplosionUtil.explosionDamage(
                  mc.level, center, strength, module.getFakePlayer());
          var dummy = module.getFakePlayer();
          DebugLogger.get()
              .log(
                  "FakePlayer",
                  DebugLogger.Level.INFO,
                  String.format(
                      "boom center=(%.2f,%.2f,%.2f) radius=%.1f dummy=(%.2f,%.2f,%.2f) hp=%.1f"
                          + " dmg=%.1f",
                      center.x,
                      center.y,
                      center.z,
                      strength,
                      dummy.getX(),
                      dummy.getY(),
                      dummy.getZ(),
                      dummy.getHealth(),
                      damage));
          if (damage > 0) {
            module.getFakePlayer().applyDamage(damage);
          }
        });
  }
}
