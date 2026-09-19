package me.earth.earthhack.impl.modules.misc.ghastnotifier;

import me.earth.earthhack.impl.event.events.misc.TickEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.util.text.ChatUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityGhast;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.text.TextFormatting;

import java.util.HashSet;
import java.util.Set;

final class ListenerTick extends ModuleListener<GhastNotifier, TickEvent>
{
    static final Set<Entity> seen = new HashSet<>();

    public ListenerTick(GhastNotifier module)
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
            if (!(entity instanceof EntityGhast) || seen.contains(entity))
            {
                continue;
            }

            seen.add(entity);
            if (module.chat.getValue())
            {
                if (module.censorCoords.getValue())
                {
                    ChatUtil.sendMessage(TextFormatting.GOLD
                        + "There is a ghast!");
                }
                else
                {
                    ChatUtil.sendMessage(TextFormatting.GOLD
                        + "There is a ghast at: "
                        + entity.getPosition().getX() + "X, "
                        + entity.getPosition().getY() + "Y, "
                        + entity.getPosition().getZ() + "Z.");
                }
            }

            if (module.sound.getValue())
            {
                mc.player.playSound(SoundEvents.BLOCK_ANVIL_DESTROY,
                                    1.0f,
                                    1.0f);
            }
        }
    }
}
