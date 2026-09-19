package me.earth.earthhack.impl.modules.player.fastexp;

import me.earth.earthhack.impl.event.events.misc.TickEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.init.Items;

final class ListenerTick extends ModuleListener<FastExp, TickEvent>
{
    public ListenerTick(FastExp module)
    {
        super(module, TickEvent.class);
    }

    @Override
    public void invoke(TickEvent event)
    {
        if (mc.world == null || mc.player == null)
        {
            return;
        }

        if (module.autoSwitch.getValue()
            && mc.player.getHeldItemMainhand().getItem()
                != Items.EXPERIENCE_BOTTLE)
        {
            int slot = findXp();
            if (slot == -1)
            {
                if (module.autoDisable.getValue())
                {
                    module.disable();
                }

                return;
            }

            mc.player.inventory.currentItem = slot;
        }

        if (module.autoThrow.getValue()
            && mc.player.getHeldItemMainhand().getItem()
                == Items.EXPERIENCE_BOTTLE)
        {
            mc.rightClickMouse();
        }
    }

    private int findXp()
    {
        for (int i = 0; i < 9; i++)
        {
            if (mc.player.inventory.getStackInSlot(i).getItem()
                == Items.EXPERIENCE_BOTTLE)
            {
                return i;
            }
        }

        return -1;
    }
}
