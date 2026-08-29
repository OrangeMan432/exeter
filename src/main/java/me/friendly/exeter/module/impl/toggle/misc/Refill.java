package me.friendly.exeter.module.impl.toggle.misc;

import me.friendly.api.event.Listener;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;

/**
 * Refill. Keeps the hotbar filled from the main inventory. Whenever a hotbar slot is empty it picks
 * up a stack from the main inventory and drops it into that slot. All movement goes through
 * {@code AbstractContainerMenu.clicked(...)} with {@link ContainerInput#PICKUP}, which simulates the
 * click the way the server does and respects the server-reported max stack size, so overstacked
 * (127) items are moved correctly instead of silently dropped or corrupted.
 *
 * Only runs while no container screen is open (it operates on the player's own inventory).
 */
public class Refill extends ToggleableModule {
    private int cooldown = 0;

    public Refill() {
        super("Refill", new String[]{"refill", "invrefill"}, ModuleType.MISCELLANEOUS);

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

                Slot target = null;
                for (Slot slot : menu.slots) {
                    if (slot.container == inv && slot.getContainerSlot() <= 8 && slot.getItem().isEmpty()) {
                        target = slot;
                        break;
                    }
                }
                if (target == null) return;

                Slot source = null;
                for (Slot slot : menu.slots) {
                    if (slot.container == inv && slot.getContainerSlot() >= 9 && slot.getContainerSlot() <= 35
                            && !slot.getItem().isEmpty()) {
                        source = slot;
                        break;
                    }
                }
                if (source == null) return;

                minecraft.player.containerMenu.clicked(source.index, 0, ContainerInput.PICKUP, minecraft.player);
                minecraft.player.containerMenu.clicked(target.index, 0, ContainerInput.PICKUP, minecraft.player);
                cooldown = 1;
            }
        });
    }
}
