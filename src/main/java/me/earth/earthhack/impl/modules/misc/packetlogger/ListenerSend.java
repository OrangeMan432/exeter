package me.earth.earthhack.impl.modules.misc.packetlogger;

import me.earth.earthhack.impl.event.events.network.PacketEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.util.text.ChatUtil;

final class ListenerSend extends
        ModuleListener<PacketLogger, PacketEvent.Send<?>>
{
    int total;

    public ListenerSend(PacketLogger module)
    {
        super(module, PacketEvent.Send.class);
    }

    @Override
    public void invoke(PacketEvent.Send<?> event)
    {
        if (!module.outgoing.getValue())
        {
            return;
        }

        String name = event.getPacket().getClass().getSimpleName();
        module.outCounts.merge(name, 1, Integer::sum);
        if (++total % 600 == 0)
        {
            report(module.outCounts, "out");
            total = 0;
        }
    }

    private void report(java.util.Map<String, Integer> counts, String dir)
    {
        counts.entrySet().stream()
            .sorted((a, b) -> b.getValue() - a.getValue())
            .limit(module.topCount.getValue())
            .forEach(e -> ChatUtil.sendMessage(
                "[PacketLogger " + dir + "] " + e.getKey()
                    + ": " + e.getValue()));
        counts.clear();
    }
}
