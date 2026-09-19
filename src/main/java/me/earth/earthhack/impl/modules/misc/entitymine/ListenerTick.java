package me.earth.earthhack.impl.modules.misc.entitymine;

import me.earth.earthhack.impl.event.events.misc.TickEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;

final class ListenerTick extends ModuleListener<EntityMine, TickEvent>
{
    boolean focus;

    public ListenerTick(EntityMine module)
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

        for (Entity entity : mc.world.loadedEntityList)
        {
            if (entity instanceof EntityLivingBase
                && entity != mc.player
                && !entity.isDead)
            {
                RayTraceResult bypass =
                    ((EntityLivingBase) entity).rayTrace(
                        6.0, mc.getRenderPartialTicks());
                if (bypass != null
                    && focus
                    && bypass.typeOfHit == RayTraceResult.Type.BLOCK)
                {
                    BlockPos pos = bypass.getBlockPos();
                    if (mc.gameSettings.keyBindAttack.isKeyDown())
                    {
                        mc.playerController.onPlayerDamageBlock(
                            pos, EnumFacing.UP);
                    }
                }
            }
        }

        RayTraceResult normal = mc.objectMouseOver;
        focus = normal != null
            && normal.typeOfHit == RayTraceResult.Type.ENTITY;
    }
}
