package me.earth.earthhack.impl.modules.misc.manualdupe;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.impl.util.client.SimpleData;
import net.minecraft.client.gui.inventory.GuiScreenHorseInventory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.AbstractChestHorse;
import net.minecraft.network.play.client.CPacketUseEntity;
import net.minecraft.util.EnumHand;

/**
 * Donkey dupe interact while riding with chest open.
 * Ported from SalHack (ManualDupe).
 */
public class ManualDupe extends Module
{
    public ManualDupe()
    {
        super("ManualDupe", Category.Misc);
        this.setData(new SimpleData(this,
            "Chest-horse dupe interact, ride first."));
    }

    @Override
    protected void onEnable()
    {
        if (mc.world == null || mc.player == null)
        {
            disable();
            return;
        }

        Entity best = null;
        double bestDist = Double.MAX_VALUE;
        for (Entity entity : mc.world.loadedEntityList)
        {
            if (!(entity instanceof AbstractChestHorse))
            {
                continue;
            }

            AbstractChestHorse horse = (AbstractChestHorse) entity;
            if (horse.isChild() || !horse.isTame())
            {
                continue;
            }

            double dist = mc.player.getDistance(entity);
            if (dist < bestDist)
            {
                bestDist = dist;
                best = entity;
            }
        }

        if (mc.currentScreen instanceof GuiScreenHorseInventory
            && best instanceof AbstractChestHorse
            && mc.player.getRidingEntity() != null
            && ((AbstractChestHorse) best).hasChest())
        {
            mc.player.connection.sendPacket(new CPacketUseEntity(
                best, EnumHand.MAIN_HAND, best.getPositionVector()));
        }

        disable();
    }
}
