package me.earth.earthhack.impl.modules.misc.framedupe;

import me.earth.earthhack.impl.event.events.misc.TickEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.init.Items;
import net.minecraft.item.ItemShulkerBox;
import net.minecraft.util.EnumHand;

final class ListenerTick extends ModuleListener<FrameDupe, TickEvent>
{
    public ListenerTick(FrameDupe module)
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

        if (module.shulkersOnly.getValue())
        {
            int shulker = findShulker();
            if (shulker != -1)
            {
                mc.player.inventory.currentItem = shulker;
            }
        }

        for (Entity entity : mc.world.loadedEntityList)
        {
            if (!(entity instanceof EntityItemFrame)
                || mc.player.getDistance(entity)
                    > module.range.getValue())
            {
                continue;
            }

            EntityItemFrame frame = (EntityItemFrame) entity;
            if (module.timeout++ < module.ticks.getValue())
            {
                continue;
            }

            if (frame.getDisplayedItem().getItem() == Items.AIR
                && !mc.player.getHeldItemMainhand().isEmpty())
            {
                mc.playerController.interactWithEntity(
                    mc.player, entity, EnumHand.MAIN_HAND);
            }

            if (frame.getDisplayedItem().getItem() != Items.AIR)
            {
                for (int i = 0; i < module.turns.getValue(); i++)
                {
                    mc.playerController.interactWithEntity(
                        mc.player, entity, EnumHand.MAIN_HAND);
                }

                mc.playerController.attackEntity(mc.player, entity);
                module.timeout = 0;
            }
        }
    }

    private int findShulker()
    {
        for (int i = 0; i < 9; i++)
        {
            if (mc.player.inventory.getStackInSlot(i).getItem()
                    instanceof ItemShulkerBox)
            {
                return i;
            }
        }

        return -1;
    }
}
