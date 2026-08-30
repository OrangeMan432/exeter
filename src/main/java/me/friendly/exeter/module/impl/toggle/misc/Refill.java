package me.friendly.exeter.module.impl.toggle.misc;

import java.util.HashMap;
import java.util.Map;

import me.friendly.api.event.Listener;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Refill. Keeps the hotbar filled from the main inventory.
 *
 * Each hotbar slot is remembered by the item it holds; when that slot empties, the module
 * restocks it with the <b>same item</b> from the main inventory, so a thrown sword gets
 * replaced by another sword instead of whatever stack happens to be first. Slots the player
 * deliberately emptied (dropped Q, etc.) are restocked too — clear memory for a slot by
 * holding a different item in it or using the forget command behavior below.
 *
 * All movement goes through {@code AbstractContainerMenu.clicked(...)} with
 * {@link ContainerInput#PICKUP}, which simulates the click the way the server does and
 * respects the server-reported max stack size, so overstacked (127) items are moved
 * correctly instead of silently dropped or corrupted.
 *
 * Only runs while no container screen is open (it operates on the player's own inventory).
 */
public class Refill extends ToggleableModule {
    /** Hotbar slot index -> last non-empty item seen in that slot. */
    private final Map<Integer, Item> slotMemory = new HashMap<>();

    private final Property<Boolean> sameItemOnly = new Property<>(true, "Same Item Only", "sameitem", "strict");
    private final Property<Boolean> topOffStacks = new Property<>(false, "Top Off Stacks", "topoff", "topup");
    private final NumberProperty<Integer> cooldownTicks = new NumberProperty<>(1, 1, 20, "Cooldown Ticks", "cooldown", "cd");

    private int cooldown = 0;

    public Refill() {
        super("Refill", new String[]{"refill", "invrefill"}, ModuleType.MISCELLANEOUS);
        offerProperties(sameItemOnly, topOffStacks, cooldownTicks);

        this.listeners.add(new Listener<TickEvent>("refill_tick") {
            @Override
            public void call(TickEvent event) {
                if (minecraft.player == null || minecraft.level == null) return;
                if (minecraft.gui.screen() != null) return;

                if (cooldown > 0) {
                    cooldown--;
                    return;
                }

                Inventory inv = minecraft.player.getInventory();
                AbstractContainerMenu menu = minecraft.player.containerMenu;

                // 1. Update memory: remember the item living in each hotbar slot right now.
                for (Slot slot : menu.slots) {
                    if (slot.container != inv) continue;
                    int containerSlot = slot.getContainerSlot();
                    if (containerSlot > 8) continue;
                    ItemStack stack = slot.getItem();
                    if (!stack.isEmpty()) {
                        slotMemory.put(containerSlot, stack.getItem());
                    }
                }

                // 2. Find a hotbar slot that needs restocking.
                Slot target = null;
                Item wanted = null;
                for (Slot slot : menu.slots) {
                    if (slot.container != inv) continue;
                    int containerSlot = slot.getContainerSlot();
                    if (containerSlot > 8) continue;

                    ItemStack stack = slot.getItem();
                    if (stack.isEmpty()) {
                        Item remembered = slotMemory.get(containerSlot);
                        if (remembered != null) {
                            target = slot;
                            wanted = remembered;
                            break;
                        }
                        // Never-seen slot: leave it alone, the player chose to keep it empty.
                        continue;
                    }

                    // Top-off: stack present but below max, and something in the main
                    // inventory matches it.
                    if (topOffStacks.getValue() && wanted == null && stack.getCount() < stack.getMaxStackSize()) {
                        Slot match = findMatchingSource(menu, inv, stack.getItem());
                        if (match != null) {
                            target = slot;
                            wanted = stack.getItem();
                            break;
                        }
                    }
                }

                if (target == null || wanted == null) return;

                // 3. Restock with the same item type.
                Slot source;
                if (sameItemOnly.getValue()) {
                    source = findMatchingSource(menu, inv, wanted);
                    if (source == null) return; // nothing of that item left; don't grab junk
                } else {
                    source = findAnySource(menu, inv);
                    if (source == null) return;
                }

                minecraft.player.containerMenu.clicked(source.index, 0, ContainerInput.PICKUP, minecraft.player);
                minecraft.player.containerMenu.clicked(target.index, 0, ContainerInput.PICKUP, minecraft.player);
                cooldown = Math.max(1, cooldownTicks.getValue());
            }
        });
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        slotMemory.clear();
        cooldown = 0;
    }

    @Override
    protected void onDisable() {
        super.onDisable();
        slotMemory.clear();
    }

    /**
     * Finds a main-inventory (slots 9-35) stack of exactly {@code item}.
     */
    private Slot findMatchingSource(AbstractContainerMenu menu, Inventory inv, Item item) {
        for (Slot slot : menu.slots) {
            if (slot.container != inv) continue;
            int containerSlot = slot.getContainerSlot();
            if (containerSlot < 9 || containerSlot > 35) continue;
            ItemStack stack = slot.getItem();
            if (!stack.isEmpty() && stack.getItem() == item) {
                return slot;
            }
        }
        return null;
    }

    /**
     * Finds any non-empty main-inventory (slots 9-35) stack. Used only when Same Item Only
     * is off — the legacy "whatever is next" behavior.
     */
    private Slot findAnySource(AbstractContainerMenu menu, Inventory inv) {
        for (Slot slot : menu.slots) {
            if (slot.container != inv) continue;
            int containerSlot = slot.getContainerSlot();
            if (containerSlot < 9 || containerSlot > 35) continue;
            if (!slot.getItem().isEmpty()) {
                return slot;
            }
        }
        return null;
    }
}
