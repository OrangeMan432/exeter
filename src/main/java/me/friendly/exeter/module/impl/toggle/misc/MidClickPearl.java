package me.friendly.exeter.module.impl.toggle.misc;

import me.friendly.api.event.Listener;
import me.friendly.exeter.events.InputEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.Property;
import me.friendly.exeter.util.PlayerUtil;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * MidClickPearl. Middle-click throws an ender pearl from anywhere in the hotbar/inventory.
 *
 * Classic 5b5t pearl-clutch helper: no need to scroll to the pearl slot mid-fight. Finds the
 * first pearl (hotbar first, then inventory), silent-swaps to it (server-side carried-item
 * packet), fires the use packet with the current view, swaps back. The view never moves.
 *
 * Requires: pearl present, not already using an item, no screen open (middle-click in a
 * container screen keeps vanilla pick-block).
 */
public class MidClickPearl extends ToggleableModule {
    private final Property<Boolean> silent = new Property<>(true, "Silent", "silent", "s");

    public MidClickPearl() {
        super("MidClickPearl", new String[]{"midclickpearl", "mcp", "pearlclutch"}, ModuleType.MISCELLANEOUS);
        offerProperties(silent);

        this.listeners.add(new Listener<InputEvent>("mid_click_pearl_input") {
            @Override
            public void call(InputEvent event) {
                if (event.getType() != InputEvent.Type.MOUSE_MIDDLE_CLICK) return;

                LocalPlayer player = minecraft.player;
                if (player == null || minecraft.level == null || minecraft.gameMode == null) return;
                if (minecraft.gui.screen() != null) return;
                if (player.isUsingItem()) return;

                int pearlSlot = findPearl(player);
                if (pearlSlot == -1) return;

                int original = player.getInventory().getSelectedSlot();
                boolean swapped = pearlSlot != original && pearlSlot <= 8;

                if (swapped) {
                    if (pearlSlot > 8) {
                        // Inventory pearl: swap into the selected hotbar slot via container click.
                        swapInventoryToHotbar(player, pearlSlot, original);
                    } else {
                        PlayerUtil.swapTo(pearlSlot);
                    }
                }

                // Fire the pearl with the current view.
                minecraft.gameMode.useItem(player, InteractionHand.MAIN_HAND);
                player.swing(InteractionHand.MAIN_HAND);

                if (swapped && silent.getValue()) {
                    PlayerUtil.swapBack();
                } else if (swapped) {
                    player.getInventory().setSelectedSlot(original);
                }
            }
        });
    }

    private static int findPearl(LocalPlayer player) {
        // Hotbar first (0-8), then main inventory (9-35).
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() == Items.ENDER_PEARL) {
                return i;
            }
        }
        return -1;
    }

    private static void swapInventoryToHotbar(LocalPlayer player, int invSlot, int hotbarSlot) {
        var menu = player.containerMenu;
        var inv = player.getInventory();
        net.minecraft.world.inventory.Slot from = null;
        net.minecraft.world.inventory.Slot to = null;
        for (net.minecraft.world.inventory.Slot s : menu.slots) {
            if (s.container != inv) continue;
            int cs = s.getContainerSlot();
            if (cs == invSlot) from = s;
            else if (cs == hotbarSlot) to = s;
        }
        if (from == null || to == null) return;

        menu.clicked(from.index, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, player);
        menu.clicked(to.index, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, player);
        if (!menu.getCarried().isEmpty()) {
            menu.clicked(from.index, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, player);
        }
    }
}
