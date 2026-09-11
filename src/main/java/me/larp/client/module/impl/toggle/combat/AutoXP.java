package me.larp.client.module.impl.toggle.combat;

import me.larp.api.event.Listener;
import me.larp.api.event.Stage;
import me.larp.client.events.TickEvent;
import me.larp.client.module.ModuleType;
import me.larp.client.module.ToggleableModule;
import me.larp.client.properties.NumberProperty;
import me.larp.client.properties.Property;
import me.larp.client.util.PlayerUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Throws XP bottles for mending, looking straight down like the originals. */
public class AutoXP extends ToggleableModule {

  private final NumberProperty<Integer> delay =
      new NumberProperty<Integer>(2, 0, 20, "Delay");
  private final Property<Boolean> rotate = new Property<Boolean>(true, "Rotate");
  private final Property<Boolean> autoSwitch = new Property<Boolean>(true, "Auto Switch");

  private int tickCounter;

  public AutoXP() {
    super("AutoXP", new String[] {"autoxp", "auto-xp"}, 0xFF0000, ModuleType.COMBAT);
    setDescription("Throws XP bottles to mend gear.");
    offerProperties(delay, rotate, autoSwitch);
    this.listeners.add(
        new Listener<TickEvent>("autoxp_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            AutoXP.this.onTick();
          }
        });
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    tickCounter = 0;
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null || minecraft.gameMode == null) return;
    if (minecraft.player.isDeadOrDying()) return;
    if (tickCounter++ < delay.getValue()) return;
    tickCounter = 0;

    int bottleSlot = PlayerUtil.findInHotbar(
        s -> !s.isEmpty() && s.getItem() == Items.EXPERIENCE_BOTTLE);
    if (bottleSlot == -1) return;

    boolean needSwitch =
        autoSwitch.getValue() && bottleSlot != minecraft.player.getInventory().getSelectedSlot();
    if (needSwitch) {
      PlayerUtil.swapTo(bottleSlot);
    }
    ItemStack held = minecraft.player.getMainHandItem();
    if (held.getItem() != Items.EXPERIENCE_BOTTLE) {
      if (needSwitch) {
        PlayerUtil.swapBack();
      }
      return;
    }

    float yaw = minecraft.player.getYRot();
    float pitch = minecraft.player.getXRot();
    if (rotate.getValue()) {
      PlayerUtil.setRotation(yaw, 90f);
    }
    minecraft.gameMode.useItem(minecraft.player, InteractionHand.MAIN_HAND);
    if (rotate.getValue()) {
      PlayerUtil.restoreRotation(yaw, pitch);
    }
    if (needSwitch) {
      PlayerUtil.swapBack();
    }
  }
}
