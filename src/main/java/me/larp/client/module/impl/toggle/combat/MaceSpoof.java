package me.larp.client.module.impl.toggle.combat;

import java.util.HashSet;
import java.util.Set;
import me.larp.api.event.Listener;
import me.larp.client.events.PacketEvent;
import me.larp.client.module.ModuleType;
import me.larp.client.module.ToggleableModule;
import me.larp.client.properties.EnumProperty;
import me.larp.client.properties.NumberProperty;
import me.larp.client.properties.Property;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * TrollHack-pattern MaceSpoof: on mace swings, spoofs a high Y so the server
 * computes smash-attack fall damage. Spoof Ground mode forces airborne flags.
 */
public class MaceSpoof extends ToggleableModule {

  public enum Mode {
    VANILLA,
    SPOOF_GROUND
  }

  private final EnumProperty<Mode> mode =
      new EnumProperty<Mode>(Mode.VANILLA, "Mode");
  private final NumberProperty<Integer> fallDistance =
      new NumberProperty<Integer>(10, 0, 340, "Fall Distance");

  private final Set<Packet<?>> own = new HashSet<>();

  public MaceSpoof() {
    super("MaceSpoof", new String[] {"macespoof", "mace-spoof"}, 0xFF0000, ModuleType.COMBAT);
    setDescription("Spoofs fall height for mace smash attacks.");
    offerProperties(mode, fallDistance);
    this.listeners.add(
        new Listener<PacketEvent>("macespoof_packet") {
          @Override
          public void call(PacketEvent event) {
            MaceSpoof.this.onPacket(event);
          }
        });
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    own.clear();
  }

  private void onPacket(PacketEvent event) {
    if (minecraft.player == null || minecraft.level == null
        || minecraft.getConnection() == null) return;
    if (own.remove(event.getPacket())) return;

    if (event.getPacket() instanceof ServerboundAttackPacket
        && mode.getValue() == Mode.VANILLA) {
      if (minecraft.player.getMainHandItem().getItem() != Items.MACE) return;
      int height = maxHeightAbove();
      if (height <= 0) return;
      int packets = Math.max(1, Math.min(20, (int) Math.ceil(height / 10.0)));
      double x = minecraft.player.getX();
      double y = minecraft.player.getY();
      double z = minecraft.player.getZ();
      boolean collision = minecraft.player.horizontalCollision;
      for (int i = 0; i < packets - 1; i++) {
        send(new ServerboundMovePlayerPacket.StatusOnly(false, collision));
      }
      send(new ServerboundMovePlayerPacket.Pos(x, y + height, z, false, collision));
      // Snap back twice so the server keeps our real spot.
      send(new ServerboundMovePlayerPacket.Pos(x, y, z, false, collision));
      send(new ServerboundMovePlayerPacket.Pos(x, y, z, false, collision));
      setTag("MaceSpoof [" + height + "]");
      return;
    }

    if (event.getPacket() instanceof ServerboundMovePlayerPacket
        && mode.getValue() == Mode.SPOOF_GROUND) {
      // No onGround accessor in 26.2: reissue the packet at our real position
      // with the flag forced false, preserving movement.
      event.setCanceled(true);
      send(new ServerboundMovePlayerPacket.Pos(
          minecraft.player.getX(), minecraft.player.getY(), minecraft.player.getZ(),
          false, minecraft.player.horizontalCollision));
    }
  }

  private void send(Packet<?> packet) {
    own.add(packet);
    minecraft.getConnection().send(packet);
  }

  private int maxHeightAbove() {
    BlockPos base = minecraft.player.blockPosition();
    int max = Math.min(base.getY() + fallDistance.getValue(), minecraft.level.getMaxY() - 2);
    for (int y = max; y > base.getY(); y--) {
      BlockPos a = new BlockPos(base.getX(), y, base.getZ());
      if (isSafe(a) && isSafe(a.above())) {
        return y - base.getY();
      }
    }
    return 0;
  }

  private boolean isSafe(BlockPos pos) {
    var state = minecraft.level.getBlockState(pos);
    return state.canBeReplaced()
        && minecraft.level.getFluidState(pos).isEmpty()
        && state.getBlock() != Blocks.POWDER_SNOW;
  }
}
