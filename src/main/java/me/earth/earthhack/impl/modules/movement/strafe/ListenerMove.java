package me.earth.earthhack.impl.modules.movement.strafe;

import me.earth.earthhack.impl.event.events.movement.MoveEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.util.minecraft.MovementUtil;
import net.minecraft.init.MobEffects;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class ListenerMove extends ModuleListener<Strafe, MoveEvent>
{
    public ListenerMove(Strafe module)
    {
        super(module, MoveEvent.class);
    }

    @Override
    public void invoke(MoveEvent event)
    {
        if (mc.player == null || mc.world == null || module.shouldReturn())
        {
            return;
        }

        if (!mc.player.onGround)
        {
            if (module.wait.getValue() && module.waitForGround)
            {
                return;
            }
        }
        else
        {
            module.waitForGround = false;
        }

        if (module.mode.getValue() == Strafe.Mode.NCP)
        {
            doNCP(event);
        }
        else
        {
            doBhop(event);
        }
    }

    private void doBhop(MoveEvent event)
    {
        float moveForward = mc.player.movementInput.moveForward;
        float moveStrafe = mc.player.movementInput.moveStrafe;
        float rotationYaw = mc.player.rotationYaw;

        if (module.step.getValue() == 1)
        {
            mc.player.stepHeight = 0.6f;
        }

        if (module.bhop.getValue()
            && mc.player.onGround
            && module.getSpeedKpH() < module.speedLimit2.getValue())
        {
            module.stage = 2;
        }

        if (module.setGround.getValue()
            && round(mc.player.posY - (int) mc.player.posY, 3)
                == round(module.groundLimit.getValue() / 1000.0, 3)
            && (!module.noGroundLag.getValue() || isEntityMoving()))
        {
            if (module.setNull.getValue())
            {
                mc.player.motionY = 0.0;
            }
            else
            {
                mc.player.motionY -= module.groundFactor.getValue() / 100.0;
                event.setY(event.getY()
                    - module.groundFactor.getValue() / 100.0);
                if (module.setPos.getValue())
                {
                    mc.player.posY -=
                        module.groundFactor.getValue() / 100.0;
                }
            }
        }

        if (module.stage == 1 && MovementUtil.isMoving())
        {
            module.stage = 2;
            module.moveSpeed =
                module.getMultiplier() * Strafe.getBaseMoveSpeed() - 0.01;
        }
        else if (module.stage == 2 && MovementUtil.isMoving())
        {
            module.stage = 3;
            mc.player.motionY = module.yOffset.getValue() / 1000.0;
            event.setY(module.yOffset.getValue() / 1000.0);
            if (module.cooldownHops > 0)
            {
                module.cooldownHops--;
            }

            module.hops++;
            double accel = module.acceleration.getValue() == 2149
                ? 2.149802
                : module.acceleration.getValue() / 1000.0;
            module.moveSpeed *= accel;
        }
        else if (module.stage == 3)
        {
            module.stage = 4;
            double difference =
                0.66 * (module.lastDist - Strafe.getBaseMoveSpeed());
            module.moveSpeed = module.lastDist - difference;
        }
        else
        {
            if (mc.world.getCollisionBoxes(mc.player,
                    mc.player.getEntityBoundingBox()
                        .offset(0.0, mc.player.motionY, 0.0)).size() > 0
                || mc.player.collidedVertically && module.stage > 0)
            {
                module.stage = module.hop.getValue()
                    && module.getSpeedKpH() >= module.speedLimit.getValue()
                        ? 0
                        : mc.player.moveForward != 0.0f
                            || mc.player.moveStrafing != 0.0f ? 1 : 0;
            }

            module.moveSpeed =
                module.lastDist - module.lastDist / module.dFactor.getValue();
        }

        module.moveSpeed =
            Math.max(module.moveSpeed, Strafe.getBaseMoveSpeed());
        if (module.hopWait.getValue()
            && module.bhop.getValue()
            && module.hops < 2)
        {
            module.moveSpeed = MovementUtil.getSpeed(false);
        }

        if (moveForward == 0.0f && moveStrafe == 0.0f)
        {
            event.setX(0.0);
            event.setZ(0.0);
            module.moveSpeed = 0.0;
        }
        else if (moveForward != 0.0f)
        {
            if (moveStrafe >= 1.0f)
            {
                rotationYaw += moveForward > 0.0f ? -45.0f : 45.0f;
                moveStrafe = 0.0f;
            }
            else if (moveStrafe <= -1.0f)
            {
                rotationYaw += moveForward > 0.0f ? 45.0f : -45.0f;
                moveStrafe = 0.0f;
            }

            moveForward = moveForward > 0.0f ? 1.0f : -1.0f;
        }

        double motionX = Math.cos(Math.toRadians(rotationYaw + 90.0f));
        double motionZ = Math.sin(Math.toRadians(rotationYaw + 90.0f));
        if (module.cooldownHops == 0)
        {
            event.setX(moveForward * module.moveSpeed * motionX
                + moveStrafe * module.moveSpeed * motionZ);
            event.setZ(moveForward * module.moveSpeed * motionZ
                - moveStrafe * module.moveSpeed * motionX);
        }

        if (module.step.getValue() == 2)
        {
            mc.player.stepHeight = 0.6f;
        }

        if (moveForward == 0.0f && moveStrafe == 0.0f)
        {
            module.timer.reset();
            event.setX(0.0);
            event.setZ(0.0);
        }
    }

    private void doNCP(MoveEvent event)
    {
        if (!module.setGround.getValue() && mc.player.onGround)
        {
            module.stage = 2;
        }

        switch (module.stage)
        {
            case 0:
                module.stage++;
                module.lastDist = 0.0;
                break;
            case 2:
            {
                double motionY = 0.40123128;
                if (mc.player.moveForward == 0.0f
                    && mc.player.moveStrafing == 0.0f
                    || !mc.player.onGround)
                {
                    break;
                }

                if (mc.player.isPotionActive(MobEffects.JUMP_BOOST)
                    && mc.player.getActivePotionEffect(MobEffects.JUMP_BOOST)
                        != null)
                {
                    motionY += (mc.player.getActivePotionEffect(
                        MobEffects.JUMP_BOOST).getAmplifier() + 1) * 0.1;
                }

                mc.player.motionY = motionY;
                event.setY(mc.player.motionY);
                module.moveSpeed *= 2.149;
                break;
            }
            case 3:
                module.moveSpeed = module.lastDist
                    - 0.76 * (module.lastDist - getBaseMoveSpeedStatic());
                break;
            default:
                if (mc.world.getCollisionBoxes(mc.player,
                        mc.player.getEntityBoundingBox()
                            .offset(0.0, mc.player.motionY, 0.0)).size() > 0
                    || mc.player.collidedVertically && module.stage > 0)
                {
                    module.stage = module.hop.getValue()
                        && module.getSpeedKpH() >= module.speedLimit.getValue()
                            ? 0
                            : mc.player.moveForward != 0.0f
                                || mc.player.moveStrafing != 0.0f ? 1 : 0;
                }

                module.moveSpeed =
                    module.lastDist - module.lastDist / 159.0;
                break;
        }

        module.moveSpeed =
            Math.max(module.moveSpeed, getBaseMoveSpeedStatic());
        double forward = mc.player.movementInput.moveForward;
        double strafe = mc.player.movementInput.moveStrafe;
        double yaw = mc.player.rotationYaw;
        if (forward == 0.0 && strafe == 0.0)
        {
            event.setX(0.0);
            event.setZ(0.0);
        }
        else if (forward != 0.0 && strafe != 0.0)
        {
            forward *= Math.sin(0.7853981633974483);
            strafe *= Math.cos(0.7853981633974483);
        }

        event.setX((forward * module.moveSpeed * -Math.sin(Math.toRadians(yaw))
            + strafe * module.moveSpeed * Math.cos(Math.toRadians(yaw)))
            * 0.99);
        event.setZ((forward * module.moveSpeed * Math.cos(Math.toRadians(yaw))
            - strafe * module.moveSpeed * -Math.sin(Math.toRadians(yaw)))
            * 0.99);
        module.stage++;
    }

    private boolean isEntityMoving()
    {
        return Math.hypot(mc.player.motionX, mc.player.motionZ) > 0.01;
    }

    private double getBaseMoveSpeedStatic()
    {
        return Strafe.getBaseMoveSpeed();
    }

    private static double round(double value, int places)
    {
        if (places < 0)
        {
            throw new IllegalArgumentException();
        }

        return new BigDecimal(value).setScale(places, RoundingMode.HALF_UP)
            .doubleValue();
    }
}
