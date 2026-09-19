package me.earth.earthhack.impl.modules.player.automend;

import me.earth.earthhack.impl.event.events.misc.TickEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

final class ListenerTick extends ModuleListener<AutoMend, TickEvent>
{
    public ListenerTick(AutoMend module)
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

        boolean needsMend = false;
        for (ItemStack stack : mc.player.inventory.armorInventory)
        {
            if (stack != null
                && !stack.isEmpty()
                && stack.isItemDamaged()
                && durabilityPercent(stack) < module.threshold.getValue())
            {
                needsMend = true;
                break;
            }
        }

        if (!needsMend)
        {
            if (module.mending
                && module.initSlot != -1
                && module.initSlot != mc.player.inventory.currentItem)
            {
                mc.player.inventory.currentItem = module.initSlot;
            }

            module.mending = false;
            return;
        }

        module.mending = true;
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
            mc.rightClickDelayTimer = 0;
            mc.rightClickMouse();
        }
    }

    private int durabilityPercent(ItemStack stack)
    {
        if (stack.getMaxDamage() <= 0)
        {
            return 100;
        }

        return (stack.getMaxDamage() - stack.getItemDamage()) * 100
            / stack.getMaxDamage();
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
