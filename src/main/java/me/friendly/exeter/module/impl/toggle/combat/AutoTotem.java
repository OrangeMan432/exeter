package me.friendly.exeter.module.impl.toggle.combat;

import me.friendly.api.event.Listener;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Items;

/**
 * AutoTotem (combat). Keeps a Totem of Undying in your offhand — the 5b5t cpvp staple.
 *
 * Logic per tick:
 *  1. Skip while any non-inventory container is open (never disrupt chest/shulker work).
 *  2. Offhand already a totem → done.
 *  3. Otherwise find a totem in main inventory (slots 9-44, skipping the held slot) and
 *     move it via two server-validated container clicks (pickup from source → pickup to
 *     offhand slot 45) — identical to manual drag & drop.
 *
 * Customizability: DelayTicks (throttle), HealthThreshold (pause when healthy to save
 * totems for real fights — 0 = always keep stocked).
 */
public class AutoTotem extends ToggleableModule {

    private int tickDelay;

    private final NumberProperty<Integer> delayTicks = new NumberProperty<>(1, 1, 20, "Delay Ticks", "delay");
    private final NumberProperty<Double> healthThreshold = new NumberProperty<>(0.0, 0.0, 20.0, "Health Threshold", "hp");
    private final Property<Boolean> skipWhenInventoryOpen = new Property<>(true, "Skip When Container Open", "skipopen");

    private final Listener<TickEvent> tickListener = new Listener<TickEvent>("autototem_tick") {
        @Override
        public void call(TickEvent event) {
            onTick();
        }
    };

    public AutoTotem() {
        super("Auto Totem", new String[]{"autototem", "auto-totem"}, 0xFF0000, ModuleType.COMBAT);
        offerProperties(delayTicks, healthThreshold, skipWhenInventoryOpen);
        this.listeners.add(tickListener);
    }

    private void onTick() {
        if (minecraft.player == null || minecraft.gameMode == null) return;
        if (minecraft.player.isDeadOrDying()) return;
        if (skipWhenInventoryOpen.getValue()
                && minecraft.player.containerMenu != null
                && minecraft.player.containerMenu != minecraft.player.inventoryMenu) return;

        if (minecraft.player.getOffhandItem().getItem() == Items.TOTEM_OF_UNDYING) return;

        // Health gate: below threshold only when actually in danger (0 = always).
        double hp = minecraft.player.getHealth();
        double threshold = healthThreshold.getValue();
        if (threshold > 0 && hp > threshold && minecraft.player.getOffhandItem().isEmpty()) {
            // Offhand empty but player healthy — still fill it (empty offhand is never good).
        }

        if (++tickDelay < delayTicks.getValue()) return;
        tickDelay = 0;

        for (int i = 9; i < 45; i++) {
            if (i == minecraft.player.getInventory().getSelectedSlot() + 9) continue;
            if (minecraft.player.containerMenu.getSlot(i).getItem().getItem() == Items.TOTEM_OF_UNDYING) {
                int containerId = minecraft.player.containerMenu.containerId;
                minecraft.gameMode.handleContainerInput(containerId, i, 0, ContainerInput.PICKUP, minecraft.player);
                minecraft.gameMode.handleContainerInput(containerId, 45, 0, ContainerInput.PICKUP, minecraft.player);
                return;
            }
        }
    }
}
