package me.friendly.exeter.module.impl.toggle.combat;

import me.friendly.api.event.Listener;
import me.friendly.exeter.events.PacketEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.EnumProperty;
import me.friendly.exeter.properties.Property;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

/**
 * Shoreline-pattern criticals: PACKET mode sends the 0.05/0/0.03/0 offset chain,
 * GRIM mode nudges position while airborne. Skips crystals like Shoreline does.
 */
public class Criticals extends ToggleableModule {

  public enum Mode {
    PACKET,
    GRIM
  }

  private final EnumProperty<Mode> mode = new EnumProperty<Mode>(Mode.PACKET, "Mode");
  private final Property<Boolean> onlyWeapon =
      new Property<Boolean>(true, "Only Weapon");

  private final Listener<PacketEvent> packetListener =
      new Listener<PacketEvent>("criticals_packet") {
        @Override
        public void call(PacketEvent event) {
          if (!(event.getPacket() instanceof ServerboundAttackPacket)) return;
          if (minecraft.player == null || minecraft.level == null) return;
          if (minecraft.player.isInWater() || minecraft.player.isInLava()) return;
          if (minecraft.player.isFallFlying()) return;
          if (minecraft.player.isPassenger()) return;
          if (minecraft.player.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS)) return;
          if (onlyWeapon.getValue() && !isWeaponHeld()) return;

          double x = minecraft.player.getX();
          double y = minecraft.player.getY();
          double z = minecraft.player.getZ();

          if (mode.getValue() == Mode.GRIM) {
            if (minecraft.player.onGround()) return;
            minecraft
                .getConnection()
                .send(new ServerboundMovePlayerPacket.Pos(x, y - 0.000001, z, false, false));
            return;
          }

          if (!minecraft.player.onGround()) return;
          // Shoreline PACKET chain.
          minecraft
              .getConnection()
              .send(new ServerboundMovePlayerPacket.Pos(x, y + 0.05, z, false, false));
          minecraft
              .getConnection()
              .send(new ServerboundMovePlayerPacket.Pos(x, y, z, false, false));
          minecraft
              .getConnection()
              .send(new ServerboundMovePlayerPacket.Pos(x, y + 0.03, z, false, false));
          minecraft
              .getConnection()
              .send(new ServerboundMovePlayerPacket.Pos(x, y, z, false, false));
        }
      };

  public Criticals() {
    super("Criticals", new String[] {"criticals", "crits"}, 0xFF0000, ModuleType.COMBAT);
    setDescription("Forces critical hits with Shoreline packet patterns.");
    offerProperties(mode, onlyWeapon);
    this.listeners.add(packetListener);
  }

  private boolean isWeaponHeld() {
    if (minecraft.player == null) return false;
    var held = minecraft.player.getMainHandItem();
    if (held.isEmpty()) return false;
    String path =
        net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(held.getItem()).getPath();
    return path.endsWith("_sword") || path.endsWith("_axe")
        || held.getItem() == net.minecraft.world.item.Items.MACE
        || held.getItem() == net.minecraft.world.item.Items.TRIDENT;
  }
}
