package me.earth.earthhack.impl.modules.movement.elytraplus;

import me.earth.earthhack.impl.event.events.movement.MoveEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.util.MovementInput;

final class ListenerMove extends ModuleListener<ElytraPlus, MoveEvent>
{
    public ListenerMove(ElytraPlus module)
    {
        super(module, MoveEvent.class);
    }

    @Override
    public void invoke(MoveEvent event)
    {
        if (mc.world == null
            || mc.player == null
            || !mc.player.isElytraFlying())
        {
            return;
        }

        if (module.stopWater.getValue() && mc.player.isInWater())
        {
            return;
        }

        if (module.stopLava.getValue() && mc.player.isInLava())
        {
            return;
        }

        if (module.mode.getValue() == ElytraPlus.Mode.Wasp)
        {
            doWasp(event);
        }
        else
        {
            doControl(event);
        }
    }

    private void doWasp(MoveEvent event)
    {
        updateMovement();
        module.pitch = mc.player.rotationPitch;

        double cos = Math.cos(Math.toRadians(module.yaw + 90.0));
        double sin = Math.sin(Math.toRadians(module.yaw + 90.0));
        double x = module.moving ? cos * module.horizontal.getValue() : 0.0;
        double y = -module.fallSpeed.getValue();
        double z = module.moving ? sin * module.horizontal.getValue() : 0.0;

        if (module.smartFall.getValue())
        {
            y *= Math.abs(Math.sin(Math.toRadians(module.pitch)));
        }

        if (mc.gameSettings.keyBindSneak.isKeyDown()
            && !mc.gameSettings.keyBindJump.isKeyDown())
        {
            y = -module.down.getValue();
        }

        if (!mc.gameSettings.keyBindSneak.isKeyDown()
            && mc.gameSettings.keyBindJump.isKeyDown())
        {
            y = module.up.getValue();
        }

        event.setX(x);
        event.setY(y);
        event.setZ(z);
        mc.player.motionX = 0.0;
        mc.player.motionY = 0.0;
        mc.player.motionZ = 0.0;
    }

    private void doControl(MoveEvent event)
    {
        updateMovement();
        module.pitch = 0.0f;

        boolean movingUp = false;
        if (!mc.gameSettings.keyBindSneak.isKeyDown()
            && mc.gameSettings.keyBindJump.isKeyDown()
            && module.velocity > module.speed.getValue() * 0.4)
        {
            module.p = (float) Math.min(
                module.p + 0.1 * (1 - module.p) * (1 - module.p)
                    * (1 - module.p), 1.0f);
            module.pitch = Math.max(Math.max(module.p, 0.0f) * -90.0f, -90.0f);
            movingUp = true;
            module.moving = false;
        }
        else
        {
            module.velocity = module.speed.getValue();
            module.p = -0.2f;
        }

        module.velocity = module.moving
            ? module.speed.getValue()
            : Math.min(module.velocity
                + Math.sin(Math.toRadians(module.pitch)) * 0.08,
                module.speed.getValue());

        double cos = Math.cos(Math.toRadians(module.yaw + 90.0));
        double sin = Math.sin(Math.toRadians(module.yaw + 90.0));
        double x = module.moving && !movingUp
            ? cos * module.speed.getValue()
            : movingUp
                ? module.velocity * Math.cos(Math.toRadians(module.pitch))
                    * cos
                : 0.0;
        double y = module.pitch < 0.0f
            ? module.velocity * module.upMultiplier.getValue()
                * -Math.sin(Math.toRadians(module.pitch))
                * module.velocity
            : -module.fallSpeed.getValue();
        double z = module.moving && !movingUp
            ? sin * module.speed.getValue()
            : movingUp
                ? module.velocity * Math.cos(Math.toRadians(module.pitch))
                    * sin
                : 0.0;

        y *= Math.abs(Math.sin(Math.toRadians(
            movingUp ? module.pitch : mc.player.rotationPitch)));
        if (mc.gameSettings.keyBindSneak.isKeyDown()
            && !mc.gameSettings.keyBindJump.isKeyDown())
        {
            y = -module.down.getValue();
        }

        event.setX(x);
        event.setY(y);
        event.setZ(z);
        mc.player.motionX = 0.0;
        mc.player.motionY = 0.0;
        mc.player.motionZ = 0.0;
    }

    private void updateMovement()
    {
        float yaw = mc.player.rotationYaw;
        MovementInput input = mc.player.movementInput;
        float forward = input.moveForward;
        float strafe = input.moveStrafe;

        if (forward > 0.0f)
        {
            module.moving = true;
            yaw += strafe > 0.0f ? -45.0f : strafe < 0.0f ? 45.0f : 0.0f;
        }
        else if (forward < 0.0f)
        {
            module.moving = true;
            yaw += strafe > 0.0f ? -135.0f : strafe < 0.0f ? 135.0f : 180.0f;
        }
        else
        {
            module.moving = strafe != 0.0f;
            yaw += strafe > 0.0f ? -90.0f : strafe < 0.0f ? 90.0f : 0.0f;
        }

        module.yaw = yaw;
    }
}
