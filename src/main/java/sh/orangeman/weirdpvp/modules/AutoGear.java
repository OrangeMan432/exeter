package sh.orangeman.weirdpvp.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.item.ItemStack;
import sh.orangeman.weirdpvp.WeirdPvP;
import sh.orangeman.weirdpvp.commands.AutoGearCommand;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class AutoGear extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> tickDelay = sgGeneral.add(new IntSetting.Builder()
        .name("tick-delay")
        .description("Delay in ticks between sort actions.")
        .defaultValue(0)
        .range(0, 20)
        .build()
    );

    private final Setting<Integer> switchForTick = sgGeneral.add(new IntSetting.Builder()
        .name("switch-per-tick")
        .description("The amount of items to move per tick.")
        .defaultValue(1)
        .range(1, 100)
        .build()
    );

    private final Setting<Boolean> enderChest = sgGeneral.add(new BoolSetting.Builder()
        .name("ender-chest")
        .description("Also sorts when an ender chest is open.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> confirmSort = sgGeneral.add(new BoolSetting.Builder()
        .name("confirm-sort")
        .description("Re-checks the inventory once after sorting.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> invasive = sgGeneral.add(new BoolSetting.Builder()
        .name("invasive")
        .description("Overwrites inventory slots that don't match the plan.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> closeAfter = sgGeneral.add(new BoolSetting.Builder()
        .name("close-after")
        .description("Closes the container when the sort is finished.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> infoMsgs = sgGeneral.add(new BoolSetting.Builder()
        .name("info-msgs")
        .description("Prints messages when a config is activated or the inventory is sorted.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> debugMode = sgGeneral.add(new BoolSetting.Builder()
        .name("debug-mode")
        .description("Prints every click the module would make.")
        .defaultValue(false)
        .build()
    );

    private HashMap<Integer, String> planInventory = new HashMap<>();
    private final HashMap<Integer, String> containerInv = new HashMap<>();
    private ArrayList<Integer> sortItems = new ArrayList<>();
    private int delayTimeTicks;
    private int stepNow;
    private boolean openedBefore;
    private boolean finishSort;
    private boolean doneBefore;

    public AutoGear() {
        super(WeirdPvP.CATEGORY, "auto-gear", "Sorts your inventory to match a saved kit from a container.");
    }

    @Override
    public void onActivate() {
        reload();
    }

    @Override
    public void onDeactivate() {
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
        String curConfigName = AutoGearCommand.getCurrentSet();
        if (curConfigName.equals("")) {
            planInventory = new HashMap<>();
            delayTimeTicks = 0;
            openedBefore = false;
            doneBefore = false;
            return;
        }

        if (infoMsgs.get()) {
            info("Config %s activated.", curConfigName);
        }

        String inventoryConfig = AutoGearCommand.getInventoryKit(curConfigName);
        if (inventoryConfig.equals("")) {
            planInventory = new HashMap<>();
            delayTimeTicks = 0;
            openedBefore = false;
            doneBefore = false;
            return;
        }

        String[] inventoryDivided = inventoryConfig.split(" ");
        planInventory = new HashMap<>();
        HashMap<String, Integer> nItems = new HashMap<>();

        for (int i = 0; i < inventoryDivided.length; i++) {
            if (!inventoryDivided[i].contains("air")) {
                planInventory.put(i, inventoryDivided[i]);
                nItems.merge(inventoryDivided[i], 1, Integer::sum);
            }
        }

        delayTimeTicks = 0;
        openedBefore = false;
        doneBefore = false;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (delayTimeTicks < tickDelay.get()) {
            delayTimeTicks++;
        } else {
            delayTimeTicks = 0;
            if (planInventory.size() == 0) {
                return;
            }

            AbstractContainerMenu menu = mc.player.containerMenu;
            if (menu instanceof ChestMenu || menu instanceof ShulkerBoxMenu) {
                boolean chest = menu instanceof ChestMenu;
                boolean isEnderChest = chest && ((ChestMenu) menu).getContainer() instanceof PlayerEnderChestContainer;
                if ((!chest || (!enderChest.get() && isEnderChest)) && !(menu instanceof ShulkerBoxMenu)) {
                    openedBefore = false;
                } else {
                    sortInventoryAlgo(menu);
                }
            } else {
                openedBefore = false;
            }
        }
    }

    private void sortInventoryAlgo(AbstractContainerMenu menu) {
        if (!openedBefore) {
            int maxValue = menu instanceof ChestMenu ? ((ChestMenu) menu).getContainer().getContainerSize() : 27;

            containerInv.clear();
            for (int i = 0; i < maxValue; i++) {
                ItemStack item = menu.slots.get(i).getItem();
                containerInv.put(i, AutoGearCommand.getItemKey(item));
            }

            openedBefore = true;
            HashMap<Integer, String> inventoryCopy = getInventoryCopy(menu, maxValue);
            HashMap<Integer, String> aimInventory = getInventoryCopy(maxValue, planInventory);
            sortItems = getInventorySort(menu, inventoryCopy, aimInventory, maxValue);

            if (sortItems.size() == 0 && !doneBefore) {
                finishSort = false;
                if (closeAfter.get()) {
                    mc.player.closeContainer();
                }
            } else {
                finishSort = true;
                stepNow = 0;
            }
        } else if (finishSort) {
            for (int i = 0; i < switchForTick.get(); i++) {
                if (sortItems.size() != 0) {
                    if (!menu.getCarried().isEmpty()) flushCursor(menu);
                    int slotChange = sortItems.get(stepNow++);
                    mc.gameMode.handleContainerInput(menu.containerId, slotChange, 0, ContainerInput.PICKUP, mc.player);
                }

                if (stepNow == sortItems.size()) {
                    if (confirmSort.get() && !doneBefore) {
                        openedBefore = false;
                        finishSort = false;
                        doneBefore = true;
                        checkLastItem(menu);
                        return;
                    }

                    finishSort = false;
                    if (infoMsgs.get()) {
                        info("Inventory sorted.");
                    }

                    checkLastItem(menu);
                    doneBefore = false;
                    if (closeAfter.get()) {
                        mc.player.closeContainer();
                    }

                    return;
                }
            }
        }
    }

    private void checkLastItem(AbstractContainerMenu menu) {
        flushCursor(menu);
    }

    // Overstacked (e.g. 127-count) items can't always fit in one slot per click, so a PICKUP
    // place leaves the remainder on the cursor. Flush it into any empty or partially-fillable
    // slot so the next pick starts from a clean cursor and the sort isn't corrupted.
    private void flushCursor(AbstractContainerMenu menu) {
        int guard = 0;
        while (!menu.getCarried().isEmpty() && guard++ < 128) {
            ItemStack cursor = menu.getCarried();
            boolean placed = false;
            for (int i = 0; i < menu.slots.size(); i++) {
                ItemStack slot = menu.slots.get(i).getItem();
                if (slot.isEmpty()) {
                    mc.gameMode.handleContainerInput(menu.containerId, i, 0, ContainerInput.PICKUP, mc.player);
                    placed = true;
                    break;
                } else if (ItemStack.matches(slot, cursor)
                        && slot.getCount() < slot.getMaxStackSize()) {
                    mc.gameMode.handleContainerInput(menu.containerId, i, 0, ContainerInput.PICKUP, mc.player);
                    placed = true;
                    break;
                }
            }
            if (!placed) break;
        }
    }

    private ArrayList<Integer> getInventorySort(AbstractContainerMenu menu, HashMap<Integer, String> copyInventory, HashMap<Integer, String> planInventoryCopy, int startValues) {
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
                nItemsCopy.put(planInventoryCopy.get(item), nItemsCopy.get(planInventoryCopy.get(item)) - 1);
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
                Optional<Map.Entry<Integer, String>> momentAim = planInventoryCopy.entrySet().stream()
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

        if (planMove.size() >= 2 && planMove.get(planMove.size() - 1).equals(planMove.get(planMove.size() - 2))) {
            planMove.remove(planMove.size() - 1);
        }

        Object[] keyList = containerInv.keySet().toArray();
        for (Object value : keyList) {
            int itemC = (Integer) value;
            if (nItemsCopy.containsKey(containerInv.get(itemC))) {
                int start = planInventoryCopy.entrySet().stream()
                    .filter(x -> x.getValue().equals(containerInv.get(itemC)))
                    .findFirst()
                    .get()
                    .getKey();
                if (invasive.get() || menu.slots.get(start).getItem().isEmpty()) {
                    planMove.add(start);
                    planMove.add(itemC);
                    planMove.add(start);
                    nItemsCopy.put(planInventoryCopy.get(start), nItemsCopy.get(planInventoryCopy.get(start)) - 1);
                    if (nItemsCopy.get(planInventoryCopy.get(start)) == 0) {
                        nItemsCopy.remove(planInventoryCopy.get(start));
                    }
                    planInventoryCopy.remove(start);
                }
            }
        }

        if (debugMode.get()) {
            for (int valuePath : planMove) {
                info("%d", valuePath);
            }
        }

        return planMove;
    }

    private HashMap<Integer, String> getInventoryCopy(AbstractContainerMenu menu, int startPoint) {
        HashMap<Integer, String> output = new HashMap<>();
        int sizeInventory = mc.player.getInventory().getNonEquipmentItems().size();

        for (int i = 0; i < sizeInventory; i++) {
            int value = i + startPoint + (i < 9 ? sizeInventory - 9 : -9);
            output.put(value, AutoGearCommand.getItemKey(menu.slots.get(value).getItem()));
        }

        return output;
    }

    private HashMap<Integer, String> getInventoryCopy(int startPoint, HashMap<Integer, String> inventory) {
        HashMap<Integer, String> output = new HashMap<>();
        int sizeInventory = mc.player.getInventory().getNonEquipmentItems().size();

        for (int val : inventory.keySet()) {
            output.put(val + startPoint + (val < 9 ? sizeInventory - 9 : -9), inventory.get(val));
        }

        return output;
    }
}