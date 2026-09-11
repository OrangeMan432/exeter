package me.larp.client.module.impl.toggle.misc;

import me.larp.api.event.Listener;
import me.larp.api.event.Stage;
import me.larp.client.events.TickEvent;
import me.larp.client.module.ModuleType;
import me.larp.client.module.ToggleableModule;
import me.larp.client.properties.Property;
import me.larp.client.util.PlayerUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Swaps to the fastest hotbar tool for the block you are mining. */
public class AutoTool extends ToggleableModule {

  private final Property<Boolean> swingHand = new Property<Boolean>(false, "Swing Hand");

  public AutoTool() {
    super("AutoTool", new String[] {"autotool", "auto-tool"}, 0x00FFFF, ModuleType.MISCELLANEOUS);
    setDescription("Auto-swaps to the best mining tool.");
    offerProperties(swingHand);
    this.listeners.add(
        new Listener<TickEvent>("autotool_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            AutoTool.this.onTick();
          }
        });
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null || minecraft.gameMode == null) return;
    if (minecraft.player.isDeadOrDying()) return;
    if (!minecraft.gameMode.isDestroying()) return;
    if (!(minecraft.hitResult instanceof BlockHitResult hit)) return;

    BlockState state = minecraft.level.getBlockState(hit.getBlockPos());
    if (state.isAir()) return;

    int best = -1;
    float bestSpeed = 1.0f;
    for (int i = 0; i < 9; i++) {
      ItemStack stack = minecraft.player.getInventory().getItem(i);
      if (stack.isEmpty()) continue;
      float speed = stack.getDestroySpeed(state);
      if (speed > bestSpeed) {
        bestSpeed = speed;
        best = i;
      }
    }
    if (best != -1 && best != minecraft.player.getInventory().getSelectedSlot()) {
      PlayerUtil.swapTo(best);
      if (swingHand.getValue()) {
        PlayerUtil.swingHand();
      }
    }
  }
}
