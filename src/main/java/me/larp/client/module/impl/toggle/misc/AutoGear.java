package me.larp.client.module.impl.toggle.misc;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import me.larp.api.event.Listener;
import me.larp.api.event.Stage;
import me.larp.client.command.impl.server.GearCommand;
import me.larp.client.events.TickEvent;
import me.larp.client.module.ModuleType;
import me.larp.client.module.ToggleableModule;
import me.larp.client.properties.Property;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.item.ItemStack;

public class AutoGear extends ToggleableModule {

  private final Property<Boolean> enderChest = new Property<Boolean>(false, "Ender Chest");
  private final Property<Boolean> confirmSort = new Property<Boolean>(true, "Confirm Sort");
  private final Property<Boolean> invasive = new Property<Boolean>(false, "Invasive");
  private final Property<Boolean> closeAfter = new Property<Boolean>(false, "Close After");
  private final me.larp.client.properties.NumberProperty<Integer> tickDelay =
      new me.larp.client.properties.NumberProperty<Integer>(0, 0, 20, "Tick Delay");
  private final me.larp.client.properties.NumberProperty<Integer> movesPerTick =
      new me.larp.client.properties.NumberProperty<Integer>(36, 1, 36, "Moves Per Tick");

  private HashMap<Integer, String> planInventory = new HashMap<>();
  private final HashMap<Integer, String> containerInv = new HashMap<>();
  private ArrayList<Integer> sortItems = new ArrayList<>();
  private int delayTimeTicks;
  private int stepNow;
  private boolean openedBefore;
  private boolean finishSort;
  private boolean doneBefore;

  private final Listener<TickEvent> tickListener =
      new Listener<TickEvent>("autogear_tick") {
        @Override
        public void call(TickEvent event) {
          if (event.getStage() != Stage.PRE) return;
          onTick();
        }
      };

  public AutoGear() {
    super("AutoGear", new String[] {"autogear", "auto-gear"}, 0xFF0000, ModuleType.MISCELLANEOUS);
    setDescription("Automatically sorts and equips best gear from your inventory.");
    offerProperties(enderChest, confirmSort, invasive, closeAfter, tickDelay, movesPerTick);
    this.listeners.add(tickListener);
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    reload();
  }

  @Override
  protected void onDisable() {
    super.onDisable();
    planInventory.clear();
    containerInv.clear();
    sortItems.clear();
    delayTimeTicks = 0;
    stepNow = 0;
    openedBefore = false;
    finishSort = false;
    doneBefore = false;
  }

  public void reload() {
    String curConfigName = GearCommand.getCurrentSet();
    if (curConfigName.isEmpty()) {
      planInventory = new HashMap<>();
      delayTimeTicks = 0;
      openedBefore = false;
      doneBefore = false;
      return;
    }

    String inventoryConfig = GearCommand.getInventoryKit(curConfigName);
    if (inventoryConfig.isEmpty()) {
      planInventory = new HashMap<>();
      delayTimeTicks = 0;
      openedBefore = false;
      doneBefore = false;
      return;
    }

    String[] inventoryDivided = inventoryConfig.split(" ");
    planInventory = new HashMap<>();

    for (int i = 0; i < inventoryDivided.length; i++) {
      if (!inventoryDivided[i].contains("air")) {
        planInventory.put(i, inventoryDivided[i]);
      }
    }

    delayTimeTicks = 0;
    openedBefore = false;
    doneBefore = false;
  }

  private void onTick() {
    if (minecraft.player == null || minecraft.level == null) return;
    if (planInventory.isEmpty()) return;

    if (delayTimeTicks < tickDelay.getValue()) {
      delayTimeTicks++;
      return;
    }
    delayTimeTicks = 0;

    AbstractContainerMenu menu = minecraft.player.containerMenu;
    if (menu instanceof ChestMenu || menu instanceof ShulkerBoxMenu) {
      boolean chest = menu instanceof ChestMenu;
      boolean isEnderChest =
          chest && ((ChestMenu) menu).getContainer() instanceof PlayerEnderChestContainer;
      if ((!chest || (!enderChest.getValue() && isEnderChest))
          && !(menu instanceof ShulkerBoxMenu)) {
        openedBefore = false;
      } else {
        sortInventoryAlgo(menu);
      }
    } else {
      openedBefore = false;
    }
  }

