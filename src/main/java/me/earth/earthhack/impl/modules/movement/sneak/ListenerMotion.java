package me.earth.earthhack.impl.modules.movement.sneak;

import me.earth.earthhack.impl.event.events.network.MotionUpdateEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.network.play.client.CPacketEntityAction;

final class ListenerMotion extends ModuleListener<Sneak, MotionUpdateEvent>
{
    public ListenerMotion(Sneak module)
    {
        super(module, MotionUpdateEvent.class);
    }

    @Override
    public void invoke(MotionUpdateEvent event)
    {
        if (mc.world == null || mc.player == null)
        {
            return;
        }

        switch (event.getStage())
        {
            case PRE:
                break;
            default:
                return;
        }

        switch (module.mode.getValue())
        {
            case Vanilla:
                mc.player.connection.sendPacket(new CPacketEntityAction(
                    mc.player,
                    CPacketEntityAction.Action.START_SNEAKING));
                break;
            case NCP:
                if (!mc.player.isSneaking())
                {
                    if (isMoving())
                    {
                        mc.player.connection.sendPacket(
                            new CPacketEntityAction(
                                mc.player,
                                CPacketEntityAction.Action.START_SNEAKING));
                        mc.player.connection.sendPacket(
                            new CPacketEntityAction(
                                mc.player,
                                CPacketEntityAction.Action.STOP_SNEAKING));
                    }
                    else
                    {
                        mc.player.connection.sendPacket(
                            new CPacketEntityAction(
                                mc.player,
                                CPacketEntityAction.Action.START_SNEAKING));
                    }
                }

                break;
            case Always:
                mc.gameSettings.keyBindSneak.pressed = true;
                break;
            default:
                break;
        }
    }

    private boolean isMoving()
    {
        return GameSettings.isKeyDown(mc.gameSettings.keyBindForward)
            || GameSettings.isKeyDown(mc.gameSettings.keyBindLeft)
            || GameSettings.isKeyDown(mc.gameSettings.keyBindRight)
            || GameSettings.isKeyDown(mc.gameSettings.keyBindBack);
    }
}
