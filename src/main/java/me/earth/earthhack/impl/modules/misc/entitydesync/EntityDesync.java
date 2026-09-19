package me.earth.earthhack.impl.modules.misc.entitydesync;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.impl.util.client.SimpleData;
import me.earth.earthhack.impl.util.text.ChatUtil;
import net.minecraft.entity.Entity;
import net.minecraft.util.text.TextFormatting;

/**
 * Clientside dismount for boat and entity desync.
 * Ported from SalHack (EntityDesync).
 */
public class EntityDesync extends Module
{
    Entity riding;

    public EntityDesync()
    {
        super("EntityDesync", Category.Misc);
        this.listeners.add(new ListenerTick(this));
        this.listeners.add(new ListenerPassengers(this));
        this.listeners.add(new ListenerDestroy(this));
        this.setData(new SimpleData(this,
            "Desyncs ridden entities, boat fly tricks."));
    }

    @Override
    protected void onEnable()
    {
        if (mc.player == null)
        {
            riding = null;
            disable();
            return;
        }

        if (!mc.player.isRiding())
        {
            ChatUtil.sendMessage(TextFormatting.RED
                + "[EntityDesync] You are not riding anything.");
            riding = null;
            disable();
            return;
        }

        riding = mc.player.getRidingEntity();
        mc.player.dismountRidingEntity();
        mc.world.removeEntity(riding);
    }

    @Override
    protected void onDisable()
    {
        if (riding != null)
        {
            riding.isDead = false;
            if (mc.player != null && !mc.player.isRiding())
            {
                mc.world.spawnEntity(riding);
                mc.player.startRiding(riding, true);
                ChatUtil.sendMessage(TextFormatting.GREEN
                    + "[EntityDesync] Forced a remount.");
            }

            riding = null;
        }
    }
}
