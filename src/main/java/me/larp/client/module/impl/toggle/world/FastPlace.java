package me.larp.client.module.impl.toggle.world;

import me.larp.api.event.Listener;
import me.larp.api.event.Stage;
import me.larp.client.events.TickEvent;
import me.larp.client.module.ModuleType;
import me.larp.client.module.ToggleableModule;
import me.larp.client.properties.EnumProperty;
import me.larp.client.properties.NumberProperty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Shoreline-pattern FastPlace: hammer the use key path directly so place
 * cooldown never applies. Whitelist, blacklist, or everything.
 */
public class FastPlace extends ToggleableModule {

  public enum Selection {
    WHITELIST,
    BLACKLIST,
    ALL
  }

  private final EnumProperty<Selection> selection =
      new EnumProperty<Selection>(Selection.WHITELIST, "Selection");
  private final NumberProperty<Integer> delay =
      new NumberProperty<Integer>(0, 0, 4, "Delay");

  private int tickCounter;

  public FastPlace() {
    super("FastPlace", new String[] {"fastplace", "fast-place"}, 0x0000FF, ModuleType.WORLD);
    setDescription("Places and uses items with no cooldown.");
    offerProperties(selection, delay);
    this.listeners.add(
        new Listener<TickEvent>("fastplace_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            FastPlace.this.onTick();
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
    if (!minecraft.options.keyUse.isDown()) {
      tickCounter = 0;
      return;
    }
    if (tickCounter++ < delay.getValue()) return;
    ItemStack held = minecraft.player.getMainHandItem();
    if (held.isEmpty() || !allowed(held.getItem())) return;
    minecraft.gameMode.useItem(minecraft.player, InteractionHand.MAIN_HAND);
  }

  private boolean allowed(Item item) {
    return switch (selection.getValue()) {
      case WHITELIST ->
          item == Items.EXPERIENCE_BOTTLE
              || item == Items.SNOWBALL
              || item == Items.EGG
              || item == Items.WATER_BUCKET
              || item == Items.LAVA_BUCKET;
      case BLACKLIST ->
          item != Items.ENDER_PEARL
              && item != Items.ENDER_EYE
              && item != Items.CHORUS_FRUIT;
      case ALL -> true;
    };
  }
}
