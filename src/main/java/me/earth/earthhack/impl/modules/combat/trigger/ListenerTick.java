package me.earth.earthhack.impl.modules.combat.trigger;

import me.earth.earthhack.impl.event.events.misc.TickEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.managers.Managers;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemSword;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.RayTraceResult;
import org.lwjgl.input.Mouse;

final class ListenerTick extends ModuleListener<Trigger, TickEvent>
{
    public ListenerTick(Trigger module)
    {
        super(module, TickEvent.class);
    }

    @Override
    public void invoke(TickEvent event)
    {
        if (mc.world == null
            || mc.player == null
            || mc.currentScreen != null
            || mc.playerController == null)
        {
            return;
        }

        boolean attack;
        switch (module.attackCheck.getValue())
        {
            case MOUSE:
                attack = Mouse.isButtonDown(0);
                break;
            case CROSSHAIR:
                attack = validTarget();
                break;
            default:
                attack = true;
                break;
        }

        if (!attack)
        {
            return;
        }

        if (module.weaponCheck.getValue()
            && !(mc.player.getHeldItemMainhand().getItem()
                    instanceof ItemSword)
            && !(mc.player.getHeldItemMainhand().getItem()
                    instanceof ItemAxe))
        {
            return;
        }

        double jitter = Math.random() * module.randomSpeed.getValue() * 2.0
            - module.randomSpeed.getValue();
        long delay = (long) Math.max(
            1000.0 / module.cps.getValue() + jitter * 50.0, 25.0);
        if (!module.timer.passed(delay))
        {
            return;
        }

        if (mc.objectMouseOver != null
            && mc.objectMouseOver.entityHit instanceof EntityLivingBase)
        {
            mc.playerController.attackEntity(
                mc.player, mc.objectMouseOver.entityHit);
            mc.player.swingArm(EnumHand.MAIN_HAND);
            module.timer.reset();
        }
        else if (module.attackCheck.getValue()
            == Trigger.AttackCheck.ALWAYS)
        {
            module.timer.reset();
        }
    }

    private boolean validTarget()
    {
        if (mc.objectMouseOver == null
            || !(mc.objectMouseOver.entityHit instanceof EntityLivingBase))
        {
            return false;
        }

        EntityLivingBase entity =
            (EntityLivingBase) mc.objectMouseOver.entityHit;
        if (mc.objectMouseOver.typeOfHit != RayTraceResult.Type.ENTITY)
        {
            return false;
        }

        if (module.invisibleCheck.getValue() && entity.isInvisible())
        {
            return false;
        }

        if (module.teamCheck.getValue() && entity.isOnSameTeam(mc.player))
        {
            return false;
        }

        return !module.friendCheck.getValue()
            || !Managers.FRIENDS.contains(entity);
    }
}