  private void sortInventoryAlgo(AbstractContainerMenu menu) {
    if (!openedBefore) {
      int maxValue =
          menu instanceof ChestMenu ? ((ChestMenu) menu).getContainer().getContainerSize() : 27;

      containerInv.clear();
      for (int i = 0; i < maxValue; i++) {
        ItemStack item = menu.slots.get(i).getItem();
        containerInv.put(i, getItemKey(item));
      }

      openedBefore = true;
      HashMap<Integer, String> inventoryCopy = getInventoryCopy(menu, maxValue);
      HashMap<Integer, String> aimInventory = getInventoryCopy(maxValue, planInventory);
      sortItems = getInventorySort(menu, inventoryCopy, aimInventory, maxValue);

      if (sortItems.isEmpty() && !doneBefore) {
        finishSort = false;
        if (closeAfter.getValue()) {
          minecraft.player.closeContainer();
        }
      } else {
        finishSort = true;
        stepNow = 0;
      }
    } else if (finishSort) {
      for (int i = 0; i < movesPerTick.getValue(); i++) {
        if (!sortItems.isEmpty()) {
          int slotChange = sortItems.get(stepNow++);
          minecraft.gameMode.handleContainerInput(
              menu.containerId, slotChange, 0, ContainerInput.PICKUP, minecraft.player);
        }

        if (stepNow == sortItems.size()) {
          if (confirmSort.getValue() && !doneBefore) {
            openedBefore = false;
            finishSort = false;
            doneBefore = true;
            checkLastItem(menu);
            return;
          }

          finishSort = false;

          checkLastItem(menu);
          doneBefore = false;
          if (closeAfter.getValue()) {
            minecraft.player.closeContainer();
          }

          return;
        }
      }
    }
  }

  private void checkLastItem(AbstractContainerMenu menu) {
    if (!sortItems.isEmpty()) {
      int slotChange = sortItems.get(sortItems.size() - 1);
      if (menu.slots.get(slotChange).getItem().isEmpty()) {
        minecraft.gameMode.handleContainerInput(
            menu.containerId, slotChange, 0, ContainerInput.PICKUP, minecraft.player);
      }
    }
  }

