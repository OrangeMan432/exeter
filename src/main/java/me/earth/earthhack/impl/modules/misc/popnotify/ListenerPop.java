package me.earth.earthhack.impl.modules.misc.popnotify;

import me.earth.earthhack.impl.event.events.misc.TotemPopEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.managers.Managers;
import me.earth.earthhack.impl.util.text.ChatUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextFormatting;

final class ListenerPop extends ModuleListener<PopNotify, TotemPopEvent>
{
    public ListenerPop(PopNotify module)
    {
        super(module, TotemPopEvent.class);
    }

    @Override
    public void invoke(TotemPopEvent event)
    {
        if (mc.world == null
            || mc.player == null
            || mc.player.equals(event.getEntity()))
        {
            return;
        }

        EntityPlayer player = event.getEntity();
        int pops = module.totemPops.getOrDefault(player.getName(), 0) + 1;
        module.totemPops.put(player.getName(), pops);

        boolean friend = Managers.FRIENDS.contains(player);
        ChatUtil.sendMessage(
            TextFormatting.RESET
                + (friend ? TextFormatting.AQUA : TextFormatting.WHITE)
                + player.getName()
                + TextFormatting.RESET + " has popped "
                + (friend ? TextFormatting.AQUA : TextFormatting.WHITE)
                + TextFormatting.BOLD + pops + TextFormatting.RESET
                + (pops == 1 ? " totem!" : " totems!"),
            player.getEntityId());
    }
}
