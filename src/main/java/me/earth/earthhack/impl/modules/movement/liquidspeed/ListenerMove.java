package me.earth.earthhack.impl.modules.movement.liquidspeed;

import me.earth.earthhack.impl.event.events.movement.MoveEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.managers.Managers;
import net.minecraft.init.MobEffects;
import net.minecraft.util.MovementInput;

final class ListenerMove extends ModuleListener<LiquidSpeed, MoveEvent>
{
    public ListenerMove(LiquidSpeed module)
    {
        super(module, MoveEvent.class);
    }

    @Override
    public void invoke(MoveEvent event)
    {
        if (mc.world == null || mc.player == null)
        {
            return;
        }

        if (!mc.player.isInWater() && !mc.player.isInLava())
        {
            return;
        }

        if (!module.groundIgnore.getValue() && mc.player.onGround)
        {
            event.setX(0.0);
            event.setZ(0.0);
            module.moveSpeed = 0.0;
            module.motionY = 0.0;
            return;
        }

        if (mc.player.isInWater())
        {
            swim(event,
                 module.yBoostWater.getValue(),
                 module.upWater.getValue(),
                 module.downWater.getValue() * 20.0,
                 module.xzBoostWater.getValue(),
                 module.xzWater.getValue());
        }
        else
        {
            swim(event,
                 module.yBoostLava.getValue(),
                 module.upLava.getValue(),
                 module.downLava.getValue(),
                 module.xzBoostLava.getValue(),
                 module.xzLava.getValue());
        }
    }

    private void swim(MoveEvent event,
                      double yBoost,
                      double upSpeed,
                      double downSpeed,
                      double xzBoost,
                      double xzSpeed)
    {
        MovementInput input = mc.player.movementInput;
        boolean jump = input.jump;
        boolean sneak = input.sneak;
        module.motionY = Math.pow(0.1, module.jitter.getValue());
        if (!jump || !sneak)
        {
            if (jump)
            {
                module.motionY = Math.min(
                    module.motionY + yBoost / 20.0, upSpeed / 20.0);
            }

            if (sneak)
            {
                module.motionY = Math.max(
                    module.motionY - yBoost / 20.0, -downSpeed / 20.0);
            }
        }

        event.setY(module.motionY);
        if (jump && sneak || !jump && !sneak)
        {
            Managers.TIMER.setTimer(1.0f);
        }
        else
        {
            Managers.TIMER.setTimer(
                module.timerSpeed.getValue().floatValue());
        }

        if (input.moveForward == 0.0f && input.moveStrafe == 0.0f)
        {
            event.setX(0.0);
            event.setZ(0.0);
            module.moveSpeed = 0.0;
            return;
        }

        double yaw = moveYaw();
        double multiplier = potionMultiplier();
        module.moveSpeed = Math.min(
            Math.max(module.moveSpeed * xzBoost, 0.05), xzSpeed / 20.0);
        if (sneak && !jump)
        {
            double downMotion = mc.player.motionY * 0.25;
            module.moveSpeed = Math.min(
                module.moveSpeed,
                Math.max(module.moveSpeed + downMotion, 0.0));
        }

        module.moveSpeed *= multiplier;
        event.setX(-Math.sin(yaw) * module.moveSpeed);
        event.setZ(Math.cos(yaw) * module.moveSpeed);
    }

    private double moveYaw()
    {
        float yaw = mc.player.rotationYaw;
        float forward = mc.player.movementInput.moveForward;
        float strafe = mc.player.movementInput.moveStrafe;
        if (forward > 0.0f)
        {
            yaw += strafe > 0.0f ? -45.0f : strafe < 0.0f ? 45.0f : 0.0f;
        }
        else if (forward < 0.0f)
        {
            yaw += strafe > 0.0f ? -135.0f : strafe < 0.0f ? 135.0f : 180.0f;
        }
        else
        {
            yaw += strafe > 0.0f ? -90.0f : strafe < 0.0f ? 90.0f : 0.0f;
        }

        return Math.toRadians(yaw);
    }

    private double potionMultiplier()
    {
        double result = 1.0;
        if (mc.player.isPotionActive(net.minecraft.init.MobEffects.SPEED)
            && mc.player.getActivePotionEffect(
                net.minecraft.init.MobEffects.SPEED) != null)
        {
            result += (mc.player.getActivePotionEffect(
                net.minecraft.init.MobEffects.SPEED).getAmplifier() + 1)
                * 0.2;
        }

        if (mc.player.isPotionActive(
                net.minecraft.init.MobEffects.SLOWNESS)
            && mc.player.getActivePotionEffect(
                net.minecraft.init.MobEffects.SLOWNESS) != null)
        {
            result -= (mc.player.getActivePotionEffect(
                net.minecraft.init.MobEffects.SLOWNESS).getAmplifier() + 1)
                * 0.15;
        }

        return result;
    }
}
