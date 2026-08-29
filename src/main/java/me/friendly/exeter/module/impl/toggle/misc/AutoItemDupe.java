package me.friendly.exeter.module.impl.toggle.misc;

import me.friendly.api.event.Listener;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.Property;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.ContainerInput;

/**
 * Auto item dupe, ported in spirit from Lambda's 5bDupes (ToxicAven) AutoItemDupe module.
 *
 * On 5b5t the dupe works by throwing the held stack out of the inventory and then firing the
 * recipe-book "place recipe" packet (wooden_button), which copies the thrown stack back into your
 * inventory. This module performs the throw (the portable part). The recipe-place trigger is NOT
 * wired: in Minecraft 26.2 that packet is {@code ServerboundPlaceRecipePacket(int, RecipeDisplayId,
 * boolean)} and {@code RecipeDisplayId} is an int index only resolvable through internal
 * recipe-manager state, so it cannot be built from public APIs here. Once a display-id lookup is
 * available, send {@code ServerboundPlaceRecipePacket(containerId, displayId, false)} right after
 * the throw to complete the 5b5t dupe. Requires wooden planks anywhere in your inventory.
 */
public class AutoItemDupe extends ToggleableModule {
    private enum Phase {
        NONE, DROP, PICKUP
    }

    private final Property<Boolean> cancelGui = new Property<>(false, "Cancel GUI", "cancelgui");

    private Phase phase = Phase.NONE;
    private long lastClick = 0L;

    public AutoItemDupe() {
        super("AutoItemDupe", new String[]{"autoitemdupe", "aidd"}, ModuleType.MISCELLANEOUS);
        offerProperties(cancelGui);

        this.listeners.add(new Listener<TickEvent>("auto_item_dupe_tick") {
            @Override
            public void call(TickEvent event) {
                if (minecraft.player == null) return;
                if (phase == Phase.NONE) return;

                if (phase == Phase.DROP) {
                    if (minecraft.player.getInventory().getSelectedItem().isEmpty()) {
                        phase = Phase.NONE;
                        return;
                    }
                    // Throw the currently held stack out of the inventory.
                    minecraft.player.containerMenu.clicked(
                            minecraft.player.getInventory().getSelectedSlot(), 1, ContainerInput.THROW, minecraft.player);
                    phase = Phase.PICKUP;
                    lastClick = System.currentTimeMillis();
                } else if (phase == Phase.PICKUP) {
                    if (System.currentTimeMillis() - lastClick >= 300L) phase = Phase.NONE;
                }
            }
        });
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        if (minecraft.player == null) {
            phase = Phase.NONE;
            return;
        }
        boolean hasPlanks = false;
        for (int i = 0; i < 36; i++) {
            ItemStack s = minecraft.player.getInventory().getItem(i);
            if (!s.isEmpty() && s.getItem() instanceof BlockItem bi
                    && bi.getBlock().defaultBlockState().is(BlockTags.PLANKS)) {
                hasPlanks = true;
                break;
            }
        }
        phase = hasPlanks ? Phase.DROP : Phase.NONE;
    }

    @Override
    protected void onDisable() {
        super.onDisable();
        if (cancelGui.getValue() && minecraft.gui.screen() != null) minecraft.gui.setScreen(null);
    }
}
