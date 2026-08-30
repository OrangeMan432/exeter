package me.friendly.exeter.module.impl.toggle.misc;

import me.friendly.api.event.Listener;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

/**
 * AutoEat. Starts eating the best food in the hotbar when hunger drops below a threshold, and
 * keeps eating until a stop level is reached (default: full).
 *
 * How it works on 26.2: food items carry {@link DataComponents#FOOD} (nutrition/saturation)
 * and {@code CONSUMABLE} (consume seconds). The module picks the best edible hotbar stack,
 * swaps it into the selected slot through the stack-size-aware click path if needed, and
 * calls {@code startUsingItem(MAIN_HAND)} — vanilla's consume loop finishes the eating; the
 * module only initiates, and re-initiates if the player released early.
 *
 * Customization:
 *  - Trigger Level   : hunger (1-19) at which eating starts.
 *  - Stop At Level   : hunger at which an in-progress meal is allowed to stop (default 19 = full).
 *  - Prefer Best     : highest nutrition first; off = first edible found.
 *  - Allow Golden    : permit golden apples (usually saved for combat); off = skip them.
 *  - Pause In Combat : don't start eating when health recently dropped (under attack).
 *  - Health Floor    : never start eating below this health (eat-and-run safety).
 *
 * Safety: never eats while any screen is open (chat/inventory), never eats items without a
 * FOOD component, never eats golden apples unless allowed, and never eats while already
 * using an item (respects bows, ender pearls, etc.).
 */
public class AutoEat extends ToggleableModule {
    private final NumberProperty<Integer> triggerLevel = new NumberProperty<>(16, 1, 19, "Trigger Level", "trigger", "at");
    private final NumberProperty<Integer> stopLevel = new NumberProperty<>(19, 1, 19, "Stop At Level", "stopat", "until");
    private final Property<Boolean> preferBest = new Property<>(true, "Prefer Best", "preferbest", "best");
    private final Property<Boolean> allowGolden = new Property<>(false, "Allow Golden", "allowgolden", "golden");
    private final Property<Boolean> pauseInCombat = new Property<>(true, "Pause In Combat", "pausecombat", "combat");
    private final NumberProperty<Float> healthFloor = new NumberProperty<>(6.0f, 0.0f, 20.0f, "Health Floor", "healthfloor");

    private int cooldown = 0;
    private float lastHealth = -1f;

    public AutoEat() {
        super("AutoEat", new String[]{"autoeat", "eat"}, ModuleType.MISCELLANEOUS);
        offerProperties(triggerLevel, stopLevel, preferBest, allowGolden, pauseInCombat, healthFloor);

        this.listeners.add(new Listener<TickEvent>("auto_eat_tick") {
            @Override
            public void call(TickEvent event) {
                LocalPlayer player = minecraft.player;
                if (player == null || minecraft.level == null) return;
                if (minecraft.gui.screen() != null) return;

                if (cooldown > 0) {
                    cooldown--;
                    return;
                }

                FoodData food = player.getFoodData();
                int level = food.getFoodLevel();
                int stopAt = Math.max(1, Math.min(19, stopLevel.getValue()));

                // Combat detection: a recent health drop means "under attack".
                float health = player.getHealth();
                boolean inCombat = pauseInCombat.getValue()
                        && lastHealth >= 0f && health < lastHealth - 0.01f;
                lastHealth = health;
                if (inCombat) return;
                if (health <= healthFloor.getValue()) return; // eat-and-run floor

                // Already eating? Let vanilla finish unless we've passed the stop level.
                if (player.isUsingItem()) {
                    if (level >= stopAt && isEddible(player.getUseItem())) player.stopUsingItem();
                    return;
                }

                if (level >= stopAt) return; // not hungry enough

                int hotbar = player.getInventory().getSelectedSlot();
                int bestSlot = findFoodSlot(player, preferBest.getValue(), allowGolden.getValue());
                if (bestSlot == -1) return; // no food in hotbar

                // Swap the chosen food into the selected slot if needed (one server round-trip).
                if (bestSlot != hotbar) {
                    swapHotbarSlots(player, bestSlot, hotbar);
                    cooldown = 2;
                    return;
                }

                // Start eating; vanilla consume loop takes over.
                player.startUsingItem(InteractionHand.MAIN_HAND);
                cooldown = 1;
            }
        });
    }

    private static boolean isEddible(ItemStack stack) {
        return !stack.isEmpty() && stack.get(DataComponents.FOOD) != null;
    }

    /**
     * Finds the hotbar slot (0-8) holding edible food. With {@code best}, returns the
     * highest-nutrition stack; otherwise the first edible stack. Skips golden apples
     * unless {@code allowGolden}.
     */
    private static int findFoodSlot(LocalPlayer player, boolean best, boolean allowGolden) {
        int bestSlot = -1;
        int bestNutrition = -1;
        for (int i = 0; i <= 8; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;

            FoodProperties food = stack.get(DataComponents.FOOD);
            if (food == null) continue;
            if (food.nutrition() <= 0) continue; // only foods that actually restore hunger
            if (!allowGolden && isGoldenApple(stack)) continue;

            if (!best) return i;
            if (food.nutrition() > bestNutrition) {
                bestNutrition = food.nutrition();
                bestSlot = i;
            }
        }
        return bestSlot;
    }

    private static boolean isGoldenApple(ItemStack stack) {
        var key = stack.getItem().builtInRegistryHolder().unwrapKey();
        return key.isPresent()
                && key.get().identifier().getPath().contains("golden_apple");
    }

    /**
     * Swaps two hotbar slots through the stack-size-aware click path (PICKUP twice:
     * pick up source, drop onto target; returns the leftover to the source slot if the
     * stacks didn't merge).
     */
    private static void swapHotbarSlots(LocalPlayer player, int from, int to) {
        var menu = player.containerMenu;
        var inv = player.getInventory();
        net.minecraft.world.inventory.Slot slotFrom = null;
        net.minecraft.world.inventory.Slot slotTo = null;
        for (net.minecraft.world.inventory.Slot s : menu.slots) {
            if (s.container != inv) continue;
            int cs = s.getContainerSlot();
            if (cs == from) slotFrom = s;
            else if (cs == to) slotTo = s;
        }
        if (slotFrom == null || slotTo == null) return;

        menu.clicked(slotFrom.index, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, player);
        menu.clicked(slotTo.index, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, player);
        if (!menu.getCarried().isEmpty()) {
            menu.clicked(slotFrom.index, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, player);
        }
    }
}
