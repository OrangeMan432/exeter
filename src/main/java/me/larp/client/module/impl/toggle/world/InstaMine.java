package me.larp.client.module.impl.toggle.world;

import me.larp.api.event.Listener;
import me.larp.api.event.Stage;
import me.larp.client.events.TickEvent;
import me.larp.client.module.ModuleType;
import me.larp.client.module.ToggleableModule;
import me.larp.client.properties.Property;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Packet mine: fires STOP right after START so soft blocks break instantly. */
public class InstaMine extends ToggleableModule {

  private final Property<Boolean> rotate = new Property<Boolean>(false, "Rotate");

  public InstaMine() {
    super("InstaMine", new String[] {"instamine", "insta-mine"}, 0x0000FF, ModuleType.WORLD);
    setDescription("Breaks soft blocks instantly with packets.");
    offerProperties(rotate);
    this.listeners.add(
        new Listener<TickEvent>("instamine_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            InstaMine.this.onTick();
          }
        });
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null || minecraft.getConnection() == null) {
      return;
    }
    if (minecraft.player.isDeadOrDying()) return;
    if (!minecraft.gameMode.isDestroying()) return;
    if (!(minecraft.hitResult instanceof BlockHitResult hit)) return;

    BlockPos pos = hit.getBlockPos();
    BlockState state = minecraft.level.getBlockState(pos);
    if (state.isAir()) return;
    // Never waste packets on unbreakables.
    if (state.getBlock() == Blocks.BEDROCK
        || state.getBlock() == Blocks.OBSIDIAN
        || state.getBlock() == Blocks.REINFORCED_DEEPSLATE
        || state.getBlock() == Blocks.END_PORTAL_FRAME) {
      return;
    }
    if (!minecraft.player.blockPosition().closerThan(pos, 5.0)) return;

    minecraft
        .getConnection()
        .send(
            new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, hit.getDirection()));
  }
}
