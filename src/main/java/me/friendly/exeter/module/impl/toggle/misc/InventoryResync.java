package me.friendly.exeter.module.impl.toggle.misc;

import me.friendly.api.event.Listener;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.Property;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Inventory resync. Clears client-side ghost items after container-desync dupes.
 *
 * After a dupe sequence (ShulkerDupe, AutoItemDupe) the client inventory can hold stacks the
 * server doesn't know about — "ghost items". They look real client-side until the next
 * server-confirmed container update, then snap away (or worse, desync clicks). This module
 * forces the client to adopt the server's authoritative view.
 *
 * Direction matters: {@code AbstractContainerMenu.broadcastFullState()} pushes the CLIENT's
 * slot list to the server as authoritative — that would *spread* ghosts, not clear them. The
 * correct direction is to make the server re-send its state:
 *
 * - With a container screen open: close it. The vanilla close path notifies the server, which
 *   reconciles the container; the next open pulls fresh slot data.
 * - Without a screen (player inventory): send a QUICK_MOVE container click on the offhand
 *   slot (container slot 45). For empty hands it's a server-side no-op, but it forces a
 *   {@code ServerboundContainerClickPacket} round-trip carrying our stateId — a mismatched
 *   stateId makes the server respond with its authoritative slot list, and the ghosts vanish.
 *
 * Modes:
 * - Auto: after a dupe module's state machine goes idle, resync after the configured delay.
 * - Manual: toggle-triggered resync (toggling the module off performs one resync).
 */
public class InventoryResync extends ToggleableModule {
    private final Property<Boolean> auto = new Property<>(true, "Auto", "auto");
    private final Property<Integer> delayTicks = new Property<>(10, "Delay Ticks", "delay");

    private int pendingTicks = -1;

    public InventoryResync() {
        super("InventoryResync", new String[]{"invresync", "resync"}, ModuleType.MISCELLANEOUS);
        offerProperties(auto, delayTicks);

        this.listeners.add(new Listener<TickEvent>("inv_resync_tick") {
            @Override
            public void call(TickEvent event) {
                if (minecraft.player == null) {
                    pendingTicks = -1;
                    return;
                }

                if (pendingTicks >= 0) {
                    if (--pendingTicks > 0) return;
                    pendingTicks = -1;
                    resync();
                    return;
                }

                if (!auto.getValue()) return;

                // Auto trigger: a dupe module just finished a sequence (ShulkerDupe /
                // AutoItemDupe are registered before this module). When neither is mid-phase
                // and a carried stack exists the server may not know about, schedule a resync
                // after the configured delay.
                if (isDupeActive()) return;

                if (minecraft.gui.screen() instanceof AbstractContainerScreen<?>) {
                    AbstractContainerMenu menu = minecraft.player.containerMenu;
                    if (!menu.getCarried().isEmpty()) {
                        pendingTicks = Math.max(1, delayTicks.getValue());
                    }
                }
            }
        });
    }

    private static boolean isDupeActive() {
        var moduleManager = me.friendly.exeter.core.Exeter.getInstance().getModuleManager();
        // Mid-sequence dupes carry state; a resync during a dupe would break it.
        var shulker = moduleManager.getModuleByAlias("shulkerdupe");
        var itemDupe = moduleManager.getModuleByAlias("autoitemdupe");
        return (shulker instanceof ToggleableModule t && t.isRunning())
                || (itemDupe instanceof ToggleableModule t2 && t2.isRunning());
    }

    /**
     * Forces the server's authoritative inventory state to replace the client's.
     *
     * With a container screen open: close it — the server reconciles the container and
     * pushes its authoritative slots on the next open.
     * Without a screen: poke the player inventory with a no-op QUICK_MOVE round-trip.
     */
    private void resync() {
        if (minecraft.player == null) return;

        if (minecraft.gui.screen() instanceof AbstractContainerScreen<?>) {
            // Closing the screen is the clean resync: the server reconciles the container
            // and pushes its authoritative slots on the next open.
            minecraft.gui.setScreen(null);
            return;
        }

        // No screen open: poke the player inventory with a no-op click round-trip.
        var menu = minecraft.player.containerMenu;
        if (!menu.getCarried().isEmpty()) {
            // Carried stack the server may not know about: drop it client-side first
            // (client-only view), then the poke round-trip replaces our slot view.
            menu.setCarried(net.minecraft.world.item.ItemStack.EMPTY);
        }
        menu.clicked(45, 0, net.minecraft.world.inventory.ContainerInput.QUICK_MOVE, minecraft.player);
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        pendingTicks = -1;
    }

    @Override
    protected void onDisable() {
        super.onDisable();
        // Manual trigger: toggling off performs one immediate resync.
        pendingTicks = -1;
        if (minecraft.player != null) resync();
    }
}
