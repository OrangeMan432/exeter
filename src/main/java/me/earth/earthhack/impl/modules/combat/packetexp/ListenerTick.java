package me.earth.earthhack.impl.modules.combat.packetexp;

import me.earth.earthhack.impl.event.events.misc.TickEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.item.ItemExpBottle;
import net.minecraft.network.play.client.CPacketHeldItemChange;
import net.minecraft.network.play.client.CPacketPlayerTryUseItem;
import net.minecraft.util.EnumHand;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

final class ListenerTick extends ModuleListener<PacketExp, TickEvent>
{
    public ListenerTick(PacketExp module)
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

        if (module.mode.getValue() == PacketExp.Mode.MIDDLECLICK
            && Mouse.isButtonDown(2))
        {
            throwExp();
        }
        else if (module.mode.getValue() == PacketExp.Mode.KEY
            && module.getBind().getKey() != -1
            && Keyboard.isKeyDown(module.getBind().getKey()))
        {
            throwExp();
        }
    }

    private void throwExp()
    {
        int oldSlot = mc.player.inventory.currentItem;
        int xpSlot = findXp();
        if (xpSlot != -1
            && module.delayTimer.passed(module.delay.getValue() * 20L))
        {
            mc.player.connection.sendPacket(
                new CPacketHeldItemChange(xpSlot));
            mc.player.connection.sendPacket(
                new CPacketPlayerTryUseItem(EnumHand.MAIN_HAND));
            mc.player.connection.sendPacket(
                new CPacketHeldItemChange(oldSlot));
            module.delayTimer.reset();
        }
    }

    private int findXp()
    {
        for (int i = 0; i < 9; i++)
        {
            if (mc.player.inventory.getStackInSlot(i).getItem()
                    instanceof ItemExpBottle)
            {
                return i;
            }
        }

        return -1;
    }
}
