package me.earth.earthhack.impl.modules.misc.packetlogger;

import me.earth.earthhack.impl.event.events.network.PacketEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.util.text.ChatUtil;

final class ListenerReceive extends
        ModuleListener<PacketLogger, PacketEvent.Receive<?>>
{
    int total;

    public ListenerReceive(PacketLogger module)
    {
        super(module, PacketEvent.Receive.class);
    }

    @Override
    public void invoke(PacketEvent.Receive<?> event)
    {
        if (!module.incoming.getValue())
        {
            return;
        }

        String name = event.getPacket().getClass().getSimpleName();
        module.inCounts.merge(name, 1, Integer::sum);
        if (++total % 600 == 0)
        {
            report(module.inCounts, "in");
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
