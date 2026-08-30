package me.friendly.exeter.module.impl.toggle.combat;

import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Items;

public class AutoTotem extends ToggleableModule {

    private int tickDelay;

    private final Listener<TickEvent> tickListener = new Listener<TickEvent>("autototem_tick") {
        @Override
        public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            onTick();
        }
    };

    public AutoTotem() {
        super("AutoTotem", new String[]{"autototem", "auto-totem"}, 0xFF0000, ModuleType.COMBAT);
        this.listeners.add(tickListener);
    }

    private void onTick() {
        if (minecraft.player == null || minecraft.gameMode == null) return;
        if (minecraft.player.containerMenu != null && minecraft.player.containerMenu != minecraft.player.inventoryMenu) return;

        if (minecraft.player.getOffhandItem().getItem() == Items.TOTEM_OF_UNDYING) return;

        if (++tickDelay < 1) return;
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
