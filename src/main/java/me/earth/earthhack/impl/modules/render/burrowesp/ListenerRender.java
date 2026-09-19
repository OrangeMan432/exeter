package me.earth.earthhack.impl.modules.render.burrowesp;

import me.earth.earthhack.impl.event.events.render.Render3DEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.managers.Managers;
import me.earth.earthhack.impl.util.render.RenderUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;

import java.awt.*;

final class ListenerRender extends ModuleListener<BurrowESP, Render3DEvent>
{
    public ListenerRender(BurrowESP module)
    {
        super(module, Render3DEvent.class);
    }

    @Override
    public void invoke(Render3DEvent event)
    {
        if (mc.world == null || mc.player == null)
        {
            return;
        }

        for (EntityPlayer player : mc.world.playerEntities)
        {
            if (player == null || player.isDead)
            {
                continue;
            }

            BlockPos pos = new BlockPos(
                player.posX, player.posY, player.posZ);
            if (mc.world.getBlockState(pos).getBlock() == Blocks.AIR)
            {
                continue;
            }

            Color color = null;
            if (player == mc.player && module.showSelf.getValue())
            {
                color = module.selfColor.getValue();
            }
            else if (Managers.FRIENDS.contains(player)
                && module.showFriends.getValue())
            {
                color = module.friendColor.getValue();
            }
            else if (player != mc.player
                && module.showEnemies.getValue())
            {
                color = module.enemyColor.getValue();
            }

            if (color != null)
            {
                RenderUtil.renderBox(pos,
                                     color,
                                     module.height.getValue());
            }
        }
    }
}
