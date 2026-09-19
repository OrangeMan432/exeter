package me.earth.earthhack.impl.modules.misc.killeffects;

import me.earth.earthhack.impl.event.events.misc.DeathEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;

final class ListenerDeath extends ModuleListener<KillEffects, DeathEvent>
{
    public ListenerDeath(KillEffects module)
    {
        super(module, DeathEvent.class);
    }

    @Override
    public void invoke(DeathEvent event)
    {
        if (mc.world == null
            || mc.player == null
            || mc.player.isDead
            || !(event.getEntity() instanceof EntityPlayer))
        {
            return;
        }

        EntityPlayer player = (EntityPlayer) event.getEntity();
        if (player == mc.player || player.getHealth() > 0.0f)
        {
            return;
        }

        if (!module.timer.passed(1500))
        {
            return;
        }

        if (module.lightning.getValue() != KillEffects.Lightning.OFF)
        {
            mc.world.spawnEntity(new EntityLightningBolt(mc.world,
                                                         player.posX,
                                                         player.posY,
                                                         player.posZ,
                                                         true));
            if (module.lightning.getValue() == KillEffects.Lightning.NORMAL)
            {
                mc.player.playSound(SoundEvents.ENTITY_LIGHTNING_THUNDER,
                                    0.5f,
                                    1.0f);
            }
        }

        if (module.killSound.getValue() == KillEffects.KillSound.HYPIXEL)
        {
            mc.player.playSound(
                SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        }

        module.timer.reset();
    }
}