  private ArrayList<Integer> getInventorySort(
      AbstractContainerMenu menu,
      HashMap<Integer, String> copyInventory,
      HashMap<Integer, String> planInventoryCopy,
      int startValues) {
    ArrayList<Integer> planMove = new ArrayList<>();
    HashMap<String, Integer> nItemsCopy = new HashMap<>();

    for (String value : planInventoryCopy.values()) {
      nItemsCopy.merge(value, 1, Integer::sum);
    }

    ArrayList<Integer> ignoreValues = new ArrayList<>();
    int[] listValue = new int[planInventoryCopy.size()];
    int id = 0;
    for (int idx : planInventoryCopy.keySet()) {
      listValue[id++] = idx;
    }

    for (int item : listValue) {
      if (copyInventory.get(item).equals(planInventoryCopy.get(item))) {
        ignoreValues.add(item);
        nItemsCopy.put(
            planInventoryCopy.get(item), nItemsCopy.get(planInventoryCopy.get(item)) - 1);
        if (nItemsCopy.get(planInventoryCopy.get(item)) == 0) {
          nItemsCopy.remove(planInventoryCopy.get(item));
        }
        planInventoryCopy.remove(item);
      }
    }

    String pickedItem = null;
    for (int i = startValues; i < startValues + copyInventory.size(); i++) {
      if (!ignoreValues.contains(i)) {
        String itemCheck = copyInventory.get(i);
        Optional<Map.Entry<Integer, String>> momentAim =
            planInventoryCopy.entrySet().stream()
                .filter(x -> x.getValue().equals(itemCheck))
                .findFirst();
        if (momentAim.isPresent()) {
          if (pickedItem == null) {
            planMove.add(i);
          }

          int aimKey = momentAim.get().getKey();
          planMove.add(aimKey);

          if (pickedItem == null || !pickedItem.equals(itemCheck)) {
            ignoreValues.add(aimKey);
          }

          nItemsCopy.put(itemCheck, nItemsCopy.get(itemCheck) - 1);
          if (nItemsCopy.get(itemCheck) == 0) {
            nItemsCopy.remove(itemCheck);
          }

          copyInventory.put(i, copyInventory.get(aimKey));
          copyInventory.put(aimKey, itemCheck);

          if (!copyInventory.get(aimKey).equals("minecraft:air0")) {
            if (i >= startValues + copyInventory.size()) {
              continue;
            }
            pickedItem = copyInventory.get(i);
            i--;
          } else {
            pickedItem = null;
          }

          planInventoryCopy.remove(aimKey);
        } else if (pickedItem != null) {
          planMove.add(i);
          copyInventory.put(i, pickedItem);
          pickedItem = null;
        }
      }
    }

    if (planMove.size() >= 2
        && planMove.get(planMove.size() - 1).equals(planMove.get(planMove.size() - 2))) {
      planMove.remove(planMove.size() - 1);
    }

    Object[] keyList = containerInv.keySet().toArray();
    for (Object value : keyList) {
      int itemC = (Integer) value;
      if (nItemsCopy.containsKey(containerInv.get(itemC))) {
        Optional<Map.Entry<Integer, String>> match =
            planInventoryCopy.entrySet().stream()
                .filter(x -> x.getValue().equals(containerInv.get(itemC)))
                .findFirst();
        if (match.isPresent()) {
          int start = match.get().getKey();
          if (invasive.getValue() || menu.slots.get(start).getItem().isEmpty()) {
            planMove.add(start);
            planMove.add(itemC);
            planMove.add(start);
            nItemsCopy.put(
                planInventoryCopy.get(start), nItemsCopy.get(planInventoryCopy.get(start)) - 1);
            if (nItemsCopy.get(planInventoryCopy.get(start)) == 0) {
              nItemsCopy.remove(planInventoryCopy.get(start));
            }
            planInventoryCopy.remove(start);
          }
        }
      }
    }

    return planMove;
  }

  private HashMap<Integer, String> getInventoryCopy(AbstractContainerMenu menu, int startPoint) {
    HashMap<Integer, String> output = new HashMap<>();
    int sizeInventory = minecraft.player.getInventory().getNonEquipmentItems().size();

    for (int i = 0; i < sizeInventory; i++) {
      int value = i + startPoint + (i < 9 ? sizeInventory - 9 : -9);
      output.put(value, getItemKey(menu.slots.get(value).getItem()));
    }

    return output;
  }

  private HashMap<Integer, String> getInventoryCopy(
      int startPoint, HashMap<Integer, String> inventory) {
    HashMap<Integer, String> output = new HashMap<>();
    int sizeInventory = minecraft.player.getInventory().getNonEquipmentItems().size();

    for (int val : inventory.keySet()) {
      output.put(val + startPoint + (val < 9 ? sizeInventory - 9 : -9), inventory.get(val));
    }

    return output;
  }

  public static String getItemKey(ItemStack item) {
    if (item.isEmpty()) {
      return "minecraft:air0";
    }
    return BuiltInRegistries.ITEM.getKey(item.getItem()).toString() + item.getDamageValue();
  }

  public static File getFile() {
    return new File(
        net.minecraft.client.Minecraft.getInstance().gameDirectory, "LarpClient/AutoGear.json");
  }

  public static JsonObject readJson() {
    try {
      File file = getFile();
      if (!file.exists()) return null;
      return JsonParser.parseReader(new FileReader(file)).getAsJsonObject();
    } catch (Exception e) {
      return null;
    }
  }

  public static void writeJson(JsonObject json) {
    try {
      File file = getFile();
      if (file.getParentFile() != null) {
        file.getParentFile().mkdirs();
      }
      java.io.FileWriter writer = new java.io.FileWriter(file);
      writer.write(json.toString());
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
