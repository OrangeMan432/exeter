package me.earth.earthhack.impl.modules.misc.visualrange;

import me.earth.earthhack.impl.event.events.misc.TickEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.managers.Managers;
import me.earth.earthhack.impl.util.text.ChatUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextFormatting;

import java.util.ArrayList;
import java.util.List;

final class ListenerTick extends ModuleListener<VisualRange, TickEvent>
{
    public ListenerTick(VisualRange module)
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

        List<String> tickPlayers = new ArrayList<>();
        for (Entity entity : mc.world.loadedEntityList)
        {
            if (entity instanceof EntityPlayer
                && !entity.getName().equals(mc.player.getName()))
            {
                tickPlayers.add(entity.getName());
            }
        }

        for (String name : tickPlayers)
        {
            if (module.knownPlayers.contains(name))
            {
                continue;
            }

            module.knownPlayers.add(name);
            announce(name, true);
            return;
        }

        for (String name : new ArrayList<>(module.knownPlayers))
        {
            if (tickPlayers.contains(name))
            {
                continue;
            }

            module.knownPlayers.remove(name);
            if (module.leaving.getValue())
            {
                announce(name, false);
            }

            return;
        }
    }

    private void announce(String name, boolean entered)
    {
        EntityPlayer player = mc.world.getPlayerEntityByName(name);
        boolean friend = player != null && Managers.FRIENDS.contains(player);
        ChatUtil.sendMessage(
            (friend ? TextFormatting.GREEN : TextFormatting.RED)
                + name
                + TextFormatting.RESET
                + (entered
                    ? " entered visual range!"
                    : " left visual range!"));
    }
}
