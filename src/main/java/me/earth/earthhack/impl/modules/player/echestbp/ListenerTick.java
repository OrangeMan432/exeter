package me.earth.earthhack.impl.modules.player.echestbp;

import me.earth.earthhack.impl.event.events.misc.TickEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.InventoryBasic;

final class ListenerTick extends ModuleListener<EchestBP, TickEvent>
{
    public ListenerTick(EchestBP module)
    {
        super(module, TickEvent.class);
    }

    @Override
    public void invoke(TickEvent event)
    {
        if (mc.player == null)
        {
            return;
        }

        if (mc.currentScreen instanceof GuiContainer
            && ((GuiContainer) mc.currentScreen).inventorySlots
                instanceof ContainerChest
            && ((ContainerChest) ((GuiContainer) mc.currentScreen)
                .inventorySlots).getLowerChestInventory()
                    instanceof InventoryBasic
            && ((InventoryBasic) ((ContainerChest) ((GuiContainer)
                mc.currentScreen).inventorySlots)
                    .getLowerChestInventory())
                .getName().equalsIgnoreCase("Ender Chest"))
        {
            module.screen = mc.currentScreen;
            mc.currentScreen = null;
        }
    }
}
