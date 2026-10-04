package me.friendly.exeter.module.impl.toggle.world.fakeplayer;

import me.friendly.api.event.Listener;
import me.friendly.exeter.events.PacketEvent;
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
          double x = center.x();
          double y = center.y();
          double z = center.z();
          float strength = packet.radius();

          double dx = module.getFakePlayer().getX() - x;
          double dy = module.getFakePlayer().getY() - y;
          double dz = module.getFakePlayer().getZ() - z;
          double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

          if (distance > 12.0) return;

          double distFactor = 1.0 - distance / 12.0;
          if (distFactor <= 0) return;

          double exposure = (distFactor * distFactor + distFactor) / 2.0;
          float damage =
              (float) ((exposure * exposure + exposure) / 2.0 * 7.0 * strength * 2.0 + 1.0);

          module.getFakePlayer().applyDamage(damage);
        });
  }
}
