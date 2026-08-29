package me.friendly.exeter.module.impl.toggle.misc;

import me.friendly.api.event.Listener;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Items;

/**
 * AutoTotem. Keeps a Totem of Undying in your offhand. Whenever the offhand is not a totem, it
 * finds a totem in the main inventory / hotbar and moves it into the offhand using the stack-size
 * aware click path. Skips a tick if you are already holding an item in the cursor so it never
 * disrupts manual inventory work.
 */
public class AutoTotem extends ToggleableModule {
    public AutoTotem() {
        super("AutoTotem", new String[]{"autototem", "totem"}, ModuleType.MISCELLANEOUS);

        this.listeners.add(new Listener<TickEvent>("auto_totem_tick") {
            @Override
            public void call(TickEvent event) {
                if (minecraft.player == null) return;
                Inventory inv = minecraft.player.getInventory();
                if (inv.getItem(40).is(Items.TOTEM_OF_UNDYING)) return;

                int src = -1;
                for (int i = 0; i <= 35; i++) {
                    if (inv.getItem(i).is(Items.TOTEM_OF_UNDYING)) {
                        src = i;
                        break;
                    }
                }
                if (src == -1) return;

                AbstractContainerMenu menu = minecraft.player.containerMenu;
                if (!menu.getCarried().isEmpty()) return;

                Slot offhandSlot = null;
                Slot sourceSlot = null;
                for (Slot s : menu.slots) {
                    if (s.container == inv) {
                        if (s.getContainerSlot() == 40) offhandSlot = s;
                        else if (s.getContainerSlot() == src) sourceSlot = s;
                    }
                }
                if (offhandSlot == null || sourceSlot == null) return;

                menu.clicked(sourceSlot.index, 0, ContainerInput.PICKUP, minecraft.player);
                menu.clicked(offhandSlot.index, 0, ContainerInput.PICKUP, minecraft.player);
            }
        });
    }
}
