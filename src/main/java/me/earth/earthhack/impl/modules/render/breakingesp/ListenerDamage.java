package me.earth.earthhack.impl.modules.render.breakingesp;

import me.earth.earthhack.impl.event.events.misc.DamageBlockEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.init.Blocks;

final class ListenerDamage
        extends ModuleListener<BreakingESP, DamageBlockEvent>
{
    public ListenerDamage(BreakingESP module)
    {
        super(module, DamageBlockEvent.class);
    }

    @Override
    public void invoke(DamageBlockEvent event)
    {
        if (mc.world == null
            || mc.player == null
            || mc.world.getBlockState(event.getPos()).getBlock()
                == Blocks.AIR)
        {
            return;
        }

        double rangeSq =
            module.range.getValue() * module.range.getValue();
        if (mc.player.getDistanceSq(event.getPos()) > rangeSq)
        {
            return;
        }

        if (event.getDamage() > 0.0f)
        {
            module.blocks.put(event.getPos(), event.getDamage());
        }
        else
        {
            module.blocks.remove(event.getPos());
        }
    }
}
