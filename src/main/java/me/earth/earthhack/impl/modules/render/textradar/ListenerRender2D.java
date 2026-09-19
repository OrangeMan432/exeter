package me.earth.earthhack.impl.modules.render.textradar;

import me.earth.earthhack.impl.event.events.render.Render2DEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.managers.Managers;
import net.minecraft.entity.player.EntityPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class ListenerRender2D
        extends ModuleListener<TextRadar, Render2DEvent>
{
    public ListenerRender2D(TextRadar module)
    {
        super(module, Render2DEvent.class);
    }

    @Override
    public void invoke(Render2DEvent event)
    {
        if (mc.world == null || mc.player == null)
        {
            return;
        }

        List<EntityPlayer> players = new ArrayList<>();
        for (EntityPlayer player : mc.world.playerEntities)
        {
            if (player == null
                || player == mc.player
                || player.isDead
                || mc.player.getDistance(player) > module.range.getValue())
            {
                continue;
            }

            if (module.display.getValue() == TextRadar.Display.Friend
                && !Managers.FRIENDS.contains(player))
            {
                continue;
            }

            if (module.display.getValue() == TextRadar.Display.Enemy
                && !Managers.ENEMIES.contains(player)
                && Managers.FRIENDS.contains(player))
            {
                continue;
            }

            players.add(player);
        }

        players.sort(Comparator.comparingDouble(mc.player::getDistance));
        int y = module.y.getValue();
        for (EntityPlayer player : players)
        {
            boolean friend = Managers.FRIENDS.contains(player);
            String text = (friend ? "\247b" : "\247f") + player.getName()
                + " \2477" + (int) mc.player.getDistance(player) + "m";
            mc.fontRenderer.drawStringWithShadow(
                text, module.x.getValue(), y, 0xFFFFFF);
            y += 10;
        }
    }
}
