package me.earth.earthhack.impl.modules.misc.kitdelete;

import me.earth.earthhack.impl.event.events.misc.TickEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import org.lwjgl.input.Keyboard;

final class ListenerTick extends ModuleListener<KitDelete, TickEvent>
{
    public ListenerTick(KitDelete module)
    {
        super(module, TickEvent.class);
    }

    @Override
    public void invoke(TickEvent event)
    {
        if (mc.world == null
            || mc.player == null
            || module.deleteKey.getValue().getKey() == -1)
        {
            return;
        }

        if (mc.currentScreen instanceof GuiContainer
            && Keyboard.isKeyDown(module.deleteKey.getValue().getKey()))
        {
            Slot slot = ((GuiContainer) mc.currentScreen).getSlotUnderMouse();
            if (slot != null && !module.keyDown)
            {
                mc.player.sendChatMessage("/deleteukit "
                    + slot.getStack().getDisplayName());
                module.keyDown = true;
            }
        }
        else if (module.keyDown)
        {
            module.keyDown = false;
        }
    }
}
