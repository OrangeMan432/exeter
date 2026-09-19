package me.earth.earthhack.impl.modules.player.keypearl;

import me.earth.earthhack.impl.event.events.misc.TickEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.RayTraceResult;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

final class ListenerTick extends ModuleListener<KeyPearl, TickEvent>
{
    public ListenerTick(KeyPearl module)
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

        if (module.mode.getValue() == KeyPearl.Mode.MIDDLECLICK)
        {
            if (Mouse.isButtonDown(2))
            {
                if (!module.clicked)
                {
                    throwPearl();
                }

                module.clicked = true;
            }
            else
            {
                module.clicked = false;
            }
        }
        else if (module.getBind().getKey() != -1
            && Keyboard.isKeyDown(module.getBind().getKey()))
        {
            throwPearl();
        }
    }

    private void throwPearl()
    {
        if (module.noPlayerTrace.getValue()
            && mc.objectMouseOver != null
            && mc.objectMouseOver.typeOfHit == RayTraceResult.Type.ENTITY
            && mc.objectMouseOver.entityHit instanceof EntityPlayer)
        {
            return;
        }

        int pearlSlot = findPearl();
        boolean offhand = mc.player.getHeldItemOffhand().getItem()
            == Items.ENDER_PEARL;
        if (pearlSlot == -1 && !offhand)
        {
            return;
        }

        int oldSlot = mc.player.inventory.currentItem;
        if (!offhand)
        {
            mc.player.inventory.currentItem = pearlSlot;
            mc.playerController.updateController();
        }

        mc.playerController.processRightClick(
            mc.player,
            mc.world,
            offhand ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND);

        if (!offhand)
        {
            mc.player.inventory.currentItem = oldSlot;
            mc.playerController.updateController();
        }
    }

    private int findPearl()
    {
        for (int i = 0; i < 9; i++)
        {
            if (mc.player.inventory.getStackInSlot(i).getItem()
                    instanceof ItemEnderPearl)
            {
                return i;
            }
        }

        return -1;
    }
}
