package me.friendly.exeter.module.impl.toggle.combat;

import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import me.friendly.exeter.util.PlayerUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class AutoEat extends ToggleableModule {

  private final NumberProperty<Integer> hunger =
      new NumberProperty<Integer>(16, 0, 20, "Hunger");
  private final NumberProperty<Double> health =
      new NumberProperty<Double>(14.0, 0.0, 20.0, "Health");
  private final Property<Boolean> gappleFirst =
      new Property<Boolean>(true, "Gapple First");

  private boolean eating;
  private int eatSlot = -1;

  public AutoEat() {
    super("AutoEat", new String[] {"autoeat", "auto-eat"}, 0xFF0000, ModuleType.COMBAT);
    setDescription("Eats food or gapples when hungry or low.");
    offerProperties(hunger, health, gappleFirst);
    this.listeners.add(
        new Listener<TickEvent>("autoeat_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            AutoEat.this.onTick();
          }
        });
  }

  @Override
  protected void onDisable() {
    super.onDisable();
    stopEating();
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null || minecraft.gameMode == null) return;
    if (minecraft.player.isDeadOrDying()) return;

    boolean needFood = minecraft.player.getFoodData().getFoodLevel() <= hunger.getValue();
    boolean needHeal = minecraft.player.getHealth() <= health.getValue().floatValue()
        && hasGapple();

    if (eating) {
      if ((!needFood && !needHeal) || eatSlot == -1) {
        stopEating();
        return;
      }
      // Keep holding right-click on the food.
      minecraft.gameMode.useItem(minecraft.player, InteractionHand.MAIN_HAND);
      return;
    }

    if (!needFood && !needHeal) return;

    int slot = -1;
    if (needHeal && gappleFirst.getValue()) {
      slot = findGapple();
    }
    if (slot == -1) {
      slot = findFood();
    }
    if (slot == -1) return;

    eatSlot = slot;
    boolean needSwitch = slot != minecraft.player.getInventory().getSelectedSlot();
    if (needSwitch) {
      PlayerUtil.swapTo(slot);
    }
    minecraft.gameMode.useItem(minecraft.player, InteractionHand.MAIN_HAND);
    eating = true;
  }

  private void stopEating() {
    if (eating && minecraft.player != null) {
      minecraft.player.stopUsingItem();
      PlayerUtil.swapBack();
    }
    eating = false;
    eatSlot = -1;
  }

  private boolean hasGapple() {
    return findGapple() != -1;
  }

  private int findGapple() {
    return PlayerUtil.findInHotbar(
        s -> !s.isEmpty()
            && (s.getItem() == Items.GOLDEN_APPLE || s.getItem() == Items.ENCHANTED_GOLDEN_APPLE));
  }

  private int findFood() {
    // 6b6t-pattern SmartEat: highest saturation wins, nutrition breaks ties.
    int best = -1;
    double bestScore = -1.0;
    for (int i = 0; i < 9; i++) {
      ItemStack stack = minecraft.player.getInventory().getItem(i);
      if (stack.isEmpty()) continue;
      var food = stack.get(net.minecraft.core.component.DataComponents.FOOD);
      if (food == null) continue;
      double score = food.saturation() * 2.0 + food.nutrition();
      if (score > bestScore) {
        bestScore = score;
        best = i;
      }
    }
    return best;
  }
}
