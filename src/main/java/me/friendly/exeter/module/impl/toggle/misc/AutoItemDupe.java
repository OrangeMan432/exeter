package me.friendly.exeter.module.impl.toggle.misc;

import java.util.List;
import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.Property;
import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;

public class AutoItemDupe extends ToggleableModule {

  private enum Phase {
    NONE,
    THROW,
    WAIT,
    RECIPE,
    DONE
  }

  private final Property<Boolean> cancelGui = new Property<>(false, "Cancel GUI");

  private Phase phase = Phase.NONE;
  private long phaseStart;
  private int throwSlot;
  private int recipeDisplayId = -1;

  public AutoItemDupe() {
    super("AutoItemDupe", new String[] {"autoitemdupe", "aidd"}, ModuleType.MISCELLANEOUS);
    setDescription("Dupes wooden buttons using the crafting recipe bug.");
    offerProperties(cancelGui);

    this.listeners.add(
        new Listener<TickEvent>("auto_item_dupe_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            AutoItemDupe.this.onTick();
          }
        });
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    phase = Phase.NONE;
    recipeDisplayId = -1;

    if (minecraft.player == null || minecraft.level == null) return;
    if (minecraft.player.getInventory().getSelectedItem().isEmpty()) return;
    if (!hasPlanks()) return;

    recipeDisplayId = findWoodenButtonRecipe();
    if (recipeDisplayId == -1) return;

    throwSlot = minecraft.player.getInventory().getSelectedSlot();
    phase = Phase.THROW;
    phaseStart = System.currentTimeMillis();
  }

  @Override
  protected void onDisable() {
    super.onDisable();
    phase = Phase.NONE;
    if (cancelGui.getValue() && minecraft.gui.screen() != null) {
      minecraft.gui.setScreen(null);
    }
  }

  private void onTick() {
    if (minecraft.player == null || minecraft.level == null) return;
    if (phase == Phase.NONE) return;

    long elapsed = System.currentTimeMillis() - phaseStart;

    switch (phase) {
      case THROW -> {
        if (elapsed < 150) return;
        minecraft.player.containerMenu.clicked(
            throwSlot + 36, 1, ContainerInput.THROW, minecraft.player);
        phase = Phase.WAIT;
        phaseStart = System.currentTimeMillis();
      }
      case WAIT -> {
        if (elapsed < 1000) return;
        phase = Phase.RECIPE;
        phaseStart = System.currentTimeMillis();
      }
      case RECIPE -> {
        if (elapsed < 100) return;
        minecraft
            .getConnection()
            .send(
                new ServerboundPlaceRecipePacket(
                    minecraft.player.containerMenu.containerId,
                    new RecipeDisplayId(recipeDisplayId),
                    false));
        phase = Phase.DONE;
        phaseStart = System.currentTimeMillis();
      }
      case DONE -> {
        if (elapsed < 500) return;
        phase = Phase.NONE;
      }
      default -> {}
    }
  }

  private boolean hasPlanks() {
    for (int i = 0; i < 36; i++) {
      ItemStack s = minecraft.player.getInventory().getItem(i);
      if (!s.isEmpty()
          && s.getItem() instanceof BlockItem bi
          && bi.getBlock().defaultBlockState().is(BlockTags.PLANKS)) {
        return true;
      }
    }
    return false;
  }

  private int findWoodenButtonRecipe() {
    var recipeBook = minecraft.player.getRecipeBook();
    var collections = recipeBook.getCollections();
    for (var collection : collections) {
      List<RecipeDisplayEntry> entries = collection.getRecipes();
      for (var entry : entries) {
        var reqs = entry.craftingRequirements();
        if (reqs.isEmpty()) continue;
        List<Ingredient> ingredients = reqs.get();
        if (ingredients.size() == 1) {
          ItemStack plankTest = new ItemStack(net.minecraft.world.item.Items.OAK_PLANKS);
          if (ingredients.get(0).test(plankTest)) {
            return entry.id().index();
          }
        }
      }
    }
    return -1;
  }
}
