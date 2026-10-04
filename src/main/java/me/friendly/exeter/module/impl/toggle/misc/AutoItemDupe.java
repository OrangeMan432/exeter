package me.friendly.exeter.module.impl.toggle.misc;

import java.util.List;
import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.logging.DebugLogger;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.Property;
import me.friendly.exeter.util.NotificationManager;
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

    if (minecraft.player == null || minecraft.level == null) {
      DebugLogger.get().log(getLabel(), DebugLogger.Level.WARN, "player/level null on enable");
      return;
    }
    if (minecraft.player.getInventory().getSelectedItem().isEmpty()) {
      DebugLogger.get().log(getLabel(), DebugLogger.Level.WARN, "selected slot empty, abort");
      return;
    }
    if (!hasPlanks()) {
      DebugLogger.get().log(getLabel(), DebugLogger.Level.WARN, "no planks in inventory");
      return;
    }

    recipeDisplayId = findWoodenButtonRecipe();
    if (recipeDisplayId == -1) {
      DebugLogger.get()
          .log(
              getLabel(), DebugLogger.Level.WARN, "recipe not found on enable, will retry on tick");
    } else {
      DebugLogger.get().log(getLabel(), DebugLogger.Level.INFO, "recipe found " + recipeDisplayId);
    }

    throwSlot = minecraft.player.getInventory().getSelectedSlot();
    DebugLogger.get()
        .log(
            getLabel(),
            DebugLogger.Level.INFO,
            "starting dupe slot=" + throwSlot + " recipe=" + recipeDisplayId);
    NotificationManager.push("AutoDupe started slot " + throwSlot, "crafting_table");
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
        DebugLogger.get().log(getLabel(), DebugLogger.Level.INFO, "THROW slot " + throwSlot);
        NotificationManager.push("Dupe: throwing", "dropper");
        try {
          if (minecraft.gui.screen() == null) {
            if (minecraft.player.getInventory().getSelectedSlot() != throwSlot) {
              minecraft.player.getInventory().setSelectedSlot(throwSlot);
              minecraft
                  .getConnection()
                  .send(
                      new net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket(
                          throwSlot));
              // wait a tick for server to ack slot change before dropping
              phaseStart = System.currentTimeMillis();
              return;
            }
            // use ServerboundPlayerActionPacket like pressing Q - more reliable than container
            // click for Via
            // DROP_ALL_ITEMS (entire stack) matches THROW button 1
            minecraft
                .getConnection()
                .send(
                    new net.minecraft.network.protocol.game.ServerboundPlayerActionPacket(
                        net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action
                            .DROP_ALL_ITEMS,
                        minecraft.player.blockPosition(),
                        net.minecraft.core.Direction.DOWN));
          } else {
            minecraft.player.containerMenu.clicked(
                throwSlot + 36, 1, ContainerInput.THROW, minecraft.player);
          }
        } catch (Exception e) {
          DebugLogger.get()
              .log(getLabel(), DebugLogger.Level.ERROR, "throw failed " + e.getMessage());
          NotificationManager.push("Dupe throw failed", "error");
          phase = Phase.NONE;
          return;
        }
        phase = Phase.WAIT;
        phaseStart = System.currentTimeMillis();
      }
      case WAIT -> {
        if (elapsed < 1000) return;
        DebugLogger.get().log(getLabel(), DebugLogger.Level.INFO, "WAIT -> RECIPE");
        NotificationManager.push("Dupe: waiting done", "clock");
        phase = Phase.RECIPE;
        phaseStart = System.currentTimeMillis();
      }
      case RECIPE -> {
        if (elapsed < 100) return;
        if (recipeDisplayId == -1) {
          recipeDisplayId = findWoodenButtonRecipe();
          if (recipeDisplayId == -1) {
            DebugLogger.get().log(getLabel(), DebugLogger.Level.ERROR, "still no recipe, abort");
            NotificationManager.push("Dupe: recipe still not found", "error");
            phase = Phase.NONE;
            return;
          }
          DebugLogger.get()
              .log(getLabel(), DebugLogger.Level.INFO, "found recipe on retry " + recipeDisplayId);
        }
        DebugLogger.get()
            .log(getLabel(), DebugLogger.Level.INFO, "sending recipe " + recipeDisplayId);
        NotificationManager.push("Dupe: crafting", "crafting_table");
        try {
          minecraft
              .getConnection()
              .send(
                  new ServerboundPlaceRecipePacket(
                      minecraft.player.containerMenu.containerId,
                      new RecipeDisplayId(recipeDisplayId),
                      false));
        } catch (Exception e) {
          DebugLogger.get()
              .log(getLabel(), DebugLogger.Level.ERROR, "recipe send failed " + e.getMessage());
          NotificationManager.push("Dupe recipe failed", "error");
          phase = Phase.NONE;
          return;
        }
        phase = Phase.DONE;
        phaseStart = System.currentTimeMillis();
      }
      case DONE -> {
        if (elapsed < 500) return;
        DebugLogger.get().log(getLabel(), DebugLogger.Level.INFO, "DONE -> NONE");
        NotificationManager.push("Dupe complete", "success");
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

  private boolean isPlanksIngredient(Ingredient ing) {
    ItemStack[] tests = {
      new ItemStack(net.minecraft.world.item.Items.OAK_PLANKS),
      new ItemStack(net.minecraft.world.item.Items.SPRUCE_PLANKS),
      new ItemStack(net.minecraft.world.item.Items.BIRCH_PLANKS),
      new ItemStack(net.minecraft.world.item.Items.JUNGLE_PLANKS),
      new ItemStack(net.minecraft.world.item.Items.ACACIA_PLANKS),
      new ItemStack(net.minecraft.world.item.Items.DARK_OAK_PLANKS),
      new ItemStack(net.minecraft.world.item.Items.MANGROVE_PLANKS),
      new ItemStack(net.minecraft.world.item.Items.CHERRY_PLANKS),
      new ItemStack(net.minecraft.world.item.Items.PALE_OAK_PLANKS)
    };
    for (ItemStack s : tests) if (ing.test(s)) return true;
    return false;
  }

  private int findWoodenButtonRecipe() {
    if (minecraft.player == null) return -1;
    var recipeBook = minecraft.player.getRecipeBook();
    var collections = recipeBook.getCollections();
    // primary: recipe book
    for (var collection : collections) {
      List<RecipeDisplayEntry> entries = collection.getRecipes();
      for (var entry : entries) {
        var reqs = entry.craftingRequirements();
        if (reqs.isEmpty()) continue;
        List<Ingredient> ingredients = reqs.get();
        if (ingredients.size() == 1 && isPlanksIngredient(ingredients.get(0))) {
          return entry.id().index();
        }
      }
    }
    DebugLogger.get()
        .log(getLabel(), DebugLogger.Level.WARN, "recipe book size " + collections.size());
    return -1;
  }
}
