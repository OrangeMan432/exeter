package me.earth.earthhack.impl.modules.combat.nofriendattack;

import me.earth.earthhack.impl.event.events.network.PacketEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.managers.Managers;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.CPacketUseEntity;

final class ListenerSend extends
        ModuleListener<NoFriendAttack, PacketEvent.Send<CPacketUseEntity>>
{
    public ListenerSend(NoFriendAttack module)
    {
        super(module, PacketEvent.Send.class, CPacketUseEntity.class);
    }

    @Override
    public void invoke(PacketEvent.Send<CPacketUseEntity> event)
    {
        if (mc.world == null)
        {
            return;
        }

        CPacketUseEntity packet = event.getPacket();
        if (packet.getAction() != CPacketUseEntity.Action.ATTACK)
        {
            return;
        }

        Entity entity = packet.getEntityFromWorld(mc.world);
        if (entity instanceof EntityPlayer
            && Managers.FRIENDS.contains((EntityPlayer) entity))
        {
            event.setCancelled(true);
        }
    }
}
