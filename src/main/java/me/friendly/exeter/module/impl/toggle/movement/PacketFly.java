package me.friendly.exeter.module.impl.toggle.movement;

import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.events.PacketEvent;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;

/**
 * BlackOut-addon-pattern PacketFly: cancels vanilla movement, sends its own
 * position + bounds-offset packets, confirms matching server teleports.
 * Self-sent packets ride an allowlist so they are never re-canceled.
 */
public class PacketFly extends ToggleableModule {

  private final NumberProperty<Double> speed =
      new NumberProperty<Double>(0.5, 0.05, 5.0, "Speed");
  private final NumberProperty<Double> packets =
      new NumberProperty<Double>(2.0, 1.0, 10.0, "Packets");
  private final NumberProperty<Double> upSpeed =
      new NumberProperty<Double>(0.5, 0.05, 5.0, "Up Speed");
  private final NumberProperty<Double> downSpeed =
      new NumberProperty<Double>(0.5, 0.05, 5.0, "Down Speed");
  private final NumberProperty<Double> xzBound =
      new NumberProperty<Double>(10.0, 0.0, 100.0, "XZ Bound");
  private final NumberProperty<Double> yBound =
      new NumberProperty<Double>(5.0, 0.0, 100.0, "Y Bound");
  private final Property<Boolean> onGroundSpoof =
      new Property<Boolean>(true, "Ground Spoof");
  private final Property<Boolean> strictVertical =
      new Property<Boolean>(false, "Strict Vertical");
  private final Property<Boolean> antiKick =
      new Property<Boolean>(true, "Anti Kick");
  private final NumberProperty<Double> kickAmount =
      new NumberProperty<Double>(0.04, 0.0, 0.2, "Kick Amount");
  private final NumberProperty<Integer> kickDelay =
      new NumberProperty<Integer>(20, 5, 100, "Kick Delay");
  private final Property<Boolean> phase =
      new Property<Boolean>(true, "Phase");

  private final Set<Packet<?>> own = new HashSet<>();
  private final Map<Integer, Vec3> expected = new ConcurrentHashMap<>();
  private final Random random = new Random();
  private double packetsToSend;
  private int ticks;
  private int teleportId = -1;
  private boolean moving;

  public PacketFly() {
    super("PacketFly", new String[] {"packetfly", "packet-fly"}, 0x00FF00, ModuleType.MOVEMENT);
    setDescription("Packet-based flight with bounds spoofing.");
    offerProperties(
        speed, packets, upSpeed, downSpeed, xzBound, yBound, onGroundSpoof, strictVertical,
        antiKick, kickAmount, kickDelay, phase);
    this.listeners.add(
        new Listener<TickEvent>("packetfly_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            PacketFly.this.onTick();
          }
        });
    this.listeners.add(
        new Listener<PacketEvent>("packetfly_packet") {
          @Override
          public void call(PacketEvent event) {
            PacketFly.this.onPacket(event);
          }
        });
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    own.clear();
    expected.clear();
    packetsToSend = 0;
    ticks = 0;
    teleportId = -1;
  }

  @Override
  protected void onDisable() {
    super.onDisable();
    own.clear();
    expected.clear();
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null) return;
    if (minecraft.player.isDeadOrDying()) return;
    ticks++;
    // Stale teleport expectations never resolve: bound the map.
    if (expected.size() > 64) {
      expected.clear();
    }

    boolean phasing = phase.getValue() && insideBlock();
    if (insideBlock() && !phase.getValue()) {
      // No noClip field exists in 26.2: hold still instead of fighting collision.
      minecraft.player.setDeltaMovement(0, 0, 0);
      return;
    }

    packetsToSend += packets.getValue();
    boolean kick = antiKick.getValue() && ticks % kickDelay.getValue() == 0 && !phasing;

    double yaw = movementYaw();
    double motion = speed.getValue();
    double x = 0;
    double y = 0;
    double z = 0;
    if (minecraft.options.keyJump.isDown()) {
      y = upSpeed.getValue();
    } else if (minecraft.options.keyShift.isDown()) {
      y = -downSpeed.getValue();
    }
    if (moving) {
      x = Math.cos(Math.toRadians(yaw + 90)) * motion;
      z = Math.sin(Math.toRadians(yaw + 90)) * motion;
    } else {
      packetsToSend = Math.min(packetsToSend, 1);
    }

    Vec3 offset = Vec3.ZERO;
    boolean kickSent = false;
    for (; packetsToSend >= 1; packetsToSend -= 1) {
      double yOff = y;
      if (kick && y >= 0 && !kickSent) {
        yOff = -kickAmount.getValue();
        kickSent = true;
      }
      boolean verticalOnly = strictVertical.getValue() && yOff != 0;
      offset = offset.add(verticalOnly ? 0 : x, yOff, verticalOnly ? 0 : z);
      sendMoved(offset);
      if (x == 0 && z == 0 && y == 0) break;
    }
    minecraft.player.setDeltaMovement(offset.x, offset.y, offset.z);
    packetsToSend = Math.min(packetsToSend, 1);
    setTag("PacketFly [" + (teleportId < 0 ? "?" : teleportId) + "]");
  }

  private void sendMoved(Vec3 offset) {
    Vec3 base = minecraft.player.position().add(offset);
    boolean ground = onGroundSpoof.getValue() ? minecraft.player.onGround() : false;
    send(new ServerboundMovePlayerPacket.Pos(base.x, base.y, base.z, ground, false));
    // Bounds packet: random far offset the anticheat must reconcile.
    double yaw = random.nextDouble() * 360.0;
    Vec3 bound =
        new Vec3(
            base.x + Math.cos(Math.toRadians(yaw)) * xzBound.getValue(),
            base.y + yBound.getValue(),
            base.z + Math.sin(Math.toRadians(yaw)) * xzBound.getValue());
    send(new ServerboundMovePlayerPacket.Pos(bound.x, bound.y, bound.z, ground, false));
    expected.put(teleportId + 1, base);
  }

  private void send(Packet<?> packet) {
    own.add(packet);
    minecraft.getConnection().send(packet);
  }

  private void onPacket(PacketEvent event) {
    if (event.getPacket() instanceof ServerboundMovePlayerPacket packet) {
      if (own.remove(packet)) return;
      event.setCanceled(true);
      return;
    }
    if (event.getPacket() instanceof ClientboundPlayerPositionPacket packet) {
      Vec3 pos = packet.change().position();
      Vec3 want = expected.remove(packet.id());
      if (want != null && want.distanceToSqr(pos) < 0.01) {
        // Server echoed our position: accept and swallow the rubberband.
        event.setCanceled(true);
        send(new ServerboundAcceptTeleportationPacket(packet.id()));
        return;
      }
      teleportId = packet.id();
    }
  }

  private double movementYaw() {
    double f = minecraft.player.zza;
    double s = minecraft.player.xxa;
    double yaw = minecraft.player.getYRot();
    if (f > 0) {
      moving = true;
      yaw += s > 0 ? -45 : s < 0 ? 45 : 0;
    } else if (f < 0) {
      moving = true;
      yaw += s > 0 ? -135 : s < 0 ? 135 : 180;
    } else {
      moving = s != 0;
      yaw += s > 0 ? -90 : s < 0 ? 90 : 0;
    }
    return yaw;
  }

  private boolean insideBlock() {
    return !minecraft.level.noCollision(
        minecraft.player,
        minecraft.player.getBoundingBox().deflate(0.0625, 0, 0.0625));
  }
}
